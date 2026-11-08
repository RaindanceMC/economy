package de.raindancer.modules.economy.service;

import de.raindancer.core.social.economy.Money;
import de.raindancer.core.ui.scoreboard.Scoreboards;
import de.raindancer.core.ui.scoreboard.SidebarSections;
import de.raindancer.modules.economy.EconomySettings;
import de.raindancer.modules.economy.model.Account;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SidebarRichestTest {

    private final SidebarService sidebar = new SidebarService(mock(RainEconomy.class), mock(LeaderboardService.class),
            mock(Scoreboards.class), EconomySettings.DEFAULTS);

    @AfterEach
    void clear() {
        sidebar.unregisterSections();
    }

    private static List<Account> accounts(int count) {
        List<Account> all = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Account account = mock(Account.class);
            when(account.name()).thenReturn("Player" + i);
            when(account.balance()).thenReturn(Money.of(1000 - i));
            all.add(account);
        }
        return all;
    }

    @Test
    @DisplayName("the richest list as many as the player chose, up to ten, and never more than there are")
    void sized() {
        sidebar.showTop(accounts(10));
        assertThat(sidebar.richestLines(UUID.randomUUID(), 2)).hasSize(3);
        assertThat(sidebar.richestLines(UUID.randomUUID(), 10)).hasSize(11);
        sidebar.showTop(accounts(4));
        assertThat(sidebar.richestLines(UUID.randomUUID(), 10)).hasSize(5);
    }

    @Test
    @DisplayName("the section is one players size themselves, from one to ten, the server's number until they do")
    void registered() {
        sidebar.registerSections();
        SidebarSections.Section richest = SidebarSections.sections().stream()
                .filter(section -> section.owner().equals("economy-richest")).findFirst().orElseThrow();
        assertThat(richest.size()).isNotNull();
        assertThat(richest.size().max()).isEqualTo(10);
        assertThat(richest.size().fallback()).isEqualTo(Math.max(1, EconomySettings.DEFAULTS.sidebarRichest()));
    }
}
