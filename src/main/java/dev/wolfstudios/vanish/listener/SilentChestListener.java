package dev.wolfstudios.vanish.listener;

import dev.wolfstudios.vanish.manager.SilentChestManager;
import dev.wolfstudios.vanish.manager.VanishManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SilentChestListener implements Listener {

    private final VanishManager vanishManager;
    private final SilentChestManager silentChestManager;
    private final Set<UUID> opening = ConcurrentHashMap.newKeySet();

    public SilentChestListener(VanishManager vanishManager, SilentChestManager silentChestManager) {
        this.vanishManager = vanishManager;
        this.silentChestManager = silentChestManager;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        if (!vanishManager.isVanished(player)) return;
        if (!silentChestManager.hasSilentChest(player.getUniqueId())) return;
        if (!isContainer(e.getInventory().getType())) return;

        UUID uuid = player.getUniqueId();
        if (opening.contains(uuid)) {
            opening.remove(uuid);
            return;
        }

        e.setCancelled(true);
        opening.add(uuid);
        player.openInventory(e.getInventory());
    }

    private boolean isContainer(InventoryType type) {
        return switch (type) {
            case CHEST, BARREL, ENDER_CHEST, SHULKER_BOX, HOPPER,
                 DISPENSER, DROPPER, FURNACE, BLAST_FURNACE, SMOKER,
                 BREWING, ANVIL, ENCHANTING, GRINDSTONE, LOOM,
                 STONECUTTER, CARTOGRAPHY, SMITHING -> true;
            default -> false;
        };
    }
}
