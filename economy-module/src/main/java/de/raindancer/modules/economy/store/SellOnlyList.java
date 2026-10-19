package de.raindancer.modules.economy.store;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** The shipped {@code sell-only.yml}: one name or pattern a line, {@code - } in front or not, {@code #} for comments. */
public final class SellOnlyList {

    public static final String RESOURCE = "sell-only.yml";

    private SellOnlyList() {
    }

    public static List<String> parse(InputStream in) {
        List<String> read = new ArrayList<>();
        if (in == null) {
            return read;
        }
        try (BufferedReader lines = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            for (String line = lines.readLine(); line != null; line = lines.readLine()) {
                String text = line.strip();
                int comment = text.indexOf('#');
                if (comment >= 0) {
                    text = text.substring(0, comment).strip();
                }
                if (text.startsWith("-")) {
                    text = text.substring(1).strip();
                }
                if (!text.isEmpty()) {
                    read.add(text);
                }
            }
        } catch (IOException unreadable) {
            return read;
        }
        return read;
    }
}
