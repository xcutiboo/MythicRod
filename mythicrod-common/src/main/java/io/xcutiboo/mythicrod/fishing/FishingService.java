package io.xcutiboo.mythicrod.fishing;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.xcutiboo.mythicrod.MythicRodPlugin;
import io.xcutiboo.mythicrod.api.platform.PlatformLocation;
import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;
import io.xcutiboo.mythicrod.api.platform.PlatformServer;
import io.xcutiboo.mythicrod.drops.CustomDrop;

public class FishingService {
    private final MythicRodPlugin plugin;
    private final Map<UUID, FishingState> activeFishing = new ConcurrentHashMap<>();

    public FishingService(MythicRodPlugin plugin) {
        this.plugin = plugin;
    }

    public void startFishing(UUID hookId, UUID playerId, PlatformLocation castLocation) {
        FishingState state = new FishingState(playerId, castLocation);
        activeFishing.put(hookId, state);
    }

    public FishingResult processCatch(UUID hookId, PlatformPlayer player, PlatformLocation hookLocation, String biomeName) {
        FishingState state = activeFishing.computeIfAbsent(
            hookId,
            id -> new FishingState(player.getUniqueId(), hookLocation)
        );

        if (state.hasReceivedReward()) {
            activeFishing.remove(hookId);
            return FishingResult.alreadyProcessed();
        }

        state.setReceivedReward(true);

        if (hookLocation == null || hookLocation.getWorldName() == null) {
            activeFishing.remove(hookId);
            return FishingResult.invalidLocation();
        }

        CustomDrop drop = plugin.getDropManager().getRandomDrop(player, biomeName);

        if (drop == null) {
            activeFishing.remove(hookId);
            return FishingResult.noDrop();
        }

        activeFishing.remove(hookId);
        return FishingResult.success(drop, biomeName);
    }

    public void endFishing(UUID hookId) {
        activeFishing.remove(hookId);
    }

    public void cleanupStaleHooks(PlatformServer server) {
        activeFishing.entrySet().removeIf(entry -> {
            UUID hookId = entry.getKey();
            FishingState state = entry.getValue();

            if (!server.isEntityValid(hookId)) {
                return true;
            }

            return System.currentTimeMillis() - state.getStartTime() > 300000;
        });
    }

    public static class FishingState {
        private final long startTime;
        private final UUID playerId;
        private final String worldName;
        private final double x, y, z;
        private boolean receivedReward;

        public FishingState(UUID playerId, PlatformLocation castLocation) {
            this.startTime = System.currentTimeMillis();
            this.playerId = playerId;
            this.worldName = castLocation != null ? castLocation.getWorldName() : null;
            this.x = castLocation != null ? castLocation.getX() : 0;
            this.y = castLocation != null ? castLocation.getY() : 0;
            this.z = castLocation != null ? castLocation.getZ() : 0;
            this.receivedReward = false;
        }

        public long getStartTime() {
            return startTime;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public String getWorldName() {
            return worldName;
        }
        
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }

        public boolean hasReceivedReward() {
            return receivedReward;
        }

        public void setReceivedReward(boolean receivedReward) {
            this.receivedReward = receivedReward;
        }
    }

    public static class FishingResult {
        private final ResultType type;
        private final CustomDrop drop;
        private final String biomeName;

        private FishingResult(ResultType type, CustomDrop drop, String biomeName) {
            this.type = type;
            this.drop = drop;
            this.biomeName = biomeName;
        }

        public static FishingResult success(CustomDrop drop, String biomeName) {
            return new FishingResult(ResultType.SUCCESS, drop, biomeName);
        }

        public static FishingResult alreadyProcessed() {
            return new FishingResult(ResultType.ALREADY_PROCESSED, null, null);
        }

        public static FishingResult invalidLocation() {
            return new FishingResult(ResultType.INVALID_LOCATION, null, null);
        }

        public static FishingResult noDrop() {
            return new FishingResult(ResultType.NO_DROP, null, null);
        }

        public boolean isSuccess() {
            return type == ResultType.SUCCESS;
        }

        public ResultType getType() {
            return type;
        }

        public CustomDrop getDrop() {
            return drop;
        }

        public String getBiomeName() {
            return biomeName;
        }

        public enum ResultType {
            SUCCESS,
            ALREADY_PROCESSED,
            INVALID_LOCATION,
            NO_DROP
        }
    }
}
