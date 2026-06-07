package dev.wolfstudios.vanish;

import dev.wolfstudios.vanish.command.VanishCommand;
import dev.wolfstudios.vanish.command.VanishConfigCommand;
import dev.wolfstudios.vanish.gui.ConfigGUI;
import dev.wolfstudios.vanish.gui.ConfigGUIListener;
import dev.wolfstudios.vanish.listener.ProtectionListener;
import dev.wolfstudios.vanish.listener.SilentChestListener;
import dev.wolfstudios.vanish.manager.SilentChestManager;
import dev.wolfstudios.vanish.manager.VanishManager;
import dev.wolfstudios.vanish.storage.MysqlStorage;
import dev.wolfstudios.vanish.storage.SqliteStorage;
import dev.wolfstudios.vanish.storage.StorageBackend;
import dev.wolfstudios.vanish.storage.YamlStorage;
import dev.wolfstudios.vanish.update.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VanishPlugin extends JavaPlugin implements Listener {

    private final Set<UUID> savedVanish = Collections.synchronizedSet(new HashSet<>());
    private final Set<UUID> silentChestPlayers = Collections.synchronizedSet(new HashSet<>());

    private LogManager logManager;
    private LangManager langManager;
    private VanishManager vanishManager;
    private SilentChestManager silentChestManager;
    private StorageBackend storage;
    private ConfigGUI configGUI;
    private Metrics metrics;
    private UpdateChecker updateChecker;

    public LogManager logManager() { return logManager; }
    public LangManager langManager() { return langManager; }
    public VanishManager vanishManager() { return vanishManager; }
    public SilentChestManager silentChestManager() { return silentChestManager; }
    public StorageBackend storage() { return storage; }
    public Set<UUID> savedVanish() { return savedVanish; }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        logManager = new LogManager(this);
        logManager.init();

        langManager = new LangManager(this);
        langManager.load();

        int cooldownMs = Math.max(getConfig().getInt("settings.cooldown-ms", 1000), 250);

        vanishManager = new VanishManager(this, langManager, logManager);
        vanishManager.setCooldownMs(cooldownMs);

        silentChestManager = new SilentChestManager(silentChestPlayers);

        storage = createStorage();
        savedVanish.addAll(storage.loadVanished());

        configGUI = new ConfigGUI(this, langManager, silentChestManager);

        metrics = new Metrics(this, 31815);
        metrics.addCustomChart(new SimplePie("language", () -> langManager.getLanguage()));
        metrics.addCustomChart(new SimplePie("backend", () -> vanishManager.isFolia() ? "Folia" : "Bukkit/Spigot/Paper/Purpur"));
        metrics.addCustomChart(new SimplePie("storage", () -> getConfig().getString("storage.type", "yaml")));
        metrics.addCustomChart(new SingleLineChart("vanished_players", () -> vanishManager.getVanishedPlayers().size()));

        getCommand("vanish").setExecutor(new VanishCommand(this, vanishManager, langManager, logManager, storage));
        getCommand("vanishconfig").setExecutor(new VanishConfigCommand(this, configGUI, langManager));

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this, vanishManager), this);
        getServer().getPluginManager().registerEvents(new SilentChestListener(vanishManager, silentChestManager), this);
        getServer().getPluginManager().registerEvents(new ConfigGUIListener(configGUI), this);

        updateChecker = new UpdateChecker(this, logManager);
        updateChecker.checkAsync();

        logManager.log("SYSTEM", "Servidor", "Plugin ativado. Backend: " + (vanishManager.isFolia() ? "Folia" : "Bukkit/Spigot/Paper/Purpur") + " | Storage: " + getConfig().getString("storage.type", "yaml"));
        getLogger().info("Vanish+ ativado! Idioma: " + langManager.getLanguage() + " | Backend: " + (vanishManager.isFolia() ? "Folia" : "Bukkit/Spigot/Paper/Purpur") + " | Storage: " + getConfig().getString("storage.type", "yaml"));
    }

    @Override
    public void onDisable() {
        for (UUID uuid : new HashSet<>(vanishManager.getVanishedPlayers())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) vanishManager.revealPlayer(p);
            savedVanish.remove(uuid);
        }
        if (storage != null) {
            storage.saveVanished(vanishManager.getVanishedPlayers());
            storage.close();
        }
        if (metrics != null) metrics.shutdown();
        getLogger().info("Vanish+ desativado!");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        vanishManager.hideVanishedFrom(player);

        if (getConfig().getBoolean("settings.save-vanish-on-quit", true)
                && savedVanish.contains(player.getUniqueId())) {
            vanishManager.vanishPlayer(player);
            savedVanish.remove(player.getUniqueId());

            if (getConfig().getBoolean("settings.silent-joinquit", false)) {
                e.joinMessage(null);
            }
        }

        if (player.hasPermission("vanish.see") && !vanishManager.getVanishedPlayers().isEmpty()) {
            player.sendMessage(langManager.t("vanished-count")
                    .replace("{count}", String.valueOf(vanishManager.getVanishedPlayers().size())));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        boolean wasVanished = vanishManager.isVanished(player);

        if (wasVanished) {
            savedVanish.add(uuid);
            vanishManager.removeVanished(uuid);

            if (getConfig().getBoolean("settings.silent-joinquit", false)) {
                e.quitMessage(null);
            }
        } else {
            savedVanish.remove(uuid);
        }

        if (!vanishManager.getVanishedPlayers().isEmpty() || !savedVanish.isEmpty()) {
            storage.saveVanished(vanishManager.getVanishedPlayers());
        }
    }

    private StorageBackend createStorage() {
        FileConfiguration cfg = getConfig();
        return switch (cfg.getString("storage.type", "yaml").toLowerCase()) {
            case "mysql" -> new MysqlStorage(this,
                    cfg.getString("storage.mysql.host", "localhost"),
                    cfg.getInt("storage.mysql.port", 3306),
                    cfg.getString("storage.mysql.database", "vanishplus"),
                    cfg.getString("storage.mysql.username", "root"),
                    cfg.getString("storage.mysql.password", ""),
                    cfg.getString("storage.mysql.table-prefix", "vp_"));
            case "sqlite" -> new SqliteStorage(this);
            default -> new YamlStorage(this, logManager);
        };
    }
}
