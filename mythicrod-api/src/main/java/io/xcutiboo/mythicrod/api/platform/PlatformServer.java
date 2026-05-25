package io.xcutiboo.mythicrod.api.platform;

import java.io.File;
import java.io.InputStream;
import java.util.UUID;
import java.util.logging.Logger;

/// Platform services required by MythicRod's shared implementation.
///
/// This is not a general Bukkit replacement. It keeps common code away from
/// Paper classes while making lifecycle, configuration, item creation,
/// scheduling, and command operations explicit.
public interface PlatformServer {

    /// Returns the logger used by the hosting platform implementation.
    ///
    /// @return platform logger
    Logger getLogger();

    /// Returns the scheduler facade for owner-aware task handoff.
    ///
    /// @return platform scheduler facade
    PlatformScheduler getScheduler();

    /// Finds an online player by UUID.
    ///
    /// @param uuid player UUID
    /// @return player view, or `null` when unavailable
    PlatformPlayer getPlayer(UUID uuid);

    /// Finds a command sender by player name, falling back to console when no
    /// matching online player exists.
    ///
    /// @param name sender or player name
    /// @return sender view
    PlatformCommandSender getCommandSender(String name);

    /// Checks whether an entity is currently known to the platform.
    ///
    /// @param entityId entity UUID
    /// @return `true` when the platform can resolve it
    boolean isEntityValid(UUID entityId);

    /// Returns whether the Nexo integration is currently enabled.
    ///
    /// @return `true` when Nexo is available for item creation
    boolean isNexoEnabled();

    /// Loads configuration from a file.
    ///
    /// @param file source file
    /// @return platform configuration wrapper
    PlatformConfiguration loadConfiguration(File file);

    /// Loads configuration from a stream.
    ///
    /// @param stream source stream
    /// @return platform configuration wrapper
    PlatformConfiguration loadConfiguration(InputStream stream);

    /// Creates an empty mutable configuration wrapper.
    ///
    /// @return empty platform configuration
    PlatformConfiguration createEmptyConfiguration();

    /// Looks up a world by name.
    ///
    /// @param name world name
    /// @return world view, or `null` when unavailable
    PlatformWorld getWorld(String name);

    /// Returns the runtime item factory.
    ///
    /// @return platform item factory
    PlatformItemFactory getItemFactory();

    /// Dispatches a command as console.
    ///
    /// @param command command line without a leading slash
    void dispatchCommandConsole(String command);

    /// Broadcasts a MiniMessage-formatted message through the platform.
    ///
    /// @param message MiniMessage text
    void broadcastMessage(String message);
}
