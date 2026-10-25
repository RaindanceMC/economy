package de.raindancer.modules.economy.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PodiumTest {

    @Test
    @DisplayName("a podium is read from the settings and written back the same way, its key with it")
    void roundTrip() {
        Podium podium = Podium.parse("casino 10.5 64 -3.25 90 casino").orElseThrow();
        assertThat(podium.world()).isEqualTo("casino");
        assertThat(podium.yaw()).isEqualTo(90f);
        assertThat(podium.board()).isEqualTo(Podium.Board.CASINO);
        Podium again = Podium.parse(podium.written()).orElseThrow();
        assertThat(again).isEqualTo(podium);
        assertThat(again.key()).isEqualTo(podium.key());
    }

    @Test
    @DisplayName("broken lines are skipped rather than guessed at")
    void broken() {
        assertThat(Podium.parse("casino 1 2 3 0")).isEmpty();
        assertThat(Podium.parse("casino 1 2 3 0 jackpot")).isEmpty();
        assertThat(Podium.parse("casino a 2 3 0 casino")).isEmpty();
        assertThat(Podium.parse(null)).isEmpty();
    }

    @Test
    @DisplayName("a board is named loosely when typed")
    void boards() {
        assertThat(Podium.Board.read("Richest")).contains(Podium.Board.RICHEST);
        assertThat(Podium.Board.read("CASINO")).contains(Podium.Board.CASINO);
        assertThat(Podium.Board.read("jackpot")).isEmpty();
        assertThat(Podium.Board.read(null)).isEmpty();
    }
}
