package de.raindancer.modules.economy.service;

import de.raindancer.core.data.sql.Database;
import de.raindancer.core.testkit.TestDatabases;
import de.raindancer.core.social.economy.Money;
import de.raindancer.modules.economy.model.Account;
import de.raindancer.modules.economy.model.Podium;
import de.raindancer.modules.economy.model.Standing;
import de.raindancer.modules.economy.rules.BalanceRule;
import de.raindancer.modules.economy.store.AccountBook;
import de.raindancer.modules.economy.store.EconomyDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("database")
class LeaderboardServiceTest {

    @TempDir
    Path folder;

    private final AtomicLong clock = new AtomicLong(1_000_000L);
    private Database database;
    private AccountBook book;

    @BeforeEach
    void open() {
        database = TestDatabases.open(folder.resolve("economy.db"), EconomyDatabase.SCHEMA);
        book = new AccountBook(database, new BalanceRule(), clock::get);
        book.load();
    }

    @AfterEach
    void close() {
        database.close();
    }

    @Test
    @DisplayName("a Bedrock player is ranked like anybody else; the server's pots are not")
    void bedrockRanked() {
        UUID java = UUID.randomUUID();
        UUID bedrock = UUID.fromString("00000000-0000-0000-0009-01f2c3d4e5f6");
        book.open(java, "Kaspar", Money.of(5_000));
        book.open(bedrock, ".Bedrock", Money.of(126_310));
        book.open(AccountBook.LOTTERY_POT, "Lottery", Money.of(1_000_000));

        LeaderboardService leaderboard = new LeaderboardService(book, clock::get);

        assertThat(leaderboard.ranking()).extracting(Account::id).containsExactly(bedrock, java);
        assertThat(leaderboard.placeOf(bedrock)).isEqualTo(1);
    }

    @Test
    @DisplayName("the casino board ranks what each player won at the tables, net; losers and pots are not on it")
    void casinoWinners() {
        UUID ada = UUID.randomUUID();
        UUID bo = UUID.randomUUID();
        UUID loser = UUID.randomUUID();
        UUID rich = UUID.randomUUID();
        book.open(ada, "Ada", Money.of(1_000));
        book.open(bo, "Bo", Money.of(1_000));
        book.open(loser, "Lu", Money.of(1_000));
        book.open(rich, "Rich", Money.of(900_000));
        Money most = Money.of(1_000_000_000L);
        book.play(ada, Money.of(100), Money.of(600), "Slots — won", most);
        book.play(ada, Money.of(100), Money.ZERO, "Slots", most);
        book.play(bo, Money.of(100), Money.of(300), "Dice — won", most);
        book.play(loser, Money.of(500), Money.ZERO, "Roulette", most);
        book.open(AccountBook.LOTTERY_POT, "Lottery", Money.of(1_000_000));

        LeaderboardService leaderboard = new LeaderboardService(book, clock::get);

        assertThat(leaderboard.standings(Podium.Board.CASINO)).extracting(Standing::name)
                .containsExactly("Ada", "Bo");
        assertThat(leaderboard.standings(Podium.Board.CASINO).getFirst().amount()).isEqualTo(Money.of(400));
        assertThat(leaderboard.standings(Podium.Board.RICHEST).getFirst().name()).isEqualTo("Rich");
    }

    @Test
    @DisplayName("the playtime board ranks by minutes played on the network; nobody who never played, no pots")
    void playtime() {
        UUID ada = UUID.randomUUID();
        UUID bo = UUID.randomUUID();
        UUID fresh = UUID.randomUUID();
        book.open(ada, "Ada", Money.of(1));
        book.open(bo, "Bo", Money.of(900_000));
        book.open(fresh, "Fresh", Money.of(1));
        book.open(AccountBook.LOTTERY_POT, "Lottery", Money.of(1_000_000));
        java.util.Map<UUID, Long> minutes = java.util.Map.of(ada, 600L, bo, 90L, AccountBook.LOTTERY_POT, 99_999L);

        LeaderboardService leaderboard = new LeaderboardService(book, clock::get,
                id -> minutes.getOrDefault(id, 0L));

        assertThat(leaderboard.standings(Podium.Board.PLAYTIME)).extracting(Standing::name)
                .containsExactly("Ada", "Bo");
        assertThat(leaderboard.standings(Podium.Board.PLAYTIME).getFirst().minutes()).isEqualTo(600L);
    }
}
