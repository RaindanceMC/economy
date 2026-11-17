package de.raindancer.modules.economy.model;

import java.util.Optional;

/** What a floating list (and a statue, through Core's statue boards) ranks or thanks. */
public enum Board {
    /** Net winnings at the games of chance and the lottery, over the account's whole life. */
    CASINO,
    /** Balance, like /baltop. */
    RICHEST,
    /** Time played on the whole network, away time included, as Core counts it. */
    PLAYTIME,
    /** Who holds Prime right now, longest-running first: Prime's statue board, shown here as a list. */
    PRIME,
    /** The server's creators: Ranks' statue board, shown here as a list. */
    CREATOR;

    /** A thank-you rather than a ranking: its people are listed without an amount. */
    public boolean thankYou() {
        return this == PRIME || this == CREATOR;
    }

    /**
     * MiniMessage over a floating list of this board and its statues. Prime's and the creators' statue boards are
     * their own plugins'; a list shows their heading when they are offered, this one otherwise.
     */
    public String heading() {
        return switch (this) {
            case CASINO -> "<gold><bold>Biggest casino winners</bold></gold>";
            case RICHEST -> "<gold><bold>Richest players</bold></gold>";
            case PLAYTIME -> "<gold><bold>Most time played</bold></gold>";
            case PRIME -> "<gold><bold>Prime supporters</bold></gold>";
            case CREATOR -> "<gold><bold>Creators</bold></gold>";
        };
    }

    /** The id of Core's statue board this is ({@code richest}). */
    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static Optional<Board> read(String typed) {
        if (typed == null) {
            return Optional.empty();
        }
        for (Board board : values()) {
            if (board.name().equalsIgnoreCase(typed.strip())) {
                return Optional.of(board);
            }
        }
        return Optional.empty();
    }
}
