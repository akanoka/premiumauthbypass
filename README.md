# PremiumAuthBypass

**IP-based premium bypass (opt-in) for AuthMe Reloaded**

An add-on plugin for AuthMe Reloaded that allows players to **opt-in** to saving their IP address after a successful login. If the player reconnects from the same IP address, the plugin automatically calls `AuthMe#forceLogin` to log them in without requiring a password.

> ⚠️ This system is **optional** (opt-in) — the player must explicitly run `/premiumbypass accept` after their first login to save their IP address.

---

## Table of Contents

* [Overview](#overview)
* [Features](#features)
* [Workflow](#workflow)
* [Installation](#installation)
* [Configuration (example)](#configuration-example)
* [Commands](#commands)
* [Development & Compilation](#development--compilation)
* [Security & Privacy](#security--privacy)
* [Contributing](#contributing)
* [License](#license)

---

## Overview

PremiumAuthBypass improves the user experience on servers using AuthMe by providing a simple "automatic login" mechanism based on the player's registered IP address. It is designed for private or semi-private servers where the IP address is a reasonable indicator of trust.

---

## Features

* Opt-in saving of the player's IP address after successful authentication.
* Automatic bypass (call to `forceLogin`) if the player's IP address matches the saved IP address.
* Simple management of IP identifiers on the plugin side.
* Basic support for "Bedrock-like" names (names starting with `_`) — treated like other names.

---

## Workflow

1. The player connects normally and authenticates via AuthMe (`/login`). 2. After the first successful connection, the plugin prompts the player to run `/premiumbypass accept` to register their current IP address.
3. When the player returns, if their current IP matches the stored IP, the plugin calls `AuthMe.forceLogin(player)` and automatically logs them in.
4. If the IP changes, the player must re-authenticate and run `/premiumbypass accept` again.

---

## Installation

1. Download the compiled version of the plugin (JAR file) from the GitHub Releases.
2. Place `PremiumAuthBypass.jar` in the `plugins/` folder of your Minecraft server.
3. Restart the server.

**Note**: AuthMe Reloaded must be installed and working on your server for this plugin to function.

---

## Configuration (example)

The plugin can store IPs in an internal file (JSON/YAML format depending on the implementation). Example of a possible entry:

```yaml
# premiumbypass.yml (example)
akaknoyw:
prompted: true
ip: 127.0.0.1
ips:
- 127.0.0.1
- 192.168.1.254
```

> Adjust the configuration according to your needs and security policy.

---

## Commands

* `/premiumbypass accept` — Registers the player's current IP address for future bypass.
* `/premiumbypass remove` — Removes the player's IP registration.
* `/premiumbypass status` — Displays the current status (registered IP, date, etc.).

> These commands may require permissions defined in the plugin (e.g., `premiumbypass.accept`, `premiumbypass.remove`, `premiumbypass.status`).

---

## Development & Compilation

The project uses Gradle. If you want to compile locally:

1. Place the AuthMe JAR in the `libs/` folder of the project (if necessary). 2. In `build.gradle`, uncomment the line `compileOnly files('libs/authme.jar')` if it exists.
3. Compile:

```bash
./gradlew clean build
```

The compiled JAR will usually be located in `build/libs/`.

---

## Security & Privacy

* **IP Risk**: Using an IP address to authenticate a player is less secure than a password. IP addresses can be shared (NAT), change with connections, or be compromised.
* **Opt-in**: The plugin requires the player to consent (`/premiumbypass accept`) — do not enable it by default without the player's consent.
* **Personal Data**: IP addresses are considered personal data in several jurisdictions (e.g., EU). Make sure you comply with applicable legislation (GDPR): inform players, retain data for a limited time, allow deletion upon request, etc.

Recommendations: Add an option for automatic IP expiration and a data export/deletion mechanism for user requests.

---

## Contributing

Contributions are welcome: bug reports, improvements, security suggestions. Please fork the repository, create a dedicated branch, and open a Pull Request.

---

## License

This project is licensed under the **Apache-2.0** license. See the `LICENSE` file for the full text.

---

*Last updated: October 31, 2025*
