package com.craftmining;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;

public class PlayerListener implements Listener {

    private final CraftMining plugin;
    private final PlayerDataManager dataManager;

    public PlayerListener(CraftMining plugin, PlayerDataManager dataManager) {
        this.plugin      = plugin;
        this.dataManager = dataManager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        dataManager.loadPlayer(player);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        dataManager.unloadPlayer(player);
        plugin.getProgressBarManager().hide(player);
    }
}