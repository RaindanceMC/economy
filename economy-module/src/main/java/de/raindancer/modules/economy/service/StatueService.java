package de.raindancer.modules.economy.service;

import com.destroystokyo.paper.profile.PlayerProfile;
import de.raindancer.core.data.settings.SettingsStore;
import de.raindancer.core.platform.util.Scheduling;
import de.raindancer.core.world.npc.Attendants;
import de.raindancer.modules.economy.EconomySettings;
import de.raindancer.modules.economy.model.Podium;
import de.raindancer.modules.economy.model.StatueSpot;
import de.raindancer.modules.economy.model.Standing;
import de.raindancer.modules.economy.rules.StatueRule;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Statues of the top three: each on its own spot with a pedestal, wearing the skin of whoever holds its place,
 * and labelled with what it ranks — placed one by one wherever staff like, or three at once as a podium (whose
 * highest statue carries the label). The spots live in the settings; everything standing on one is tagged with
 * it, so a lost piece is simply put back on the next refresh and a piece nobody owns any more is taken away.
 * Everything touching the world happens on the region that owns it.
 */
public final class StatueService implements IEconomyService {

    /** On every piece of a statue: its spot's key, then {@code #statue}, {@code #pedestal} or {@code #label}. */
    public static final NamespacedKey PIECE = Objects.requireNonNull(NamespacedKey.fromString("rainseconomy:statue"));
    /** On a statue: whose skin it wears, so a refresh changes it only when the place changes hands. */
    private static final NamespacedKey HOLDER = Objects.requireNonNull(NamespacedKey.fromString("rainseconomy:statue_holder"));
    private static final Material[] PEDESTALS = {Material.GOLD_BLOCK, Material.IRON_BLOCK, Material.WAXED_COPPER_BLOCK};
    private static final String NOBODY = "-";
    private static final double REACH_SQUARED = 25;
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final Plugin plugin;
    private final Server server;
    private final LeaderboardService leaderboard;
    private final SettingsStore<EconomySettings> store;
    private final StatueRule rule = new StatueRule();
    private final AtomicBoolean migrating = new AtomicBoolean();
    private volatile EconomySettings settings;

    public StatueService(Plugin plugin, Server server, LeaderboardService leaderboard,
                         SettingsStore<EconomySettings> store, EconomySettings settings) {
        this.plugin = plugin;
        this.server = server;
        this.leaderboard = leaderboard;
        this.store = store;
        settings(settings);
    }

    @Override
    public void settings(EconomySettings updated) {
        this.settings = updated == null ? EconomySettings.DEFAULTS : updated;
    }

    /** Every statue, podiums from before statues stood on their own read as their three. */
    public List<StatueSpot> spots() {
        return rule.read(settings.statuePodiums());
    }

    /** Puts every statue whose ground is loaded up to date; writes old podiums back as statues once. */
    public void refresh() {
        EconomySettings live = settings;
        if (!live.statuesEnabled()) {
            return;
        }
        if (rule.hasLegacy(live.statuePodiums()) && migrating.compareAndSet(false, true)) {
            write(lines -> { });
        }
        List<StatueSpot> all = rule.read(live.statuePodiums());
        Set<String> known = new HashSet<>();
        all.forEach(spot -> known.add(spot.key()));
        Map<Podium.Board, List<Standing>> tops = new EnumMap<>(Podium.Board.class);
        for (StatueSpot spot : all) {
            World world = server.getWorld(spot.world());
            if (world == null) {
                continue;
            }
            List<Standing> top = tops.computeIfAbsent(spot.board(), leaderboard::standings);
            Standing holder = spot.place() <= top.size() ? top.get(spot.place() - 1) : null;
            boolean labelled = rule.labelled(spot, all);
            Location ground = new Location(world, spot.x(), spot.y(), spot.z());
            Scheduling.region(plugin, ground, () -> {
                if (!world.isChunkLoaded(ground.getBlockX() >> 4, ground.getBlockZ() >> 4)) {
                    return;
                }
                build(spot, ground, holder, labelled, known, live);
            });
        }
    }

