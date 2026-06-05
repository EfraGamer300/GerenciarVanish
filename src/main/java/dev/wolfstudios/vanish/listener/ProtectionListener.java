package dev.wolfstudios.vanish.listener;

import dev.wolfstudios.vanish.VanishPlugin;
import dev.wolfstudios.vanish.manager.VanishManager;
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
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class ProtectionListener implements Listener {

    private final VanishPlugin plugin;
    private final VanishManager vanishManager;

    public ProtectionListener(VanishPlugin plugin, VanishManager vanishManager) {
        this.plugin = plugin;
        this.vanishManager = vanishManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getPlayer().hasPermission("vanish.bypass.protection")) return;
        if (plugin.getConfig().getBoolean("protection.block-interact", true) && vanishManager.isVanished(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (e.getPlayer().hasPermission("vanish.bypass.protection")) return;
        if (plugin.getConfig().getBoolean("protection.block-break", true) && vanishManager.isVanished(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (e.getPlayer().hasPermission("vanish.bypass.protection")) return;
        if (plugin.getConfig().getBoolean("protection.block-place", true) && vanishManager.isVanished(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (e.getPlayer().hasPermission("vanish.bypass.protection")) return;
        if (plugin.getConfig().getBoolean("protection.item-drop", true) && vanishManager.isVanished(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        if (p.hasPermission("vanish.bypass.protection")) return;
        if (plugin.getConfig().getBoolean("protection.item-pickup", true) && vanishManager.isVanished(p)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent e) {
        if (!(e.getTarget() instanceof Player)) return;
        Player p = (Player) e.getTarget();
        if (p.hasPermission("vanish.bypass.protection")) return;
        if (plugin.getConfig().getBoolean("protection.mob-target", true) && vanishManager.isVanished(p)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        Player p = (Player) e.getDamager();
        if (p.hasPermission("vanish.bypass.protection")) return;
        if (plugin.getConfig().getBoolean("protection.pvp", true) && vanishManager.isVanished(p)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (e.getPlayer().hasPermission("vanish.bypass.protection")) return;
        if (plugin.getConfig().getBoolean("protection.block-interact", true) && vanishManager.isVanished(e.getPlayer())) {
            e.setCancelled(true);
        }
    }
}
