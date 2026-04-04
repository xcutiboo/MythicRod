package io.xcutiboo.mythicrod.paper.platform;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;

import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;
import lombok.Getter;

public class PlatformPlayerRegistry {
    @Getter
    private final Map<UUID, PaperPlayer> activePlayers = new ConcurrentHashMap<>();
    
    public PlatformPlayer get(Player nativePlayer) {
        return activePlayers.computeIfAbsent(
            nativePlayer.getUniqueId(),
            id -> new PaperPlayer(nativePlayer)
        );
    }
    
    public void remove(UUID playerId) {
        activePlayers.remove(playerId);
    }
    
    public void clear() {
        activePlayers.clear();
    }
}
