package io.xcutiboo.mythicrod.gui.menus;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.config.ConfigManager;
import io.xcutiboo.mythicrod.gui.utils.ItemBuilder;
public class ConfigMenu extends BaseMenu {
    private static final String ADMIN_PERMISSION = "mythicrod.admin.config";
    
    public ConfigMenu(MythicRod plugin, Player player) {
        super(plugin, player);
    }
    
    @Override
    public String getRequiredPermission() {
        return ADMIN_PERMISSION;
    }
    @Override
    protected int getSize() {
        return 54;
    }
    @Override
    protected String getTitle() {
        return "&6&lMythicRod &8⚙ &7Configuration";
    }
    @Override
    protected void build() {
        ConfigManager config = plugin.getConfigManager();
        // Fill border
        fillBorder();
        // === VISUAL SETTINGS ===
        // Sounds toggle
        boolean useSounds = config.useSounds();
        ItemStack soundsItem = new ItemBuilder(useSounds ? Material.NOTE_BLOCK : Material.GRAY_STAINED_GLASS)
                .name("&e&lSound Effects " + (useSounds ? "&a✓" : "&c✗"))
                .lore(
                        "&7Play sounds when catching items",
                        "&7and interacting with menus",
                        "",
                        "&7Status: " + getStatusColor(useSounds) + "&l" + (useSounds ? "ENABLED" : "DISABLED"),
                        "",
                        "&e▶ Click to " + (useSounds ? "&cdisable" : "&aenable")
                )
                .glow(useSounds)
                .build();
        setItem(11, soundsItem, event -> {
            if (!validatePermission()) return;
            config.setSoundsEnabled(!useSounds);
            playClickSound();
            refresh();
        });
        // Particles toggle
        boolean useParticles = config.useParticles();
        ItemStack particlesItem = new ItemBuilder(useParticles ? Material.BLAZE_POWDER : Material.GRAY_DYE)
                .name("&e&lParticle Effects " + (useParticles ? "&a✓" : "&c✗"))
                .lore(
                        "&7Display particles for rare catches",
                        "&7and special events",
                        "",
                        "&7Status: " + getStatusColor(useParticles) + "&l" + (useParticles ? "ENABLED" : "DISABLED"),
                        "",
                        "&e▶ Click to " + (useParticles ? "&cdisable" : "&aenable")
                )
                .glow(useParticles)
                .build();
        setItem(12, particlesItem, event -> {
            if (!validatePermission()) return;
            config.setParticlesEnabled(!useParticles);
            playClickSound();
            refresh();
        });
        // === FEATURE SETTINGS ===
        // Statistics tracking
        boolean trackStats = config.trackStatistics();
        ItemStack statsItem = new ItemBuilder(trackStats ? Material.WRITABLE_BOOK : Material.BOOK)
                .name("&d&lStatistics Tracking " + (trackStats ? "&a✓" : "&c✗"))
                .lore(
                        "&7Track getPlayer() fishing statistics",
                        "&7and maintain leaderboards",
                        "",
                        "&7Status: " + getStatusColor(trackStats) + "&l" + (trackStats ? "ENABLED" : "DISABLED"),
                        "",
                        trackStats ? "&7Stats are being recorded" : "&cStats will not be tracked",
                        "",
                        "&e▶ Click to " + (trackStats ? "&cdisable" : "&aenable")
                )
                .glow(trackStats)
                .build();
        setItem(14, statsItem, event -> {
            if (!validatePermission()) return;
            config.setStatisticsEnabled(!trackStats);
            playClickSound();
            refresh();
        });
        // Biome-specific drops
        boolean biomeDrops = config.enableBiomeSpecificDrops();
        ItemStack biomeItem = new ItemBuilder(biomeDrops ? Material.GRASS_BLOCK : Material.DIRT)
                .name("&a&lBiome-Specific Drops " + (biomeDrops ? "&a✓" : "&c✗"))
                .lore(
                        "&7Enable different drops per biome",
                        "&7type for more variety",
                        "",
                        "&7Status: " + getStatusColor(biomeDrops) + "&l" + (biomeDrops ? "ENABLED" : "DISABLED"),
                        "",
                        biomeDrops ? "&7Biome drops are active" : "&cOnly global drops will work",
                        "",
                        "&e▶ Click to " + (biomeDrops ? "&cdisable" : "&aenable")
                )
                .glow(biomeDrops)
                .build();
        setItem(15, biomeItem, event -> {
            if (!validatePermission()) return;
            config.setBiomeDropsEnabled(!biomeDrops);
            playClickSound();
            refresh();
        });
        // Permissions system
        boolean usePerms = config.usePermissions();
        ItemStack permsItem = new ItemBuilder(usePerms ? Material.DIAMOND : Material.COAL)
                .name("&c&lPermission System " + (usePerms ? "&a✓" : "&c✗"))
                .lore(
                        "&7Require permissions for drops",
                        "&7and command access",
                        "",
                        "&7Status: " + getStatusColor(usePerms) + "&l" + (usePerms ? "ENABLED" : "DISABLED"),
                        "",
                        usePerms ? "&7Permission checks active" : "&aAll players have full access",
                        "",
                        "&c⚠ Warning: &7May restrict access",
                        "",
                        "&e▶ Click to " + (usePerms ? "&cdisable" : "&aenable")
                )
                .glow(usePerms)
                .build();
        setItem(20, permsItem, event -> {
            if (!validatePermission()) return;
            config.setPermissionsEnabled(!usePerms);
            playClickSound();
            refresh();
        });
        // === PERFORMANCE SETTINGS ===
        // Stats save interval
        int statsSaveInterval = config.getStatsSaveInterval();
        ItemStack saveIntervalItem = new ItemBuilder(Material.CLOCK)
                .name("&b&lStatistics Save Interval")
                .lore(
                        "&7How often to auto-save statistics",
                        "&7to prevent data loss",
                        "",
                        "&eCurrent: &f" + formatTime(statsSaveInterval) + " &7(&f" + statsSaveInterval + "s&7)",
                        "",
                        statsSaveInterval <= 300 ? "&aFrequent saves" : statsSaveInterval <= 600 ? "&7Balanced" : "&eInfrequent saves",
                        "",
                        "&e▶ Controls:",
                        "&7  Left-click: &a+60s",
                        "&7  Right-click: &c-60s",
                        "&7  Shift-left: &a+300s &7(5min)",
                        "&7  Shift-right: &c-300s &7(5min)",
                        "",
                        "&8Minimum: 60 seconds"
                )
                .build();
        setItem(23, saveIntervalItem, event -> {
            if (!validatePermission()) return;
            int change = 60;
            if (event.isShiftClick()) {
                change = 300;
            }
            int newValue = statsSaveInterval;
            if (event.isLeftClick()) {
                newValue += change;
            } else if (event.isRightClick()) {
                newValue = Math.max(60, newValue - change);
            }
            config.setStatsSaveInterval(newValue);
            playClickSound();
            refresh();
        });
        // Hook cleanup interval
        int hookCleanup = config.getHookCleanupInterval();
        ItemStack cleanupItem = new ItemBuilder(Material.FISHING_ROD)
                .name("&b&lHook Cleanup Interval")
                .lore(
                        "&7How often to clean up old hooks",
                        "&7to improve performance",
                        "",
                        "&eCurrent: &f" + formatTime(hookCleanup) + " &7(&f" + hookCleanup + "s&7)",
                        "",
                        hookCleanup <= 300 ? "&aFrequent cleanup" : hookCleanup <= 600 ? "&7Balanced" : "&eInfrequent cleanup",
                        "",
                        "&e▶ Controls:",
                        "&7  Left-click: &a+60s",
                        "&7  Right-click: &c-60s",
                        "&7  Shift-left: &a+300s &7(5min)",
                        "&7  Shift-right: &c-300s &7(5min)",
                        "",
                        "&8Minimum: 60 seconds"
                )
                .build();
        setItem(24, cleanupItem, event -> {
            if (!validatePermission()) return;
            int change = 60;
            if (event.isShiftClick()) {
                change = 300;
            }
            int newValue = hookCleanup;
            if (event.isLeftClick()) {
                newValue += change;
            } else if (event.isRightClick()) {
                newValue = Math.max(60, newValue - change);
            }
            config.setHookCleanupInterval(newValue);
            playClickSound();
            refresh();
        });
        // === DEBUG & ADVANCED ===
        // Debug mode
        boolean debugMode = config.isDebugMode();
        ItemStack debugItem = new ItemBuilder(debugMode ? Material.REDSTONE_TORCH : Material.TORCH)
                .name("&c&lDebug Mode " + (debugMode ? "&a✓" : "&c✗"))
                .lore(
                        "&7Enable detailed logging for",
                        "&7troubleshooting and development",
                        "",
                        "&7Status: " + getStatusColor(debugMode) + "&l" + (debugMode ? "ENABLED" : "DISABLED"),
                        "",
                        debugMode ? "&eVerbose logs active" : "&7Normal logging",
                        "",
                        "&c⚠ Warning: &7Generates lots of logs",
                        "",
                        "&e▶ Click to " + (debugMode ? "&cdisable" : "&aenable")
                )
                .glow(debugMode)
                .build();
        setItem(29, debugItem, event -> {
            if (!validatePermission()) return;
            boolean newDebugMode = !debugMode;
            config.setDebugEnabled(newDebugMode);
            plugin.getDropManager().setDebugMode(newDebugMode);
            playClickSound();
            refresh();
        });
        // Language selection
        ItemStack languageItem = new ItemBuilder(Material.REPEATER)
                .name("&5&lLanguage Settings 🌐")
                .lore(
                        "&7Change the display language",
                        "&7for all menus and messages",
                        "",
                        "&eCurrent Language: &f" + formatLanguageName(plugin.getLanguageManager().getLanguage()),
                        "",
                        "&7Available:",
                        "&8• English (UK) &7- English language",
                        "&8• 日本語 &7- Japanese language",
                        "",
                        "&e▶ Click to change language"
                )
                .glow(true)
                .build();
        setItem(33, languageItem, event -> {
            plugin.getGUIManager().openMenu(getPlayer(), "language");
        });
        // === INFO PANEL ===
        // Configuration info
        ItemStack infoItem = new ItemBuilder(Material.BOOK)
                .name("&6&lConfiguration Info")
                .lore(
                        "&7All changes are applied",
                        "&7instantly and in real-time!",
                        "",
                        "&7Click items to toggle settings",
                        "&7or adjust values. Don't forget",
                        "&7to save when you're done!",
                        "",
                        "&e✓ &7= Enabled/Active",
                        "&c✗ &7= Disabled/Inactive",
                        "",
                        "&8Real-time configuration system"
                )
                .build();
        setItem(31, infoItem);
        // === NAVIGATION ===
        // Back button
        ItemStack backItem = new ItemBuilder(Material.ARROW)
                .name("&e← Back to Main Menu")
                .lore("&7Return to the main hub")
                .build();
        setItem(45, backItem, event -> {
            plugin.getGUIManager().openMainHub(getPlayer());
        });
        // Save and apply button
        ItemStack saveItem = new ItemBuilder(Material.EMERALD)
                .name("&a&l✓ Save Configuration")
                .lore(
                        "&7Save all changes to config.yml",
                        "",
                        "&aChanges are already active!",
                        "&7This saves them permanently",
                        "&7to disk for next restart.",
                        "",
                        "&e▶ Click to save to disk"
                )
                .glow(true)
                .build();
        setItem(49, saveItem, event -> {
            if (!validatePermission()) return;
            try {
                config.saveConfig();
                sendMessage("&a✓ Configuration saved successfully!");
                sendMessage("&7Changes are active. Use &e/mythicrod reload &7for full reload if needed.");
                playSuccessSound();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save configuration: " + e.getMessage());
                sendMessage("&c✗ Failed to save configuration!");
                playErrorSound();
            }
        });
        // Close button
        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
                .name("&c&lClose")
                .lore("&7Close without saving")
                .build();
        setItem(53, closeItem, event -> getPlayer().closeInventory());
    }
    private void fillBorder() {
        ItemStack borderItem = new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE)
                .name(" ")
                .build();
        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            setItem(i, borderItem);
            setItem(45 + i, borderItem);
        }
        // Left and right columns (skip navigation row)
        for (int row = 1; row < 5; row++) {
            setItem(row * 9, borderItem);
            setItem(row * 9 + 8, borderItem);
        }
    }
    private String getStatusColor(boolean enabled) {
        return enabled ? "&a" : "&c";
    }
    private String formatLanguageName(String code) {
        return switch (code.toLowerCase()) {
            case "en" -> "&fEnglish";
            case "jp" -> "&f日本語";
            default -> "&f" + code.toUpperCase();
        };
    }
    private String formatTime(int seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        if (minutes < 60) {
            return minutes + "m" + (remainingSeconds > 0 ? " " + remainingSeconds + "s" : "");
        }
        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;
        return hours + "h" + (remainingMinutes > 0 ? " " + remainingMinutes + "m" : "");
    }
    private void playClickSound() {
        if (plugin.getConfigManager().useSounds()) {
            Player p = getPlayer(); if (p != null) p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        }
    }
    private void playSuccessSound() {
        if (plugin.getConfigManager().useSounds()) {
            Player p = getPlayer(); if (p != null) p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        }
    }
    private void playErrorSound() {
        if (plugin.getConfigManager().useSounds()) {
            Player p = getPlayer(); if (p != null) p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
}
