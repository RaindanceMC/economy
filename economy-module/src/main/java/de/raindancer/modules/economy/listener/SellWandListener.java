package de.raindancer.modules.economy.listener;

import de.raindancer.modules.economy.service.SellWandService;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.UUID;

/**
 * A right click on a chest with the Sell Wand. Last, so a claim that refused the chest has already said so — the
 * wand reaches only what its holder could open.
 */
public final class SellWandListener implements IEconomyListener {

    private final SellWandService wands;

    public SellWandListener(SellWandService wands) {
        this.wands = wands;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onUse(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND || block == null
                || !wands.isWand(event.getItem()) || !SellWandService.sellsFrom(block)) {
            return;
        }
        boolean refused = event.useInteractedBlock() == Event.Result.DENY;
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        if (!refused) {
            wands.preview(event.getPlayer(), block);
        }
    }

    @Override
    public void forget(UUID player) {
        // Nothing is remembered between clicks.
    }
}
