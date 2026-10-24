package de.raindancer.modules.economy.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.raindancer.core.ui.menu.Backdrop;
import de.raindancer.modules.economy.screen.AdminMenu;
import de.raindancer.modules.economy.screen.ClientItemsMenu;
import de.raindancer.modules.economy.screen.ClientShopMenu;
import de.raindancer.modules.economy.screen.ClientTradeMenu;
import de.raindancer.modules.economy.screen.CurrencyMenu;
import de.raindancer.modules.economy.screen.EnchantMenu;
import de.raindancer.modules.economy.screen.ExperienceMenu;
import de.raindancer.modules.economy.screen.FeatureSwitchMenu;
import de.raindancer.modules.economy.screen.PacksMenu;
import de.raindancer.modules.economy.screen.PotionMenu;
import de.raindancer.modules.economy.screen.PriceEditMenu;
import de.raindancer.modules.economy.screen.RainsEnchantMenu;
import de.raindancer.modules.economy.screen.RainsItemsMenu;
import de.raindancer.modules.economy.screen.SellMenu;
import de.raindancer.modules.economy.screen.ShopEditorMenu;
import de.raindancer.modules.economy.screen.ShopItemsMenu;
import de.raindancer.modules.economy.screen.ShopMenu;
import de.raindancer.modules.economy.screen.ShopSearchMenu;
import de.raindancer.modules.economy.screen.TradeMenu;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** The shop is a merchant's stall, the owner's screens are the workshop, and the stall's paintings ship. */
class ShopLookTest {

    static final List<Class<?>> STALL = List.of(ShopMenu.class, ClientShopMenu.class, ShopItemsMenu.class,
            ShopSearchMenu.class, SellMenu.class, TradeMenu.class, ClientTradeMenu.class, ClientItemsMenu.class,
            PotionMenu.class, EnchantMenu.class, ExperienceMenu.class, PacksMenu.class, RainsEnchantMenu.class,
            RainsItemsMenu.class);
    static final List<Class<?>> WORKSHOP = List.of(AdminMenu.class, CurrencyMenu.class, ShopEditorMenu.class,
            PriceEditMenu.class, FeatureSwitchMenu.class);

    @Test
    @DisplayName("Every page of the shop and every owner's screen chooses its backdrop")
    void everyPageChooses() throws NoSuchMethodException {
        for (Class<?> page : STALL) {
            assertThat(page.getDeclaredMethod("backdrop").getReturnType()).as(page.getSimpleName())
                    .isEqualTo(Backdrop.class);
        }
        for (Class<?> page : WORKSHOP) {
            assertThat(page.getDeclaredMethod("backdrop").getReturnType()).as(page.getSimpleName())
                    .isEqualTo(Backdrop.class);
        }
    }

    @Test
    @DisplayName("The stall reaches over the window: its painting is the window plus the awning, in the stall font")
    void stallPaintings() throws Exception {
        Map<Character, JsonObject> glyphs = new HashMap<>();
        try (InputStream in = EconomyPack.class.getResourceAsStream(EconomyPack.ROOT + "assets/rainseconomy/font/stall.json")) {
            assertThat(in).isNotNull();
            JsonObject font = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonElement p : font.getAsJsonArray("providers")) {
                JsonObject provider = p.getAsJsonObject();
                if (provider.has("chars")) {
                    glyphs.put(provider.getAsJsonArray("chars").get(0).getAsString().charAt(0), provider);
                }
            }
        }
        for (int rows = 1; rows <= 6; rows++) {
            Backdrop stall = EconomyPack.stall(rows);
            assertThat(stall.font()).isEqualTo(EconomyPack.STALL_FONT);
            JsonObject provider = glyphs.get(stall.glyphs().charAt(1));
            assertThat(provider).as("rows " + rows).isNotNull();
            String file = provider.get("file").getAsString().replace("rainseconomy:", "assets/rainseconomy/textures/");
            assertThat(EconomyPack.FILES).contains(file);
            try (InputStream in = EconomyPack.class.getResourceAsStream(EconomyPack.ROOT + file)) {
                BufferedImage image = ImageIO.read(in);
                assertThat(image.getWidth()).isEqualTo(176 + 2 * EconomyPack.STALL_SIDE);
                assertThat(image.getHeight()).isEqualTo(114 + 18 * rows + EconomyPack.STALL_OVER);
                assertThat(provider.get("height").getAsInt()).isEqualTo(image.getHeight());
                assertThat(provider.get("ascent").getAsInt()).isEqualTo(13 + EconomyPack.STALL_OVER);
            }
        }
    }

    @Test
    @DisplayName("Everything the pack lists is in the jar")
    void everyFileShips() {
        assertThat(EconomyPack.FILES).contains("assets/rainseconomy/items/purse.json");
        for (String file : EconomyPack.FILES) {
            assertThat(EconomyPack.class.getResource(EconomyPack.ROOT + file)).as(file).isNotNull();
        }
    }
}
