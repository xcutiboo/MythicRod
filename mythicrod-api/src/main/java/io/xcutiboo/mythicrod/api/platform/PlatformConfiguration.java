package io.xcutiboo.mythicrod.api.platform;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Minimal configuration adapter used by MythicRod's shared code.
///
/// This keeps the common module independent from Bukkit configuration types
/// while still supporting the YAML-style lookups MythicRod uses internally.
public interface PlatformConfiguration {

    /// Returns whether the supplied path exists in this configuration.
    ///
    /// @param path dot-separated path
    /// @return `true` when a value exists at that path
    boolean contains(String path);

    /// Reads a string value from the configuration.
    ///
    /// @param path dot-separated path
    /// @return stored string value, or `null` when absent or not a string-like value
    String getString(String path);

    /// Reads a string value or returns a fallback.
    ///
    /// @param path dot-separated path
    /// @param def fallback value when absent
    /// @return stored string value or the supplied default
    String getString(String path, String def);

    /// Reads an integer value from the configuration.
    ///
    /// @param path dot-separated path
    /// @return stored integer value, or the platform default when absent
    int getInt(String path);

    /// Reads an integer value or returns a fallback.
    ///
    /// @param path dot-separated path
    /// @param def fallback value when absent
    /// @return stored integer value or the supplied default
    int getInt(String path, int def);

    /// Reads a boolean value from the configuration.
    ///
    /// @param path dot-separated path
    /// @return stored boolean value, or the platform default when absent
    boolean getBoolean(String path);

    /// Reads a boolean value or returns a fallback.
    ///
    /// @param path dot-separated path
    /// @param def fallback value when absent
    /// @return stored boolean value or the supplied default
    boolean getBoolean(String path, boolean def);

    /// Reads a double value from the configuration.
    ///
    /// @param path dot-separated path
    /// @return stored double value, or the platform default when absent
    double getDouble(String path);

    /// Reads a double value or returns a fallback.
    ///
    /// @param path dot-separated path
    /// @param def fallback value when absent
    /// @return stored double value or the supplied default
    double getDouble(String path, double def);

    /// Reads a string list from the configuration.
    ///
    /// @param path dot-separated path
    /// @return snapshot list; callers must not assume mutations write through
    List<String> getStringList(String path);

    /// Reads a list of mapped configuration values.
    ///
    /// @param path dot-separated path
    /// @return snapshot list; callers must not assume mutations write through
    List<Map<?, ?>> getMapList(String path);

    /// Writes a value to the supplied path.
    ///
    /// @param path dot-separated path
    /// @param value value to store
    void set(String path, Object value);

    /// Returns keys in this configuration section.
    ///
    /// @param deep whether nested keys should be included
    /// @return snapshot set of keys
    Set<String> getKeys(boolean deep);

    /// Returns keys under the requested section path.
    ///
    /// @param path section path, or empty for the root
    /// @param deep whether nested keys should be included
    /// @return snapshot set of keys for the requested section
    default Set<String> getKeys(String path, boolean deep) {
        if (path == null || path.isEmpty()) {
            return getKeys(deep);
        }
        PlatformConfiguration section = getSection(path);
        return section != null ? section.getKeys(deep) : Collections.emptySet();
    }

    /// Returns a nested configuration section.
    ///
    /// @param path dot-separated section path
    /// @return nested section wrapper, or `null` when absent
    PlatformConfiguration getSection(String path);

    /// Persists the current configuration state to disk.
    ///
    /// @param file destination file
    /// @throws IOException when the underlying platform serializer cannot write the file
    void save(File file) throws IOException;
}
