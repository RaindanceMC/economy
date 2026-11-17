package de.raindancer.modules.economy;

import de.raindancer.core.ui.preference.Preference;
import de.raindancer.modules.economy.service.AuctionService;
import de.raindancer.modules.economy.service.SidebarService;
import de.raindancer.modules.economy.util.PermissionNodes;

import java.util.List;
import java.util.function.Supplier;

/** What Economy offers to Core's {@code /preferences} page: the same switches {@code /bank sidebar} and {@code /auction mute} flip. */
final class EconomyOffers {

    private EconomyOffers() {
    }

    static Preference sidebar(SidebarService sidebar) {
        return Preference.toggle("economy:sidebar", "Economy", "Money on the sidebar",
                "Your balance on the side of the screen.",
                SidebarService.SHOWN::isOn, (player, on) -> {
                    if (on != SidebarService.SHOWN.isOn(player)) {
                        sidebar.toggle(player);
                    }
                    sidebar.refresh(List.of(player));
                });
    }

    static Preference auctionNews(AuctionService auctions) {
        return Preference.toggle("economy:auction-news", "Auctions", "Auction news in chat",
                "Chat messages when an auction starts or ends.",
                AuctionService.NEWS::isOn, (player, on) -> {
                    if (on != AuctionService.NEWS.isOn(player)) {
                        auctions.toggleNews(player);
                    }
                });
    }

    static Preference betInsurance(Supplier<EconomyServices> services) {
        return Preference.toggle("economy:bet-insurance", "Casino", "Insure your bets",
                "Every stake costs a little more, and part of a lost stake comes back.",
                player -> services.get().gambling().insures(player.getUniqueId()), (player, on) -> {
                    EconomyServices live = services.get();
                    if (on != live.gambling().insures(player.getUniqueId())) {
                        live.gambling().toggleInsurance(player);
                    }
                }).shownTo(player -> {
            EconomyServices live = services.get();
            return live != null && live.config().gamblingEnabled() && live.gambling().insuranceOffered()
                    && player.hasPermission(PermissionNodes.GAMBLE)
                    && !de.raindancer.core.social.desk.ServiceDesks.inPersonOnly(
                            de.raindancer.core.social.desk.ServiceDesk.INSURANCE);
        });
    }
}
