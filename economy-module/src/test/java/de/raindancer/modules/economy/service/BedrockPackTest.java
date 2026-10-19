package de.raindancer.modules.economy.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** The Bedrock side of the Sell Wand: Geyser's mapping, the icon it names, and the Java model it matches. */
class BedrockPackTest {

    private static final Path BEDROCK = Path.of("bedrock");

    private static JsonObject json(Path file) throws IOException {
        return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    }

    @Test
    @DisplayName("Geyser maps the wand's Java item model to a Bedrock item whose icon the pack has a texture for")
    void wandMapsToAnIcon() throws IOException {
        JsonObject mapping = json(BEDROCK.resolve("custom_mappings/rainseconomy.json"));
        assertThat(mapping.get("format_version").getAsInt()).isEqualTo(2);
        JsonObject wand = mapping.getAsJsonObject("items").getAsJsonArray("minecraft:blaze_rod").get(0).getAsJsonObject();
        assertThat(wand.get("model").getAsString()).isEqualTo(EconomyPack.SELL_WAND.asString());
        assertThat(wand.get("bedrock_identifier").getAsString()).doesNotStartWith("minecraft:");
        assertThat(SellWandService.definition().material().getKey().asString()).isEqualTo("minecraft:blaze_rod");

        String icon = wand.getAsJsonObject("bedrock_options").get("icon").getAsString();
        JsonObject textures = json(BEDROCK.resolve("pack/textures/item_texture.json")).getAsJsonObject("texture_data");
        assertThat(textures.has(icon)).as(icon).isTrue();
        String texture = textures.getAsJsonObject(icon).getAsJsonArray("textures").get(0).getAsString();
        assertThat(BEDROCK.resolve("pack/" + texture + ".png")).exists();
    }

    @Test
    @DisplayName("the pack declares itself the way Bedrock reads it: a header and a resources module with their own ids")
    void manifest() throws IOException {
        JsonObject manifest = json(BEDROCK.resolve("pack/manifest.json"));
        String header = manifest.getAsJsonObject("header").get("uuid").getAsString();
        String module = manifest.getAsJsonArray("modules").get(0).getAsJsonObject().get("uuid").getAsString();
        assertThat(header).isNotEqualTo(module);
        assertThat(manifest.getAsJsonArray("modules").get(0).getAsJsonObject().get("type").getAsString()).isEqualTo("resources");
    }
}
