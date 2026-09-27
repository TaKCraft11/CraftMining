package com.craftmining;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class CrystalManager {

    private final CraftMining plugin;
    private final PlayerDataManager playerDataManager;

    public CrystalManager(CraftMining plugin, PlayerDataManager playerDataManager) {
        this.plugin            = plugin;
        this.playerDataManager = playerDataManager;
    }

    public long getBalance(Player player) {
        return playerDataManager.getPlayerData(player.getUniqueId()).getCrystals();
    }

    public void addCrystals(Player player, long amount) {
        if (amount <= 0) return;
        playerDataManager.getPlayerData(player.getUniqueId()).addCrystals(amount);
    }

    public boolean removeCrystals(Player player, long amount) {
        if (amount <= 0) return false;
        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
        if (data.getCrystals() < amount) return false;
        data.removeCrystals(amount);
        return true;
    }

    public boolean hasCrystals(Player player, long amount) {
        return getBalance(player) >= amount;
    }

    public long calculateCrystals(String blockType, int y) {
        FileConfiguration cfg = plugin.getConfig();
        long base = cfg.getLong("crystal-rates." + blockType, 0L);
        if (base <= 0) return 0;
        double multiplier = getDepthMultiplier(y, cfg);
        return Math.round(base * multiplier);
    }

    private double getDepthMultiplier(int y, FileConfiguration cfg) {
        if (y <= -58) return 1.0 + cfg.getDouble("depth-bonus.tier4.bonus", 0.75);
        if (y <= -32) return 1.0 + cfg.getDouble("depth-bonus.tier3.bonus", 0.50);
        if (y <= -16) return 1.0 + cfg.getDouble("depth-bonus.tier2.bonus", 0.25);
        if (y <=   0) return 1.0 + cfg.getDouble("depth-bonus.tier1.bonus", 0.10);
        return 1.0;
    }

    public String format(long amount) {
        return String.format("%,d", amount).replace(',', ' ');
    }
}