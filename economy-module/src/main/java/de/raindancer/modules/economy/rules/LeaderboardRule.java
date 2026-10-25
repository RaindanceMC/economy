package de.raindancer.modules.economy.rules;

import java.util.List;

/** How the top players of a board are listed — in the sidebar and on a leaderboard alike. */
public final class LeaderboardRule implements IEconomyRule {

    public <T> List<T> top(List<T> ranking, int size) {
        return ranking.subList(0, Math.max(0, Math.min(size, ranking.size())));
    }

    /** Gold, silver, bronze, then white. */
    public String colourOf(int place) {
        return switch (place) {
            case 1 -> "<gold>";
            case 2 -> "<gray>";
            case 3 -> "<#cd7f32>";
            default -> "<white>";
        };
    }

    public String shortName(String name, int most) {
        return name.length() <= most ? name : name.substring(0, Math.max(1, most - 1)) + "…";
    }

    @Override
    public String describe() {
        return "how the top players of a board are listed";
    }
}
