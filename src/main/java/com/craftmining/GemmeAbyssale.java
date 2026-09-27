package com.craftmining;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.UUID;

/**
 * La Gemme Abyssale : objet échangeable valant 10 000 cristaux.
 * Chaque retrait crée une pile avec un identifiant unique, enregistré dans le registre des gemmes.
 */
public class GemmeAbyssale {

    public static final long CRISTAUX_PAR_GEMME = 10_000L;

    private static final String KEY_UUID = "gemme_uuid";
    private static final String KEY_USED = "gemme_used"; // ancien système, lu seulement

    private final CraftMining plugin;
    private final NamespacedKey keyUuid;
    private final NamespacedKey keyUsed;

    public GemmeAbyssale(CraftMining plugin) {
        this.plugin  = plugin;
        this.keyUuid = new NamespacedKey(plugin, KEY_UUID);
        this.keyUsed = new NamespacedKey(plugin, KEY_USED);
    }

    /** Crée une pile de gemmes et l'enregistre dans le registre. */
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
                ChatColor.YELLOW + "▶ Clic droit"
                        + ChatColor.GRAY + " → déposer ×1",
                ChatColor.YELLOW + "▶ Sneak + Clic droit"
                        + ChatColor.GRAY + " → déposer la pile",
                ChatColor.DARK_GRAY + "─────────────────────────"
        ));

        // Effet brillant sans afficher l'enchantement
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        // Identifiant anti-dupe
        String uuid = UUID.randomUUID().toString();
        meta.getPersistentDataContainer().set(keyUuid, PersistentDataType.STRING, uuid);
        item.setItemMeta(meta);

        if (!plugin.getDatabaseManager().registerGems(uuid, quantite)) {
            plugin.getLogger().severe("Gemmes créées SANS enregistrement (base indisponible) : " + uuid);
        }
        return item;
    }

    /** L'objet est-il une Gemme Abyssale (valide ou non) ? */
    public boolean isGem(ItemStack item) {
        if (item == null || item.getType() != Material.AMETHYST_SHARD) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(keyUuid, PersistentDataType.STRING);
    }

    /** Gemme utilisable pour un dépôt (pas marquée "utilisée" par l'ancien système). */
    public boolean isValid(ItemStack item) {
        if (!isGem(item)) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        Byte used = pdc.get(keyUsed, PersistentDataType.BYTE);
        return used == null || used == 0;
    }

    public String getUuid(ItemStack item) {
        if (!isValid(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(keyUuid, PersistentDataType.STRING);
    }

    public long getValeur(ItemStack item) {
        if (item == null) return 0L;
        return item.getAmount() * CRISTAUX_PAR_GEMME;
    }
}