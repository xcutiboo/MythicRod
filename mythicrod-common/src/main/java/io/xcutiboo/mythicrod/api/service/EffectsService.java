package io.xcutiboo.mythicrod.api.service;

import io.xcutiboo.mythicrod.api.platform.PlatformLocation;
import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;

public interface EffectsService {
    void spawnCatchEffects(PlatformPlayer player, PlatformLocation location);
    void spawnExperienceEffects(PlatformPlayer player, int amount);
}
