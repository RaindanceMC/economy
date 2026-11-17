package de.raindancer.modules.economy.rules;

import de.raindancer.core.social.economy.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RainsBookPriceRuleTest {

    private final RainsBookPriceRule rule = new RainsBookPriceRule(new Money(25_000));

    @Test
    @DisplayName("a book costs the money per level, times its level, and nothing but money")
    void money() {
        assertThat(rule.money(1)).isEqualTo(new Money(25_000));
        assertThat(rule.money(3)).isEqualTo(new Money(75_000));
    }

    @Test
    @DisplayName("level 0 is priced as level 1, never as free")
    void neverFree() {
        assertThat(rule.money(0)).isEqualTo(new Money(25_000));
    }
}
