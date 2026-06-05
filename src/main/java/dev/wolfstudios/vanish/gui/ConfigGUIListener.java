package dev.wolfstudios.vanish.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ConfigGUIListener implements Listener {

    private final ConfigGUI configGUI;

    public ConfigGUIListener(ConfigGUI configGUI) {
        this.configGUI = configGUI;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();
        String title = configGUI.getGuiTitle(player);
        if (title == null) return;
        if (!e.getView().getTitle().equals(title)) return;
        configGUI.handleClick(player, e);
    }
}
