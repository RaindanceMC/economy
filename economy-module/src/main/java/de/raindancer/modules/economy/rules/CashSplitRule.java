package de.raindancer.modules.economy.rules;

import de.raindancer.core.social.economy.Money;

/** How an amount handed out as cash is made up: coins up to a limit, the rest on one cheque or left on the balance. */
public final class CashSplitRule implements IEconomyRule {

    /**
     * @param coins  how many one-unit coins
     * @param cheque what the cheque is for; zero for none
     * @param kept   what stays on the balance because cheques are off
     */
    public record Split(long coins, Money cheque, Money kept) {

        /** What comes off the balance as cash. */
        public Money taken() {
            return Money.of(coins).plus(cheque);
        }
    }

    public Split split(Money amount, int mostCoins, boolean cheques) {
        if (amount == null || !amount.isPositive()) {
            return new Split(0, Money.ZERO, Money.ZERO);
        }
        long coins = Math.min(amount.minor(), Math.max(0, mostCoins));
        Money rest = amount.minus(Money.of(coins));
        return cheques ? new Split(coins, rest, Money.ZERO) : new Split(coins, Money.ZERO, rest);
    }

    @Override
    public String describe() {
        return "how cash handed out splits into coins and a cheque";
    }
}
