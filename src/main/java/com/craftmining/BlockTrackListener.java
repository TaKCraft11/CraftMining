package com.craftmining;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Anti-exploit : mémorise en base toute position où un minerai est apparu autrement
 * que par la génération du monde (pose, piston, entité, pousse...).
 * Ces blocs ne donnent pas de récompense quand on les mine.
 */
public class BlockTrackListener implements Listener {

    private static final Set<Material> BLOCS_TRACKED = Set.of(
            Material.COAL_ORE,          Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE,          Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE,        Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE,          Material.DEEPSLATE_GOLD_ORE,
            Material.LAPIS_ORE,         Material.DEEPSLATE_LAPIS_ORE,
            Material.REDSTONE_ORE,      Material.DEEPSLATE_REDSTONE_ORE,
            Material.DIAMOND_ORE,       Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE,       Material.DEEPSLATE_EMERALD_ORE,
            Material.ANCIENT_DEBRIS,
            Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE,
            Material.AMETHYST_CLUSTER
    );

    private final CraftMining plugin;
    private final DatabaseManager db;

    public BlockTrackListener(CraftMining plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db     = db;
    }

    // ─────────────────────────────────────────────
    //  1. POSE PAR UN JOUEUR (tous modes de jeu, créatif compris)
    // ─────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (!isTracked(block.getType())) return;
        mark(block);
        debug("Posé par " + event.getPlayer().getName() + " : " + describe(block));
    }

    // ─────────────────────────────────────────────
    //  2. PISTONS (normal, collant, slime, miel, machines volantes)
    // ─────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        followMovedBlocks(event.getBlocks(), event.getDirection());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        followMovedBlocks(event.getBlocks(), event.getDirection());
    }

    private void followMovedBlocks(List<Block> blocks, BlockFace direction) {
        // D'abord repérer tous les blocs marqués (un bloc peut prendre la place d'un autre)
        List<Block> marked = new ArrayList<>();
        for (Block block : blocks) {
            if (isTracked(block.getType()) && isMarked(block)) {
                marked.add(block);
            }
        }
        // Puis marquer leur nouvelle position (l'ancienne reste marquée, par sécurité)
        for (Block block : marked) {
            Block destination = block.getRelative(direction);
            mark(destination);
            debug("Déplacé par piston : " + describe(block) + " -> " + destination.getX()
                    + "," + destination.getY() + "," + destination.getZ());
        }
    }

    // ─────────────────────────────────────────────
    //  3. ENTITÉS (bloc qui tombe, enderman, etc.)
    // ─────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!isTracked(event.getTo())) return;
        // Même type avant/après = simple changement d'état (ex. redstone qui s'allume), pas une pose
        if (event.getBlock().getType() == event.getTo()) return;
        mark(event.getBlock());
        debug("Posé par une entité (" + event.getEntityType().name() + ") : " + event.getTo().name()
                + " à " + event.getBlock().getX() + "," + event.getBlock().getY() + "," + event.getBlock().getZ());
    }

    // ─────────────────────────────────────────────
    //  4. POUSSE / FORMATION NATURELLE (améthyste qui repousse, etc.)
    // ─────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        markIfGrown(event.getBlock(), event.getNewState().getType());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent event) {
        markIfGrown(event.getBlock(), event.getNewState().getType());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        markIfGrown(event.getBlock(), event.getNewState().getType());
    }

    private void markIfGrown(Block block, Material newType) {
        if (!isTracked(newType)) return;
        mark(block);
        debug("Apparu par pousse : " + newType.name() + " à "
                + block.getX() + "," + block.getY() + "," + block.getZ());
    }

    // ─────────────────────────────────────────────
    //  VÉRIFICATION (utilisée par les listeners de minage)
    // ─────────────────────────────────────────────

    /** @return true si ce bloc n'est pas d'origine (pas de récompense). */
    public boolean isReplaced(Block block) {
        boolean result = isMarked(block);
        debug("isReplaced pour " + block.getType().name() + " = " + result);
        return result;
    }

    // ─────────────────────────────────────────────
    //  OUTILS INTERNES
    // ─────────────────────────────────────────────

    private boolean isTracked(Material material) {
        return BLOCS_TRACKED.contains(material);
    }

    private void mark(Block block) {
        db.markBlockReplaced(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    private boolean isMarked(Block block) {
        return db.isBlockReplaced(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    private String describe(Block block) {
        return block.getType().name() + " à " + block.getWorld().getName()
                + " " + block.getX() + "," + block.getY() + "," + block.getZ();
    }

    /** N'affiche le message que si "debug: true" dans config.yml. */
    private void debug(String message) {
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }
}