package com.craftmining;

import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private int xp;
    private int level;
    private int blocksMined;
    private long lastMineTime;
    private int itemsSmelted;
    private int tokens;
    private long crystals;

    public PlayerData(UUID uuid) {
        this.uuid         = uuid;
        this.xp           = 0;
        this.level        = 1;
        this.blocksMined  = 0;
        this.itemsSmelted = 0;
        this.tokens       = 0;
        this.lastMineTime = 0L;
        this.crystals     = 0L;
    }

    public UUID getUuid()                 { return uuid; }

    public int getXp()                    { return xp; }
    public void setXp(int xp)            { this.xp = Math.max(0, xp); }
    public void addXp(int amount)        { this.xp += amount; }

    public int getLevel()                 { return level; }
    public void setLevel(int level)      { this.level = Math.max(1, level); }
    public void incrementLevel()         { this.level++; }

    public int getBlocksMined()           { return blocksMined; }
    public void setBlocksMined(int v)    { this.blocksMined = Math.max(0, v); }
    public void incrementBlocksMined()   { this.blocksMined++; }

    public long getLastMineTime()         { return lastMineTime; }
    public void updateLastMineTime()     { this.lastMineTime = System.currentTimeMillis(); }

    public int getItemsSmelted()          { return itemsSmelted; }
    public void setItemsSmelted(int v)   { this.itemsSmelted = Math.max(0, v); }
    public void incrementItemsSmelted()  { this.itemsSmelted++; }

    public int getTokens()                { return tokens; }
    public void setTokens(int v)         { this.tokens = Math.max(0, v); }
    public void addTokens(int amount)    { this.tokens += Math.max(0, amount); }
    public void removeTokens(int amount) { this.tokens = Math.max(0, this.tokens - amount); }

    public long getCrystals()                  { return crystals; }
    public void setCrystals(long v)           { this.crystals = Math.max(0L, v); }
    public void addCrystals(long amount)      { this.crystals += Math.max(0L, amount); }
    public void removeCrystals(long amount)   { this.crystals = Math.max(0L, this.crystals - amount); }
}