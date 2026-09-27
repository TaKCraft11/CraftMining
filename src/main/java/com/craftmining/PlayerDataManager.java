package com.craftmining;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PlayerDataManager {

    private final CraftMining plugin;
    private final DatabaseManager db;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private final Set<UUID> loading = ConcurrentHashMap.newKeySet();
    private final int autoSaveMinutes;

    /** Un seul thread pour toutes les lectures/écritures en base : elles s'exécutent dans l'ordre. */
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "CraftMining-DB");
        thread.setDaemon(true);
        return thread;
    });

    private BukkitTask autoSaveTask;

    public PlayerDataManager(CraftMining plugin, DatabaseManager db) {
        this.plugin          = plugin;
        this.db              = db;
        this.autoSaveMinutes = plugin.getConfig().getInt("database.auto-save-minutes", 5);
    }

    // ------------------------------------------------------------------
    // Sauvegarde automatique
    // ------------------------------------------------------------------

    public void startAutoSave() {
        long intervalTicks = autoSaveMinutes * 60L * 20L;
        // Tâche sur le thread principal : on prépare la liste et les pseudos ici,
        // puis seule l'écriture en base part sur le thread DB.
        autoSaveTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (cache.isEmpty()) return;
            List<PlayerData> snapshot = new ArrayList<>();
            Map<UUID, String> names = takeSnapshot(snapshot);
            runDb(() -> db.saveAll(snapshot, uuid -> names.getOrDefault(uuid, uuid.toString())));
        }, intervalTicks, intervalTicks);
        plugin.getLogger().info("Auto-save activé toutes les " + autoSaveMinutes + " minutes.");
    }

    /** Arrête la sauvegarde auto et attend la fin des écritures en cours (10 secondes max). */
    public void stopAutoSave() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
        dbExecutor.shutdown();
        try {
            if (!dbExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Certaines écritures en base n'ont pas pu se terminer à temps.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ------------------------------------------------------------------
    // Connexion / déconnexion
    // ------------------------------------------------------------------

    public void loadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        loading.add(uuid);

        runDb(() -> {
            PlayerData loaded = db.loadPlayer(uuid);
            boolean isNew = (loaded == null);
            PlayerData data = isNew ? new PlayerData(uuid) : loaded;

            if (!plugin.isEnabled()) return; // plugin en cours d'arrêt
            Bukkit.getScheduler().runTask(plugin, () -> {
                loading.remove(uuid);
                // Le joueur a pu se déconnecter pendant le chargement : dans ce cas on ne garde rien.
                if (Bukkit.getPlayer(uuid) == null) return;
                cache.put(uuid, data);
                if (isNew) {
                    plugin.getLogger().info("Nouveau joueur : " + name);
                }
            });
        });
    }

    public void unloadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        loading.remove(uuid);
        PlayerData data = cache.remove(uuid);
        if (data != null) {
            runDb(() -> db.savePlayer(data, name));
        }
    }

    // ------------------------------------------------------------------
    // Accès aux données
    // ------------------------------------------------------------------

    /**
     * Données d'un joueur chargé.
     * Si le joueur n'est pas (encore) en cache, renvoie une fiche TEMPORAIRE qui n'est pas mise
     * en cache : elle ne sera jamais sauvegardée et ne peut donc pas écraser les vraies données en base.
     */
    public PlayerData getPlayerData(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data != null) return data;

        // Joueur en ligne mais jamais chargé (ex. plugin rechargé serveur allumé) : on lance le chargement.
        Player online = Bukkit.getPlayer(uuid);
        if (online != null && !loading.contains(uuid)) {
            loadPlayer(online);
        }
        return new PlayerData(uuid);
    }

    public boolean isLoaded(UUID uuid) {
        return cache.containsKey(uuid);
    }

    /** Sauvegarde synchrone de tout le cache (utilisée à l'arrêt du plugin). */
    public void saveAll() {
        if (cache.isEmpty()) return;
        List<PlayerData> snapshot = new ArrayList<>();
        Map<UUID, String> names = takeSnapshot(snapshot);
        db.saveAll(snapshot, uuid -> names.getOrDefault(uuid, uuid.toString()));
        plugin.getLogger().info("Sauvegarde finale : " + snapshot.size() + " joueur(s).");
    }

    public Collection<PlayerData> getAllCached() {
        return Collections.unmodifiableCollection(cache.values());
    }

    public DatabaseManager getDatabase() { return db; }

    // ------------------------------------------------------------------
    // Outils internes
    // ------------------------------------------------------------------

    /** Remplit {@code out} avec les fiches du cache et renvoie leurs pseudos (à appeler sur le thread principal). */
    private Map<UUID, String> takeSnapshot(List<PlayerData> out) {
        Map<UUID, String> names = new HashMap<>();
        for (Map.Entry<UUID, PlayerData> entry : cache.entrySet()) {
            out.add(entry.getValue());
            Player player = Bukkit.getPlayer(entry.getKey());
            names.put(entry.getKey(), player != null ? player.getName() : entry.getKey().toString());
        }
        return names;
    }

    /** Envoie une tâche au thread DB, ou l'exécute directement si le plugin est en cours d'arrêt. */
    private void runDb(Runnable task) {
        if (dbExecutor.isShutdown()) {
            task.run();
        } else {
            dbExecutor.execute(task);
        }
    }
}