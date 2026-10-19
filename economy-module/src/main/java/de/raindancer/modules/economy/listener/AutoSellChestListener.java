package de.raindancer.modules.economy.listener;

import de.raindancer.modules.economy.service.AutoSellChestService;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * Auto Sell Chests: made at the anvil, placed, opened as a selling window, broken back into the item.
 * Explosions pass them by — what they would drop is a plain chest, and the enchantment would be lost.
 */
public final class AutoSellChestListener implements IEconomyListener {

    private final AutoSellChestService chests;

    public AutoSellChestListener(AutoSellChestService chests) {
        this.chests = chests;
    }

    /** After the enchants module's gate (NORMAL), which may already have refused a switched-off Auto Sell. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onAnvil(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();
        ItemStack input = event.getInventory().getFirstItem();
        if (input == null || !AutoSellChestService.isItem(result)) {
            return;
        }
        event.setResult(chests.anvilMakes(input, result) ? AutoSellChestService.dress(result.clone()) : null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (block.getType() != Material.CHEST) {
            return;
        }
        if (AutoSellChestService.isItem(event.getItemInHand())) {
            chests.placed(block, event.getPlayer());
        } else {
            chests.keepSingle(block);
        }
    }

    /** The chest comes back as an Auto Sell Chest; first, so no other listener sees a plain chest drop. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrop(BlockDropItemEvent event) {
        if (AutoSellChestService.ownerOf(event.getBlockState()).isEmpty()) {
            return;
        }
        for (Item item : event.getItems()) {
            ItemStack stack = item.getItemStack();
            if (stack.getType() == Material.CHEST && !AutoSellChestService.isItem(stack)) {
                ItemStack dressed = AutoSellChestService.item();
                dressed.setAmount(stack.getAmount());
                item.setItemStack(dressed);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.CHEST) {
            chests.gone(event.getBlock());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        spare(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplode(BlockExplodeEvent event) {
        spare(event.blockList());
    }

    private void spare(List<Block> blocks) {
        blocks.removeIf(block -> block.getType() == Material.CHEST && chests.isChest(block));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onOpen(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND || block == null
                || block.getType() != Material.CHEST || event.useInteractedBlock() == Event.Result.DENY
                || !chests.isChest(block)) {
            return;
        }
        Player player = event.getPlayer();
        if (player.isSneaking() && !player.getInventory().getItemInMainHand().isEmpty()) {
            return;
        }
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        chests.open(player, block);
    }

    /** Hopper input is sold the tick after it arrives; the sweep every few seconds only catches the rest. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFed(org.bukkit.event.inventory.InventoryMoveItemEvent event) {
        if (event.getDestination().getHolder(false) instanceof org.bukkit.block.Chest chest
                && chests.isChest(chest.getBlock())) {
            chests.fed(chest.getBlock());
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        chests.loaded(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        chests.unloaded(event.getChunk());
    }

    @Override
    public void forget(UUID player) {
        // Core's tray settles itself when it closes, which a player leaving does too.
    }
}
