package de.raindancer.modules.economy;

import de.raindancer.core.social.desk.DeskOffer;
import de.raindancer.core.ui.menu.Icons;
import de.raindancer.modules.economy.util.PermissionNodes;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Supplier;

/** The bank at a banker: the same screen as /bank — and with "bank in person only" on, the only way to it. */
public final class BankDeskOffer implements DeskOffer {

    private final Supplier<EconomyServices> services;

    public BankDeskOffer(Supplier<EconomyServices> services) {
        this.services = services;
    }

    @Override
    public boolean available() {
        return services.get() != null;
    }

    @Override
    public ItemStack icon(Player viewer) {
        return viewer.hasPermission(PermissionNodes.BALANCE)
                ? Icons.of(Material.GOLD_INGOT, "<white>My bank account",
                        "<gray>Your balance, transfers and statements.", "<yellow>Click<gray> to open")
                : null;
    }

    @Override
    public void use(Player viewer, Runnable backToDesk) {
        EconomyServices live = services.get();
        if (live != null && viewer.hasPermission(PermissionNodes.BALANCE)) {
            live.screens().bank(viewer);
        }
    }
}
