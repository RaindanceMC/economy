package de.raindancer.modules.economy.rules;

import java.util.Locale;

/**
 * What an item is worth when nothing else prices it — no shipped value, no recipe, nothing an owner wrote: its
 * rarity, as the game colours its name. Only ever paid, never charged: the shop does not sell these.
 */
public final class FallbackPriceRule implements IEconomyRule {

    @Override
    public String describe() {
        return "an item nothing else prices is worth its rarity";
    }

    /** @param rarity the game's rarity name; anything else counts as common */
    public long coins(String rarity) {
        return switch (rarity == null ? "" : rarity.toUpperCase(Locale.ROOT)) {
            case "UNCOMMON" -> 250;
            case "RARE" -> 1_000;
            case "EPIC" -> 5_000;
            default -> 50;
        };
    }
}
