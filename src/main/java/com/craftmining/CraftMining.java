package com.craftmining;

import com.craftmining.leaderboard.LeaderboardGUI;
import com.craftmining.shop.ShopGUI;
import com.craftmining.shop.ShopManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.PluginCommand;

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

        // 1. Database
        databaseManager = new DatabaseManager(this);
        if (!databaseManager.initialize()) {
            getLogger().severe("Impossible d'initialiser la base de donnees ! Desactivation.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 2. Managers
        playerDataManager   = new PlayerDataManager(this, databaseManager);
        tokenManager        = new TokenManager(this, playerDataManager);
        progressBarManager  = new ProgressBarManager(this, playerDataManager);
        shopManager         = new ShopManager(this, tokenManager);
        shopGUI             = new ShopGUI(this, shopManager, tokenManager);
        leaderboardGUI      = new LeaderboardGUI(this);
        crystalManager      = new CrystalManager(this, playerDataManager);
        gemmeAbyssale       = new GemmeAbyssale(this);
        blockTrackListener  = new BlockTrackListener(this, databaseManager);

        // 3. Auto-save
        playerDataManager.startAutoSave();

        // 4. Listeners
        getServer().getPluginManager().registerEvents(
                new MiningListener(this, tokenManager, progressBarManager), this);
        getServer().getPluginManager().registerEvents(
                new FurnaceListener(this, tokenManager, progressBarManager), this);
        getServer().getPluginManager().registerEvents(
                new PlayerListener(this, playerDataManager), this);
        getServer().getPluginManager().registerEvents(shopGUI, this);
        getServer().getPluginManager().registerEvents(leaderboardGUI, this);
        getServer().getPluginManager().registerEvents(blockTrackListener, this);
        getServer().getPluginManager().registerEvents(
                new CrystalMiningListener(this, crystalManager, blockTrackListener), this);
        getServer().getPluginManager().registerEvents(
                new GemmeDepotListener(this, crystalManager, gemmeAbyssale, databaseManager), this);

        // 5. Command
        CraftMiningCommand cmd =
                new CraftMiningCommand(this, tokenManager, progressBarManager,
                        shopManager, shopGUI, leaderboardGUI, crystalManager, gemmeAbyssale);
        PluginCommand pluginCmd = getCommand("craftmining");
        if (pluginCmd != null) {
            pluginCmd.setExecutor(cmd);
            pluginCmd.setTabCompleter(cmd);
        }

        getLogger().info("CraftMining active ! (SQLite + Shop + Classement + Cristaux)");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.stopAutoSave();
            playerDataManager.saveAll();
        }
        if (databaseManager != null) databaseManager.close();
        if (progressBarManager != null) progressBarManager.shutdown();
        getLogger().info("CraftMining desactive. Donnees sauvegardees.");
    }

    private void saveResourceIfAbsent(String name) {
        java.io.File f = new java.io.File(getDataFolder(), name);
        if (!f.exists()) saveResource(name, false);
    }

    public DatabaseManager    getDatabaseManager()    { return databaseManager; }
    public PlayerDataManager  getPlayerDataManager()  { return playerDataManager; }
    public TokenManager       getTokenManager()        { return tokenManager; }
    public ProgressBarManager getProgressBarManager()  { return progressBarManager; }
    public ShopManager        getShopManager()         { return shopManager; }
    public ShopGUI            getShopGUI()             { return shopGUI; }
    public LeaderboardGUI     getLeaderboardGUI()      { return leaderboardGUI; }
    public CrystalManager     getCrystalManager()      { return crystalManager; }
    public GemmeAbyssale      getGemmeAbyssale()       { return gemmeAbyssale; }
    public BlockTrackListener getBlockTrackListener()  { return blockTrackListener; }
}