package de.raindancer.modules.economy.rules;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Chest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AutoSellChestRuleTest {

    private final AutoSellChestRule rule = new AutoSellChestRule();

    @Test
    @DisplayName("an anvil makes an Auto Sell Chest out of one plain chest and the book, never out of a stack")
    void anvil() {
        assertThat(rule.makes(Material.CHEST, 1, 1)).isTrue();
        assertThat(rule.makes(Material.CHEST, 2, 1)).as("one book, sixty-four chests").isFalse();
        assertThat(rule.makes(Material.CHEST, 1, 0)).as("no Auto Sell on the result").isFalse();
        assertThat(rule.makes(Material.TRAPPED_CHEST, 1, 1)).isFalse();
        assertThat(rule.makes(Material.DIAMOND_PICKAXE, 1, 1)).as("a tool is no chest").isFalse();
    }

    @Test
    @DisplayName("a half of a double chest finds its other half the way the game pairs them")
    void partner() {
        assertThat(rule.partner(BlockFace.NORTH, Chest.Type.LEFT)).contains(BlockFace.EAST);
        assertThat(rule.partner(BlockFace.NORTH, Chest.Type.RIGHT)).contains(BlockFace.WEST);
        assertThat(rule.partner(BlockFace.SOUTH, Chest.Type.LEFT)).contains(BlockFace.WEST);
        assertThat(rule.partner(BlockFace.EAST, Chest.Type.RIGHT)).contains(BlockFace.NORTH);
        assertThat(rule.partner(BlockFace.WEST, Chest.Type.LEFT)).contains(BlockFace.NORTH);
        assertThat(rule.partner(BlockFace.NORTH, Chest.Type.SINGLE)).isEmpty();
    }

    @Test
    @DisplayName("the display turns to face where the chest faces; the model's front looks south")
    void yaw() {
        assertThat(rule.yaw(BlockFace.SOUTH)).isEqualTo(0f);
        assertThat(rule.yaw(BlockFace.WEST)).isEqualTo(90f);
        assertThat(rule.yaw(BlockFace.NORTH)).isEqualTo(180f);
        assertThat(rule.yaw(BlockFace.EAST)).isEqualTo(270f);
        assertThat(rule.yaw(BlockFace.UP)).as("a chest never faces up; south is the safe guess").isEqualTo(0f);
    }
}
