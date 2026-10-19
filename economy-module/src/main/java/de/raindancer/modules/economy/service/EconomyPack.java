package de.raindancer.modules.economy.service;

import de.raindancer.core.RainsCore;
import de.raindancer.core.content.pack.PackContribution;
import de.raindancer.core.platform.log.Log;
import de.raindancer.core.platform.util.Scheduling;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/** The economy's own looks — the Auto Sell Chest, the Sell Wand: copied out of the jar and offered to Core, which builds and sends the pack. */
public final class EconomyPack {

    public static final Key ITEM_MODEL = Key.key("rainseconomy", "auto_sell_chest");
    public static final Key SELL_WAND = Key.key("rainseconomy", "sell_wand");
    static final String ROOT = "/de/raindancer/modules/economy/pack/";
    static final List<String> FILES = List.of(
            "pack.mcmeta",
            "assets/rainseconomy/items/auto_sell_chest.json",
            "assets/rainseconomy/textures/entity/chest/auto_sell.png",
            "assets/rainseconomy/items/sell_wand.json",
            "assets/rainseconomy/models/item/sell_wand.json",
            "assets/rainseconomy/textures/item/sell_wand.png");
    private static final String OWNER = "Economy";
    private static final String NAME = "items";

    private EconomyPack() {
    }

    /** Offers the pack; a server that has already built its pack builds again, so nobody waits for a restart. */
    public static void contribute(Plugin plugin, RainsCore core) {
        Path folder = plugin.getDataFolder().toPath().resolve("pack").resolve(NAME);
        try {
            for (String file : FILES) {
                Path target = folder.resolve(file);
                Files.createDirectories(target.getParent());
                try (InputStream in = EconomyPack.class.getResourceAsStream(ROOT + file)) {
                    if (in == null) {
                        throw new IOException(file + " is missing from the jar");
                    }
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException failure) {
            Log.of("economy").warn("The Auto Sell Chest's and the Sell Wand's looks could not be written ({}); "
                    + "both work, but look like a plain chest and a blaze rod.", failure.getMessage());
            return;
        }
        var packs = core.resourcePacks();
        packs.contribute(PackContribution.of(OWNER, NAME, folder).describedAs("The Auto Sell Chest and the Sell Wand"));
        if (packs.current().isPresent()) {
            Scheduling.async(plugin, () -> packs.rebuild().ifPresent(built -> packs.sendToAll(
                    plugin.getServer().getOnlinePlayers().stream().map(Player::getUniqueId).toList())));
        }
    }

    public static void withdraw(RainsCore core) {
        core.resourcePacks().withdraw(OWNER.toLowerCase(java.util.Locale.ROOT) + ":" + NAME);
    }
}
