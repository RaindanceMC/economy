package de.raindancer.modules.economy.store;

import de.raindancer.modules.economy.model.RecipeShape;
import org.bukkit.Material;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import net.kyori.adventure.key.Key;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.inventory.StonecuttingRecipe;
import org.bukkit.inventory.TransmuteRecipe;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The server's recipes, reduced to what pricing needs. A recipe that asks for one exact item (with its own
 * name or data) is skipped: the solver prices materials, and an exact item is not one.
 */
public final class RecipeReader {

    private RecipeReader() {
    }

    public static List<RecipeShape> read(Iterator<Recipe> recipes) {
        List<RecipeShape> read = new ArrayList<>();
        while (recipes.hasNext()) {
            Recipe recipe;
            try {
                recipe = recipes.next();
            } catch (RuntimeException unreadable) {
                continue;
            }
            RecipeShape shape = shapeOf(recipe);
            if (shape != null) {
                read.add(shape);
            }
        }
        return read;
    }

    static RecipeShape shapeOf(Recipe recipe) {
        if (recipe == null || recipe.getResult().getType().isAir()) {
            return null;
        }
        String result = recipe.getResult().getType().name();
        int amount = recipe.getResult().getAmount();
        List<List<String>> slots = new ArrayList<>();
        RecipeShape.Process process = RecipeShape.Process.CRAFT;
        switch (recipe) {
            case ShapedRecipe shaped -> {
                Map<Character, RecipeChoice> choices = shaped.getChoiceMap();
                for (String row : shaped.getShape()) {
                    for (char key : row.toCharArray()) {
                        RecipeChoice choice = choices.get(key);
                        if (choice != null && !add(slots, choice)) {
                            return null;
                        }
                    }
                }
            }
            case ShapelessRecipe shapeless -> {
                for (RecipeChoice choice : shapeless.getChoiceList()) {
                    if (!add(slots, choice)) {
                        return null;
                    }
                }
            }
            case CookingRecipe<?> cooking -> {
                process = RecipeShape.Process.SMELT;
                if (!add(slots, cooking.getInputChoice())) {
                    return null;
                }
            }
            case StonecuttingRecipe cutting -> {
                process = RecipeShape.Process.CUT;
                if (!add(slots, cutting.getInputChoice())) {
                    return null;
                }
            }
            case SmithingTransformRecipe smithing -> {
                process = RecipeShape.Process.SMITH;
                if (!add(slots, smithing.getTemplate()) || !add(slots, smithing.getBase())
                        || !add(slots, smithing.getAddition())) {
                    return null;
                }
            }
            case TransmuteRecipe transmute -> {
                if (!add(slots, transmute.getInput()) || !add(slots, transmute.getMaterial())) {
                    return null;
                }
            }
            default -> {
                return null;
            }
        }
        return slots.isEmpty() ? null : new RecipeShape(result, amount, slots, process);
    }

    /** False for a choice pricing cannot read, which drops the whole recipe. */
    private static boolean add(List<List<String>> slots, RecipeChoice choice) {
        if (choice == null) {
            return true;
        }
        if (choice instanceof RecipeChoice.MaterialChoice materials) {
            List<String> names = materials.getChoices().stream().filter(material -> !material.isAir())
                    .map(Material::name).toList();
            if (names.isEmpty()) {
                return false;
            }
            slots.add(names);
            return true;
        }
        // Paper hands smithing ingredients over as item types, not as materials.
        if (choice instanceof RecipeChoice.ItemTypeChoice types) {
            List<String> names;
            try {
                // Resolved rather than read: netherite's ingot slot is a tag (#netherite_tool_materials), whose
                // members only the registry knows.
                names = materialNames(types.itemTypes().resolve(org.bukkit.Registry.ITEM).stream()
                        .map(org.bukkit.Keyed::getKey).toList());
            } catch (RuntimeException unreadable) {
                return false;
            }
            if (names.isEmpty()) {
                return false;
            }
            slots.add(names);
            return true;
        }
        return false;
    }