    private void build(StatueSpot spot, Location ground, Standing holder, boolean labelled, Set<String> known,
                       EconomySettings live) {
        List<Entity> pieces = new ArrayList<>();
        for (Entity nearby : ground.getWorld().getNearbyEntities(ground, 3, 4, 3)) {
            String tag = nearby.getPersistentDataContainer().get(PIECE, PersistentDataType.STRING);
            if (tag == null) {
                continue;
            }
            int cut = tag.lastIndexOf('#');
            if (cut < 0 || !known.contains(tag.substring(0, cut))) {
                // Left by a statue that was moved or removed while nobody was near, or by an old podium.
                nearby.remove();
            } else if (tag.startsWith(spot.key() + "#")) {
                pieces.add(nearby);
            }
        }
        String own = spot.key() + "#";
        Optional<TextDisplay> label = find(pieces, TextDisplay.class, own + "label");
        if (labelled) {
            Location above = labelAt(spot, ground);
            TextDisplay shown = label.orElseGet(() -> spawnLabel(spot, above));
            shown.text(MINI.deserialize(rule.heading(spot.board())));
            if (shown.getLocation().distanceSquared(above) > 0.01) {
                // Labels from before 0.61.2 floated on the statue's own name.
                shown.teleportAsync(above);
            }
        } else {
            label.ifPresent(Entity::remove);
        }
        if (find(pieces, BlockDisplay.class, own + "pedestal").isEmpty()) {
            spawnPedestal(ground, own + "pedestal", spot.place());
        }
        Mannequin statue = find(pieces, Mannequin.class, own + "statue")
                .orElseGet(() -> spawnStatue(standing(spot, ground), spot.yaw(), own + "statue"));
        dress(statue, spot, holder, live);
    }

    private void dress(Mannequin statue, StatueSpot spot, Standing holder, EconomySettings live) {
        statue.customName(MINI.deserialize(rule.title(spot.place(), holder == null ? null : holder.name())));
        statue.setDescription(holder == null ? Component.empty()
                : MINI.deserialize(rule.caption(spot.board()))
                        .append(LeaderboardDisplayService.worth(holder, spot.board(), live.currency())));
        String wearing = statue.getPersistentDataContainer().get(HOLDER, PersistentDataType.STRING);
        String now = holder == null ? NOBODY : holder.id().toString();
        if (!now.equals(wearing)) {
            statue.setProfile(holder == null ? Attendants.look(null) : skinOf(holder.id()));
            statue.getPersistentDataContainer().set(HOLDER, PersistentDataType.STRING, now);
        }
    }

    /** An online player's profile carries the skin already; anybody else's is looked up by id. */
    private ResolvableProfile skinOf(UUID id) {
        Player online = server.getPlayer(id);
        if (online != null) {
            PlayerProfile profile = online.getPlayerProfile();
            if (profile.hasTextures()) {
                return ResolvableProfile.resolvableProfile(profile);
            }
        }
        return ResolvableProfile.resolvableProfile().uuid(id).build();
    }

    private Location standing(StatueSpot spot, Location ground) {
        Location at = ground.clone().add(0, rule.height(spot.place()), 0);
        at.setYaw(spot.yaw());
        at.setPitch(0);
        return at;
    }

    private static List<Entity> piecesOf(StatueSpot spot, Location ground) {
        List<Entity> found = new ArrayList<>();
        for (Entity nearby : ground.getWorld().getNearbyEntities(ground, 3, 4, 3)) {
            String tag = nearby.getPersistentDataContainer().get(PIECE, PersistentDataType.STRING);
            if (tag != null && tag.startsWith(spot.key() + "#")) {
                found.add(nearby);
            }
        }
        return found;
    }

    private static <T extends Entity> Optional<T> find(List<Entity> pieces, Class<T> type, String tag) {
        for (Entity piece : pieces) {
            if (type.isInstance(piece) && tag.equals(piece.getPersistentDataContainer().get(PIECE, PersistentDataType.STRING))) {
                return Optional.of(type.cast(piece));
            }
        }
        return Optional.empty();
    }

    private Location labelAt(StatueSpot spot, Location ground) {
        return ground.clone().add(0, rule.labelHeight(spot.place()), 0);
    }

    private TextDisplay spawnLabel(StatueSpot spot, Location above) {
        return above.getWorld().spawn(above, TextDisplay.class, display -> {
            display.setBillboard(Display.Billboard.CENTER);
            display.setPersistent(true);
            display.setShadowed(true);
            display.getPersistentDataContainer().set(PIECE, PersistentDataType.STRING, spot.key() + "#label");
        });
    }

