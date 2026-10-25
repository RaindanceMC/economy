package de.raindancer.modules.economy.rules;

import de.raindancer.modules.economy.model.Podium;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class StatueRuleTest {

    private final StatueRule rule = new StatueRule();

    @Test
    @DisplayName("the winner stands highest in the middle, second and third lower beside")
    void heights() {
        assertThat(rule.height(1)).isGreaterThan(rule.height(2));
        assertThat(rule.height(2)).isGreaterThan(rule.height(3));
        assertThat(rule.offset(1, 0f)[0]).isCloseTo(0, within(1e-9));
        assertThat(rule.offset(1, 0f)[2]).isCloseTo(0, within(1e-9));
    }

    @Test
    @DisplayName("seen from the crowd it reads 2 · 1 · 3: second on the statues' right hand")
    void sides() {
        // Facing south (yaw 0) the right hand points west, -x; the crowd, facing north, sees it on its left.
        double[] second = rule.offset(2, 0f);
        double[] third = rule.offset(3, 0f);
        assertThat(second[0]).isNegative();
        assertThat(third[0]).isPositive();
        assertThat(second[2]).isCloseTo(0, within(1e-9));
        // Facing west (yaw 90) the right hand points north, -z.
        assertThat(rule.offset(2, 90f)[2]).isNegative();
        assertThat(rule.offset(2, 90f)[0]).isCloseTo(0, within(1e-9));
        assertThat(rule.offset(2, 0f)[1]).isEqualTo(rule.height(2));
    }

    @Test
    @DisplayName("a statue is named with its medal colour, an empty place says so, names cannot inject tags")
    void titles() {
        assertThat(rule.title(1, "Ada")).isEqualTo("<gold><bold>#1</bold> <white>Ada");
        assertThat(rule.title(3, "Cy")).startsWith("<#cd7f32>");
        assertThat(rule.title(2, null)).isEqualTo("<gray><bold>#2</bold> <dark_gray>Nobody yet");
        assertThat(rule.title(1, "<red>x")).endsWith("\\<red>x");
    }

    @Test
    @DisplayName("each board says what it ranks")
    void headings() {
        assertThat(rule.heading(Podium.Board.CASINO)).contains("casino");
        assertThat(rule.heading(Podium.Board.RICHEST)).contains("Richest");
        assertThat(rule.caption(Podium.Board.CASINO)).isEqualTo("<gray>won ");
        assertThat(rule.caption(Podium.Board.RICHEST)).isEqualTo("<gray>");
        assertThat(rule.heading(Podium.Board.PLAYTIME)).contains("time played");
        assertThat(rule.caption(Podium.Board.PLAYTIME)).isEqualTo("<gray>played ");
    }
}
