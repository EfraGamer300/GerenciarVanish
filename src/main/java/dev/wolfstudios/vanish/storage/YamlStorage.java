package dev.wolfstudios.vanish.storage;

import dev.wolfstudios.vanish.LogManager;
import dev.wolfstudios.vanish.VanishPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class YamlStorage implements StorageBackend {

    private final VanishPlugin plugin;
    private final LogManager log;
    private File vanishDataFile;
    private FileConfiguration vanishData;

    public YamlStorage(VanishPlugin plugin, LogManager log) {
        this.plugin = plugin;
        this.log = log;
    }

    @Override
    public Set<UUID> loadVanished() {
        vanishDataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!vanishDataFile.exists()) {
            try {
                if (!vanishDataFile.createNewFile()) {
                    plugin.getLogger().warning("[Vanish+] Nao foi possivel criar data.yml");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("[Vanish+] Erro ao criar data.yml: " + e.getMessage());
            }
        }
        vanishData = YamlConfiguration.loadConfiguration(vanishDataFile);
        List<String> loaded = vanishData.getStringList("vanished");
        Set<UUID> result = new LinkedHashSet<>();
        int invalid = 0;

        for (String uuidStr : loaded) {
            try {
                UUID uuid = UUID.fromString(uuidStr.trim());
                if (uuid.version() == 4 || uuid.version() == 3) {
                    result.add(uuid);
                } else {
                    invalid++;
                }
            } catch (IllegalArgumentException e) {
                invalid++;
                if (log != null) log.log("INVALID", "Carregamento", "UUID invalido descartado: " + uuidStr);
            }
        }

        if (invalid > 0) {
            plugin.getLogger().warning("[Vanish+] %d UUID(s) invalido(s) descartado(s) de data.yml".formatted(invalid));
        }
        return result;
    }

    @Override
    public void saveVanished(Set<UUID> vanished) {
        if (vanishData == null || vanishDataFile == null) return;
        List<String> list = vanished.stream().map(UUID::toString).toList();
        vanishData.set("vanished", list);
        try {
            vanishData.save(vanishDataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[Vanish+] Erro ao salvar data.yml: " + e.getMessage());
        }
    }

    @Override
    public void removeVanished(UUID uuid) {
        if (vanishData == null || vanishDataFile == null) return;
        List<String> list = vanishData.getStringList("vanished");
        list.remove(uuid.toString());
        vanishData.set("vanished", list);
        try {
            vanishData.save(vanishDataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[Vanish+] Erro ao salvar data.yml: " + e.getMessage());
        }
    }

    @Override
    public void close() {
    }
}
