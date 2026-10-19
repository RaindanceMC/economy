package de.raindancer.modules.economy.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Auto Sell Chests in loaded chunks, with who placed them and the display dressing each. The chest's own
 * data is the truth; this is rebuilt from it whenever a chunk loads. Safe from any region's thread.
 */
public final class AutoSellChests {

    private final Map<ChestSpot, UUID> owners = new ConcurrentHashMap<>();
    private final Map<ChestSpot, UUID> displays = new ConcurrentHashMap<>();

    public void add(ChestSpot spot, UUID owner) {
        owners.put(spot, owner);
    }

    public Optional<UUID> owner(ChestSpot spot) {
        return Optional.ofNullable(owners.get(spot));
    }

    /** Forgets a chest; its owner, if it was known. Its display is forgotten too — take it first. */
    public Optional<UUID> remove(ChestSpot spot) {
        displays.remove(spot);
        return Optional.ofNullable(owners.remove(spot));
    }

    /** Forgets every chest in one chunk; the ones it knew. */
    public List<ChestSpot> removeChunk(UUID world, int chunkX, int chunkZ) {
        List<ChestSpot> gone = owners.keySet().stream()
                .filter(spot -> spot.world().equals(world) && spot.chunkX() == chunkX && spot.chunkZ() == chunkZ)
                .toList();
        gone.forEach(this::remove);
        return gone;
    }

    public void display(ChestSpot spot, UUID display) {
        displays.put(spot, display);
    }

    public Optional<UUID> display(ChestSpot spot) {
        return Optional.ofNullable(displays.get(spot));
    }

    public Map<ChestSpot, UUID> all() {
        return Map.copyOf(owners);
    }
}
