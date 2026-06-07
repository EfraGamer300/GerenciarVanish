package dev.wolfstudios.vanish.manager;

import dev.wolfstudios.vanish.LangManager;
import dev.wolfstudios.vanish.LogManager;
import dev.wolfstudios.vanish.VanishPlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class VanishManager {

    private final VanishPlugin plugin;
    private final Set<UUID> vanished = Collections.synchronizedSet(new HashSet<>());
    private final Map<UUID, Long> vanishCooldowns = Collections.synchronizedMap(new HashMap<>());
    private final Map<UUID, Boolean> prevAllowFlight = Collections.synchronizedMap(new HashMap<>());
    private final Map<UUID, Boolean> prevFlying = Collections.synchronizedMap(new HashMap<>());
    private final Map<UUID, String> prevTabNames = Collections.synchronizedMap(new HashMap<>());
    private final LangManager lang;
    private final LogManager log;
    private final boolean folia;
    private int cooldownMs = 1000;

    public VanishManager(VanishPlugin plugin, LangManager lang, LogManager log) {
        this.plugin = plugin;
        this.lang = lang;
        this.log = log;
        this.folia = detectFolia();
    }

    public void setCooldownMs(int cooldownMs) {
        this.cooldownMs = cooldownMs;
    }

    public boolean isFolia() {
        return folia;
    }

    public Set<UUID> getVanishedPlayers() {
        return Collections.unmodifiableSet(vanished);
    }

    public boolean isVanished(Player player) {
        return vanished.contains(player.getUniqueId());
    }

    public void removeVanished(UUID uuid) {
        vanished.remove(uuid);
        prevAllowFlight.remove(uuid);
        prevFlying.remove(uuid);
        prevTabNames.remove(uuid);
    }

    public boolean isOnCooldown(UUID uuid) {
        synchronized (vanishCooldowns) {
            Long last = vanishCooldowns.get(uuid);
            if (last != null && System.currentTimeMillis() - last < cooldownMs) return true;
            vanishCooldowns.put(uuid, System.currentTimeMillis());
            return false;
        }
    }

    public void toggle(Player player) {
        if (isOnCooldown(player.getUniqueId())) {
            player.sendMessage(lang.t("cooldown"));
            return;
        }
        if (isVanished(player)) {
            revealPlayer(player);
            player.sendMessage(lang.t("vanish-disabled-msg"));
        } else {
            vanishPlayer(player);
            player.sendMessage(lang.t("vanish-enabled-msg"));
        }
    }

    public void vanishPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        vanished.add(uuid);
        prevAllowFlight.put(uuid, player.getAllowFlight());
        prevFlying.put(uuid, player.isFlying());

        scheduleNextTick(player, () -> {
            player.setInvisible(true);
            player.setAllowFlight(true);
            player.setFlying(player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR);
        });

        if (!plugin.getConfig().getBoolean("settings.show-vanished-in-tab", false)) {
            prevTabNames.put(uuid, player.getPlayerListName());
            player.setPlayerListName("");
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission("vanish.see")) {
                hideFrom(online, player);
            } else if (!online.getUniqueId().equals(uuid)) {
                online.sendMessage(lang.t("vanish-staff-notify")
                        .replace("{player}", player.getName())
                        .replace("{status}", lang.raw("status-on")));
            }
        }

        log.log("VANISH", player.getName(), "Vanished (entrou em vanish)");
    }

    public void revealPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        vanished.remove(uuid);
        Boolean prevFlight = prevAllowFlight.remove(uuid);
        Boolean prevFly = prevFlying.remove(uuid);

        String prevTab = prevTabNames.remove(uuid);
        if (prevTab != null) {
            player.setPlayerListName(prevTab.isEmpty() ? player.getName() : prevTab);
        }

        scheduleNextTick(player, () -> {
            player.setInvisible(false);
            if (prevFlight != null) {
                player.setAllowFlight(prevFlight);
                player.setFlying(prevFly != null && prevFly);
            } else {
                GameMode gm = player.getGameMode();
                if (gm != GameMode.CREATIVE && gm != GameMode.SPECTATOR) {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                }
            }
        });

        for (Player online : Bukkit.getOnlinePlayers()) {
            showTo(online, player);
        }

        log.log("VANISH", player.getName(), "Revealed (saiu do vanish)");
    }

    public void revealAll() {
        for (UUID uuid : new HashSet<>(vanished)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) revealPlayer(p);
        }
    }

    public void hideVanishedFrom(Player viewer) {
        boolean canSee = viewer.hasPermission("vanish.see");
        for (UUID uuid : vanished) {
            Player v = Bukkit.getPlayer(uuid);
            if (v != null && !canSee) {
                hideFrom(viewer, v);
            }
        }
    }

    private void hideFrom(Player viewer, Player target) {
        if (folia) {
            scheduleNextTick(viewer, () -> viewer.hidePlayer(plugin, target));
        } else {
            viewer.hidePlayer(plugin, target);
        }
    }

    private void showTo(Player viewer, Player target) {
        if (folia) {
            scheduleNextTick(viewer, () -> viewer.showPlayer(plugin, target));
        } else {
            viewer.showPlayer(plugin, target);
        }
    }

    private void scheduleNextTick(Player player, Runnable task) {
        if (folia) {
            try {
                var getScheduler = player.getClass().getMethod("getScheduler");
                var entityScheduler = getScheduler.invoke(player);
                var run = entityScheduler.getClass().getMethod("run", org.bukkit.plugin.Plugin.class, Runnable.class, Runnable.class);
                run.invoke(entityScheduler, plugin, task, null);
                return;
            } catch (Exception e) {
                log.log("ERROR", "FoliaScheduler", "Falha: " + e.getMessage());
                task.run();
                return;
            }
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    private boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
