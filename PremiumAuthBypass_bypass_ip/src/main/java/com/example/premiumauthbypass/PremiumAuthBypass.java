package com.example.premiumauthbypass;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PremiumAuthBypass with IP-based opt-in bypass.
 *
 * Flow:
 * - If player's name is linked and stored IP matches current IP -> forceLogin via AuthMe.
 * - Otherwise, player must login normally. After they are authenticated, they can run
 *   /premiumbypass accept to opt-in: their current IP is saved and future logins from that IP
 *   will be auto-logged.
 * - If IP changes, they must re-authenticate and run /premiumbypass accept again.
 *
 * Bedrock players (usernames starting with '_') are supported the same way.
 *
 * This plugin uses reflection to find a compatible AuthMe API at runtime, so it compiles without
 * a specific AuthMe jar, and works with several AuthMe versions.
 */
public class PremiumAuthBypass extends JavaPlugin implements Listener {

    private Object authMeApiInstance = null; // reflection-based API instance
    private Method mIsAuthenticated = null;
    private Method mForceLogin = null;

    private final Map<String, Boolean> premiumCache = new ConcurrentHashMap<>();
    private File linkedFile;
    private FileConfiguration linkedConfig;
    private FileConfiguration cfg;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        cfg = getConfig();

        // prepare linked.yml
        linkedFile = new File(getDataFolder(), "linked.yml");
        if (!linkedFile.exists()) {
            linkedFile.getParentFile().mkdirs();
            try {
                linkedFile.createNewFile();
            } catch (IOException e) {
                getLogger().warning("Could not create linked.yml: " + e.getMessage());
            }
        }
        linkedConfig = YamlConfiguration.loadConfiguration(linkedFile);

