# ⛏️ CraftMining

<div align="center">

![Version](https://img.shields.io/badge/version-5.0.0-brightgreen?style=for-the-badge&logo=openjdk&logoColor=white)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8-62B47A?style=for-the-badge&logo=creativecommons&logoColor=white)
![Paper API](https://img.shields.io/badge/Paper_API-1.21-F7CF0D?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![SQLite](https://img.shields.io/badge/SQLite-003B57?style=for-the-badge&logo=sqlite&logoColor=white)
![Status](https://img.shields.io/badge/statut-stable-brightgreen?style=for-the-badge)

> *« Du charbon à la Netherite — chaque coup de pioche compte. »*

**Plugin Minecraft Java · Paper 1.21.8 · SQLite · v5.0.0**

</div>

---

## 💎 C'est quoi CraftMining ?

CraftMining transforme complètement l'expérience de minage sur les serveurs Minecraft Survival.
Tokens, cristaux, gemmes abyssales, shop, classement — chaque bloc cassé te rapproche de la légende.

---

## ✨ Fonctionnalités

### ⚡ Niveaux de minage XP & Tokens
```
[Débutant] ──► [Apprenti] ──► [Mineur] ──► [Expert] ──► [Légendaire]
    0 XP          500 XP       2000 XP      5000 XP       10 000 XP
```
- Gagne des tokens en minant et en utilisant les fourneaux
- Chaque minerai rapporte plus ou moins selon sa rareté
- Barre de progression visuelle en temps réel
- Auto-save des données joueur via SQLite

---

### 💎 Cristaux & Gemmes Abyssales
- Système de cristaux uniques obtenables en minant
- Gemmes Abyssales — objets rares avec comportements spéciaux
- Dépôt de gemmes avec gestion avancée

---

### 🛒 Shop avec GUI
- Interface graphique complète pour dépenser ses tokens
- Catalogue d'items configurables via `shop.yml`
- Système de balance et retrait intégré

---

### 🏆 Classement (Leaderboard)
- Interface graphique du classement des meilleurs mineurs
- Top joueurs consultable avec `/cm top`
- Mise à jour en temps réel

---

### 🛡️ Anti-cheat intégré
- Détection des blocs placés puis reminés (BlockTracker)
- Protection contre les farms automatiques
- Logs des comportements suspects

---

### 🗺️ Roadmap

| Fonctionnalité | Statut |
|----------------|--------|
| ⚡ Tokens & XP de minage | ✅ Terminé |
| 🔥 Tokens via fourneaux | ✅ Terminé |
| 📊 Barre de progression | ✅ Terminé |
| 🗄️ Base de données SQLite | ✅ Terminé |
| 💾 Auto-save joueurs | ✅ Terminé |
| 🛒 Shop avec GUI | ✅ Terminé |
| 🏆 Leaderboard avec GUI | ✅ Terminé |
| 💎 Système de cristaux | ✅ Terminé |
| 🌑 Gemmes Abyssales | ✅ Terminé |
| 🛡️ Anti-cheat BlockTracker | ✅ Terminé |
| ⚙️ Config YAML complète | ✅ Terminé |
| 🌍 Support multi-serveur | 📋 Prévu |

---

## 🛠️ Installation

```bash
# 1. Télécharge le .jar depuis les Releases GitHub
# 2. Glisse le .jar dans ton dossier /plugins
# 3. Redémarre ton serveur Paper 1.21
# 4. Configure plugins/CraftMining/config.yml et shop.yml
```

**Prérequis :**
- Serveur **Paper 1.21.8**
- **Java 21**
- Pas de dépendances externes

---

## 📋 Commandes

| Commande | Description | Permission |
|----------|-------------|------------|
| `/cm stats` | Voir ses statistiques de minage | `craftmining.use` |
| `/cm balance` | Voir son solde de tokens | `craftmining.use` |
| `/cm retrait` | Retirer ses tokens | `craftmining.use` |
| `/cm shop` | Ouvrir le shop | `craftmining.use` |
| `/cm top` | Voir le classement | `craftmining.top` |
| `/cm tokens` | Gestion des tokens | `craftmining.use` |
| `/cm reload` | Recharger la config | `craftmining.admin` |

**Aliases :** `/cm` · `/mining`

---

## 🔐 Permissions

| Permission | Description | Défaut |
|------------|-------------|--------|
| `craftmining.use` | Accès aux commandes de base | Tous |
| `craftmining.top` | Voir le classement | Tous |
| `craftmining.admin` | Commandes admin (reload, gestion) | OP |

---

## ⚙️ Configuration

```yaml
# config.yml — personnalisable selon ton serveur
craftmining:
  xp:
    coal: 10
    iron: 25
    gold: 40
    diamond: 100
    netherite: 250
  anticheat:
    enabled: true
    sensitivity: medium
  levels:
    max: 1000
```

---

## 👨‍💻 Auteur

| | |
|--|--|
| **Pseudo** | TaKCraft11 (Cyrill) |
| **Rôle** | Dev Java · Créateur de CraftMining |
| **Discord** | `takcraft11` |
| **Mail** | TaKCraft11@gmail.com |

> *Débutant Java mais vétéran Minecraft — je construis ce que je connais.*

---

## 🤝 Contribuer

- 🐛 **Un bug ?** Ouvre une [Issue](https://github.com/TaKCraft11/CraftMining/issues)
- 💡 **Une idée ?** Propose-la en discussion
- 🎮 **Tu veux tester ?** Lance un serveur Paper local et installe le .jar !

---

## 🔒 Licence

**© 2026 TaKCraft11 (Cyrill) — Tous droits réservés.**

Ce projet est sous licence propriétaire. Le code source est partagé à titre de transparence uniquement.

**Il est strictement interdit de :**
- Copier, modifier ou redistribuer ce code sans autorisation écrite
- Utiliser ce plugin ou des parties de son code dans un autre projet
- Revendre ou distribuer ce plugin sous quelque forme que ce soit

Pour toute demande d'autorisation : `TaKCraft11@gmail.com` · Discord : `takcraft11`

---

<div align="center">

⛏️ *Fait avec passion et beaucoup de diamants cassés*

**Discord : takcraft11**

</div>
