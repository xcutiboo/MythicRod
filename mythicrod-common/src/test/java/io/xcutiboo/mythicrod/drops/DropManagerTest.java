package io.xcutiboo.mythicrod.drops;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import io.xcutiboo.mythicrod.api.platform.PlatformConfiguration;
import io.xcutiboo.mythicrod.api.platform.PlatformInventory;
import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;

class DropManagerTest {

    @Test
    void reloadWaitsForPendingAsyncPersistenceBeforePublishingNewDrops() throws Exception {
        DropManager manager = new DropManager(Logger.getLogger(DropManagerTest.class.getName()));
        ExecutorService executor = Executors.newSingleThreadExecutor();

        manager.loadDrops(dropConfig("COD"));
        manager.beginAsyncPersistenceOperation();

        try {
            Future<?> reloadFuture = executor.submit(() -> manager.reload(dropConfig("SALMON")));

            TimeoutException timeout = assertThrows(TimeoutException.class, () -> reloadFuture.get(200, TimeUnit.MILLISECONDS));
            assertNotNull(timeout);
            assertEquals("COD", manager.getDrops("fish").get(0).getIdentifier());

            manager.endAsyncPersistenceOperation();
            reloadFuture.get(2, TimeUnit.SECONDS);

            assertEquals("SALMON", manager.getDrops("fish").get(0).getIdentifier());
        } finally {
            manager.endAsyncPersistenceOperation();
            executor.shutdownNow();
        }
    }

    @Test
    void guiUpdateAndDeleteTargetExactDropWhenIdentifiersRepeat() {
        DropManager manager = new DropManager(Logger.getLogger(DropManagerTest.class.getName()));
        manager.loadDrops(duplicateDropConfig());

        List<CustomDrop> originalDrops = manager.getDrops("fish");
        CustomDrop firstDiamond = originalDrops.get(0);
        CustomDrop secondDiamond = originalDrops.get(1);

        assertTrue(manager.updateDrop(secondDiamond, "fish", "EMERALD", 50, 3, "<aqua>Edited Emerald", List.of(), true));

        List<CustomDrop> updatedDrops = manager.getDrops("fish");
        assertEquals(10, updatedDrops.get(0).getWeight());
        assertEquals("DIAMOND", updatedDrops.get(0).getIdentifier());
        assertEquals("EMERALD", updatedDrops.get(1).getIdentifier());
        assertEquals(50, updatedDrops.get(1).getWeight());
        assertEquals(3, updatedDrops.get(1).getAmount());
        assertEquals("<aqua>Edited Emerald", updatedDrops.get(1).getCustomName());

        assertTrue(manager.deleteDrop(firstDiamond, "fish"));

        List<CustomDrop> remainingDrops = manager.getDrops("fish");
        assertEquals(1, remainingDrops.size());
        assertEquals("EMERALD", remainingDrops.get(0).getIdentifier());
        assertEquals(50, remainingDrops.get(0).getWeight());
    }

    @Test
    void guiUpdateCanSwitchDropBetweenVanillaAndNexoIdentifiers() {
        DropManager manager = new DropManager(Logger.getLogger(DropManagerTest.class.getName()));
        manager.loadDrops(duplicateDropConfig());

        CustomDrop targetDrop = manager.getDrops("fish").get(0);
        assertTrue(manager.updateDrop(targetDrop, "fish", "nexo:ancient_pearl", 25, 1, null, List.of(), false));

        CustomDrop updatedDrop = manager.getDrops("fish").get(0);
        assertEquals("nexo:ancient_pearl", updatedDrop.getIdentifier());
        assertEquals("ancient_pearl", updatedDrop.getNexoItemId());
        assertTrue(updatedDrop.isNexoItem());
    }

    @Test
    void guiUpdateCanReplaceEveryEditableDropField() {
        DropManager manager = new DropManager(Logger.getLogger(DropManagerTest.class.getName()));
        manager.loadDrops(duplicateDropConfig());

        CustomDrop targetDrop = manager.getDrops("fish").get(0);
        assertTrue(manager.updateDrop(
            targetDrop,
            "fish",
            "EMERALD",
            6,
            2,
            "<green>Polished Emerald",
            List.of("<gray>Line one"),
            12345,
            Map.of("minecraft:unbreaking", 2),
            List.of("HIDE_ENCHANTS"),
            true,
            "mythicrod.drops.test",
            List.of("minecraft:ocean")
        ));

        CustomDrop updatedDrop = manager.getDrops("fish").get(0);
        assertEquals("EMERALD", updatedDrop.getIdentifier());
        assertEquals(6, updatedDrop.getWeight());
        assertEquals(2, updatedDrop.getAmount());
        assertEquals("<green>Polished Emerald", updatedDrop.getCustomName());
        assertEquals(List.of("<gray>Line one"), updatedDrop.getLore());
        assertEquals(12345, updatedDrop.getCustomModelData());
        assertEquals(Map.of("minecraft:unbreaking", 2), updatedDrop.getEnchantments());
        assertEquals(List.of("HIDE_ENCHANTS"), updatedDrop.getItemFlags());
        assertEquals("mythicrod.drops.test", updatedDrop.getPermission());
        assertEquals(List.of("minecraft:ocean"), updatedDrop.getBiomes());
        assertTrue(updatedDrop.isGlowing());
    }

