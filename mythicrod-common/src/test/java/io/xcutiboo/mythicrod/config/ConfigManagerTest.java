package io.xcutiboo.mythicrod.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import io.xcutiboo.mythicrod.api.platform.PlatformCommandSender;
import io.xcutiboo.mythicrod.api.platform.PlatformConfiguration;
import io.xcutiboo.mythicrod.api.platform.PlatformItemFactory;
import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;
import io.xcutiboo.mythicrod.api.platform.PlatformScheduler;
import io.xcutiboo.mythicrod.api.platform.PlatformServer;
import io.xcutiboo.mythicrod.api.platform.PlatformWorld;
import io.xcutiboo.mythicrod.internal.runtime.MythicRodRuntime;

class ConfigManagerTest {

    @Test
    void rewardDeliveryModeDefaultsToVanillaRetrieve() {
        ConfigManager manager = new ConfigManager(new FakeRuntime(), new MapPlatformConfiguration(Map.of()));

        assertEquals(RewardDeliveryMode.VANILLA_RETRIEVE, manager.getRewardDeliveryMode());
    }

    @Test
    void configuredDeliveryModeOverridesDefault() {
        ConfigManager manager = new ConfigManager(
            new FakeRuntime(),
            new MapPlatformConfiguration(Map.of(
                "features", Map.of(
                    "drops", Map.of(
                        "delivery-mode", "inventory"
                    )
                )
            ))
        );

        assertEquals(RewardDeliveryMode.INVENTORY, manager.getRewardDeliveryMode());
    }

    @Test
    void invalidDeliveryModeFallsBackToVanillaRetrieve() {
        ConfigManager manager = new ConfigManager(
            new FakeRuntime(),
            new MapPlatformConfiguration(Map.of(
                "features", Map.of(
                    "drops", Map.of(
                        "delivery-mode", "made_up_mode"
                    )
                )
            ))
        );

        assertEquals(RewardDeliveryMode.VANILLA_RETRIEVE, manager.getRewardDeliveryMode());
    }

    @Test
    void documentedProfilesRemainValid() {
        ConfigManager manager = new ConfigManager(
            new FakeRuntime(),
            new MapPlatformConfiguration(Map.of("profile", "performance"))
        );

        assertEquals("performance", manager.getProfile());
    }

    @Test
    void permissionGatesAreEnabledByDefault() {
        ConfigManager manager = new ConfigManager(new FakeRuntime(), new MapPlatformConfiguration(Map.of()));

        assertTrue(manager.usePermissions());
    }

    @Test
    void rodLuckMultipliersUseProductionDefaults() {
        ConfigManager manager = new ConfigManager(new FakeRuntime(), new MapPlatformConfiguration(Map.of()));

        assertEquals(1.0D, manager.getRodLuckMultiplier("basic"));
        assertEquals(1.25D, manager.getRodLuckMultiplier("advanced"));
        assertEquals(1.5D, manager.getRodLuckMultiplier("legendary"));
        assertEquals(1.0D, manager.getRodLuckMultiplier("unknown"));
    }

    @Test
    void rodLuckMultipliersClampUnsafeValues() {
        ConfigManager manager = new ConfigManager(
            new FakeRuntime(),
            new MapPlatformConfiguration(Map.of(
                "features", Map.of(
                    "rods", Map.of(
                        "luck-multipliers", Map.of(
                            "basic", -1.0D,
                            "advanced", 50.0D,
                            "legendary", 2.0D
                        )
                    )
                )
            ))
        );

        assertEquals(0.01D, manager.getRodLuckMultiplier("basic"));
        assertEquals(10.0D, manager.getRodLuckMultiplier("advanced"));
        assertEquals(2.0D, manager.getRodLuckMultiplier("legendary"));
    }

    @Test
    void missingCatchTemplatesKeepFullDefaultMessages() {
        ConfigManager manager = new ConfigManager(new FakeRuntime(), new MapPlatformConfiguration(Map.of()));

        assertContainsCatchPlaceholders(manager.getMsgLegendary());
        assertContainsCatchPlaceholders(manager.getMsgRare());
        assertContainsCatchPlaceholders(manager.getMsgUncommon());
        assertContainsCatchPlaceholders(manager.getMsgCommon());
    }

