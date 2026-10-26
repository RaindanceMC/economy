package de.raindancer.modules.economy.command;

import de.raindancer.modules.economy.util.PermissionNodes;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EcoStatueSuggestTest {

    private final EcoCommand command = new EcoCommand(() -> null);
    private final CommandSourceStack source = mock(CommandSourceStack.class);

    EcoStatueSuggestTest() {
        CommandSender staff = mock(CommandSender.class);
        when(staff.hasPermission(PermissionNodes.ADMIN)).thenReturn(true);
        when(source.getSender()).thenReturn(staff);
    }

    @Test
    @DisplayName("/eco statue offers placing, moving and removing, then the board, then the place")
    void statue() {
        assertThat(command.suggest(source, new String[]{"statue", ""})).containsExactly("place", "move", "remove");
        assertThat(command.suggest(source, new String[]{"statue", "place", ""}))
                .containsExactly("casino", "richest", "playtime");
        assertThat(command.suggest(source, new String[]{"statue", "place", "richest", ""}))
                .containsExactly("1", "2", "3");
        assertThat(command.suggest(source, new String[]{"statue", "place", "jackpot", ""})).isEmpty();
        assertThat(command.suggest(source, new String[]{"statue", "move", "ca"})).containsExactly("casino");
        assertThat(command.suggest(source, new String[]{"statue", "move", "casino", ""}))
                .containsExactly("1", "2", "3");
        assertThat(command.suggest(source, new String[]{"statue", "remove", ""})).containsExactly("podium");
        assertThat(command.suggest(source, new String[]{"statue", "place", "richest", "1", ""})).isEmpty();
    }
}
