package com.craftmining;

import com.craftmining.leaderboard.LeaderboardGUI;
import com.craftmining.shop.ShopGUI;
import com.craftmining.shop.ShopManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class CraftMiningCommand implements CommandExecutor, TabCompleter {

    private final CraftMining        plugin;
    private final TokenManager       tokenManager;
    private final ProgressBarManager progressBar;
    private final ShopManager        shopManager;
    private final ShopGUI            shopGUI;
    private final LeaderboardGUI     leaderboardGUI;
    private final CrystalManager     crystalManager;
    private final GemmeAbyssale      gemmeAbyssale;

    public CraftMiningCommand(CraftMining plugin, TokenManager tokenManager,
                              ProgressBarManager progressBar,
                              ShopManager shopManager, ShopGUI shopGUI,
                              LeaderboardGUI leaderboardGUI,
                              CrystalManager crystalManager,
                              GemmeAbyssale gemmeAbyssale) {
        this.plugin         = plugin;
        this.tokenManager   = tokenManager;
        this.progressBar    = progressBar;
        this.shopManager    = shopManager;
        this.shopGUI        = shopGUI;
        this.leaderboardGUI = leaderboardGUI;
        this.crystalManager = crystalManager;
        this.gemmeAbyssale  = gemmeAbyssale;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }
        switch (args[0].toLowerCase()) {
            case "stats"    -> handleStats(sender, args);
            case "reload"   -> handleReload(sender);
            case "tokens"   -> handleTokens(sender, args);
            case "shop"     -> handleShop(sender);
            case "top"      -> handleTop(sender, args);
            case "balance"  -> handleBalance(sender);
            case "retrait"  -> handleRetrait(sender, args);
            default         -> sendHelp(sender);
        }
        return true;
    }

    // ─────────────────────────────────────────────
    //  BALANCE
    // ─────────────────────────────────────────────

    private void handleBalance(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cCommande réservée aux joueurs.")); return;
        }
        long solde = crystalManager.getBalance(player);
        player.sendMessage(color(""));
        player.sendMessage(color("&5✦ &dCristaux des Abymes"));
        player.sendMessage(color("  &7Solde : &f" + crystalManager.format(solde) + " &d💎"));
        player.sendMessage(color("  &8Retrait : /cm retrait <montant>"));
        player.sendMessage(color(""));
    }

    // ─────────────────────────────────────────────
    //  RETRAIT
    // ─────────────────────────────────────────────

    private void handleRetrait(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cCommande réservée aux joueurs.")); return;
        }
        if (args.length < 2) {
            player.sendMessage(color("&cUsage : /cm retrait <montant>"));
            player.sendMessage(color("&7Le montant doit être un multiple de 10 000."));
            return;
        }

        long demande;
        try {
            demande = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(color("&cMontant invalide : " + args[1]));
            return;
        }

        if (demande <= 0) {
            player.sendMessage(color("&cLe montant doit être positif.")); return;
        }

        // Arrondir au multiple de 10 000
        long parGemme = GemmeAbyssale.CRISTAUX_PAR_GEMME;
        if (demande % parGemme != 0) {
            demande = (demande / parGemme) * parGemme;
            if (demande <= 0) {
                player.sendMessage(color("&cMinimum : " + parGemme + " cristaux.")); return;
            }
            player.sendMessage(color("&eArrondi à " + crystalManager.format(demande) + " cristaux."));
        }

        int nbGemmes = (int) (demande / parGemme);
        if (nbGemmes > 64) {
            player.sendMessage(color("&cMaximum 64 gemmes par retrait."));
            nbGemmes = 64;
            demande  = nbGemmes * parGemme;
        }

        if (!crystalManager.hasCrystals(player, demande)) {
            player.sendMessage(color("&c❌ Solde insuffisant. Tu as "
                    + crystalManager.format(crystalManager.getBalance(player))
                    + " 💎"));
            return;
        }

        // Vérifier inventaire
        int slotsLibres = 0;
        for (ItemStack s : player.getInventory().getStorageContents()) {
            if (s == null || s.getType().isAir()) slotsLibres++;
        }
        if (slotsLibres == 0) {
            player.sendMessage(color("&c❌ Inventaire plein !")); return;
        }

        crystalManager.removeCrystals(player, demande);
        player.getInventory().addItem(gemmeAbyssale.create(nbGemmes));

        player.sendMessage(color(""));
        player.sendMessage(color("&5✦ &dRetrait effectué !"));
        player.sendMessage(color("  &7-" + crystalManager.format(demande) + " &d💎"));
        player.sendMessage(color("  &7Solde restant : &f"
                + crystalManager.format(crystalManager.getBalance(player)) + " &d💎"));
        player.sendMessage(color("  &8Vendable sur &e/ah &8et &e/trade"));
        player.sendMessage(color(""));
    }

    // ─────────────────────────────────────────────
    //  COMMANDES EXISTANTES
    // ─────────────────────────────────────────────

    private void handleTop(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cCommande réservée aux joueurs.")); return;
        }
        if (!player.hasPermission("craftmining.top")) {
            player.sendMessage(color("&cPermission insuffisante.")); return;
        }
        DatabaseManager.LeaderboardType tab = DatabaseManager.LeaderboardType.LEVEL;
        if (args.length >= 2) {
            tab = switch (args[1].toLowerCase()) {
                case "blocs", "blocks" -> DatabaseManager.LeaderboardType.BLOCKS_MINED;
                case "tokens"          -> DatabaseManager.LeaderboardType.TOKENS;
                default                -> DatabaseManager.LeaderboardType.LEVEL;
            };
        }
        leaderboardGUI.open(player, tab);
    }

    private void handleShop(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cCommande réservée aux joueurs.")); return;
        }
        shopGUI.open(player);
    }

    private void handleStats(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            if (!sender.hasPermission("craftmining.admin")) {
                sender.sendMessage(color("&cPermission insuffisante.")); return;
            }
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(color("&cJoueur introuvable : &e" + args[1])); return;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Usage: /craftmining stats <joueur>"); return;
            }
            target = (Player) sender;
        }

        PlayerData data      = plugin.getPlayerDataManager().getPlayerData(target.getUniqueId());
        int xpForNext        = ProgressBarManager.getXpForLevel(data.getLevel() + 1);
        int xpForCurrent     = ProgressBarManager.getXpForLevel(data.getLevel());
        int xpIntoLevel      = data.getXp() - xpForCurrent;
        int xpNeeded         = xpForNext - xpForCurrent;

        sender.sendMessage(color(""));
        sender.sendMessage(color("&8&m-----------------------------"));
        sender.sendMessage(color("  &6⛏ &eCraftMining &7— &b" + target.getName()));
        sender.sendMessage(color("&8&m-----------------------------"));
        sender.sendMessage(color("  &7Niveau       : &a" + data.getLevel()));
        sender.sendMessage(color("  &7XP           : &b" + xpIntoLevel + " &7/ &b" + xpNeeded));
        sender.sendMessage(color("  &7Tokens       : &d" + data.getTokens() + " ✦"));
        sender.sendMessage(color("  &7Cristaux     : &5" + crystalManager.format(data.getCrystals()) + " 💎"));
        sender.sendMessage(color("  &7Blocs minés  : &e" + data.getBlocksMined()));
        sender.sendMessage(color("  &7Items fondus : &e" + data.getItemsSmelted()));
        sender.sendMessage(color("&8&m-----------------------------"));
        sender.sendMessage(color(""));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("craftmining.admin")) {
            sender.sendMessage(color("&cPermission insuffisante.")); return;
        }
        plugin.reloadConfig();
        tokenManager.reload();
        shopManager.reload();
        sender.sendMessage(color("&a✔ CraftMining — config + shop rechargés."));
    }

    private void handleTokens(CommandSender sender, String[] args) {
        if (!sender.hasPermission("craftmining.admin")) {
            sender.sendMessage(color("&cPermission insuffisante.")); return;
        }
        if (args.length < 4) {
            sender.sendMessage(color("&cUsage : /craftmining tokens <give|take|set> <joueur> <montant>")); return;
        }
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(color("&cJoueur introuvable : &e" + args[2])); return;
        }
        int amount;
        try { amount = Integer.parseInt(args[3]); if (amount < 0) throw new NumberFormatException(); }
        catch (NumberFormatException e) { sender.sendMessage(color("&cMontant invalide.")); return; }

        switch (args[1].toLowerCase()) {
            case "give" -> {
                tokenManager.addTokens(target, amount);
                sender.sendMessage(color("&a✔ &d+" + amount + " ✦ &7→ &e" + target.getName()));
                target.sendMessage(color("&7Vous recevez &d+" + amount + " tokens ✦ &7(admin)."));
            }
            case "take" -> {
                if (!tokenManager.removeTokens(target, amount))
                    sender.sendMessage(color("&cSolde insuffisant (" + tokenManager.getTokens(target) + " ✦)."));
                else {
                    sender.sendMessage(color("&a✔ &c-" + amount + " ✦ &7→ &e" + target.getName()));
                    target.sendMessage(color("&c-" + amount + " tokens ✦ &7retirés (admin)."));
                }
            }
            case "set" -> {
                tokenManager.setTokens(target, amount);
                sender.sendMessage(color("&a✔ Tokens de &e" + target.getName() + " &7= &d" + amount + " ✦"));
            }
            default -> sender.sendMessage(color("&cAction inconnue : give / take / set"));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(color(""));
        sender.sendMessage(color("&6⛏ CraftMining &7— Aide"));
        sender.sendMessage(color("  &b/cm stats &8[joueur]"));
        sender.sendMessage(color("  &b/cm balance"));
        sender.sendMessage(color("  &b/cm retrait &8<montant>"));
        sender.sendMessage(color("  &b/cm shop"));
        sender.sendMessage(color("  &b/cm top &8[level|blocs|tokens]"));
        if (sender.hasPermission("craftmining.admin")) {
            sender.sendMessage(color("  &b/cm reload"));
            sender.sendMessage(color("  &b/cm tokens give|take|set &8<joueur> <montant>"));
        }
        sender.sendMessage(color(""));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1)
            return filter(List.of("stats", "reload", "tokens", "shop", "top", "balance", "retrait"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("top"))
            return filter(List.of("level", "blocs", "tokens"), args[1]);
        if (args.length == 2 && args[0].equalsIgnoreCase("stats"))
            return onlinePlayers(args[1]);
        if (args.length == 2 && args[0].equalsIgnoreCase("tokens"))
            return filter(List.of("give", "take", "set"), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("tokens"))
            return onlinePlayers(args[2]);
        if (args.length == 4 && args[0].equalsIgnoreCase("tokens"))
            return filter(List.of("1", "10", "50", "100"), args[3]);
        if (args.length == 2 && args[0].equalsIgnoreCase("retrait"))
            return filter(List.of("10000", "50000", "100000", "500000"), args[1]);
        return Collections.emptyList();
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }
    private List<String> filter(List<String> opts, String prefix) {
        return opts.stream().filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase())).collect(Collectors.toList());
    }
    private List<String> onlinePlayers(String prefix) {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(prefix.toLowerCase())).collect(Collectors.toList());
    }
}