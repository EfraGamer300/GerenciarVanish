package dev.wolfstudios.vanish;

import dev.wolfstudios.vanish.command.VanishCommand;
import dev.wolfstudios.vanish.command.VanishConfigCommand;
import dev.wolfstudios.vanish.gui.ConfigGUI;
import dev.wolfstudios.vanish.gui.ConfigGUIListener;
import dev.wolfstudios.vanish.listener.ProtectionListener;
import dev.wolfstudios.vanish.listener.SilentChestListener;
import dev.wolfstudios.vanish.manager.SilentChestManager;
import dev.wolfstudios.vanish.manager.VanishDataManager;
import dev.wolfstudios.vanish.manager.VanishManager;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
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

    private final Set<UUID> savedVanish = Collections.synchronizedSet(new HashSet<UUID>());
    private final Set<UUID> silentChestPlayers = Collections.synchronizedSet(new HashSet<UUID>());

    private LogManager logManager;
    private LangManager langManager;
    private VanishManager vanishManager;
    private SilentChestManager silentChestManager;
    private VanishDataManager dataManager;
    private ConfigGUI configGUI;
    private Metrics metrics;

    public Metrics getMetrics() { return metrics; }

    public LogManager getLogManager() { return logManager; }
    public LangManager getLangManager() { return langManager; }
    public VanishManager getVanishManager() { return vanishManager; }
    public SilentChestManager getSilentChestManager() { return silentChestManager; }
    public VanishDataManager getDataManager() { return dataManager; }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        logManager = new LogManager(this);
        logManager.init();

        langManager = new LangManager(this);
        langManager.load();

        int cooldownMs = getConfig().getInt("settings.cooldown-ms", 1000);
        if (cooldownMs < 250) cooldownMs = 250;

        vanishManager = new VanishManager(this, langManager, logManager);
        vanishManager.setCooldownMs(cooldownMs);

        silentChestManager = new SilentChestManager(silentChestPlayers);

        dataManager = new VanishDataManager(this, savedVanish);
        dataManager.load();
        dataManager.loadVanishedList();

        configGUI = new ConfigGUI(this, langManager, silentChestManager);

        metrics = new Metrics(this, 31815);
        metrics.addCustomChart(new SimplePie("language", () -> langManager.getLanguage()));
        metrics.addCustomChart(new SimplePie("backend", () -> vanishManager.isFolia() ? "Folia" : "Bukkit/Spigot/Paper/Purpur"));
        metrics.addCustomChart(new SingleLineChart("vanished_players", () -> vanishManager.getVanishedPlayers().size()));

        getCommand("vanish").setExecutor(new VanishCommand(this, vanishManager, langManager, logManager, dataManager));
        getCommand("vanishconfig").setExecutor(new VanishConfigCommand(this, configGUI, langManager));

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this, vanishManager), this);
        getServer().getPluginManager().registerEvents(new SilentChestListener(vanishManager, silentChestManager), this);
        getServer().getPluginManager().registerEvents(new ConfigGUIListener(configGUI), this);

        logManager.log("SYSTEM", "Servidor", "Plugin ativado. Backend: " + (vanishManager.isFolia() ? "Folia" : "Bukkit/Spigot/Paper/Purpur"));
        getLogger().info("Vanish+ ativado! Idioma: " + langManager.getLanguage() + " | Backend: " + (vanishManager.isFolia() ? "Folia" : "Bukkit/Spigot/Paper/Purpur"));
    }

    @Override
    public void onDisable() {
        for (UUID uuid : new HashSet<>(vanishManager.getVanishedPlayers())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) vanishManager.revealPlayer(p);
            savedVanish.remove(uuid);
        }
        dataManager.save(vanishManager.getVanishedPlayers());
        if (metrics != null) metrics.shutdown();
        getLogger().info("Vanish+ desativado!");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        vanishManager.hideVanishedFrom(player);

        if (getConfig().getBoolean("settings.save-vanish-on-quit", true)) {
            if (savedVanish.contains(player.getUniqueId())) {
                vanishManager.vanishPlayer(player);
                savedVanish.remove(player.getUniqueId());
            }
        }

        if (player.hasPermission("vanish.see") && !vanishManager.getVanishedPlayers().isEmpty()) {
            player.sendMessage(langManager.t("vanished-count").replace("{count}", String.valueOf(vanishManager.getVanishedPlayers().size())));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        if (vanishManager.isVanished(player)) {
            savedVanish.add(uuid);
            vanishManager.removeVanished(uuid);
        } else {
            savedVanish.remove(uuid);
        }
        if (!vanishManager.getVanishedPlayers().isEmpty() || !savedVanish.isEmpty()) {
            dataManager.save(vanishManager.getVanishedPlayers());
        }
    }
}
