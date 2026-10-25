package de.raindancer.modules.economy.service;

import de.raindancer.core.social.economy.Money;
import de.raindancer.modules.economy.model.Account;
import de.raindancer.modules.economy.model.Podium;
import de.raindancer.modules.economy.model.Standing;
import de.raindancer.modules.economy.model.TransactionKind;
import de.raindancer.modules.economy.store.AccountBook;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.function.ToLongFunction;

/** The boards — richest, casino winners, time played — each sorted at most every few seconds however often asked. */
public final class LeaderboardService {

    private static final long FRESH_FOR_MILLIS = 4_000;
    /** What the casino board counts: the games and the lottery, not raffles (a host's ticket money is no win). */
    private static final Set<TransactionKind> CASINO = Set.of(TransactionKind.GAMBLE, TransactionKind.LOTTERY);

    private final AccountBook book;
    private final LongSupplier clock;
    private volatile List<Account> sorted = List.of();
    private volatile long sortedAt = Long.MIN_VALUE / 2;
    private volatile List<Standing> winners = List.of();
    private volatile Map<UUID, Money> casinoNet = Map.of();
    private volatile long winnersAt = Long.MIN_VALUE / 2;
    private final ToLongFunction<UUID> minutesPlayed;
    private volatile List<Standing> players = List.of();
    private volatile long playersAt = Long.MIN_VALUE / 2;

    public LeaderboardService(AccountBook book, LongSupplier clock) {
        this(book, clock, id -> 0);
    }

    /** @param minutesPlayed how long a player has played on the network, in minutes */
    public LeaderboardService(AccountBook book, LongSupplier clock, ToLongFunction<UUID> minutesPlayed) {
        this.book = book;
        this.clock = clock;
        this.minutesPlayed = minutesPlayed;
    }

    public List<Account> ranking() {
        long now = clock.getAsLong();
        if (now - sortedAt > FRESH_FOR_MILLIS) {
            sorted = book.all().stream().filter(account -> !AccountBook.isSystem(account.id()))
                    .sorted(Comparator.comparing(Account::balance).reversed().thenComparing(Account::name))
                    .toList();
            sortedAt = now;
        }
        return sorted;
    }

    /** Whoever a board ranks, best first. The casino board lists only players who came out ahead. */
    public List<Standing> standings(Podium.Board board) {
        return switch (board) {
            case RICHEST -> ranking().stream().map(each -> new Standing(each.id(), each.name(), each.balance())).toList();
            case CASINO -> casinoWinners();
            case PLAYTIME -> longestPlayed();
        };
    }

    private List<Standing> longestPlayed() {
        long now = clock.getAsLong();
        if (now - playersAt > FRESH_FOR_MILLIS) {
            players = book.all().stream()
                    .filter(account -> !AccountBook.isSystem(account.id()))
                    .map(account -> Standing.played(account.id(), account.name(), minutesPlayed.applyAsLong(account.id())))
                    .filter(standing -> standing.minutes() > 0)
                    .sorted(Comparator.comparingLong(Standing::minutes).reversed().thenComparing(Standing::name))
                    .toList();
            playersAt = now;
        }
        return players;
    }

    private List<Standing> casinoWinners() {
        long now = clock.getAsLong();
        if (now - winnersAt > FRESH_FOR_MILLIS) {
            Map<UUID, Money> net = book.netOf(CASINO);
            casinoNet = net;
            winners = book.all().stream()
                    .filter(account -> !AccountBook.isSystem(account.id()))
                    .map(account -> new Standing(account.id(), account.name(), net.getOrDefault(account.id(), Money.ZERO)))
                    .filter(standing -> standing.amount().isPositive())
                    .sorted(Comparator.comparing(Standing::amount).reversed().thenComparing(Standing::name))
                    .toList();
            winnersAt = now;
        }
        return winners;
    }

    /** What a player won (or lost, negative) at the games and the lottery, over the account's whole life. */
    public Money casinoNet(UUID id) {
        casinoWinners();
        return casinoNet.getOrDefault(id, Money.ZERO);
    }

    /** 1-based place, or 0 for no account. */
    public int placeOf(UUID id) {
        List<Account> all = ranking();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id().equals(id)) {
                return i + 1;
            }
        }
        return 0;
    }
}
