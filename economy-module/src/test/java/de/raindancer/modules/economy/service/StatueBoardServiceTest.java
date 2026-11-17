package de.raindancer.modules.economy.service;

import de.raindancer.core.social.economy.Money;
import de.raindancer.core.ui.statue.StatueBoard;
import de.raindancer.core.ui.statue.StatueBoards;
import de.raindancer.modules.economy.EconomySettings;
import de.raindancer.modules.economy.model.Board;
import de.raindancer.modules.economy.model.Standing;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StatueBoardServiceTest {

    private static final UUID ADA = UUID.randomUUID();
    private static final UUID BO = UUID.randomUUID();

    private final Map<Board, List<Standing>> standings = Map.of(
            Board.RICHEST, List.of(new Standing(ADA, "Ada", Money.of(5000)), new Standing(BO, "Bo", Money.of(10))),
            Board.CASINO, List.of(new Standing(BO, "Bo", Money.of(400))),
            Board.PLAYTIME, List.of(Standing.played(ADA, "Ada", 600)));
    private final StatueBoardService service = new StatueBoardService(null,
            board -> standings.getOrDefault(board, List.of()), EconomySettings.DEFAULTS);

    @AfterEach
    void clear() {
        StatueBoards.clear();
    }

    private static String plain(StatueBoard board, StatueBoard.Standing standing) {
        return PlainTextComponentSerializer.plainText().serialize(board.line(standing));
    }

    @Test
    @DisplayName("Economy's three rankings are statue boards: typed as before, a top three each, with their words")
    void threeRankings() {
        assertThat(service.boards()).extracting(StatueBoard::id).containsExactly("casino", "richest", "playtime");
        assertThat(service.boards()).noneMatch(StatueBoard::thankYou);
        assertThat(service.board(Board.RICHEST).heading()).contains("Richest players");
        assertThat(service.board(Board.RICHEST).ranks()).isEqualTo("richest players");
        assertThat(service.board(Board.CASINO).ranks()).isEqualTo("biggest casino winners");
        assertThat(service.board(Board.PLAYTIME).ranks()).isEqualTo("players who played longest");
    }

    @Test
    @DisplayName("a statue shows who holds the place and what with: money, or time played")
    void standingsAndWorth() {
        StatueBoard richest = service.board(Board.RICHEST);
        List<StatueBoard.Standing> top = richest.standings().get();
        assertThat(top).extracting(StatueBoard.Standing::name).containsExactly("Ada", "Bo");
        assertThat(plain(richest, top.getFirst())).contains("5,000");
        StatueBoard casino = service.board(Board.CASINO);
        assertThat(plain(casino, casino.standings().get().getFirst())).startsWith("won ").contains("400");
        StatueBoard playtime = service.board(Board.PLAYTIME);
        assertThat(plain(playtime, playtime.standings().get().getFirst())).startsWith("played ").contains("10 hours");
    }

    @Test
    @DisplayName("offered while the statues switch is on, withdrawn when it goes off; Prime's and creators' are not Economy's")
    void switchedOnAndOff() {
        service.switched(true);
        assertThat(StatueBoards.ids()).containsExactlyInAnyOrder("casino", "richest", "playtime");
        service.switched(false);
        assertThat(StatueBoards.ids()).isEmpty();
        assertThat(service.ours("RICHEST")).isTrue();
        assertThat(service.ours("prime")).isFalse();
        assertThat(service.ours(null)).isFalse();
    }

    @Test
    @DisplayName("without Core's statues there is nobody to hand the old statues to, and that is no error")
    void handOverWithoutCore() {
        service.handOver();
        assertThat(StatueBoards.ids()).isEmpty();
    }
}
