package de.raindancer.modules.economy.model;

import de.raindancer.core.social.economy.Money;

import java.util.UUID;

/** One player's place on a board: who, and what they are ranked by — an amount of money or minutes played. */
public record Standing(UUID id, String name, Money amount, long minutes) {

    public Standing(UUID id, String name, Money amount) {
        this(id, name, amount, 0);
    }

    public static Standing played(UUID id, String name, long minutes) {
        return new Standing(id, name, Money.ZERO, minutes);
    }
}
