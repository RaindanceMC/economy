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

    /**
     * What something handed over to be sold is priced as when the shop has no price for it as it is: a potion or a
     * book it does not stock is still a bottle or a book, a tipped arrow still an arrow.
     */
    public org.bukkit.Material pricedAs(org.bukkit.Material material) {
        return switch (material) {
            case ENCHANTED_BOOK -> org.bukkit.Material.BOOK;
            case POTION, SPLASH_POTION, LINGERING_POTION -> org.bukkit.Material.GLASS_BOTTLE;
            case TIPPED_ARROW -> org.bukkit.Material.ARROW;
            default -> material;
        };
    }
}
