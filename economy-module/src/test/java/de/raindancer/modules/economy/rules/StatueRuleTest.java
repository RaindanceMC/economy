package de.raindancer.modules.economy.rules;

import de.raindancer.modules.economy.model.Podium;
import de.raindancer.modules.economy.model.StatueSpot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

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

    @Test
    @DisplayName("a whole podium is three statues in one group: 1 in the middle on the ground it was placed on, 2 and 3 beside")
    void podium() {
        List<StatueSpot> three = rule.podium("w", 0.5, 64, 0.5, 0f, Podium.Board.RICHEST, "a1b2c3d4");
        assertThat(three).extracting(StatueSpot::place).containsExactly(1, 2, 3);
        assertThat(three).allSatisfy(spot -> {
            assertThat(spot.group()).isEqualTo("a1b2c3d4");
            assertThat(spot.y()).isEqualTo(64);
            assertThat(spot.yaw()).isEqualTo(0f);
            assertThat(spot.board()).isEqualTo(Podium.Board.RICHEST);
        });
        assertThat(three.get(0).x()).isCloseTo(0.5, within(1e-9));
        assertThat(three.get(1).x()).isCloseTo(-0.5, within(1e-9));
        assertThat(three.get(2).x()).isCloseTo(1.5, within(1e-9));
    }

    @Test
    @DisplayName("podiums placed before single statues existed are read as three statues where theirs stood")
    void legacy() {
        List<String> lines = List.of("casino 10.50 64.00 -3.25 90.0 casino", "w 1.00 2.00 3.00 0.0 richest 2 -");
        assertThat(rule.hasLegacy(lines)).isTrue();
        List<StatueSpot> read = rule.read(lines);
        assertThat(read).hasSize(4);
        List<StatueSpot> podium = read.subList(0, 3);
        assertThat(podium).extracting(StatueSpot::place).containsExactly(1, 2, 3);
        assertThat(podium).extracting(StatueSpot::group).doesNotContain("-").containsOnly(podium.get(0).group());
        // Facing west (yaw 90) second stands north of the winner, -z.
        assertThat(podium.get(0).z()).isCloseTo(-3.25, within(1e-9));
        assertThat(podium.get(1).z()).isCloseTo(-4.25, within(1e-9));
        assertThat(podium.get(1).x()).isCloseTo(10.5, within(1e-9));
        assertThat(podium).allSatisfy(spot -> assertThat(spot.board()).isEqualTo(Podium.Board.CASINO));

        // Written back, the same statues come out again — reading twice never moves or regroups anything.
        List<String> migrated = rule.migrate(lines);
        assertThat(rule.hasLegacy(migrated)).isFalse();
        assertThat(migrated).hasSize(4);
        assertThat(rule.read(migrated)).isEqualTo(read);
        assertThat(rule.read(lines)).isEqualTo(read);
        assertThat(rule.migrate(List.of("nonsense"))).containsExactly("nonsense");
    }

    @Test
    @DisplayName("a statue standing alone is labelled; of a podium only its highest place still standing is")
    void labels() {
        StatueSpot alone = StatueSpot.parse("w 0 64 0 0 casino 3 -").orElseThrow();
        List<StatueSpot> podium = rule.podium("w", 20.5, 64, 0.5, 0f, Podium.Board.RICHEST, "g1");
        List<StatueSpot> all = new java.util.ArrayList<>(podium);
        all.add(alone);
        assertThat(rule.labelled(alone, all)).isTrue();
        assertThat(rule.labelled(podium.get(0), all)).isTrue();
        assertThat(rule.labelled(podium.get(1), all)).isFalse();
        all.remove(podium.get(0));
        assertThat(rule.labelled(podium.get(1), all)).isTrue();
        assertThat(rule.labelled(podium.get(2), all)).isFalse();
    }

    @Test
    @DisplayName("statues face the nearest step of the way the placer looks, a podium the nearest quarter")
    void facing() {
        assertThat(rule.facing(100f, 90f)).isEqualTo(90f);
        assertThat(rule.facing(-100f, 90f)).isEqualTo(270f);
        assertThat(rule.facing(30f, 45f)).isEqualTo(45f);
        assertThat(rule.facing(359f, 45f)).isEqualTo(0f);
    }

    @Test
    @DisplayName("the nearest statue within reach is found, optionally only of one board and place")
    void nearest() {
        StatueSpot near = StatueSpot.parse("w 1 64 1 0 casino 1 -").orElseThrow();
        StatueSpot far = StatueSpot.parse("w 4 64 0 0 richest 2 -").orElseThrow();
        StatueSpot elsewhere = StatueSpot.parse("nether 0 64 0 0 richest 2 -").orElseThrow();
        List<StatueSpot> all = List.of(near, far, elsewhere);
        assertThat(rule.nearest(all, "w", 0, 64, 0, 25, spot -> true)).contains(near);
        assertThat(rule.nearest(all, "w", 0, 64, 0, 25, spot -> spot.place() == 2)).contains(far);
        assertThat(rule.nearest(all, "w", 0, 64, 0, 4, spot -> spot.place() == 2)).isEmpty();
        assertThat(rule.nearest(all, "end", 0, 64, 0, Double.MAX_VALUE, spot -> true)).isEmpty();
    }
}
