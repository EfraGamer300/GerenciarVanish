package dev.wolfstudios.vanish.storage;

import java.util.Set;
import java.util.UUID;

public interface StorageBackend {

    Set<UUID> loadVanished();

    void saveVanished(Set<UUID> vanished);

    void removeVanished(UUID uuid);

    void close();
}
