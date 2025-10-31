# PremiumAuthBypass

**Bypass premium basé sur l'IP (opt‑in) pour AuthMe Reloaded**

Un plugin additionnel pour AuthMe Reloaded qui permet aux joueurs **d'accepter** d'enregistrer leur IP après une connexion réussie. Si le joueur se reconnecte depuis la même IP, le plugin appelle automatiquement `AuthMe#forceLogin` pour le connecter sans resaisie du mot de passe.

> ⚠️ Ce système est **optionnel** (opt‑in) — le joueur doit explicitement exécuter `/premiumbypass accept` après sa première connexion pour enregistrer son IP.

---

## Table des matières

* [Présentation](#présentation)
* [Fonctionnalités](#fonctionnalit%C3%A9s)
* [Flux de fonctionnement](#flux-de-fonctionnement)
* [Installation](#installation)
* [Configuration (exemple)](#configuration-exemple)
* [Commandes](#commandes)
* [Développement & Compilation](#d%C3%A9veloppement--compilation)
* [Sécurité & vie privée](#s%C3%A9curit%C3%A9--vie-priv%C3%A9)
* [Contribuer](#contribuer)
* [Licence](#licence)

---

## Présentation

PremiumAuthBypass permet d'améliorer l'expérience utilisateur sur les serveurs où AuthMe est utilisé en proposant un mécanisme simple de "connexion automatique" basé sur l'adresse IP enregistrée du joueur. Il est pensé pour les serveurs privés ou semi‑privés où l'adresse IP est un index de confiance raisonnable.

---

## Fonctionnalités

* Enregistrement opt‑in de l'IP du joueur après une authentification réussie.
* Bypass automatique (appel à `forceLogin`) si l'IP du joueur correspond à l'IP enregistrée.
* Gestion simple des identifiants IP côté plugin.
* Support basique des noms "Bedrock‑like" (noms commençant par `_`) — traités comme les autres noms.

---

## Flux de fonctionnement

1. Le joueur se connecte normalement et s'authentifie via AuthMe (`/login`).
2. Après la première connexion réussie, le plugin propose au joueur d'exécuter `/premiumbypass accept` pour enregistrer son IP actuelle.
3. Lorsque le joueur revient, si son IP actuelle correspond à l'IP stockée, le plugin appelle `AuthMe.forceLogin(player)` et le connecte automatiquement.
4. Si l'IP change, le joueur doit se réauthentifier et exécuter à nouveau `/premiumbypass accept`.

---

## Installation

1. Téléchargez la version compilée du plugin (fichier JAR) depuis les Releases GitHub.
2. Placez `PremiumAuthBypass.jar` dans le dossier `plugins/` de votre serveur Minecraft.
3. Redémarrez le serveur.

**Note** : AuthMe Reloaded doit être installé et fonctionnel sur votre serveur pour que ce plugin fonctionne.

---

## Configuration (exemple)

Le plugin peut stocker les IPs dans un fichier interne (format JSON/YAML selon l'implémentation). Exemple d'entrée possible :

```yaml
# premiumbypass.yml (exemple)
akaknoyw:
  prompted: true
  ip: 127.0.0.1
  ips:
  - 127.0.0.1
  - 192.168.1.254
```

> Ajustez la configuration selon vos besoins et votre politique de sécurité.

---

## Commandes

* `/premiumbypass accept` — Enregistre l'IP actuelle du joueur pour le bypass à l'avenir.
* `/premiumbypass remove` — Supprime l'enregistrement IP du joueur.
* `/premiumbypass status` — Affiche l'état actuel (IP enregistrée, date, etc.).

> Ces commandes peuvent nécessiter des permissions définies dans le plugin (p.ex. `premiumbypass.accept`, `premiumbypass.remove`, `premiumbypass.status`).

---

## Développement & Compilation

Le projet utilise Gradle. Si vous souhaitez compiler localement :

1. Placez le JAR d'AuthMe dans `libs/` du projet (si nécessaire).
2. Dans `build.gradle`, décommentez la ligne `compileOnly files('libs/authme.jar')` si elle existe.
3. Compilez :

```bash
./gradlew clean build
```

Le JAR compilé se trouvera généralement dans `build/libs/`.

---

## Sécurité & vie privée

* **Risque IP** : L'utilisation d'une IP pour authentifier un joueur est moins sécurisée qu'un mot de passe. Les IP peuvent être partagées (NAT), changent avec les connexions, ou être compromises.
* **Opt‑in** : Le plugin oblige le joueur à consentir (`/premiumbypass accept`) — ne l'activez pas par défaut sans le consentement du joueur.
* **Données personnelles** : Les adresses IP sont des données personnelles dans plusieurs juridictions (p.ex. UE). Assurez‑vous de respecter la législation applicable (RGPD) : informer les joueurs, conserver les données pour une durée limitée, permettre la suppression à la demande, etc.

Recommandations : ajouter une option d'expiration automatique des IP et un mécanisme d'export/suppression des données pour les demandes des utilisateurs.

---

## Contribuer

Les contributions sont bienvenues : bugs, améliorations, suggestions de sécurité. Merci de forker le dépôt, créer une branche dédiée, et ouvrir une Pull Request.

---

## Licence

Ce projet est sous licence **Apache‑2.0**. Voir le fichier `LICENSE` pour le texte complet.

---

*Dernière mise à jour : 31 Octobre 2025*
