package de.raindancer.modules.economy.rules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackPriceRuleTest {

    private final FallbackPriceRule rule = new FallbackPriceRule();

    @Test
    @DisplayName("the rarer the item, the more it is worth; an unknown rarity counts as common")
    void byRarity() {
        assertThat(rule.coins("COMMON")).isPositive();
        assertThat(rule.coins("UNCOMMON")).isGreaterThan(rule.coins("COMMON"));
        assertThat(rule.coins("RARE")).isGreaterThan(rule.coins("UNCOMMON"));
        assertThat(rule.coins("EPIC")).isGreaterThan(rule.coins("RARE"));
        assertThat(rule.coins("legendary")).isEqualTo(rule.coins("COMMON"));
        assertThat(rule.coins(null)).isEqualTo(rule.coins("COMMON"));
    }

    @Test
    @DisplayName("sell-only names match exactly or around a star, whatever their case")
    void sellOnly() {
        SellOnlyRule only = new SellOnlyRule(java.util.List.of("*_ore", "infested_*", "elytra", "music_disc_*"));
        assertThat(only.matches("DEEPSLATE_IRON_ORE")).isTrue();
        assertThat(only.matches("INFESTED_STONE")).isTrue();
        assertThat(only.matches("ELYTRA")).isTrue();
        assertThat(only.matches("MUSIC_DISC_CAT")).isTrue();
        assertThat(only.matches("IRON_INGOT")).isFalse();
        assertThat(only.matches("ORE")).as("a star stands for something").isFalse();
        assertThat(only.matches("ELYTRA_WINGS")).isFalse();
    }
}
