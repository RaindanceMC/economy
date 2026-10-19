package de.raindancer.modules.economy.model;

import de.raindancer.core.social.economy.Money;

/** What one Auto Sell took: per stack whether it went, in order, and what it fetched together. */
public record AutoSold(boolean[] sold, Money total) {

    public boolean any() {
        for (boolean each : sold) {
            if (each) {
                return true;
            }
        }
        return false;
    }
}
