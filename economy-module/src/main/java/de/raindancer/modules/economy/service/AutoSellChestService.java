package de.raindancer.modules.economy.service;

import de.raindancer.core.content.enchant.CustomEnchantments;
import de.raindancer.core.content.enchant.RainsEnchants;
import de.raindancer.core.platform.util.Scheduling;
import de.raindancer.core.ui.chat.Brand;
import de.raindancer.core.ui.actionbar.ActionBarPriority;
import de.raindancer.core.ui.actionbar.ActionBars;
import de.raindancer.core.ui.menu.ItemSinkMenu;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.economy.model.AutoSellChests;
import de.raindancer.modules.economy.model.ChestSpot;
import de.raindancer.modules.economy.rules.AutoSellChestRule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Auto Sell on a chest: a real chest, so hoppers feed it, that sells what lands in it and pays whoever placed it.
 *
 * <p>The game draws every chest with the one chest texture, so the look is an item display wearing the chest
 * model with this module's texture, drawn a hair larger than the chest it covers. Players never open the chest
 * itself — that would swing the plain lid out through the display — but Core's item sink: what is dropped in is
 * sold on the spot and gone, and what the shop will not buy comes straight back. What a hopper puts in is sold the tick after;
 * what the shop does not buy stays, and comes out when the chest is broken.
 */
public final class AutoSellChestService {

    public static final NamespacedKey OWNER = Objects.requireNonNull(NamespacedKey.fromString("rainseconomy:auto-sell-chest"));
    private static final NamespacedKey DISPLAY = Objects.requireNonNull(NamespacedKey.fromString("rainseconomy:auto-sell-chest-display"));
    private static final float SCALE = 1.01f;

    private final Plugin plugin;
    private final Server server;
    private final ShopService shop;
    private final Messages messages;
    private final AutoSellChests chests = new AutoSellChests();
    private final AutoSellChestRule rule = new AutoSellChestRule();
    private final Brand brand;
    private final ActionBars bars;
    private final java.util.Set<ChestSpot> due = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private volatile boolean stopped;

    public AutoSellChestService(Plugin plugin, ShopService shop, Messages messages, Brand brand, ActionBars bars) {
        this.plugin = plugin;
        this.brand = brand;
        this.bars = bars;
        this.server = plugin.getServer();
        this.shop = shop;
        this.messages = messages;
    }

    // ---------------------------------------------------------------------------- the item

    public static boolean isItem(ItemStack item) {
        return item != null && item.getType() == Material.CHEST
                && CustomEnchantments.levelOn(item, RainsEnchants.AUTO_SELL) > 0;
    }

