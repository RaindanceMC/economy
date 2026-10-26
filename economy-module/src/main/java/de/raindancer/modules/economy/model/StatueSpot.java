package de.raindancer.modules.economy.model;

import java.util.Locale;
import java.util.Optional;

/**
 * Where one statue of a top three stands, as written in the settings:
 * {@code "casino 10.50 64.00 -3.25 90.0 richest 2 a1b2c3d4"}. {@code y} is the ground its pedestal stands on.
 * Statues put up together as a podium share a {@code group}; one placed on its own has {@code "-"}.
 */
public record StatueSpot(String world, double x, double y, double z, float yaw, Podium.Board board, int place,
                         String group) {

    public static final String ALONE = "-";

    public static Optional<StatueSpot> parse(String line) {
        if (line == null) {
            return Optional.empty();
        }
        String[] words = line.trim().split("\\s+");
        if (words.length != 8) {
            return Optional.empty();
        }
        Optional<Podium.Board> board = Podium.Board.read(words[5]);
        Optional<Integer> place = readPlace(words[6]);
        if (board.isEmpty() || place.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new StatueSpot(words[0], Double.parseDouble(words[1]), Double.parseDouble(words[2]),
                    Double.parseDouble(words[3]), Float.parseFloat(words[4]), board.get(), place.get(), words[7]));
        } catch (NumberFormatException notANumber) {
            return Optional.empty();
        }
    }

    /** A place as typed: 1, 2 or 3, a leading {@code #} allowed. */
    public static Optional<Integer> readPlace(String typed) {
        if (typed == null) {
            return Optional.empty();
        }
        String digits = typed.strip();
        if (digits.startsWith("#")) {
            digits = digits.substring(1);
        }
        return switch (digits) {
            case "1" -> Optional.of(1);
            case "2" -> Optional.of(2);
            case "3" -> Optional.of(3);
            default -> Optional.empty();
        };
    }

    public String written() {
        return String.format(Locale.ROOT, "%s %.2f %.2f %.2f %.1f %s %d %s", world, x, y, z, yaw,
                board.name().toLowerCase(Locale.ROOT), place, group);
    }

    /** What everything standing on this spot is tagged with, before a {@code #}. */
    public String key() {
        return written();
    }

    public boolean standalone() {
        return ALONE.equals(group);
    }

    /** The same statue somewhere else; moved away from its podium it stands on its own. */
    public StatueSpot at(String otherWorld, double ox, double oy, double oz, float facing) {
        return new StatueSpot(otherWorld, ox, oy, oz, facing, board, place, ALONE);
    }

    public double distanceSquared(String otherWorld, double ox, double oy, double oz) {
        if (!world.equals(otherWorld)) {
            return Double.MAX_VALUE;
        }
        return (x - ox) * (x - ox) + (y - oy) * (y - oy) + (z - oz) * (z - oz);
    }
}
