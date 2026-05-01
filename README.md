# 🔐 PremiumAuthBypass

**PremiumAuthBypass** is a Bukkit/Spigot plugin designed to streamline authentication for premium Minecraft players by allowing automatic login based on trusted IP addresses.

It integrates with **AuthMe Reloaded** and securely links player accounts to their IPs using AES encryption.

---

## ✨ Features

- 🔓 **Automatic Login**
  - Instantly logs in players if they connect from a trusted IP.

- 🔗 **Premium Account Verification**
  - Checks usernames against Mojang API to ensure they are premium accounts.

- 🌐 **IP Linking System**
  - Players can link their IP to their account for future automatic authentication.

- 🔐 **AES Encryption**
  - All stored IP addresses are encrypted (AES-128) for security.

- ⚡ **Async Operations**
  - Network requests and file saves are handled asynchronously to avoid server lag.

- 🧠 **Smart AuthMe Integration**
  - Uses reflection to support multiple versions of AuthMe.

- 💬 **Fully Configurable Messages**
  - Customize all plugin messages via config.

---

## 📦 Requirements

- Java 8+
- Bukkit / Spigot / Paper server
- **AuthMe Reloaded** (recommended for full functionality)

---

## ⚙️ How It Works

1. When a player joins:
   - The plugin checks if their IP is already linked.
   - If yes → automatic login via AuthMe.

2. If not linked:
   - The plugin verifies if the account is **premium**.
   - Prompts the player to link their IP.

3. Once linked:
   - Future logins from that IP are automatic.

---

## 🔧 Commands

| Command | Description |
|--------|-------------|
| `/premium accept` | Link your current IP to your account |
| `/premium revoke` | Remove your current IP |
| `/premium revoke all` | Remove all linked IPs |
| `/premium list` | Show linked IPs (confirmation required) |
| `/premium about` | Show plugin info |

---

## 📁 Configuration Files

### `config.yml`
- Enable/disable plugin
- Customize messages
- Control join behavior

### `linked.yml`
- Stores encrypted IPs per player
- Automatically managed by the plugin

---

## 🔒 Security

- Uses **AES-128 encryption** for IP storage
- Fallback to Base64 if encryption fails
- No plain IPs stored in files

---

## 🔄 Compatibility

The plugin dynamically detects AuthMe API using:
- `fr.xephi.authme.api.v3.AuthMeApi`
- `fr.xephi.authme.api.API`
- Legacy `AuthMe` API

---

## 🌍 External API

- Uses Mojang endpoint: https://api.mojang.com/users/profiles/minecraft/{username}

---

to verify premium accounts.

---

## ⚠️ Notes

- Requires AuthMe to force login players.
- Without AuthMe, the plugin still runs but cannot auto-login users.
- IP-based login assumes stable player IPs.

---

## 🧩 Example Use Case

> A premium player joins your server regularly from the same location.

✔ First login → manually authenticate  
✔ Accept IP linking via `/premium accept`  
✔ Next logins → instant automatic login 🚀
