package de.raindancer.modules.economy.rules;

import de.raindancer.core.social.economy.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShardPriceRuleTest {

    private final ShardPriceRule rule = new ShardPriceRule(List.of("8", "16", "32"), new Money(25_000));

    @Test
    @DisplayName("a book takes the level's shards, and past the list each level doubles the last")
    void shards() {
        assertThat(rule.shards(1)).isEqualTo(8);
        assertThat(rule.shards(3)).isEqualTo(32);
        assertThat(rule.shards(4)).isEqualTo(64);
        assertThat(rule.shards(5)).isEqualTo(128);
        assertThat(rule.shards(0)).as("no level, no book").isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("and the money per level on top")
    void money() {
        assertThat(rule.money(1)).isEqualTo(new Money(25_000));
        assertThat(rule.money(3)).isEqualTo(new Money(75_000));
    }

    @Test
    @DisplayName("a mangled list falls back to the built-in steps rather than giving books away")
    void mangled() {
        ShardPriceRule broken = new ShardPriceRule(List.of("eight", "-3"), new Money(1));
        assertThat(broken.shards(1)).isEqualTo(8);
        assertThat(broken.shards(2)).isEqualTo(16);
    }

    @Test
    @DisplayName("no level costs more than two books of the level below — those make it in an anvil")
    void neverDearerThanCombining() {
        for (ShardPriceRule each : List.of(new ShardPriceRule(List.of(), new Money(1)),
                new ShardPriceRule(List.of("8", "24", "64", "128", "256"), new Money(1)),
                new ShardPriceRule(List.of("5", "100", "7"), new Money(1)))) {
            for (int level = 2; level <= 8; level++) {
                assertThat(each.shards(level)).as("level %d", level).isLessThanOrEqualTo(2 * each.shards(level - 1));
            }
        }
    }

    @Test
    @DisplayName("an old, steeper list is capped at doubling; a gentler one is left as written")
    void capped() {
        ShardPriceRule old = new ShardPriceRule(List.of("8", "24", "64", "128", "256"), new Money(1));
        assertThat(List.of(old.shards(1), old.shards(2), old.shards(3), old.shards(4), old.shards(5)))
                .containsExactly(8, 16, 32, 64, 128);
        ShardPriceRule gentle = new ShardPriceRule(List.of("8", "12", "20"), new Money(1));
        assertThat(List.of(gentle.shards(1), gentle.shards(2), gentle.shards(3), gentle.shards(4)))
                .containsExactly(8, 12, 20, 40);
    }
}
