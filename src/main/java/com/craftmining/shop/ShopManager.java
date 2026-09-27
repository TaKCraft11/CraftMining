package com.craftmining.shop;

import com.craftmining.CraftMining;
import com.craftmining.TokenManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class ShopManager {

    /** Anciens noms d'effets (avant 1.20.5) -> noms actuels, pour rester compatible avec les vieux shop.yml. */
    private static final Map<String, String> LEGACY_EFFECTS = Map.of(
            "fast_digging",      "haste",
            "slow_digging",      "mining_fatigue",
            "damage_resistance", "resistance",
            "increase_damage",   "strength",
            "jump",              "jump_boost",
            "heal",              "instant_health",
            "harm",              "instant_damage",
            "confusion",         "nausea",
            "slow",              "slowness"
    );

    /** Niveau de Résistance donné par le pouvoir Invincibilité (4 = Résistance V). */
    private static final int INVINCIBILITY_LEVEL = 4;

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

    // ─────────────────────────────────────────────
    //  CHARGEMENT
    // ─────────────────────────────────────────────

    public void reload() {
        shopFile = new File(plugin.getDataFolder(), "shop.yml");
        if (!shopFile.exists()) plugin.saveResource("shop.yml", false);
        shopConfig = YamlConfiguration.loadConfiguration(shopFile);
        loadItems();
        validateCrates();
    }

    private void loadItems() {
        items.clear();
        ConfigurationSection section = shopConfig.getConfigurationSection("items");
        if (section == null) {
            plugin.getLogger().warning("shop.yml : section 'items' introuvable.");
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) continue;
            try {
                ShopItem item = parseItem(key, s);
                if (item != null) items.add(item);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Erreur de chargement de l'article '" + key + "'", e);
            }
        }
        plugin.getLogger().info("Shop : " + items.size() + " article(s) chargé(s).");
    }

    private ShopItem parseItem(String id, ConfigurationSection s) {
        String typeStr = s.getString("type", "BUFF").toUpperCase(Locale.ROOT);
        ShopItem.ItemType type;
        try {
            type = ShopItem.ItemType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Type inconnu '" + typeStr + "' pour " + id);
            return null;
        }

        Material icon = Material.matchMaterial(s.getString("icon", "EMERALD"));
        if (icon == null) icon = Material.EMERALD;

        int price = s.getInt("price", 10);
        if (price < 0) {
            plugin.getLogger().warning("Prix négatif pour " + id + " : article ignoré.");
            return null;
        }

        ShopItem.Builder b = new ShopItem.Builder(id)
                .displayName(color(s.getString("name", id)))
                .description(color(s.getString("description", "")))
                .icon(icon)
                .price(price)
                .type(type);

        switch (type) {
            case BUFF -> {
                String effectName = s.getString("effect", "HASTE");
                PotionEffectType effect = resolveEffect(effectName);
                if (effect == null) {
                    plugin.getLogger().warning("Effet inconnu '" + effectName + "' pour " + id);
                    return null;
                }
                b.potionEffect(effect)
                        .buffDuration(s.getInt("duration", 60))
                        .buffAmplifier(s.getInt("amplifier", 0));
            }
            case LOOT_CRATE -> b.crateId(s.getString("crate-id", "default"));
            case COSMETIC   -> b.cosmeticId(s.getString("cosmetic-id", ""));
            case POWER      -> b
                    .powerType(s.getString("power-type", "").toUpperCase(Locale.ROOT))
                    .powerValue(s.getDouble("power-value", 1.25))
                    .powerDuration(s.getInt("power-duration", 120));
            case TOOL -> {
                String toolName = s.getString("tool-material", "DIAMOND_PICKAXE");
                Material tool = Material.matchMaterial(toolName);
                if (tool == null || !tool.isItem()) {
                    plugin.getLogger().warning("Outil inconnu '" + toolName + "' pour " + id);
                    return null;
                }
                b.toolMaterial(tool).enchants(parseEnchants(id, s.getString("enchants", "")));
            }
        }
        return b.build();
    }

    /** Lit une liste du type "FORTUNE:3,EFFICIENCY:4,UNBREAKING:3". */
    private Map<Enchantment, Integer> parseEnchants(String id, String raw) {
        Map<Enchantment, Integer> result = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) return result;

        for (String part : raw.split(",")) {
            String[] kv   = part.trim().split(":");
            String name   = kv[0].trim().toLowerCase(Locale.ROOT);
            Enchantment e = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(name));
            if (e == null) {
                plugin.getLogger().warning("Enchantement inconnu '" + kv[0].trim() + "' pour " + id);
                continue;
            }
            int level = 1;
            if (kv.length > 1) {
                try {
                    level = Integer.parseInt(kv[1].trim());
                } catch (NumberFormatException ex) {
                    plugin.getLogger().warning("Niveau invalide pour '" + kv[0].trim() + "' dans " + id);
                    continue;
                }
            }
            result.put(e, level);
        }
        return result;
    }

    /** Signale dans la console toute récompense de caisse sans commande associée. */
    private void validateCrates() {
        ConfigurationSection crates = shopConfig.getConfigurationSection("crates");
        if (crates == null) return;
        for (String crateId : crates.getKeys(false)) {
            for (String reward : shopConfig.getStringList("crates." + crateId + ".rewards")) {
                if (getRewardCommand(crateId, reward).isEmpty()) {
                    plugin.getLogger().warning("Caisse '" + crateId
                            + "' : aucune commande pour la récompense \"" + reward + "\"");
                }
            }
        }
    }

    // ─────────────────────────────────────────────
    //  ACHAT
    // ─────────────────────────────────────────────

    public enum PurchaseResult {
        /** Achat effectué. */
        SUCCESS,
        /** Pas assez de tokens. */
        INSUFFICIENT_TOKENS,
        /** Article inexistant (conservé pour compatibilité). */
        ITEM_NOT_FOUND,
        /** Plus de place dans l'inventaire. */
        INVENTORY_FULL,
        /** Achat refusé ou annulé : la raison a déjà été envoyée au joueur. */
        CANCELLED
    }

    public PurchaseResult purchase(Player player, ShopItem item) {
        // 1. Toutes les vérifications AVANT de toucher aux tokens
        String unavailable = checkAvailability(player, item);
        if (unavailable != null) {
            player.sendMessage(color(unavailable));
            return PurchaseResult.CANCELLED;
        }
        if (item.getType() == ShopItem.ItemType.TOOL && player.getInventory().firstEmpty() == -1) {
            return PurchaseResult.INVENTORY_FULL;
        }
        if (tokenManager.getTokens(player) < item.getPrice()) {
            return PurchaseResult.INSUFFICIENT_TOKENS;
        }

        // 2. Paiement puis livraison
        tokenManager.removeTokens(player, item.getPrice());

        boolean delivered = switch (item.getType()) {
            case BUFF       -> applyBuff(player, item);
            case LOOT_CRATE -> openCrate(player, item);
            case COSMETIC   -> applyCosmetic(player, item);
            case POWER      -> applyPower(player, item);
            case TOOL       -> giveTool(player, item);
        };

        if (!delivered) {
            // Problème imprévu (config incorrecte...) : on rend les tokens
            tokenManager.addTokens(player, item.getPrice());
            player.sendMessage(color("&7Vos &d" + item.getPrice() + " tokens &7vous ont été rendus."));
            return PurchaseResult.CANCELLED;
        }
        return PurchaseResult.SUCCESS;
    }

    /**
     * Indique si un article peut être acheté maintenant.
     * @return null si l'achat est possible, sinon la raison du refus (message pour le joueur).
     */
    public String checkAvailability(Player player, ShopItem item) {
        return switch (item.getType()) {
            case BUFF -> hasEffectAtLeast(player, item.getPotionEffect(), item.getBuffAmplifier())
                    ? "&e⚠ " + item.getDisplayName() + " &eest déjà actif !"
                    : null;
            case COSMETIC -> item.getCosmeticId().equals(getActivePrefix(player.getUniqueId()))
                    ? "&e⚠ Ce préfixe est déjà le vôtre !"
                    : null;
            case POWER -> switch (item.getPowerType()) {
                case "XP_BOOST" -> hasXpBoost(player.getUniqueId())
                        ? "&e⚠ Un XP Boost est déjà actif !" : null;
                case "INVINCIBILITY" -> hasEffectAtLeast(player, PotionEffectType.RESISTANCE, INVINCIBILITY_LEVEL)
                        ? "&e⚠ Vous êtes déjà invincible !" : null;
                case "FIRE_RESISTANCE" -> hasEffectAtLeast(player, PotionEffectType.FIRE_RESISTANCE, 0)
                        ? "&e⚠ Vous résistez déjà au feu !" : null;
                case "REGEN" -> player.getHealth() >= getMaxHealth(player)
                        ? "&e⚠ Votre vie est déjà au maximum !" : null;
                default -> null;
            };
            default -> null;
        };
    }

    private boolean applyBuff(Player player, ShopItem item) {
        if (item.getPotionEffect() == null) {
            player.sendMessage(color("&cCet effet est mal configuré, contactez le staff."));
            return false;
        }
        player.addPotionEffect(new PotionEffect(
                item.getPotionEffect(),
                item.getBuffDuration() * 20,
                item.getBuffAmplifier(),
                false, true, true));
        player.sendMessage(color("&a✔ Buff &e" + item.getDisplayName()
                + " &aactivé pour &e" + item.getBuffDuration() + "s !"));
        return true;
    }

    private boolean applyPower(Player player, ShopItem item) {
        UUID uuid = player.getUniqueId();
        switch (item.getPowerType()) {
            case "XP_BOOST" -> {
                if (hasXpBoost(uuid)) return false; // sécurité : déjà vérifié avant le paiement
                long expiry = System.currentTimeMillis() + (item.getPowerDuration() * 1000L);
                xpBoostExpiry.put(uuid, expiry);
                xpBoostMult.put(uuid, item.getPowerValue());
                player.sendMessage(color("&a✔ &6XP Boost x" + item.getPowerValue()
                        + " &aactivé pendant &e" + item.getPowerDuration() + "s !"));
                return true;
            }
            case "INVINCIBILITY" -> {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.RESISTANCE,
                        item.getPowerDuration() * 20, INVINCIBILITY_LEVEL, false, true, true));
                player.sendMessage(color("&a✔ &7Invincibilité activée pendant &e"
                        + item.getPowerDuration() + "s !"));
                return true;
            }
            case "REGEN" -> {
                player.setHealth(getMaxHealth(player));
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.REGENERATION, 100, 2, false, true, true));
                player.sendMessage(color("&a✔ &cRégénération instantanée activée !"));
                return true;
            }
            case "FIRE_RESISTANCE" -> {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.FIRE_RESISTANCE,
                        item.getPowerDuration() * 20, 0, false, true, true));
                player.sendMessage(color("&a✔ &6Résistance au feu activée pendant &e"
                        + item.getPowerDuration() + "s !"));
                return true;
            }
            default -> {
                player.sendMessage(color("&cPouvoir inconnu : " + item.getPowerType()));
                return false;
            }
        }
    }

    private boolean openCrate(Player player, ShopItem item) {
        String crateId       = item.getCrateId();
        List<String> rewards = shopConfig.getStringList("crates." + crateId + ".rewards");
        if (rewards.isEmpty()) {
            player.sendMessage(color("&cErreur : la caisse '" + crateId + "' n'est pas configurée."));
            return false;
        }

        String reward = rewards.get(ThreadLocalRandom.current().nextInt(rewards.size()));
        String cmd    = getRewardCommand(crateId, reward);
        if (cmd.isEmpty()) {
            player.sendMessage(color("&cErreur : récompense mal configurée, contactez le staff."));
            plugin.getLogger().warning("Caisse '" + crateId + "' : pas de commande pour \"" + reward + "\"");
            return false;
        }

        player.sendMessage(color("&6Caisse ouverte ! Vous obtenez : &r" + reward));
        plugin.getServer().dispatchCommand(
                plugin.getServer().getConsoleSender(),
                cmd.replace("{player}", player.getName()));
        return true;
    }

    private boolean applyCosmetic(Player player, ShopItem item) {
        activePrefixes.put(player.getUniqueId(), item.getCosmeticId());
        player.sendMessage(color("&a✔ Cosmétique &e" + item.getDisplayName() + " &aactivé !"));
        return true;
    }

    private boolean giveTool(Player player, ShopItem item) {
        ItemStack tool = new ItemStack(item.getToolMaterial());
        tool.addUnsafeEnchantments(item.getEnchants());

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(tool);
        if (!leftover.isEmpty()) {
            player.sendMessage(color("&cVotre inventaire est plein !"));
            return false;
        }
        player.sendMessage(color("&a✔ Vous avez reçu : " + item.getDisplayName() + "&a !"));
        return true;
    }

    // ─────────────────────────────────────────────
    //  XP BOOST / COSMÉTIQUES
    // ─────────────────────────────────────────────

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

    public List<ShopItem>    getItems()                 { return Collections.unmodifiableList(items); }
    public FileConfiguration getShopConfig()            { return shopConfig; }
    public String            getActivePrefix(UUID uuid) { return activePrefixes.getOrDefault(uuid, ""); }

    // ─────────────────────────────────────────────
    //  OUTILS INTERNES
    // ─────────────────────────────────────────────

    private boolean hasEffectAtLeast(Player player, PotionEffectType type, int amplifier) {
        if (type == null) return false;
        PotionEffect current = player.getPotionEffect(type);
        return current != null && current.getAmplifier() >= amplifier;
    }

    private double getMaxHealth(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        return maxHealth != null ? maxHealth.getValue() : 20.0;
    }

    /** Retrouve la commande d'une récompense, en lisant la clé telle quelle (sans interprétation). */
    private String getRewardCommand(String crateId, String reward) {
        ConfigurationSection cmds = shopConfig.getConfigurationSection("crates." + crateId + ".reward-commands");
        if (cmds == null) return "";
        Object value = cmds.getValues(false).get(reward);
        return value instanceof String command ? command : "";
    }

    /** Accepte les noms actuels ("haste") comme les anciens ("FAST_DIGGING"). */
    private PotionEffectType resolveEffect(String name) {
        String key = name.trim().toLowerCase(Locale.ROOT);
        key = LEGACY_EFFECTS.getOrDefault(key, key);
        return Registry.EFFECT.get(NamespacedKey.minecraft(key));
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}