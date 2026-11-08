package de.raindancer.modules.economy.service;

import org.bukkit.Material;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What each player last bought and last sold at the shop, for the "again" buttons on its front page. This session
 * only: it is a convenience, not a record, and a restart forgetting it costs one click.
 */
public final class RecentTrades {

    private static final Map<UUID, Material> bought = new ConcurrentHashMap<>();
    private static final Map<UUID, Material> sold = new ConcurrentHashMap<>();

    private RecentTrades() {
    }

    public static void bought(UUID player, Material material) {
        if (player != null && material != null) {
            bought.put(player, material);
        }
    }

    public static void sold(UUID player, Material material) {
        if (player != null && material != null) {
            sold.put(player, material);
        }
    }

    public static Optional<Material> lastBought(UUID player) {
        return Optional.ofNullable(player == null ? null : bought.get(player));
    }

    public static Optional<Material> lastSold(UUID player) {
        return Optional.ofNullable(player == null ? null : sold.get(player));
    }

    public static void forget(UUID player) {
        bought.remove(player);
        sold.remove(player);
    }
}
