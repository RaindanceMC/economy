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

    /** 1st, 2nd, 3rd, 4th … 11th, 12th, 13th, 21st. */
    public String ordinal(int place) {
        int last = place % 10;
        int lastTwo = place % 100;
        String suffix = lastTwo >= 11 && lastTwo <= 13 ? "th"
                : last == 1 ? "st" : last == 2 ? "nd" : last == 3 ? "rd" : "th";
        return place + suffix;
    }

    /** A mark for the first three places, the place itself for the rest — the sidebar's leaderboard. */
    public String medal(int place) {
        return switch (place) {
            case 1 -> "①";
            case 2 -> "②";
            case 3 -> "③";
            default -> place + ".";
        };
    }

    @Override
    public String describe() {
        return "how the top players of a board are listed";
    }
}
