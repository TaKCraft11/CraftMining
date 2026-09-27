package com.craftmining;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Règles communes du minage récompensé : quels blocs, dans quelle dimension, et anti-exploit.
 * Le verdict est calculé UNE seule fois par bloc cassé, puis partagé entre les listeners
 * (XP/tokens et cristaux) : la base n'est interrogée qu'une fois.
 */
public final class MiningRules {

    public enum Verdict {
        /** Bloc valide : on récompense. */
        REWARD,
        /** Pas un bloc récompensé, ou joueur en créatif : on ne fait rien. */
        IGNORED,
        /** Bloc qui n'est pas d'origine (posé, déplacé, repoussé...). */
        REPLACED,
        /** Minerai du Nether cassé hors du Nether. */
        NETHER_ORE_OUTSIDE_NETHER,
        /** Minerai de l'Overworld cassé hors de l'Overworld. */
        OVERWORLD_ORE_OUTSIDE_OVERWORLD
    }

    /** Tous les blocs qui peuvent rapporter une récompense. */
    public static final Set<Material> REWARDED = Set.of(
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

    /** Minerais du Nether. Tous les autres blocs de REWARDED appartiennent à l'Overworld. */
    private static final Set<Material> NETHER_ONLY = Set.of(
            Material.ANCIENT_DEBRIS,
            Material.NETHER_QUARTZ_ORE,
            Material.NETHER_GOLD_ORE
    );

    /** Verdict déjà calculé pour chaque casse en cours (se vide tout seul ensuite). */
    private static final Map<BlockBreakEvent, Verdict> CACHE = new WeakHashMap<>();

    private MiningRules() {}

    public static Verdict check(BlockBreakEvent event, BlockTrackListener tracker) {
        return CACHE.computeIfAbsent(event, e -> compute(e, tracker));
    }

    private static Verdict compute(BlockBreakEvent event, BlockTrackListener tracker) {
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) return Verdict.IGNORED;

        Block block   = event.getBlock();
        Material type = block.getType();
        if (!REWARDED.contains(type)) return Verdict.IGNORED;

        // Contrôles rapides d'abord (dimension), la base de données en dernier
        World.Environment env = block.getWorld().getEnvironment();
        if (NETHER_ONLY.contains(type)) {
            if (env != World.Environment.NETHER) return Verdict.NETHER_ORE_OUTSIDE_NETHER;
        } else if (env != World.Environment.NORMAL) {
            return Verdict.OVERWORLD_ORE_OUTSIDE_OVERWORLD;
        }

        if (tracker.isReplaced(block)) return Verdict.REPLACED;
        return Verdict.REWARD;
    }
}
