package com.craftmining;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceExtractEvent;

public class FurnaceListener implements Listener {

    private final CraftMining plugin;
    private final TokenManager tokenManager;
    private final ProgressBarManager progressBar;

    public FurnaceListener(CraftMining plugin,
                           TokenManager tokenManager,
                           ProgressBarManager progressBar) {
        this.plugin       = plugin;
        this.tokenManager = tokenManager;
        this.progressBar  = progressBar;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        int xpBase    = plugin.getConfig().getInt("xp.per-smelt", 5);

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        data.addXp(xpBase);

        int tokensEarned = 0;
        for (int i = 0; i < event.getItemAmount(); i++) {
            tokensEarned += tokenManager.onItemSmelted(player);
        }

        if (tokensEarned > 0) {
            String msg = plugin.getConfig()
                    .getString("messages.token-earned-smelt",
                            "&7+&d{amount} token(s) ✦ &8(fonte)")
                    .replace("{amount}", String.valueOf(tokensEarned));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
        }

        if (plugin.getConfig().getBoolean("progressbar.enabled", true)) {
            progressBar.show(player);
        }
    }
}