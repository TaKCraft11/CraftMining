package com.craftmining;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * Accès à la base SQLite du plugin.
 * Toutes les méthodes publiques sont "synchronized" : une seule opération à la fois sur la
 * connexion, quel que soit le thread qui l'appelle (thread principal ou thread de sauvegarde).
 */
public class DatabaseManager {

    private final CraftMining plugin;
    private Connection connection;

    // ─────────────────────────────────────────────
    //  REQUÊTES
    // ─────────────────────────────────────────────

    private static final String CREATE_PLAYER_TABLE = """
            CREATE TABLE IF NOT EXISTS player_data (
                uuid           TEXT    PRIMARY KEY,
                username       TEXT    NOT NULL,
                xp             INTEGER NOT NULL DEFAULT 0,
                level          INTEGER NOT NULL DEFAULT 1,
                tokens         INTEGER NOT NULL DEFAULT 0,
                blocks_mined   INTEGER NOT NULL DEFAULT 0,
                items_smelted  INTEGER NOT NULL DEFAULT 0,
                last_mine_time INTEGER NOT NULL DEFAULT 0,
                last_seen      INTEGER NOT NULL DEFAULT 0,
                crystals       INTEGER NOT NULL DEFAULT 0
            );
            """;

    /** Ancien système anti-dupe (un identifiant = un dépôt). Conservé pour les anciennes gemmes. */
    private static final String CREATE_CRYSTAL_ITEMS = """
            CREATE TABLE IF NOT EXISTS crystal_items_used (
                item_uuid   TEXT    PRIMARY KEY,
                player_uuid TEXT    NOT NULL,
                amount      INTEGER NOT NULL,
                used_at     INTEGER NOT NULL DEFAULT (strftime('%s','now'))
            );
            """;

    /** Registre des gemmes : combien ont été émises et combien déposées, par identifiant. */
    private static final String CREATE_GEM_LEDGER = """
            CREATE TABLE IF NOT EXISTS gem_ledger (
                gem_uuid   TEXT    PRIMARY KEY,
                issued     INTEGER NOT NULL,
                redeemed   INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL DEFAULT (strftime('%s','now'))
            );
            """;

    private static final String CREATE_REPLACED_BLOCKS = """
            CREATE TABLE IF NOT EXISTS replaced_blocks (
                world     TEXT    NOT NULL,
                x         INTEGER NOT NULL,
                y         INTEGER NOT NULL,
                z         INTEGER NOT NULL,
                placed_at INTEGER NOT NULL DEFAULT (strftime('%s','now')),
                PRIMARY KEY (world, x, y, z)
            );
            """;

    private static final String ADD_CRYSTALS_COLUMN =
            "ALTER TABLE player_data ADD COLUMN crystals INTEGER NOT NULL DEFAULT 0;";

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

    /** %s = critère de tri (vient toujours de l'enum ci-dessous, jamais d'un joueur). */
    private static final String SELECT_TOP =
            "SELECT * FROM player_data ORDER BY %s LIMIT ?;";

    public DatabaseManager(CraftMining plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────
    //  OUVERTURE / FERMETURE
    // ─────────────────────────────────────────────

    public synchronized boolean initialize() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                plugin.getLogger().severe("Impossible de créer le dossier du plugin : " + dataFolder);
                return false;
            }

