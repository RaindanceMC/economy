package de.raindancer.modules.economy.service;

import de.raindancer.core.RainsCore;
import de.raindancer.core.content.pack.JarPack;
import de.raindancer.core.platform.log.Log;
import de.raindancer.core.ui.menu.Backdrop;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** The economy's own looks — the Auto Sell Chest, the Sell Wand — offered to Core's resource packs through {@code JarPack}. */
public final class EconomyPack {

    public static final Key ITEM_MODEL = Key.key("rainseconomy", "auto_sell_chest");
    public static final Key SELL_WAND = Key.key("rainseconomy", "sell_wand");
    public static final Key PURSE = Key.key("rainseconomy", "purse");
    public static final Key STALL_FONT = Key.key("rainseconomy", "stall");
    /** How far the stall's awning rises above the window and overhangs it at each side — see {@code art/stall}. */
    static final int STALL_OVER = 20;
    static final int STALL_SIDE = 4;
    private static final TextColor STALL_INK = TextColor.color(0xF4E4C1);
    static final String ROOT = "/de/raindancer/modules/economy/pack/";
    static final List<String> FILES = java.util.stream.Stream.concat(Stream.of(
            "pack.mcmeta",
            "assets/rainseconomy/items/auto_sell_chest.json",
            "assets/rainseconomy/textures/entity/chest/auto_sell.png",
            "assets/rainseconomy/items/sell_wand.json",
            "assets/rainseconomy/models/item/sell_wand.json",
            "assets/rainseconomy/textures/item/sell_wand.png",
            "assets/rainseconomy/font/stall.json",
            "assets/rainseconomy/items/purse.json",
            "assets/rainseconomy/models/item/purse.json",
            "assets/rainseconomy/textures/item/purse.png"),
            IntStream.rangeClosed(1, 6).mapToObj(rows -> "assets/rainseconomy/textures/font/stall/stall_" + rows + ".png"))
            .toList();
    private static final String OWNER = "Economy";
    private static final String NAME = "items";

    private EconomyPack() {
    }

    /** The shop as a merchant's stall, its awning reaching over the top of the window. */
    public static Backdrop stall(int rows) {
        return new Backdrop(STALL_FONT, "\uF801" + (char) (0xE000 + Math.max(1, Math.min(6, rows))) + "\uF802",
                STALL_INK);
    }

    /** The shop's balance icon as the stall's coin purse, for somebody wearing the pack. */
    public static ItemStack purse(ItemStack icon) {
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.setItemModel(new NamespacedKey(PURSE.namespace(), PURSE.value()));
            icon.setItemMeta(meta);
        }
        return icon;
    }

    /** Offers the pack through Core's {@code JarPack}. */
    public static void contribute(Plugin plugin, RainsCore core) {
        try {
            JarPack.offer(plugin, core.resourcePacks(), EconomyPack.class, ROOT, FILES, OWNER, NAME,
                    "The Auto Sell Chest, the Sell Wand and the shop's stall");
        } catch (IOException failure) {
            Log.of("economy").warn("The Auto Sell Chest's and the Sell Wand's looks could not be written ({}); "
                    + "both work, but look like a plain chest and a blaze rod.", failure.getMessage());
        }
    }

    public static void withdraw(RainsCore core) {
        core.resourcePacks().withdraw(OWNER.toLowerCase(java.util.Locale.ROOT) + ":" + NAME);
    }
}
