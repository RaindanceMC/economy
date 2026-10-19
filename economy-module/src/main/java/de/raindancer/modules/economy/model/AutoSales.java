package de.raindancer.modules.economy.model;

import de.raindancer.core.social.economy.Money;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * What Auto Sell sold and has not paid yet. Drops sell the moment they fall, but are paid a few at a time,
 * so a vein is one line in the statement and not sixty. Plain items are kept as counts; anything else (worn,
 * enchanted, full) as the stack itself — together, all it takes to hand them back if a payment fails.
 */
public final class AutoSales {

    public record Batch(Money total, Map<Material, Integer> counts, List<ItemStack> stacks) {

        public Batch(Money total, Map<Material, Integer> counts) {
            this(total, counts, List.of());
        }

        public int items() {
            return counts.values().stream().mapToInt(Integer::intValue).sum()
                    + stacks.stream().mapToInt(ItemStack::getAmount).sum();
        }
    }

    private final Map<UUID, Batch> owed = new HashMap<>();

    public synchronized void add(UUID player, Material material, int count, Money worth) {
        Batch old = owed.get(player);
        Map<Material, Integer> counts = old == null ? new EnumMap<>(Material.class) : old.counts();
        counts.merge(material, count, Integer::sum);
        owed.put(player, new Batch(old == null ? worth : old.total().plus(worth), counts,
                old == null ? List.of() : old.stacks()));
    }

    /** Something sold that is not a plain item, kept as it is. */
    public synchronized void addStack(UUID player, ItemStack stack, Money worth) {
        Batch old = owed.get(player);
        List<ItemStack> stacks = new ArrayList<>(old == null ? List.of() : old.stacks());
        stacks.add(stack);
        owed.put(player, new Batch(old == null ? worth : old.total().plus(worth),
                old == null ? new EnumMap<>(Material.class) : old.counts(), List.copyOf(stacks)));
    }

    public synchronized Money owed(UUID player) {
        Batch batch = owed.get(player);
        return batch == null ? Money.ZERO : batch.total();
    }

    public synchronized Money owedByAll() {
        Money total = Money.ZERO;
        for (Batch batch : owed.values()) {
            total = total.plus(batch.total());
        }
        return total;
    }

    public synchronized Map<UUID, Batch> drain() {
        Map<UUID, Batch> all = Map.copyOf(owed);
        owed.clear();
        return all;
    }

    public synchronized Optional<Batch> drain(UUID player) {
        return Optional.ofNullable(owed.remove(player));
    }
}
