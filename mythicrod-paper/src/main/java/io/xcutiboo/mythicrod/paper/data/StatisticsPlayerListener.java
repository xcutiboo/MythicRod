package io.xcutiboo.mythicrod.paper.data;

import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import io.xcutiboo.mythicrod.MythicRod;

public final class StatisticsPlayerListener implements Listener {
    private final MythicRod plugin;

    public StatisticsPlayerListener(MythicRod plugin) {
        this.plugin = plugin;
    }

    public void preloadOnlinePlayers() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player != null && player.isOnline()) {
                queueLoad(player.getUniqueId(), player.getName());
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        queueLoad(player.getUniqueId(), player.getName());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        queueUnload(player.getUniqueId());
    }

    private void queueLoad(UUID playerId, String playerName) {
        plugin.getPlatformScheduler().runAsync(() -> {
            try {
                plugin.getStatisticsManager().loadPlayer(playerId, playerName);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                    "[MythicRod-StatisticsPlayerListener] Failed to load statistics for " + playerId, e);
            }
        });
    }

    private void queueUnload(UUID playerId) {
        plugin.getPlatformScheduler().runAsync(() -> {
            try {
                plugin.getStatisticsManager().unloadPlayer(playerId);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                    "[MythicRod-StatisticsPlayerListener] Failed to unload statistics for " + playerId, e);
            }
        });
    }
}
