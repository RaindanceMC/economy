package de.raindancer.modules.economy.screen;

import de.raindancer.core.social.economy.Money;
import de.raindancer.core.ui.choose.Catalogue;
import de.raindancer.core.ui.menu.Backdrop;
import de.raindancer.core.ui.menu.Icons;
import de.raindancer.core.ui.menu.Menu;
import de.raindancer.core.ui.menu.PaginatedMenu;
import de.raindancer.modules.economy.EconomyServices;
import de.raindancer.modules.economy.model.ShopGood;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The drawer of Rain's items — the Sell Wand, the Auto Sell Chest — each for what its ingredients cost. */
public final class RainsItemsMenu extends PaginatedMenu<Map.Entry<ShopGood, Money>> implements IEconomyScreen {

    private final EconomyServices services;

    RainsItemsMenu(EconomyServices services, Player viewer, Menu parent) {
        super(viewer, services.brand(), parent);
        this.services = services;
    }

    @Override
    protected Backdrop backdrop() {
        return de.raindancer.modules.economy.service.EconomyPack.stall(layout().rows());
    }

    @Override
    protected Component title() {
        return MiniMessage.miniMessage().deserialize("<dark_gray>Rain's items");
    }

    @Override
    public String breadcrumb() {
        return "Rain's items";
    }

    @Override
    protected List<Map.Entry<ShopGood, Money>> entries() {
        return services.shop().goodOffers(viewer.getUniqueId());
    }

    @Override
    protected ItemStack emptyIcon() {
        return Icons.of(Material.BARRIER, "<gray>Nothing here", "<gray>None of Rain's items are sold on this server.");
    }

    @Override
    protected ItemStack icon(Map.Entry<ShopGood, Money> offer) {
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Costs " + MiniMessage.miniMessage().serialize(services.currency().render(offer.getValue()))
                + "<gray> — what its ingredients cost:");
        offer.getKey().ingredients().forEach((material, count) ->
                lore.add("<dark_gray> " + count + " × " + Catalogue.readable(material.name())));
        lore.add("");
        lore.add("<yellow>Click<gray> to buy one");
        ItemStack item = offer.getKey().item().get().orElse(new ItemStack(Material.BARRIER));
        return Icons.showing(item, "<gold>" + offer.getKey().name(), lore);
    }

    @Override
    protected void onClick(Map.Entry<ShopGood, Money> offer, InventoryClickEvent event) {
        services.shop().buyGood(viewer, offer.getKey(), offer.getValue());
        refresh();
    }

    @Override
    protected List<String> helpLines() {
        return List.of("Things the server makes: the Sell Wand and the Auto Sell Chest.",
                "Each costs what its ingredients cost you here — you can craft them too.");
    }

    @Override
    public String describe() {
        return "Rain's made items for money";
    }
}