    @Test
    void guiAddDropAppliesCategoryDefaults() {
        DropManager manager = new DropManager(Logger.getLogger(DropManagerTest.class.getName()));
        manager.loadDrops(duplicateDropConfig());

        CustomDrop rareDrop = manager.addDrop(
            "rare",
            "EMERALD",
            4,
            1,
            null,
            List.of(),
            0,
            Map.of(),
            List.of(),
            false,
            null,
            List.of()
        );
        CustomDrop oceanDrop = manager.addDrop(
            "biome_ocean",
            "TROPICAL_FISH",
            20,
            1,
            null,
            List.of(),
            0,
            Map.of(),
            List.of(),
            false,
            null,
            List.of()
        );

        assertEquals("mythicrod.drops.rare", rareDrop.getPermission());
        assertEquals(List.of("minecraft:ocean"), oceanDrop.getBiomes());
        assertEquals(1, manager.getDrops("rare").size());
        assertEquals(1, manager.getDrops("biome_ocean").size());
    }

    @Test
    void modernBiomeCategoriesWinOverStaleBiomeDropSection() {
        DropManager manager = new DropManager(Logger.getLogger(DropManagerTest.class.getName()));

        manager.loadDrops(new MapPlatformConfiguration(Map.<String, Object>of(
            "drops", Map.<String, Object>of(
                "biome_ocean", List.of(
                    Map.<String, Object>of("identifier", "HEART_OF_THE_SEA", "weight", 5, "amount", 1)
                )
            ),
            "biome-drops", Map.<String, Object>of(
                "ocean", List.of("PRISMARINE_SHARD,30,1"),
                "desert", List.of("CACTUS,30,1")
            )
        )));

        assertEquals(1, manager.getDrops("biome_ocean").size());
        assertEquals("HEART_OF_THE_SEA", manager.getDrops("biome_ocean").get(0).getIdentifier());
        assertEquals("CACTUS", manager.getDrops("biome_desert").get(0).getIdentifier());
        assertEquals(List.of("minecraft:desert"), manager.getDrops("biome_desert").get(0).getBiomes());
    }

    @Test
    void permissionToggleControlsPermissionGatedDrops() {
        DropManager manager = new DropManager(Logger.getLogger(DropManagerTest.class.getName()));
        manager.loadDrops(new MapPlatformConfiguration(Map.<String, Object>of(
            "drops", Map.<String, Object>of(
                "rare", List.of(
                    Map.<String, Object>of(
                        "identifier", "DIAMOND",
                        "weight", 10,
                        "amount", 1
                    )
                )
            )
        )));

        assertEquals("mythicrod.drops.rare", manager.getDrops("rare").get(0).getPermission());

        PlatformPlayer playerWithoutPermission = new FakePlayer(Set.of());
        PlatformPlayer playerWithPermission = new FakePlayer(Set.of("mythicrod.drops.rare"));

        manager.setUsePermissions(false);
        assertEquals(1, manager.getEligibleDrops(playerWithoutPermission, "minecraft:ocean").size());

        manager.setUsePermissions(true);
        assertEquals(0, manager.getEligibleDrops(playerWithoutPermission, "minecraft:ocean").size());
        assertEquals(1, manager.getEligibleDrops(playerWithPermission, "minecraft:ocean").size());
    }

    @Test
    void previousWeightKeyStillLoadsForDiskMigration() {
        DropManager manager = new DropManager(Logger.getLogger(DropManagerTest.class.getName()));
        manager.loadDrops(new MapPlatformConfiguration(Map.<String, Object>of(
            "drops", Map.<String, Object>of(
                "fish", List.of(
                    Map.<String, Object>of("identifier", "SALMON", "chance", 7, "amount", 1)
                )
            )
        )));

        assertEquals(7, manager.getDrops("fish").get(0).getWeight());
    }

