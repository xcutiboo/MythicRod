package io.xcutiboo.mythicrod.gui.menus;
import java.net.MalformedURLException;
import java.net.URL;
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
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
        return "&6&lMythicRod &8🌐 &7Language Selection";
    }
    @Override
    protected void build() {
        fillBorder();
        String currentLang = plugin.getLanguageManager().getLanguage();
        // English option (United Kingdom flag texture)
        ItemStack englishItem = createLanguageHead(
            "http://textures.minecraft.net/texture/a9edcdd7b06173d7d221c7274c86cba35730170788bb6a1db09cc6810435b92c",
            Component.text("English").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD),
            List.of(
                Component.text("Switch to English", NamedTextColor.GRAY),
                Component.text("United Kingdom", NamedTextColor.DARK_GRAY)
            ),
            currentLang.equals("en")
        );
        setItem(10, englishItem, event -> {
            switchLanguage("en", "English");
        });
        // Current language info
        ItemStack infoItem = new ItemBuilder(Material.BOOK)
            .name("&6&lLanguage Settings")
            .lore(
                "&7Choose your preferred language",
                "",
                "&eCurrent Language: &f" + formatLanguageName(currentLang),
                "",
                "&7Click on a language to switch.",
                "&7Changes apply instantly!",
                "",
                "&8This is the language used for:",
                "&8• Chat messages",
                "&8• GUI menus",
                "&8• Command feedback"
            )
            .build();
        setItem(13, infoItem);
        // Japanese option (Japan flag texture)
        ItemStack japaneseItem = createLanguageHead(
            "http://textures.minecraft.net/texture/d6c2ca7238666ae1b9dd9daa3d4fc829db22609fb569312dec1fb0c8d6dd6c1d",
            Component.text("日本語").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD),
            List.of(
                Component.text("日本語に切り替える", NamedTextColor.GRAY),
                Component.text("Japan · 日本", NamedTextColor.DARK_GRAY)
            ),
            currentLang.equals("jp")
        );
        setItem(16, japaneseItem, event -> {
            switchLanguage("jp", "日本語");
        });
        // Back button
        ItemStack backItem = new ItemBuilder(Material.ARROW)
            .name("&e← Back to Configuration")
            .lore("&7Return to the configuration menu")
            .build();
        setItem(24, backItem, event -> {
            plugin.getGUIManager().openMenu(player, "config");
        });
    }
    private ItemStack createLanguageHead(String textureUrl, Component name, List<Component> lore, boolean isCurrent) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            // Add selection indicator to name if current
            if (isCurrent) {
                meta.displayName(
                    Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .append(name)
                        .append(Component.text(" ✓", NamedTextColor.GREEN, TextDecoration.BOLD))
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
                loreLines.add(Component.text("━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY));
                loreLines.add(Component.text("✓ Currently Active", NamedTextColor.GREEN, TextDecoration.BOLD));
                loreLines.add(Component.text("━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY));
            } else {
                loreLines.add(Component.text("Click to activate", NamedTextColor.YELLOW));
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
            // Extract texture hash from URL
            String textureHash = extractTextureHash(textureUrl);
            if (textureHash == null) {
                plugin.getLogger().warning("Invalid texture URL format: " + textureUrl);
                return;
            }
            // Create Paper PlayerProfile with texture
            PlayerProfile profile = Bukkit.getServer().createProfile(UUID.randomUUID());
            // Build the texture JSON value
            String textureJson = String.format(
                "{\"textures\":{\"SKIN\":{\"url\":\"%s\"}}}",
                textureUrl
            );
            // Encode to base64
            String encoded = java.util.Base64.getEncoder().encodeToString(textureJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            // Apply as property
            profile.setProperty(new ProfileProperty("textures", encoded));
            // Set the profile
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
            plugin.getLanguageManager().setLanguage(langCode);
            plugin.getConfigManager().setLanguage(langCode);
            plugin.getConfigManager().saveConfig();
            sendMessage("&a✓ Language changed to &f" + displayName + "&a!");
            sendMessage("&7All menus and messages will now display in " + displayName);
            // Refresh the menu to show updated selections
            refresh();
            // Play success sound
            if (plugin.getConfigManager().useSounds()) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            }
        } catch (Exception e) {
            sendMessage("&c✗ Failed to change language! Check console for errors.");
            plugin.getLogger().severe("Error changing language: " + e.getMessage());
            // Play error sound
            if (plugin.getConfigManager().useSounds()) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }
    private String formatLanguageName(String code) {
        return switch (code.toLowerCase()) {
            case "en" -> "&fEnglish";
            case "jp" -> "&f日本語";
            default -> "&f" + code.toUpperCase();
        };
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
        player.sendMessage(component);
    }
}
