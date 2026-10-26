package de.raindancer.modules.economy.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatueSpotTest {

    @Test
    @DisplayName("a statue's spot is read from the settings and written back the same way, its key with it")
    void roundTrip() {
        StatueSpot spot = StatueSpot.parse("casino 10.5 64 -3.25 45 richest 2 -").orElseThrow();
        assertThat(spot.world()).isEqualTo("casino");
        assertThat(spot.yaw()).isEqualTo(45f);
        assertThat(spot.board()).isEqualTo(Podium.Board.RICHEST);
        assertThat(spot.place()).isEqualTo(2);
        assertThat(spot.standalone()).isTrue();
        StatueSpot again = StatueSpot.parse(spot.written()).orElseThrow();
        assertThat(again).isEqualTo(spot);
        assertThat(again.key()).isEqualTo(spot.key()).doesNotContain("#");
        assertThat(StatueSpot.parse("w 0 0 0 0 casino 1 a1b2c3d4").orElseThrow().standalone()).isFalse();
    }

    @Test
    @DisplayName("broken lines and places off the podium are skipped rather than guessed at")
    void broken() {
        assertThat(StatueSpot.parse("w 1 2 3 0 casino 4 -")).isEmpty();
        assertThat(StatueSpot.parse("w 1 2 3 0 casino 0 -")).isEmpty();
        assertThat(StatueSpot.parse("w 1 2 3 0 jackpot 1 -")).isEmpty();
        assertThat(StatueSpot.parse("w a 2 3 0 casino 1 -")).isEmpty();
        assertThat(StatueSpot.parse("w 1 2 3 0 casino 1")).isEmpty();
        assertThat(StatueSpot.parse(null)).isEmpty();
    }

    @Test
    @DisplayName("a place is typed as 1, 2 or 3, with or without a #")
    void places() {
        assertThat(StatueSpot.readPlace("1")).contains(1);
        assertThat(StatueSpot.readPlace("#3")).contains(3);
        assertThat(StatueSpot.readPlace("4")).isEmpty();
        assertThat(StatueSpot.readPlace("first")).isEmpty();
        assertThat(StatueSpot.readPlace(null)).isEmpty();
    }

    @Test
    @DisplayName("moved somewhere else a statue faces the new way and stands on its own")
    void moved() {
        StatueSpot spot = new StatueSpot("w", 0.5, 64, 0.5, 0, Podium.Board.CASINO, 1, "a1b2c3d4");
        StatueSpot there = spot.at("nether", 10.5, 70, -2.5, 90);
        assertThat(there).isEqualTo(new StatueSpot("nether", 10.5, 70, -2.5, 90, Podium.Board.CASINO, 1, "-"));
    }
}
