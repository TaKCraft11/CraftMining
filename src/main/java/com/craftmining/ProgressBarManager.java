package com.craftmining;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProgressBarManager {

    private static final int BAR_LENGTH    = 20;
    private static final int DISPLAY_TICKS = 60;

    private final CraftMining plugin;
    private final PlayerDataManager dataManager;
    private final Map<UUID, BukkitTask> fadeTasks = new HashMap<>();

    public ProgressBarManager(CraftMining plugin, PlayerDataManager dataManager) {
        this.plugin      = plugin;
        this.dataManager = dataManager;
    }

    public void show(Player player) {
        PlayerData data      = dataManager.getPlayerData(player.getUniqueId());
        int level            = data.getLevel();
        int currentXP        = data.getXp();
        int xpForNext        = getXpForLevel(level + 1);
        int xpForCurrent     = getXpForLevel(level);
        int xpIntoLevel      = currentXP - xpForCurrent;
        int xpNeeded         = xpForNext - xpForCurrent;

        double progress      = xpNeeded > 0 ? (double) xpIntoLevel / xpNeeded : 1.0;
        progress             = Math.min(1.0, Math.max(0.0, progress));

        int filled           = (int) Math.round(progress * BAR_LENGTH);
        int empty            = BAR_LENGTH - filled;

        StringBuilder bar = new StringBuilder();
        bar.append("§8[");
        bar.append("§a").append("█".repeat(filled));
        bar.append("§7").append("░".repeat(empty));
        bar.append("§8] ");
        bar.append("§eLvl §6").append(level)
                .append(" §8| §b").append(xpIntoLevel).append("§7/§b").append(xpNeeded).append(" XP")
                .append(" §8| §d").append(data.getTokens()).append(" ✦");

        player.sendActionBar(Component.text(bar.toString()));

        cancelFade(player.getUniqueId());
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            fadeTasks.remove(player.getUniqueId());
            player.sendActionBar(Component.empty());
        }, DISPLAY_TICKS);
        fadeTasks.put(player.getUniqueId(), task);
    }

    public void hide(Player player) {
        cancelFade(player.getUniqueId());
        player.sendActionBar(Component.empty());
    }

    public void shutdown() {
        fadeTasks.values().forEach(BukkitTask::cancel);
        fadeTasks.clear();
    }

    public static int getXpForLevel(int level) {
        if (level <= 1) return 0;
        return (level - 1) * (level - 1) * 50;
    }

    private void cancelFade(UUID uuid) {
        BukkitTask existing = fadeTasks.remove(uuid);
        if (existing != null) existing.cancel();
    }
}