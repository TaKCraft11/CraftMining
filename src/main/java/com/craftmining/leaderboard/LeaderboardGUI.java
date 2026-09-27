package com.craftmining.leaderboard;

import com.craftmining.CraftMining;
import com.craftmining.DatabaseManager;
import com.craftmining.DatabaseManager.LeaderboardEntry;
import com.craftmining.DatabaseManager.LeaderboardType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class LeaderboardGUI implements Listener {

    private static final int ROWS      = 6;
    private static final int TOP_LIMIT = 10;

    private static final int SLOT_TAB_LEVEL  = 2;
    private static final int SLOT_TAB_BLOCKS = 4;
    private static final int SLOT_TAB_TOKENS = 6;
    private static final int SLOT_CLOSE      = 49;

    private static final int[] ENTRY_SLOTS = {
            19, 20, 21, 22, 23, 24, 25, 28, 29, 30
    };

    private static final String[] MEDALS = {
            "§6①", "§7②", "§7③", "§e④", "§e⑤",
            "§e⑥", "§e⑦", "§e⑧", "§e⑨", "§e⑩"
    };

    private final CraftMining plugin;
    private final Map<Inventory, UUID>            openGUIs   = new HashMap<>();
    private final Map<Inventory, LeaderboardType> activeTabs = new HashMap<>();

    public LeaderboardGUI(CraftMining plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        open(player, LeaderboardType.LEVEL);
    }

    public void open(Player player, LeaderboardType tab) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<LeaderboardEntry> entries =
                    plugin.getDatabaseManager().getTop(tab, TOP_LIMIT);
            Bukkit.getScheduler().runTask(plugin, () -> {
                Inventory inv = buildInventory(tab, entries, player);
                openGUIs.put(inv, player.getUniqueId());
                activeTabs.put(inv, tab);
                player.openInventory(inv);
            });
        });
    }

    private Inventory buildInventory(LeaderboardType tab,
                                     List<LeaderboardEntry> entries,
                                     Player viewer) {
        String title = ChatColor.DARK_GRAY + "⛏ " + ChatColor.GOLD + "Classement — " + tabLabel(tab);
        Inventory inv = Bukkit.createInventory(null, ROWS * 9, title);

        fillBorder(inv);

        inv.setItem(SLOT_TAB_LEVEL,  makeTab(LeaderboardType.LEVEL, tab));
        inv.setItem(SLOT_TAB_BLOCKS, makeTab(LeaderboardType.BLOCKS_MINED, tab));
        inv.setItem(SLOT_TAB_TOKENS, makeTab(LeaderboardType.TOKENS, tab));
        inv.setItem(4, makeTitleItem(tab));

        ItemStack sep = makeGlass(Material.BLACK_STAINED_GLASS_PANE);
        for (int col = 1; col <= 7; col++) inv.setItem(9 + col, sep);

        for (int i = 0; i < Math.min(entries.size(), ENTRY_SLOTS.length); i++)
            inv.setItem(ENTRY_SLOTS[i], makeEntryItem(entries.get(i), tab, viewer));

        ItemStack filler = makeGlass(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = entries.size(); i < ENTRY_SLOTS.length; i++)
            inv.setItem(ENTRY_SLOTS[i], filler);

        inv.setItem(31, makeViewerItem(viewer));
        inv.setItem(SLOT_CLOSE, makeCloseButton());

        return inv;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        if (!openGUIs.containsKey(inv)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getRawSlot();

        if (slot == SLOT_CLOSE) { player.closeInventory(); return; }

        LeaderboardType currentTab = activeTabs.get(inv);
        LeaderboardType newTab     = null;

        if (slot == SLOT_TAB_LEVEL  && currentTab != LeaderboardType.LEVEL)
            newTab = LeaderboardType.LEVEL;
        else if (slot == SLOT_TAB_BLOCKS && currentTab != LeaderboardType.BLOCKS_MINED)
            newTab = LeaderboardType.BLOCKS_MINED;
        else if (slot == SLOT_TAB_TOKENS && currentTab != LeaderboardType.TOKENS)
            newTab = LeaderboardType.TOKENS;

        if (newTab != null) {
            player.closeInventory();
            open(player, newTab);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        openGUIs.remove(inv);
        activeTabs.remove(inv);
    }

    private ItemStack makeTab(LeaderboardType type, LeaderboardType activeTab) {
        boolean active = type == activeTab;
        Material mat   = active ? tabMaterialActive(type) : tabMaterialInactive(type);
        ItemStack stack = new ItemStack(mat);
        ItemMeta  meta  = stack.getItemMeta();
        if (meta == null) return stack;
        meta.setDisplayName(color((active ? "&e▶ " : "&7") + tabLabel(type)));
        meta.setLore(List.of(color(active ? "&aOnglet actif" : "&7Cliquer pour voir")));
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack makeTitleItem(LeaderboardType tab) {
        ItemStack stack = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta  meta  = stack.getItemMeta();
        if (meta == null) return stack;
        meta.setDisplayName(color("&6⛏ Top " + TOP_LIMIT + " — " + tabLabel(tab)));
        meta.setLore(List.of(color("&7Classement mis à jour en temps réel.")));
        stack.setItemMeta(meta);
        return stack;
    }

    @SuppressWarnings("deprecation")
    private ItemStack makeEntryItem(LeaderboardEntry entry, LeaderboardType tab, Player viewer) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta  = (SkullMeta) skull.getItemMeta();
        if (meta == null) return skull;

        meta.setOwningPlayer(Bukkit.getOfflinePlayer(entry.uuid()));
        String medal    = entry.rank() <= MEDALS.length ? MEDALS[entry.rank() - 1] : "&7#" + entry.rank();
        boolean isViewer = entry.uuid().equals(viewer.getUniqueId());

        meta.setDisplayName(color(medal + " &f" + entry.username()
                + (isViewer ? " &8(vous)" : "")));

        List<String> lore = new ArrayList<>();
        lore.add(color("&7Niveau  : &a" + entry.level()));
        lore.add(color("&7XP      : &b" + entry.xp()));
        lore.add(color("&7Blocs   : &e" + entry.blocksMined()));
        lore.add(color("&7Tokens  : &d" + entry.tokens() + " ✦"));
        lore.add("");
        lore.add(switch (tab) {
            case LEVEL        -> color("&a▶ Niveau : " + entry.level());
            case BLOCKS_MINED -> color("&e▶ Blocs : " + entry.blocksMined());
            case TOKENS       -> color("&d▶ Tokens : " + entry.tokens() + " ✦");
            default           -> "";
        });

        meta.setLore(lore);
        skull.setItemMeta(meta);
        return skull;
    }

    private ItemStack makeViewerItem(Player viewer) {
        var data  = plugin.getPlayerDataManager().getPlayerData(viewer.getUniqueId());
        ItemStack stack = new ItemStack(Material.COMPASS);
        ItemMeta  meta  = stack.getItemMeta();
        if (meta == null) return stack;
        meta.setDisplayName(color("&b✦ Votre position"));
        meta.setLore(List.of(
                color("&7Niveau  : &a" + data.getLevel()),
                color("&7Blocs   : &e" + data.getBlocksMined()),
                color("&7Tokens  : &d" + data.getTokens() + " ✦")
        ));
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack makeCloseButton() {
        ItemStack stack = new ItemStack(Material.BARRIER);
        ItemMeta  meta  = stack.getItemMeta();
        if (meta != null) { meta.setDisplayName(color("&cFermer")); stack.setItemMeta(meta); }
        return stack;
    }

    private void fillBorder(Inventory inv) {
        ItemStack glass = makeGlass(Material.GRAY_STAINED_GLASS_PANE);
        int size = inv.getSize();
        for (int i = 0; i < size; i++) {
            int row = i / 9, col = i % 9;
            if (row == 0 || row == ROWS - 1 || col == 0 || col == 8)
                inv.setItem(i, glass);
        }
    }

    private ItemStack makeGlass(Material mat) {
        ItemStack g = new ItemStack(mat);
        ItemMeta  m = g.getItemMeta();
        if (m != null) { m.setDisplayName(" "); g.setItemMeta(m); }
        return g;
    }

    private String tabLabel(LeaderboardType type) {
        return switch (type) {
            case LEVEL        -> "Niveau 🏆";
            case BLOCKS_MINED -> "Blocs minés ⛏";
            case TOKENS       -> "Tokens ✦";
            default           -> "XP";
        };
    }

    private Material tabMaterialActive(LeaderboardType type) {
        return switch (type) {
            case LEVEL        -> Material.LIME_STAINED_GLASS_PANE;
            case BLOCKS_MINED -> Material.YELLOW_STAINED_GLASS_PANE;
            case TOKENS       -> Material.PURPLE_STAINED_GLASS_PANE;
            default           -> Material.WHITE_STAINED_GLASS_PANE;
        };
    }

    private Material tabMaterialInactive(LeaderboardType type) {
        return switch (type) {
            case LEVEL        -> Material.GREEN_STAINED_GLASS_PANE;
            case BLOCKS_MINED -> Material.ORANGE_STAINED_GLASS_PANE;
            case TOKENS       -> Material.MAGENTA_STAINED_GLASS_PANE;
            default           -> Material.GRAY_STAINED_GLASS_PANE;
        };
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }
}