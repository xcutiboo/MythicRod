package io.xcutiboo.mythicrod.drops;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;

public class DropSelector {
    private final Random random = ThreadLocalRandom.current();
    private final Logger logger;
    private final boolean usePermissions;
    private final boolean debugMode;

    public DropSelector(Logger logger, boolean usePermissions, boolean debugMode) {
        this.logger = logger;
        this.usePermissions = usePermissions;
        this.debugMode = debugMode;
    }

    public CustomDrop selectDrop(List<CustomDrop> drops, PlatformPlayer player, String biomeName) {
        List<CustomDrop> eligible = filterEligibleDrops(drops, player, biomeName);
        
        if (eligible.isEmpty()) {
            if (debugMode) {
                logger.info("No eligible drops for " + player.getName() + " in biome " + biomeName);
            }
            return null;
        }

        return selectWeightedRandom(eligible);
    }

    private List<CustomDrop> filterEligibleDrops(List<CustomDrop> drops, PlatformPlayer player, String biomeName) {
        List<CustomDrop> eligible = new ArrayList<>();
        
        for (CustomDrop drop : drops) {
            if (!hasPermission(player, drop)) continue;
            if (!matchesBiome(drop, biomeName)) continue;
            eligible.add(drop);
        }
        
        return eligible;
    }

    private boolean hasPermission(PlatformPlayer player, CustomDrop drop) {
        if (!usePermissions) return true;
        
        String permission = drop.getPermission();
        if (permission == null || permission.isEmpty()) return true;
        
        return player.hasPermission(permission);
    }

    private boolean matchesBiome(CustomDrop drop, String biomeName) {
        List<String> biomes = drop.getBiomes();
        if (biomes.isEmpty()) return true;
        
        return biomes.stream()
            .anyMatch(b -> b.equalsIgnoreCase(biomeName));
    }

    private CustomDrop selectWeightedRandom(List<CustomDrop> drops) {
        int totalWeight = drops.stream()
            .mapToInt(CustomDrop::getChance)
            .sum();
        
        int roll = random.nextInt(totalWeight);
        int currentWeight = 0;
        
        for (CustomDrop drop : drops) {
            currentWeight += drop.getChance();
            if (roll < currentWeight) {
                return drop;
            }
        }
        
        return drops.get(drops.size() - 1);
    }
}
