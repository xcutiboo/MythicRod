package io.xcutiboo.mythicrod.internal.runtime;

import java.io.File;
import java.util.logging.Logger;

import io.xcutiboo.mythicrod.api.platform.PlatformConfiguration;
import io.xcutiboo.mythicrod.api.platform.PlatformServer;

/**
 * Internal runtime bridge used by common-layer services.
 *
 * <p>This is intentionally separate from MythicRod's public API. Common code
 * depends on this contract so it can reach lifecycle-owned runtime services
 * without depending on the Paper plugin main class.
 */
public interface MythicRodRuntime {

    Logger getLogger();

    File getDataFolder();

    PlatformServer getPlatform();

    PlatformConfiguration loadConfig(File file);

    PlatformConfiguration createEmptyConfig();
}