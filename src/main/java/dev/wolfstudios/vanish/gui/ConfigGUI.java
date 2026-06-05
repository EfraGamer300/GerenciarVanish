package dev.wolfstudios.vanish.gui;

import dev.wolfstudios.vanish.LangManager;
import dev.wolfstudios.vanish.VanishPlugin;
import dev.wolfstudios.vanish.manager.SilentChestManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ConfigGUI {

    private final VanishPlugin plugin;
    private final LangManager lang;
    private final SilentChestManager silentChestManager;
    private final Map<UUID, String> playerTitles = new HashMap<>();

    public ConfigGUI(VanishPlugin plugin, LangManager lang, SilentChestManager silentChestManager) {
        this.plugin = plugin;
        this.lang = lang;
        this.silentChestManager = silentChestManager;
    }

    public String getGuiTitle(Player player) {
        return playerTitles.get(player.getUniqueId());
    }

    public void open(Player player) {
        String title = ChatColor.translateAlternateColorCodes('&', lang.raw("config-gui-title"));
        playerTitles.put(player.getUniqueId(), title);
        Inventory inv = Bukkit.createInventory(null, 54, title);

        boolean vanishEnabled = plugin.getConfig().getBoolean("settings.vanish-enabled", true);
        boolean saveOnQuit = plugin.getConfig().getBoolean("settings.save-vanish-on-quit", true);
        boolean silentChestEnabled = plugin.getConfig().getBoolean("protection.silent-chest", true);
        boolean blockInteract = plugin.getConfig().getBoolean("protection.block-interact", true);
        boolean blockBreak = plugin.getConfig().getBoolean("protection.block-break", true);
        boolean blockPlace = plugin.getConfig().getBoolean("protection.block-place", true);
        boolean itemDrop = plugin.getConfig().getBoolean("protection.item-drop", true);
        boolean itemPickup = plugin.getConfig().getBoolean("protection.item-pickup", true);
        boolean mobTarget = plugin.getConfig().getBoolean("protection.mob-target", true);
        boolean pvp = plugin.getConfig().getBoolean("protection.pvp", true);

        inv.setItem(10, toggleItem(vanishEnabled, lang.raw("gui.vanish-enabled"), lang.raw("gui.vanish-enabled-desc")));
        inv.setItem(11, toggleItem(saveOnQuit, lang.raw("gui.save-vanish"), lang.raw("gui.save-vanish-desc")));
        inv.setItem(12, toggleItem(silentChestEnabled, lang.raw("gui.silent-chest"), lang.raw("gui.silent-chest-desc")));
        inv.setItem(13, toggleItem(blockInteract, lang.raw("gui.block-interact"), ""));
        inv.setItem(14, toggleItem(blockBreak, lang.raw("gui.block-break"), ""));
        inv.setItem(15, toggleItem(blockPlace, lang.raw("gui.block-place"), ""));
        inv.setItem(16, toggleItem(itemDrop, lang.raw("gui.item-drop"), ""));
        inv.setItem(19, toggleItem(itemPickup, lang.raw("gui.item-pickup"), ""));
        inv.setItem(20, toggleItem(mobTarget, lang.raw("gui.mob-target"), ""));
        inv.setItem(21, toggleItem(pvp, lang.raw("gui.pvp"), ""));

        boolean playerSilent = silentChestManager.hasSilentChest(player.getUniqueId());
        inv.setItem(23, toggleItem(playerSilent, lang.raw("gui.my-silent-chest"), lang.raw("gui.silent-chest-desc")));

        ItemStack gray = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, gray);
        }

        player.openInventory(inv);
    }

    public void handleClick(Player player, InventoryClickEvent e) {
        String title = playerTitles.get(player.getUniqueId());
        if (title == null || !e.getView().getTitle().equals(title)) return;
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        switch (slot) {
            case 10:
                toggleConfig("settings.vanish-enabled");
                break;
            case 11:
                toggleConfig("settings.save-vanish-on-quit");
                break;
            case 12:
                toggleConfig("protection.silent-chest");
                break;
            case 13:
                toggleConfig("protection.block-interact");
                break;
            case 14:
                toggleConfig("protection.block-break");
                break;
            case 15:
                toggleConfig("protection.block-place");
                break;
            case 16:
                toggleConfig("protection.item-drop");
                break;
            case 19:
                toggleConfig("protection.item-pickup");
                break;
            case 20:
                toggleConfig("protection.mob-target");
                break;
            case 21:
                toggleConfig("protection.pvp");
                break;
            case 23:
                silentChestManager.toggle(player.getUniqueId());
                break;
            default:
                return;
        }

        plugin.saveConfig();
        player.sendMessage(lang.t("config-saved"));
        open(player);
    }

    private void toggleConfig(String path) {
        boolean cur = plugin.getConfig().getBoolean(path, true);
        plugin.getConfig().set(path, !cur);
    }

    private ItemStack toggleItem(boolean enabled, String name, String desc) {
        ItemStack item = new ItemStack(enabled ? Material.LIME_DYE : Material.RED_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(enabled ? ChatColor.GREEN + "✔ " + name : ChatColor.RED + "✘ " + name);
        List<String> lore = new ArrayList<>();
        if (desc != null && !desc.isEmpty()) lore.add(ChatColor.GRAY + desc);
        lore.add("");
        lore.add(enabled ? ChatColor.GREEN + "" + ChatColor.BOLD + lang.raw("gui.enabled") : ChatColor.RED + "" + ChatColor.BOLD + lang.raw("gui.disabled"));
        lore.add("");
        lore.add(ChatColor.YELLOW + lang.raw("gui.toggle-click"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
}
