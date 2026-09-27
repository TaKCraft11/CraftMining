package com.craftmining;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class TokenManager {

    private final CraftMining plugin;
    private final PlayerDataManager dataManager;

    private int tokensPerXBlocks;
    private int tokensPerXSmelts;
    private int tokenLevelUpMultiplier;
    private boolean streakEnabled;
    private int streakIntervalSeconds;
    private int streakBonusTokens;

    public TokenManager(CraftMining plugin, PlayerDataManager dataManager) {
        this.plugin      = plugin;
        this.dataManager = dataManager;
        reload();
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();
        tokensPerXBlocks       = config.getInt("tokens.mining.blocks-per-token", 10);
        tokensPerXSmelts       = config.getInt("tokens.smelting.smelts-per-token", 5);
        tokenLevelUpMultiplier = config.getInt("tokens.levelup.level-multiplier", 5);
        streakEnabled          = config.getBoolean("tokens.streak.enabled", true);
        streakIntervalSeconds  = config.getInt("tokens.streak.interval-seconds", 30);
        streakBonusTokens      = config.getInt("tokens.streak.bonus-tokens", 2);
    }

    public int getTokens(Player player) {
        return dataManager.getPlayerData(player.getUniqueId()).getTokens();
    }

    public int addTokens(Player player, int amount) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        data.addTokens(amount);
        return data.getTokens();
    }

    public boolean removeTokens(Player player, int amount) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTokens() < amount) return false;
        data.removeTokens(amount);
        return true;
    }

    public void setTokens(Player player, int amount) {
        dataManager.getPlayerData(player.getUniqueId()).setTokens(Math.max(0, amount));
    }

    public int onBlockMined(Player player) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        data.incrementBlocksMined();

        int totalMined = data.getBlocksMined();
        if (totalMined % tokensPerXBlocks == 0) {
            int awarded = 1;
            if (streakEnabled && isOnStreak(data)) {
                awarded += streakBonusTokens;
            }
            data.addTokens(awarded);
            data.updateLastMineTime();
            return awarded;
        }
        data.updateLastMineTime();
        return 0;
    }

    public int onItemSmelted(Player player) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        data.incrementItemsSmelted();

        int totalSmelted = data.getItemsSmelted();
        if (totalSmelted % tokensPerXSmelts == 0) {
            data.addTokens(1);
            return 1;
        }
        return 0;
    }

    public int onLevelUp(Player player, int newLevel) {
        int awarded = newLevel * tokenLevelUpMultiplier;
        dataManager.getPlayerData(player.getUniqueId()).addTokens(awarded);
        return awarded;
    }

    private boolean isOnStreak(PlayerData data) {
        long lastMine = data.getLastMineTime();
        if (lastMine == 0) return false;
        long elapsed = (System.currentTimeMillis() - lastMine) / 1000L;
        return elapsed <= streakIntervalSeconds;
    }

    public int getTokensPerXBlocks()       { return tokensPerXBlocks; }
    public int getTokensPerXSmelts()       { return tokensPerXSmelts; }
    public int getTokenLevelUpMultiplier() { return tokenLevelUpMultiplier; }
    public boolean isStreakEnabled()       { return streakEnabled; }
    public int getStreakIntervalSeconds()  { return streakIntervalSeconds; }
    public int getStreakBonusTokens()      { return streakBonusTokens; }
}