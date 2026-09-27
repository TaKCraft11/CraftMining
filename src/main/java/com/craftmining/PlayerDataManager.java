package com.craftmining;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private final CraftMining plugin;
    private final DatabaseManager db;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private BukkitTask autoSaveTask;
    private int autoSaveMinutes;

    public PlayerDataManager(CraftMining plugin, DatabaseManager db) {
        this.plugin          = plugin;
        this.db              = db;
        this.autoSaveMinutes = plugin.getConfig().getInt("database.auto-save-minutes", 5);
    }

    public void startAutoSave() {
        long intervalTicks = autoSaveMinutes * 60L * 20L;
        autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            List<PlayerData> snapshot = new ArrayList<>(cache.values());
            db.saveAll(snapshot, uuid -> {
                Player p = Bukkit.getPlayer(uuid);
                return p != null ? p.getName() : uuid.toString();
            });
        }, intervalTicks, intervalTicks);
        plugin.getLogger().info("[CraftMining] Auto-save activé toutes les " + autoSaveMinutes + " minutes.");
    }

    public void stopAutoSave() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
    }

    public void loadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = db.loadPlayer(uuid);
            if (data == null) {
                data = new PlayerData(uuid);
                plugin.getLogger().info("[CraftMining] Nouveau joueur : " + player.getName());
            }
            final PlayerData finalData = data;
            Bukkit.getScheduler().runTask(plugin, () -> cache.put(uuid, finalData));
        });
    }

    public void unloadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData data = cache.remove(uuid);
        if (data != null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin,
                    () -> db.savePlayer(data, player.getName()));
        }
    }

    public PlayerData getPlayerData(UUID uuid) {
        return cache.computeIfAbsent(uuid, PlayerData::new);
    }

    public boolean isLoaded(UUID uuid) {
        return cache.containsKey(uuid);
    }

    public void saveAll() {
        List<PlayerData> snapshot = new ArrayList<>(cache.values());
        if (snapshot.isEmpty()) return;
        db.saveAll(snapshot, uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            return p != null ? p.getName() : uuid.toString();
        });
        plugin.getLogger().info("[CraftMining] Sauvegarde finale : " + snapshot.size() + " joueur(s).");
    }

    public Collection<PlayerData> getAllCached() {
        return Collections.unmodifiableCollection(cache.values());
    }

    public DatabaseManager getDatabase() { return db; }
}