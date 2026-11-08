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
        List<Account> top = rule.top(leaderboard.ranking(), MOST_RICHEST);
        richest = top;
        for (Player player : online) {
            if (!live.sidebarEnabled() || !SHOWN.isOn(player)) {
                scoreboards.clear(player.getUniqueId(), OWNER);
                continue;
            }
            scoreboards.show(player.getUniqueId(), OWNER, sidebarFor(player.getUniqueId(), top, live),
                    ScoreboardPriority.LOW);
        }
    }

    private volatile List<Account> richest = List.of();
    /** The most a player can ask the sidebar to list. */
    static final int MOST_RICHEST = 10;
    private volatile java.util.function.Predicate<UUID> dailyReady = id -> false;

    /** Who has a daily reward waiting — said under the balance, so nobody forgets to collect it. */
    public void dailyReady(java.util.function.Predicate<UUID> ready) {
        this.dailyReady = ready == null ? id -> false : ready;
    }

    /** The sections the economy puts on the sidebar: the balance first, the lottery and the richest at the end. */
    public void registerSections() {
        de.raindancer.core.ui.scoreboard.SidebarSections.register("economy-balance", "Balance", 0, this::balanceLines);
        de.raindancer.core.ui.scoreboard.SidebarSections.register("economy-lottery", "Lottery pot", 60, this::lotteryLines);
        de.raindancer.core.ui.scoreboard.SidebarSections.registerSized("economy-richest", "Richest", 90,
                new de.raindancer.core.ui.scoreboard.SidebarSections.Size(1, MOST_RICHEST,
                        Math.max(1, settings.sidebarRichest()), "players"), this::richestLines);
    }

    public void unregisterSections() {
        for (String owner : List.of("economy-balance", "economy-lottery", "economy-richest")) {
            de.raindancer.core.ui.scoreboard.SidebarSections.unregister(owner);
        }
    }

    private List<Component> balanceLines(UUID player) {
        Currency currency = settings.currency();
        List<Component> lines = new ArrayList<>();
        lines.add(heading("Balance"));
        lines.add(value(currency.render(economy.balance(player))));
        int place = leaderboard.placeOf(player);
        if (place > 0) {
            lines.add(MINI.deserialize("   <dark_gray>" + rule.ordinal(place) + " richest"));
        }
        if (settings.dailyEnabled() && dailyReady.test(player)) {
            lines.add(MINI.deserialize("   <yellow>Daily reward ready <dark_gray>/daily"));
        }
        return lines;
    }

    private List<Component> lotteryLines(UUID player) {
        EconomySettings live = settings;
        var jackpot = live.gameOpen(live.lotteryEnabled()) ? pot.get() : null;
        if (jackpot == null) {
            return List.of();
        }
        return List.of(heading("Lottery pot"), value(live.currency().render(jackpot)));
    }

    /** For a test: the richest as a refresh would have found them. */
    void showTop(List<Account> top) {
        richest = List.copyOf(top);
    }

    /** The richest, as many as the player chose — the section is theirs to size (Core 2.67.0). */
    List<Component> richestLines(UUID player, int count) {
        List<Account> all = richest;
        if (all.isEmpty() || count <= 0) {
            return List.of();
        }
        List<Account> top = all.subList(0, Math.min(count, all.size()));
        Currency currency = settings.currency();
        List<Component> lines = new ArrayList<>();
        lines.add(heading("Richest"));
        for (int i = 0; i < top.size(); i++) {
            Account account = top.get(i);
            lines.add(MINI.deserialize("   " + rule.colourOf(i + 1) + rule.medal(i + 1) + " <white>"
                            + MINI.escapeTags(rule.shortName(account.name(), 11)) + " <gray>")
                    .append(currency.renderCompact(account.balance())));
        }
        return lines;
    }

    /**
     * The whole sidebar is sections now — the economy's own and other plugins' — composed in the player's order
     * with the ones they hid left out (Core 2.58.0, {@code /bank sidebar}). The title is the server's.
     */
    private Sidebar sidebarFor(UUID player, List<Account> top, EconomySettings live) {
        Currency currency = live.currency();
        List<Component> lines = de.raindancer.core.ui.scoreboard.SidebarSections.compose(player,
                de.raindancer.core.ui.scoreboard.Sidebar.MAX_LINES);
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
