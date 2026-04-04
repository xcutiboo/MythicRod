package io.xcutiboo.mythicrod.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiniMessageMigrator {
    
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    
    private static final Map<Character, String> COLOR_MAP = new HashMap<>();
    private static final Map<Character, String> FORMAT_MAP = new HashMap<>();
    
    static {
        COLOR_MAP.put('0', "<black>");
        COLOR_MAP.put('1', "<dark_blue>");
        COLOR_MAP.put('2', "<dark_green>");
        COLOR_MAP.put('3', "<dark_aqua>");
        COLOR_MAP.put('4', "<dark_red>");
        COLOR_MAP.put('5', "<dark_purple>");
        COLOR_MAP.put('6', "<gold>");
        COLOR_MAP.put('7', "<gray>");
        COLOR_MAP.put('8', "<dark_gray>");
        COLOR_MAP.put('9', "<blue>");
        COLOR_MAP.put('a', "<green>");
        COLOR_MAP.put('b', "<aqua>");
        COLOR_MAP.put('c', "<red>");
        COLOR_MAP.put('d', "<light_purple>");
        COLOR_MAP.put('e', "<yellow>");
        COLOR_MAP.put('f', "<white>");
        
        FORMAT_MAP.put('k', "<obfuscated>");
        FORMAT_MAP.put('l', "<bold>");
        FORMAT_MAP.put('m', "<strikethrough>");
        FORMAT_MAP.put('n', "<underline>");
        FORMAT_MAP.put('o', "<italic>");
        FORMAT_MAP.put('r', "<reset>");
    }
    
    public static boolean containsLegacyCodes(String input) {
        if (input == null || input.isEmpty()) return false;
        return input.contains("&") && input.matches(".*&[0-9a-fA-Fk-oK-OrR].*") || 
               input.contains("&#") && input.matches(".*&#[0-9a-fA-F]{6}.*");
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
        
        try {
            Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(legacy);
            return MINI_MESSAGE.serialize(component);
        } catch (Exception e) {
            return migrate(legacy);
        }
    }
    
    private static String convertHexColors(String input) {
        Pattern hexPattern = Pattern.compile("&#([0-9a-fA-F]{6})");
        Matcher matcher = hexPattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String hex = matcher.group(1).toLowerCase(java.util.Locale.ROOT);
            matcher.appendReplacement(sb, "<#" + hex + ">");
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    private static String convertLegacyCodes(String input) {
        StringBuilder result = new StringBuilder();
        char[] chars = input.toCharArray();
        
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '&' && i + 1 < chars.length) {
                char code = chars[i + 1];
                String replacement = null;
                
                if (COLOR_MAP.containsKey(Character.toLowerCase(code))) {
                    replacement = COLOR_MAP.get(Character.toLowerCase(code));
                }
                else if (FORMAT_MAP.containsKey(Character.toLowerCase(code))) {
                    replacement = FORMAT_MAP.get(Character.toLowerCase(code));
                }
                
                if (replacement != null) {
                    result.append(replacement);
                    i++; // Skip the code character
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
