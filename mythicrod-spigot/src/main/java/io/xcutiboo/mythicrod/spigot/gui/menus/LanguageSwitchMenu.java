package io.xcutiboo.mythicrod.spigot.gui.menus;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.spigot.gui.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class LanguageSwitchMenu extends BaseMenu {

    public LanguageSwitchMenu(MythicRod plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected int getSize() {
        return 27; // 3 rows
    }

    @Override
    protected String getTitle() {
        Player p = getPlayer();
        return p != null ? plugin.getLanguageManager().trForSender(getPlatformPlayer(), "gui.language.title") : "Language Selection";
    }

    @Override
    protected void build() {
        fillBorder();

        String currentLang = plugin.getLanguageManager().getLanguage();

        // English option (United Kingdom flag texture)
        ItemStack englishItem = createLanguageHead(
            "http://textures.minecraft.net/texture/a9edcdd7b06173d7d221c7274c86cba35730170788bb6a1db09cc6810435b92c",
            Component.text(plugin.getLanguageManager().trForSender(getPlatformPlayer(), "gui.language.languages.english.name")).color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD),
            List.of(
                Component.text(plugin.getLanguageManager().trForSender(getPlatformPlayer(), "gui.language.languages.english.description"), NamedTextColor.GRAY),
                Component.text(plugin.getLanguageManager().trForSender(getPlatformPlayer(), "gui.language.languages.english.region"), NamedTextColor.DARK_GRAY)
            ),
            currentLang.equals("en_US")
        );
        setItem(10, englishItem, event -> {
            switchLanguage("en_US", plugin.getLanguageManager().trForSender(getPlatformPlayer(), "gui.language.languages.english.name"));
        });

        // Current language info
        ItemStack infoItem = new ItemBuilder(Material.BOOK)
            .name(plugin.getLanguageManager().trForSender(getPlatformPlayer(), "gui.language.info.name"))
            .lore(
                plugin.getLanguageManager().trForSender(getPlatformPlayer(), "gui.language.info.select"),
                "",
                plugin.getLanguageManager().trForSender(getPlatformPlayer(), "language.current", java.util.Map.of("language", formatLanguageName(currentLang)))
            )
            .build();
        setItem(13, infoItem);

        // Japanese option (Japan flag texture)
        ItemStack japaneseItem = createLanguageHead(
            "http://textures.minecraft.net/texture/d6c2ca7238666ae1b9dd9daa3d4fc829db22609fb569312dec1fb0c8d6dd6c1d",
            Component.text(plugin.getLanguageManager().trForSender(getPlatformPlayer(), "gui.language.languages.japanese.name")).color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD),
            List.of(
                Component.text(plugin.getLanguageManager().trForSender(getPlatformPlayer(), "gui.language.languages.japanese.description"), NamedTextColor.GRAY),
                Component.text(plugin.getLanguageManager().trForSender(getPlatformPlayer(), "gui.language.languages.japanese.region"), NamedTextColor.DARK_GRAY)
            ),
            currentLang.equals("ja_JP")
        );
        setItem(16, japaneseItem, event -> {
            switchLanguage("ja_JP", plugin.getLanguageManager().trForSender(getPlatformPlayer(), "gui.language.languages.japanese.name"));
        });

        // Back button
        ItemStack backItem = new ItemBuilder(Material.ARROW)
            .name(plugin.getLanguageManager().trForSender(getPlatformPlayer(), "gui.language.back.name"))
            .lore(plugin.getLanguageManager().trForSender(getPlatformPlayer(), "gui.language.back.lore"))
            .build();
        setItem(24, backItem, event -> {
            plugin.getGUIManager().openMenu(getPlayer(), "config");
        });
    }

    private ItemStack createLanguageHead(String textureUrl, Component name, List<Component> lore, boolean isCurrent) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            // Add selection indicator to name if current
            if (isCurrent) {
                Component displayName = Component.text(plugin.getLanguageManager().trForSender(getPlatformPlayer(), "gui.language.indicator.prefix"), NamedTextColor.GREEN, TextDecoration.BOLD)
                    .append(name)
                    .append(Component.text(plugin.getLanguageManager().trForSender(getPlatformPlayer(), "gui.language.indicator.suffix"), NamedTextColor.GREEN, TextDecoration.BOLD));
                plugin.audiences().player(getPlayer()).sendMessage(displayName); // Cache for display
                meta.setDisplayName(LegacyComponentSerializer.legacySection().serialize(displayName));
            } else {
                meta.setDisplayName(LegacyComponentSerializer.legacySection().serialize(name));
            }

            // Build lore with enhanced current language indicator
            List<String> loreLines = new java.util.ArrayList<>();
            loreLines.add("");
            for (Component loreLine : lore) {
                loreLines.add(LegacyComponentSerializer.legacySection().serialize(loreLine));
            }
            loreLines.add("");
            if (isCurrent) {
                loreLines.add(LegacyComponentSerializer.legacySection().serialize(
                    Component.text(plugin.getLanguageManager().trForSender(getPlatformPlayer(), "gui.language.separator"), NamedTextColor.DARK_GRAY)));
                loreLines.add(LegacyComponentSerializer.legacySection().serialize(
                    Component.text(plugin.getLanguageManager().trForSender(getPlatformPlayer(), "gui.language.status.active"), NamedTextColor.GREEN, TextDecoration.BOLD)));
                loreLines.add(LegacyComponentSerializer.legacySection().serialize(
                    Component.text(plugin.getLanguageManager().trForSender(getPlatformPlayer(), "gui.language.separator"), NamedTextColor.DARK_GRAY)));
            } else {
                loreLines.add(LegacyComponentSerializer.legacySection().serialize(
                    Component.text(plugin.getLanguageManager().trForSender(getPlatformPlayer(), "gui.language.status.inactive"), NamedTextColor.YELLOW)));
            }
            meta.setLore(loreLines);

            // Apply custom texture (Spigot API)
            applyTexture(meta, textureUrl);

            // Add enchant glint to current language
            if (isCurrent) {
                meta.setEnchantmentGlintOverride(true);
            }

            head.setItemMeta(meta);
        }
        return head;
    }

    private void applyTexture(SkullMeta meta, String textureUrl) {
        try {
            // Extract texture hash from URL
            String textureHash = extractTextureHash(textureUrl);
            if (textureHash == null) {
                plugin.getLogger().log(java.util.logging.Level.WARNING, "[MythicRod-LanguageSwitchMenu] Invalid texture URL format for language head: " + textureUrl + ". Using default skin instead.");
                return;
            }

            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            PlayerTextures textures = profile.getTextures();

            URL url = java.net.URI.create(textureUrl).toURL();
            textures.setSkin(url);
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
        } catch (MalformedURLException e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "[MythicRod-LanguageSwitchMenu] Failed to fetch language head texture from URL: " + textureUrl + ". Reason: " + e.getMessage());
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "[MythicRod-LanguageSwitchMenu] Error applying language head texture. Using default skin instead. Check texture URL format.", e);
        }
    }

    private String extractTextureHash(String url) {
        if (url == null || !url.startsWith("http://textures.minecraft.net/texture/")) {
            return null;
        }
        return url.substring("http://textures.minecraft.net/texture/".length());
    }

    private void switchLanguage(String langCode, String displayName) {
        try {
            // Set per-player language preference instead of global
            Player p = getPlayer();
            if (p == null) return;
            plugin.getLanguageManager().setPlayerLanguage(p.getUniqueId(), langCode);

            sendMessage(plugin.getLanguageManager().trForSender(getPlatformPlayer(), "gui.language.changed", java.util.Map.of("name", displayName)));
            sendMessage(plugin.getLanguageManager().trForSender(getPlatformPlayer(), "gui.language.changed-info", java.util.Map.of("name", displayName)));

            // Refresh the menu to show updated selections
            refresh();

            // Play success sound
            if (plugin.getConfigManager().useSounds()) {
                getPlayer().playSound(getPlayer().getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            }
        } catch (Exception e) {
            sendMessage(plugin.getLanguageManager().trForSender(getPlatformPlayer(), "gui.language.failed"));
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "[MythicRod-LanguageSwitchMenu] Critical error changing language. Language may not have been updated. Check configuration files.", e);

            // Play error sound
            if (plugin.getConfigManager().useSounds()) {
                getPlayer().playSound(getPlayer().getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }

    private String formatLanguageName(String code) {
        String key = "language.names." + code;
        return plugin.getLanguageManager().trForSender(getPlatformPlayer(), key);
    }

    private void fillBorder() {
        ItemStack borderItem = new ItemBuilder(Material.PURPLE_STAINED_GLASS_PANE)
            .name(" ")
            .build();

        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            setItem(i, borderItem);
            setItem(18 + i, borderItem);
        }

        // Left and right columns
        setItem(9, borderItem);
        setItem(17, borderItem);
    }

    private void sendMessage(String message) {
        Component component = LegacyComponentSerializer.legacyAmpersand()
            .deserialize(plugin.getConfigManager().getPrefix() + message);
        plugin.audiences().player(getPlayer()).sendMessage(component);
    }
}
