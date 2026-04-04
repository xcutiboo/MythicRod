package io.xcutiboo.mythicrod.gui.menus;

import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

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
        return tr("gui.language.title");
    }
    @Override
    protected void build() {
        fillBorder(Material.PURPLE_STAINED_GLASS_PANE);
        String currentLang = plugin.getLanguageManager().getLanguage();
        ItemStack englishItem = createLanguageHead(
            "http://textures.minecraft.net/texture/a9edcdd7b06173d7d221c7274c86cba35730170788bb6a1db09cc6810435b92c",
            trComponent("gui.language.languages.english.name").decorate(TextDecoration.BOLD),
            List.of(
                trComponent("gui.language.languages.english.description"),
                trComponent("gui.language.languages.english.region")
            ),
            currentLang.equals("en_US")
        );
        setActionItem(10, englishItem, () -> switchLanguage("en_US", tr("gui.language.languages.english.name")));
        ItemStack infoItem = new ItemBuilder(Material.BOOK)
            .name(tr("gui.language.info.name"))
            .lore(
                tr("gui.language.info.select"),
                "",
                tr("language.current", Map.of("language", formatLanguageName(currentLang)))
            )
            .build();
        setItem(13, infoItem);
        ItemStack japaneseItem = createLanguageHead(
            "http://textures.minecraft.net/texture/d6c2ca7238666ae1b9dd9daa3d4fc829db22609fb569312dec1fb0c8d6dd6c1d",
            trComponent("gui.language.languages.japanese.name").decorate(TextDecoration.BOLD),
            List.of(
                trComponent("gui.language.languages.japanese.description"),
                trComponent("gui.language.languages.japanese.region")
            ),
            currentLang.equals("ja_JP")
        );
        setActionItem(16, japaneseItem, () -> switchLanguage("ja_JP", tr("gui.language.languages.japanese.name")));
        ItemStack backItem = new ItemBuilder(Material.ARROW)
            .name(tr("gui.language.back.name"))
            .lore(tr("gui.language.back.lore"))
            .build();
        setNavigationItem(24, backItem, "config");
    }
    private ItemStack createLanguageHead(String textureUrl, Component name, List<Component> lore, boolean isCurrent) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            if (isCurrent) {
                meta.displayName(
                    trComponent("gui.language.indicator.prefix")
                        .append(name)
                        .append(trComponent("gui.language.indicator.suffix"))
                );
            } else {
                meta.displayName(name);
            }
            List<Component> loreLines = new ArrayList<>();
            loreLines.add(Component.empty());
            loreLines.addAll(lore);
            loreLines.add(Component.empty());
            if (isCurrent) {
                loreLines.add(trComponent("gui.language.separator"));
                loreLines.add(trComponent("gui.language.status.active"));
                loreLines.add(trComponent("gui.language.separator"));
            } else {
                loreLines.add(trComponent("gui.language.status.inactive"));
            }
            meta.lore(loreLines);
            applyTexture(meta, textureUrl);
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
    private void switchLanguage(String langCode, String displayName) {
        try {
            plugin.getLanguageManager().setPlayerLanguage(getPlayer().getUniqueId(), langCode);
            sendMessage(tr("gui.language.changed", Map.of("name", displayName)));
            sendMessage(tr("gui.language.changed-info", Map.of("name", displayName)));
            refresh();
            if (plugin.getConfigManager().useSounds()) {
                Player p = getPlayer(); if (p != null) p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            }
        } catch (Exception e) {
            sendMessage(tr("gui.language.failed"));
            plugin.getLogger().severe("Error changing language: " + e.getMessage());
            if (plugin.getConfigManager().useSounds()) {
                Player p = getPlayer(); if (p != null) p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }
    private String formatLanguageName(String code) {
        String key = "language.names." + code;
        return tr(key);
    }
    /**
     * DRY: Removed local fillBorder() - now using BaseMenu.fillBorder(Material).
     */
}
