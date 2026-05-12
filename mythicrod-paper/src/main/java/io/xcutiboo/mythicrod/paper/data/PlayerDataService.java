package io.xcutiboo.mythicrod.paper.data;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import io.xcutiboo.mythicrod.MythicRod;

/**
 * Manages per-player persistent data used by Paper-only gameplay and UI code.
 *
 * <p>The data lives in the player's {@link PersistentDataContainer}, so it moves
 * with normal player data and does not leak Paper presentation preferences into
 * the shared module.
 */
public class PlayerDataService implements Listener {
    private static final Logger LOGGER = Logger.getLogger("MythicRod");
    private static final String DEFAULT_TIER = "basic";

    private final NamespacedKey rodTierKey;
    private final NamespacedKey reducedEffectsKey;
    private final Map<UUID, String>  tierCache          = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> reducedEffectsCache = new ConcurrentHashMap<>();

    public PlayerDataService(MythicRod plugin) {
        this.rodTierKey      = new NamespacedKey(plugin, "mythicrod_tier");
        this.reducedEffectsKey = new NamespacedKey(plugin, "mythicrod_reduced_effects");
    }

    /**
     * Evicts per-player cache entries when a player leaves.
     *
     * <p>Without this handler the caches retain one entry per unique player
     * until plugin shutdown.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        try {
            clearCache(event.getPlayer().getUniqueId());
        } catch (Exception e) {
            warning(e, () -> "Failed to handle player quit cache eviction");
        }
    }

    public String getRodTier(Player player) {
        try {
            if (player == null) {
                return DEFAULT_TIER;
            }
            return tierCache.computeIfAbsent(player.getUniqueId(), uuid -> {
                try {
                    PersistentDataContainer pdc = player.getPersistentDataContainer();
                    if (pdc.has(rodTierKey, PersistentDataType.STRING)) {
                        String tier = pdc.get(rodTierKey, PersistentDataType.STRING);
                        return tier != null ? tier : DEFAULT_TIER;
                    }
                } catch (Exception e) {
                    warning(e, () -> "Failed to read rod tier for " + uuid);
                }
                return DEFAULT_TIER;
            });
        } catch (Exception e) {
            warning(e, () -> "Failed to resolve rod tier");
            return DEFAULT_TIER;
        }
    }

    public void setRodTier(Player player, String tier) {
        try {
            if (player == null || tier == null) {
                return;
            }
            player.getPersistentDataContainer().set(rodTierKey, PersistentDataType.STRING, tier);
            tierCache.put(player.getUniqueId(), tier);
        } catch (Exception e) {
            warning(e, () -> "Failed to persist rod tier");
        }
    }

    public boolean hasReducedEffects(Player player) {
        try {
            if (player == null) {
                return false;
            }
            return reducedEffectsCache.computeIfAbsent(player.getUniqueId(), uuid -> {
                try {
                    PersistentDataContainer pdc = player.getPersistentDataContainer();
                    if (pdc.has(reducedEffectsKey, PersistentDataType.BYTE)) {
                        Byte value = pdc.get(reducedEffectsKey, PersistentDataType.BYTE);
                        return value != null && value == (byte) 1;
                    }
                } catch (Exception e) {
                    warning(e, () -> "Failed to read reduced-effects preference for " + uuid);
                }
                return false;
            });
        } catch (Exception e) {
            warning(e, () -> "Failed to resolve reduced-effects preference");
            return false;
        }
    }

    public void setReducedEffects(Player player, boolean enabled) {
        try {
            if (player == null) {
                return;
            }
            player.getPersistentDataContainer().set(reducedEffectsKey, PersistentDataType.BYTE, enabled ? (byte) 1 : (byte) 0);
            reducedEffectsCache.put(player.getUniqueId(), enabled);
        } catch (Exception e) {
            warning(e, () -> "Failed to persist reduced-effects preference");
        }
    }

    public void toggleReducedEffects(Player player) {
        try {
            setReducedEffects(player, !hasReducedEffects(player));
        } catch (Exception e) {
            warning(e, () -> "Failed to toggle reduced-effects preference");
        }
    }

    public void clearCache(UUID uuid) {
        try {
            if (uuid != null) {
                tierCache.remove(uuid);
                reducedEffectsCache.remove(uuid);
            }
        } catch (Exception e) {
            warning(e, () -> "Failed to clear player-data cache for " + uuid);
        }
    }

    public void clearAllCache() {
        try {
            tierCache.clear();
            reducedEffectsCache.clear();
        } catch (Exception e) {
            warning(e, () -> "Failed to clear all player-data caches");
        }
    }

    private void warning(Throwable thrown, Supplier<String> messageSupplier) {
        LOGGER.log(Level.WARNING, thrown, messageSupplier);
    }
}
