package com.craftmining.shop;

import com.craftmining.CraftMining;
import com.craftmining.TokenManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class ShopManager {

    private final CraftMining  plugin;
    private final TokenManager tokenManager;
    private final List<ShopItem> items = new ArrayList<>();
    private File shopFile;
    private FileConfiguration shopConfig;
    private final Map<UUID, String> activePrefixes = new HashMap<>();

    // XP Boost actifs
    private final Map<UUID, Long>   xpBoostExpiry = new HashMap<>();
    private final Map<UUID, Double> xpBoostMult   = new HashMap<>();

    public ShopManager(CraftMining plugin, TokenManager tokenManager) {
        this.plugin       = plugin;
        this.tokenManager = tokenManager;
        reload();
    }

    public void reload() {
        shopFile   = new File(plugin.getDataFolder(), "shop.yml");
        if (!shopFile.exists()) plugin.saveResource("shop.yml", false);
        shopConfig = YamlConfiguration.loadConfiguration(shopFile);
        loadItems();
    }

    private void loadItems() {
        items.clear();
        ConfigurationSection section = shopConfig.getConfigurationSection("items");
        if (section == null) {
            plugin.getLogger().warning("[CraftMining] shop.yml : section 'items' introuvable.");
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) continue;
            try {
                ShopItem item = parseItem(key, s);
                if (item != null) items.add(item);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "[CraftMining] Erreur chargement article '" + key + "'", e);
            }
        }
        plugin.getLogger().info("[CraftMining] Shop : " + items.size() + " article(s) charge(s).");
    }

    private ShopItem parseItem(String id, ConfigurationSection s) {
        String typeStr = s.getString("type", "BUFF").toUpperCase();
        ShopItem.ItemType type;
        try { type = ShopItem.ItemType.valueOf(typeStr); }
        catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[CraftMining] Type inconnu '" + typeStr + "' pour " + id);
            return null;
        }

        Material icon = Material.matchMaterial(s.getString("icon", "EMERALD").toUpperCase());
        if (icon == null) icon = Material.EMERALD;

        ShopItem.Builder b = new ShopItem.Builder(id)
                .displayName(color(s.getString("name", id)))
                .description(color(s.getString("description", "")))
                .icon(icon)
                .price(s.getInt("price", 10))
                .type(type);

        switch (type) {
            case BUFF -> {
                PotionEffectType pet = PotionEffectType.getByName(
                        s.getString("effect", "HASTE").toUpperCase());
                if (pet == null) pet = PotionEffectType.getByName("HASTE");
                b.potionEffect(pet)
                        .buffDuration(s.getInt("duration", 60))
                        .buffAmplifier(s.getInt("amplifier", 0));
            }
            case LOOT_CRATE -> b.crateId(s.getString("crate-id", "default"));
            case COSMETIC   -> b.cosmeticId(s.getString("cosmetic-id", ""));
            case POWER      -> b
                    .powerType(s.getString("power-type", "").toUpperCase())
                    .powerValue(s.getDouble("power-value", 1.25))
                    .powerDuration(s.getInt("power-duration", 120));
        }
        return b.build();
    }

    public enum PurchaseResult {
        SUCCESS, INSUFFICIENT_TOKENS, ITEM_NOT_FOUND, INVENTORY_FULL
    }

    public PurchaseResult purchase(Player player, ShopItem item) {
        if (tokenManager.getTokens(player) < item.getPrice())
            return PurchaseResult.INSUFFICIENT_TOKENS;

        tokenManager.removeTokens(player, item.getPrice());

        switch (item.getType()) {
            case BUFF       -> applyBuff(player, item);
            case LOOT_CRATE -> openCrate(player, item);
            case COSMETIC   -> applyCosmetic(player, item);
            case POWER      -> applyPower(player, item);
        }
        return PurchaseResult.SUCCESS;
    }

    private void applyBuff(Player player, ShopItem item) {
        if (item.getPotionEffect() == null) return;
        player.addPotionEffect(new PotionEffect(
                item.getPotionEffect(),
                item.getBuffDuration() * 20,
                item.getBuffAmplifier(),
                false, true, true));
        player.sendMessage(color("&a✔ Buff &e" + item.getDisplayName()
                + " &aactive pour &e" + item.getBuffDuration() + "s !"));
    }

    private void applyPower(Player player, ShopItem item) {
        UUID uuid = player.getUniqueId();
        switch (item.getPowerType()) {
            case "XP_BOOST" -> {
                if (hasXpBoost(uuid)) {
                    player.sendMessage(color("&e⚠ Un XP Boost est deja actif !"));
                    tokenManager.addTokens(player, item.getPrice());
                    return;
                }
                long expiry = System.currentTimeMillis() + (item.getPowerDuration() * 1000L);
                xpBoostExpiry.put(uuid, expiry);
                xpBoostMult.put(uuid, item.getPowerValue());
                player.sendMessage(color("&a✔ &6XP Boost x" + item.getPowerValue()
                        + " &aactive pendant &e" + item.getPowerDuration() + "s !"));
            }
            case "INVINCIBILITY" -> {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.getByName("DAMAGE_RESISTANCE"),
                        item.getPowerDuration() * 20, 4, false, true, true));
                player.sendMessage(color("&a✔ &7Invincibilite activee pendant &e"
                        + item.getPowerDuration() + "s !"));
            }
            case "REGEN" -> {
                player.setHealth(player.getMaxHealth());
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.getByName("REGENERATION"),
                        100, 2, false, true, true));
                player.sendMessage(color("&a✔ &cRegénération instantanee activee !"));
            }
            case "FIRE_RESISTANCE" -> {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.getByName("FIRE_RESISTANCE"),
                        item.getPowerDuration() * 20, 0, false, true, true));
                player.sendMessage(color("&a✔ &6Resistance au feu activee pendant &e"
                        + item.getPowerDuration() + "s !"));
            }
            default -> player.sendMessage(color("&cPouvoir inconnu : " + item.getPowerType()));
        }
    }

    private void openCrate(Player player, ShopItem item) {
        String crateId       = item.getCrateId();
        List<String> rewards = shopConfig.getStringList("crates." + crateId + ".rewards");
        if (rewards.isEmpty()) {
            player.sendMessage(color("&cErreur : caisse '" + crateId + "' non configuree.")); return;
        }
        String reward = rewards.get(new Random().nextInt(rewards.size()));
        player.sendMessage(color("&6Caisse ouverte ! Vous obtenez : &e" + color(reward)));
        String cmd = shopConfig.getString(
                "crates." + crateId + ".reward-commands." + sanitize(reward), "");
        if (!cmd.isEmpty()) {
            plugin.getServer().dispatchCommand(
                    plugin.getServer().getConsoleSender(),
                    cmd.replace("{player}", player.getName()));
        }
    }

    private void applyCosmetic(Player player, ShopItem item) {
        activePrefixes.put(player.getUniqueId(), item.getCosmeticId());
        player.sendMessage(color("&a✔ Cosmetique &e" + item.getDisplayName() + " &aactive !"));
    }

    public boolean hasXpBoost(UUID uuid) {
        Long expiry = xpBoostExpiry.get(uuid);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            xpBoostExpiry.remove(uuid);
            xpBoostMult.remove(uuid);
            return false;
        }
        return true;
    }

    public double getXpMultiplier(UUID uuid) {
        if (!hasXpBoost(uuid)) return 1.0;
        return xpBoostMult.getOrDefault(uuid, 1.0);
    }

    public List<ShopItem>    getItems()                { return Collections.unmodifiableList(items); }
    public FileConfiguration getShopConfig()           { return shopConfig; }
    public String            getActivePrefix(UUID uuid){ return activePrefixes.getOrDefault(uuid, ""); }

    private String color(String s)    { return ChatColor.translateAlternateColorCodes('&', s); }
    private String sanitize(String s) { return s.replaceAll("[^a-zA-Z0-9_\\-]", "_"); }
}