package de.raindancer.modules.economy.service;

import de.raindancer.core.content.items.CustomItem;
import de.raindancer.core.content.items.CustomItems;
import de.raindancer.core.content.items.ItemFactory;
import de.raindancer.core.content.items.ItemRecipes;
import de.raindancer.core.social.economy.Money;
import de.raindancer.core.ui.chat.Brand;
import de.raindancer.core.ui.menu.ConfirmMenu;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.economy.model.SaleLot;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The Sell Wand: right-click a chest or a barrel with it, see what is in there that the shop buys and what it all
 * fetches, say yes, and it is sold and gone. What the shop cannot buy stays in the chest. A Core custom item, so
 * an owner can rename it, change its recipe or hand it out like any other; crafted by the recipe it carries.
 */
public final class SellWandService {

    public static final String PLUGIN = "economy";
    public static final String ID = "sell-wand";
    public static final String KEY = PLUGIN + ":" + ID;
    private static final int LINES = 10;
    private static final double REACH = 8;

    private final Plugin plugin;
    private final ShopService shop;
    private final Messages messages;
    private final Brand brand;
    private final CustomItems items;
    private final ItemFactory factory;
    private NamespacedKey recipe;

    public SellWandService(Plugin plugin, ShopService shop, Messages messages, Brand brand, CustomItems items,
                           ItemFactory factory) {
        this.plugin = plugin;
        this.shop = shop;
        this.messages = messages;
        this.brand = brand;
        this.items = items;
        this.factory = factory;
    }

    public static CustomItem definition() {
        return CustomItem.builder(PLUGIN, ID)
                .material(Material.BLAZE_ROD)
                .name("<gradient:#FFD86B:#3DDC84><bold>Sell Wand</bold></gradient>")
                .lore(List.of(
                        "<gray>Right-click a chest or a barrel:",
                        "<gray>see what it all fetches, say yes,",
                        "<gray>and everything the shop buys is sold.",
                        "",
                        "<dark_gray>What the shop cannot buy stays put."))
                .itemModel(EconomyPack.SELL_WAND.asString())
                .recipe(List.of(
                        "GOLD_INGOT EMERALD GOLD_INGOT",
                        "- BLAZE_ROD -",
                        "- BLAZE_ROD -"))
                .notAnIngredient()
                .build();
    }

    /** Tells Core about the wand and registers its recipe — the owner's version of it, if they changed it. */
    public void start() {
        items.defineIfAbsent(definition());
        CustomItem wand = items.byKey(KEY).orElse(definition());
        factory.create(wand).flatMap(stack -> ItemRecipes.build(plugin, wand, stack)).ifPresent(shaped -> {
            plugin.getServer().removeRecipe(shaped.getKey());
            if (plugin.getServer().addRecipe(shaped)) {
                recipe = shaped.getKey();
            }
        });
    }

    public void stop() {
        if (recipe != null) {
            plugin.getServer().removeRecipe(recipe);
            recipe = null;
        }
    }

    public boolean isWand(ItemStack stack) {
        return stack != null && factory.is(stack, KEY);
    }

    public Optional<ItemStack> wand() {
        return factory.create(items.byKey(KEY).orElse(definition()));
    }

    /** Whether the wand works on this block: a chest of any kind or a barrel — not an ender chest, not a furnace. */
    public static boolean sellsFrom(Block block) {
        return block.getState(false) instanceof Container container
                && (container.getInventory().getType() == InventoryType.CHEST
                || container.getInventory().getType() == InventoryType.BARREL);
    }

    /** Shows what this chest holds that the shop buys, and what it fetches; sells it on yes. */
    public void preview(Player player, Block block) {
        if (!(block.getState(false) instanceof Container container)) {
            return;
        }
        if (container.isLocked()) {
            messages.send(player, "economy.sell-wand.locked");
            return;
        }
        Map<String, long[]> lines = new LinkedHashMap<>();
        Map<String, Money> worth = new LinkedHashMap<>();
        Money total = Money.ZERO;
        int kept = 0;
        for (ItemStack stack : container.getInventory().getContents()) {
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            Optional<SaleLot> lot = shop.appraiseAnything(player.getUniqueId(), stack);
            if (lot.isEmpty()) {
                kept++;
                continue;
            }
            lines.computeIfAbsent(lot.get().label(), label -> new long[1])[0] += stack.getAmount();
            worth.merge(lot.get().label(), lot.get().total(), Money::plus);
            total = total.plus(lot.get().total());
        }
        if (lines.isEmpty()) {
            messages.send(player, "economy.sell-wand.nothing");
            return;
        }
        MiniMessage mini = MiniMessage.miniMessage();
        List<String> consequences = new ArrayList<>();
        int shown = 0;
        for (Map.Entry<String, long[]> line : lines.entrySet()) {
            if (shown++ == LINES) {
                consequences.add("<gray>… and " + (lines.size() - LINES) + " more kinds");
                break;
            }
            consequences.add("<white>" + line.getValue()[0] + " × " + mini.escapeTags(line.getKey()) + " <dark_gray>— "
                    + mini.serialize(shop.render(worth.get(line.getKey()))));
        }
        if (kept > 0) {
            consequences.add("");
            consequences.add("<gray>" + kept + " stack(s) the shop cannot buy stay in the chest.");
        }
        Location at = block.getLocation();
        new ConfirmMenu(player, brand, null, "<dark_gray>Sell it all for " + mini.serialize(shop.render(total)) + "?",
                consequences, "<dark_gray>Prices are today's; the money arrives a moment after.",
                () -> sell(player, at)).open();
    }

    /** The yes: what is in the chest now, sold — read again, so nothing put in or taken out since is lost. */
    private void sell(Player player, Location at) {
        Block block = at.getBlock();
        if (!player.getWorld().equals(at.getWorld()) || player.getLocation().distance(at.clone().add(0.5, 0.5, 0.5)) > REACH
                || !sellsFrom(block) || !(block.getState(false) instanceof Container container)) {
            messages.send(player, "economy.sell-wand.gone");
            return;
        }
        Inventory inside = container.getInventory();
        List<ItemStack> stacks = new ArrayList<>();
        List<Integer> slots = new ArrayList<>();
        ItemStack[] contents = inside.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (contents[slot] != null && !contents[slot].getType().isAir()) {
                stacks.add(contents[slot]);
                slots.add(slot);
            }
        }
        var sold = shop.autoSelling(player.getUniqueId(), stacks);
        int count = 0;
        for (int i = 0; i < stacks.size(); i++) {
            if (sold.sold()[i]) {
                count += stacks.get(i).getAmount();
                inside.setItem(slots.get(i), null);
            }
        }
        if (count == 0) {
            messages.send(player, "economy.sell-wand.nothing");
            return;
        }
        messages.send(player, "economy.sell-wand.sold", "count", String.valueOf(count),
                "amount", shop.render(sold.total()));
    }
}