        // attempt to find AuthMe API reflectively
        try {
            findAuthMeApi();
        } catch (Exception e) {
            getLogger().warning("AuthMe API not found via reflection. Plugin will still run but cannot force-login players until AuthMe is present.");
        }

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("PremiumAuthBypass enabled. Use /premiumbypass accept|revoke|status");
    }

    @Override
    public void onDisable() {
        if (linkedConfig != null && linkedFile != null) {
            try {
                linkedConfig.save(linkedFile);
            } catch (IOException e) {
                getLogger().warning("Could not save linked.yml: " + e.getMessage());
            }
        }
        getLogger().info("PremiumAuthBypass disabled.");
    }

    /**
     * Try several known AuthMe API entry-points via reflection to obtain:
     * - an object instance (or null if static methods used)
     * - a method to check authentication: isAuthenticated(Player)
     * - a method to force login: forceLogin(Player)
     *
     * This increases compatibility with various AuthMe versions.
     */
    private void findAuthMeApi() {
        // Try v3 style: fr.xephi.authme.api.v3.AuthMeApi.getInstance()
        try {
            Class<?> c = Class.forName("fr.xephi.authme.api.v3.AuthMeApi");
            Method getInstance = c.getMethod("getInstance");
            Object instance = getInstance.invoke(null);
            Method isAuth = c.getMethod("isAuthenticated", org.bukkit.entity.Player.class);
            Method force = c.getMethod("forceLogin", org.bukkit.entity.Player.class);
            authMeApiInstance = instance;
            mIsAuthenticated = isAuth;
            mForceLogin = force;
            getLogger().info("Found AuthMe API: fr.xephi.authme.api.v3.AuthMeApi");
            return;
        } catch (Throwable ignored) {}

        // Try older API: fr.xephi.authme.api.API.getInstance()
        try {
            Class<?> c = Class.forName("fr.xephi.authme.api.API");
            Method getInstance = c.getMethod("getInstance");
            Object instance = getInstance.invoke(null);
            Method isAuth = c.getMethod("isAuthenticated", org.bukkit.entity.Player.class);
            Method force = c.getMethod("forceLogin", org.bukkit.entity.Player.class);
            authMeApiInstance = instance;
            mIsAuthenticated = isAuth;
            mForceLogin = force;
            getLogger().info("Found AuthMe API: fr.xephi.authme.api.API");
            return;
        } catch (Throwable ignored) {}

        // Try fr.xephi.authme.AuthMe.getInstance().getAPI()
        try {
            Class<?> main = Class.forName("fr.xephi.authme.AuthMe");
            Method getInstance = main.getMethod("getInstance");
            Object mainInstance = getInstance.invoke(null);
            Method getAPI = main.getMethod("getAPI");
            Object apiInstance = getAPI.invoke(mainInstance);
            Class<?> apiClass = apiInstance.getClass();
            Method isAuth = apiClass.getMethod("isAuthenticated", org.bukkit.entity.Player.class);
            Method force = apiClass.getMethod("forceLogin", org.bukkit.entity.Player.class);
            authMeApiInstance = apiInstance;
            mIsAuthenticated = isAuth;
            mForceLogin = force;
            getLogger().info("Found AuthMe API via fr.xephi.authme.AuthMe.getInstance().getAPI()");
            return;
        } catch (Throwable ignored) {}

        // If we reach here, API not found. That's okay; commands can still work after AuthMe is present.
    }

    private boolean isAuthenticated(Player p) {
        if (mIsAuthenticated == null) return false;
        try {
            Object res = mIsAuthenticated.invoke(authMeApiInstance, p);
            if (res instanceof Boolean) return (Boolean) res;
        } catch (IllegalAccessException | InvocationTargetException e) {
            getLogger().warning("Error invoking isAuthenticated via reflection: " + e.getMessage());
        }
        return false;
    }

    private void forceLogin(Player p) {
        if (mForceLogin == null) {
            getLogger().warning("Cannot forceLogin - AuthMe API not found.");
            return;
        }
        try {
            mForceLogin.invoke(authMeApiInstance, p);
        } catch (IllegalAccessException | InvocationTargetException e) {
            getLogger().warning("Error invoking forceLogin via reflection: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();
        String lower = name.toLowerCase();

        // Bedrock usernames: optional special handling can be added (user said they start with '_').
        boolean isBedrockLike = name.startsWith("_");

        // If stored IP exists and matches player's current IP -> auto-login
        String storedIp = linkedConfig.getString(lower + ".ip", null);
        String currentIp = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : null;

        if (storedIp != null && currentIp != null && storedIp.equals(currentIp)) {
            // auto login
            if (!isAuthenticated(player)) {
                forceLogin(player);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        cfg.getString("messages.auto_login_success", "&aConnexion automatique détectée (compte premium) — authentifié.")));
                getLogger().info("Auto-logged (IP matched) player: " + name + " (" + currentIp + ")");
            }
            return;
        }

        // If player is authenticated (they logged in with password), prompt once to offer bypass
        if (isAuthenticated(player)) {
            boolean alreadyPrompted = linkedConfig.getBoolean(lower + ".prompted", false);
            if (!alreadyPrompted) {
                player.sendMessage(ChatColor.YELLOW + "Voulez-vous activer le bypass de connexion pour cette IP ?");
                player.sendMessage(ChatColor.YELLOW + "Tapez " + ChatColor.AQUA + "/premiumbypass accept" + ChatColor.YELLOW + " pour autoriser cette IP.");
                player.sendMessage(ChatColor.YELLOW + "Tapez " + ChatColor.AQUA + "/premiumbypass revoke" + ChatColor.YELLOW + " pour retirer l'autorisation plus tard.");
                linkedConfig.set(lower + ".prompted", true);
                try {
                    linkedConfig.save(linkedFile);
                } catch (IOException e) {
                    getLogger().warning("Could not save linked.yml: " + e.getMessage());
                }
            }
        } else {
            // Not authenticated and IP different -> no auto-login. Nothing else to do.
        }
    }

    // Command handling: /premiumbypass accept | revoke | status
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command is only for players.");
            return true;
        }
        Player p = (Player) sender;
        String lower = p.getName().toLowerCase();
        String currentIp = p.getAddress() != null ? p.getAddress().getAddress().getHostAddress() : null;

        if (args.length == 0) {
            p.sendMessage(ChatColor.YELLOW + "Usage: /premiumbypass accept|revoke|status");
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("accept")) {
            // Only allow accept if player is authenticated right now
            if (!isAuthenticated(p)) {
                p.sendMessage(ChatColor.RED + "Vous devez vous être authentifié (tapez /login <mdp>) avant d'activer le bypass.");
                return true;
            }
            if (currentIp == null) {
                p.sendMessage(ChatColor.RED + "Impossible de récupérer votre IP.");
                return true;
            }
            linkedConfig.set(lower + ".ip", currentIp);
            linkedConfig.set(lower + ".prompted", true);
            try {
                linkedConfig.save(linkedFile);
                p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        cfg.getString("messages.premium_link_success", "&aCompte premium lié et connecté !")));
                getLogger().info("Linked " + p.getName() + " to IP " + currentIp);
            } catch (IOException e) {
                p.sendMessage(ChatColor.RED + "Erreur lors de la sauvegarde: " + e.getMessage());
            }
            return true;
        } else if (sub.equals("revoke")) {
            linkedConfig.set(lower + ".ip", null);
            linkedConfig.set(lower + ".prompted", false);
            try {
                linkedConfig.save(linkedFile);
                p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        cfg.getString("messages.premium_revoke", "&cBypass retiré pour ce compte.")));
                getLogger().info("Revoked bypass for " + p.getName());
            } catch (IOException e) {
                p.sendMessage(ChatColor.RED + "Erreur lors de la sauvegarde: " + e.getMessage());
            }
            return true;
        } else if (sub.equals("status")) {
            String ip = linkedConfig.getString(lower + ".ip", null);
            if (ip == null) {
                p.sendMessage(ChatColor.YELLOW + "Aucune IP liée pour votre compte.");
            } else {
                p.sendMessage(ChatColor.GREEN + "IP liée: " + ip);
            }
            return true;
        } else {
            p.sendMessage(ChatColor.YELLOW + "Usage: /premiumbypass accept|revoke|status");
            return true;
        }
    }

    /**
     * OPTIONAL: simple Mojang username check method (kept for compatibility or info).
     * Not used by the IP-bypass flow, but present if you want to enable detection.
     */
    @SuppressWarnings("unused")
    private boolean checkUsernameIsPremium(String username) {
        HttpURLConnection con = null;
        try {
            String url = "https://api.mojang.com/users/profiles/minecraft/" + username;
            con = (HttpURLConnection) new URL(url).openConnection();
            con.setConnectTimeout(3000);
            con.setReadTimeout(3000);
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "PremiumAuthBypass/1.2");

            int code = con.getResponseCode();
            if (code == 200) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) sb.append(line);
                    return sb.length() > 0;
                }
            }
            return false;
        } catch (Exception e) {
            getLogger().warning("Error checking Mojang API for " + username + ": " + e.getMessage());
            return false;
        } finally {
            if (con != null) con.disconnect();
        }
    }
}