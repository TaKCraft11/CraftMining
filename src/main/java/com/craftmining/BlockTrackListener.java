package com.craftmining;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockTrackListener implements Listener {

    private static final java.util.Set<Material> BLOCS_TRACKED = java.util.Set.of(
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
            Material.MAGMA_BLOCK,           Material.AMETHYST_CLUSTER
    );

    private final CraftMining plugin;
    private final DatabaseManager db;

    public BlockTrackListener(CraftMining plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db     = db;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        Block block = event.getBlock();
        if (!BLOCS_TRACKED.contains(block.getType())) return;

        plugin.getLogger().info("[DEBUG-PLACE] Bloc posé : " + block.getType().name()
                + " à " + block.getWorld().getName()
                + " " + block.getX() + "," + block.getY() + "," + block.getZ());

        db.markBlockReplaced(
                block.getWorld().getName(),
                block.getX(), block.getY(), block.getZ()
        );

        plugin.getLogger().info("[DEBUG-PLACE] Marqué en BDD !");
    }

    public boolean isReplaced(Block block) {
        boolean result = db.isBlockReplaced(
                block.getWorld().getName(),
                block.getX(), block.getY(), block.getZ()
        );
        plugin.getLogger().info("[DEBUG-CHECK] isReplaced pour "
                + block.getType().name() + " = " + result);
        return result;
    }
}