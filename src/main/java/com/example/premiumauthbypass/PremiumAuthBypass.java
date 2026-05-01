package com.example.premiumauthbypass;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class PremiumAuthBypass extends JavaPlugin implements Listener {

    private Object authMeApiInstance = null;
    private Method mIsAuthenticated = null;
    private Method mForceLogin = null;
    
    private File linkedFile;
    private FileConfiguration linkedConfig;
    private FileConfiguration cfg;
    
    // Clé statique de 16 octets pour AES-128
    private static final byte[] KEY_BYTES = "Pr3m1umByp4ssK3y".getBytes(StandardCharsets.UTF_8);

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        this.cfg = getConfig();
        
        this.linkedFile = new File(getDataFolder(), "linked.yml");
        if (!this.linkedFile.exists()) {
            this.linkedFile.getParentFile().mkdirs();
            try {
                this.linkedFile.createNewFile();
            } catch (IOException e) {
                getLogger().warning("Could not create linked.yml: " + e.getMessage());
            }
        }
        this.linkedConfig = YamlConfiguration.loadConfiguration(this.linkedFile);
        
        try {
            findAuthMeApi();
        } catch (Exception e) {
            getLogger().warning("AuthMe API not found via reflection. Plugin will still run but cannot force-login players until AuthMe is present.");
        }
        
        getServer().getPluginManager().registerEvents(this, this);
        
        if (getCommand("premium") != null) {
            getCommand("premium").setTabCompleter(this);
        }
        
        getLogger().info("PremiumAuthBypass enabled.");
    }

    @Override
    public void onDisable() {
        if (this.linkedConfig != null && this.linkedFile != null) {
            try {
                this.linkedConfig.save(this.linkedFile);
            } catch (IOException e) {
                getLogger().warning("Could not save linked.yml: " + e.getMessage());
            }
        }
        getLogger().info("PremiumAuthBypass disabled.");
    }

    private void findAuthMeApi() {
        try {
            Class<?> c = Class.forName("fr.xephi.authme.api.v3.AuthMeApi");
            Method getInstance = c.getMethod("getInstance");
            Object instance = getInstance.invoke(null);
            Method isAuth = c.getMethod("isAuthenticated", Player.class);
            Method force = c.getMethod("forceLogin", Player.class);
            this.authMeApiInstance = instance;
            this.mIsAuthenticated = isAuth;
            this.mForceLogin = force;
            getLogger().info("Found AuthMe API: fr.xephi.authme.api.v3.AuthMeApi");
            return;
        } catch (Throwable throwable) {
            try {
                Class<?> c = Class.forName("fr.xephi.authme.api.API");
                Method getInstance = c.getMethod("getInstance");
                Object instance = getInstance.invoke(null);
                Method isAuth = c.getMethod("isAuthenticated", Player.class);
                Method force = c.getMethod("forceLogin", Player.class);
                this.authMeApiInstance = instance;
                this.mIsAuthenticated = isAuth;
                this.mForceLogin = force;
                getLogger().info("Found AuthMe API: fr.xephi.authme.api.API");
                return;
            } catch (Throwable throwable1) {
                try {
                    Class<?> main = Class.forName("fr.xephi.authme.AuthMe");
                    Method getInstance = main.getMethod("getInstance");
                    Object mainInstance = getInstance.invoke(null);
                    Method getAPI = main.getMethod("getAPI");
                    Object apiInstance = getAPI.invoke(mainInstance);
                    Class<?> apiClass = apiInstance.getClass();
                    Method isAuth = apiClass.getMethod("isAuthenticated", Player.class);
                    Method force = apiClass.getMethod("forceLogin", Player.class);
                    this.authMeApiInstance = apiInstance;
                    this.mIsAuthenticated = isAuth;
                    this.mForceLogin = force;
                    getLogger().info("Found AuthMe API via fr.xephi.authme.AuthMe.getInstance().getAPI()");
                } catch (Throwable throwable2) {
                    // AuthMe non trouvé
                }
            }
        }
    }

    private boolean isAuthenticated(Player p) {
        if (this.mIsAuthenticated == null) return false;
        try {
            Object res = this.mIsAuthenticated.invoke(this.authMeApiInstance, p);
            if (res instanceof Boolean) return (Boolean) res;
        } catch (Exception e) {
            getLogger().warning("Error invoking isAuthenticated: " + e.getMessage());
        }
        return false;
    }

    private void forceLogin(Player p) {
        if (this.mForceLogin == null) return;
        try {
            this.mForceLogin.invoke(this.authMeApiInstance, p);
        } catch (Exception e) {
            getLogger().warning("Error invoking forceLogin: " + e.getMessage());
        }
    }

    private String getMessage(String path, String def) {
        return ChatColor.translateAlternateColorCodes('&', this.cfg.getString(path, def));
    }

    // --- Système de Chiffrement AES ---
    private String encryptIp(String ip) {
        try {
            Key key = new SecretKeySpec(KEY_BYTES, "AES");
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.ENCRYPT_MODE, key);
            byte[] encVal = c.doFinal(ip.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encVal);
        } catch (Exception e) {
            getLogger().warning("Error encrypting IP: " + e.getMessage());
            return Base64.getEncoder().encodeToString(ip.getBytes(StandardCharsets.UTF_8)); // Fallback Base64 simple
        }
    }

    private String decryptIp(String encryptedIp) {
        try {
            Key key = new SecretKeySpec(KEY_BYTES, "AES");
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.DECRYPT_MODE, key);
            byte[] decordedValue = Base64.getDecoder().decode(encryptedIp);
            byte[] decValue = c.doFinal(decordedValue);
            return new String(decValue, StandardCharsets.UTF_8);
        } catch (Exception e) {
            try {
                return new String(Base64.getDecoder().decode(encryptedIp), StandardCharsets.UTF_8); // Fallback Base64
            } catch (Exception ex) {
                return encryptedIp; // Retourne tel quel si illisible
            }
        }
    }
    // ----------------------------------

    private void saveLinkedConfigAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                this.linkedConfig.save(this.linkedFile);
            } catch (IOException e) {
                getLogger().warning("Could not save linked.yml: " + e.getMessage());
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!this.cfg.getBoolean("settings.enableplugin", true)) {
            return;
        }

        Player player = event.getPlayer();
        String name = player.getName();
        String lower = name.toLowerCase();
        String currentIp = (player.getAddress() != null) ? player.getAddress().getAddress().getHostAddress() : null;
        
        List<String> encryptedIps = this.linkedConfig.getStringList(lower + ".ips");
        boolean isIpAuthorized = false;

        // Déchiffrement à la volée pour vérifier
        if (currentIp != null && encryptedIps != null) {
            for (String encIp : encryptedIps) {
                if (currentIp.equals(decryptIp(encIp))) {
                    isIpAuthorized = true;
                    break;
                }
            }
        }

        if (isIpAuthorized) {
            if (!isAuthenticated(player)) {
                forceLogin(player);
                player.sendMessage(getMessage("messages.auto_login_success", "&aConnexion automatique détectée !"));
                getLogger().info("Auto-logged player: " + name);
            }
            return;
        }

        if (isAuthenticated(player) && this.cfg.getBoolean("settings.prompt_on_join", true)) {
            boolean alreadyPrompted = this.linkedConfig.getBoolean(lower + ".prompted", false);
            if (!alreadyPrompted) {
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    boolean isPremium = checkUsernameIsPremium(name);
                    if (isPremium) {
                        Bukkit.getScheduler().runTask(this, () -> {
                            if (player.isOnline()) {
                                player.sendMessage(getMessage("messages.prompt_ip", "&eVoulez-vous activer le bypass de connexion pour cette IP ?"));
                                player.sendMessage(getMessage("messages.prompt_accept_howto", "&eFaites &b/premium accept&e pour autoriser cette IP."));
                                player.sendMessage(getMessage("messages.prompt_revoke_howto", "&eFaites &b/premium revoke&e pour retirer l'autorisation."));
                                this.linkedConfig.set(lower + ".prompted", true);
                                saveLinkedConfigAsync();
                            }
                        });
                    }
                });
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("messages.not_player", "This command is only for players."));
            return true;
        }

        Player p = (Player) sender;
        
        if (!this.cfg.getBoolean("settings.enableplugin", true)) {
            p.sendMessage(getMessage("messages.plugin_disabled", "&cPlugin désactivé."));
            return true;
        }

        String lower = p.getName().toLowerCase();
        String currentIp = (p.getAddress() != null) ? p.getAddress().getAddress().getHostAddress() : null;

        if (args.length == 0) {
            p.sendMessage(getMessage("messages.help_header", "&6=== Commandes Premium ==="));
            p.sendMessage(getMessage("messages.help_accept", "&e/premium accept"));
            p.sendMessage(getMessage("messages.help_revoke", "&e/premium revoke"));
            p.sendMessage(getMessage("messages.help_list", "&e/premium list"));
            p.sendMessage(getMessage("messages.help_about", "&e/premium about"));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "accept":
                return handleAccept(p, lower, currentIp);
            case "revoke":
                return handleRevoke(p, lower, currentIp, args);
            case "list":
                return handleList(p, lower, args);
            case "about":
                return handleAbout(p);
            default:
                p.sendMessage(getMessage("messages.unknown_command", "&eCommande inconnue."));
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!(sender instanceof Player)) return completions;

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (String cmd : Arrays.asList("accept", "revoke", "list", "about")) {
                if (cmd.startsWith(partial)) completions.add(cmd);
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            String partial = args[1].toLowerCase();
            
            if (sub.equals("list")) {
                if ("sure".startsWith(partial)) completions.add("sure");
            } else if (sub.equals("revoke")) {
                if ("all".startsWith(partial)) completions.add("all");
            }
        }
        return completions;
    }

    private boolean handleAccept(Player p, String lower, String currentIp) {
        if (!isAuthenticated(p)) {
            p.sendMessage(getMessage("messages.not_authenticated", "&cVous devez être connecté."));
            return true;
        }

        if (currentIp == null) {
            p.sendMessage(getMessage("messages.no_ip", "&cImpossible de récupérer votre adresse IP."));
            return true;
        }

        p.sendMessage(getMessage("messages.mojang_check", "&eVérification de votre compte..."));

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            boolean isPremium = checkUsernameIsPremium(p.getName());
            
            Bukkit.getScheduler().runTask(this, () -> {
                if (!isPremium) {
                    p.sendMessage(getMessage("messages.not_premium", "&cCe compte n'est pas Premium."));
                    return;
                }

                List<String> encryptedIps = this.linkedConfig.getStringList(lower + ".ips");
                if (encryptedIps == null) encryptedIps = new ArrayList<>();

                boolean alreadyLinked = false;
                for (String encIp : encryptedIps) {
                    if (currentIp.equals(decryptIp(encIp))) {
                        alreadyLinked = true;
                        break;
                    }
                }

                if (!alreadyLinked) {
                    encryptedIps.add(encryptIp(currentIp));
                    this.linkedConfig.set(lower + ".ips", encryptedIps);
                    this.linkedConfig.set(lower + ".prompted", true);
                    
                    saveLinkedConfigAsync(); 
                    p.sendMessage(getMessage("messages.premium_link_success", "&aVotre compte premium est désormais lié !"));
                    getLogger().info("Linked " + p.getName() + " to IP (encrypted)");
                } else {
                    p.sendMessage(getMessage("messages.already_linked", "&aCette IP est déjà autorisée."));
                }
            });
        });
        return true;
    }

    private boolean handleRevoke(Player p, String lower, String currentIp, String[] args) {
        List<String> encryptedIps = this.linkedConfig.getStringList(lower + ".ips");
        if (encryptedIps == null) encryptedIps = new ArrayList<>();

        if (args.length == 1) {
            if (currentIp == null) {
                p.sendMessage(getMessage("messages.no_ip", "&cImpossible de récupérer l'IP."));
                return true;
            }
            
            boolean removed = false;
            for (int i = 0; i < encryptedIps.size(); i++) {
                if (currentIp.equals(decryptIp(encryptedIps.get(i)))) {
                    encryptedIps.remove(i);
                    removed = true;
                    break;
                }
            }

            if (removed) {
                this.linkedConfig.set(lower + ".ips", encryptedIps);
                if (encryptedIps.isEmpty()) this.linkedConfig.set(lower + ".prompted", false);
                saveLinkedConfigAsync();
                p.sendMessage(getMessage("messages.premium_revoke", "&cLe bypass a été retiré."));
            } else {
                p.sendMessage(getMessage("messages.ip_not_in_list", "&eCette adresse IP n'est pas dans la liste."));
            }
            return true;
        }
        
        String opt = args[1].toLowerCase(Locale.ROOT);
        if (opt.equals("all")) {
            this.linkedConfig.set(lower + ".ips", new ArrayList<>());
            this.linkedConfig.set(lower + ".prompted", false);
            saveLinkedConfigAsync();
            p.sendMessage(getMessage("messages.premium_revoke_all", "&cToutes les adresses retirées."));
            return true;
        }
        
        // Retrait d'une IP spécifique (L'utilisateur la donne en clair)
        String ipToRemove = args[1];
        boolean removed = false;
        for (int i = 0; i < encryptedIps.size(); i++) {
            if (ipToRemove.equals(decryptIp(encryptedIps.get(i)))) {
                encryptedIps.remove(i);
                removed = true;
                break;
            }
        }

        if (removed) {
            this.linkedConfig.set(lower + ".ips", encryptedIps);
            if (encryptedIps.isEmpty()) this.linkedConfig.set(lower + ".prompted", false);
            saveLinkedConfigAsync();
            p.sendMessage(getMessage("messages.premium_revoke_specific", "&cL'IP a été retirée."));
        } else {
            p.sendMessage(getMessage("messages.ip_not_in_list", "&eL'adresse IP n'est pas dans la liste."));
        }
        return true;
    }

    private boolean handleList(Player p, String lower, String[] args) {
        if (args.length == 1) {
            p.sendMessage(getMessage("messages.list_confirm", "&eÊtes-vous sûr ?"));
            p.sendMessage(getMessage("messages.list_howto", "&eFaites /premium list sure"));
            return true;
        }
        
        if (args.length == 2 && args[1].equalsIgnoreCase("sure")) {
            List<String> encryptedIps = this.linkedConfig.getStringList(lower + ".ips");
            if (encryptedIps == null || encryptedIps.isEmpty()) {
                p.sendMessage(getMessage("messages.list_empty", "&eAucune IP."));
            } else {
                p.sendMessage(getMessage("messages.list_header", "&aIPs liées :"));
                for (String encIp : encryptedIps) {
                    String decrypted = decryptIp(encIp);
                    String format = getMessage("messages.list_format", "&b - %ip%");
                    p.sendMessage(format.replace("%ip%", decrypted));
                }
            }
        } else {
            p.sendMessage(getMessage("messages.unknown_command", "&eCommande inconnue."));
        }
        return true;
    }

    private boolean handleAbout(Player p) {
        List<String> lines = this.cfg.getStringList("messages.about");
        if (lines == null || lines.isEmpty()) {
            p.sendMessage(ChatColor.RED + "Aucune information à propos du plugin n'a été configurée.");
            return true;
        }
        
        for (String line : lines) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
        }
        return true;
    }

    private boolean checkUsernameIsPremium(String username) {
        HttpURLConnection con = null;
        try {
            String url = "https://api.mojang.com/users/profiles/minecraft/" + username;
            con = (HttpURLConnection) (new URL(url)).openConnection();
            con.setConnectTimeout(3000);
            con.setReadTimeout(3000);
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "PremiumAuthBypass/1.3");
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
