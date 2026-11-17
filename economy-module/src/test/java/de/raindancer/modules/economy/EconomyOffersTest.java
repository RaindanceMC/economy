package de.raindancer.modules.economy;

import de.raindancer.core.ui.preference.Preference;
import de.raindancer.modules.economy.service.AuctionService;
import de.raindancer.modules.economy.service.SidebarService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EconomyOffersTest {

    @Test
    @DisplayName("the sidebar is a preference in the Economy group")
    void sidebar() {
        Preference preference = EconomyOffers.sidebar(mock(SidebarService.class));

        assertThat(preference.id()).isEqualTo("economy:sidebar");
        assertThat(preference.group()).isEqualTo("Economy");
        assertThat(preference.title()).isEqualTo("Money on the sidebar");
        assertThat(preference.isPage()).isFalse();
    }

    @Test
    @DisplayName("auction news is a preference in the Auctions group")
    void auctionNews() {
        Preference preference = EconomyOffers.auctionNews(mock(AuctionService.class));

        assertThat(preference.id()).isEqualTo("economy:auction-news");
        assertThat(preference.group()).isEqualTo("Auctions");
        assertThat(preference.title()).isEqualTo("Auction news in chat");
        assertThat(preference.isPage()).isFalse();
    }
}
