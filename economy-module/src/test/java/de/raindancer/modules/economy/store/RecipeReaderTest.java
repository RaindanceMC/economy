package de.raindancer.modules.economy.store;

import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeReaderTest {

    @Test
    @DisplayName("item types named by key read as material names — how Paper hands over smithing ingredients")
    void itemTypeKeys() {
        assertThat(RecipeReader.materialNames(List.of(Key.key("minecraft:diamond_sword"),
                Key.key("minecraft:netherite_upgrade_smithing_template"))))
                .containsExactly("DIAMOND_SWORD", "NETHERITE_UPGRADE_SMITHING_TEMPLATE");
        assertThat(RecipeReader.materialNames(List.of(Key.key("somepack:ruby")))).as("not a vanilla item").isEmpty();
    }

    @Test
    @DisplayName("stripping is a recipe: every stripped block comes from its plain one, nothing else is invented")
    void stripping() {
        var shapes = RecipeReader.inWorld(List.of("OAK_LOG", "STRIPPED_OAK_LOG", "CRIMSON_STEM",
                "STRIPPED_CRIMSON_STEM", "STRIPPED_NOTHING", "DIRT"));
        assertThat(shapes).extracting(de.raindancer.modules.economy.model.RecipeShape::result)
                .containsExactly("STRIPPED_OAK_LOG", "STRIPPED_CRIMSON_STEM");
        assertThat(shapes.getFirst().slots()).containsExactly(List.of("OAK_LOG"));
        assertThat(shapes.getFirst().process()).isEqualTo(de.raindancer.modules.economy.model.RecipeShape.Process.WORLD);
    }

    @Test
    @DisplayName("what tools, water and time make: paths, farmland, mud, concrete, aged copper")
    void madeInTheWorld() {
        var shapes = RecipeReader.inWorld(List.of("DIRT", "GRASS_BLOCK", "DIRT_PATH", "FARMLAND", "MUD",
                "RED_CONCRETE_POWDER", "RED_CONCRETE", "COPPER_BLOCK", "EXPOSED_COPPER", "WEATHERED_COPPER",
                "OXIDIZED_COPPER", "CUT_COPPER", "EXPOSED_CUT_COPPER"));
        java.util.Map<String, List<List<String>>> by = new java.util.HashMap<>();
        shapes.forEach(shape -> by.put(shape.result(), shape.slots()));
        assertThat(by.get("DIRT_PATH")).containsExactly(List.of("DIRT", "GRASS_BLOCK"));
        assertThat(by.get("FARMLAND")).containsExactly(List.of("DIRT", "GRASS_BLOCK"));
        assertThat(by.get("MUD")).containsExactly(List.of("DIRT"));
        assertThat(by.get("RED_CONCRETE")).containsExactly(List.of("RED_CONCRETE_POWDER"));
        assertThat(by.get("EXPOSED_COPPER")).as("plain copper is a block").containsExactly(List.of("COPPER_BLOCK"));
        assertThat(by.get("WEATHERED_COPPER")).containsExactly(List.of("EXPOSED_COPPER"));
        assertThat(by.get("OXIDIZED_COPPER")).containsExactly(List.of("WEATHERED_COPPER"));
        assertThat(by.get("EXPOSED_CUT_COPPER")).containsExactly(List.of("CUT_COPPER"));
        assertThat(by).doesNotContainKey("DIRT");
    }

    @Test
    @DisplayName("what is filled, mined whole, left to die or worn down: buckets, ores, coral, infested stone, anvils")
    void gatheredWhole() {
        var shapes = RecipeReader.inWorld(List.of("BUCKET", "WATER_BUCKET", "MILK_BUCKET", "COD", "COD_BUCKET",
                "RAW_IRON", "IRON_ORE", "DEEPSLATE_IRON_ORE", "GOLD_INGOT", "NETHER_GOLD_ORE", "TUBE_CORAL",
                "DEAD_TUBE_CORAL", "STONE", "INFESTED_STONE", "ANVIL", "CHIPPED_ANVIL", "DAMAGED_ANVIL", "PUMPKIN",
                "CARVED_PUMPKIN", "WRITABLE_BOOK", "WRITTEN_BOOK", "COPPER_GOLEM_STATUE", "WAXED_COPPER_GOLEM_STATUE",
                "HONEYCOMB"));
        java.util.Map<String, de.raindancer.modules.economy.model.RecipeShape> by = new java.util.HashMap<>();
        shapes.forEach(shape -> by.put(shape.result(), shape));
        assertThat(by.get("WATER_BUCKET").slots()).as("water is free: a full bucket is worth the bucket")
                .containsExactly(List.of("BUCKET"));
        assertThat(by.get("MILK_BUCKET").slots()).containsExactly(List.of("BUCKET"));
        assertThat(by.get("COD_BUCKET").slots()).containsExactly(List.of("BUCKET"), List.of("COD"));
        assertThat(by.get("IRON_ORE").slots()).as("worth one drop — never what Fortune makes of it")
                .containsExactly(List.of("RAW_IRON"));
        assertThat(by.get("DEEPSLATE_IRON_ORE").slots()).containsExactly(List.of("RAW_IRON"));
        assertThat(by.get("NETHER_GOLD_ORE").slots()).as("what smelting it makes, so it never prices gold down")
                .containsExactly(List.of("GOLD_INGOT"));
        assertThat(by.get("DEAD_TUBE_CORAL").slots()).containsExactly(List.of("TUBE_CORAL"));
        assertThat(by.get("INFESTED_STONE").slots()).containsExactly(List.of("STONE"));
        assertThat(by.get("CHIPPED_ANVIL").amount()).as("half an anvil").isEqualTo(2);
        assertThat(by.get("DAMAGED_ANVIL").amount()).as("a quarter").isEqualTo(4);
        assertThat(by.get("CARVED_PUMPKIN").slots()).containsExactly(List.of("PUMPKIN"));
        assertThat(by.get("WRITTEN_BOOK").slots()).containsExactly(List.of("WRITABLE_BOOK"));
        assertThat(by.get("WAXED_COPPER_GOLEM_STATUE").slots())
                .containsExactly(List.of("COPPER_GOLEM_STATUE"), List.of("HONEYCOMB"));
        assertThat(by).as("nothing to fill a bucket with that is not there").doesNotContainKey("SALMON_BUCKET");
    }
}
