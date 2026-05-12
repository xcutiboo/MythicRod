package io.xcutiboo.mythicrod.paper.util;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Vibration;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;

/**
 * Shared particle-name helpers for admin commands and GUI controls.
 *
 * <p>The GUI intentionally cycles through a small curated list of readable,
 * low-noise particles. Commands may still accept any valid Paper particle enum.
 */
public final class ParticleOptions {
    private static final List<String> SUGGESTED_NAMES = List.of(
        "SPLASH",
        "BUBBLE_POP",
        "HAPPY_VILLAGER",
        "TOTEM_OF_UNDYING",
        "HEART",
        "NOTE",
        "FLAME",
        "END_ROD",
        "WITCH"
    );
    private static final List<String> CONFIGURABLE_NAMES = Arrays.stream(Particle.values())
        .filter(ParticleOptions::supportsDefaultData)
        .map(Enum::name)
        .sorted()
        .toList();

    private ParticleOptions() {
    }

    public static List<String> suggestedNames() {
        return SUGGESTED_NAMES;
    }

    public static List<String> configurableNames() {
        return CONFIGURABLE_NAMES;
    }

    public static String normalize(String particleName) {
        if (particleName == null || particleName.isBlank()) {
            return "";
        }
        return particleName.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean isConfigurableParticleName(String particleName) {
        String normalized = normalize(particleName);
        if (normalized.isEmpty()) {
            return false;
        }
        try {
            return supportsDefaultData(Particle.valueOf(normalized));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public static Object defaultDataFor(Particle particle, Location location) {
        Class<?> dataType = particle.getDataType();
        if (dataType == Void.class) {
            return null;
        }
        if (dataType == Color.class) {
            return Color.AQUA;
        }
        if (dataType == Particle.DustOptions.class) {
            return new Particle.DustOptions(Color.AQUA, 1.0F);
        }
        if (dataType == Particle.DustTransition.class) {
            return new Particle.DustTransition(Color.AQUA, Color.WHITE, 1.0F);
        }
        if (dataType == Particle.Spell.class) {
            return new Particle.Spell(Color.AQUA, 1.0F);
        }
        if (dataType == ItemStack.class) {
            return ItemStack.of(Material.COD);
        }
        if (dataType == BlockData.class) {
            return Material.SAND.createBlockData();
        }
        if (dataType == Float.class) {
            return 1.0F;
        }
        if (dataType == Integer.class) {
            return 0;
        }
        if (dataType == Vibration.class && location != null && location.getWorld() != null) {
            Location target = location.clone().add(0.0D, 1.0D, 0.0D);
            return new Vibration(new Vibration.Destination.BlockDestination(target), 20);
        }
        if (dataType == Particle.Trail.class && location != null && location.getWorld() != null) {
            return new Particle.Trail(location.clone().add(0.0D, 1.0D, 0.0D), Color.AQUA, 20);
        }
        return null;
    }

    public static String nextSuggested(String currentName) {
        return move(currentName, 1);
    }

    private static String move(String currentName, int direction) {
        String normalized = normalize(currentName);
        int index = SUGGESTED_NAMES.indexOf(normalized);
        if (index < 0) {
            return SUGGESTED_NAMES.get(0);
        }

        int nextIndex = Math.floorMod(index + direction, SUGGESTED_NAMES.size());
        return SUGGESTED_NAMES.get(nextIndex);
    }

    private static boolean supportsDefaultData(Particle particle) {
        Class<?> dataType = particle.getDataType();
        return dataType == Void.class
            || dataType == Color.class
            || dataType == Particle.DustOptions.class
            || dataType == Particle.DustTransition.class
            || dataType == Particle.Spell.class
            || dataType == ItemStack.class
            || dataType == BlockData.class
            || dataType == Float.class
            || dataType == Integer.class
            || dataType == Vibration.class
            || dataType == Particle.Trail.class;
    }
}