            File dbFile = new File(dataFolder, "craftmining.db");
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL;");
                stmt.execute("PRAGMA synchronous=NORMAL;");
                stmt.execute(CREATE_PLAYER_TABLE);
                stmt.execute(CREATE_CRYSTAL_ITEMS);
                stmt.execute(CREATE_GEM_LEDGER);
                stmt.execute(CREATE_REPLACED_BLOCKS);
            }

            // Migration : bases créées avant l'ajout des cristaux
            if (!columnExists("player_data", "crystals")) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute(ADD_CRYSTALS_COLUMN);
                }
                plugin.getLogger().info("Migration : colonne 'crystals' ajoutée à la base.");
            }

            plugin.getLogger().info("Base de données SQLite initialisée : " + dbFile.getName());
            return true;

        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("Driver SQLite introuvable ! " + e.getMessage());
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur SQL à l'initialisation de la base", e);
        }
        return false;
    }

    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Connexion SQLite fermée.");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erreur à la fermeture de SQLite", e);
        }
    }

    public synchronized boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    // ─────────────────────────────────────────────
    //  JOUEURS
    // ─────────────────────────────────────────────

    public synchronized void savePlayer(PlayerData data, String username) {
        if (!isConnected()) return;
        try (PreparedStatement ps = connection.prepareStatement(UPSERT_PLAYER)) {
            bindPlayer(ps, data, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erreur de sauvegarde du joueur " + username, e);
        }
    }

    public synchronized PlayerData loadPlayer(UUID uuid) {
        if (!isConnected()) return null;
        try (PreparedStatement ps = connection.prepareStatement(SELECT_PLAYER)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
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
            plugin.getLogger().log(Level.WARNING, "Erreur de chargement du joueur " + uuid, e);
            return null;
        }
    }

    /** Sauvegarde plusieurs joueurs en une seule transaction. */
    public synchronized void saveAll(List<PlayerData> dataList, Function<UUID, String> nameResolver) {
        if (!isConnected() || dataList.isEmpty()) return;
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement(UPSERT_PLAYER)) {
                for (PlayerData data : dataList) {
                    String name = nameResolver.apply(data.getUuid());
                    bindPlayer(ps, data, name != null ? name : data.getUuid().toString());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            connection.commit();
            plugin.getLogger().info("Sauvegarde : " + dataList.size() + " joueur(s).");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la sauvegarde groupée", e);
            try {
                connection.rollback();
            } catch (SQLException ignored) {
                // rien de plus à faire : l'erreur principale est déjà dans la console
            }
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
                // connexion probablement fermée
            }
        }
    }

    /** Remplit les 10 paramètres de UPSERT_PLAYER. */
    private void bindPlayer(PreparedStatement ps, PlayerData data, String username) throws SQLException {
        ps.setString(1, data.getUuid().toString());
        ps.setString(2, username);
        ps.setInt(3, data.getXp());
        ps.setInt(4, data.getLevel());
        ps.setInt(5, data.getTokens());
        ps.setInt(6, data.getBlocksMined());
        ps.setInt(7, data.getItemsSmelted());
        ps.setLong(8, data.getLastMineTime());
        ps.setLong(9, System.currentTimeMillis());
        ps.setLong(10, data.getCrystals());
    }

    // ─────────────────────────────────────────────
    //  GEMMES ABYSSALES — Registre anti-dupe
    // ─────────────────────────────────────────────

    /** Enregistre l'émission de {@code quantity} gemmes sous un même identifiant. */
    public synchronized boolean registerGems(String gemUuid, int quantity) {
        if (!isConnected()) return false;
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO gem_ledger (gem_uuid, issued, redeemed) VALUES (?, ?, 0)")) {
            ps.setString(1, gemUuid);
            ps.setInt(2, quantity);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur d'enregistrement de gemmes " + gemUuid, e);
            return false;
        }
    }

    /**
     * Dépose des gemmes : n'accorde JAMAIS plus que ce qui a été émis pour cet identifiant.
     * @param requested   nombre de gemmes que le joueur veut déposer
     * @param stackAmount taille de la pile en main (sert uniquement pour les anciennes gemmes)
     * @return nombre de gemmes accordées (0 = déjà toutes déposées), ou -1 en cas d'erreur
     */
    public synchronized int redeemGems(String gemUuid, int requested, int stackAmount) {
        if (!isConnected() || requested <= 0) return -1;
        try {
            int issued   = -1;
            int redeemed = 0;
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT issued, redeemed FROM gem_ledger WHERE gem_uuid = ?")) {
                ps.setString(1, gemUuid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        issued   = rs.getInt("issued");
                        redeemed = rs.getInt("redeemed");
                    }
                }
            }

            if (issued < 0) {
                // Gemme créée avant le registre (ancien système)
                if (isCrystalItemUsed(gemUuid)) return 0;
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO gem_ledger (gem_uuid, issued, redeemed) VALUES (?, ?, ?)")) {
                    ps.setString(1, gemUuid);
                    ps.setInt(2, stackAmount);
                    ps.setInt(3, requested);
                    ps.executeUpdate();
                }
                return requested;
            }

            int granted = Math.min(requested, issued - redeemed);
            if (granted <= 0) return 0;

            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE gem_ledger SET redeemed = redeemed + ? WHERE gem_uuid = ?")) {
                ps.setInt(1, granted);
                ps.setString(2, gemUuid);
                ps.executeUpdate();
            }
            return granted;

        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erreur de dépôt de gemmes " + gemUuid, e);
            return -1;
        }
    }

    /** Ancien système : cet identifiant a-t-il déjà été déposé ? */
    public synchronized boolean isCrystalItemUsed(String itemUuid) {
        if (!isConnected()) return true; // par prudence : on considère la gemme comme déjà utilisée
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM crystal_items_used WHERE item_uuid = ?")) {
            ps.setString(1, itemUuid);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erreur de vérification d'une gemme", e);
            return true;
        }
    }

    /** Ancien système, conservé pour compatibilité. */
    public synchronized boolean consumeCrystalItem(String itemUuid, UUID playerUuid, long amount) {
        if (!isConnected()) return false;
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR IGNORE INTO crystal_items_used (item_uuid, player_uuid, amount) VALUES (?, ?, ?)")) {
            ps.setString(1, itemUuid);
            ps.setString(2, playerUuid.toString());
            ps.setLong(3, amount);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erreur de consommation d'une gemme", e);
            return false;
        }
    }

    // ─────────────────────────────────────────────
    //  BLOCS RE-POSÉS — Anti-exploit
    // ─────────────────────────────────────────────

    public synchronized void markBlockReplaced(String world, int x, int y, int z) {
        if (!isConnected()) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO replaced_blocks (world, x, y, z) VALUES (?, ?, ?, ?)")) {
            bindBlock(ps, world, x, y, z);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erreur d'enregistrement d'un bloc posé", e);
        }
    }

    public synchronized boolean isBlockReplaced(String world, int x, int y, int z) {
        if (!isConnected()) return false;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM replaced_blocks WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
            bindBlock(ps, world, x, y, z);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erreur de vérification d'un bloc posé", e);
            return false;
        }
    }

    public synchronized void removeBlockReplaced(String world, int x, int y, int z) {
        if (!isConnected()) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM replaced_blocks WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
            bindBlock(ps, world, x, y, z);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erreur de suppression d'un bloc posé", e);
        }
    }

    private void bindBlock(PreparedStatement ps, String world, int x, int y, int z) throws SQLException {
        ps.setString(1, world);
        ps.setInt(2, x);
        ps.setInt(3, y);
        ps.setInt(4, z);
    }

    // ─────────────────────────────────────────────
    //  CLASSEMENT
    // ─────────────────────────────────────────────

    public enum LeaderboardType { LEVEL, XP, TOKENS, BLOCKS_MINED }

    public synchronized List<LeaderboardEntry> getTop(LeaderboardType type, int limit) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        if (!isConnected()) return entries;

        String orderBy = switch (type) {
            case LEVEL        -> "level DESC, xp DESC"; // à niveau égal, le plus d'XP passe devant
            case XP           -> "xp DESC";
            case TOKENS       -> "tokens DESC";
            case BLOCKS_MINED -> "blocks_mined DESC";
        };

        try (PreparedStatement ps = connection.prepareStatement(String.format(SELECT_TOP, orderBy))) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
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
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erreur de lecture du classement", e);
        }
        return entries;
    }

    public record LeaderboardEntry(
            String username, UUID uuid, int rank,
            int level, int xp, int tokens, int blocksMined) {}

    // ─────────────────────────────────────────────
    //  OUTILS INTERNES
    // ─────────────────────────────────────────────

    private boolean columnExists(String table, String column) throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + table + ");")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) return true;
            }
        }
        return false;
    }
}