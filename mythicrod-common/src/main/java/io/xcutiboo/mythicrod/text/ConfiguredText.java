package io.xcutiboo.mythicrod.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Parses text owned by configuration files, language files, or in-game editors.
 *
 * <p>Admins are allowed to type imperfect MiniMessage while configuring the
 * plugin. A malformed line should not break a menu, command, or reward preview;
 * it should fall back to plain visible text so the bad value can be corrected.
 */
public final class ConfiguredText {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private ConfiguredText() {
    }

    public static Component parse(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        try {
            return MINI_MESSAGE.deserialize(MiniMessageMigrator.migrateWithSerializer(text));
        } catch (Exception | LinkageError _) {
            return Component.text(text);
        }
    }
}
