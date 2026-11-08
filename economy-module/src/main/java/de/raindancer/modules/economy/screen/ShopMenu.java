package de.raindancer.modules.economy.screen;

import de.raindancer.core.ui.choose.Category;
import de.raindancer.core.ui.menu.Backdrop;
import de.raindancer.core.ui.menu.Icons;
import de.raindancer.core.ui.menu.Menu;
import de.raindancer.core.ui.menu.MenuLayout;
import de.raindancer.core.ui.prompt.AnvilInput;
import de.raindancer.core.ui.prompt.Parsers;
import de.raindancer.modules.economy.EconomyServices;
import de.raindancer.modules.economy.EconomySettings;
import de.raindancer.modules.economy.util.Mini;
import de.raindancer.modules.economy.util.PermissionNodes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * {@code /shop}: the creative inventory's drawers. A drawer the owner closed is shown greyed with the
 * reason rather than hidden, so the page is the same shape for everybody. Admins switch a drawer with a
 * right click.
 */
public final class ShopMenu extends Menu implements IEconomyScreen {

    private final EconomyServices services;

    public ShopMenu(EconomyServices services, Player viewer, Menu parent) {
        super(viewer, services.brand(), parent);
        this.services = services;
    }

    /** The balance, on the stall's coin purse for somebody who sees the stall. */
    private org.bukkit.inventory.ItemStack purse(org.bukkit.inventory.ItemStack icon) {
        return designed() ? de.raindancer.modules.economy.service.EconomyPack.purse(icon) : icon;
    }

    @Override
    protected Backdrop backdrop() {
        return de.raindancer.modules.economy.service.EconomyPack.stall(layout().rows());
    }

    @Override
    protected Component title() {
        return MiniMessage.miniMessage().deserialize("<dark_gray>Shop");
    }

    @Override
    public String breadcrumb() {
        return "Shop";
    }

    @Override
    protected void render() {
        EconomySettings live = services.config();
        boolean admin = viewer.hasPermission(PermissionNodes.ADMIN);
        set(MenuLayout.HEADER_SUBJECT, purse(Icons.of(Material.EMERALD, "<green>Shop",
                "<gray>Your balance: " + Mini.of(services.currency().render(services.economy().balance(viewer.getUniqueId()))),
                "<dark_gray>Prices move with supply and demand.")));

        // Three rows, each one kind of thing: the creative inventory's drawers as one full-width row, the
        // server's own wares as the row below, and the two things that are not drawers — search and sell — on
        // their own with a pane between them. Eighteen buttons spread over three mixed bands read as a wall.

        // ── the creative inventory's drawers ───────────────────────────────────────────────────────────
        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            Category category = categories[i];
            int column = i;
            boolean open = live.categoryOpen(category);
            int items = open ? services.shop().prices().tradableIn(category).size() : 0;
            Material icon = Material.matchMaterial(category.icon());
            List<String> lore = new java.util.ArrayList<>();
            lore.add(open ? "<gray>" + items + " item(s) for sale" : "<red>Closed on this server");
            lore.add("");
            lore.add(open ? "<yellow>Click<gray> to open the drawer" : "<dark_gray>Nothing to open");
            if (admin) {
                lore.add("<yellow>Right click<gray> to " + (open ? "close" : "open") + " it");
            }
            cell(MenuLayout.WHO, column, Icons.of(icon == null ? Material.CHEST : icon,
                    (open ? "<green>" : "<gray>") + category.title(), lore), click -> {
                if (admin && click.isRightClick()) {
                    services.store().set(EconomySettings.categoryKey(category), String.valueOf(!open));
                    services.store().trySave();
                    refresh();
                } else if (open) {
                    services.screens().shopCategory(viewer, category);
                } else {
                    services.messages().send(viewer, "economy.shop.category-closed", "category", category.title());
                }
            });
        }

