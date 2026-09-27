package com.craftmining;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.entity.Player;

import java.util.Set;

public class MiningListener implements Listener {

    private static final Set<Material> BLOCS_RECOMPENSES = Set.of(
            Material.COAL_ORE,              Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE,              Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE,            Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE,              Material.DEEPSLATE_GOLD_ORE,
            Material.LAPIS_ORE,             Material.DEEPSLATE_LAPIS_ORE,
            Material.REDSTONE_ORE,          Material.DEEPSLATE_REDSTONE_ORE,
            Material.DIAMOND_ORE,           Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE,           Material.DEEPSLATE_EMERALD_ORE,
            Material.ANCIENT_DEBRIS,
            Material.NETHER_QUARTZ_ORE,     Material.NETHER_GOLD_ORE,
            Material.AMETHYST_CLUSTER
    );

    private static final Set<Material> BLOCS_NETHER_ONLY = Set.of(
            Material.ANCIENT_DEBRIS,
            Material.NETHER_QUARTZ_ORE,
            Material.NETHER_GOLD_ORE
    );

    private static final Set<Material> BLOCS_OVERWORLD_ONLY = Set.of(
            Material.COAL_ORE,              Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE,              Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE,            Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE,              Material.DEEPSLATE_GOLD_ORE,
            Material.LAPIS_ORE,             Material.DEEPSLATE_LAPIS_ORE,
            Material.REDSTONE_ORE,          Material.DEEPSLATE_REDSTONE_ORE,
            Material.DIAMOND_ORE,           Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE,           Material.DEEPSLATE_EMERALD_ORE,
            Material.AMETHYST_CLUSTER
    );

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

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material type = event.getBlock().getType();

        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (!BLOCS_RECOMPENSES.contains(type)) return;

        // Anti-exploit bloc re-posé
        if (plugin.getBlockTrackListener().isReplaced(event.getBlock())) return;

        // Anti-exploit dimension
        World.Environment env = event.getBlock().getWorld().getEnvironment();

        if (BLOCS_NETHER_ONLY.contains(type) && env != World.Environment.NETHER) return;
        if (BLOCS_OVERWORLD_ONLY.contains(type) && env != World.Environment.NORMAL) return;

        int xpGained = getXpForBlock(event);
        if (xpGained <= 0) return;

        double mult = plugin.getShopManager().getXpMultiplier(player.getUniqueId());
        xpGained = (int) Math.ceil(xpGained * mult);

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        data.addXp(xpGained);

        checkLevelUp(player, data);

        int tokensAwarded = tokenManager.onBlockMined(player);
        if (tokensAwarded > 0) {
            String msg = plugin.getConfig()
                    .getString("messages.token-earned-mining",
                            "&7+&d{amount} token(s) ✦ &8(minage)")
                    .replace("{amount}", String.valueOf(tokensAwarded));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
        }

        if (plugin.getConfig().getBoolean("progressbar.enabled", true)) {
            progressBar.show(player);
        }
    }

    private void checkLevelUp(Player player, PlayerData data) {
        int currentLevel    = data.getLevel();
        int xpNeededForNext = ProgressBarManager.getXpForLevel(currentLevel + 1);

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

    private int getXpForBlock(BlockBreakEvent event) {
        return switch (event.getBlock().getType()) {
            case COAL_ORE, DEEPSLATE_COAL_ORE ->
                    plugin.getConfig().getInt("xp.per-block", 10);
            case IRON_ORE, DEEPSLATE_IRON_ORE,
                 COPPER_ORE, DEEPSLATE_COPPER_ORE ->
                    (int)(plugin.getConfig().getInt("xp.per-block", 10) * 1.5);
            case GOLD_ORE, DEEPSLATE_GOLD_ORE,
                 LAPIS_ORE, DEEPSLATE_LAPIS_ORE,
                 REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE ->
                    (int)(plugin.getConfig().getInt("xp.per-block", 10) * 2.0);
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE,
                 EMERALD_ORE, DEEPSLATE_EMERALD_ORE ->
                    (int)(plugin.getConfig().getDouble("xp.rare-block-multiplier", 3.0)
                            * plugin.getConfig().getInt("xp.per-block", 10));
            case NETHER_QUARTZ_ORE, NETHER_GOLD_ORE ->
                    plugin.getConfig().getInt("xp.per-block", 10);
            case ANCIENT_DEBRIS ->
                    (int)(plugin.getConfig().getInt("xp.per-block", 10) * 5.0);
            default -> 0;
        };
    }
}