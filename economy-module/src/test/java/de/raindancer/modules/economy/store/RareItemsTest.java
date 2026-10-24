package de.raindancer.modules.economy.store;

import de.raindancer.modules.economy.EconomyModule;
import de.raindancer.modules.economy.EconomySettings;
import de.raindancer.modules.economy.EconomySettingsTest;
import de.raindancer.modules.economy.model.RecipeShape;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RareItemsTest {

    private static final List<RecipeShape> BEACON = List.of(new RecipeShape("BEACON", 1, List.of(
            List.of("NETHER_STAR"), List.of("OBSIDIAN"), List.of("GLASS")), RecipeShape.Process.CRAFT));

    private PriceBook book(EconomySettings settings) {
        byte[] raw = """
                nether_star: 50000
                obsidian: 50
                glass: 10
                """.getBytes(StandardCharsets.UTF_8);
        PriceBook book = new PriceBook(currency -> BasePrices.parse(new ByteArrayInputStream(raw), currency),
                material -> 0.0, material -> 64, List.of(), List.of("nether_star", "netherite_*"));
        book.recompute(settings, BEACON);
        return book;
    }

    @Test
    @DisplayName("a rare item is not for sale in the shop, though the shop still buys it")
    void rareNotSold() {
        PriceBook book = book(EconomySettings.DEFAULTS);
        assertThat(book.tag("NETHER_STAR").buyable()).isFalse();
        assertThat(book.tag("NETHER_STAR").sellable()).isTrue();
        assertThat(book.tag("OBSIDIAN").buyable()).as("an ordinary item is").isTrue();
    }

    @Test
    @DisplayName("what is made of a rare item is rare too: no beacon without the nether star")
    void madeOfRare() {
        PriceBook book = book(EconomySettings.DEFAULTS);
        assertThat(book.tag("BEACON").buyable()).isFalse();
        assertThat(book.tag("BEACON").sellable()).isTrue();
    }

    @Test
    @DisplayName("switched on, the shop sells rare items like any other")
    void switchedOn() {
        PriceBook book = book(EconomySettingsTest.with("shop.sell-rare", "true"));
        assertThat(book.tag("NETHER_STAR").buyable()).isTrue();
        assertThat(book.tag("BEACON").buyable()).isTrue();
    }

    @Test
    @DisplayName("an owner's own buy price puts a rare item on sale all the same")
    void ownersBuyPrice() {
        PriceBook book = book(EconomySettingsTest.with("shop.buy-prices", "nether_star 90000"));
        assertThat(book.tag("NETHER_STAR").buyable()).isTrue();
    }

    @Test
    @DisplayName("the shipped list reads, and holds the end-game items")
    void shippedList() {
        List<String> rare = SellOnlyList.parse(EconomyModule.class.getResourceAsStream(SellOnlyList.RARE_RESOURCE));
        assertThat(rare).contains("nether_star", "totem_of_undying", "netherite_*", "enchanted_golden_apple", "elytra",
                "sniffer_egg");
        List<String> sellOnly = SellOnlyList.parse(EconomyModule.class.getResourceAsStream(SellOnlyList.RESOURCE));
        assertThat(rare).as("never sold anyway, so not listed twice").doesNotContainAnyElementsOf(sellOnly);
    }

    @Test
    @DisplayName("an elytra and a sniffer egg are for sale only while rare items are")
    void elytraAndSnifferEgg() {
        List<String> sellOnly = SellOnlyList.parse(EconomyModule.class.getResourceAsStream(SellOnlyList.RESOURCE));
        List<String> rare = SellOnlyList.parse(EconomyModule.class.getResourceAsStream(SellOnlyList.RARE_RESOURCE));
        byte[] raw = "elytra: 50000\nsniffer_egg: 2500\n".getBytes(StandardCharsets.UTF_8);
        java.util.function.Function<EconomySettings, PriceBook> book = settings -> {
            PriceBook made = new PriceBook(currency -> BasePrices.parse(new ByteArrayInputStream(raw), currency),
                    material -> 0.0, material -> 64, sellOnly, rare);
            made.recompute(settings, List.of());
            return made;
        };
        PriceBook off = book.apply(EconomySettings.DEFAULTS);
        PriceBook on = book.apply(EconomySettingsTest.with("shop.sell-rare", "true"));
        for (String item : List.of("ELYTRA", "SNIFFER_EGG")) {
            assertThat(off.tag(item).buyable()).as(item).isFalse();
            assertThat(off.tag(item).sellable()).as(item).isTrue();
            assertThat(on.tag(item).buyable()).as(item).isTrue();
        }
    }
}
