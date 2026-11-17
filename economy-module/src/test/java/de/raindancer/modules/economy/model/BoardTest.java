package de.raindancer.modules.economy.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BoardTest {

    @Test
    @DisplayName("a board is named loosely when typed, and is Core's statue board of the same name")
    void boards() {
        assertThat(Board.read("Richest")).contains(Board.RICHEST);
        assertThat(Board.read("CASINO")).contains(Board.CASINO);
        assertThat(Board.read("playtime")).contains(Board.PLAYTIME);
        assertThat(Board.read("jackpot")).isEmpty();
        assertThat(Board.read(null)).isEmpty();
        assertThat(Board.RICHEST.id()).isEqualTo("richest");
    }

    @Test
    @DisplayName("Prime and creators are thanked, the others ranked; each list has a heading")
    void kinds() {
        assertThat(Board.PRIME.thankYou()).isTrue();
        assertThat(Board.CREATOR.thankYou()).isTrue();
        assertThat(Board.RICHEST.thankYou()).isFalse();
        for (Board board : Board.values()) {
            assertThat(board.heading()).isNotBlank();
        }
        assertThat(Board.CASINO.heading()).contains("casino");
    }
}
