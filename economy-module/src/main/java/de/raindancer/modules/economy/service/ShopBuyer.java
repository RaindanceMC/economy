package de.raindancer.modules.economy.service;

import de.raindancer.core.social.economy.ItemBuyer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The shop, offered to the rest of the server through Core's {@code ItemSales}: a vault's "sell this page" is a
 * {@code /sell} of plain items, at the seller's prices, within the day's budgets. Here, where the ledger is, a sale is
 * settled at once; on a client server it is a question to the bank.
 */
public final class ShopBuyer implements ItemBuyer {

    private final ShopService shop;

    public ShopBuyer(ShopService shop) {
        this.shop = shop;
    }

    @Override
    public boolean buys(ItemStack stack) {
        return shop.buysPlain(stack).isPresent();
    }

    @Override
    public CompletableFuture<List<Boolean>> sell(Player seller, List<ItemStack> stacks) {
        return CompletableFuture.completedFuture(shop.sellStacks(seller, stacks));
    }

    /** The same on a server whose bank is elsewhere: each material is one sale, booked there. */
    public static ItemBuyer viaTheBank(ClientShop shop) {
        return new ItemBuyer() {
            @Override
            public boolean buys(ItemStack stack) {
                return stack != null && !stack.isEmpty() && ShopService.sellableStack(stack, stack.getType());
            }

            @Override
            public CompletableFuture<List<Boolean>> sell(Player seller, List<ItemStack> stacks) {
                return shop.sellStacks(seller, stacks);
            }
        };
    }
}
