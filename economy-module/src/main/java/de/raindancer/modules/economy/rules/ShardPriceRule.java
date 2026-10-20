package de.raindancer.modules.economy.rules;

import de.raindancer.core.social.economy.Money;

import java.util.ArrayList;
import java.util.List;

/** What a book of one of Rain's enchantments costs at the shop: the level's shards, and money per level. */
public final class ShardPriceRule implements IEconomyRule {

    private static final List<Integer> BUILT_IN = List.of(8, 16, 32, 64, 128);

    private final List<Integer> steps;
    private final Money perLevel;
    private final boolean freeOnceUnlocked;

    /** @param steps shards for level I, II, …, as written in the settings */
    public ShardPriceRule(List<String> steps, Money perLevel) {
        this(steps, perLevel, false);
    }

    /** @param freeOnceUnlocked whether a book of an enchantment the guild unlocked takes no shards at all */
    public ShardPriceRule(List<String> steps, Money perLevel, boolean freeOnceUnlocked) {
        List<Integer> read = new ArrayList<>();
        for (String step : steps) {
            try {
                int value = Integer.parseInt(step.strip());
                if (value <= 0) {
                    read.clear();
                    break;
                }
                read.add(value);
            } catch (NumberFormatException mangled) {
                read.clear();
                break;
            }
        }
        // Two books of a level make the next one in an anvil, so a step past double the one before is never
        // paid — players buy two of the cheaper level and combine them. Capped here rather than refused,
        // because servers still have the old 8, 24, 64, … written in their settings.
        for (int level = 1; level < read.size(); level++) {
            read.set(level, Math.min(read.get(level), 2 * read.get(level - 1)));
        }
        this.steps = read.isEmpty() ? BUILT_IN : List.copyOf(read);
        this.perLevel = perLevel;
        this.freeOnceUnlocked = freeOnceUnlocked;
    }

    /** The shards a book takes for this buyer: none once unlocked, when the server says so. */
    public int shards(int level, boolean unlocked) {
        int shards = shards(level);
        return freeOnceUnlocked && unlocked && shards != Integer.MAX_VALUE ? 0 : shards;
    }

    public int shards(int level) {
        if (level < 1) {
            return Integer.MAX_VALUE;
        }
        if (level <= steps.size()) {
            return steps.get(level - 1);
        }
        long value = steps.getLast();
        for (int extra = steps.size(); extra < level; extra++) {
            value = Math.min(Integer.MAX_VALUE, value * 2);
        }
        return (int) value;
    }

    public Money money(int level) {
        return new Money(perLevel.minor() * Math.max(1, level));
    }

    @Override
    public String describe() {
        return "shards and money for a book of Rain's enchantments, by level";
    }
}
