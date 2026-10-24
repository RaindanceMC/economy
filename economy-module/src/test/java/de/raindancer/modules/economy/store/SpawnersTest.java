package de.raindancer.modules.economy.store;

import de.raindancer.core.social.economy.Money;
import de.raindancer.modules.economy.EconomySettings;
import de.raindancer.modules.economy.EconomySettingsTest;
import de.raindancer.modules.economy.model.PriceTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SpawnersTest {

    private PriceBook book(EconomySettings settings) {
        byte[] raw = "diamond: 10000\n".getBytes(StandardCharsets.UTF_8);
        PriceBook book = new PriceBook(currency -> BasePrices.parse(new ByteArrayInputStream(raw), currency),
                material -> -1000.0, material -> 64, List.of(), List.of());
        book.recompute(settings, List.of(), List.of(), List.of(), List.of(), Map.of("SPAWNER", "EPIC"));
        return book;
    }

    @Test
    @DisplayName("by default the shop does not sell spawners")
    void notSoldByDefault() {
        assertThat(book(EconomySettings.DEFAULTS).tag("SPAWNER").buyable()).isFalse();
    }

    @Test
    @DisplayName("switched on, a spawner costs exactly its price: 30 million, no markup, no supply and demand")
    void soldAtItsPrice() {
        PriceBook book = book(EconomySettingsTest.with("shop.sell-spawners", "true"));
        PriceTag spawner = book.tag("SPAWNER");
        assertThat(spawner.buyable()).isTrue();
        assertThat(spawner.buy()).isEqualTo(Money.of(30_000_000));
        assertThat(book.multiplier("SPAWNER")).as("not moved by the flood every other item is in").isEqualTo(1.0);
    }

    @Test
    @DisplayName("the price is a setting of its own, and an owner's own buy price still wins")
    void ownPrice() {
        assertThat(book(EconomySettingsTest.with("shop.sell-spawners", "true", "shop.spawner-price", "5000000"))
                .tag("SPAWNER").buy()).isEqualTo(Money.of(5_000_000));
        assertThat(book(EconomySettingsTest.with("shop.sell-spawners", "true", "shop.buy-prices", "spawner 100"))
                .tag("SPAWNER").buy()).isEqualTo(Money.of(100));
    }

    @Test
    @DisplayName("spawners are not rare items: shop.sell-rare does not put them on sale")
    void notRare() {
        assertThat(book(EconomySettingsTest.with("shop.sell-rare", "true")).tag("SPAWNER").buyable()).isFalse();
    }
}
