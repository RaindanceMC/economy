package de.raindancer.modules.economy.service;

import de.raindancer.core.social.economy.Currency;
import de.raindancer.core.social.economy.Money;
import de.raindancer.core.ui.hologram.ScreenKind;
import de.raindancer.core.ui.hologram.Screens;
import de.raindancer.core.world.time.Times;
import de.raindancer.modules.economy.EconomySettings;
import de.raindancer.modules.economy.model.Account;
import de.raindancer.modules.economy.model.Auction;
import de.raindancer.modules.economy.model.Podium;
import de.raindancer.modules.economy.rules.ScreenLinesRule;
import de.raindancer.modules.economy.store.AccountBook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.List;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** The economy's screens for Core's {@code /screen}: everybody's own money, daily, casino and tickets, and the boards. */
public final class EconomyScreens {

    public static final List<String> KINDS = List.of("balance", "daily", "casino-you", "lottery-you", "lottery",
            "auctions", "richest", "casino-winners", "most-played");
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final int AUCTIONS_SHOWN = 5;

    private final AccountBook book;
    private final LeaderboardService leaderboard;
    private final LeaderboardDisplayService boards;
    private final LotteryService lottery;
    private final AuctionService auctions;
    private final DailyService daily;
    private final Supplier<EconomySettings> settings;
    private final LongSupplier clock;
    private final ScreenLinesRule rule = new ScreenLinesRule();

    public EconomyScreens(AccountBook book, LeaderboardService leaderboard, LeaderboardDisplayService boards,
                          LotteryService lottery, AuctionService auctions, DailyService daily,
                          Supplier<EconomySettings> settings, LongSupplier clock) {
        this.book = book;
        this.leaderboard = leaderboard;
        this.boards = boards;
        this.lottery = lottery;
        this.auctions = auctions;
        this.daily = daily;
        this.settings = settings;
        this.clock = clock;
    }

    public void register(Screens screens, Plugin owner) {
        screens.register(owner, ScreenKind.personal("balance", "Your money",
                "Everybody's own balance and their place on /baltop.", Material.GOLD_INGOT, this::balance));
        screens.register(owner, ScreenKind.personal("daily", "Your daily reward",
                "Everybody sees whether their daily reward is ready, and their streak.", Material.CHEST, this::daily));
        screens.register(owner, ScreenKind.personal("casino-you", "Your casino record",
                "Everybody's own winnings or losses at the games and the lottery.", Material.EMERALD, this::casino));
        screens.register(owner, ScreenKind.personal("lottery-you", "Your lottery tickets",
                "Everybody's own tickets for the next draw.", Material.PAPER, this::tickets));
        screens.register(owner, ScreenKind.everyone("lottery", "Lottery", "The pot, the next draw and the last numbers.",
                Material.SUNFLOWER, this::lottery));
        screens.register(owner, ScreenKind.everyone("auctions", "Auctions", "What is up for auction, ending soonest first.",
                Material.BELL, this::auctions));
        screens.register(owner, ScreenKind.everyone("richest", "Richest players", "The richest players, like /baltop.",
                Material.GOLD_BLOCK, () -> boards.board(Podium.Board.RICHEST)));
        screens.register(owner, ScreenKind.everyone("casino-winners", "Biggest casino winners",
                "Who won the most at the games and the lottery, net.", Material.DIAMOND, () -> boards.board(Podium.Board.CASINO)));
        screens.register(owner, ScreenKind.everyone("most-played", "Most time played",
                "Who played longest on the network.", Material.CLOCK, () -> boards.board(Podium.Board.PLAYTIME)));
    }

    private Currency currency() {
        return settings.get().currency();
    }

    private static Component heading(String title) {
        return MINI.deserialize("<gold><bold>" + title + "</bold></gold>");
    }

