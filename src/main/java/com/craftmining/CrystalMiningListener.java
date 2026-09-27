package com.craftmining;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Set;

public class CrystalMiningListener implements Listener {

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
    private final CrystalManager crystalManager;
    private final BlockTrackListener blockTracker;

    public CrystalMiningListener(CraftMining plugin,
                                 CrystalManager crystalManager,
                                 BlockTrackListener blockTracker) {
        this.plugin         = plugin;
        this.crystalManager = crystalManager;
        this.blockTracker   = blockTracker;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block   = event.getBlock();

        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (!BLOCS_RECOMPENSES.contains(block.getType())) return;

        // Anti-exploit bloc re-posé
        if (blockTracker.isReplaced(block)) {
            sendActionBar(player, "§c❌ Ce bloc a été re-posé — aucun cristal !");
            return;
        }

        // Anti-exploit dimension
        World.Environment env = block.getWorld().getEnvironment();
        Material type = block.getType();

        if (BLOCS_NETHER_ONLY.contains(type) && env != World.Environment.NETHER) {
            sendActionBar(player, "§c❌ Ce minerai appartient aux profondeurs du Nether.");
            return;
        }
        if (BLOCS_OVERWORLD_ONLY.contains(type) && env != World.Environment.NORMAL) {
            sendActionBar(player, "§c❌ Ce minerai appartient aux profondeurs de l'Overworld.");
            return;
        }

        long gagne = crystalManager.calculateCrystals(block.getType().name(), block.getY());
        if (gagne <= 0) return;

        crystalManager.addCrystals(player, gagne);

        sendActionBar(player, "§d§l+" + crystalManager.format(gagne)
                + " 💎§r  §7│  §dSolde : §f"
                + crystalManager.format(crystalManager.getBalance(player))
                + " §dcristaux");
    }

    private void sendActionBar(Player player, String message) {
        player.sendActionBar(net.kyori.adventure.text.Component.text(message));
    }
}