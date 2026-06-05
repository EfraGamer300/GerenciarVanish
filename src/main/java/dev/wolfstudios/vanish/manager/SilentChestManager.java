package dev.wolfstudios.vanish.manager;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class SilentChestManager {

    private final Set<UUID> silentChestPlayers;

    public SilentChestManager(Set<UUID> silentChestPlayers) {
        this.silentChestPlayers = silentChestPlayers;
    }

    public boolean hasSilentChest(UUID uuid) {
        return silentChestPlayers.contains(uuid);
    }

    public void toggle(UUID uuid) {
        if (silentChestPlayers.contains(uuid)) {
            silentChestPlayers.remove(uuid);
        } else {
            silentChestPlayers.add(uuid);
        }
    }

    public Set<UUID> getPlayers() {
        return Collections.unmodifiableSet(silentChestPlayers);
    }
}
