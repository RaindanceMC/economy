package de.raindancer.modules.economy.rules;

import de.raindancer.modules.economy.model.Podium;
import de.raindancer.modules.economy.model.StatueSpot;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * How the top three stand: each statue on its own spot, or three together as a podium — who is where, how high,
 * which one is labelled, and what their names say.
 */
public final class StatueRule implements IEconomyRule {

    public static final int PLACES = 3;
    private static final double SPACING = 1.0;

    private final LeaderboardRule leaderboard = new LeaderboardRule();

    /** How tall the pedestal under each place is, in blocks. */
    public double height(int place) {
        return switch (place) {
            case 1 -> 1.0;
            case 2 -> 0.7;
            default -> 0.45;
        };
    }

    /**
     * Where a place stands from the podium's centre, {x, y, z}, for statues facing {@code yaw}. Second goes on the
     * statues' right hand, so the crowd in front reads 2 · 1 · 3.
     */
    public double[] offset(int place, float yaw) {
        double side = switch (place) {
            case 1 -> 0;
            case 2 -> SPACING;
            default -> -SPACING;
        };
        double radians = Math.toRadians(yaw);
        // Minecraft's yaw 0 faces +z; the right hand then points to -x.
        return new double[]{-Math.cos(radians) * side, height(place), -Math.sin(radians) * side};
    }

    /** The name over a statue; {@code name} null for a place nobody holds yet. */
    public String title(int place, String name) {
        if (name == null) {
            return "<gray><bold>#" + place + "</bold> <dark_gray>Nobody yet";
        }
        return leaderboard.colourOf(place) + "<bold>#" + place + "</bold> <white>"
                + MiniMessage.miniMessage().escapeTags(name);
    }

    /** What floats over the podium. */
    public String heading(Podium.Board board) {
        return switch (board) {
            case CASINO -> "<gold><bold>Biggest casino winners</bold></gold>";
            case RICHEST -> "<gold><bold>Richest players</bold></gold>";
            case PLAYTIME -> "<gold><bold>Most time played</bold></gold>";
        };
    }

    /** What goes before the amount under a name. */
    public String caption(Podium.Board board) {
        return switch (board) {
            case CASINO -> "<gray>won ";
            case RICHEST -> "<gray>";
            case PLAYTIME -> "<gray>played ";
        };
    }

    /** A whole podium of statues facing {@code yaw}, the winner on the ground at {@code x y z}. */
    public List<StatueSpot> podium(String world, double x, double y, double z, float yaw, Podium.Board board,
                                   String group) {
        List<StatueSpot> three = new ArrayList<>();
        for (int place = 1; place <= PLACES; place++) {
            double[] offset = offset(place, yaw);
            StatueSpot spot = new StatueSpot(world, x + offset[0], y, z + offset[2], yaw, board, place, group);
            // Through the written form, so a spot read back from the settings is equal to the one placed.
            three.add(StatueSpot.parse(spot.written()).orElseThrow());
        }
        return three;
    }

    /** Whether some lines are podiums from before statues stood on their own. */
    public boolean hasLegacy(List<String> lines) {
        return lines.stream().anyMatch(line -> StatueSpot.parse(line).isEmpty() && Podium.parse(line).isPresent());
    }

    /** Every statue in the settings, an old podium read as its three statues. */
    public List<StatueSpot> read(List<String> lines) {
        List<StatueSpot> spots = new ArrayList<>();
        for (String line : lines) {
            Optional<StatueSpot> spot = StatueSpot.parse(line);
            if (spot.isPresent()) {
                spots.add(spot.get());
            } else {
                Podium.parse(line).ifPresent(old -> spots.addAll(fromLegacy(old)));
            }
        }
        return spots;
    }

    /** The lines with every old podium written as its three statues; anything else stays as it was. */
    public List<String> migrate(List<String> lines) {
        List<String> migrated = new ArrayList<>();
        for (String line : lines) {
            Optional<Podium> old = StatueSpot.parse(line).isPresent() ? Optional.empty() : Podium.parse(line);
            if (old.isPresent()) {
                fromLegacy(old.get()).forEach(spot -> migrated.add(spot.written()));
            } else {
                migrated.add(line);
            }
        }
        return migrated;
    }

    /** The group follows from the old podium, so reading it before and after it was written back agrees. */
    private List<StatueSpot> fromLegacy(Podium old) {
        String group = String.format(Locale.ROOT, "%08x", old.key().hashCode());
        return podium(old.world(), old.x(), old.y(), old.z(), old.yaw(), old.board(), group);
    }

    /** Whether a statue carries the label of what it ranks: on its own always, of a podium the highest left. */
    public boolean labelled(StatueSpot spot, List<StatueSpot> all) {
        if (spot.standalone()) {
            return true;
        }
        return all.stream().noneMatch(other -> other.group().equals(spot.group()) && other.place() < spot.place());
    }

    /** {@code yaw} turned to the nearest multiple of {@code step}, between 0 and 360. */
    public float facing(float yaw, float step) {
        float turned = Math.round(yaw / step) * step;
        return ((turned % 360) + 360) % 360;
    }

    /** The statue nearest to a point, closer than {@code maxSquared} (squared blocks), among those that match. */
    public Optional<StatueSpot> nearest(List<StatueSpot> all, String world, double x, double y, double z,
                                        double maxSquared, Predicate<StatueSpot> which) {
        StatueSpot nearest = null;
        double best = maxSquared;
        for (StatueSpot spot : all) {
            double distance = spot.distanceSquared(world, x, y, z);
            if (which.test(spot) && distance < best) {
                best = distance;
                nearest = spot;
            }
        }
        return Optional.ofNullable(nearest);
    }

    /** What a board ranks, for a sentence: "the #2 of the richest players". */
    public String ranks(Podium.Board board) {
        return switch (board) {
            case CASINO -> "biggest casino winners";
            case RICHEST -> "richest players";
            case PLAYTIME -> "players who played longest";
        };
    }

    @Override
    public String describe() {
        return "how the top three stand on a podium";
    }
}
