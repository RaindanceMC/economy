package de.raindancer.modules.economy.service;

import de.raindancer.modules.economy.EconomySettings;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SidebarTitleTest {

    private static String plain(net.kyori.adventure.text.Component text) {
        return PlainTextComponentSerializer.plainText().serialize(text);
    }

    @Test
    void untouchedTheTitleIsTheCurrency() {
        var currency = EconomySettings.DEFAULTS.currency();
        assertThat(plain(SidebarService.title("", currency)))
                .isEqualTo(plain(currency.renderSymbol()) + " " + plain(currency.renderName(true)));
    }

    @Test
    void aServerCanPutItsAddressThere() {
        assertThat(plain(SidebarService.title("hyadesmc.net", EconomySettings.DEFAULTS.currency())))
                .isEqualTo("hyadesmc.net");
    }

    @Test
    void theBalanceHeadingCanBeRenamed() {
        assertThat(SidebarService.balanceHeading("Drachmas")).isEqualTo("Drachmas");
        assertThat(SidebarService.balanceHeading(" ")).isEqualTo("Balance");
        assertThat(EconomySettings.DEFAULTS.sidebarTitle()).isEmpty();
        assertThat(EconomySettings.DEFAULTS.sidebarBalanceHeading()).isEqualTo("Balance");
    }
}
