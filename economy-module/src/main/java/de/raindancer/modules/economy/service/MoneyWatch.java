package de.raindancer.modules.economy.service;

import de.raindancer.core.platform.log.LogChannel;
import de.raindancer.core.social.economy.Fees;
import de.raindancer.core.social.economy.Money;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.economy.EconomySettings;
import de.raindancer.modules.economy.model.TransactionKind;
import de.raindancer.modules.economy.rules.VelocityRule;
import de.raindancer.modules.economy.util.PermissionNodes;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.LongSupplier;

/**
 * Watches how fast money comes in and tells the economy's admins when one player gains more in ten minutes than the
 * ceiling — the first sign of a dupe, a mispriced shop or a broken reward. A line in chat and the log, nothing more:
 * what to do about it is theirs.
 */
public final class MoneyWatch implements RainEconomy.BalanceWatcher, IEconomyService {

    /** Gains within ten minutes past this many of the currency's units are worth a word. */
    public static final long CEILING_MAJOR = 50_000;

    private final Server server;
    private final Messages messages;
    private final LogChannel log;
    private final Function<UUID, String> names;
    private final LongSupplier clock;
    private final VelocityRule rule = new VelocityRule();
    private volatile EconomySettings settings;

    public MoneyWatch(Server server, Messages messages, LogChannel log, Function<UUID, String> names, LongSupplier clock,
                      EconomySettings settings) {
        this.server = server;
        this.messages = messages;
        this.log = log;
        this.names = names;
        this.clock = clock;
        settings(settings);
    }

    @Override
    public void settings(EconomySettings updated) {
        this.settings = updated == null ? EconomySettings.DEFAULTS : updated;
    }

    @Override
    public void moved(UUID who, Money delta, Money balance, TransactionKind kind) {
        Money ceiling = settings.currency().ofMajor(CEILING_MAJOR);
        rule.record(who, delta, kind, clock.getAsLong(), ceiling).ifPresent(gained -> {
            String name = names.apply(who);
            String amount = Fees.format(gained);
            log.warn("{} gained {} within ten minutes (last: {} by {}); balance now {}.", name, amount,
                    Fees.format(delta), kind.name().toLowerCase(java.util.Locale.ROOT), Fees.format(balance));
            for (Player staff : server.getOnlinePlayers()) {
                if (staff.hasPermission(PermissionNodes.ADMIN)) {
                    messages.send(staff, "economy.watch.velocity", "player", name, "amount", amount,
                            "kind", kind.label(), "balance", Fees.format(balance));
                }
            }
        });
    }

    public void forget(UUID player) {
        rule.forget(player);
    }

    public String describe() {
        return "a word to admins when one player gains faster than anybody earns";
    }
}
