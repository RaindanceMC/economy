package de.raindancer.modules.economy.rules;

import de.raindancer.core.ui.choose.Catalogue;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Stacks somebody else took out for selling — a vault's page: one sale per material, and which stacks went.
 * A stack is either a plain one of a material the shop buys or it goes back untouched.
 */
public final class StackSaleRule implements IEconomyRule {

    /**
     * @param plain   per stack, its material when it is a plain one the shop buys
     * @param amounts per stack, how many
     * @return how many of each material, in the order they first came
     */
    public Map<Material, Integer> counts(List<Optional<Material>> plain, List<Integer> amounts) {
        Map<Material, Integer> counts = new LinkedHashMap<>();
        for (int index = 0; index < plain.size(); index++) {
            int amount = amounts.get(index);
            plain.get(index).ifPresent(material -> counts.merge(material, amount, Integer::sum));
        }
        return counts;
    }

    /** Per stack, whether it was sold: it was plain and its material was paid for. */
    public List<Boolean> sold(List<Optional<Material>> plain, Set<Material> paid) {
        List<Boolean> sold = new ArrayList<>(plain.size());
        plain.forEach(material -> sold.add(material.map(paid::contains).orElse(false)));
        return sold;
    }

    /** The statement line: "12 × Oak Log", or "7 items" for several kinds. */
    public String what(Map<Material, Integer> counts) {
        int items = counts.values().stream().mapToInt(Integer::intValue).sum();
        return counts.size() == 1
                ? items + " × " + Catalogue.readable(counts.keySet().iterator().next().name())
                : items + " items";
    }

    @Override
    public String describe() {
        return "which of the stacks a vault hands over are sold, one sale per material";
    }
}
