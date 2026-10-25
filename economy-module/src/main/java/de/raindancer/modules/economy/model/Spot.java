package de.raindancer.modules.economy.model;

import java.util.Locale;
import java.util.Optional;

/**
 * Where a leaderboard floats and what it lists, as written in the settings: {@code "world 10.5 70 -3.5 playtime"}.
 * A line without a board is from before boards existed and lists the richest.
 */
public record Spot(String world, double x, double y, double z, Podium.Board board) {

    public Spot {
        board = board == null ? Podium.Board.RICHEST : board;
    }

    public Spot(String world, double x, double y, double z) {
        this(world, x, y, z, Podium.Board.RICHEST);
    }

    public static Optional<Spot> parse(String line) {
        if (line == null) {
            return Optional.empty();
        }
        String[] words = line.trim().split("\\s+");
        if (words.length != 4 && words.length != 5) {
            return Optional.empty();
        }
        Optional<Podium.Board> board = words.length == 5 ? Podium.Board.read(words[4])
                : Optional.of(Podium.Board.RICHEST);
        if (board.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Spot(words[0], Double.parseDouble(words[1]), Double.parseDouble(words[2]),
                    Double.parseDouble(words[3]), board.get()));
        } catch (NumberFormatException notANumber) {
            return Optional.empty();
        }
    }

    public String written() {
        return String.format(Locale.ROOT, "%s %.2f %.2f %.2f %s", world, x, y, z,
                board.name().toLowerCase(Locale.ROOT));
    }

    public double distanceSquared(String otherWorld, double ox, double oy, double oz) {
        if (!world.equals(otherWorld)) {
            return Double.MAX_VALUE;
        }
        return (x - ox) * (x - ox) + (y - oy) * (y - oy) + (z - oz) * (z - oz);
    }
}
