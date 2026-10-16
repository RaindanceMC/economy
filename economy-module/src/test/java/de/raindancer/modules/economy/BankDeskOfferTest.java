package de.raindancer.modules.economy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("the bank at a banker")
class BankDeskOfferTest {

    @Test
    @DisplayName("not on offer while the economy is not running")
    void notWithoutTheEconomy() {
        assertThat(new BankDeskOffer(() -> null).available()).isFalse();
    }
}