    @Test
    void modernBiomeCategoryAddsImplicitBiomeCondition() {
        DropManager manager = new DropManager(Logger.getLogger(DropManagerTest.class.getName()));
        manager.loadDrops(new MapPlatformConfiguration(Map.<String, Object>of(
            "drops", Map.<String, Object>of(
                "global", List.of(
                    Map.<String, Object>of("identifier", "COD", "weight", 100, "amount", 1)
                ),
                "biome_ocean", List.of(
                    Map.<String, Object>of("identifier", "NAUTILUS_SHELL", "weight", 10, "amount", 1)
                )
            )
        )));

        CustomDrop oceanDrop = manager.getDrops("biome_ocean").get(0);
        assertEquals(List.of("minecraft:ocean"), oceanDrop.getBiomes());

        PlatformPlayer player = new FakePlayer(Set.of());
        assertEquals(2, manager.getEligibleDrops(player, "minecraft:ocean").size());
        assertEquals(1, manager.getEligibleDrops(player, "minecraft:forest").size());
        assertEquals(1, manager.getEligibleDrops(player, null).size());
    }

    @Test
    void disablingBiomeSpecificDropsExcludesBiomeConstrainedDrops() {
        DropManager manager = new DropManager(Logger.getLogger(DropManagerTest.class.getName()));
        manager.loadDrops(new MapPlatformConfiguration(Map.<String, Object>of(
            "drops", Map.<String, Object>of(
                "global", List.of(
                    Map.<String, Object>of("identifier", "COD", "weight", 100, "amount", 1)
                ),
                "biome_ocean", List.of(
                    Map.<String, Object>of("identifier", "NAUTILUS_SHELL", "weight", 10, "amount", 1)
                )
            )
        )));

        PlatformPlayer player = new FakePlayer(Set.of());

        manager.setUseBiomeSpecificDrops(false);
        assertEquals(1, manager.getEligibleDrops(player, "minecraft:ocean").size());

        manager.setUseBiomeSpecificDrops(true);
        assertEquals(2, manager.getEligibleDrops(player, "minecraft:ocean").size());
    }

    @Test
    void rodLuckMultiplierOnlyRaisesRareAndLegendaryWeights() {
        DropManager manager = new DropManager(Logger.getLogger(DropManagerTest.class.getName()));

        assertEquals(8, manager.getEffectiveWeight(dropWithWeight(5), 1.5D));
        assertEquals(2, manager.getEffectiveWeight(dropWithWeight(1), 1.5D));
        assertEquals(15, manager.getEffectiveWeight(dropWithWeight(15), 1.5D));
        assertEquals(40, manager.getEffectiveWeight(dropWithWeight(40), 1.5D));
    }

    private static PlatformConfiguration dropConfig(String material) {
        return new MapPlatformConfiguration(Map.<String, Object>of(
            "drops", Map.<String, Object>of(
                "fish", Map.<String, Object>of(
                    "catch", Map.<String, Object>of(
                        "material", material,
                        "weight", 100,
                        "amount", 1
                    )
                )
            )
        ));
    }

    private static PlatformConfiguration duplicateDropConfig() {
        return new MapPlatformConfiguration(Map.<String, Object>of(
            "drops", Map.<String, Object>of(
                "fish", List.of(
                    Map.<String, Object>of("identifier", "DIAMOND", "weight", 10, "amount", 1),
                    Map.<String, Object>of("identifier", "DIAMOND", "weight", 20, "amount", 2)
                )
            )
        ));
    }

    private static CustomDrop dropWithWeight(int weight) {
        return new CustomDrop(new DropConfigurationRecord(
            "COD",
            weight,
            1,
            null,
            List.of(),
            0,
            Map.of(),
            List.of(),
            false,
            null,
            List.of(),
            null
        ));
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

            List<String> strings = new java.util.ArrayList<>();
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
            throw new UnsupportedOperationException("Not needed for DropManager tests");
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
            throw new UnsupportedOperationException("Not needed for DropManager tests");
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

    private static final class FakePlayer implements PlatformPlayer {
        private final Set<String> permissions;

        private FakePlayer(Set<String> permissions) {
            this.permissions = Set.copyOf(permissions);
        }

        @Override
        public UUID getUniqueId() {
            return UUID.fromString("00000000-0000-0000-0000-000000000001");
        }

        @Override
        public String getName() {
            return "TestPlayer";
        }

        @Override
        public boolean isOnline() {
            return true;
        }

        @Override
        public boolean isOp() {
            return false;
        }

        @Override
        public void closeInventory() {
        }

        @Override
        public PlatformInventory getInventory() {
            return null;
        }

        @Override
        public void sendMessage(String message) {
        }

        @Override
        public boolean hasPermission(String permission) {
            return permissions.contains(permission);
        }
    }
}
