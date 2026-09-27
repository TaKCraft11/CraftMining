package com.craftmining;

import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/** Donne les Cristaux des Abymes au minage. */
public class CrystalMiningListener implements Listener {

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

        switch (MiningRules.check(event, blockTracker)) {
            case REWARD -> giveCrystals(player, event.getBlock());
            case REPLACED -> sendActionBar(player, "§c❌ Ce bloc a été re-posé — aucun cristal !");
            case NETHER_ORE_OUTSIDE_NETHER ->
                    sendActionBar(player, "§c❌ Ce minerai appartient aux profondeurs du Nether.");
            case OVERWORLD_ORE_OUTSIDE_OVERWORLD ->
                    sendActionBar(player, "§c❌ Ce minerai appartient aux profondeurs de l'Overworld.");
            case IGNORED -> { /* bloc sans récompense : rien à faire */ }
        }
    }

    private void giveCrystals(Player player, Block block) {
        long gagne = crystalManager.calculateCrystals(block.getType().name(), block.getY());
        if (gagne <= 0) return;

        crystalManager.addCrystals(player, gagne);

        sendActionBar(player, "§d§l+" + crystalManager.format(gagne)
                + " 💎§r  §7│  §dSolde : §f"
                + crystalManager.format(crystalManager.getBalance(player))
                + " §dcristaux");
    }

    private void sendActionBar(Player player, String message) {
        player.sendActionBar(Component.text(message));
    }
}