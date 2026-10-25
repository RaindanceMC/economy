package de.raindancer.modules.economy.rules;

import de.raindancer.core.social.economy.Money;
import de.raindancer.modules.economy.model.Auction;

import java.util.Comparator;
import java.util.List;

/** What the economy's screens pick and say. */
public final class ScreenLinesRule implements IEconomyRule {

    /** Auctions still running, ending soonest first. */
    public List<Auction> endingSoonest(List<Auction> auctions, long now, int size) {
        return auctions.stream().filter(auction -> auction.endsAt() > now)
                .sorted(Comparator.comparingLong(Auction::endsAt)).limit(Math.max(0, size)).toList();
    }

    public String casinoWord(Money net) {
        return net.isPositive() ? "won" : net.isNegative() ? "lost" : "even";
    }

    @Override
    public String describe() {
        return "what the economy's screens pick and say";
    }
}
