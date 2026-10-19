package de.raindancer.modules.economy.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A made thing the shop sells — the Sell Wand, the Auto Sell Chest — priced from what it is made of.
 *
 * @param item a fresh one, or empty when it cannot be made here right now (its plugin is off)
 */
public record ShopGood(String id, String name, Supplier<Optional<ItemStack>> item, Map<Material, Integer> ingredients) {

    public ShopGood {
        ingredients = Map.copyOf(ingredients);
    }
}
