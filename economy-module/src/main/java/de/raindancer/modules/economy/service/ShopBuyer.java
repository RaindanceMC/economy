package de.raindancer.modules.economy.service;

import de.raindancer.core.social.economy.ItemBuyer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import de.raindancer.modules.economy.store.CashTags;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The shop, offered to the rest of the server through Core's {@code ItemSales}: a vault's "sell this page" sells
 * whatever a sell wand would, at the seller's prices, within the day's budgets. Here, where the ledger is, a sale is
 * settled at once; on a client server it is a question to the bank.
 */
public final class ShopBuyer implements ItemBuyer {

    private final ShopService shop;
    private final CashService cash;

    public ShopBuyer(ShopService shop, CashService cash) {
        this.shop = shop;
        this.cash = cash;
    }

    @Override
    public boolean buys(ItemStack stack) {
        return CashTags.isCash(stack) || shop.buys(stack);
    }

    /** Cash is paid in at its face value, everything else sold; one answer per stack, in the order given. */
    @Override
    public CompletableFuture<List<Boolean>> sell(Player seller, List<ItemStack> stacks) {
        List<ItemStack> money = new ArrayList<>();
        List<ItemStack> goods = new ArrayList<>();
        stacks.forEach(stack -> (CashTags.isCash(stack) ? money : goods).add(stack));
        Iterator<Boolean> paidIn = (money.isEmpty() ? List.<Boolean>of() : cash.depositStacks(seller, money)).iterator();
        Iterator<Boolean> sold = (goods.isEmpty() ? List.<Boolean>of() : shop.sellStacks(seller, goods)).iterator();
        List<Boolean> answer = new ArrayList<>(stacks.size());
        stacks.forEach(stack -> answer.add(CashTags.isCash(stack) ? paidIn.next() : sold.next()));
        return CompletableFuture.completedFuture(answer);
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
