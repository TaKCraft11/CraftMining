package com.craftmining;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/** Donne l'XP de minage, les montées de niveau et les tokens. */
public class MiningListener implements Listener {

    private final CraftMining plugin;
    private final TokenManager tokenManager;
    private final ProgressBarManager progressBar;

    public MiningListener(CraftMining plugin,
                          TokenManager tokenManager,
                          ProgressBarManager progressBar) {
        this.plugin       = plugin;
        this.tokenManager = tokenManager;
        this.progressBar  = progressBar;
    }

    // MONITOR : on récompense en dernier, une fois que la casse est confirmée
    // (aucun plugin de protection ne peut plus l'annuler après nous).
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (MiningRules.check(event, plugin.getBlockTrackListener()) != MiningRules.Verdict.REWARD) return;

        Player player = event.getPlayer();
        int xpGained = getXpForBlock(event.getBlock().getType());
        if (xpGained <= 0) return;

        double mult = plugin.getShopManager().getXpMultiplier(player.getUniqueId());
        xpGained = (int) Math.ceil(xpGained * mult);

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        data.addXp(xpGained);

        checkLevelUp(player, data);

        int tokensAwarded = tokenManager.onBlockMined(player);
        if (tokensAwarded > 0) {
            String msg = plugin.getConfig()
                    .getString("messages.token-earned-mining", "&7+&d{amount} token(s) ✦ &8(minage)")
                    .replace("{amount}", String.valueOf(tokensAwarded));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
        }

        if (plugin.getConfig().getBoolean("progressbar.enabled", true)) {
            progressBar.show(player);
        }
    }

    private void checkLevelUp(Player player, PlayerData data) {
        int xpNeededForNext = ProgressBarManager.getXpForLevel(data.getLevel() + 1);

        while (data.getXp() >= xpNeededForNext) {
            data.incrementLevel();
            int newLevel    = data.getLevel();
            int bonusTokens = tokenManager.onLevelUp(player, newLevel);

            String msg = plugin.getConfig()
                    .getString("messages.level-up",
                            "&6⬆ &eLevel UP ! &7Niveau &a{level}&7 ! &d+{tokens} tokens ✦")
                    .replace("{level}", String.valueOf(newLevel))
                    .replace("{tokens}", String.valueOf(bonusTokens));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));

            xpNeededForNext = ProgressBarManager.getXpForLevel(newLevel + 1);
        }
    }

    private int getXpForBlock(Material type) {
        int base = plugin.getConfig().getInt("xp.per-block", 10);
        return switch (type) {
            case COAL_ORE, DEEPSLATE_COAL_ORE,
                 NETHER_QUARTZ_ORE, NETHER_GOLD_ORE -> base;
            case IRON_ORE, DEEPSLATE_IRON_ORE,
                 COPPER_ORE, DEEPSLATE_COPPER_ORE -> (int) (base * 1.5);
            case GOLD_ORE, DEEPSLATE_GOLD_ORE,
                 LAPIS_ORE, DEEPSLATE_LAPIS_ORE,
                 REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> base * 2;
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE,
                 EMERALD_ORE, DEEPSLATE_EMERALD_ORE ->
                    (int) (base * plugin.getConfig().getDouble("xp.rare-block-multiplier", 3.0));
            case ANCIENT_DEBRIS -> base * 5;
            default -> 0; // améthyste : cristaux uniquement, pas d'XP
        };
    }
}