package com.craftmining.shop;

import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

public class ShopItem {

    public enum ItemType {
        BUFF, LOOT_CRATE, COSMETIC, POWER
    }

    private final String     id;
    private final String     displayName;
    private final String     description;
    private final Material   icon;
    private final int        price;
    private final ItemType   type;

    private PotionEffectType potionEffect;
    private int              buffDuration;
    private int              buffAmplifier;
    private String           crateId;
    private String           cosmeticId;
    private String           powerType;
    private double           powerValue;
    private int              powerDuration;

    private ShopItem(Builder b) {
        this.id            = b.id;
        this.displayName   = b.displayName;
        this.description   = b.description;
        this.icon          = b.icon;
        this.price         = b.price;
        this.type          = b.type;
        this.potionEffect  = b.potionEffect;
        this.buffDuration  = b.buffDuration;
        this.buffAmplifier = b.buffAmplifier;
        this.crateId       = b.crateId;
        this.cosmeticId    = b.cosmeticId;
        this.powerType     = b.powerType;
        this.powerValue    = b.powerValue;
        this.powerDuration = b.powerDuration;
    }

    public String          getId()            { return id; }
    public String          getDisplayName()   { return displayName; }
    public String          getDescription()   { return description; }
    public Material        getIcon()          { return icon; }
    public int             getPrice()         { return price; }
    public ItemType        getType()          { return type; }
    public PotionEffectType getPotionEffect() { return potionEffect; }
    public int             getBuffDuration()  { return buffDuration; }
    public int             getBuffAmplifier() { return buffAmplifier; }
    public String          getCrateId()       { return crateId; }
    public String          getCosmeticId()    { return cosmeticId; }
    public String          getPowerType()     { return powerType; }
    public double          getPowerValue()    { return powerValue; }
    public int             getPowerDuration() { return powerDuration; }

    public static class Builder {
        private final String   id;
        private String         displayName   = "Article";
        private String         description   = "";
        private Material       icon          = Material.EMERALD;
        private int            price         = 10;
        private ItemType       type          = ItemType.BUFF;
        private PotionEffectType potionEffect = null;
        private int            buffDuration   = 60;
        private int            buffAmplifier  = 0;
        private String         crateId       = "default";
        private String         cosmeticId    = "";
        private String         powerType     = "";
        private double         powerValue    = 1.0;
        private int            powerDuration  = 60;

        public Builder(String id)                        { this.id = id; }
        public Builder displayName(String v)             { this.displayName = v; return this; }
        public Builder description(String v)             { this.description = v; return this; }
        public Builder icon(Material v)                  { this.icon = v; return this; }
        public Builder price(int v)                      { this.price = v; return this; }
        public Builder type(ItemType v)                  { this.type = v; return this; }
        public Builder potionEffect(PotionEffectType v)  { this.potionEffect = v; return this; }
        public Builder buffDuration(int v)               { this.buffDuration = v; return this; }
        public Builder buffAmplifier(int v)              { this.buffAmplifier = v; return this; }
        public Builder crateId(String v)                 { this.crateId = v; return this; }
        public Builder cosmeticId(String v)              { this.cosmeticId = v; return this; }
        public Builder powerType(String v)               { this.powerType = v; return this; }
        public Builder powerValue(double v)              { this.powerValue = v; return this; }
        public Builder powerDuration(int v)              { this.powerDuration = v; return this; }
        public ShopItem build()                          { return new ShopItem(this); }
    }
}