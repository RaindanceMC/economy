package de.raindancer.modules.economy.service;

import com.destroystokyo.paper.profile.PlayerProfile;
import de.raindancer.core.data.settings.SettingsStore;
import de.raindancer.core.platform.util.Scheduling;
import de.raindancer.core.world.npc.Attendants;
import de.raindancer.modules.economy.EconomySettings;
import de.raindancer.modules.economy.model.Podium;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Podiums of the top three: on each a pedestal per place, a statue wearing the skin of whoever holds it, and a
 * heading over them. The podiums live in the settings; everything standing on one is tagged with it, so a lost
 * piece is simply put back on the next refresh. Everything touching the world happens on the region that owns it.
 */
public final class StatueService implements IEconomyService {

    /** On every piece of a podium: the podium's key, then {@code #place} — or {@code #heading} for the heading. */
    public static final NamespacedKey PIECE = Objects.requireNonNull(NamespacedKey.fromString("rainseconomy:statue"));
    /** On a statue: whose skin it wears, so a refresh changes it only when the place changes hands. */
    private static final NamespacedKey HOLDER = Objects.requireNonNull(NamespacedKey.fromString("rainseconomy:statue_holder"));
    private static final Material[] PEDESTALS = {Material.GOLD_BLOCK, Material.IRON_BLOCK, Material.WAXED_COPPER_BLOCK};
    private static final String NOBODY = "-";
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final Plugin plugin;
    private final Server server;
    private final LeaderboardService leaderboard;
    private final SettingsStore<EconomySettings> store;
    private final StatueRule rule = new StatueRule();
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

    public List<Podium> podiums() {
        List<Podium> read = new ArrayList<>();
        settings.statuePodiums().forEach(line -> Podium.parse(line).ifPresent(read::add));
        return read;
    }

    /** Puts every podium whose ground is loaded up to date. */
    public void refresh() {
        EconomySettings live = settings;
        if (!live.statuesEnabled()) {
            return;
        }
        for (Podium podium : podiums()) {
            World world = server.getWorld(podium.world());
            if (world == null) {
                continue;
            }
            List<Standing> top = leaderboard.standings(podium.board());
            List<Standing> three = top.subList(0, Math.min(StatueRule.PLACES, top.size()));
            Location centre = new Location(world, podium.x(), podium.y(), podium.z());
            Scheduling.region(plugin, centre, () -> {
                if (!world.isChunkLoaded(centre.getBlockX() >> 4, centre.getBlockZ() >> 4)) {
                    return;
                }
                build(podium, centre, three, live);
            });
        }
    }

    private void build(Podium podium, Location centre, List<Standing> three, EconomySettings live) {
        List<Entity> pieces = piecesOf(podium, centre);
        TextDisplay heading = find(pieces, TextDisplay.class, podium.key() + "#heading")
                .orElseGet(() -> spawnHeading(podium, centre));
        heading.text(MINI.deserialize(rule.heading(podium.board())));
        for (int place = 1; place <= StatueRule.PLACES; place++) {
            String tag = podium.key() + "#" + place;
            Location at = spot(podium, centre, place);
            int index = place - 1;
            if (find(pieces, BlockDisplay.class, tag).isEmpty()) {
                spawnPedestal(at, tag, place);
            }
            Mannequin statue = find(pieces, Mannequin.class, tag).orElseGet(() -> spawnStatue(at, podium.yaw(), tag));
            Standing holder = index < three.size() ? three.get(index) : null;
            dress(statue, place, holder, podium, live);
        }
    }

    private void dress(Mannequin statue, int place, Standing holder, Podium podium, EconomySettings live) {
        statue.customName(MINI.deserialize(rule.title(place, holder == null ? null : holder.name())));
        statue.setDescription(holder == null ? Component.empty()
                : MINI.deserialize(rule.caption(podium.board())).append(live.currency().render(holder.amount())));
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

    private Location spot(Podium podium, Location centre, int place) {
        double[] offset = rule.offset(place, podium.yaw());
        Location at = centre.clone().add(offset[0], offset[1], offset[2]);
        at.setYaw(podium.yaw());
        at.setPitch(0);
        return at;
    }

    private static List<Entity> piecesOf(Podium podium, Location centre) {
        List<Entity> found = new ArrayList<>();
        for (Entity nearby : centre.getWorld().getNearbyEntities(centre, 3, 4, 3)) {
            String tag = nearby.getPersistentDataContainer().get(PIECE, PersistentDataType.STRING);
            if (tag != null && tag.startsWith(podium.key() + "#")) {
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

    private TextDisplay spawnHeading(Podium podium, Location centre) {
        Location above = centre.clone().add(0, rule.height(1) + 2.4, 0);
        return centre.getWorld().spawn(above, TextDisplay.class, display -> {
            display.setBillboard(Display.Billboard.CENTER);
            display.setPersistent(true);
            display.setShadowed(true);
            display.getPersistentDataContainer().set(PIECE, PersistentDataType.STRING, podium.key() + "#heading");
        });
    }

    private void spawnPedestal(Location statueAt, String tag, int place) {
        float height = (float) rule.height(place);
        // A display's model grows from its corner, so the block is set down half a block off the statue's centre.
        Location corner = statueAt.clone().add(-0.5, -height, -0.5);
        corner.setYaw(0);
        corner.setPitch(0);
        statueAt.getWorld().spawn(corner, BlockDisplay.class, display -> {
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
     * A new podium centred on the block the player stands on, the statues facing the way the player looks, turned
     * to the nearest quarter so the pedestals line up with the blocks around them.
     */
    public Podium place(Player player, Podium.Board board) {
        Location feet = player.getLocation();
        float yaw = Math.round(feet.getYaw() / 90f) * 90f;
        yaw = ((yaw % 360) + 360) % 360;
        Podium podium = new Podium(feet.getWorld().getName(), feet.getBlockX() + 0.5, feet.getBlockY(),
                feet.getBlockZ() + 0.5, yaw, board);
        write(lines -> lines.add(podium.written()));
        return podium;
    }

    /** Takes away the nearest podium within a few blocks, everything on it with it; whether there was one. */
    public boolean remove(Player player) {
        Location at = player.getLocation();
        Podium nearest = null;
        double best = 25;
        for (Podium podium : podiums()) {
            double distance = podium.distanceSquared(at.getWorld().getName(), at.getX(), at.getY(), at.getZ());
            if (distance < best) {
                best = distance;
                nearest = podium;
            }
        }
        if (nearest == null) {
            return false;
        }
        Podium gone = nearest;
        write(lines -> lines.removeIf(line -> Podium.parse(line).map(gone::equals).orElse(false)));
        Location there = new Location(at.getWorld(), gone.x(), gone.y(), gone.z());
        Scheduling.region(plugin, there, () -> piecesOf(gone, there).forEach(Entity::remove));
        return true;
    }

    /**
     * Changes the list of podiums and saves it, off the server's threads (the settings live in a database), then
     * builds what changed. One at a time, so two placed at once both stay.
     */
    private void write(java.util.function.Consumer<List<String>> change) {
        Scheduling.async(plugin, () -> {
            synchronized (this) {
                List<String> lines = new ArrayList<>(store.current().statuePodiums());
                change.accept(lines);
                store.set("statue.podiums", String.join(", ", lines));
                store.trySave();
            }
            refresh();
        });
    }
}
