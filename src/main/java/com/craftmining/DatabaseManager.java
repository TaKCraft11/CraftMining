package com.craftmining;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final CraftMining plugin;
    private Connection connection;

    private static final String CREATE_PLAYER_TABLE = """
            CREATE TABLE IF NOT EXISTS player_data (
                uuid          TEXT    PRIMARY KEY,
                username      TEXT    NOT NULL,
                xp            INTEGER NOT NULL DEFAULT 0,
                level         INTEGER NOT NULL DEFAULT 1,
                tokens        INTEGER NOT NULL DEFAULT 0,
                blocks_mined  INTEGER NOT NULL DEFAULT 0,
                items_smelted INTEGER NOT NULL DEFAULT 0,
                last_mine_time INTEGER NOT NULL DEFAULT 0,
                last_seen     INTEGER NOT NULL DEFAULT 0,
                crystals      INTEGER NOT NULL DEFAULT 0
            );
            """;

    private static final String CREATE_CRYSTAL_ITEMS = """
            CREATE TABLE IF NOT EXISTS crystal_items_used (
                item_uuid   TEXT PRIMARY KEY,
                player_uuid TEXT NOT NULL,
                amount      INTEGER NOT NULL,
                used_at     INTEGER NOT NULL DEFAULT (strftime('%s','now'))
            );
            """;

    private static final String CREATE_REPLACED_BLOCKS = """
            CREATE TABLE IF NOT EXISTS replaced_blocks (
                world TEXT NOT NULL,
                x     INTEGER NOT NULL,
                y     INTEGER NOT NULL,
                z     INTEGER NOT NULL,
                placed_at INTEGER NOT NULL DEFAULT (strftime('%s','now')),
                PRIMARY KEY (world, x, y, z)
            );
            """;

    private static final String ADD_CRYSTALS_COLUMN = """
            ALTER TABLE player_data ADD COLUMN crystals INTEGER NOT NULL DEFAULT 0;
            """;

    private static final String UPSERT_PLAYER = """
            INSERT INTO player_data
                (uuid, username, xp, level, tokens, blocks_mined, items_smelted, last_mine_time, last_seen, crystals)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                username       = excluded.username,
                xp             = excluded.xp,
                level          = excluded.level,
                tokens         = excluded.tokens,
                blocks_mined   = excluded.blocks_mined,
                items_smelted  = excluded.items_smelted,
                last_mine_time = excluded.last_mine_time,
                last_seen      = excluded.last_seen,
                crystals       = excluded.crystals;
            """;

    private static final String SELECT_PLAYER =
            "SELECT * FROM player_data WHERE uuid = ?;";

    private static final String SELECT_TOP =
            "SELECT * FROM player_data ORDER BY %s DESC LIMIT ?;";

    public DatabaseManager(CraftMining plugin) {
        this.plugin = plugin;
    }

    public boolean initialize() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) dataFolder.mkdirs();

            File dbFile = new File(dataFolder, "craftmining.db");
            String url  = "jdbc:sqlite:" + dbFile.getAbsolutePath();

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(url);

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL;");
                stmt.execute("PRAGMA synchronous=NORMAL;");
                stmt.execute("PRAGMA foreign_keys=ON;");
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute(CREATE_PLAYER_TABLE);
                stmt.execute(CREATE_CRYSTAL_ITEMS);
                stmt.execute(CREATE_REPLACED_BLOCKS);
            }

            // Migration colonne crystals
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(ADD_CRYSTALS_COLUMN);
            } catch (SQLException ignored) {}

            plugin.getLogger().info("[CraftMining] Base de données SQLite initialisée : " + dbFile.getName());
            return true;

        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("[CraftMining] Driver SQLite introuvable ! " + e.getMessage());
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[CraftMining] Erreur SQL à l'initialisation", e);
        }
        return false;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("[CraftMining] Connexion SQLite fermée.");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[CraftMining] Erreur fermeture SQLite", e);
        }
    }

    public void savePlayer(PlayerData data, String username) {
        if (!isConnected()) return;
        try (PreparedStatement ps = connection.prepareStatement(UPSERT_PLAYER)) {
            ps.setString(1,  data.getUuid().toString());
            ps.setString(2,  username);
            ps.setInt(3,     data.getXp());
            ps.setInt(4,     data.getLevel());
            ps.setInt(5,     data.getTokens());
            ps.setInt(6,     data.getBlocksMined());
            ps.setInt(7,     data.getItemsSmelted());
            ps.setLong(8,    data.getLastMineTime());
            ps.setLong(9,    System.currentTimeMillis());
            ps.setLong(10,   data.getCrystals());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING,
                    "[CraftMining] Erreur sauvegarde joueur " + username, e);
        }
    }

    public PlayerData loadPlayer(UUID uuid) {
        if (!isConnected()) return null;
        try (PreparedStatement ps = connection.prepareStatement(SELECT_PLAYER)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                PlayerData data = new PlayerData(uuid);
                data.setXp(rs.getInt("xp"));
                data.setLevel(rs.getInt("level"));
                data.setTokens(rs.getInt("tokens"));
                data.setBlocksMined(rs.getInt("blocks_mined"));
                data.setItemsSmelted(rs.getInt("items_smelted"));
                data.setCrystals(rs.getLong("crystals"));
                return data;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING,
                    "[CraftMining] Erreur chargement joueur " + uuid, e);
        }
        return null;
    }

    public void saveAll(List<PlayerData> dataList,
                        java.util.function.Function<UUID, String> nameResolver) {
        if (!isConnected() || dataList.isEmpty()) return;
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement(UPSERT_PLAYER)) {
                for (PlayerData data : dataList) {
                    String name = nameResolver.apply(data.getUuid());
                    if (name == null) name = data.getUuid().toString();
                    ps.setString(1,  data.getUuid().toString());
                    ps.setString(2,  name);
                    ps.setInt(3,     data.getXp());
                    ps.setInt(4,     data.getLevel());
                    ps.setInt(5,     data.getTokens());
                    ps.setInt(6,     data.getBlocksMined());
                    ps.setInt(7,     data.getItemsSmelted());
                    ps.setLong(8,    data.getLastMineTime());
                    ps.setLong(9,    System.currentTimeMillis());
                    ps.setLong(10,   data.getCrystals());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            connection.commit();
            connection.setAutoCommit(true);
            plugin.getLogger().info("[CraftMining] Auto-save : " + dataList.size() + " joueur(s).");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[CraftMining] Erreur saveAll", e);
            try { connection.rollback(); connection.setAutoCommit(true); }
            catch (SQLException ignored) {}
        }
    }

    // ─────────────────────────────────────────────
    //  GEMMES ABYSSALES — Anti-dupe
    // ─────────────────────────────────────────────

    public boolean isCrystalItemUsed(String itemUuid) {
        if (!isConnected()) return true;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM crystal_items_used WHERE item_uuid = ?")) {
            ps.setString(1, itemUuid);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[CM] isCrystalItemUsed error", e);
            return true;
        }
    }

    public boolean consumeCrystalItem(String itemUuid, UUID playerUuid, long amount) {
        if (!isConnected()) return false;
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR IGNORE INTO crystal_items_used (item_uuid, player_uuid, amount) VALUES (?, ?, ?)")) {
            ps.setString(1, itemUuid);
            ps.setString(2, playerUuid.toString());
            ps.setLong(3, amount);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[CM] consumeCrystalItem error", e);
            return false;
        }
    }

    // ─────────────────────────────────────────────
    //  BLOCS RE-POSÉS — Anti-exploit BDD
    // ─────────────────────────────────────────────

    public void markBlockReplaced(String world, int x, int y, int z) {
        if (!isConnected()) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO replaced_blocks (world, x, y, z) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, world);
            ps.setInt(2, x);
            ps.setInt(3, y);
            ps.setInt(4, z);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[CM] markBlockReplaced error", e);
        }
    }

    public boolean isBlockReplaced(String world, int x, int y, int z) {
        if (!isConnected()) return false;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM replaced_blocks WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
            ps.setString(1, world);
            ps.setInt(2, x);
            ps.setInt(3, y);
            ps.setInt(4, z);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[CM] isBlockReplaced error", e);
            return false;
        }
    }

    public void removeBlockReplaced(String world, int x, int y, int z) {
        if (!isConnected()) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM replaced_blocks WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
            ps.setString(1, world);
            ps.setInt(2, x);
            ps.setInt(3, y);
            ps.setInt(4, z);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[CM] removeBlockReplaced error", e);
        }
    }

    // ─────────────────────────────────────────────
    //  LEADERBOARD
    // ─────────────────────────────────────────────

    public enum LeaderboardType { LEVEL, XP, TOKENS, BLOCKS_MINED }

    public List<LeaderboardEntry> getTop(LeaderboardType type, int limit) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        if (!isConnected()) return entries;

        String column = switch (type) {
            case LEVEL        -> "level";
            case XP           -> "xp";
            case TOKENS       -> "tokens";
            case BLOCKS_MINED -> "blocks_mined";
        };

        String sql = String.format(SELECT_TOP, column);
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            int rank = 1;
            while (rs.next()) {
                entries.add(new LeaderboardEntry(
                        rs.getString("username"),
                        UUID.fromString(rs.getString("uuid")),
                        rank++,
                        rs.getInt("level"),
                        rs.getInt("xp"),
                        rs.getInt("tokens"),
                        rs.getInt("blocks_mined")
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[CraftMining] Erreur leaderboard", e);
        }
        return entries;
    }

    public boolean isConnected() {
        try { return connection != null && !connection.isClosed(); }
        catch (SQLException e) { return false; }
    }

    public record LeaderboardEntry(
            String username, UUID uuid, int rank,
            int level, int xp, int tokens, int blocksMined) {}
}