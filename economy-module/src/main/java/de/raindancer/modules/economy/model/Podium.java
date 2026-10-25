package de.raindancer.modules.economy.model;

import java.util.Locale;
import java.util.Optional;

/**
 * Where a podium of the top three stands, which way the statues face, and what it ranks — as written in the
 * settings: {@code "casino 10.50 64.00 -3.25 90.0 casino"}.
 */
public record Podium(String world, double x, double y, double z, float yaw, Board board) {

    /** What a podium ranks. */
    public enum Board {
        /** Net winnings at the games of chance and the lottery, over the account's whole life. */
        CASINO,
        /** Balance, like /baltop. */
        RICHEST;

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

    public static Optional<Podium> parse(String line) {
        if (line == null) {
            return Optional.empty();
        }
        String[] words = line.trim().split("\\s+");
        if (words.length != 6) {
            return Optional.empty();
        }
        Optional<Board> board = Board.read(words[5]);
        if (board.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Podium(words[0], Double.parseDouble(words[1]), Double.parseDouble(words[2]),
                    Double.parseDouble(words[3]), Float.parseFloat(words[4]), board.get()));
        } catch (NumberFormatException notANumber) {
            return Optional.empty();
        }
    }

    public String written() {
        return String.format(Locale.ROOT, "%s %.2f %.2f %.2f %.1f %s", world, x, y, z, yaw,
                board.name().toLowerCase(Locale.ROOT));
    }

    /** What the statues of this podium are tagged with, so two podiums side by side never swap theirs. */
    public String key() {
        return written();
    }

    public double distanceSquared(String otherWorld, double ox, double oy, double oz) {
        if (!world.equals(otherWorld)) {
            return Double.MAX_VALUE;
        }
        return (x - ox) * (x - ox) + (y - oy) * (y - oy) + (z - oz) * (z - oz);
    }
}