    private void spawnPedestal(Location ground, String tag, int place) {
        float height = (float) rule.height(place);
        // A display's model grows from its corner, so the block is set down half a block off the statue's centre.
        Location corner = ground.clone().add(-0.5, 0, -0.5);
        corner.setYaw(0);
        corner.setPitch(0);
        ground.getWorld().spawn(corner, BlockDisplay.class, display -> {
            display.setBlock(PEDESTALS[place - 1].createBlockData());
            display.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                    new Vector3f(1, height, 1), new AxisAngle4f()));
            display.setPersistent(true);
            display.getPersistentDataContainer().set(PIECE, PersistentDataType.STRING, tag);
        });
    }

    private static Mannequin spawnStatue(Location at, float yaw, String tag) {
        Mannequin statue = Attendants.place(at, Component.empty(), Component.empty(), Attendants.look(null),
                placed -> {
                    placed.setGravity(false);
                    placed.getPersistentDataContainer().set(PIECE, PersistentDataType.STRING, tag);
                });
        statue.setRotation(yaw, 0);
        return statue;
    }

    /**
     * A whole podium centred on the block the player stands on, the statues facing the way the player looks, turned
     * to the nearest quarter so the pedestals line up with the blocks around them.
     */
    public List<StatueSpot> place(Player player, Podium.Board board) {
        Location feet = player.getLocation();
        String group = UUID.randomUUID().toString().substring(0, 8);
        List<StatueSpot> three = rule.podium(feet.getWorld().getName(), feet.getBlockX() + 0.5, feet.getBlockY(),
                feet.getBlockZ() + 0.5, rule.facing(feet.getYaw(), 90f), board, group);
        write(lines -> three.forEach(spot -> lines.add(spot.written())));
        return three;
    }

    /** One statue for {@code place} on the block the player stands on, facing the nearest eighth of their look. */
    public StatueSpot placeOne(Player player, Podium.Board board, int place) {
        StatueSpot spot = here(player, new StatueSpot("-", 0, 0, 0, 0, board, place, StatueSpot.ALONE));
        write(lines -> lines.add(spot.written()));
        return spot;
    }

    /**
     * Brings the nearest statue of {@code board} and {@code place} in the player's world to where they stand; it
     * leaves its podium, if it had one. Whether there was one.
     */
    public boolean move(Player player, Podium.Board board, int place) {
        Location at = player.getLocation();
        Optional<StatueSpot> nearest = rule.nearest(spots(), at.getWorld().getName(), at.getX(), at.getY(),
                at.getZ(), Double.MAX_VALUE, spot -> spot.board() == board && spot.place() == place);
        if (nearest.isEmpty()) {
            return false;
        }
        StatueSpot old = nearest.get();
        StatueSpot moved = here(player, old);
        write(lines -> lines.replaceAll(line -> StatueSpot.parse(line).map(old::equals).orElse(false)
                ? moved.written() : line));
        clear(old);
        return true;
    }

    private StatueSpot here(Player player, StatueSpot spot) {
        Location feet = player.getLocation();
        StatueSpot there = spot.at(feet.getWorld().getName(), feet.getBlockX() + 0.5, feet.getBlockY(),
                feet.getBlockZ() + 0.5, rule.facing(feet.getYaw(), 45f));
        return StatueSpot.parse(there.written()).orElseThrow();
    }

    /**
     * Takes away the nearest statue within five blocks — with {@code wholePodium}, every statue of the podium it
     * stands on. Whether there was one.
     */
    public boolean remove(Player player, boolean wholePodium) {
        Location at = player.getLocation();
        List<StatueSpot> all = spots();
        Optional<StatueSpot> nearest = rule.nearest(all, at.getWorld().getName(), at.getX(), at.getY(), at.getZ(),
                REACH_SQUARED, spot -> true);
        if (nearest.isEmpty()) {
            return false;
        }
        StatueSpot hit = nearest.get();
        List<StatueSpot> gone = wholePodium && !hit.standalone()
                ? all.stream().filter(spot -> spot.group().equals(hit.group())).toList() : List.of(hit);
        write(lines -> lines.removeIf(line -> StatueSpot.parse(line).map(gone::contains).orElse(false)));
        gone.forEach(this::clear);
        return true;
    }

    private void clear(StatueSpot spot) {
        World world = server.getWorld(spot.world());
        if (world == null) {
            return;
        }
        Location there = new Location(world, spot.x(), spot.y(), spot.z());
        Scheduling.region(plugin, there, () -> piecesOf(spot, there).forEach(Entity::remove));
    }

    /**
     * Changes the list of statues and saves it, off the server's threads (the settings live in a database), then
     * builds what changed. One at a time, so two placed at once both stay. Old podiums are written as their
     * statues first, so a change always finds the lines it means.
     */
    private void write(Consumer<List<String>> change) {
        Scheduling.async(plugin, () -> {
            synchronized (this) {
                List<String> lines = new ArrayList<>(rule.migrate(store.current().statuePodiums()));
                change.accept(lines);
                store.set("statue.podiums", String.join(", ", lines));
                store.trySave();
                migrating.set(false);
            }
            refresh();
        });
    }
}
