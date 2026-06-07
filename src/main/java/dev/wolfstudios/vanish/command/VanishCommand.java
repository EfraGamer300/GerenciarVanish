package dev.wolfstudios.vanish.command;

import dev.wolfstudios.vanish.LangManager;
import dev.wolfstudios.vanish.LogManager;
import dev.wolfstudios.vanish.VanishPlugin;
import dev.wolfstudios.vanish.manager.VanishManager;
import dev.wolfstudios.vanish.storage.StorageBackend;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VanishCommand implements CommandExecutor {

    private final VanishPlugin plugin;
    private final VanishManager vanishManager;
    private final LangManager lang;
    private final LogManager log;
    private final StorageBackend storage;

    public VanishCommand(VanishPlugin plugin, VanishManager vanishManager, LangManager lang, LogManager log, StorageBackend storage) {
        this.plugin = plugin;
        this.vanishManager = vanishManager;
        this.lang = lang;
        this.log = log;
        this.storage = storage;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!plugin.getConfig().getBoolean("settings.vanish-enabled", true)) {
            sender.sendMessage(lang.t("vanish-disabled-server"));
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(lang.t("command-usage"));
                return true;
            }
            if (!player.hasPermission("vanish.use")) {
                player.sendMessage(lang.t("no-permission"));
                return true;
            }
            vanishManager.toggle(player);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "reveal" -> handleReveal(sender, args);
            case "revealall" -> handleRevealAll(sender);
            case "list" -> handleList(sender);
            default -> handleToggleOther(sender, args);
        };
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("vanish.reload")) {
            sender.sendMessage(lang.t("no-permission"));
            return true;
        }
        plugin.reloadConfig();
        plugin.saveDefaultConfig();
        lang.reload();
        int cooldownMs = Math.max(plugin.getConfig().getInt("settings.cooldown-ms", 1000), 250);
        vanishManager.setCooldownMs(cooldownMs);

        if (!plugin.getConfig().getBoolean("settings.vanish-enabled", true)) {
            var toReveal = new ArrayList<>(vanishManager.getVanishedPlayers());
            for (UUID uuid : toReveal) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    vanishManager.revealPlayer(p);
                    plugin.savedVanish().remove(uuid);
                    p.sendMessage(lang.t("vanish-disabled-server"));
                }
            }
            log.log("SYSTEM", "Reload", "Vanish desativado — " + toReveal.size() + " jogador(es) revelado(s)");
        }

        sender.sendMessage(lang.t("reload-success"));
        log.log("SYSTEM", sender.getName(), "Config recarregada");
        return true;
    }

    private boolean handleReveal(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vanish.reveal")) {
            sender.sendMessage(lang.t("no-permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(lang.t("command-reveal-usage"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(lang.t("player-not-found").replace("{player}", args[1]));
            return true;
        }
        if (!vanishManager.isVanished(target)) {
            sender.sendMessage(lang.t("reveal-not-vanished").replace("{player}", target.getName()));
            return true;
        }
        vanishManager.revealPlayer(target);
        storage.removeVanished(target.getUniqueId());
        plugin.savedVanish().remove(target.getUniqueId());
        target.sendMessage(lang.t("vanish-revealed-by-admin").replace("{admin}", sender.getName()));
        sender.sendMessage(lang.t("reveal-success").replace("{player}", target.getName()));
        log.log("REVEAL", sender.getName(), "Revealed " + target.getName());
        return true;
    }

    private boolean handleRevealAll(CommandSender sender) {
        if (!sender.hasPermission("vanish.revealall")) {
            sender.sendMessage(lang.t("no-permission"));
            return true;
        }
        int count = 0;
        var toReveal = new ArrayList<>(vanishManager.getVanishedPlayers());
        for (UUID uuid : toReveal) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                vanishManager.revealPlayer(p);
                plugin.savedVanish().remove(uuid);
                p.sendMessage(lang.t("vanish-revealed-by-admin").replace("{admin}", sender.getName()));
                count++;
            }
        }
        if (sender.hasPermission("vanish.see")) {
            sender.sendMessage(lang.t("revealall-success").replace("{count}", String.valueOf(count)));
        }
        log.log("REVEALALL", sender.getName(), "Revealed " + count + " jogadores");
        return true;
    }

    private boolean handleList(CommandSender sender) {
        if (!sender.hasPermission("vanish.see")) {
            sender.sendMessage(lang.t("no-permission"));
            return true;
        }
        sender.sendMessage(lang.t("vanished-list"));
        int i = 1;
        for (UUID uuid : vanishManager.getVanishedPlayers()) {
            Player v = Bukkit.getPlayer(uuid);
            String name = v != null ? v.getName() : "???";
            sender.sendMessage(lang.raw("vanished-list-format")
                    .replace("{index}", String.valueOf(i))
                    .replace("{player}", name));
            i++;
        }
        if (vanishManager.getVanishedPlayers().isEmpty()) {
            sender.sendMessage(lang.t("no-vanished"));
        }
        return true;
    }

    private boolean handleToggleOther(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vanish.others")) {
            sender.sendMessage(lang.t("no-permission"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(lang.t("player-not-found").replace("{player}", args[0]));
            return true;
        }
        boolean wasVanished = vanishManager.isVanished(target);
        vanishManager.toggle(target);
        boolean nowVanished = vanishManager.isVanished(target);
        sender.sendMessage(lang.t("vanish-other")
                .replace("{player}", target.getName())
                .replace("{status}", nowVanished ? lang.raw("status-on-vanish") : lang.raw("status-off-vanish")));
        return true;
    }
}
