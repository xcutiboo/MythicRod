package io.xcutiboo.mythicrod.text;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class MiniMessageMigrator {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern LEGACY_CODE_PATTERN = Pattern.compile("&[0-9a-fA-Fk-oK-OrR]");

    private static final Map<Character, String> COLOR_MAP = Map.ofEntries(
        Map.entry('0', "<black>"),
        Map.entry('1', "<dark_blue>"),
        Map.entry('2', "<dark_green>"),
        Map.entry('3', "<dark_aqua>"),
        Map.entry('4', "<dark_red>"),
        Map.entry('5', "<dark_purple>"),
        Map.entry('6', "<gold>"),
        Map.entry('7', "<gray>"),
        Map.entry('8', "<dark_gray>"),
        Map.entry('9', "<blue>"),
        Map.entry('a', "<green>"),
        Map.entry('b', "<aqua>"),
        Map.entry('c', "<red>"),
        Map.entry('d', "<light_purple>"),
        Map.entry('e', "<yellow>"),
        Map.entry('f', "<white>")
    );
    private static final Map<Character, String> FORMAT_MAP = Map.of(
        'k', "<obfuscated>",
        'l', "<bold>",
        'm', "<strikethrough>",
        'n', "<underline>",
        'o', "<italic>",
        'r', "<reset>"
    );

    public static boolean containsLegacyCodes(String input) {
        if (input == null || input.isEmpty()) return false;
        return LEGACY_CODE_PATTERN.matcher(input).find() || HEX_COLOR_PATTERN.matcher(input).find();
    }

    public static String migrate(String legacy) {
        if (legacy == null || legacy.isEmpty()) return legacy;

        String result = legacy;

        result = convertHexColors(result);

        result = convertLegacyCodes(result);

        return result;
    }

    public static String migrateWithSerializer(String legacy) {
        if (legacy == null || legacy.isEmpty()) return legacy;
        if (!containsLegacyCodes(legacy)) return legacy;
        if (HEX_COLOR_PATTERN.matcher(legacy).find()) return migrate(legacy);

        try {
            Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(legacy);
            return MINI_MESSAGE.serialize(component);
        } catch (Exception | LinkageError e) {
            return migrate(legacy);
        }
    }

    private static String convertHexColors(String input) {
        Matcher matcher = HEX_COLOR_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1).toLowerCase(Locale.ROOT);
            matcher.appendReplacement(sb, Matcher.quoteReplacement("<#" + hex + ">"));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private static String convertLegacyCodes(String input) {
        StringBuilder result = new StringBuilder();
        char[] chars = input.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '&' && i + 1 < chars.length) {
                char code = Character.toLowerCase(chars[i + 1]);
                String replacement = COLOR_MAP.get(code);
                if (replacement == null) {
                    replacement = FORMAT_MAP.get(code);
                }

                if (replacement != null) {
                    result.append(replacement);
                    i++;
                } else {
                    result.append(chars[i]);
                }
            } else {
                result.append(chars[i]);
            }
        }

        return result.toString();
    }
}
