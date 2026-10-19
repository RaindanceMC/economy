package de.raindancer.modules.economy.rules;

import de.raindancer.core.social.economy.Money;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * What a made thing the shop sells costs (the Sell Wand, the Auto Sell Chest): its ingredients at this buyer's
 * prices. Never less, so buying it ready-made never beats crafting it from what the shop sells.
 */
public final class GoodPriceRule implements IEconomyRule {

    @Override
    public String describe() {
        return "a made thing costs what its ingredients cost";
    }

    /** @param buy what this many of one ingredient cost this buyer; empty when the shop does not sell it */
    public Optional<Money> price(Map<Material, Integer> ingredients, BiFunction<Material, Integer, Optional<Money>> buy) {
        if (ingredients.isEmpty()) {
            return Optional.empty();
        }
        Money total = Money.ZERO;
        for (Map.Entry<Material, Integer> each : ingredients.entrySet()) {
            Optional<Money> part = buy.apply(each.getKey(), each.getValue());
            if (part.isEmpty() || !part.get().isPositive()) {
                return Optional.empty();
            }
            total = total.plus(part.get());
        }
        return Optional.of(total);
    }

    /** A shaped recipe's ingredients: each key letter of the rows, counted. */
    public Map<Material, Integer> count(List<String> rows, Map<Character, Material> key) {
        Map<Material, Integer> counted = new EnumMap<>(Material.class);
        for (String row : rows) {
            for (char letter : row.toCharArray()) {
                Material material = key.get(letter);
                if (material != null) {
                    counted.merge(material, 1, Integer::sum);
                }
            }
        }
        return counted;
    }

    /** The same for rows of material names, {@code -} for an empty slot — how a Core custom item writes its recipe. */
    public Map<Material, Integer> countNamed(List<String> rows) {
        Map<Material, Integer> counted = new EnumMap<>(Material.class);
        for (String row : rows) {
            for (String name : row.trim().split("\\s+")) {
                Material material = name.equals("-") ? null : Material.matchMaterial(name.toUpperCase(Locale.ROOT));
                if (material != null) {
                    counted.merge(material, 1, Integer::sum);
                }
            }
        }
        return counted;
    }
}
