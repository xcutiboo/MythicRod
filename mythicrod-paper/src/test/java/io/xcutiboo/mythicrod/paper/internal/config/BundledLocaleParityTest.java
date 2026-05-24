package io.xcutiboo.mythicrod.paper.internal.config;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Compile-time guard that every bundled locale matches `en_US.yml` in
/// both key set and placeholder set.
///
/// Crowdin imports keep targets aligned with the source, but only after a
/// sync. This test catches manual edits to bundled files that drift
/// before Crowdin gets a chance to run.
class BundledLocaleParityTest {

    private static final String SOURCE_LOCALE = "en_US";
    private static final List<String> BUNDLED_TARGETS = List.of("ja_JP");
    private static final Pattern PLACEHOLDER = Pattern.compile("%[a-zA-Z0-9_]+%");

    @Test
    void everyBundledTargetMatchesSourceKeySet() {
        Map<String, String> source = flatten(loadBundledLocale(SOURCE_LOCALE));
        Set<String> sourceKeys = new LinkedHashSet<>(source.keySet());

        for (String locale : BUNDLED_TARGETS) {
            Map<String, String> target = flatten(loadBundledLocale(locale));
            Set<String> targetKeys = target.keySet();

            List<String> missing = new ArrayList<>();
            for (String key : sourceKeys) {
                if (!targetKeys.contains(key)) missing.add(key);
            }
            List<String> extra = new ArrayList<>();
            for (String key : targetKeys) {
                if (!sourceKeys.contains(key)) extra.add(key);
            }
            missing.sort(Comparator.naturalOrder());
            extra.sort(Comparator.naturalOrder());

            assertTrue(missing.isEmpty(),
                () -> locale + " is missing " + missing.size() + " keys, first 5: "
                    + missing.subList(0, Math.min(5, missing.size())));
            assertTrue(extra.isEmpty(),
                () -> locale + " has " + extra.size() + " extra keys, first 5: "
                    + extra.subList(0, Math.min(5, extra.size())));
        }
    }

    @Test
    void placeholderTokensAreIdenticalAcrossLocales() {
        Map<String, String> source = flatten(loadBundledLocale(SOURCE_LOCALE));

        for (String locale : BUNDLED_TARGETS) {
            Map<String, String> target = flatten(loadBundledLocale(locale));

            for (Map.Entry<String, String> entry : source.entrySet()) {
                Set<String> sourceTokens = extractPlaceholders(entry.getValue());
                String localized = target.get(entry.getKey());
                if (localized == null) continue;
                Set<String> targetTokens = extractPlaceholders(localized);

                assertEquals(sourceTokens, targetTokens,
                    () -> locale + " key '" + entry.getKey()
                        + "' placeholder mismatch. source=" + sourceTokens
                        + " target=" + targetTokens);
            }
        }
    }

    private static YamlConfiguration loadBundledLocale(String locale) {
        URL url = BundledLocaleParityTest.class.getClassLoader()
            .getResource("lang/" + locale + ".yml");
        assertTrue(url != null, () -> "bundled locale not on test classpath: " + locale);
        Path path = toPath(url);
        return YamlConfiguration.loadConfiguration(path.toFile());
    }

    private static Path toPath(URL url) {
        try {
            return Paths.get(url.toURI());
        } catch (Exception exception) {
            return new File(url.getFile()).toPath();
        }
    }

    private static Map<String, String> flatten(ConfigurationSection root) {
        Map<String, String> out = new LinkedHashMap<>();
        flatten(root, "", out);
        return out;
    }

    private static void flatten(ConfigurationSection section, String prefix, Map<String, String> out) {
        for (String key : section.getKeys(false)) {
            String full = prefix.isEmpty() ? key : prefix + "." + key;
            Object value = section.get(key);
            if (value instanceof ConfigurationSection child) {
                flatten(child, full, out);
            } else if (value != null) {
                out.put(full, String.valueOf(value));
            }
        }
    }

    private static Set<String> extractPlaceholders(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        Matcher matcher = PLACEHOLDER.matcher(text);
        while (matcher.find()) {
            tokens.add(matcher.group().toLowerCase(Locale.ROOT));
        }
        return tokens;
    }
}
