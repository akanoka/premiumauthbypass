/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package com.example.premiumauthbypass;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class PremiumAuthBypass
extends JavaPlugin
implements Listener {
    private Object authMeApiInstance = null;
    private Method mIsAuthenticated = null;
    private Method mForceLogin = null;
    private final Map<String, Boolean> premiumCache = new ConcurrentHashMap<String, Boolean>();
    private File linkedFile;
    private FileConfiguration linkedConfig;
    private FileConfiguration cfg;

    public void onEnable() {
        this.saveDefaultConfig();
        this.cfg = this.getConfig();
        this.linkedFile = new File(this.getDataFolder(), "linked.yml");
        if (!this.linkedFile.exists()) {
            this.linkedFile.getParentFile().mkdirs();
            try {
                this.linkedFile.createNewFile();
            }
            catch (IOException e) {
                this.getLogger().warning("Could not create linked.yml: " + e.getMessage());
            }
        }
        this.linkedConfig = YamlConfiguration.loadConfiguration((File)this.linkedFile);
        try {
            this.findAuthMeApi();
        }
        catch (Exception e) {
            this.getLogger().warning("AuthMe API not found via reflection. Plugin will still run but cannot force-login players until AuthMe is present.");
        }
        this.getServer().getPluginManager().registerEvents((Listener)this, (Plugin)this);
        this.getLogger().info("PremiumAuthBypass enabled. Use /premiumbypass accept|revoke|status");
    }

    public void onDisable() {
        if (this.linkedConfig != null && this.linkedFile != null) {
            try {
                this.linkedConfig.save(this.linkedFile);
            }
            catch (IOException e) {
                this.getLogger().warning("Could not save linked.yml: " + e.getMessage());
            }
        }
        this.getLogger().info("PremiumAuthBypass disabled.");
    }

    private void findAuthMeApi() {
        try {
            Class<?> c = Class.forName("fr.xephi.authme.api.v3.AuthMeApi");
            Method getInstance = c.getMethod("getInstance", new Class[0]);
            Object instance = getInstance.invoke(null, new Object[0]);
            Method isAuth = c.getMethod("isAuthenticated", Player.class);
            Method force = c.getMethod("forceLogin", Player.class);
            this.authMeApiInstance = instance;
            this.mIsAuthenticated = isAuth;
            this.mForceLogin = force;
            this.getLogger().info("Found AuthMe API: fr.xephi.authme.api.v3.AuthMeApi");
            return;
        }
        catch (Throwable c) {
            try {
                Class<?> c2 = Class.forName("fr.xephi.authme.api.API");
                Method getInstance = c2.getMethod("getInstance", new Class[0]);
                Object instance = getInstance.invoke(null, new Object[0]);
                Method isAuth = c2.getMethod("isAuthenticated", Player.class);
                Method force = c2.getMethod("forceLogin", Player.class);
                this.authMeApiInstance = instance;
                this.mIsAuthenticated = isAuth;
                this.mForceLogin = force;
                this.getLogger().info("Found AuthMe API: fr.xephi.authme.api.API");
                return;
            }
            catch (Throwable c2) {
                try {
                    Class<?> main = Class.forName("fr.xephi.authme.AuthMe");
                    Method getInstance = main.getMethod("getInstance", new Class[0]);
                    Object mainInstance = getInstance.invoke(null, new Object[0]);
                    Method getAPI = main.getMethod("getAPI", new Class[0]);
                    Object apiInstance = getAPI.invoke(mainInstance, new Object[0]);
                    Class<?> apiClass = apiInstance.getClass();
                    Method isAuth = apiClass.getMethod("isAuthenticated", Player.class);
                    Method force = apiClass.getMethod("forceLogin", Player.class);
                    this.authMeApiInstance = apiInstance;
                    this.mIsAuthenticated = isAuth;
                    this.mForceLogin = force;
                    this.getLogger().info("Found AuthMe API via fr.xephi.authme.AuthMe.getInstance().getAPI()");
                    return;
                }
                catch (Throwable throwable) {
                    return;
                }
            }
        }
    }

    private boolean isAuthenticated(Player p) {
        if (this.mIsAuthenticated == null) {
            return false;
        }
        try {
            Object res = this.mIsAuthenticated.invoke(this.authMeApiInstance, p);
            if (res instanceof Boolean) {
                return (Boolean)res;
            }
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            this.getLogger().warning("Error invoking isAuthenticated via reflection: " + e.getMessage());
        }
        return false;
    }

    private void forceLogin(Player p) {
        if (this.mForceLogin == null) {
            this.getLogger().warning("Cannot forceLogin - AuthMe API not found.");
            return;
        }
        try {
            this.mForceLogin.invoke(this.authMeApiInstance, p);
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            this.getLogger().warning("Error invoking forceLogin via reflection: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        boolean alreadyPrompted;
        Player player = event.getPlayer();
        String name = player.getName();
        String lower = name.toLowerCase();
        String currentIp = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : null;
        List storedIps = this.linkedConfig.getStringList(lower + ".ips");
        if (currentIp != null && storedIps != null && storedIps.contains(currentIp)) {
            if (!this.isAuthenticated(player)) {
                this.forceLogin(player);
                player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)this.cfg.getString("messages.auto_login_success", "&aConnexion automatique d\u00e9tect\u00e9e (compte premium) \u2014 authentifi\u00e9.")));
                this.getLogger().info("Auto-logged (IP matched) player: " + name + " (" + currentIp + ")");
            }
            return;
        }
        if (this.isAuthenticated(player) && !(alreadyPrompted = this.linkedConfig.getBoolean(lower + ".prompted", false))) {
            player.sendMessage(String.valueOf(ChatColor.YELLOW) + "Voulez-vous activer le bypass de connexion pour cette IP ?");
            player.sendMessage(String.valueOf(ChatColor.YELLOW) + "Tapez " + String.valueOf(ChatColor.AQUA) + "/premiumbypass accept" + String.valueOf(ChatColor.YELLOW) + " pour autoriser cette IP.");
            player.sendMessage(String.valueOf(ChatColor.YELLOW) + "Tapez " + String.valueOf(ChatColor.AQUA) + "/premiumbypass revoke" + String.valueOf(ChatColor.YELLOW) + " pour retirer l'autorisation plus tard.");
            this.linkedConfig.set(lower + ".prompted", (Object)true);
            try {
                this.linkedConfig.save(this.linkedFile);
            }
            catch (IOException e) {
                this.getLogger().warning("Could not save linked.yml: " + e.getMessage());
            }
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub;
        String currentIp;
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cette commande est uniquement pour les joueurs.");
            return true;
        }
        Player p = (Player)sender;
        String lower = p.getName().toLowerCase();
        String string = currentIp = p.getAddress() != null ? p.getAddress().getAddress().getHostAddress() : null;
        if (args.length == 0) {
            p.sendMessage(String.valueOf(ChatColor.YELLOW) + "Usage: /premium accept|revoke [all|<ip>]|status");
            return true;
        }
        switch (sub = args[0].toLowerCase(Locale.ROOT)) {
            case "accept": {
                return this.handleAccept(p, lower, currentIp);
            }
            case "revoke": {
                return this.handleRevoke(p, lower, currentIp, args);
            }
            case "status": {
                return this.handleStatus(p, lower);
            }
        }
        p.sendMessage(String.valueOf(ChatColor.YELLOW) + "Usage: /premium accept|revoke [all|<ip>]|status");
        return true;
    }

    private boolean handleAccept(Player p, String lower, String currentIp) {
        if (!this.isAuthenticated(p)) {
            p.sendMessage(String.valueOf(ChatColor.RED) + "Vous devez vous \u00eatre authentifi\u00e9 (tapez /login <mdp>) avant d'activer le bypass.");
            return true;
        }
        if (currentIp == null) {
            p.sendMessage(String.valueOf(ChatColor.RED) + "Impossible de r\u00e9cup\u00e9rer votre IP.");
            return true;
        }
        ArrayList<String> ips = new ArrayList<String>(this.linkedConfig.getStringList(lower + ".ips"));
        if (!ips.contains(currentIp)) {
            ips.add(currentIp);
            this.linkedConfig.set(lower + ".ips", ips);
            this.linkedConfig.set(lower + ".prompted", (Object)true);
            try {
                this.linkedConfig.save(this.linkedFile);
                p.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)this.cfg.getString("messages.premium_link_success", "&aIP ajout\u00e9e et activ\u00e9e pour le bypass !")));
                this.getLogger().info("Linked " + p.getName() + " to IP " + currentIp);
            }
            catch (IOException e) {
                p.sendMessage(String.valueOf(ChatColor.RED) + "Erreur lors de la sauvegarde: " + e.getMessage());
            }
        } else {
            p.sendMessage(String.valueOf(ChatColor.GREEN) + "Cette IP est d\u00e9j\u00e0 autoris\u00e9e pour le bypass.");
        }
        return true;
    }

    private boolean handleRevoke(Player p, String lower, String currentIp, String[] args) {
        ArrayList ips = new ArrayList(this.linkedConfig.getStringList(lower + ".ips"));
        if (args.length == 1) {
            if (currentIp == null) {
                p.sendMessage(String.valueOf(ChatColor.RED) + "Impossible de r\u00e9cup\u00e9rer votre IP.");
                return true;
            }
            if (ips.remove(currentIp)) {
                this.linkedConfig.set(lower + ".ips", ips);
                if (ips.isEmpty()) {
                    this.linkedConfig.set(lower + ".prompted", (Object)false);
                }
                try {
                    this.linkedConfig.save(this.linkedFile);
                    p.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)this.cfg.getString("messages.premium_revoke", "&cBypass retir\u00e9 pour cette IP.")));
                    this.getLogger().info("Revoked bypass for " + p.getName() + " IP: " + currentIp);
                }
                catch (IOException e) {
                    p.sendMessage(String.valueOf(ChatColor.RED) + "Erreur lors de la sauvegarde: " + e.getMessage());
                }
            } else {
                p.sendMessage(String.valueOf(ChatColor.YELLOW) + "Votre IP courante n'est pas dans la liste des IP autoris\u00e9es.");
            }
            return true;
        }
        String opt = args[1].toLowerCase(Locale.ROOT);
        if (opt.equals("all")) {
            this.linkedConfig.set(lower + ".ips", new ArrayList());
            this.linkedConfig.set(lower + ".prompted", (Object)false);
            try {
                this.linkedConfig.save(this.linkedFile);
                p.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)this.cfg.getString("messages.premium_revoke_all", "&cToutes les IPs autoris\u00e9es ont \u00e9t\u00e9 retir\u00e9es.")));
                this.getLogger().info("Revoked ALL bypass IPs for " + p.getName());
            }
            catch (IOException e) {
                p.sendMessage(String.valueOf(ChatColor.RED) + "Erreur lors de la sauvegarde: " + e.getMessage());
            }
            return true;
        }
        String ipToRemove = args[1];
        if (ips.remove(ipToRemove)) {
            this.linkedConfig.set(lower + ".ips", ips);
            if (ips.isEmpty()) {
                this.linkedConfig.set(lower + ".prompted", (Object)false);
            }
            try {
                this.linkedConfig.save(this.linkedFile);
                p.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)this.cfg.getString("messages.premium_revoke_specific", "&cIP supprim\u00e9e de la liste d'autorisations.")));
                this.getLogger().info("Revoked IP " + ipToRemove + " for " + p.getName());
            }
            catch (IOException e) {
                p.sendMessage(String.valueOf(ChatColor.RED) + "Erreur lors de la sauvegarde: " + e.getMessage());
            }
        } else {
            p.sendMessage(String.valueOf(ChatColor.YELLOW) + "L'IP sp\u00e9cifi\u00e9e n'est pas dans la liste.");
        }
        return true;
    }

    private boolean handleStatus(Player p, String lower) {
        List ips = this.linkedConfig.getStringList(lower + ".ips");
        if (ips == null || ips.isEmpty()) {
            p.sendMessage(String.valueOf(ChatColor.YELLOW) + "Aucune IP li\u00e9e pour votre compte.");
        } else {
            p.sendMessage(String.valueOf(ChatColor.GREEN) + "IPs li\u00e9es :");
            for (String ip : ips) {
                p.sendMessage(String.valueOf(ChatColor.AQUA) + " - " + ip);
            }
        }
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean checkUsernameIsPremium(String username) {
        HttpURLConnection con = null;
        try {
            String url = "https://api.mojang.com/users/profiles/minecraft/" + username;
            con = (HttpURLConnection)new URL(url).openConnection();
            con.setConnectTimeout(3000);
            con.setReadTimeout(3000);
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "PremiumAuthBypass/1.2");
            int code = con.getResponseCode();
            if (code == 200) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));){
                    String line;
                    StringBuilder sb = new StringBuilder();
                    while ((line = in.readLine()) != null) {
                        sb.append(line);
                    }
                    boolean bl = sb.length() > 0;
                    return bl;
                }
            }
            boolean bl = false;
            return bl;
        }
        catch (Exception e) {
            this.getLogger().warning("Error checking Mojang API for " + username + ": " + e.getMessage());
            boolean bl = false;
            return bl;
        }
        finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }
}
