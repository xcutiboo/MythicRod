package io.xcutiboo.mythicrod.gui.menus;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.xcutiboo.mythicrod.MythicRod;
import io.xcutiboo.mythicrod.config.ConfigManager;
import io.xcutiboo.mythicrod.gui.MenuItemFactory;
import io.xcutiboo.mythicrod.item.ItemBuilder;

import java.util.Map;
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
        return tr("gui.config.title");
    }
    @Override
    protected void build() {
        ConfigManager config = plugin.getConfigManager();
        fillBorder(Material.BLACK_STAINED_GLASS_PANE);
        
        // Sound Effects Toggle
        boolean useSounds = config.useSounds();
        ItemStack soundsItem = MenuItemFactory.createToggleItem(
            useSounds,
            tr("gui.config.sounds"),
            tr("gui.config.sounds_lore")
        );
        setConfigurableToggle(11, soundsItem, () -> config.setSoundsEnabled(!useSounds));

        // Particle Effects Toggle
        boolean useParticles = config.useParticles();
        ItemStack particlesItem = MenuItemFactory.createToggleItem(
            useParticles,
            tr("gui.config.particles"),
            tr("gui.config.particles_lore")
        );
        setConfigurableToggle(12, particlesItem, () -> config.setParticlesEnabled(!useParticles));
        // Particle Settings Button
        String catchParticle = config.getCatchParticle();
        String bubbleParticle = config.getBubbleParticle();
        String successParticle = config.getSuccessParticle();
        ItemStack particleSettingsItem = new ItemBuilder(Material.FIREWORK_ROCKET)
                .name(tr("gui.config.particles_settings.name"))
                .lore(
                        tr("gui.config.particles_settings.lore1"),
                        tr("gui.config.particles_settings.lore2"),
                        "",
                        tr("gui.config.particles_settings.lore3"),
                        tr("gui.config.particles_settings.current_catch", Map.of("%particle%", catchParticle)),
                        tr("gui.config.particles_settings.current_bubble", Map.of("%particle%", bubbleParticle)),
                        tr("gui.config.particles_settings.current_success", Map.of("%particle%", successParticle)),
                        "",
                        tr("gui.config.particles_settings.click_change")
                )
                .glow(true)
                .build();
        setItem(13, particleSettingsItem, event -> {
            if (!validatePermission()) return;
            playClickSound();
            getPlayer().closeInventory();
            getPlayer().sendMessage(MiniMessage.miniMessage().deserialize(
                "<gold><bold>[MythicRod]</bold></gold> <yellow>Use these commands to change particles:</yellow>\n" +
                "<gray>/mythicrod particle catch <type> - Change catch particle</gray>\n" +
                "<gray>/mythicrod particle bubble <type> - Change bubble particle</gray>\n" +
                "<gray>/mythicrod particle success <type> - Change success particle</gray>\n" +
                "<green>Available: SPLASH, BUBBLE_POP, HAPPY_VILLAGER, TOTEM, HEART, NOTE, FLAME, etc."
            ));
        });
        // Statistics Toggle
        boolean trackStats = config.trackStatistics();
        ItemStack statsItem = MenuItemFactory.createToggleItem(
            trackStats,
            tr("gui.config.stats"),
            tr("gui.config.stats_lore")
        );
        setConfigurableToggle(14, statsItem, () -> config.setStatisticsEnabled(!trackStats));

        // Biome Drops Toggle
        boolean biomeDrops = config.enableBiomeSpecificDrops();
        ItemStack biomeItem = MenuItemFactory.createToggleItem(
            biomeDrops,
            tr("gui.config.biome_drops"),
            tr("gui.config.biome_drops_lore")
        );
        setConfigurableToggle(15, biomeItem, () -> config.setBiomeDropsEnabled(!biomeDrops));

        // Drop to Inventory Toggle
        boolean dropToInv = config.dropToInventory();
        ItemStack dropToInvItem = new ItemBuilder(dropToInv ? Material.CHEST : Material.FISHING_ROD)
                .name(tr("gui.config.drop_to_inv.name", Map.of("%status%", dropToInv ? "<green>✓" : "<red>✗")))
                .lore(
                        tr("gui.config.drop_to_inv.lore1"),
                        tr("gui.config.drop_to_inv.lore2"),
                        "",
                        tr("gui.config.drop_to_inv.status", Map.of("%color%", getStatusColor(dropToInv), "%status%", dropToInv ? tr("gui.config.enabled") : tr("gui.config.disabled"))),
                        "",
                        dropToInv ? tr("gui.config.drop_to_inv.enabled_desc") : tr("gui.config.drop_to_inv.disabled_desc"),
                        "",
                        tr("gui.config.drop_to_inv.click", Map.of("%action%", dropToInv ? tr("gui.config.disable") : tr("gui.config.enable"))),
                        ""
                )
                .glow(dropToInv)
                .build();
        setItem(16, dropToInvItem, event -> {
            if (!validatePermission()) return;
            config.setDropToInventory(!dropToInv);
            playClickSound();
            refresh();
        });
        boolean usePerms = config.usePermissions();
        ItemStack permsItem = new ItemBuilder(usePerms ? Material.DIAMOND : Material.COAL)
                .name(tr("gui.config.perms.name", Map.of("%status%", usePerms ? "<green>✓" : "<red>✗")))
                .lore(
                        tr("gui.config.perms.lore1"),
                        tr("gui.config.perms.lore2"),
                        "",
                        tr("gui.config.perms.status", Map.of("%color%", getStatusColor(usePerms), "%status%", usePerms ? tr("gui.config.enabled") : tr("gui.config.disabled"))),
                        "",
                        usePerms ? tr("gui.config.perms.active") : tr("gui.config.perms.inactive"),
                        "",
                        tr("gui.config.perms.warning"),
                        "",
                        tr("gui.config.perms.click", Map.of("%action%", usePerms ? tr("gui.config.disable") : tr("gui.config.enable")))
                )
                .glow(usePerms)
                .build();
        setItem(20, permsItem, event -> {
            if (!validatePermission()) return;
            config.setPermissionsEnabled(!usePerms);
            playClickSound();
            refresh();
        });
        int statsSaveInterval = config.getStatsSaveInterval();
        ItemStack saveIntervalItem = new ItemBuilder(Material.CLOCK)
                .name(tr("gui.config.save_interval.name"))
                .lore(
                        tr("gui.config.save_interval.lore1"),
                        tr("gui.config.save_interval.lore2"),
                        "",
                        tr("gui.config.save_interval.current", Map.of("%time%", formatTime(statsSaveInterval), "%seconds%", String.valueOf(statsSaveInterval))),
                        "",
                        statsSaveInterval <= 300 ? tr("gui.config.save_interval.frequent") : statsSaveInterval <= 600 ? tr("gui.config.save_interval.balanced") : tr("gui.config.save_interval.infrequent"),
                        "",
                        tr("gui.config.save_interval.controls"),
                        tr("gui.config.save_interval.left_click"),
                        tr("gui.config.save_interval.right_click"),
                        tr("gui.config.save_interval.shift_left"),
                        tr("gui.config.save_interval.shift_right"),
                        "",
                        tr("gui.config.save_interval.minimum")
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
        int hookCleanup = config.getHookCleanupInterval();
        ItemStack cleanupItem = new ItemBuilder(Material.FISHING_ROD)
                .name(tr("gui.config.cleanup.name"))
                .lore(
                        tr("gui.config.cleanup.lore1"),
                        tr("gui.config.cleanup.lore2"),
                        "",
                        tr("gui.config.cleanup.current", Map.of("%time%", formatTime(hookCleanup), "%seconds%", String.valueOf(hookCleanup))),
                        "",
                        hookCleanup <= 300 ? tr("gui.config.cleanup.frequent") : hookCleanup <= 600 ? tr("gui.config.cleanup.balanced") : tr("gui.config.cleanup.infrequent"),
                        "",
                        tr("gui.config.cleanup.controls"),
                        tr("gui.config.cleanup.left_click"),
                        tr("gui.config.cleanup.right_click"),
                        tr("gui.config.cleanup.shift_left"),
                        tr("gui.config.cleanup.shift_right"),
                        "",
                        tr("gui.config.cleanup.minimum")
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
        boolean debugMode = config.isDebugMode();
        ItemStack debugItem = new ItemBuilder(debugMode ? Material.REDSTONE_TORCH : Material.TORCH)
                .name(tr("gui.config.debug.name", Map.of("%status%", debugMode ? "<green>✓" : "<red>✗")))
                .lore(
                        tr("gui.config.debug.lore1"),
                        tr("gui.config.debug.lore2"),
                        "",
                        tr("gui.config.debug.status", Map.of("%color%", getStatusColor(debugMode), "%status%", debugMode ? tr("gui.config.enabled") : tr("gui.config.disabled"))),
                        "",
                        debugMode ? tr("gui.config.debug.active") : tr("gui.config.debug.inactive"),
                        "",
                        tr("gui.config.debug.warning"),
                        "",
                        tr("gui.config.debug.click", Map.of("%action%", debugMode ? tr("gui.config.disable") : tr("gui.config.enable")))
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
        ItemStack languageItem = new ItemBuilder(Material.REPEATER)
                .name(tr("gui.config.language.name"))
                .lore(
                        tr("gui.config.language.lore1"),
                        tr("gui.config.language.lore2"),
                        "",
                        tr("gui.config.language.current", Map.of("%lang%", formatLanguageName(plugin.getLanguageManager().getLanguage()))),
                        "",
                        tr("gui.config.language.available"),
                        "<dark_gray>• English (UK) <gray>- English language</gray></dark_gray>",
                        "<dark_gray>• 日本語 <gray>- Japanese language</gray></dark_gray>",
                        "",
                        tr("gui.config.language.click")
                )
                .glow(true)
                .build();
        setItem(33, languageItem, event -> {
            playClickSound();
            plugin.getGUIManager().openMenu(getPlayer(), "language");
        });
        ItemStack infoItem = new ItemBuilder(Material.BOOK)
                .name(tr("gui.config.info.name"))
                .lore(
                        tr("gui.config.info.lore1"),
                        tr("gui.config.info.lore2"),
                        "",
                        tr("gui.config.info.lore3"),
                        tr("gui.config.info.lore4"),
                        "",
                        tr("gui.config.info.lore5")
                )
                .build();
        setItem(31, infoItem);
        ItemStack backItem = new ItemBuilder(Material.ARROW)
                .name(tr("gui.config.back.name"))
                .lore(tr("gui.config.back.lore"))
                .build();
        setItem(45, backItem, event -> {
            playClickSound();
            plugin.getGUIManager().openMainHub(getPlayer());
        });
        ItemStack saveItem = new ItemBuilder(Material.EMERALD)
                .name(tr("gui.config.save.name"))
                .lore(
                        tr("gui.config.save.lore1"),
                        "",
                        tr("gui.config.save.lore2"),
                        tr("gui.config.save.lore3"),
                        tr("gui.config.save.lore4"),
                        "",
                        tr("gui.config.save.lore5")
                )
                .glow(true)
                .build();
        setItem(49, saveItem, event -> {
            if (!validatePermission()) return;
            try {
                config.saveConfig();
                sendMessage("<green>✓ Configuration saved successfully!</green>");
                sendMessage("<gray>Changes are active. Use <yellow>/mythicrod reload</yellow> <gray>for full reload if needed.</gray></gray>");
                playSuccessSound();
            } catch (Exception e) {
                this.plugin.getLogger().warning("Failed to save configuration: " + e.getMessage());
                sendMessage("<red>✗ Failed to save configuration!</red>");
                playErrorSound();
            }
        });
        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
                .name(tr("gui.config.close.name"))
                .lore(tr("gui.config.close.lore"))
                .build();
        setItem(53, closeItem, event -> {
            playClickSound();
            getPlayer().closeInventory();
        });
    }

    /**
     * DRY: Removed local fillBorder() - now using BaseMenu.fillBorder(Material)
     * which accepts a configurable material parameter.
     */

    private String getStatusColor(boolean enabled) {
        return enabled ? "<green>" : "<red>";
    }

    private String formatLanguageName(String code) {
        if (code == null || code.isEmpty()) {
            return "<white>Unknown</white>";
        }
        String normalized = code.toLowerCase(java.util.Locale.ROOT);
        // Handle both short codes and full locale codes (e.g., "en", "en_US", "ja", "ja_JP")
        return switch (normalized) {
            case "en", "en_us" -> "<white>English</white>";
            case "ja", "ja_jp", "jp" -> "<white>日本語</white>";
            default -> "<white>" + code.toUpperCase(java.util.Locale.ROOT) + "</white>";
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
}
