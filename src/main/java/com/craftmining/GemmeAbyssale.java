package com.craftmining;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.UUID;

public class GemmeAbyssale {

    public static final long CRISTAUX_PAR_GEMME = 10_000L;

    private static final String KEY_UUID = "gemme_uuid";
    private static final String KEY_USED = "gemme_used";

    private final NamespacedKey keyUuid;
    private final NamespacedKey keyUsed;

    public GemmeAbyssale(CraftMining plugin) {
        this.keyUuid = new NamespacedKey(plugin, KEY_UUID);
        this.keyUsed = new NamespacedKey(plugin, KEY_USED);
    }

    public ItemStack create(int quantite) {
        quantite = Math.max(1, Math.min(64, quantite));
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD, quantite);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD
                + "💎 Gemme Abyssale " + ChatColor.DARK_PURPLE + "(10 000)");

        meta.setLore(Arrays.asList(
                ChatColor.DARK_GRAY + "─────────────────────────",
                ChatColor.GRAY + "Vendable sur "
                        + ChatColor.YELLOW + "/ah"
                        + ChatColor.GRAY + " & "
                        + ChatColor.YELLOW + "/trade",
                ChatColor.DARK_GRAY + "─────────────────────────",
                ChatColor.YELLOW + "▶ Clic droit dans le vide"
                        + ChatColor.GRAY + " → déposer ×1",
                ChatColor.YELLOW + "▶ Sneak + Clic droit"
                        + ChatColor.GRAY + " → déposer le stack",
                ChatColor.DARK_GRAY + "─────────────────────────"
        ));

        // Effet brillant sans afficher l'enchantement
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        // NBT anti-dupe
        meta.getPersistentDataContainer()
                .set(keyUuid, PersistentDataType.STRING, UUID.randomUUID().toString());
        meta.getPersistentDataContainer()
                .set(keyUsed, PersistentDataType.BYTE, (byte) 0);

        item.setItemMeta(meta);
        return item;
    }

    public boolean isValid(ItemStack item) {
        if (item == null || item.getType() != Material.AMETHYST_SHARD) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        if (!meta.getPersistentDataContainer().has(keyUuid, PersistentDataType.STRING))
            return false;
        Byte used = meta.getPersistentDataContainer().get(keyUsed, PersistentDataType.BYTE);
        return used == null || used == 0;
    }

    public String getUuid(ItemStack item) {
        if (!isValid(item)) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(keyUuid, PersistentDataType.STRING);
    }

    public long getValeur(ItemStack item) {
        if (item == null) return 0L;
        return item.getAmount() * CRISTAUX_PAR_GEMME;
    }

    public void marquerUtilisee(ItemStack item) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer()
                .set(keyUsed, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
    }
}