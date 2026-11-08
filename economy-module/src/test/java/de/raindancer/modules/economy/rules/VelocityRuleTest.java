package de.raindancer.modules.economy.rules;

import de.raindancer.core.social.economy.Money;
import de.raindancer.modules.economy.model.TransactionKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VelocityRuleTest {

    private final VelocityRule rule = new VelocityRule();
    private final UUID ada = UUID.randomUUID();
    private final Money ceiling = Money.of(50_000_00);

    @Test
    @DisplayName("honest earning under the ceiling says nothing")
    void underTheCeiling() {
        assertThat(rule.record(ada, Money.of(10_000_00), TransactionKind.SELL, 1_000, ceiling)).isEmpty();
        assertThat(rule.record(ada, Money.of(10_000_00), TransactionKind.REWARD, 2_000, ceiling)).isEmpty();
    }

    @Test
    @DisplayName("gains within the window add up and pass the ceiling once")
    void gainsAddUp() {
        rule.record(ada, Money.of(30_000_00), TransactionKind.SELL, 1_000, ceiling);
        assertThat(rule.record(ada, Money.of(25_000_00), TransactionKind.PAY, 2_000, ceiling))
                .contains(Money.of(55_000_00));
        // Said once: the next gain in the quiet spell is not a second line.
        assertThat(rule.record(ada, Money.of(25_000_00), TransactionKind.SELL, 3_000, ceiling)).isEmpty();
        // After the quiet spell, gains past the ceiling are worth a word again.
        assertThat(rule.record(ada, Money.of(60_000_00), TransactionKind.SELL, 3_000 + VelocityRule.QUIET_MILLIS, ceiling))
                .isPresent();
    }

    @Test
    @DisplayName("gains older than the window fall out")
    void theWindowSlides() {
        rule.record(ada, Money.of(40_000_00), TransactionKind.SELL, 1_000, ceiling);
        assertThat(rule.record(ada, Money.of(40_000_00), TransactionKind.SELL, 1_000 + VelocityRule.WINDOW_MILLIS + 1, ceiling))
                .isEmpty();
    }

    @Test
    @DisplayName("money moved about is not money made")
    void movingIsNotEarning() {
        assertThat(rule.record(ada, Money.of(99_000_00), TransactionKind.DEPOSIT, 1_000, ceiling)).isEmpty();
        assertThat(rule.record(ada, Money.of(99_000_00), TransactionKind.LOAN, 1_000, ceiling)).isEmpty();
        assertThat(rule.record(ada, Money.of(-99_000_00), TransactionKind.SELL, 1_000, ceiling)).isEmpty();
        assertThat(rule.record(ada, Money.of(99_000_00), TransactionKind.SELL, 1_000, Money.ZERO)).isEmpty();
    }
}