    private Component balance(ScreenKind.Viewer viewer) {
        Money balance = book.balance(viewer.id());
        int place = leaderboard.placeOf(viewer.id());
        var text = Component.text().append(heading("Your money")).append(Component.newline())
                .append(currency().render(balance));
        if (place > 0) {
            text.append(Component.newline()).append(MINI.deserialize("<gray>#" + place + " on <white>/baltop"));
        }
        return text.build();
    }

    private Component daily(ScreenKind.Viewer viewer) {
        if (!settings.get().dailyEnabled()) {
            return MINI.deserialize("<gray>No daily rewards here.");
        }
        Account account = book.find(viewer.id()).orElse(null);
        boolean ready = account == null || account.dailyDay() < daily.today();
        int streak = account == null ? 0 : account.dailyStreak();
        return MINI.deserialize("<gold><bold>Daily reward</bold></gold>\n"
                + (ready ? "<green>Ready! <white>/daily" : "<gray>Next in <white>" + daily.hoursLeft() + " h")
                + (streak > 0 ? "\n<gray>Streak <white>" + streak + " day" + (streak == 1 ? "" : "s") : ""));
    }

    private Component casino(ScreenKind.Viewer viewer) {
        Money net = leaderboard.casinoNet(viewer.id());
        String word = rule.casinoWord(net);
        var text = Component.text().append(heading("Your casino record")).append(Component.newline());
        if (word.equals("even")) {
            return text.append(MINI.deserialize("<gray>All even — try your luck")).build();
        }
        return text.append(MINI.deserialize((word.equals("won") ? "<green>won " : "<red>lost ")))
                .append(currency().render(net.isNegative() ? net.negate() : net)).build();
    }

    private Component tickets(ScreenKind.Viewer viewer) {
        var mine = lottery.ticketsOf(viewer.id());
        var text = Component.text().append(heading("Your lottery tickets"));
        if (mine.isEmpty()) {
            return text.append(MINI.deserialize("\n<gray>None for this draw — <white>/lottery")).build();
        }
        mine.stream().limit(6).forEach(ticket -> text.append(MINI.deserialize("\n<white>"
                + ticket.numbers().stream().map(String::valueOf).collect(Collectors.joining(" · ")))));
        if (mine.size() > 6) {
            text.append(MINI.deserialize("\n<gray>and " + (mine.size() - 6) + " more"));
        }
        return text.append(MINI.deserialize("\n<gray>Draw in <white>" + Times.describe(lottery.untilDraw()))).build();
    }

    private Component lottery() {
        var text = Component.text().append(heading("Lottery")).append(MINI.deserialize("\n<gray>Pot "))
                .append(currency().render(lottery.pot()))
                .append(MINI.deserialize(lottery.drawing() ? "\n<yellow>Drawing now!"
                        : "\n<gray>Draw in <white>" + Times.describe(lottery.untilDraw())));
        List<Integer> last = lottery.lastDraw();
        if (last != null && !last.isEmpty()) {
            text.append(MINI.deserialize("\n<gray>Last numbers <white>"
                    + last.stream().map(String::valueOf).collect(Collectors.joining(" · "))));
        }
        return text.build();
    }

    private Component auctions() {
        long now = clock.getAsLong();
        List<Auction> running = rule.endingSoonest(auctions.auctions(), now, AUCTIONS_SHOWN);
        var text = Component.text().append(heading("Auctions"));
        if (running.isEmpty()) {
            return text.append(MINI.deserialize("\n<gray>Nothing up right now — <white>/auction")).build();
        }
        for (Auction auction : running) {
            Money price = auction.bid().isPositive() ? auction.bid() : auction.start();
            text.append(MINI.deserialize("\n<white>" + MINI.escapeTags(auction.itemName()) + "<gray> — "))
                    .append(currency().render(price))
                    .append(MINI.deserialize("<gray> · " + Times.brief(Duration.ofMillis(auction.endsAt() - now))));
        }
        return text.build();
    }
}
