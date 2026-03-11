package io.xcutiboo.mythicrod.gui.menus;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.gui.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
        return io.xcutiboo.mythicrod.paper.util.PaperLanguageHelper.tr(plugin.getLanguageManager(), getPlayer(), "gui.language.title");
    }
    @Override
    protected void build() {
        fillBorder();
        String currentLang = plugin.getLanguageManager().getLanguage();
        // English option (United Kingdom flag texture)
        ItemStack englishItem = createLanguageHead(
            "http://textures.minecraft.net/texture/a9edcdd7b06173d7d221c7274c86cba35730170788bb6a1db09cc6810435b92c",
            Component.text(io.xcutiboo.mythicrod.paper.util.PaperLanguageHelper.tr(plugin.getLanguageManager(), getPlayer(), "gui.language.languages.english.name")).color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD),
            List.of(
                Component.text(io.xcutiboo.mythicrod.paper.util.PaperLanguageHelper.tr(plugin.getLanguageManager(), getPlayer(), "gui.language.languages.english.description"), NamedTextColor.GRAY),
                Component.text(io.xcutiboo.mythicrod.paper.util.PaperLanguageHelper.tr(plugin.getLanguageManager(), getPlayer(), "gui.language.languages.english.region"), NamedTextColor.DARK_GRAY)
            ),
            currentLang.equals("en_US")
        );
        setItem(10, englishItem, event -> {
            switchLanguage("en_US", io.xcutiboo.mythicrod.paper.util.PaperLanguageHelper.tr(plugin.getLanguageManager(), getPlayer(), "gui.language.languages.english.name"));
        });
        // Current language info
        ItemStack infoItem = new ItemBuilder(Material.BOOK)
            .name(io.xcutiboo.mythicrod.paper.util.PaperLanguageHelper.tr(plugin.getLanguageManager(), getPlayer(), "gui.language.info.name"))
            .lore(
                io.xcutiboo.mythicrod.paper.util.PaperLanguageHelper.tr(plugin.getLanguageManager(), getPlayer(), "gui.language.info.select"),
                "",
                io.xcutiboo.mythicrod.paper.util.PaperLanguageHelper.tr(plugin.getLanguageManager(), getPlayer(), "language.current", java.util.Map.of("language", formatLanguageName(currentLang)))
            )
            .build();
        setItem(13, infoItem);
        // Japanese option (Japan flag texture)
        ItemStack japaneseItem = createLanguageHead(
            "http://textures.minecraft.net/texture/d6c2ca7238666ae1b9dd9daa3d4fc829db22609fb569312dec1fb0c8d6dd6c1d",
            Component.text(io.xcutiboo.mythicrod.paper.util.PaperLanguageHelper.tr(plugin.getLanguageManager(), getPlayer(), "gui.language.languages.japanese.name")).color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD),
            List.of(
                Component.text(io.xcutiboo.mythicrod.paper.util.PaperLanguageHelper.tr(plugin.getLanguageManager(), getPlayer(), "gui.language.languages.japanese.description"), NamedTextColor.GRAY),
                Component.text(io.xcutiboo.mythicrod.paper.util.PaperLanguageHelper.tr(plugin.getLanguageManager(), getPlayer(), "gui.language.languages.japanese.region"), NamedTextColor.DARK_GRAY)
            ),
            currentLang.equals("ja_JP")
        );
        setItem(16, japaneseItem, event -> {
            switchLanguage("ja_JP", io.xcutiboo.mythicrod.paper.util.PaperLanguageHelper.tr(plugin.getLanguageManager(), getPlayer(), "gui.language.languages.japanese.name"));
        });
        // Back button
        ItemStack backItem = new ItemBuilder(Material.ARROW)
            .name(io.xcutiboo.mythicrod.paper.util.PaperLanguageHelper.tr(plugin.getLanguageManager(), getPlayer(), "gui.language.back.name"))
            .lore(io.xcutiboo.mythicrod.paper.util.PaperLanguageHelper.tr(plugin.getLanguageManager(), getPlayer(), "gui.language.back.lore"))
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
                meta.displayName(
                    Component.text(io.xcutiboo.mythicrod.paper.util.PaperLanguageHelper.tr(plugin.getLanguageManager(), getPlayer(), "gui.language.indicator.prefix"), NamedTextColor.GREEN, TextDecoration.BOLD)
                        .append(name)
                        .append(Component.text(io.xcutiboo.mythicrod.paper.util.PaperLanguageHelper.tr(plugin.getLanguageManager(), getPlayer(), "gui.language.indicator.suffix"), NamedTextColor.GREEN, TextDecoration.BOLD))
                );
            } else {
                meta.displayName(name);
            }
            // Build lore with enhanced current language indicator
            List<Component> loreLines = new java.util.ArrayList<>();
            loreLines.add(Component.empty());
            loreLines.addAll(lore);
            loreLines.add(Component.empty());
            if (isCurrent) {
                loreLines.add(Component.text(io.xcutiboo.mythicrod.paper.util.PaperLanguageHelper.tr(plugin.getLanguageManager(), getPlayer(), "gui.language.separator"), NamedTextColor.DARK_GRAY));
                loreLines.add(Component.text(io.xcutiboo.mythicrod.paper.util.PaperLanguageHelper.tr(plugin.getLanguageManager(), getPlayer(), "gui.language.status.active"), NamedTextColor.GREEN, TextDecoration.BOLD));
                loreLines.add(Component.text(io.xcutiboo.mythicrod.paper.util.PaperLanguageHelper.tr(plugin.getLanguageManager(), getPlayer(), "gui.language.separator"), NamedTextColor.DARK_GRAY));
            } else {
                loreLines.add(Component.text(io.xcutiboo.mythicrod.paper.util.PaperLanguageHelper.tr(plugin.getLanguageManager(), getPlayer(), "gui.language.status.inactive"), NamedTextColor.YELLOW));
            }
            meta.lore(loreLines);
            // Apply custom texture
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
            String textureHash = extractTextureHash(textureUrl);
            if (textureHash == null) {
                plugin.getLogger().warning("Invalid texture URL format: " + textureUrl);
                return;
            }

            PlayerProfile profile = Bukkit.getServer().createProfile(UUID.randomUUID());
            String textureJson = String.format(
                "{\"textures\":{\"SKIN\":{\"url\":\"%s\"}}}",
                textureUrl
            );

            String encoded = java.util.Base64.getEncoder().encodeToString(textureJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            profile.setProperty(new ProfileProperty("textures", encoded));
            meta.setPlayerProfile(profile);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply language head texture: " + e.getMessage());
        }
    }
    private String extractTextureHash(String url) {
        if (url == null || !url.startsWith("http://textures.minecraft.net/texture/")) {
            return null;
        }
        return url.substring("http://textures.minecraft.net/texture/".length());
    }
    private String asPlain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
    private void switchLanguage(String langCode, String displayName) {
        try {
            // Set per-player language preference instead of global
            plugin.getLanguageManager().setPlayerLanguage(getPlayer().getUniqueId(), langCode);
            sendMessage(io.xcutiboo.mythicrod.paper.util.PaperLanguageHelper.tr(plugin.getLanguageManager(), getPlayer(), "gui.language.changed", java.util.Map.of("name", displayName)));
            sendMessage(io.xcutiboo.mythicrod.paper.util.PaperLanguageHelper.tr(plugin.getLanguageManager(), getPlayer(), "gui.language.changed-info", java.util.Map.of("name", displayName)));
            // Refresh the menu to show updated selections
            refresh();
            // Play success sound
            if (plugin.getConfigManager().useSounds()) {
                Player p = getPlayer(); if (p != null) p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            }
        } catch (Exception e) {
            sendMessage(io.xcutiboo.mythicrod.paper.util.PaperLanguageHelper.tr(plugin.getLanguageManager(), getPlayer(), "gui.language.failed"));
            plugin.getLogger().severe("Error changing language: " + e.getMessage());
            // Play error sound
            if (plugin.getConfigManager().useSounds()) {
                Player p = getPlayer(); if (p != null) p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }
    private String formatLanguageName(String code) {
        String key = "language.names." + code;
        return io.xcutiboo.mythicrod.paper.util.PaperLanguageHelper.tr(plugin.getLanguageManager(), getPlayer(), key);
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
}
