package io.xcutiboo.mythicrod.paper.gui.menus;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

import io.xcutiboo.mythicrod.paper.MythicRod;
import io.xcutiboo.mythicrod.paper.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

public class LanguageSwitchMenu extends BaseMenu {

    private static final int[] LANG_SLOTS = {
        10, 11, 12, 14, 15, 16, 19, 20, 21, 23, 24, 25
    };

    private static final String TR_LANG_PREFIX = TR_LANG_PREFIX;

    public LanguageSwitchMenu(MythicRod plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected int getSize() {
        return 27;
    }

    @Override
    protected String getTitle() {
        return tr("gui.language.title");
    }

    @Override
    protected void build() {
        fillBorder(Material.PURPLE_STAINED_GLASS_PANE);

        String currentLang = plugin.getLanguageManager().getEffectivePlayerLanguage(playerUuid);
        List<String> locales = new ArrayList<>(plugin.getLanguageManager().getAvailableLanguages());
        locales.sort(Comparator.naturalOrder());

        placeInfoItem(currentLang);
        placeLocaleHeads(locales, currentLang);
        placeBackButton();
    }

    private void placeInfoItem(String currentLang) {
        ItemStack info = new ItemBuilder(Material.BOOK)
            .name(tr("gui.language.info.name"))
            .lore(
                tr("gui.language.info.select"),
                "",
                tr("language.current", Map.of("language", formatLanguageName(currentLang)))
            )
            .build();
        setItem(13, info);
    }

    private void placeLocaleHeads(List<String> locales, String currentLang) {
        int slotIndex = 0;
        for (String locale : locales) {
            if (slotIndex >= LANG_SLOTS.length) break;
            ItemStack head = createLanguageHead(locale, currentLang.equalsIgnoreCase(locale));
            setActionItem(LANG_SLOTS[slotIndex], head, () ->
                switchLanguage(locale, formatLanguageName(locale)));
            slotIndex++;
        }
    }

    private void placeBackButton() {
        ItemStack backItem = new ItemBuilder(Material.ARROW)
            .name(tr("gui.language.back.name"))
            .lore(tr("gui.language.back.lore"))
            .build();
        setNavigationItem(22, backItem, "config");
    }

    private ItemStack createLanguageHead(String locale, boolean isCurrent) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        Component name = resolveDisplayName(locale).decorate(TextDecoration.BOLD);

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
        loreLines.add(resolveDescription(locale));
        loreLines.add(resolveRegion(locale));
        loreLines.add(Component.empty());
        if (isCurrent) {
            loreLines.add(trComponent("gui.language.separator"));
            loreLines.add(trComponent("gui.language.status.active"));
            loreLines.add(trComponent("gui.language.separator"));
        } else {
            loreLines.add(trComponent("gui.language.status.inactive"));
        }
        meta.lore(loreLines);

        applyTexture(meta, textureFor(locale));
        if (isCurrent) {
            meta.setEnchantmentGlintOverride(true);
        }
        head.setItemMeta(meta);
        return head;
    }

    private String textureFor(String locale) {
        return LocaleVisuals.KNOWN_TEXTURES.getOrDefault(locale, LocaleVisuals.DEFAULT_TEXTURE);
    }

    private Component resolveDisplayName(String locale) {
        String key = TR_LANG_PREFIX + curatedKey(locale) + ".name";
        String value = plugin.getLanguageManager().tr(key);
        if (value.equals(key)) {
            return Component.text(locale.toUpperCase(Locale.ROOT));
        }
        return trComponent(key);
    }

    private Component resolveDescription(String locale) {
        String key = TR_LANG_PREFIX + curatedKey(locale) + ".description";
        String value = plugin.getLanguageManager().tr(key);
        if (value.equals(key)) {
            return Component.text(plugin.getLanguageManager().tr(
                "gui.language.languages.generic.description",
                Map.of("locale", locale)));
        }
        return trComponent(key);
    }

    private Component resolveRegion(String locale) {
        String key = TR_LANG_PREFIX + curatedKey(locale) + ".region";
        String value = plugin.getLanguageManager().tr(key);
        if (value.equals(key)) {
            return Component.text(plugin.getLanguageManager().tr(
                "gui.language.languages.generic.region",
                Map.of("locale", locale)));
        }
        return trComponent(key);
    }

    /// Lookup key the language YAML uses for curated entries
    /// (e.g. `en_US` → `english`, `ja_JP` → `japanese`). Falls back to the
    /// raw locale lowercased so unknown locales still get a stable key path.
    private String curatedKey(String locale) {
        return switch (locale) {
            case "en_US" -> "english";
            case "ja_JP" -> "japanese";
            case "de_DE" -> "german";
            case "es_ES" -> "spanish";
            case "fr_FR" -> "french";
            case "pt_BR" -> "portuguese";
            case "ru_RU" -> "russian";
            case "zh_CN" -> "chinese_simplified";
            case "zh_TW" -> "chinese_traditional";
            case "ko_KR" -> "korean";
            case "it_IT" -> "italian";
            case "nl_NL" -> "dutch";
            case "pl_PL" -> "polish";
            case "tr_TR" -> "turkish";
            case "uk_UA" -> "ukrainian";
            default -> locale.toLowerCase(Locale.ROOT);
        };
    }

    private void applyTexture(SkullMeta meta, String textureUrl) {
        try {
            if (!isValidTextureUrl(textureUrl)) {
                plugin.getLogger().log(Level.WARNING, () -> "Invalid texture URL format: " + textureUrl);
                return;
            }
            PlayerProfile profile = Bukkit.getServer().createProfile(UUID.randomUUID());
            String textureJson = String.format(
                "{\"textures\":{\"SKIN\":{\"url\":\"%s\"}}}",
                textureUrl
            );
            String encoded = Base64.getEncoder().encodeToString(textureJson.getBytes(StandardCharsets.UTF_8));
            profile.setProperty(new ProfileProperty("textures", encoded));
            meta.setPlayerProfile(profile);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to apply language head texture", e);
        }
    }

    private boolean isValidTextureUrl(String url) {
        return url != null && url.startsWith("http://textures.minecraft.net/texture/");
    }

    private void switchLanguage(String langCode, String displayName) {
        try {
            plugin.getLanguageManager().setPlayerLanguage(getPlayer().getUniqueId(), langCode);
            sendMessage(tr("gui.language.changed", Map.of("name", displayName)));
            sendMessage(tr("gui.language.changed-info", Map.of("name", displayName)));
            refresh();
            playSuccessSound();
        } catch (Exception e) {
            sendMessage(tr("gui.language.failed"));
            plugin.getLogger().log(
                Level.SEVERE,
                e,
                () -> "Failed to change language to " + langCode + " for " + getPlayer().getUniqueId()
            );
            playErrorSound();
        }
    }

    private String formatLanguageName(String code) {
        String key = "language.names." + code;
        String value = plugin.getLanguageManager().trInLanguage(key, code);
        return value.equals(key) ? code : value;
    }
}
