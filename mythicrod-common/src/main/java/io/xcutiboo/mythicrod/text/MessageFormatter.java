package io.xcutiboo.mythicrod.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MessageFormatter {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    
    private final String prefix;
    
    public MessageFormatter(String prefix) {
        this.prefix = prefix != null ? prefix : "";
    }
    
    public Component formatChatMessage(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        
        Component prefixComponent = MINI_MESSAGE.deserialize(prefix);
        Component messageComponent = MINI_MESSAGE.deserialize(message);
        
        return prefixComponent.append(messageComponent);
    }
    
    public Component formatChatMessage(String message, Map<String, String> placeholders) {
        String replaced = applyPlaceholders(message, placeholders);
        return formatChatMessage(replaced);
    }
    
    public Component formatTitle(String title) {
        if (title == null || title.isEmpty()) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(title);
    }
    
    public Component formatItemName(String name) {
        if (name == null || name.isEmpty()) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(name);
    }
    
    public Component formatLore(String loreLine) {
        if (loreLine == null || loreLine.isEmpty()) {
            return Component.empty().decoration(TextDecoration.ITALIC, false);
        }
        
        return MINI_MESSAGE.deserialize(loreLine)
            .decoration(TextDecoration.ITALIC, false);
    }
    
    public List<Component> formatLoreLines(List<String> loreLines) {
        if (loreLines == null || loreLines.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Component> components = new ArrayList<>();
        for (String line : loreLines) {
            components.add(formatLore(line));
        }
        return components;
    }
    
    public Component formatMessage(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(message);
    }
    
    public Component formatMessage(String message, Map<String, String> placeholders) {
        String replaced = applyPlaceholders(message, placeholders);
        return formatMessage(replaced);
    }
    
    private String applyPlaceholders(String message, Map<String, String> placeholders) {
        if (message == null || placeholders == null || placeholders.isEmpty()) {
            return message;
        }
        
        String result = message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }
    
    public String getPrefix() {
        return prefix;
    }
}
