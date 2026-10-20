package de.raindancer.modules.economy.service;

import de.raindancer.core.RainsCore;
import de.raindancer.core.content.pack.JarPack;
import de.raindancer.core.platform.log.Log;
import net.kyori.adventure.key.Key;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.util.List;

/** The economy's own looks — the Auto Sell Chest, the Sell Wand — offered to Core's resource packs through {@code JarPack}. */
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

    /** Offers the pack through Core's {@code JarPack}. */
    public static void contribute(Plugin plugin, RainsCore core) {
        try {
            JarPack.offer(plugin, core.resourcePacks(), EconomyPack.class, ROOT, FILES, OWNER, NAME,
                    "The Auto Sell Chest and the Sell Wand");
        } catch (IOException failure) {
            Log.of("economy").warn("The Auto Sell Chest's and the Sell Wand's looks could not be written ({}); "
                    + "both work, but look like a plain chest and a blaze rod.", failure.getMessage());
        }
    }

    public static void withdraw(RainsCore core) {
        core.resourcePacks().withdraw(OWNER.toLowerCase(java.util.Locale.ROOT) + ":" + NAME);
    }
}
