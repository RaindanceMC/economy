package de.raindancer.modules.economy.model;

import de.raindancer.core.social.economy.Money;

import java.util.UUID;

/** One player's place on a board: who, and the amount they are ranked by. */
public record Standing(UUID id, String name, Money amount) {
}
