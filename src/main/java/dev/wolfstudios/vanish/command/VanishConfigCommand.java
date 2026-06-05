package dev.wolfstudios.vanish.command;

import dev.wolfstudios.vanish.LangManager;
import dev.wolfstudios.vanish.VanishPlugin;
import dev.wolfstudios.vanish.gui.ConfigGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VanishConfigCommand implements CommandExecutor {

    private final VanishPlugin plugin;
    private final ConfigGUI configGUI;
    private final LangManager lang;

    public VanishConfigCommand(VanishPlugin plugin, ConfigGUI configGUI, LangManager lang) {
        this.plugin = plugin;
        this.configGUI = configGUI;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores!");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("vanish.config")) {
            player.sendMessage(lang.t("no-permission"));
            return true;
        }
        configGUI.open(player);
        return true;
    }
}
