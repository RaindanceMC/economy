package de.raindancer.modules.economy.screen;

import de.raindancer.core.ui.menu.Backdrop;
import de.raindancer.core.ui.menu.Icons;
import de.raindancer.core.ui.menu.Menu;
import de.raindancer.core.ui.menu.PaginatedMenu;
import de.raindancer.modules.economy.EconomyServices;
import de.raindancer.modules.economy.service.ShopService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The drawer of Rain's enchantments: every level of each, for money — to the members of a guild
 * that unlocked it. Locked ones are shown too, with how to unlock them.
 */
public final class RainsEnchantMenu extends PaginatedMenu<ShopService.RainsOffer> implements IEconomyScreen {

    private final EconomyServices services;

    RainsEnchantMenu(EconomyServices services, Player viewer, Menu parent) {
        super(viewer, services.brand(), parent);
        this.services = services;
    }

    @Override
    protected Backdrop backdrop() {
        return de.raindancer.modules.economy.service.EconomyPack.stall(layout().rows());
    }

    @Override
    protected Component title() {
        return MiniMessage.miniMessage().deserialize("<dark_gray>Rain's books");
    }

    @Override
    public String breadcrumb() {
        return "Rain's books";
    }

    @Override
    protected List<ShopService.RainsOffer> entries() {
        return services.shop().rainsOffers(viewer.getUniqueId());
    }

    @Override
    protected ItemStack emptyIcon() {
        return Icons.of(Material.ENCHANTED_BOOK, "<gray>Nothing here", "<gray>Rain's enchantments are not sold on this server.");
    }

    @Override
    protected ItemStack icon(ShopService.RainsOffer offer) {
        String name = ShopService.rainsName(offer.enchantment()) + " " + roman(offer.level());
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Costs " + MiniMessage.miniMessage().serialize(services.currency().render(offer.money())) + "<gray>.");
        lore.add("");
        if (!offer.unlocked()) {
            lore.add("<red>Locked: your guild unlocks it");
            lore.add("<red>from its bank at /guild.");
            return Icons.of(Material.BOOK, "<gray>" + name, lore);
        }
        lore.add("<yellow>Click<gray> to buy the book");
        return Icons.of(Material.ENCHANTED_BOOK, "<light_purple>" + name, lore);
    }

    @Override
    protected void onClick(ShopService.RainsOffer offer, InventoryClickEvent event) {
        services.shop().buyRains(viewer, offer);
        refresh();
    }

    static String roman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(level);
        };
    }

    @Override
    protected List<String> helpLines() {
        return List.of("Books of Rain's own enchantments.",
                "Your guild unlocks an enchantment from its bank at /guild;",
                "then every member can buy its books for money.");
    }

    @Override
    public String describe() {
        return "Rain's enchantments for money";
    }
}
