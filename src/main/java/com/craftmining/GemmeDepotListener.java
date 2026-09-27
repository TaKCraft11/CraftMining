package com.craftmining;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (!gemmeAbyssale.isValid(mainHand)) return;

        event.setCancelled(true);

        if (player.isSneaking()) {
            deposerStack(player, mainHand);
        } else {
            deposerUne(player, mainHand);
        }
    }

    private void deposerUne(Player player, ItemStack stack) {
        String uuid = gemmeAbyssale.getUuid(stack);
        if (uuid == null) return;

        if (db.isCrystalItemUsed(uuid)) {
            player.sendMessage(ChatColor.RED + "❌ Cette gemme a déjà été déposée !");
            gemmeAbyssale.marquerUtilisee(stack);
            player.getInventory().setItemInMainHand(null);
            return;
        }

        long valeurTotale = gemmeAbyssale.getValeur(stack);
        if (!db.consumeCrystalItem(uuid, player.getUniqueId(), valeurTotale)) {
            player.sendMessage(ChatColor.RED + "❌ Erreur lors du dépôt. Réessaie.");
            return;
        }

        int qte = stack.getAmount();
        crystalManager.addCrystals(player, GemmeAbyssale.CRISTAUX_PAR_GEMME);

        if (qte == 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            player.getInventory().setItemInMainHand(gemmeAbyssale.create(qte - 1));
        }

        envoyerMessage(player, GemmeAbyssale.CRISTAUX_PAR_GEMME, false);
    }

    private void deposerStack(Player player, ItemStack stack) {
        String uuid = gemmeAbyssale.getUuid(stack);
        if (uuid == null) return;

        if (db.isCrystalItemUsed(uuid)) {
            player.sendMessage(ChatColor.RED + "❌ Cette gemme a déjà été déposée !");
            gemmeAbyssale.marquerUtilisee(stack);
            player.getInventory().setItemInMainHand(null);
            return;
        }

        long valeur = gemmeAbyssale.getValeur(stack);
        if (!db.consumeCrystalItem(uuid, player.getUniqueId(), valeur)) {
            player.sendMessage(ChatColor.RED + "❌ Erreur lors du dépôt. Réessaie.");
            return;
        }

        crystalManager.addCrystals(player, valeur);
        player.getInventory().setItemInMainHand(null);
        envoyerMessage(player, valeur, true);
    }

    private void envoyerMessage(Player player, long montant, boolean stack) {
        long solde = crystalManager.getBalance(player);
        player.sendMessage("");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "✦ " + ChatColor.BOLD
                + (stack ? "Stack déposé !" : "Gemme déposée !"));
        player.sendMessage(ChatColor.GRAY + "  +" + crystalManager.format(montant)
                + ChatColor.LIGHT_PURPLE + " 💎 Cristaux des Abymes");
        player.sendMessage(ChatColor.GRAY + "  Solde : "
                + ChatColor.WHITE + crystalManager.format(solde)
                + ChatColor.LIGHT_PURPLE + " 💎");
        player.sendMessage("");
    }
}