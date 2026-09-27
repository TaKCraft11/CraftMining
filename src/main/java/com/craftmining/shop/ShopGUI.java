package com.craftmining.shop;

import com.craftmining.CraftMining;
import com.craftmining.TokenManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ShopGUI implements Listener {

    private static final String TITLE = ChatColor.DARK_PURPLE + "⛏ Boutique CraftMining";

    /** Délai minimum entre deux achats (anti double-clic). */
    private static final long CLICK_COOLDOWN_MS = 300;

    private final CraftMining  plugin;
    private final ShopManager  shopManager;
    private final TokenManager tokenManager;
    private final Map<UUID, Long> lastClick = new HashMap<>();

    /**
     * Marqueur de notre menu : permet de le reconnaître sans se fier au titre,
     * et garde la liste d'articles affichée (fiable même après un /cm reload).
     */
    private static final class ShopHolder implements InventoryHolder {
        private Inventory inventory;
        private List<ShopItem> items = List.of();

        @Override
        public Inventory getInventory() { return inventory; }
    }

    public ShopGUI(CraftMining plugin, ShopManager shopManager, TokenManager tokenManager) {
        this.plugin       = plugin;
        this.shopManager  = shopManager;
        this.tokenManager = tokenManager;
    }

    // ─────────────────────────────────────────────
    //  AFFICHAGE
    // ─────────────────────────────────────────────

    public void open(Player player) {
        int count = shopManager.getItems().size();
        int size  = Math.min(54, Math.max(9, (int) Math.ceil(count / 9.0) * 9));

        ShopHolder holder = new ShopHolder();
        Inventory inv = Bukkit.createInventory(holder, size, TITLE);
        holder.inventory = inv;

        fill(holder, player);
        player.openInventory(inv);
    }

    /** (Re)remplit le menu avec les articles et le solde à jour, sans le refermer. */
    private void fill(ShopHolder holder, Player player) {
        Inventory inv = holder.inventory;
        holder.items  = List.copyOf(shopManager.getItems());

        inv.clear();
        for (int i = 0; i < holder.items.size() && i < inv.getSize(); i++) {
            inv.setItem(i, makeItemStack(holder.items.get(i), player));
        }
    }

    private ItemStack makeItemStack(ShopItem item, Player player) {
        ItemStack stack = new ItemStack(item.getIcon());
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        int balance = tokenManager.getTokens(player);

        meta.setDisplayName(item.getDisplayName());

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + item.getDescription());
        lore.add("");
        lore.add(ChatColor.YELLOW + "Prix : " + ChatColor.LIGHT_PURPLE + item.getPrice() + " ✦");
        lore.add(ChatColor.DARK_GRAY + "Votre solde : " + balance + " ✦");
        lore.add("");
        if (shopManager.checkAvailability(player, item) != null) {
            lore.add(ChatColor.GOLD + "✗ Indisponible pour le moment");
        } else if (balance >= item.getPrice()) {
            lore.add(ChatColor.GREEN + "▶ Cliquez pour acheter");
        } else {
            lore.add(ChatColor.RED + "✗ Tokens insuffisants");
        }

        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    // ─────────────────────────────────────────────
    //  CLICS
    // ─────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ShopHolder holder)) return;
        event.setCancelled(true); // rien ne peut être pris ou déplacé dans le menu
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Seulement les clics dans la boutique (pas dans l'inventaire du joueur en dessous)
        if (event.getClickedInventory() != event.getInventory()) return;

        int slot = event.getSlot();
        if (slot < 0 || slot >= holder.items.size()) return;

        // Anti double-clic
        long now  = System.currentTimeMillis();
        Long last = lastClick.get(player.getUniqueId());
        if (last != null && now - last < CLICK_COOLDOWN_MS) return;
        lastClick.put(player.getUniqueId(), now);

        ShopItem item = holder.items.get(slot);
        switch (shopManager.purchase(player, item)) {
            case SUCCESS             -> playSuccess(player);
            case INSUFFICIENT_TOKENS -> fail(player, "&c✗ Tokens insuffisants !");
            case INVENTORY_FULL      -> fail(player, "&c✗ Inventaire plein !");
            case ITEM_NOT_FOUND      -> fail(player, "&c✗ Article introuvable.");
            case CANCELLED           -> playFail(player); // la raison a déjà été envoyée
        }

        // Mise à jour du menu (solde, disponibilité) au tick suivant
        plugin.getServer().getScheduler().runTask(plugin, () -> fill(holder, player));
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ShopHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof ShopHolder) {
            lastClick.remove(event.getPlayer().getUniqueId());
        }
    }

    // ─────────────────────────────────────────────
    //  RETOURS JOUEUR
    // ─────────────────────────────────────────────

    private void fail(Player player, String message) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        playFail(player);
    }

    private void playSuccess(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
    }

    private void playFail(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
    }
}