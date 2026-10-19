package de.raindancer.modules.economy.rules;

import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Stacks handed over by a vault: which make one sale per material, and which go back. */
class StackSaleRuleTest {

    private final StackSaleRule rule = new StackSaleRule();

    @Test
    @DisplayName("the plain stacks the shop buys are counted per material, in the order they came")
    void counts() {
        List<Optional<Material>> plain = List.of(Optional.of(Material.DIRT), Optional.empty(),
                Optional.of(Material.STONE), Optional.of(Material.DIRT));

        Map<Material, Integer> counts = rule.counts(plain, List.of(10, 1, 5, 64));

        assertThat(counts).containsExactly(Map.entry(Material.DIRT, 74), Map.entry(Material.STONE, 5));
    }

    @Test
    @DisplayName("a stack counts as sold only when its material was paid for")
    void soldFlags() {
        List<Optional<Material>> plain = List.of(Optional.of(Material.DIRT), Optional.empty(),
                Optional.of(Material.STONE), Optional.of(Material.DIRT));

        List<Boolean> sold = rule.sold(plain, Set.of(Material.DIRT));

        assertThat(sold).containsExactly(true, false, false, true);
    }

    @Test
    @DisplayName("nothing paid is nothing sold")
    void nothingPaid() {
        assertThat(rule.sold(List.of(Optional.of(Material.DIRT)), Set.of())).containsExactly(false);
        assertThat(rule.counts(List.of(), List.of())).isEmpty();
    }

    @Test
    @DisplayName("one line for one material, a count for several")
    void label() {
        assertThat(rule.what(Map.of(Material.OAK_LOG, 12))).isEqualTo("12 × Oak Log");
        assertThat(rule.what(new java.util.LinkedHashMap<>(Map.of(Material.DIRT, 3, Material.STONE, 4))))
                .isEqualTo("7 items");
        assertThat(Arrays.asList(rule.describe())).isNotEmpty();
    }
}
