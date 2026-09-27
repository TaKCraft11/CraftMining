package com.craftmining;

import com.craftmining.leaderboard.LeaderboardGUI;
import com.craftmining.shop.ShopGUI;
import com.craftmining.shop.ShopManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class CraftMining extends JavaPlugin {

    private DatabaseManager    databaseManager;
    private PlayerDataManager  playerDataManager;
    private TokenManager       tokenManager;
    private ProgressBarManager progressBarManager;
    private ShopManager        shopManager;
    private ShopGUI            shopGUI;
    private LeaderboardGUI     leaderboardGUI;
    private CrystalManager     crystalManager;
    private GemmeAbyssale      gemmeAbyssale;
    private BlockTrackListener blockTrackListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfAbsent("shop.yml");

        // 1. Base de données
        databaseManager = new DatabaseManager(this);
        if (!databaseManager.initialize()) {
            getLogger().severe("Impossible d'initialiser la base de données ! Désactivation du plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 2. Managers
        playerDataManager  = new PlayerDataManager(this, databaseManager);
        tokenManager       = new TokenManager(this, playerDataManager);
        progressBarManager = new ProgressBarManager(this, playerDataManager);
        shopManager        = new ShopManager(this, tokenManager);
        shopGUI            = new ShopGUI(this, shopManager, tokenManager);
        leaderboardGUI     = new LeaderboardGUI(this);
        crystalManager     = new CrystalManager(this, playerDataManager);
        gemmeAbyssale      = new GemmeAbyssale(this);
        blockTrackListener = new BlockTrackListener(this, databaseManager);

        // 3. Sauvegarde automatique
        playerDataManager.startAutoSave();

        // 4. Listeners
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new MiningListener(this, tokenManager, progressBarManager), this);
        pm.registerEvents(new FurnaceListener(this, tokenManager, progressBarManager), this);
        pm.registerEvents(new PlayerListener(this, playerDataManager), this);
        pm.registerEvents(shopGUI, this);
        pm.registerEvents(leaderboardGUI, this);
        pm.registerEvents(blockTrackListener, this);
        pm.registerEvents(new CrystalMiningListener(this, crystalManager, blockTrackListener), this);
        pm.registerEvents(new GemmeDepotListener(this, crystalManager, gemmeAbyssale, databaseManager), this);

        // 5. Commande
        CraftMiningCommand cmd = new CraftMiningCommand(this, tokenManager, progressBarManager,
                shopManager, shopGUI, leaderboardGUI, crystalManager, gemmeAbyssale);
        PluginCommand pluginCmd = getCommand("craftmining");
        if (pluginCmd != null) {
            pluginCmd.setExecutor(cmd);
            pluginCmd.setTabCompleter(cmd);
        } else {
            getLogger().warning("Commande 'craftmining' introuvable dans plugin.yml !");
        }

        getLogger().info("CraftMining activé ! (SQLite + Shop + Classement + Cristaux)");
    }

    @Override
    public void onDisable() {
        // 1. Arrêter la sauvegarde auto puis tout sauvegarder
        if (playerDataManager != null) {
            playerDataManager.stopAutoSave();
            playerDataManager.saveAll();
        }
        // 2. Fermer les barres de progression
        if (progressBarManager != null) {
            progressBarManager.shutdown();
        }
        // 3. Fermer la base de données en dernier
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("CraftMining désactivé. Données sauvegardées.");
    }

    /** Copie un fichier de ressources dans le dossier du plugin s'il n'existe pas encore. */
    private void saveResourceIfAbsent(String name) {
        File file = new File(getDataFolder(), name);
        if (!file.exists()) {
            saveResource(name, false);
        }
    }

    public DatabaseManager    getDatabaseManager()    { return databaseManager; }
    public PlayerDataManager  getPlayerDataManager()  { return playerDataManager; }
    public TokenManager       getTokenManager()       { return tokenManager; }
    public ProgressBarManager getProgressBarManager() { return progressBarManager; }
    public ShopManager        getShopManager()        { return shopManager; }
    public ShopGUI            getShopGUI()            { return shopGUI; }
    public LeaderboardGUI     getLeaderboardGUI()     { return leaderboardGUI; }
    public CrystalManager     getCrystalManager()     { return crystalManager; }
    public GemmeAbyssale      getGemmeAbyssale()      { return gemmeAbyssale; }
    public BlockTrackListener getBlockTrackListener() { return blockTrackListener; }
}