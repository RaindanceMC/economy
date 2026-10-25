package de.raindancer.modules.economy.rules;

import de.raindancer.modules.economy.model.Podium;
import net.kyori.adventure.text.minimessage.MiniMessage;

/** How the top three stand on a podium: who is where, how high, and what their names say. */
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

    @Override
    public String describe() {
        return "how the top three stand on a podium";
    }
}
