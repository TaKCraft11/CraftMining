# ⛏️ CraftMining

<div align="center">

![Version](https://img.shields.io/badge/version-alpha-orange?style=for-the-badge&logo=openjdk&logoColor=white)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21-62B47A?style=for-the-badge&logo=creativecommons&logoColor=white)
![Paper API](https://img.shields.io/badge/Paper_API-1.21-F7CF0D?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Status](https://img.shields.io/badge/statut-en_développement-blue?style=for-the-badge)

> *« Du charbon à la Netherite — chaque coup de pioche compte. »*

**Plugin Minecraft Java · Serveur Survival · Paper 1.21**

</div>

---

## 🪨 C'est quoi CraftMining ?

CraftMining transforme l'expérience de minage sur les serveurs Minecraft Survival.
Fini le minage bête et répétitif — ici, chaque bloc cassé te rapproche de la légende.

> ⚠️ Plugin en cours de développement actif — les fonctionnalités évoluent régulièrement !

---

## ✨ Fonctionnalités

### ⚡ Niveaux de minage XP
```
[Débutant] ──► [Apprenti] ──► [Mineur] ──► [Expert] ──► [Légendaire]
    0 XP          500 XP       2000 XP      5000 XP       10 000 XP
```
- Gagne de l'XP en minant n'importe quel bloc
- Chaque minerai rapporte plus ou moins d'XP selon sa rareté
- Les niveaux débloquent des bonus passifs (vitesse, fortune, drops...)
- 1000 niveaux t'attendent — jusqu'où iras-tu ?

---

### 🛡️ Anti-cheat intégré
- Détection des miners automatiques (bots, macros)
- Vérification des patterns de minage suspects
- Logs des comportements anormaux
- Conçu pour les serveurs Minecraft Survival compétitifs

---

### 🗺️ Roadmap

| Fonctionnalité | Statut |
|----------------|--------|
| ⚡ Niveaux XP de minage | 🔨 En cours |
| 🛡️ Anti-cheat intégré | 🔨 En cours |
| 💼 Jobs / récompenses | 📋 Prévu |
| 💎 Minerais custom Nether | 📋 Prévu |
| 📊 Leaderboard mineurs | 📋 Prévu |
| ⚙️ Config YAML complète | 📋 Prévu |

---

## 🛠️ Installation

> ⚠️ Pas encore de release publique — plugin en alpha !

```bash
# 1. Clone le repo
git clone https://github.com/TaKCraft11/CraftMining.git

# 2. Compile avec Maven
mvn clean package

# 3. Glisse le .jar dans ton dossier /plugins

# 4. Redémarre ton serveur Paper 1.21
```

**Prérequis :**
- Serveur **Paper 1.21**
- **Java 21**
- Pas de dépendances externes pour l'instant

---

## ⚙️ Configuration

```yaml
# config.yml (à venir)
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
| **Rôle** | Dev Java débutant |
| **Discord** | `takcraft11` |
| **Mail** | TaKCraft11@gmail.com |

> *Débutant Java mais vétéran Minecraft — je construis ce que je connais.*

---

## 🤝 Contribuer

Le plugin est en alpha et tout retour est le bienvenu !

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

**Seul l'auteur (TaKCraft11) est autorisé à :**
- Modifier, distribuer et utiliser ce plugin
- Accorder des exceptions individuelles sur demande

Pour toute demande d'autorisation : `TaKCraft11@gmail.com` · Discord : `takcraft11`

---

<div align="center">

⛏️ *Fait avec passion et beaucoup de diamants cassés*

**Discord : takcraft11**

</div>
