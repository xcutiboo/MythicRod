package io.xcutiboo.mythicrod.paper.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import io.xcutiboo.mythicrod.api.platform.PlatformConfiguration;

public class PaperConfiguration implements PlatformConfiguration {

    private final ConfigurationSection config;

    public PaperConfiguration(ConfigurationSection config) {
        this.config = config;
    }

    public PaperConfiguration(File file) {
        this(YamlConfiguration.loadConfiguration(file));
    }

    public PaperConfiguration(InputStream stream) {
        this(YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8)));
    }

    public PaperConfiguration() {
        this(new YamlConfiguration());
    }

    @Override
    public boolean contains(String path) {
        return config.contains(path);
    }

    @Override
    public String getString(String path) {
        return config.getString(path);
    }

    @Override
    public String getString(String path, String def) {
        return config.getString(path, def);
    }

    @Override
    public int getInt(String path) {
        return config.getInt(path);
    }

    @Override
    public int getInt(String path, int def) {
        return config.getInt(path, def);
    }

    @Override
    public boolean getBoolean(String path) {
        return config.getBoolean(path);
    }

    @Override
    public boolean getBoolean(String path, boolean def) {
        return config.getBoolean(path, def);
    }

    @Override
    public double getDouble(String path) {
        return config.getDouble(path);
    }

    @Override
    public double getDouble(String path, double def) {
        return config.getDouble(path, def);
    }

    @Override
    public List<String> getStringList(String path) {
        return config.getStringList(path);
    }

    @Override
    public List<Map<?, ?>> getMapList(String path) {
        return config.getMapList(path);
    }

    @Override
    public void set(String path, Object value) {
        config.set(path, value);
    }

    @Override
    public Set<String> getKeys(boolean deep) {
        return config.getKeys(deep);
    }

    @Override
    public PlatformConfiguration getSection(String path) {
        ConfigurationSection section = config.getConfigurationSection(path);
        return section != null ? new PaperConfiguration(section) : null;
    }

    @Override
    public void save(File file) throws IOException {
        if (config instanceof FileConfiguration fileConfig) {
            fileConfig.save(file);
        } else {
            throw new UnsupportedOperationException("Cannot save a sub-section of configuration directly to a file.");
        }
    }
}
