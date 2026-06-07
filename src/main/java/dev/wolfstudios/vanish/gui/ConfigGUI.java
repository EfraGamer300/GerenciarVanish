package dev.wolfstudios.vanish.gui;

import dev.wolfstudios.vanish.LangManager;
import dev.wolfstudios.vanish.VanishPlugin;
import dev.wolfstudios.vanish.manager.SilentChestManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
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

    private static final int[] TOGGLE_SLOTS = {
            10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 23
    };

    public ConfigGUI(VanishPlugin plugin, LangManager lang, SilentChestManager silentChestManager) {
        this.plugin = plugin;
        this.lang = lang;
        this.silentChestManager = silentChestManager;
    }

    public String getGuiTitle(Player player) {
        return playerTitles.get(player.getUniqueId());
    }

    public void open(Player player) {
        var cfg = plugin.getConfig();
        String title = ChatColor.translateAlternateColorCodes('&', lang.raw("config-gui-title"));
        playerTitles.put(player.getUniqueId(), title);
        Inventory inv = Bukkit.createInventory(null, 54, title);

        inv.setItem(10, toggleItem(cfg.getBoolean("settings.vanish-enabled", true), lang.raw("gui.vanish-enabled"), lang.raw("gui.vanish-enabled-desc")));
        inv.setItem(11, toggleItem(cfg.getBoolean("settings.save-vanish-on-quit", true), lang.raw("gui.save-vanish"), lang.raw("gui.save-vanish-desc")));
        inv.setItem(12, toggleItem(cfg.getBoolean("protection.silent-chest", true), lang.raw("gui.silent-chest"), lang.raw("gui.silent-chest-desc")));
        inv.setItem(13, toggleItem(cfg.getBoolean("protection.block-interact", true), lang.raw("gui.block-interact"), ""));
        inv.setItem(14, toggleItem(cfg.getBoolean("protection.block-break", true), lang.raw("gui.block-break"), ""));
        inv.setItem(15, toggleItem(cfg.getBoolean("protection.block-place", true), lang.raw("gui.block-place"), ""));
        inv.setItem(16, toggleItem(cfg.getBoolean("protection.item-drop", true), lang.raw("gui.item-drop"), ""));
        inv.setItem(19, toggleItem(cfg.getBoolean("protection.item-pickup", true), lang.raw("gui.item-pickup"), ""));
        inv.setItem(20, toggleItem(cfg.getBoolean("protection.mob-target", true), lang.raw("gui.mob-target"), ""));
        inv.setItem(21, toggleItem(cfg.getBoolean("protection.pvp", true), lang.raw("gui.pvp"), ""));

        inv.setItem(23, toggleItem(
                silentChestManager.hasSilentChest(player.getUniqueId()),
                lang.raw("gui.my-silent-chest"),
                lang.raw("gui.silent-chest-desc")));

        ItemStack gray = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, gray);
        }

        player.openInventory(inv);
    }

    public void handleClick(Player player, org.bukkit.event.inventory.InventoryClickEvent e) {
        String title = playerTitles.get(player.getUniqueId());
        if (title == null || !e.getView().getTitle().equals(title)) return;
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        switch (slot) {
            case 10 -> plugin.getConfig().set("settings.vanish-enabled", !plugin.getConfig().getBoolean("settings.vanish-enabled", true));
            case 11 -> plugin.getConfig().set("settings.save-vanish-on-quit", !plugin.getConfig().getBoolean("settings.save-vanish-on-quit", true));
            case 12 -> plugin.getConfig().set("protection.silent-chest", !plugin.getConfig().getBoolean("protection.silent-chest", true));
            case 13 -> plugin.getConfig().set("protection.block-interact", !plugin.getConfig().getBoolean("protection.block-interact", true));
            case 14 -> plugin.getConfig().set("protection.block-break", !plugin.getConfig().getBoolean("protection.block-break", true));
            case 15 -> plugin.getConfig().set("protection.block-place", !plugin.getConfig().getBoolean("protection.block-place", true));
            case 16 -> plugin.getConfig().set("protection.item-drop", !plugin.getConfig().getBoolean("protection.item-drop", true));
            case 19 -> plugin.getConfig().set("protection.item-pickup", !plugin.getConfig().getBoolean("protection.item-pickup", true));
            case 20 -> plugin.getConfig().set("protection.mob-target", !plugin.getConfig().getBoolean("protection.mob-target", true));
            case 21 -> plugin.getConfig().set("protection.pvp", !plugin.getConfig().getBoolean("protection.pvp", true));
            case 23 -> {
                silentChestManager.toggle(player.getUniqueId());
                player.sendMessage(silentChestManager.hasSilentChest(player.getUniqueId())
                        ? lang.t("silent-chest-enabled")
                        : lang.t("silent-chest-disabled"));
            }
            default -> { return; }
        }

        plugin.saveConfig();
        player.sendMessage(lang.t("config-saved"));
        open(player);
    }

    private ItemStack toggleItem(boolean enabled, String name, String desc) {
        ItemStack item = new ItemStack(enabled ? Material.LIME_DYE : Material.RED_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(enabled ? ChatColor.GREEN + "✔ " + name : ChatColor.RED + "✘ " + name);
        List<String> lore = new ArrayList<>();
        if (desc != null && !desc.isEmpty()) lore.add(ChatColor.GRAY + desc);
        lore.add("");
        lore.add(enabled
                ? ChatColor.GREEN + "" + ChatColor.BOLD + lang.raw("gui.enabled")
                : ChatColor.RED + "" + ChatColor.BOLD + lang.raw("gui.disabled"));
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