        // ── the server's own wares ─────────────────────────────────────────────────────────────────────
        int eggs = live.spawnEggs() ? services.shop().prices().tradableEggs().size() : 0;
        drawer(1, live.spawnEggs(), Icons.of(Material.PIG_SPAWN_EGG, "<green>Spawn eggs",
                "<gray>" + eggs + " mob(s) to hatch.", "", "<yellow>Click<gray> to open the drawer"),
                click -> ShopItemsMenu.eggs(services, viewer, this).open());
        int books = live.enchantBooks() ? services.shop().prices().tradableEnchants().size() : 0;
        drawer(2, live.enchantBooks(), Icons.of(Material.ENCHANTED_BOOK, "<light_purple>Enchantments",
                "<gray>Any enchantment as a book, for an anvil.",
                "<gray>" + books + " book(s), prices move with", "<gray>supply and demand.", "",
                "<yellow>Click<gray> to open the drawer"),
                click -> new EnchantMenu(services, viewer, this).open());
        int bottles = live.potions() ? services.shop().prices().tradablePotions().size() : 0;
        drawer(3, live.potions(), Icons.of(Material.POTION, "<aqua>Potions",
                "<gray>Potions, splash and lingering ones,",
                "<gray>and ominous bottles.",
                "<gray>" + bottles + " bottle(s), prices move with", "<gray>supply and demand.", "",
                "<yellow>Click<gray> to open the drawer"),
                click -> new PotionMenu(services, viewer, this).open());
        drawer(4, live.rainsEnchantments(), Icons.of(Material.ECHO_SHARD, "<light_purple>Rain's enchantments",
                "<gray>Books of the server's own enchantments,",
                "<gray>for their shards and a little money,",
                "<gray>once your guild has unlocked them.", "", "<yellow>Click<gray> to open the drawer"),
                click -> new RainsEnchantMenu(services, viewer, this).open());
        drawer(5, true, Icons.of(Material.BLAZE_ROD, "<gold>Rain's items",
                "<gray>The Sell Wand and the Auto Sell Chest,", "<gray>for what their ingredients cost.", "",
                "<yellow>Click<gray> to open the drawer"),
                click -> new RainsItemsMenu(services, viewer, this).open());
        drawer(6, services.packs().enabled(), Icons.of(Material.BUNDLE, "<gold>Packs",
                "<gray>Ready-made bundles, cheaper than",
                "<gray>buying it all one by one.",
                "<gray>" + services.packs().packs().size() + " pack(s).", "", "<yellow>Click<gray> to open the drawer"),
                click -> new PacksMenu(services, viewer, this).open());
        drawer(7, live.xpTradeEnabled(), Icons.of(Material.EXPERIENCE_BOTTLE, "<green>Experience",
                "<gray>Buy levels, priced by the point.",
                "<gray>A point: " + Mini.of(services.currency().render(live.xpBuyMoney())), "",
                "<yellow>Click<gray> to open the drawer"),
                click -> new ExperienceMenu(services, viewer, this).open());

        // ── not drawers: finding a thing, and selling ──────────────────────────────────────────────────
        band(MenuLayout.LAND, 3, Icons.of(Material.SPYGLASS, "<white>Search", "<gray>Find an item by name,",
                        "<gray>whichever drawer it is in.", "", "<yellow>Click<gray> to type a name"),
                click -> AnvilInput.open(viewer, "Search the shop", "", Parsers.text(32),
                        text -> services.screens().shopSearch(viewer, text), this::open));
        band(MenuLayout.LAND, 5, live.sellingEnabled(), Icons.of(Material.CHEST, "<white>Sell",
                "<gray>What you carry that the shop buys.", "", "<yellow>Click<gray> to see what it pays"), BankMenu.OFF,
                click -> services.screens().sell(viewer));
    }

    /** One of the server's own drawers, in the row under the creative inventory's; greyed with the reason when shut. */
    private void drawer(int column, boolean open, org.bukkit.inventory.ItemStack icon,
                        java.util.function.Consumer<org.bukkit.event.inventory.InventoryClickEvent> onClick) {
        if (open) {
            cell(MenuLayout.RULES, column, icon, onClick);
        } else {
            cell(MenuLayout.RULES, column, Icons.locked(icon, BankMenu.OFF), click -> { });
        }
    }

    @Override
    protected List<String> helpLines() {
        return List.of("Top row: drawers sorted the way the creative inventory is.",
                "Second row: the server's own wares — eggs, books,",
                "potions, Rain's enchantments and items, packs, levels.",
                "Below: search by name, or sell what you carry.",
                "",
                "Click a drawer, then an item to buy or sell it.");
    }

    @Override
    public String describe() {
        return "the shop's drawers";
    }
}
