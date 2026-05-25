package io.xcutiboo.mythicrod.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import org.junit.jupiter.api.Test;

class MessageFormatterTest {

    @Test
    void emptyPrefixDefaultsToBlankAndIsRetained() {
        MessageFormatter formatter = new MessageFormatter(null);
        assertEquals("", formatter.getPrefix());
    }

    @Test
    void formatChatMessageReturnsEmptyForBlankInput() {
        MessageFormatter formatter = new MessageFormatter("<gold>MR</gold> ");
        assertEquals(Component.empty(), formatter.formatChatMessage(null));
        assertEquals(Component.empty(), formatter.formatChatMessage(""));
    }

    @Test
    void formatChatMessageProducesNonEmptyComponent() {
        MessageFormatter formatter = new MessageFormatter("<gold>MR</gold> ");
        Component component = formatter.formatChatMessage("<red>Hi</red>");
        assertNotEquals(Component.empty(), component);
    }

    @Test
    void placeholdersAreSubstitutedBeforeRendering() {
        MessageFormatter formatter = new MessageFormatter("");
        Component component = formatter.formatMessage(
            "Caught {item}!",
            Map.of("item", "Pearl"));
        assertNotEquals(Component.empty(), component);
    }

    @Test
    void formatLoreDisablesItalicByDefault() {
        MessageFormatter formatter = new MessageFormatter("");
        Component lore = formatter.formatLore("Mythic");
        assertFalse(lore.hasDecoration(TextDecoration.ITALIC));
    }

    @Test
    void formatLoreLinesHandlesNullAndProducesMatchingCount() {
        MessageFormatter formatter = new MessageFormatter("");
        assertTrue(formatter.formatLoreLines(null).isEmpty());
        List<Component> rendered = formatter.formatLoreLines(List.of("a", "b", "c"));
        assertEquals(3, rendered.size());
        assertNotNull(rendered.get(0));
    }

    @Test
    void formatTitleAndItemNameReturnEmptyForBlankInput() {
        MessageFormatter formatter = new MessageFormatter("");
        assertEquals(Component.empty(), formatter.formatTitle(""));
        assertEquals(Component.empty(), formatter.formatItemName(null));
    }
}
