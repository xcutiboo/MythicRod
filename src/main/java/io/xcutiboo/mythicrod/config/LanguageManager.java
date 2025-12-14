package io.xcutiboo.mythicrod.config;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import io.xcutiboo.mythicrod.MythicRod;
public class LanguageManager {
    private final MythicRod plugin;
    private final ConfigManager configManager;
    private String languageCode = "en";
    private FileConfiguration baseEn;
    private FileConfiguration selectedLang;
    public LanguageManager(MythicRod plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.languageCode = safeLang(configManager.getConfig().getString("language", "en"));
        ensureResourceExists("messages_en.yml");
        ensureResourceExists("messages_jp.yml");
        ensureResourceExists("messages.yml");
        loadBundles();
    }
    private String safeLang(String code) {
        return (code == null || code.isEmpty()) ? "en" : code.toLowerCase();
    }
    private void ensureResourceExists(String resourceName) {
        File out = new File(plugin.getDataFolder(), resourceName);
        if (!out.exists()) {
            plugin.saveResource(resourceName, false);
        }
    }
    private FileConfiguration loadFromDataOrJar(String fileName, String jarFallback) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (file.exists()) {
            return YamlConfiguration.loadConfiguration(file);
        }
        InputStream is = plugin.getResource(jarFallback);
        if (is != null) {
            return YamlConfiguration.loadConfiguration(new InputStreamReader(is, StandardCharsets.UTF_8));
        }
        return new YamlConfiguration();
    }
    private void loadBundles() {
        baseEn = loadFromDataOrJar("messages_en.yml", "messages_en.yml");
        if (isEmptyYaml(baseEn)) {
            baseEn = loadFromDataOrJar("messages.yml", "messages.yml");
        }
        String selectedFile = "messages_" + languageCode + ".yml";
        selectedLang = loadFromDataOrJar(selectedFile, selectedFile);
    }
    private boolean isEmptyYaml(FileConfiguration cfg) {
        return cfg == null || cfg.getKeys(true).isEmpty();
    }
    public void setLanguage(String code) {
        this.languageCode = safeLang(code);
        loadBundles();
    }
    public String getLanguage() {
        return languageCode;
    }
    public String tr(String key) {
        String val = selectedLang.getString(key);
        if (val == null) {
            val = baseEn.getString(key);
        }
        return Objects.requireNonNullElse(val, key);
    }
    public String tr(String key, Map<String, String> placeholders) {
        String msg = tr(key);
        if (placeholders == null || placeholders.isEmpty()) {
            return msg;
        }
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            String ph = "{" + e.getKey() + "}";
            msg = msg.replace(ph, e.getValue());
        }
        return msg;
    }
}
