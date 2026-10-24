package de.raindancer.modules.economy;

import de.raindancer.core.ui.help.HelpLint;
import de.raindancer.modules.api.ModuleCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class GuideTest {

    @Test
    @DisplayName("every page of the guide reads, quotes real settings, and every command is explained")
    void theGuideIsComplete() {
        assertThat(HelpLint.of(Path.of("src/main/resources/rains-help"))
                .module("economy")
                .settings(EconomySettings.class, EconomySettings.DEFAULTS)
                .settings(SupplySettings.class, SupplySettings.DEFAULTS)
                .settings(ClientShopSettings.class, ClientShopSettings.DEFAULTS)
                .foreignSettings("core")
                .commands(new EconomyModule().commands().stream().map(ModuleCommand::name).toList())
                .problems()).isEmpty();
    }
}
