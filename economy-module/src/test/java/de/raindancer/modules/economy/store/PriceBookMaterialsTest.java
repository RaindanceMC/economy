package de.raindancer.modules.economy.store;

import de.raindancer.core.social.economy.Money;
import de.raindancer.modules.economy.EconomySettings;
import de.raindancer.modules.economy.EconomySettingsTest;
import de.raindancer.modules.economy.model.RecipeShape;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PriceBookMaterialsTest {

    private static final Offset<Double> CLOSE = Offset.offset(0.001);

    private final Map<String, Double> pressure = new HashMap<>();

    private static RecipeShape craft(String result, int amount, String... slots) {
        return new RecipeShape(result, amount, java.util.Arrays.stream(slots).map(List::of).toList(),
                RecipeShape.Process.CRAFT);
    }

    private static final List<RecipeShape> RECIPES = List.of(
            craft("FURNACE", 1, "COBBLESTONE", "COBBLESTONE", "COBBLESTONE", "COBBLESTONE",
                    "COBBLESTONE", "COBBLESTONE", "COBBLESTONE", "COBBLESTONE"),
            // Half cobblestone, half iron by value: 4 × 5 and 2 × 10.
            craft("MIXED", 1, "COBBLESTONE", "COBBLESTONE", "COBBLESTONE", "COBBLESTONE", "IRON_INGOT", "IRON_INGOT"),
            craft("BLAST_FURNACE", 1, "FURNACE", "IRON_INGOT"),
            // A cycle: the block from ingots and the ingots back from the block.
            craft("IRON_BLOCK", 1, "IRON_INGOT", "IRON_INGOT", "IRON_INGOT", "IRON_INGOT", "IRON_INGOT",
                    "IRON_INGOT", "IRON_INGOT", "IRON_INGOT", "IRON_INGOT"),
            craft("IRON_INGOT", 9, "IRON_BLOCK"));

    private PriceBook book(EconomySettings settings) {
        byte[] raw = """
                cobblestone: 5
                iron_ingot: 10
                """.getBytes(StandardCharsets.UTF_8);
        PriceBook book = new PriceBook(currency -> BasePrices.parse(new ByteArrayInputStream(raw), currency),
                material -> pressure.getOrDefault(material, 0.0), material -> 64);
        book.recompute(settings, RECIPES);
        return book;
    }

    private PriceBook book(String... settings) {
        return book(settings.length == 0 ? EconomySettings.DEFAULTS : EconomySettingsTest.with(settings));
    }

    @Test
    @DisplayName("a flooded material drags what is crafted from it down with it")
    void floodedMaterial() {
        pressure.put("COBBLESTONE", -1000.0);
        PriceBook book = book();
        assertThat(book.multiplier("COBBLESTONE")).isCloseTo(0.0, CLOSE);
        assertThat(book.multiplier("FURNACE")).isCloseTo(0.0, CLOSE);
        assertThat(book.tag("FURNACE").sellable()).as("worth nothing, as its cobblestone").isFalse();
    }

    @Test
    @DisplayName("a material falling a little takes the crafted thing down by as much")
    void slightFall() {
        pressure.put("COBBLESTONE", -0.5);
        PriceBook book = book();
        double cobble = book.multiplier("COBBLESTONE");
        assertThat(cobble).isLessThan(1);
        assertThat(book.multiplier("FURNACE")).isCloseTo(cobble, CLOSE);
        assertThat(book.tag("FURNACE").sell())
                .isEqualTo(Money.of((long) Math.floor(44 * cobble * 0.4 + 1e-9)));
    }

    @Test
    @DisplayName("each material counts as much as it makes up of the value")
    void weightedByValue() {
        pressure.put("COBBLESTONE", -1000.0);
        PriceBook book = book();
        assertThat(book.multiplier("MIXED")).as("half of it is iron, which holds").isCloseTo(0.5, CLOSE);
    }

    @Test
    @DisplayName("it carries through every step of a chain")
    void chain() {
        pressure.put("COBBLESTONE", -1000.0);
        PriceBook book = book();
        // Blast furnace: a furnace (44) and an ingot (10); the furnace is worth nothing now.
        assertThat(book.multiplier("BLAST_FURNACE")).isCloseTo(10.0 / 54, CLOSE);
    }

    @Test
    @DisplayName("the crafted thing's own trade still counts on top of its materials")
    void ownPressureOnTop() {
        pressure.put("COBBLESTONE", -0.5);
        pressure.put("FURNACE", -0.3);
        PriceBook book = book();
        double cobble = book.multiplier("COBBLESTONE");
        double furnaceAlone = 1 - Math.tanh(0.3);
        assertThat(book.multiplier("FURNACE")).isCloseTo(cobble * furnaceAlone, CLOSE);
    }

    @Test
    @DisplayName("bought-up materials raise it, never past the highest multiplier")
    void cappedAtHighest() {
        pressure.put("COBBLESTONE", 1000.0);
        pressure.put("FURNACE", 1000.0);
        PriceBook book = book();
        assertThat(book.multiplier("FURNACE")).isCloseTo(3.0, CLOSE);
    }

    @Test
    @DisplayName("it can be switched off: then only the item's own trade moves its price")
    void switchedOff() {
        pressure.put("COBBLESTONE", -1000.0);
        PriceBook book = book("shop.follow-materials", "false");
        assertThat(book.multiplier("FURNACE")).isCloseTo(1.0, CLOSE);
    }

    @Test
    @DisplayName("an owner's own price for the crafted thing stays put")
    void ownersPriceStays() {
        pressure.put("COBBLESTONE", -1000.0);
        PriceBook book = book("shop.sell-prices", "furnace 8");
        assertThat(book.multiplier("FURNACE")).isEqualTo(1.0);
        assertThat(book.tag("FURNACE").sell()).isEqualTo(Money.of(8));
    }

    @Test
    @DisplayName("a raw material is not made of anything, and cycles in the recipes settle")
    void rawAndCycles() {
        pressure.put("IRON_INGOT", -0.5);
        PriceBook book = book();
        double ingot = book.multiplier("IRON_INGOT");
        assertThat(ingot).isCloseTo(1 - Math.tanh(0.5), CLOSE);
        assertThat(book.multiplier("IRON_BLOCK")).isCloseTo(ingot, CLOSE);
    }

    @Test
    @DisplayName("a stack of a raw material pushes its price only by the raw share, as everything made of it follows")
    void rawPushesSofter() {
        PriceBook book = book();
        assertThat(book.pushPerStack("COBBLESTONE")).isCloseTo(0.1 * 0.25, CLOSE);
        assertThat(book.pushPerStack("FURNACE")).as("made things push in full").isCloseTo(0.1, CLOSE);
        assertThat(book("shop.raw-pressure-share", "1").pushPerStack("COBBLESTONE")).isCloseTo(0.1, CLOSE);
    }

    @Test
    @DisplayName("without following materials a raw material pushes in full again")
    void rawInFullWithoutFollowing() {
        assertThat(book("shop.follow-materials", "false").pushPerStack("COBBLESTONE")).isCloseTo(0.1, CLOSE);
    }
}
