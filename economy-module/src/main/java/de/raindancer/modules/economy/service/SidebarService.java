package de.raindancer.modules.economy.service;

import de.raindancer.core.social.economy.Currency;
import de.raindancer.core.ui.profile.PlayerSwitch;
import de.raindancer.core.ui.scoreboard.ScoreboardPriority;
import de.raindancer.core.ui.scoreboard.Scoreboards;
import de.raindancer.core.ui.scoreboard.Sidebar;
import de.raindancer.modules.economy.EconomySettings;
import de.raindancer.modules.economy.model.Account;
import de.raindancer.modules.economy.rules.LeaderboardRule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * The sidebar: your balance, your place, and the richest players. Low priority in Core's arbitration, so a
 * minigame's own sidebar wins while it runs and this one comes back after. Each player can hide it.
 */
public final class SidebarService implements IEconomyService {

    public static final PlayerSwitch SHOWN = new PlayerSwitch("rainseconomy", "sidebar", true);
    private static final String OWNER = "economy";
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final RainEconomy economy;
    private final LeaderboardService leaderboard;
    private volatile java.util.function.Supplier<de.raindancer.core.social.economy.Money> pot = () -> null;
    private final Scoreboards scoreboards;
    private final LeaderboardRule rule = new LeaderboardRule();
    private volatile EconomySettings settings;

    public SidebarService(RainEconomy economy, LeaderboardService leaderboard, Scoreboards scoreboards,
                          EconomySettings settings) {
        this.economy = economy;
        this.leaderboard = leaderboard;
        this.scoreboards = scoreboards;
        settings(settings);
    }

    @Override
    public void settings(EconomySettings updated) {
        this.settings = updated == null ? EconomySettings.DEFAULTS : updated;
    }

    /** Redraws everybody's. Core's sidebar is safe from any thread and skips what has not changed. */
    public void refresh(Collection<? extends Player> online) {
        EconomySettings live = settings;
        List<Account> top = rule.top(leaderboard.ranking(), live.sidebarRichest());
        for (Player player : online) {
            if (!live.sidebarEnabled() || !SHOWN.isOn(player)) {
                scoreboards.clear(player.getUniqueId(), OWNER);
                continue;
            }
            scoreboards.show(player.getUniqueId(), OWNER, sidebarFor(player.getUniqueId(), top, live),
                    ScoreboardPriority.LOW);
        }
    }

    private Sidebar sidebarFor(UUID player, List<Account> top, EconomySettings live) {
        Currency currency = live.currency();
        // One shape for every section: a bullet and a grey heading, the value indented under it in white, a blank
        // line before the next. The old board was headings and values in the same grey at the same indent, so the
        // eye had to read every line to find the number.
        List<Component> lines = new ArrayList<>();
        lines.add(heading("Balance"));
        lines.add(value(currency.render(economy.balance(player))));
        int place = leaderboard.placeOf(player);
        if (place > 0) {
            lines.add(MINI.deserialize("   <dark_gray>" + rule.ordinal(place) + " richest"));
        }
        // Other plugins' lines (a guild's level and bank) come right under the balance; the richest fill what is left.
        List<Component> others = de.raindancer.core.ui.scoreboard.SidebarSections.linesFor(player,
                de.raindancer.core.ui.scoreboard.Sidebar.MAX_LINES - lines.size() - 1);
        if (!others.isEmpty()) {
            lines.add(Component.empty());
            lines.addAll(others);
        }
        var jackpot = live.gameOpen(live.lotteryEnabled()) ? pot.get() : null;
        if (jackpot != null && lines.size() + 3 <= de.raindancer.core.ui.scoreboard.Sidebar.MAX_LINES) {
            lines.add(Component.empty());
            lines.add(heading("Lottery pot"));
            lines.add(value(currency.render(jackpot)));
        }
        int room = de.raindancer.core.ui.scoreboard.Sidebar.MAX_LINES - lines.size() - 2;
        if (!top.isEmpty() && room > 0) {
            lines.add(Component.empty());
            lines.add(heading("Richest"));
            for (int i = 0; i < Math.min(room, top.size()); i++) {
                Account account = top.get(i);
                lines.add(MINI.deserialize("   " + rule.colourOf(i + 1) + rule.medal(i + 1) + " <white>"
                                + MINI.escapeTags(rule.shortName(account.name(), 11)) + " <gray>")
                        .append(currency.renderCompact(account.balance())));
            }
        }
        return Sidebar.of(MINI.deserialize("<bold><gradient:" + de.raindancer.core.ui.chat.Style.brandFrom() + ":"
                + de.raindancer.core.ui.chat.Style.brandTo() + ">").append(currency.renderSymbol())
                .append(Component.text(" ")).append(currency.renderName(true)), lines);
    }

    private static Component heading(String text) {
        return MINI.deserialize("<dark_gray>▪ <gray>" + text);
    }

    private static Component value(Component money) {
        return Component.text("   ").append(money.colorIfAbsent(NamedTextColor.WHITE));
    }

    /** The lottery's pot, shown when there is a lottery. */
    public void pot(java.util.function.Supplier<de.raindancer.core.social.economy.Money> source) {
        this.pot = source;
    }

    /** Shows or hides it for one player; answers what it is now. */
    public boolean toggle(Player player) {
        boolean on = SHOWN.toggle(player);
        if (!on) {
            scoreboards.clear(player.getUniqueId(), OWNER);
        }
        return on;
    }

    public void forget(UUID player) {
        scoreboards.clear(player, OWNER);
    }
}
