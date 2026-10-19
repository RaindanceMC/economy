package de.raindancer.modules.economy.rules;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Chest;

import java.util.Optional;

/** The decisions behind an Auto Sell Chest that need no server. */
public final class AutoSellChestRule implements IEconomyRule {

    @Override
    public String describe() {
        return "Auto Sell Chest: what an anvil makes, how a chest pairs, which way the display faces";
    }

    /** Whether an anvil's result is an Auto Sell Chest it may hand out: one chest, never a stack for one book. */
    public boolean makes(Material input, int amount, int autoSellLevel) {
        return input == Material.CHEST && amount == 1 && autoSellLevel > 0;
    }

    /** Where the other half of a double chest stands, as the game pairs them. */
    public Optional<BlockFace> partner(BlockFace facing, Chest.Type type) {
        return switch (type) {
            case LEFT -> Optional.of(clockwise(facing));
            case RIGHT -> Optional.of(clockwise(clockwise(clockwise(facing))));
            case SINGLE -> Optional.empty();
        };
    }

    private static BlockFace clockwise(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> face;
        };
    }

    /** The display's yaw for a chest facing this way; the chest model's front looks south. */
    public float yaw(BlockFace facing) {
        return switch (facing) {
            case WEST -> 90f;
            case NORTH -> 180f;
            case EAST -> 270f;
            default -> 0f;
        };
    }

    /** The crafting recipe, for whoever has no Auto Sell book: dear enough that the book stays the cheap way. */
    public java.util.List<String> recipeShape() {
        return java.util.List.of("GHG", "ECE", "GBG");
    }

    public java.util.Map<Character, Material> recipeIngredients() {
        return java.util.Map.of('G', Material.GOLD_BLOCK, 'H', Material.HOPPER, 'E', Material.EMERALD,
                'C', Material.CHEST, 'B', Material.EMERALD_BLOCK);
    }
}
