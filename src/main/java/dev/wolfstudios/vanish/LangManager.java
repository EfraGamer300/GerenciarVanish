package dev.wolfstudios.vanish;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class LangManager {

    private final VanishPlugin plugin;
    private File langFile;
    private FileConfiguration lang;
    private String language = "en";

    private static final Set<String> SUPPORTED = new HashSet<>(Arrays.asList("en", "pt", "es", "fr", "de"));

    public LangManager(VanishPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        language = plugin.getConfig().getString("language", "en");

        if (!SUPPORTED.contains(language)) {
            plugin.getLogger().warning("[Vanish+] Lingua '" + language + "' nao suportada. Usando 'en'.");
            plugin.getLogger().warning("[Vanish+] Language '" + language + "' not supported. Using 'en'.");
            LogManager lm = plugin.getLogManager();
            if (lm != null) lm.log("SYSTEM", "Language", "Linguagem invalida '" + language + "', fallback para 'en'");
            language = "en";
        }

        for (String langKey : SUPPORTED) {
            plugin.saveResource("lang/" + langKey + ".yml", false);
        }

        langFile = new File(plugin.getDataFolder(), "lang/" + language + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().warning("[Vanish+] Language file '" + language + ".yml' not found. Using 'en'.");
            langFile = new File(plugin.getDataFolder(), "lang/en.yml");
        }
        lang = YamlConfiguration.loadConfiguration(langFile);
    }

    public void reload() {
        load();
    }

    public String t(String key) {
        if (lang == null) return key;
        String prefix = lang.getString("prefix", "&a&lVanish+ &8> &r");
        String message = lang.getString(key, "Vanish+: " + key);
        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }

    public String raw(String key) {
        if (lang == null) return key;
        String message = lang.getString(key, "Vanish+: " + key);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getLanguage() {
        return language;
    }

    public FileConfiguration getLangConfig() {
        return lang;
    }
}
