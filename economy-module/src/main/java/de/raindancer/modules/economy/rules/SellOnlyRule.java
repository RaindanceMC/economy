package de.raindancer.modules.economy.rules;

import java.util.List;
import java.util.Locale;

/**
 * Items the shop buys but never sells — {@code sell-only.yml}: whole names, or with one star at the start or the
 * end ({@code *_ore}, {@code music_disc_*}). Loot and ores, where a bought one is worth more than it cost: an ore
 * mined with Fortune, a map leading to treasure, a trial key opening a vault.
 */
public final class SellOnlyRule implements IEconomyRule {

    private final List<String> patterns;

    public SellOnlyRule(List<String> patterns) {
        this.patterns = (patterns == null ? List.<String>of() : patterns).stream()
                .map(pattern -> pattern.strip().toUpperCase(Locale.ROOT)).filter(pattern -> !pattern.isEmpty()).toList();
    }

    @Override
    public String describe() {
        return "which items the shop buys and never sells";
    }

    public boolean matches(String name) {
        String upper = name.toUpperCase(Locale.ROOT);
        for (String pattern : patterns) {
            if (pattern.startsWith("*") ? upper.length() > pattern.length() - 1 && upper.endsWith(pattern.substring(1))
                    : pattern.endsWith("*") ? upper.length() > pattern.length() - 1
                    && upper.startsWith(pattern.substring(0, pattern.length() - 1))
                    : upper.equals(pattern)) {
                return true;
            }
        }
        return false;
    }
}