    @Test
    void blankCatchTemplatesFallBackToFullDefaultMessages() {
        ConfigManager manager = new ConfigManager(
            new FakeRuntime(),
            new MapPlatformConfiguration(Map.of(
                "messages", Map.of(
                    "catch", Map.of(
                        "legendary", "",
                        "rare", "   ",
                        "uncommon", "",
                        "common", ""
                    )
                )
            ))
        );

        assertEquals("<gray>You caught <white><bold>{amount}x {item}</bold></white>!", manager.getMsgCommon());
        assertContainsCatchPlaceholders(manager.getMsgLegendary());
        assertContainsCatchPlaceholders(manager.getMsgRare());
        assertContainsCatchPlaceholders(manager.getMsgUncommon());
    }

    @Test
    void settersRoundTripBooleanAndDeliveryFlags() {
        ConfigManager manager = new ConfigManager(new FakeRuntime(), new MapPlatformConfiguration(Map.of()));
        manager.setSoundsEnabled(false);
        manager.setParticlesEnabled(false);
        manager.setBiomeDropsEnabled(false);
        manager.setStatisticsEnabled(false);
        manager.setPermissionsEnabled(false);
        manager.setDebugEnabled(true);
        manager.setRewardDeliveryMode(RewardDeliveryMode.DROP_AT_PLAYER);
        manager.setStatsSaveInterval(300);

        assertEquals(false, manager.useSounds());
        assertEquals(false, manager.useParticles());
        assertEquals(false, manager.enableBiomeSpecificDrops());
        assertEquals(false, manager.trackStatistics());
        assertEquals(false, manager.usePermissions());
        assertEquals(true, manager.isDebugMode());
        assertEquals(RewardDeliveryMode.DROP_AT_PLAYER, manager.getRewardDeliveryMode());
        assertEquals(300, manager.getStatsSaveInterval());
    }

    @Test
    void particleSettersTakeWhitelistedValues() {
        ConfigManager manager = new ConfigManager(new FakeRuntime(), new MapPlatformConfiguration(Map.of()));
        manager.setCatchParticle("HEART");
        manager.setBubbleParticle("BUBBLE_POP");
        manager.setSuccessParticle("CRIT");
        manager.setXpParticle("HAPPY_VILLAGER");

        assertEquals("HEART", manager.getCatchParticle());
        assertEquals("BUBBLE_POP", manager.getBubbleParticle());
        assertEquals("CRIT", manager.getSuccessParticle());
        assertEquals("HAPPY_VILLAGER", manager.getXpParticle());
    }

    private static void assertContainsCatchPlaceholders(String template) {
        assertTrue(template.contains("{amount}"), () -> "Expected amount placeholder in: " + template);
        assertTrue(template.contains("{item}"), () -> "Expected item placeholder in: " + template);
    }

    private static final class FakeRuntime implements MythicRodRuntime {
        private final Logger logger = Logger.getLogger(ConfigManagerTest.class.getName());
        private final PlatformServer platform = new FakePlatformServer();

        @Override
        public Logger getLogger() {
            return logger;
        }

        @Override
        public File getDataFolder() {
            return new File(".");
        }

        @Override
        public PlatformServer getPlatform() {
            return platform;
        }

        @Override
        public PlatformConfiguration loadConfig(File file) {
            return new MapPlatformConfiguration(Map.of());
        }

        @Override
        public PlatformConfiguration createEmptyConfig() {
            return new MapPlatformConfiguration(Map.of());
        }
    }

    private static final class FakePlatformServer implements PlatformServer {
        private final Logger logger = Logger.getLogger(FakePlatformServer.class.getName());

        @Override
        public Logger getLogger() {
            return logger;
        }

        @Override
        public PlatformScheduler getScheduler() {
            return null;
        }

        @Override
        public PlatformPlayer getPlayer(UUID uuid) {
            return null;
        }

        @Override
        public PlatformCommandSender getCommandSender(String name) {
            return null;
        }

        @Override
        public boolean isEntityValid(UUID entityId) {
            return false;
        }

