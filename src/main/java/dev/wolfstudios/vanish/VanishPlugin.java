package dev.wolfstudios.vanish;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VanishPlugin extends JavaPlugin implements Listener {

    private final Set<UUID> vanished = Collections.synchronizedSet(new HashSet<>());
    private boolean folia;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        folia = detectFolia();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("GerenciarVanish ativado! Backend: " + (folia ? "Folia" : "Bukkit/Spigot/Paper/Purpur"));
    }

    @Override
    public void onDisable() {
        for (UUID uuid : vanished) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) revealPlayer(p);
        }
        vanished.clear();
        getLogger().info("GerenciarVanish desativado!");
    }

    private boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("vanish")) return false;

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cUso: /vanish <jogador>");
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission("gerenciar.vanish.use")) {
                player.sendMessage(getMsg("sem-permissao"));
                return true;
            }
            toggleVanish(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("gerenciar.reload")) {
                sender.sendMessage(getMsg("sem-permissao"));
                return true;
            }
            reloadConfig();
            sender.sendMessage(getMsg("reload-sucesso"));
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            if (!sender.hasPermission("gerenciar.vanish.see")) {
                sender.sendMessage(getMsg("sem-permissao"));
                return true;
            }
            sender.sendMessage(getMsg("lista-vanished"));
            int i = 1;
            for (UUID uuid : vanished) {
                Player v = Bukkit.getPlayer(uuid);
                String name = v != null ? v.getName() : "???";
                sender.sendMessage("  §7" + i + ". §f" + name);
                i++;
            }
            if (vanished.isEmpty()) sender.sendMessage("  §7Nenhum jogador em vanish.");
            return true;
        }

        if (!sender.hasPermission("gerenciar.vanish.others")) {
            sender.sendMessage(getMsg("sem-permissao"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(getMsg("jogador-offline").replace("{player}", args[0]));
            return true;
        }
        toggleVanish(target);
        sender.sendMessage(getMsg("vanish-outro")
                .replace("{player}", target.getName())
                .replace("{status}", isVanished(target) ? getMsg("status-ativado") : getMsg("status-desativado")));
        return true;
    }

    public void toggleVanish(Player player) {
        if (isVanished(player)) {
            revealPlayer(player);
            player.sendMessage(getMsg("vanish-desativado"));
        } else {
            vanishPlayer(player);
            player.sendMessage(getMsg("vanish-ativado"));
        }
    }

    public void vanishPlayer(Player player) {
        vanished.add(player.getUniqueId());
        scheduleNextTick(player, () -> {
            player.setInvisible(true);
            player.setAllowFlight(true);
            player.setFlying(player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR);
        });

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission("gerenciar.vanish.see")) {
                hideFrom(online, player);
            }
        }
    }

    public void revealPlayer(Player player) {
        vanished.remove(player.getUniqueId());
        scheduleNextTick(player, () -> {
            player.setInvisible(false);
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setFlying(false);
                player.setAllowFlight(false);
            }
        });

        for (Player online : Bukkit.getOnlinePlayers()) {
            showTo(online, player);
        }
    }

    public boolean isVanished(Player player) {
        return vanished.contains(player.getUniqueId());
    }

    private void scheduleNextTick(Player player, Runnable task) {
        if (folia) {
            try {
                Method getScheduler = player.getClass().getMethod("getScheduler");
                Object entityScheduler = getScheduler.invoke(player);
                Method run = entityScheduler.getClass().getMethod("run", org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class, Runnable.class);
                run.invoke(entityScheduler, this, (java.util.function.Consumer<Object>) (o -> task.run()), null);
                return;
            } catch (Exception e) {
                getLogger().warning("Falha no scheduler Folia: " + e.getMessage());
            }
        }
        Bukkit.getScheduler().runTask(this, task);
    }

    private void hideFrom(Player viewer, Player target) {
        if (folia) {
            scheduleNextTick(viewer, () -> viewer.hidePlayer(VanishPlugin.this, target));
        } else {
            viewer.hidePlayer(VanishPlugin.this, target);
        }
    }

    private void showTo(Player viewer, Player target) {
        if (folia) {
            scheduleNextTick(viewer, () -> viewer.showPlayer(VanishPlugin.this, target));
        } else {
            viewer.showPlayer(VanishPlugin.this, target);
        }
    }

    private String getMsg(String key) {
        String msg = getConfig().getString("mensagens." + key, "&cMensagem: " + key);
        return msg.replace("&", "§");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();

        for (UUID uuid : vanished) {
            Player v = Bukkit.getPlayer(uuid);
            if (v != null && !player.hasPermission("gerenciar.vanish.see")) {
                hideFrom(player, v);
            }
        }

        if (player.hasPermission("gerenciar.vanish.see") && !vanished.isEmpty()) {
            player.sendMessage(getMsg("jogadores-vanished").replace("{count}", String.valueOf(vanished.size())));
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (getConfig().getBoolean("protecao.interagir", true) && isVanished(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (getConfig().getBoolean("protecao.quebrar", true) && isVanished(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (getConfig().getBoolean("protecao.colocar", true) && isVanished(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (getConfig().getBoolean("protecao.dropar", true) && isVanished(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (getConfig().getBoolean("protecao.pegar", true) && e.getEntity() instanceof Player p && isVanished(p)) e.setCancelled(true);
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent e) {
        if (getConfig().getBoolean("protecao.mobs", true) && e.getTarget() instanceof Player p && isVanished(p)) e.setCancelled(true);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (getConfig().getBoolean("protecao.hit", true) && e.getDamager() instanceof Player p && isVanished(p)) e.setCancelled(true);
    }
}
