package de.raindancer.modules.economy.service;

import de.raindancer.core.ui.statue.StatueBoard;
import de.raindancer.core.ui.statue.StatueBoards;
import de.raindancer.core.ui.statue.Statues;
import de.raindancer.modules.economy.EconomySettings;
import de.raindancer.modules.economy.model.Board;
import de.raindancer.modules.economy.model.Standing;
import org.bukkit.plugin.Plugin;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Economy's rankings as Core's statue boards: the biggest casino winners, the richest players, who played longest.
 * The statues themselves are Core's since 0.78.0 ({@link Statues}); this says who stands on them. Offered while the
 * statues switch is on. The statues Economy kept before are handed to Core once ({@link #handOver}).
 */
public final class StatueBoardService implements IEconomyService {

    /** Under this name Core remembers Economy's statues as taken over. */
    public static final String SOURCE = "economy";
    private static final List<Board> RANKINGS = List.of(Board.CASINO, Board.RICHEST, Board.PLAYTIME);

    private final Plugin plugin;
    private final Map<Board, StatueBoard> boards = new EnumMap<>(Board.class);
    private volatile EconomySettings settings;
    private boolean offered;

    /** @param standings a ranking's people, best first ({@link LeaderboardService#standings}) */
    public StatueBoardService(Plugin plugin, Function<Board, List<Standing>> standings, EconomySettings settings) {
        this.plugin = plugin;
        this.settings = settings == null ? EconomySettings.DEFAULTS : settings;
        for (Board board : RANKINGS) {
            boards.put(board, StatueBoard.ranking(board.id(), board.heading(), caption(board), ranks(board),
                    () -> standings.apply(board).stream().map(each -> new StatueBoard.Standing(each.id(), each.name(),
                            LeaderboardDisplayService.worth(each, board, this.settings.currency()))).toList()));
        }
    }

    private static String caption(Board board) {
        return switch (board) {
            case CASINO -> "<gray>won ";
            case PLAYTIME -> "<gray>played ";
            default -> "<gray>";
        };
    }

    /** What a board ranks, for a sentence: "the #2 of the richest players". */
    private static String ranks(Board board) {
        return switch (board) {
            case CASINO -> "biggest casino winners";
            case PLAYTIME -> "players who played longest";
            default -> "richest players";
        };
    }

    public List<StatueBoard> boards() {
        return RANKINGS.stream().map(boards::get).toList();
    }

    public StatueBoard board(Board board) {
        return boards.get(board);
    }

    /** Whether {@code typed} names one of Economy's statue boards. */
    public boolean ours(String typed) {
        return Board.read(typed).filter(RANKINGS::contains).isPresent();
    }

    @Override
    public void settings(EconomySettings updated) {
        this.settings = updated == null ? EconomySettings.DEFAULTS : updated;
        switched(this.settings.statuesEnabled());
    }

    /** Offers the boards, or withdraws them; a statue whose board is withdrawn stays as it stands. */
    public synchronized void switched(boolean on) {
        if (on == offered) {
            return;
        }
        for (StatueBoard board : boards()) {
            if (on) {
                StatueBoards.offer(plugin, board);
            } else {
                StatueBoards.withdraw(board);
            }
        }
        offered = on;
    }

    /** Hands the statues Economy kept in {@code statue.podiums} to Core, which takes them over once. */
    public void handOver() {
        Statues.installed().ifPresent(statues -> statues.adopt(SOURCE, settings.statuePodiums()));
    }

    /** Withdraws the boards, for the module stopping. */
    public void close() {
        switched(false);
    }
}
