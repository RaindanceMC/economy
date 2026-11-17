package de.raindancer.modules.economy;

import de.raindancer.core.ui.preference.Preference;
import de.raindancer.modules.economy.service.AuctionService;
import de.raindancer.modules.economy.service.SidebarService;
import de.raindancer.modules.economy.service.GamblingService;
import de.raindancer.modules.economy.util.PermissionNodes;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private final EconomyServices live = mock(EconomyServices.class);
    private final EconomySettings config = mock(EconomySettings.class);
    private final GamblingService gambling = mock(GamblingService.class);
    private final Player player = mock(Player.class);
    private final java.util.UUID id = java.util.UUID.randomUUID();

    private Preference insurance() {
        when(live.config()).thenReturn(config);
        when(live.gambling()).thenReturn(gambling);
        when(player.getUniqueId()).thenReturn(id);
        return EconomyOffers.betInsurance(() -> live);
    }

    @Test
    @DisplayName("bet insurance is a switch that shows whether you are insured")
    void betInsurance() {
        Preference preference = insurance();

        assertThat(preference.id()).isEqualTo("economy:bet-insurance");
        assertThat(preference.title()).isEqualTo("Insure your bets");
        assertThat(preference.isPage()).isFalse();
        when(gambling.insures(id)).thenReturn(true);
        assertThat(preference.state(player)).isEqualTo("On");
        when(gambling.insures(id)).thenReturn(false);
        assertThat(preference.state(player)).isEqualTo("Off");
    }

    @Test
    @DisplayName("choosing the other option toggles the insurance through the service, choosing the same does nothing")
    void betInsuranceToggles() {
        Preference preference = insurance();
        when(gambling.insures(id)).thenReturn(false);

        preference.choose().accept(player, 1);
        verify(gambling, never()).toggleInsurance(any());

        preference.choose().accept(player, 0);
        verify(gambling).toggleInsurance(player);
    }

    @Test
    @DisplayName("bet insurance is offered only with gambling on, insurance offered and the gamble permission")
    void betInsuranceGates() {
        Preference preference = insurance();
        when(config.gamblingEnabled()).thenReturn(true);
        when(gambling.insuranceOffered()).thenReturn(true);
        when(player.hasPermission(PermissionNodes.GAMBLE)).thenReturn(true);
        assertThat(preference.shown().test(player)).isTrue();

        when(player.hasPermission(PermissionNodes.GAMBLE)).thenReturn(false);
        assertThat(preference.shown().test(player)).isFalse();

        when(player.hasPermission(PermissionNodes.GAMBLE)).thenReturn(true);
        when(gambling.insuranceOffered()).thenReturn(false);
        assertThat(preference.shown().test(player)).isFalse();

        when(gambling.insuranceOffered()).thenReturn(true);
        when(config.gamblingEnabled()).thenReturn(false);
        assertThat(preference.shown().test(player)).isFalse();
    }
}
