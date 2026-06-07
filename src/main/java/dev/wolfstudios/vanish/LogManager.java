package dev.wolfstudios.vanish;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogManager {

    private final VanishPlugin plugin;
    private File logFile;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public LogManager(VanishPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        File logDir = new File(plugin.getDataFolder(), "logs");
        if (!logDir.exists() && !logDir.mkdirs()) {
            plugin.getLogger().warning("[Vanish+] Nao foi possivel criar diretorio de logs");
            return;
        }
        logFile = new File(logDir, "vanish.log");
        try {
            if (!logFile.exists() && !logFile.createNewFile()) {
                plugin.getLogger().warning("[Vanish+] Nao foi possivel criar arquivo de log");
            }
        } catch (IOException e) {
            plugin.getLogger().warning("[Vanish+] Erro ao criar log: " + e.getMessage());
        }
    }

    public void log(String type, String source, String message) {
        if (logFile == null) return;
        try (var pw = new PrintWriter(new FileWriter(logFile, true))) {
            pw.println("[%s] [%s] [%s] %s".formatted(
                    LocalDateTime.now().format(DTF), type, source, message));
        } catch (IOException e) {
            plugin.getLogger().warning("[Vanish+] Failed to write log: " + e.getMessage());
        }
    }
}
