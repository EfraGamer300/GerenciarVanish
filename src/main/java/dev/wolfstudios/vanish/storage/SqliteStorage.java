package dev.wolfstudios.vanish.storage;

import dev.wolfstudios.vanish.VanishPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SqliteStorage implements StorageBackend {

    private final VanishPlugin plugin;
    private Connection connection;

    public SqliteStorage(VanishPlugin plugin) {
        this.plugin = plugin;
    }

    private Connection getConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) return connection;
        File dbFile = new File(plugin.getDataFolder(), "vanish.db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        try (var stmt = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS vanished (uuid VARCHAR(36) PRIMARY KEY)")) {
            stmt.execute();
        }
        return connection;
    }

    @Override
    public Set<UUID> loadVanished() {
        Set<UUID> result = new HashSet<>();
        try {
            var conn = getConnection();
            var stmt = conn.prepareStatement("SELECT uuid FROM vanished");
            var rs = stmt.executeQuery();
            while (rs.next()) {
                try {
                    result.add(UUID.fromString(rs.getString("uuid")));
                } catch (IllegalArgumentException ignored) {}
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            plugin.getLogger().severe("[Vanish+] SQLite load error: " + e.getMessage());
        }
        return result;
    }

    @Override
    public void saveVanished(Set<UUID> vanished) {
        if (vanished.isEmpty()) return;
        try {
            var conn = getConnection();
            conn.createStatement().execute("DELETE FROM vanished");
            var stmt = conn.prepareStatement("INSERT INTO vanished (uuid) VALUES (?)");
            for (UUID uuid : vanished) {
                stmt.setString(1, uuid.toString());
                stmt.addBatch();
            }
            stmt.executeBatch();
            stmt.close();
        } catch (SQLException e) {
            plugin.getLogger().severe("[Vanish+] SQLite save error: " + e.getMessage());
        }
    }

    @Override
    public void removeVanished(UUID uuid) {
        try {
            var conn = getConnection();
            var stmt = conn.prepareStatement("DELETE FROM vanished WHERE uuid = ?");
            stmt.setString(1, uuid.toString());
            stmt.execute();
            stmt.close();
        } catch (SQLException e) {
            plugin.getLogger().severe("[Vanish+] SQLite remove error: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            plugin.getLogger().severe("[Vanish+] SQLite close error: " + e.getMessage());
        }
    }
}