        @Override
        public boolean isNexoEnabled() {
            return false;
        }

        @Override
        public PlatformConfiguration loadConfiguration(File file) {
            return new MapPlatformConfiguration(Map.of());
        }

        @Override
        public PlatformConfiguration loadConfiguration(java.io.InputStream stream) {
            return new MapPlatformConfiguration(Map.of());
        }

        @Override
        public PlatformConfiguration createEmptyConfiguration() {
            return new MapPlatformConfiguration(Map.of());
        }

        @Override
        public PlatformWorld getWorld(String name) {
            return null;
        }

        @Override
        public PlatformItemFactory getItemFactory() {
            return null;
        }

        @Override
        public void dispatchCommandConsole(String command) {
            // Test stub: PlatformServer side effects are not exercised here.
        }

        @Override
        public void broadcastMessage(String message) {
            // Test stub: PlatformServer side effects are not exercised here.
        }
    }

    private static final class MapPlatformConfiguration implements PlatformConfiguration {
        private final Map<String, Object> values;

        private MapPlatformConfiguration(Map<String, Object> values) {
            this.values = values;
        }

        @Override
        public boolean contains(String path) {
            return resolve(path) != null;
        }

        @Override
        public String getString(String path) {
            return getString(path, null);
        }

        @Override
        public String getString(String path, String def) {
            Object value = resolve(path);
            return value instanceof String stringValue ? stringValue : def;
        }

        @Override
        public int getInt(String path) {
            return getInt(path, 0);
        }

        @Override
        public int getInt(String path, int def) {
            Object value = resolve(path);
            return value instanceof Number number ? number.intValue() : def;
        }

        @Override
        public boolean getBoolean(String path) {
            return getBoolean(path, false);
        }

        @Override
        public boolean getBoolean(String path, boolean def) {
            Object value = resolve(path);
            if (Boolean.TRUE.equals(value)) {
                return true;
            }
            if (Boolean.FALSE.equals(value)) {
                return false;
            }
            return def;
        }

        @Override
        public double getDouble(String path) {
            return getDouble(path, 0.0D);
        }

        @Override
        public double getDouble(String path, double def) {
            Object value = resolve(path);
            return value instanceof Number number ? number.doubleValue() : def;
        }

        @Override
        public List<String> getStringList(String path) {
            Object value = resolve(path);
            if (!(value instanceof List<?> rawList)) {
                return Collections.emptyList();
            }

            List<String> strings = new ArrayList<>();
            for (Object entry : rawList) {
                if (entry instanceof String stringEntry) {
                    strings.add(stringEntry);
                }
            }
            return List.copyOf(strings);
        }

        @Override
        public List<Map<?, ?>> getMapList(String path) {
            Object value = resolve(path);
            if (!(value instanceof List<?> rawList)) {
                return Collections.emptyList();
            }

            List<Map<?, ?>> maps = new java.util.ArrayList<>();
            for (Object entry : rawList) {
                if (entry instanceof Map<?, ?> mapEntry) {
                    maps.add(mapEntry);
                }
            }
            return List.copyOf(maps);
        }

        @Override
        public void set(String path, Object value) {
            // Test stub: setters in ConfigManager call back into the config; we
            // accept the write silently so the cached state can still be asserted
            // through getters.
        }

        @Override
        public Set<String> getKeys(boolean deep) {
            return values.keySet();
        }

        @Override
        public PlatformConfiguration getSection(String path) {
            Object value = resolve(path);
            if (!(value instanceof Map<?, ?> rawMap)) {
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> nestedValues = (Map<String, Object>) rawMap;
            return new MapPlatformConfiguration(nestedValues);
        }

        @Override
        public void save(File file) {
            throw new UnsupportedOperationException("Not needed for ConfigManager tests");
        }

        private Object resolve(String path) {
            if (path == null || path.isEmpty()) {
                return values;
            }

            Object current = values;
            for (String part : path.split("\\.")) {
                if (!(current instanceof Map<?, ?> currentMap)) {
                    return null;
                }
                current = currentMap.get(part);
                if (current == null) {
                    return null;
                }
            }
            return current;
        }
    }
}
