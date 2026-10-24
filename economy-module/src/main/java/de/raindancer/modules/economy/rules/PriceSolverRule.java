package de.raindancer.modules.economy.rules;

import de.raindancer.core.social.economy.Money;
import de.raindancer.modules.economy.model.RecipeShape;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Prices for everything that can be made, from the prices of what it is made of.
 *
 * <p>Relaxation, as for shortest paths: every pass prices each recipe from what is known so far and keeps
 * the cheapest answer per item, until a pass changes nothing. A recipe's cost only ever rises with its
 * markup, so cycles — ingots to a block and the block back to ingots — settle rather than spiral. A raw
 * price is a fact and is never replaced by a recipe's.
 */
public final class PriceSolverRule implements IEconomyRule {

    /** Far more than any real chain is deep (log → planks → stick → torch …). */
    private static final int MOST_PASSES = 64;

    /** Smelting burns this; one piece smelts {@link #SMELTS_PER_COAL} items. */
    public static final String FUEL = "COAL";
    public static final int SMELTS_PER_COAL = 8;

    /**
     * Crafting adds the markup to its ingredients; smelting adds no markup, only the coal one item takes — so a
     * smelted thing is never worth more than its input and its fuel.
     *
     * @return raw prices and every price that could be worked out, keyed by material name
     */
    public Map<String, Money> solve(Map<String, Money> base, List<RecipeShape> recipes, double craftMarkup) {
        Map<String, Money> known = new HashMap<>(base);
        for (int pass = 0; pass < MOST_PASSES; pass++) {
            boolean changed = false;
            for (RecipeShape recipe : recipes) {
                if (base.containsKey(recipe.result())) {
                    continue;
                }
                Money cost = costOf(recipe, known, craftMarkup);
                if (cost == null) {
                    continue;
                }
                Money before = known.get(recipe.result());
                if (before == null || cost.isMoreThan(Money.ZERO) && before.isMoreThan(cost)) {
                    known.put(recipe.result(), cost);
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
        }
        return known;
    }

    /**
     * What each priced-by-recipe item is made of, as each material's share of the ingredients' value (summing to
     * 1) — read off the recipe whose cost became the item's price, with the cheapest material in every slot, as
     * the price itself was worked out. Raw materials are made of nothing.
     *
     * @param solved what {@link #solve} answered for the same base, recipes and markups
     */
    public Map<String, Map<String, Double>> materials(Map<String, Money> base, List<RecipeShape> recipes,
                                                      Map<String, Money> solved, double craftMarkup) {
        Map<String, Map<String, Double>> made = new HashMap<>();
        for (RecipeShape recipe : recipes) {
            String result = recipe.result();
            Money price = solved.get(result);
            if (base.containsKey(result) || made.containsKey(result) || price == null) {
                continue;
            }
            if (!price.equals(costOf(recipe, solved, craftMarkup))) {
                continue;
            }
            Map<String, Double> shares = new HashMap<>();
            double total = 0;
            for (List<String> slot : recipe.slots()) {
                String cheapest = null;
                for (String option : slot) {
                    Money each = solved.get(option);
                    if (each != null && (cheapest == null || solved.get(cheapest).isMoreThan(each))) {
                        cheapest = option;
                    }
                }
                long worth = solved.get(cheapest).minor();
                shares.merge(cheapest, (double) worth, Double::sum);
                total += worth;
            }
            double fuel = recipe.process() == RecipeShape.Process.SMELT ? fuelPerItem(solved) * recipe.amount() : 0;
            if (fuel > 0) {
                shares.merge(FUEL, fuel, Double::sum);
                total += fuel;
            }
            if (total > 0) {
                double sum = total;
                shares.replaceAll((material, worth) -> worth / sum);
                made.put(result, Map.copyOf(shares));
            }
        }
        return made;
    }

    private static double fuelPerItem(Map<String, Money> known) {
        Money coal = known.get(FUEL);
        return coal == null ? 0 : Math.max(0, coal.minor()) / (double) SMELTS_PER_COAL;
    }

    /** One of what this recipe makes, or null when an ingredient has no price yet. */
    private static Money costOf(RecipeShape recipe, Map<String, Money> known, double craftMarkup) {
        if (recipe.slots().isEmpty()) {
            return null;
        }
        long total = 0;
        for (List<String> slot : recipe.slots()) {
            Money cheapest = null;
            for (String option : slot) {
                Money price = known.get(option);
                if (price != null && (cheapest == null || cheapest.isMoreThan(price))) {
                    cheapest = price;
                }
            }
            if (cheapest == null) {
                return null;
            }
            try {
                total = Math.addExact(total, cheapest.minor());
            } catch (ArithmeticException overflow) {
                return null;
            }
        }
        double each = switch (recipe.process()) {
            case SMELT -> total / (double) recipe.amount() + fuelPerItem(known);
            case WORLD -> total / (double) recipe.amount();
            default -> total * (1.0 + Math.max(0, craftMarkup)) / recipe.amount();
        };
        if (each >= Long.MAX_VALUE) {
            return null;
        }
        return Money.of(Math.max(1, Math.round(each)));
    }

    @Override
    public String describe() {
        return "prices for everything craftable, from the prices of its ingredients";
    }
}
