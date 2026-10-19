package de.raindancer.modules.economy.rules;

import de.raindancer.core.social.economy.Money;
import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GoodPriceRuleTest {

    private final GoodPriceRule rule = new GoodPriceRule();

    @Test
    @DisplayName("a made thing costs what its ingredients cost this buyer — never cheaper than crafting it")
    void sumOfIngredients() {
        Map<Material, Money> each = Map.of(Material.GOLD_INGOT, Money.of(100), Material.EMERALD, Money.of(300),
                Material.BLAZE_ROD, Money.of(150));
        Optional<Money> price = rule.price(Map.of(Material.GOLD_INGOT, 2, Material.EMERALD, 1, Material.BLAZE_ROD, 2),
                (material, count) -> Optional.ofNullable(each.get(material)).map(unit -> unit.times(count)));
        assertThat(price).contains(Money.of(800));
    }

    @Test
    @DisplayName("if the shop does not sell one of the ingredients, it does not sell the thing either")
    void missingIngredient() {
        assertThat(rule.price(Map.of(Material.GOLD_INGOT, 2, Material.NETHER_STAR, 1),
                (material, count) -> material == Material.NETHER_STAR ? Optional.empty() : Optional.of(Money.of(count))))
                .isEmpty();
        assertThat(rule.price(Map.of(), (material, count) -> Optional.of(Money.of(1)))).as("nothing in it").isEmpty();
    }

    @Test
    @DisplayName("ingredients are counted from a recipe's rows, by key letter or by material name")
    void counting() {
        assertThat(rule.count(List.of("GHG", "ECE", "GBG"), Map.of('G', Material.GOLD_BLOCK, 'H', Material.HOPPER,
                'E', Material.EMERALD, 'C', Material.CHEST, 'B', Material.EMERALD_BLOCK)))
                .containsEntry(Material.GOLD_BLOCK, 4).containsEntry(Material.EMERALD, 2).containsEntry(Material.HOPPER, 1)
                .containsEntry(Material.CHEST, 1).containsEntry(Material.EMERALD_BLOCK, 1).hasSize(5);
        assertThat(rule.countNamed(List.of("GOLD_INGOT EMERALD GOLD_INGOT", "- BLAZE_ROD -", "- BLAZE_ROD -")))
                .containsEntry(Material.GOLD_INGOT, 2).containsEntry(Material.EMERALD, 1).containsEntry(Material.BLAZE_ROD, 2)
                .hasSize(3);
    }
}
