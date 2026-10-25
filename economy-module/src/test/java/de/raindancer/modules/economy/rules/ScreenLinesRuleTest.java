package de.raindancer.modules.economy.rules;

import de.raindancer.core.social.economy.Money;
import de.raindancer.modules.economy.model.Auction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("What the economy's screens say")
class ScreenLinesRuleTest {

    private final ScreenLinesRule rule = new ScreenLinesRule();

    private static Auction ending(String item, long endsAt) {
        return new Auction(UUID.randomUUID(), UUID.randomUUID(), "Ada", new byte[0], item, Money.of(1), Money.ZERO,
                Money.ZERO, null, "", 0, 60, 0, endsAt);
    }

    @Test
    @DisplayName("auctions ending soonest first; ended ones are left out")
    void auctions() {
        List<Auction> all = List.of(ending("Late", 9_000), ending("Over", 500), ending("Soon", 2_000));
        assertThat(rule.endingSoonest(all, 1_000, 5)).extracting(Auction::itemName).containsExactly("Soon", "Late");
        assertThat(rule.endingSoonest(all, 1_000, 1)).extracting(Auction::itemName).containsExactly("Soon");
    }

    @Test
    @DisplayName("the casino record says won, lost or even")
    void casino() {
        assertThat(rule.casinoWord(Money.of(5))).isEqualTo("won");
        assertThat(rule.casinoWord(Money.of(-5))).isEqualTo("lost");
        assertThat(rule.casinoWord(Money.ZERO)).isEqualTo("even");
    }
}
