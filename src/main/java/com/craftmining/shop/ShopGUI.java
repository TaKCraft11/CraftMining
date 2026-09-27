package com.craftmining.shop;

import com.craftmining.CraftMining;
import com.craftmining.TokenManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ShopGUI implements Listener {

    private final CraftMining plugin;
    private final ShopManager shopManager;
    private final TokenManager tokenManager;

    private static final String TITLE = ChatColor.DARK_PURPLE + "⛏ Boutique CraftMining";

    public ShopGUI(CraftMining plugin, ShopManager shopManager, TokenManager tokenManager) {
        this.plugin       = plugin;
        this.shopManager  = shopManager;
        this.tokenManager = tokenManager;
    }

    public void open(Player player) {
        List<ShopItem> items = shopManager.getItems();
        int size = Math.max(9, (int) Math.ceil(items.size() / 9.0) * 9);
        size = Math.min(size, 54);

        Inventory inv = Bukkit.createInventory(null, size, TITLE);

        for (int i = 0; i < items.size() && i < size; i++) {
            inv.setItem(i, makeItemStack(items.get(i), player));
        }

        player.openInventory(inv);
    }

    private ItemStack makeItemStack(ShopItem item, Player player) {
        ItemStack stack = new ItemStack(item.getIcon());
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        meta.setDisplayName(item.getDisplayName());

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + item.getDescription());
        lore.add("");
        lore.add(ChatColor.YELLOW + "Prix : " + ChatColor.LIGHT_PURPLE
                + item.getPrice() + " ✦");
        lore.add(ChatColor.DARK_GRAY + "Votre solde : "
                + tokenManager.getTokens(player) + " ✦");
        lore.add("");
        if (tokenManager.getTokens(player) >= item.getPrice()) {
            lore.add(ChatColor.GREEN + "▶ Cliquez pour acheter");
        } else {
            lore.add(ChatColor.RED + "✗ Tokens insuffisants");
        }

        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals(TITLE)) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        List<ShopItem> items = shopManager.getItems();
        if (slot < 0 || slot >= items.size()) return;

        ShopItem item = items.get(slot);
        ShopManager.PurchaseResult result = shopManager.purchase(player, item);

        switch (result) {
            case SUCCESS ->
                    player.sendMessage(ChatColor.GREEN + "✔ Achat réussi : " + item.getDisplayName());
            case INSUFFICIENT_TOKENS ->
                    player.sendMessage(ChatColor.RED + "✗ Tokens insuffisants !");
            case ITEM_NOT_FOUND ->
                    player.sendMessage(ChatColor.RED + "✗ Article introuvable.");
            case INVENTORY_FULL ->
                    player.sendMessage(ChatColor.RED + "✗ Inventaire plein !");
        }

        // Rafraîchir le GUI
        plugin.getServer().getScheduler().runTask(plugin, () -> open(player));
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().equals(TITLE)) {
            event.setCancelled(true);
        }
    }
}
