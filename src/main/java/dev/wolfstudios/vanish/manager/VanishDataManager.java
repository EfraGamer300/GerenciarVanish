package dev.wolfstudios.vanish.manager;

import dev.wolfstudios.vanish.LogManager;
import dev.wolfstudios.vanish.VanishPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class VanishDataManager {

    private final VanishPlugin plugin;
    private File vanishDataFile;
    private FileConfiguration vanishData;
    private final Set<UUID> savedVanish;

    public VanishDataManager(VanishPlugin plugin, Set<UUID> savedVanish) {
        this.plugin = plugin;
        this.savedVanish = savedVanish;
    }

    public void load() {
        vanishDataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!vanishDataFile.exists()) {
            try {
                vanishDataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        vanishData = YamlConfiguration.loadConfiguration(vanishDataFile);
    }

    public List<String> loadVanishedList() {
        List<String> loaded = vanishData.getStringList("vanished");
        int invalidCount = 0;
        List<String> valid = new ArrayList<>();
        LogManager lm = plugin.getLogManager();

        for (String uuidStr : loaded) {
            try {
                UUID uuid = UUID.fromString(uuidStr.trim());
                if (uuid.version() == 4 || uuid.version() == 3) {
                    valid.add(uuidStr.trim());
                    savedVanish.add(uuid);
                } else {
                    invalidCount++;
                }
            } catch (IllegalArgumentException e) {
                invalidCount++;
                if (lm != null) lm.log("INVALID", "Carregamento", "UUID invalido descartado: " + uuidStr);
            }
        }
        if (invalidCount > 0) {
            plugin.getLogger().warning("[Vanish+] " + invalidCount + " UUID(s) invalido(s) descartado(s) de data.yml");
        }
        return valid;
    }

    public void save(Set<UUID> vanished) {
        Set<String> unique = new LinkedHashSet<>();
        for (UUID uuid : savedVanish) unique.add(uuid.toString());
        for (UUID uuid : vanished) unique.add(uuid.toString());
        vanishData.set("vanished", new ArrayList<>(unique));
        try {
            vanishData.save(vanishDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Set<UUID> getSavedVanish() {
        return Collections.unmodifiableSet(savedVanish);
    }

    public void removeSaved(UUID uuid) {
        savedVanish.remove(uuid);
    }

    public void clearSaved() {
        savedVanish.clear();
    }
}
