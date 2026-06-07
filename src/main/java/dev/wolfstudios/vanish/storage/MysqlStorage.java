package dev.wolfstudios.vanish.storage;

import dev.wolfstudios.vanish.VanishPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MysqlStorage implements StorageBackend {

    private final VanishPlugin plugin;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final String tablePrefix;
    private Connection connection;

    public MysqlStorage(VanishPlugin plugin, String host, int port, String database,
                        String username, String password, String tablePrefix) {
        this.plugin = plugin;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.tablePrefix = tablePrefix;
    }

    private Connection getConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) return connection;
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&characterEncoding=UTF-8";
        connection = DriverManager.getConnection(url, username, password);
        String table = tablePrefix + "vanished";
        try (PreparedStatement stmt = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS " + table + " (uuid VARCHAR(36) PRIMARY KEY)")) {
            stmt.execute();
        }
        return connection;
    }

    private String table() {
        return tablePrefix + "vanished";
    }

    @Override
    public Set<UUID> loadVanished() {
        Set<UUID> result = new HashSet<>();
        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT uuid FROM " + table());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                try {
                    result.add(UUID.fromString(rs.getString("uuid")));
                } catch (IllegalArgumentException ignored) {
                }
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            plugin.getLogger().severe("[Vanish+] MySQL load error: " + e.getMessage());
        }
        return result;
    }

    @Override
    public void saveVanished(Set<UUID> vanished) {
        try {
            Connection conn = getConnection();
            conn.createStatement().execute("DELETE FROM " + table());
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO " + table() + " (uuid) VALUES (?)");
            for (UUID uuid : vanished) {
                stmt.setString(1, uuid.toString());
                stmt.addBatch();
            }
            stmt.executeBatch();
            stmt.close();
        } catch (SQLException e) {
            plugin.getLogger().severe("[Vanish+] MySQL save error: " + e.getMessage());
        }
    }

    @Override
    public void removeVanished(UUID uuid) {
        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement("DELETE FROM " + table() + " WHERE uuid = ?");
            stmt.setString(1, uuid.toString());
            stmt.execute();
            stmt.close();
        } catch (SQLException e) {
            plugin.getLogger().severe("[Vanish+] MySQL remove error: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            plugin.getLogger().severe("[Vanish+] MySQL close error: " + e.getMessage());
        }
    }
}
