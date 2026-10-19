package de.raindancer.modules.economy.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class EconomyPackTest {

    private static JsonObject json(String file) throws IOException {
        try (InputStream in = EconomyPack.class.getResourceAsStream(EconomyPack.ROOT + file)) {
            assertThat(in).as(file).isNotNull();
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    @DisplayName("every file the pack ships is in the jar")
    void everyFileShips() throws IOException {
        for (String file : EconomyPack.FILES) {
            try (InputStream in = EconomyPack.class.getResourceAsStream(EconomyPack.ROOT + file)) {
                assertThat(in).as(file).isNotNull();
            }
        }
    }

    @Test
    @DisplayName("the item model the code sets is the one the pack defines, and its texture is shipped")
    void modelAndTexture() throws IOException {
        assertThat(EconomyPack.FILES).contains("assets/rainseconomy/items/auto_sell_chest.json",
                "assets/rainseconomy/textures/entity/chest/auto_sell.png");
        assertThat(EconomyPack.ITEM_MODEL.asString()).isEqualTo("rainseconomy:auto_sell_chest");
        JsonObject model = json("assets/rainseconomy/items/auto_sell_chest.json").getAsJsonObject("model");
        assertThat(model.get("type").getAsString()).isEqualTo("minecraft:special");
        assertThat(model.getAsJsonObject("model").get("texture").getAsString()).isEqualTo("rainseconomy:auto_sell");
    }

    @Test
    @DisplayName("the pack declares a format range that takes in 26.2 and 26.3 clients")
    void format() throws IOException {
        JsonObject pack = json("pack.mcmeta").getAsJsonObject("pack");
        assertThat(pack.get("min_format").getAsInt()).isLessThanOrEqualTo(88);
        assertThat(pack.get("max_format").getAsInt()).isGreaterThanOrEqualTo(97);
    }

    @Test
    @DisplayName("the Sell Wand's item points at its model, and the model at a texture the pack ships")
    void sellWand() throws IOException {
        assertThat(EconomyPack.SELL_WAND.asString()).isEqualTo("rainseconomy:sell_wand");
        JsonObject item = json("assets/rainseconomy/items/sell_wand.json").getAsJsonObject("model");
        assertThat(item.get("model").getAsString()).isEqualTo("rainseconomy:item/sell_wand");
        JsonObject model = json("assets/rainseconomy/models/item/sell_wand.json");
        for (var texture : model.getAsJsonObject("textures").entrySet()) {
            String path = "assets/rainseconomy/textures/" + texture.getValue().getAsString().split(":")[1] + ".png";
            assertThat(EconomyPack.FILES).as(texture.getKey()).contains(path);
        }
        assertThat(model.getAsJsonArray("elements")).isNotEmpty();
        assertThat(model.getAsJsonObject("display").has("gui")).isTrue();
    }
}
