package io.xcutiboo.mythicrod.paper.data;

import io.xcutiboo.mythicrod.MythicRod;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages per-player persistent data (rod tier, auto-retrieve flag).
 *
 * <p>Caches PDC reads in {@link ConcurrentHashMap}s for performance.
 * Implements {@link Listener} so that it can evict stale cache entries when a
 * player disconnects — preventing unbounded memory growth in long-running servers.
 * Must be registered with {@code plugin.getServer().getPluginManager().registerEvents(this, plugin)}.
 */
public class PlayerDataService implements Listener {
    private final NamespacedKey rodTierKey;
    private final NamespacedKey autoRetrieveKey;
    private final Map<UUID, String>  tierCache          = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> autoRetrieveCache  = new ConcurrentHashMap<>();

    private static final String DEFAULT_TIER = "basic";

    // Bukkit event handlers fire only after onEnable() returns, so 'this' is fully
    // initialised before onPlayerQuit can ever be invoked.  The Java 21 this-escape
    // lint warning is a false-positive here; suppress it explicitly.
    @SuppressWarnings("this-escape")
    public PlayerDataService(MythicRod plugin) {
        this.rodTierKey      = new NamespacedKey(plugin, "mythicrod_tier");
        this.autoRetrieveKey = new NamespacedKey(plugin, "mythicrod_autoretrieve");
        // Self-register so onPlayerQuit fires without requiring a separate call-site.
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * CACHE-LEAK FIX: Evict per-player cache entries on disconnect.
     * Without this handler the ConcurrentHashMaps grow indefinitely —
     * one entry per unique player that joins, never removed.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clearCache(event.getPlayer().getUniqueId());
    }
    
    public String getRodTier(Player player) {
        return tierCache.computeIfAbsent(player.getUniqueId(), uuid -> {
            PersistentDataContainer pdc = player.getPersistentDataContainer();
            if (pdc.has(rodTierKey, PersistentDataType.STRING)) {
                String tier = pdc.get(rodTierKey, PersistentDataType.STRING);
                return tier != null ? tier : DEFAULT_TIER;
            }
            return DEFAULT_TIER;
        });
    }
    
    public void setRodTier(Player player, String tier) {
        player.getPersistentDataContainer().set(rodTierKey, PersistentDataType.STRING, tier);
        tierCache.put(player.getUniqueId(), tier);
    }
    
    public boolean hasAutoRetrieve(Player player) {
        return autoRetrieveCache.computeIfAbsent(player.getUniqueId(), uuid -> {
            PersistentDataContainer pdc = player.getPersistentDataContainer();
            if (pdc.has(autoRetrieveKey, PersistentDataType.BYTE)) {
                Byte value = pdc.get(autoRetrieveKey, PersistentDataType.BYTE);
                return value != null && value == (byte) 1;
            }
            return false;
        });
    }
    
    public void setAutoRetrieve(Player player, boolean enabled) {
        player.getPersistentDataContainer().set(autoRetrieveKey, PersistentDataType.BYTE, enabled ? (byte) 1 : (byte) 0);
        autoRetrieveCache.put(player.getUniqueId(), enabled);
    }
    
    public void toggleAutoRetrieve(Player player) {
        setAutoRetrieve(player, !hasAutoRetrieve(player));
    }
    
    public void clearCache(UUID uuid) {
        tierCache.remove(uuid);
        autoRetrieveCache.remove(uuid);
    }
    
    public void clearAllCache() {
        tierCache.clear();
        autoRetrieveCache.clear();
    }
}
