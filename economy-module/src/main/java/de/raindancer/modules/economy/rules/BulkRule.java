package de.raindancer.modules.economy.rules;

import de.raindancer.core.ui.choose.ItemSelection;
import de.raindancer.modules.economy.model.Bulk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Which items get cheaper bought in quantity, and by how much: everything that stacks and is on the owner's list
 * (by default all of it but enchanted books). What does not stack — weapons, armour, tools, books, potions — never:
 * a bulk of 128 of them could not fit into anybody's inventory.
 */
public final class BulkRule implements IEconomyRule {

    /** The most any bulk discount may be, whatever an owner writes. */
    public static final int HARD_CAP = 30;

    public Bulk forItem(String material, int stackSize, ItemSelection items, List<Bulk.Tier> tiers, int mostPercent) {
        int most = Math.clamp(mostPercent, 0, HARD_CAP);
        if (most == 0 || tiers.isEmpty() || stackSize <= 1 || !items.covers(material)) {
            return Bulk.NONE;
        }
        return new Bulk(tiers, most);
    }

    /** {@code "128 5"} — from 128, 5% off. Ascending; anything that is not two positive numbers is skipped. */
    public static List<Bulk.Tier> tiers(List<String> lines) {
        List<Bulk.Tier> read = new ArrayList<>();
        for (String line : lines == null ? List.<String>of() : lines) {
            String[] parts = line == null ? new String[0] : line.strip().split("\\s+");
            if (parts.length != 2) {
                continue;
            }
            try {
                int from = Integer.parseInt(parts[0]);
                int percent = Integer.parseInt(parts[1]);
                if (from > 0 && percent > 0) {
                    read.add(new Bulk.Tier(from, Math.min(percent, HARD_CAP)));
                }
            } catch (NumberFormatException notANumber) {
                // Skipped; the other tiers still count.
            }
        }
        read.sort(Comparator.comparingInt(Bulk.Tier::from));
        return read;
    }

    @Override
    public String describe() {
        return "how much cheaper building blocks and iron get bought in quantity, hard capped";
    }
}
