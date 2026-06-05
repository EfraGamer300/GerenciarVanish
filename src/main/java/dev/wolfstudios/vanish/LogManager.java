package dev.wolfstudios.vanish;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogManager {

    private final VanishPlugin plugin;
    private File logFile;

    public LogManager(VanishPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        File logDir = new File(plugin.getDataFolder(), "logs");
        if (!logDir.exists()) logDir.mkdirs();
        logFile = new File(logDir, "vanish.log");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void log(String type, String source, String message) {
        if (logFile == null) return;
        try (PrintWriter pw = new PrintWriter(new FileWriter(logFile, true))) {
            String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            pw.println("[" + ts + "] [" + type + "] [" + source + "] " + message);
        } catch (IOException e) {
            plugin.getLogger().warning("[Vanish+] Failed to write log: " + e.getMessage());
        }
    }
}
