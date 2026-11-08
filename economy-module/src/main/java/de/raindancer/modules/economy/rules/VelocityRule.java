package de.raindancer.modules.economy.rules;

import de.raindancer.core.social.economy.Money;
import de.raindancer.modules.economy.model.TransactionKind;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * How fast money can honestly come in. A dupe, an exploited shop price or a broken reward shows up first as one
 * player's balance climbing faster than anybody earns — before anybody notices the item that caused it. This
 * counts what each player gained within a window and says when it passes the ceiling, once per quiet spell.
 *
 * <p>Only what is earned counts: cash paid back in, a loan, an opening balance and an auction's proceeds are money
 * moved about, not made. A payment from another player is counted, because a dupe is often laundered through one.
 */
public final class VelocityRule implements IEconomyRule {

    /** The window a player's gains are summed over. */
    public static final long WINDOW_MILLIS = 10 * 60_000L;
    /** After one word to staff about a player, how long before the next. */
    public static final long QUIET_MILLIS = 30 * 60_000L;

    /** Kinds that move money rather than make it. */
    public static final Set<TransactionKind> NOT_EARNED = EnumSet.of(TransactionKind.OPENING, TransactionKind.DEPOSIT,
            TransactionKind.WITHDRAW, TransactionKind.LOAN, TransactionKind.AUCTION);

    private record Gain(long atMillis, long minor) {
    }

    private final Map<UUID, Deque<Gain>> gains = new HashMap<>();
    private final Map<UUID, Long> warnedAt = new HashMap<>();

    /** What was gained in the window, after this gain; empty when nothing is worth saying. */
    public synchronized java.util.Optional<Money> record(UUID player, Money delta, TransactionKind kind, long now, Money ceiling) {
        if (player == null || delta == null || !delta.isPositive() || kind == null || NOT_EARNED.contains(kind)
                || ceiling == null || !ceiling.isPositive()) {
            return java.util.Optional.empty();
        }
        Deque<Gain> theirs = gains.computeIfAbsent(player, id -> new ArrayDeque<>());
        theirs.addLast(new Gain(now, delta.minor()));
        while (!theirs.isEmpty() && now - theirs.peekFirst().atMillis() > WINDOW_MILLIS) {
            theirs.pollFirst();
        }
        long sum = 0;
        for (Gain gain : theirs) {
            sum += gain.minor();
        }
        if (sum < ceiling.minor()) {
            return java.util.Optional.empty();
        }
        Long last = warnedAt.get(player);
        if (last != null && now - last < QUIET_MILLIS) {
            return java.util.Optional.empty();
        }
        warnedAt.put(player, now);
        return java.util.Optional.of(Money.of(sum));
    }

    public synchronized void forget(UUID player) {
        gains.remove(player);
        warnedAt.remove(player);
    }

    @Override
    public String describe() {
        return "how fast money can honestly come in, and when staff hear about faster";
    }
}
