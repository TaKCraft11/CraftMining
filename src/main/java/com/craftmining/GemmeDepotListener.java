package com.craftmining;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/** Dépôt des Gemmes Abyssales + protections contre leur utilisation comme simple améthyste. */
public class GemmeDepotListener implements Listener {

    private final CraftMining plugin;
    private final CrystalManager crystalManager;
    private final GemmeAbyssale gemmeAbyssale;
    private final DatabaseManager db;

    public GemmeDepotListener(CraftMining plugin,
                              CrystalManager crystalManager,
                              GemmeAbyssale gemmeAbyssale,
                              DatabaseManager db) {
        this.plugin         = plugin;
        this.crystalManager = crystalManager;
        this.gemmeAbyssale  = gemmeAbyssale;
        this.db             = db;
    }

    // ─────────────────────────────────────────────
    //  DÉPÔT
    // ─────────────────────────────────────────────

    // Pas de "ignoreCancelled" : Paper marque les clics dans le vide comme déjà annulés,
    // ils seraient ignorés et le dépôt ne marcherait qu'en visant un bloc.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        ItemStack hand = player.getInventory().getItemInMainHand();
        String uuid = gemmeAbyssale.getUuid(hand);
        if (uuid == null) return;

        event.setCancelled(true);

        // Données pas encore chargées (juste après la connexion) : les cristaux seraient perdus
        if (!crystalManager.isReady(player)) {
            player.sendMessage(ChatColor.YELLOW + "⏳ Chargement de tes données en cours, réessaie dans un instant.");
            return;
        }

        int stackAmount = hand.getAmount();
        int requested   = player.isSneaking() ? stackAmount : 1;

        int granted = db.redeemGems(uuid, requested, stackAmount);
        if (granted < 0) {
            player.sendMessage(ChatColor.RED + "❌ Erreur lors du dépôt, réessaie dans un instant.");
            return;
        }

        // Retirer de la main les gemmes présentées (accordées + éventuelles copies refusées)
        int left = stackAmount - requested;
        if (left <= 0) {
            player.getInventory().setItemInMainHand(null);
        } else {
            hand.setAmount(left);
            player.getInventory().setItemInMainHand(hand);
        }

        if (granted > 0) {
            long montant = granted * GemmeAbyssale.CRISTAUX_PAR_GEMME;
            crystalManager.addCrystals(player, montant);
            envoyerMessage(player, montant, granted);
        }

        if (granted < requested) {
            int refused = requested - granted;
            player.sendMessage(ChatColor.RED + "❌ " + refused
                    + " gemme(s) déjà déposée(s) ailleurs : retirée(s).");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            plugin.getLogger().warning("Possible dupe de gemmes : " + player.getName()
                    + " a présenté " + requested + " gemme(s), " + refused
                    + " déjà déposée(s). Identifiant : " + uuid);
        }
    }

    private void envoyerMessage(Player player, long montant, int nbGemmes) {
        long solde = crystalManager.getBalance(player);
        player.sendMessage("");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "✦ " + ChatColor.BOLD
                + (nbGemmes > 1 ? nbGemmes + " gemmes déposées !" : "Gemme déposée !"));
        player.sendMessage(ChatColor.GRAY + "  +" + crystalManager.format(montant)
                + ChatColor.LIGHT_PURPLE + " 💎 Cristaux des Abymes");
        player.sendMessage(ChatColor.GRAY + "  Solde : "
                + ChatColor.WHITE + crystalManager.format(solde)
                + ChatColor.LIGHT_PURPLE + " 💎");
        player.sendMessage("");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
    }

    // ─────────────────────────────────────────────
    //  PROTECTIONS : la gemme n'est pas une améthyste ordinaire
    // ─────────────────────────────────────────────

    /** Pas de craft avec une gemme (longue-vue, verre teinté...). */
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (gemmeAbyssale.isGem(item)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    /** Pas d'ornement d'armure avec une gemme. */
    @EventHandler
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        for (ItemStack item : event.getInventory().getContents()) {
            if (gemmeAbyssale.isGem(item)) {
                event.setResult(null);
                return;
            }
        }
    }

    /** Pas de gemme donnée à un allay (il la garderait ou la consommerait). */
    @EventHandler(ignoreCancelled = true)
    public void onInteractAllay(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Allay)) return;
        ItemStack item = event.getPlayer().getInventory().getItem(event.getHand());
        if (gemmeAbyssale.isGem(item)) {
            event.setCancelled(true);
        }
    }
}