    /** Dresses an anvil's result as an Auto Sell Chest; a name typed at the anvil is kept. */
    public static ItemStack dress(ItemStack chest) {
        ItemMeta meta = chest.getItemMeta();
        meta.setItemModel(new NamespacedKey(EconomyPack.ITEM_MODEL.namespace(), EconomyPack.ITEM_MODEL.value()));
        if (!meta.hasCustomName()) {
            meta.customName(Component.text("Auto Sell Chest", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(List.of(
                Component.text("Sells what goes in, by hand or by hopper,", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("and pays whoever placed it.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        chest.setItemMeta(meta);
        return chest;
    }

    /** What a broken Auto Sell Chest drops: the chest, with the book's enchantment, dressed again. */
    public static ItemStack item() {
        ItemStack chest = new ItemStack(Material.CHEST);
        CustomEnchantments.find(RainsEnchants.AUTO_SELL).ifPresent(enchantment -> chest.addUnsafeEnchantment(enchantment, 1));
        return dress(chest);
    }

    /** The chest as the shop sells it: priced from its crafting recipe; not sold without the enchants plugin. */
    public de.raindancer.modules.economy.model.ShopGood good() {
        return new de.raindancer.modules.economy.model.ShopGood("auto-sell-chest", "Auto Sell Chest",
                () -> CustomEnchantments.find(RainsEnchants.AUTO_SELL).isPresent() ? Optional.of(item()) : Optional.empty(),
                new de.raindancer.modules.economy.rules.GoodPriceRule().count(rule.recipeShape(), rule.recipeIngredients()));
    }

    /** Whether an anvil may hand this result out: one chest becomes one Auto Sell Chest, a stack never. */
    public boolean anvilMakes(ItemStack input, ItemStack result) {
        return rule.makes(input.getType(), input.getAmount(), CustomEnchantments.levelOn(result, RainsEnchants.AUTO_SELL));
    }

    // ---------------------------------------------------------------------------- the block

    private static ChestSpot spot(Block block) {
        return new ChestSpot(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    private Optional<Block> block(ChestSpot spot) {
        World world = server.getWorld(spot.world());
        return world == null ? Optional.empty() : Optional.of(world.getBlockAt(spot.x(), spot.y(), spot.z()));
    }

    /** The owner written on this block, if it is an Auto Sell Chest. */
    public static Optional<UUID> ownerOf(BlockState state) {
        if (!(state instanceof Chest chest)) {
            return Optional.empty();
        }
        String owner = chest.getPersistentDataContainer().get(OWNER, PersistentDataType.STRING);
        try {
            return owner == null ? Optional.empty() : Optional.of(UUID.fromString(owner));
        } catch (IllegalArgumentException broken) {
            return Optional.empty();
        }
    }

    public boolean isChest(Block block) {
        return block.getType() == Material.CHEST && chests.owner(spot(block)).isPresent();
    }

    /** A just-placed chest becomes an Auto Sell Chest of this player's. */
    public void placed(Block block, Player owner) {
        if (!(block.getState() instanceof Chest chest)) {
            return;
        }
        chest.getPersistentDataContainer().set(OWNER, PersistentDataType.STRING, owner.getUniqueId().toString());
        chest.update(true, false);
        keepSingle(block);
        found(block, owner.getUniqueId());
        messages.send(owner, "economy.auto-sell-chest.placed");
    }

    /**
     * An Auto Sell Chest stays a single chest: its window is one chest's worth, and half a double chest would sell
     * the other half's contents for somebody else.
     */
    public void keepSingle(Block placed) {
        if (!(placed.getBlockData() instanceof org.bukkit.block.data.type.Chest data)
                || data.getType() == org.bukkit.block.data.type.Chest.Type.SINGLE) {
            return;
        }
        Optional<BlockFace> towards = rule.partner(data.getFacing(), data.getType());
        if (towards.isEmpty()) {
            return;
        }
        Block partner = placed.getRelative(towards.get());
        boolean involved = isChest(placed) || isChest(partner) || ownerOf(placed.getState()).isPresent();
        if (!involved) {
            return;
        }
        single(placed);
        single(partner);
    }

    private static void single(Block block) {
        if (block.getBlockData() instanceof org.bukkit.block.data.type.Chest data) {
            data.setType(org.bukkit.block.data.type.Chest.Type.SINGLE);
            block.setBlockData(data, false);
        }
    }

    /** Remembers a chest and dresses it. */
    private void found(Block block, UUID owner) {
        ChestSpot spot = spot(block);
        chests.add(spot, owner);
        removeDisplay(spot);
        BlockFace facing = block.getBlockData() instanceof org.bukkit.block.data.type.Chest data ? data.getFacing() : BlockFace.SOUTH;
        Location centre = block.getLocation().add(0.5, 0.5, 0.5);
        centre.setYaw(rule.yaw(facing));
        ItemStack look = new ItemStack(Material.CHEST);
        look.editMeta(meta -> meta.setItemModel(new NamespacedKey(EconomyPack.ITEM_MODEL.namespace(),
                EconomyPack.ITEM_MODEL.value())));
        ItemDisplay display = block.getWorld().spawn(centre, ItemDisplay.class, made -> {
            made.setPersistent(false);
            made.setItemStack(look);
            made.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            made.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                    new Vector3f(SCALE, SCALE, SCALE), new AxisAngle4f()));
            made.getPersistentDataContainer().set(DISPLAY, PersistentDataType.BYTE, (byte) 1);
        });
        chests.display(spot, display.getUniqueId());
    }

    private void removeDisplay(ChestSpot spot) {
        chests.display(spot).map(server::getEntity).ifPresent(Entity::remove);
    }

    /** The chest is gone, or about to be: forget it and take its display away. */
    public void gone(Block block) {
        ChestSpot spot = spot(block);
        removeDisplay(spot);
        chests.remove(spot);
    }

    /** A chunk came in: every Auto Sell Chest in it is remembered and dressed. Displays left from a crash go. */
    public void loaded(Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof ItemDisplay && entity.getPersistentDataContainer().has(DISPLAY)) {
                entity.remove();
            }
        }
        for (BlockState state : chunk.getTileEntities(block -> block.getType() == Material.CHEST, false)) {
            ownerOf(state).ifPresent(owner -> found(state.getBlock(), owner));
        }
    }

    public void unloaded(Chunk chunk) {
        for (ChestSpot spot : chests.all().keySet()) {
            if (spot.world().equals(chunk.getWorld().getUID()) && spot.chunkX() == chunk.getX() && spot.chunkZ() == chunk.getZ()) {
                removeDisplay(spot);
            }
        }
        chests.removeChunk(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
    }

    private NamespacedKey recipe;

    /** The recipe's key, once it is registered — for unlocking it in players' recipe books. */
    public Optional<NamespacedKey> recipe() {
        return Optional.ofNullable(recipe);
    }

    private void registerRecipe() {
        if (CustomEnchantments.find(RainsEnchants.AUTO_SELL).isEmpty()) {
            // Without the enchants plugin there is no Auto Sell, and a chest crafted here would be a plain one.
            return;
        }
        NamespacedKey key = new NamespacedKey(plugin, "auto_sell_chest");
        org.bukkit.inventory.ShapedRecipe shaped = new org.bukkit.inventory.ShapedRecipe(key, item());
        shaped.shape(rule.recipeShape().toArray(String[]::new));
        rule.recipeIngredients().forEach(shaped::setIngredient);
        server.removeRecipe(key);
        if (server.addRecipe(shaped)) {
            recipe = key;
        }
    }

    /** Every chunk already loaded when the module starts, each on its own region's thread; and the recipe. */
    public void start() {
        registerRecipe();
        for (World world : server.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                Scheduling.region(plugin, new Location(world, chunk.getX() << 4, 0, chunk.getZ() << 4), () -> {
                    if (chunk.isLoaded()) {
                        loaded(chunk);
                    }
                });
            }
        }
    }

    /** Undresses every chest. A window still open takes nothing more: what is dropped in goes back. */
    public void stop() {
        stopped = true;
        if (recipe != null) {
            server.removeRecipe(recipe);
            recipe = null;
        }
        for (ChestSpot spot : chests.all().keySet()) {
            block(spot).ifPresent(block -> Scheduling.region(plugin, block.getLocation(), () -> removeDisplay(spot)));
        }
    }

    // ---------------------------------------------------------------------------- selling

    /** What hoppers put in, sold for each chest's owner on that chest's own thread. */
    public void sweep() {
        for (Map.Entry<ChestSpot, UUID> each : chests.all().entrySet()) {
            block(each.getKey()).ifPresent(block -> Scheduling.region(plugin, block.getLocation(),
                    () -> sweep(block, each.getKey(), each.getValue())));
        }
    }

    /** A hopper put something in: sold on the next tick, once however many items arrive in it. */
    public void fed(Block block) {
        ChestSpot spot = spot(block);
        Optional<UUID> owner = chests.owner(spot);
        if (owner.isEmpty() || !due.add(spot)) {
            return;
        }
        Scheduling.regionLater(plugin, block.getLocation(), 1L, () -> {
            due.remove(spot);
            sweep(block, spot, owner.get());
        });
    }

    private void sweep(Block block, ChestSpot spot, UUID owner) {
        if (!block.getWorld().isChunkLoaded(spot.chunkX(), spot.chunkZ())) {
            return;
        }
        if (!(block.getState() instanceof Chest chest) || ownerOf(chest).isEmpty()) {
            // Gone some way no event told us about (an edit tool, a rollback).
            gone(block);
            return;
        }
        Inventory inside = chest.getBlockInventory();
        ItemStack[] contents = inside.getContents();
        List<ItemStack> stacks = new ArrayList<>();
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && !contents[i].isEmpty()) {
                stacks.add(contents[i]);
                slots.add(i);
            }
        }
        if (stacks.isEmpty()) {
            return;
        }
        boolean[] sold = shop.autoSell(owner, stacks);
        for (int i = 0; i < sold.length; i++) {
            if (sold[i]) {
                inside.setItem(slots.get(i), null);
            }
        }
    }

    /** Opens the chest's window for this player: whatever is dropped in is sold and gone. */
    public void open(Player player, Block block) {
        ChestSpot spot = spot(block);
        Optional<UUID> owner = chests.owner(spot);
        if (owner.isEmpty()) {
            return;
        }
        List<String> hint = new ArrayList<>(List.of("<gray>Whatever goes in is sold on the spot.",
                "<gray>What the shop cannot buy comes back."));
        if (!owner.get().equals(player.getUniqueId())) {
            String name = server.getOfflinePlayer(owner.get()).getName();
            hint.add("<gray>The money goes to <white>" + (name == null ? "its owner" : name) + "</white>.");
        }
        long[] items = {0};
        de.raindancer.core.social.economy.Money[] money = {de.raindancer.core.social.economy.Money.ZERO};
        new ItemSinkMenu(plugin, player, brand, null, "<dark_gray>Auto Sell Chest", hint,
                landed -> sell(player, spot, owner.get(), landed, items, money)).open();
    }

    /** What landed in the window: sold for the chest's owner, the rest handed back; the running total shown. */
    private List<ItemStack> sell(Player player, ChestSpot spot, UUID owner, List<ItemStack> landed, long[] items,
                                 de.raindancer.core.social.economy.Money[] money) {
        if (stopped || chests.owner(spot).isEmpty()) {
            return landed;
        }
        var sold = shop.autoSelling(owner, landed);
        List<ItemStack> back = new ArrayList<>();
        for (int i = 0; i < landed.size(); i++) {
            if (sold.sold()[i]) {
                items[0] += landed.get(i).getAmount();
            } else {
                back.add(landed.get(i));
            }
        }
        money[0] = money[0].plus(sold.total());
        if (sold.any()) {
            bars.show(player.getUniqueId(), "economy:auto-sell-chest", Component.text("Sold " + items[0] + " item(s) · ",
                    NamedTextColor.GREEN).append(shop.render(money[0])),
                    java.time.Duration.ofSeconds(4), ActionBarPriority.NORMAL);
        }
        if (!back.isEmpty()) {
            messages.send(player, "economy.auto-sell-chest.kept");
        }
        return back;
    }
}
