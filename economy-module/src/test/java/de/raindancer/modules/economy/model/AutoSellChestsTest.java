package de.raindancer.modules.economy.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AutoSellChestsTest {

    private static final UUID WORLD = UUID.nameUUIDFromBytes("world".getBytes());
    private static final UUID NETHER = UUID.nameUUIDFromBytes("nether".getBytes());
    private static final UUID ADA = UUID.nameUUIDFromBytes("ada".getBytes());
    private static final UUID BO = UUID.nameUUIDFromBytes("bo".getBytes());

    @Test
    @DisplayName("a chest is known with its owner until it is broken")
    void knows() {
        AutoSellChests chests = new AutoSellChests();
        ChestSpot spot = new ChestSpot(WORLD, 10, 64, -3);
        chests.add(spot, ADA);
        assertThat(chests.owner(spot)).contains(ADA);
        assertThat(chests.owner(new ChestSpot(WORLD, 10, 64, -3))).as("by place, not by instance").contains(ADA);
        assertThat(chests.remove(spot)).contains(ADA);
        assertThat(chests.owner(spot)).isEmpty();
        assertThat(chests.all()).isEmpty();
    }

    @Test
    @DisplayName("unloading a chunk forgets the chests in it and only those — negative coordinates included")
    void chunks() {
        AutoSellChests chests = new AutoSellChests();
        ChestSpot inside = new ChestSpot(WORLD, -1, 70, -16);
        ChestSpot sameChunk = new ChestSpot(WORLD, -16, 5, -1);
        ChestSpot nextChunk = new ChestSpot(WORLD, 0, 70, -16);
        ChestSpot otherWorld = new ChestSpot(NETHER, -1, 70, -16);
        chests.add(inside, ADA);
        chests.add(sameChunk, BO);
        chests.add(nextChunk, ADA);
        chests.add(otherWorld, ADA);

        assertThat(inside.chunkX()).isEqualTo(-1);
        assertThat(inside.chunkZ()).isEqualTo(-1);
        assertThat(chests.removeChunk(WORLD, -1, -1)).containsExactlyInAnyOrder(inside, sameChunk);
        assertThat(chests.all()).containsOnlyKeys(nextChunk, otherWorld);
    }

    @Test
    @DisplayName("the display standing on a chest is remembered and handed back once, when the chest goes")
    void displays() {
        AutoSellChests chests = new AutoSellChests();
        ChestSpot spot = new ChestSpot(WORLD, 1, 2, 3);
        UUID display = UUID.randomUUID();
        chests.add(spot, ADA);
        chests.display(spot, display);
        assertThat(chests.display(spot)).contains(display);
        chests.remove(spot);
        assertThat(chests.display(spot)).isEmpty();
    }
}
