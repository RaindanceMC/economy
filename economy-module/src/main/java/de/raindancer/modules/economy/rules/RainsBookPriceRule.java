package de.raindancer.modules.economy.rules;

import de.raindancer.core.social.economy.Money;

/** What a book of one of Rain's enchantments costs at the shop: money per level, times the level. */
public final class RainsBookPriceRule implements IEconomyRule {

    private final Money perLevel;

    public RainsBookPriceRule(Money perLevel) {
        this.perLevel = perLevel;
    }

    public Money money(int level) {
        return new Money(perLevel.minor() * Math.max(1, level));
    }

    @Override
    public String describe() {
        return "money for a book of Rain's enchantments, by level";
    }
}
