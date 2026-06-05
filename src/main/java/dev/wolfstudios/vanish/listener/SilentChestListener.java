package dev.wolfstudios.vanish.listener;

import dev.wolfstudios.vanish.manager.SilentChestManager;
import dev.wolfstudios.vanish.manager.VanishManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SilentChestListener implements Listener {

    private final VanishManager vanishManager;
    private final SilentChestManager silentChestManager;
    private final Set<UUID> opening = new HashSet<>();

    public SilentChestListener(VanishManager vanishManager, SilentChestManager silentChestManager) {
        this.vanishManager = vanishManager;
        this.silentChestManager = silentChestManager;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        Player player = (Player) e.getPlayer();
        if (!vanishManager.isVanished(player)) return;
        if (!silentChestManager.hasSilentChest(player.getUniqueId())) return;

        InventoryType type = e.getInventory().getType();
        if (!isContainer(type)) return;

        if (opening.contains(player.getUniqueId())) {
            opening.remove(player.getUniqueId());
            return;
        }

        e.setCancelled(true);
        opening.add(player.getUniqueId());
        player.openInventory(e.getInventory());
    }

    private boolean isContainer(InventoryType type) {
        return type == InventoryType.CHEST
                || type == InventoryType.BARREL
                || type == InventoryType.ENDER_CHEST
                || type == InventoryType.SHULKER_BOX
                || type == InventoryType.HOPPER
                || type == InventoryType.DISPENSER
                || type == InventoryType.DROPPER
                || type == InventoryType.FURNACE
                || type == InventoryType.BLAST_FURNACE
                || type == InventoryType.SMOKER
                || type == InventoryType.BREWING
                || type == InventoryType.ANVIL
                || type == InventoryType.ENCHANTING
                || type == InventoryType.GRINDSTONE
                || type == InventoryType.LOOM
                || type == InventoryType.STONECUTTER
                || type == InventoryType.CARTOGRAPHY
                || type == InventoryType.SMITHING;
    }
}
