package de.raindancer.modules.economy.rules;

import de.raindancer.core.social.economy.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CashSplitRuleTest {

    private final CashSplitRule rule = new CashSplitRule();

    @Test
    @DisplayName("an amount within the coin limit comes out as coins alone")
    void coinsOnly() {
        CashSplitRule.Split split = rule.split(Money.of(150), 192, true);
        assertThat(split.coins()).isEqualTo(150);
        assertThat(split.cheque()).isEqualTo(Money.ZERO);
        assertThat(split.kept()).isEqualTo(Money.ZERO);
    }

    @Test
    @DisplayName("more than the limit: the limit in coins, the rest on one cheque")
    void coinsAndCheque() {
        CashSplitRule.Split split = rule.split(Money.of(20_000), 192, true);
        assertThat(split.coins()).isEqualTo(192);
        assertThat(split.cheque()).isEqualTo(Money.of(19_808));
        assertThat(split.kept()).isEqualTo(Money.ZERO);
    }

    @Test
    @DisplayName("with cheques off, the rest stays on the balance")
    void chequesOff() {
        CashSplitRule.Split split = rule.split(Money.of(20_000), 192, false);
        assertThat(split.coins()).isEqualTo(192);
        assertThat(split.cheque()).isEqualTo(Money.ZERO);
        assertThat(split.kept()).isEqualTo(Money.of(19_808));
        assertThat(split.taken()).isEqualTo(Money.of(192));
    }

    @Test
    @DisplayName("nothing, a negative amount or no coins allowed take nothing out as coins")
    void edges() {
        assertThat(rule.split(Money.ZERO, 192, true).taken()).isEqualTo(Money.ZERO);
        assertThat(rule.split(Money.of(-5), 192, true).taken()).isEqualTo(Money.ZERO);
        CashSplitRule.Split noCoins = rule.split(Money.of(50), 0, true);
        assertThat(noCoins.coins()).isZero();
        assertThat(noCoins.cheque()).isEqualTo(Money.of(50));
    }
}