    /**
     * What tools, water and time make, as recipes the server's list does not have: an axe strips a log, a shovel
     * makes a path, a hoe farmland, water turns dirt to mud and powder to concrete, and copper ages. Each costs
     * what it is made from. Without these, nothing made of them — hanging signs, for one — had a price.
     *
     * @param materials every item name on the server; only pairs that both exist become recipes
     */
    public static List<RecipeShape> inWorld(java.util.Collection<String> materials) {
        java.util.Set<String> known = new java.util.HashSet<>(materials);
        List<RecipeShape> shapes = new ArrayList<>();
        java.util.function.BiConsumer<String, List<String>> add = (result, from) -> {
            List<String> present = from.stream().filter(known::contains).toList();
            if (known.contains(result) && !present.isEmpty()) {
                shapes.add(new RecipeShape(result, 1, List.of(present), RecipeShape.Process.WORLD));
            }
        };
        // Several ingredients each, or fewer than one made: a fish in a bucket, an anvil worn down to half.
        java.util.function.BiConsumer<String, List<String>> addAll = (result, from) -> {
            if (known.contains(result) && known.containsAll(from)) {
                shapes.add(new RecipeShape(result, 1, from.stream().map(List::of).toList(), RecipeShape.Process.WORLD));
            }
        };
        java.util.function.BiConsumer<String, Integer> wornAnvil = (result, parts) -> {
            if (known.contains(result) && known.contains("ANVIL")) {
                shapes.add(new RecipeShape(result, parts, List.of(List.of("ANVIL")), RecipeShape.Process.WORLD));
            }
        };
        // Water, lava, milk and snow are free; a bucket of them is worth the bucket, so filling one never pays.
        for (String free : List.of("WATER_BUCKET", "LAVA_BUCKET", "MILK_BUCKET", "POWDER_SNOW_BUCKET",
                "AXOLOTL_BUCKET", "TADPOLE_BUCKET", "SULFUR_CUBE_BUCKET")) {
            add.accept(free, List.of("BUCKET"));
        }
        for (String fish : List.of("COD", "SALMON", "PUFFERFISH", "TROPICAL_FISH")) {
            addAll.accept(fish + "_BUCKET", List.of("BUCKET", fish));
        }
        // Mined whole with Silk Touch: worth one drop, never what Fortune would make of it.
        Map<String, String> drops = Map.of("COAL", "COAL", "IRON", "RAW_IRON", "COPPER", "RAW_COPPER",
                "GOLD", "RAW_GOLD", "DIAMOND", "DIAMOND", "EMERALD", "EMERALD", "LAPIS", "LAPIS_LAZULI",
                "REDSTONE", "REDSTONE");
        drops.forEach((ore, drop) -> {
            add.accept(ore + "_ORE", List.of(drop));
            add.accept("DEEPSLATE_" + ore + "_ORE", List.of(drop));
        });
        // Not its nuggets: it smelts into an ingot, and an ore worth less than that would drag gold down with it.
        add.accept("NETHER_GOLD_ORE", List.of("GOLD_INGOT"));
        add.accept("NETHER_QUARTZ_ORE", List.of("QUARTZ"));
        wornAnvil.accept("CHIPPED_ANVIL", 2);
        wornAnvil.accept("DAMAGED_ANVIL", 4);
        add.accept("CARVED_PUMPKIN", List.of("PUMPKIN"));
        add.accept("WRITTEN_BOOK", List.of("WRITABLE_BOOK"));
        add.accept("FILLED_MAP", List.of("MAP"));
        add.accept("WET_SPONGE", List.of("SPONGE"));
        add.accept("BROWN_MUSHROOM_BLOCK", List.of("BROWN_MUSHROOM"));
        add.accept("RED_MUSHROOM_BLOCK", List.of("RED_MUSHROOM"));
        add.accept("MUSHROOM_STEM", List.of("BROWN_MUSHROOM", "RED_MUSHROOM"));
        add.accept("TALL_GRASS", List.of("SHORT_GRASS"));
        add.accept("LARGE_FERN", List.of("FERN"));
        add.accept("TALL_DRY_GRASS", List.of("SHORT_DRY_GRASS"));
        add.accept("CLOSED_EYEBLOSSOM", List.of("OPEN_EYEBLOSSOM"));
        addAll.accept("COPPER_GOLEM_STATUE", List.of("COPPER_BLOCK", "CARVED_PUMPKIN"));
        add.accept("DIRT_PATH", List.of("DIRT", "GRASS_BLOCK"));
        add.accept("FARMLAND", List.of("DIRT", "GRASS_BLOCK"));
        add.accept("MUD", List.of("DIRT"));
        for (String name : materials) {
            if (name.startsWith("DEAD_") && name.contains("CORAL")) {
                add.accept(name, List.of(name.substring("DEAD_".length())));
            } else if (name.startsWith("INFESTED_")) {
                add.accept(name, List.of(name.substring("INFESTED_".length())));
            } else if (name.startsWith("WAXED_") && name.endsWith("COPPER_GOLEM_STATUE")) {
                addAll.accept(name, List.of(name.substring("WAXED_".length()), "HONEYCOMB"));
            }
            if (name.startsWith("STRIPPED_")) {
                add.accept(name, List.of(name.substring("STRIPPED_".length())));
            } else if (name.endsWith("_CONCRETE")) {
                add.accept(name, List.of(name + "_POWDER"));
            } else if (name.startsWith("EXPOSED_")) {
                String plain = name.substring("EXPOSED_".length());
                add.accept(name, List.of(known.contains(plain) ? plain : plain + "_BLOCK"));
            } else if (name.startsWith("WEATHERED_")) {
                add.accept(name, List.of("EXPOSED_" + name.substring("WEATHERED_".length())));
            } else if (name.startsWith("OXIDIZED_")) {
                add.accept(name, List.of("WEATHERED_" + name.substring("OXIDIZED_".length())));
            }
        }
        return shapes;
    }

    /** Vanilla item keys as material names; anything from another namespace is left out. */
    static List<String> materialNames(Iterable<? extends Key> keys) {
        List<String> names = new ArrayList<>();
        for (Key key : keys) {
            if (key.namespace().equals(Key.MINECRAFT_NAMESPACE)) {
                names.add(key.value().toUpperCase(java.util.Locale.ROOT));
            }
        }
        return names;
    }
}
