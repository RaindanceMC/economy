package de.raindancer.modules.economy.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Where a leaderboard floats, and what it lists")
class SpotTest {

    @Test
    @DisplayName("a leaderboard written before boards existed lists the richest")
    void old() {
        Spot spot = Spot.parse("world 10.50 70.00 -3.50").orElseThrow();
        assertThat(spot.board()).isEqualTo(Podium.Board.RICHEST);
        assertThat(spot.x()).isEqualTo(10.5);
    }

    @Test
    @DisplayName("written and read back with its board")
    void roundTrip() {
        Spot spot = new Spot("world", 1.25, 64, -2, Podium.Board.PLAYTIME);
        assertThat(Spot.parse(spot.written())).contains(spot);
    }

    @Test
    @DisplayName("nonsense is nothing")
    void broken() {
        assertThat(Spot.parse("world 1 2")).isEmpty();
        assertThat(Spot.parse("world 1 2 3 jackpot")).isEmpty();
        assertThat(Spot.parse("world a 2 3")).isEmpty();
        assertThat(Spot.parse(null)).isEmpty();
    }
}
