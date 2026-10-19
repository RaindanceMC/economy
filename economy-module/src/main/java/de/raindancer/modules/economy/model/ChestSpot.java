package de.raindancer.modules.economy.model;

import java.util.UUID;

/** Where a block stands, without holding on to its world. */
public record ChestSpot(UUID world, int x, int y, int z) {

    public int chunkX() {
        return x >> 4;
    }

    public int chunkZ() {
        return z >> 4;
    }
}
