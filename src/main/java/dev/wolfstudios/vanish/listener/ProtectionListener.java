package dev.wolfstudios.vanish.listener;

import dev.wolfstudios.vanish.VanishPlugin;
import dev.wolfstudios.vanish.manager.VanishManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
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
        if (shouldCancel(e.getPlayer(), "protection.block-interact")) e.setCancelled(true);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (shouldCancel(e.getPlayer(), "protection.block-break")) e.setCancelled(true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (shouldCancel(e.getPlayer(), "protection.block-place")) e.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (shouldCancel(e.getPlayer(), "protection.item-drop")) e.setCancelled(true);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p && shouldCancel(p, "protection.item-pickup"))
            e.setCancelled(true);
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent e) {
        if (e.getTarget() instanceof Player p && shouldCancel(p, "protection.mob-target"))
            e.setCancelled(true);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p && shouldCancel(p, "protection.pvp"))
            e.setCancelled(true);
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (shouldCancel(e.getPlayer(), "protection.block-interact")) e.setCancelled(true);
    }

    private boolean shouldCancel(Player player, String configPath) {
        return !player.hasPermission("vanish.bypass.protection")
                && plugin.getConfig().getBoolean(configPath, true)
                && vanishManager.isVanished(player);
    }
}
