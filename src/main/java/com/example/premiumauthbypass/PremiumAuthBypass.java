/*     */ package com.example.premiumauthbypass;
/*     */ 
/*     */ import java.io.BufferedReader;
/*     */ import java.io.File;
/*     */ import java.io.IOException;
/*     */ import java.io.InputStreamReader;
/*     */ import java.lang.reflect.Method;
/*     */ import java.net.HttpURLConnection;
/*     */ import java.net.URL;
/*     */ import java.nio.charset.StandardCharsets;
/*     */ import java.util.ArrayList;
/*     */ import java.util.List;
/*     */ import java.util.Locale;
/*     */ import java.util.Map;
/*     */ import java.util.concurrent.ConcurrentHashMap;
/*     */ import org.bukkit.ChatColor;
/*     */ import org.bukkit.command.Command;
/*     */ import org.bukkit.command.CommandSender;
/*     */ import org.bukkit.configuration.file.FileConfiguration;
/*     */ import org.bukkit.configuration.file.YamlConfiguration;
/*     */ import org.bukkit.entity.Player;
/*     */ import org.bukkit.event.EventHandler;
/*     */ import org.bukkit.event.Listener;
/*     */ import org.bukkit.event.player.PlayerJoinEvent;
/*     */ import org.bukkit.plugin.Plugin;
/*     */ import org.bukkit.plugin.java.JavaPlugin;
/*     */ 
/*     */ public class PremiumAuthBypass extends JavaPlugin implements Listener {
/*  30 */   private Object authMeApiInstance = null;
/*     */   
/*  31 */   private Method mIsAuthenticated = null;
/*     */   
/*  32 */   private Method mForceLogin = null;
/*     */   
/*  34 */   private final Map<String, Boolean> premiumCache = new ConcurrentHashMap<>();
/*     */   
/*     */   private File linkedFile;
/*     */   
/*     */   private FileConfiguration linkedConfig;
/*     */   
/*     */   private FileConfiguration cfg;
/*     */   
/*     */   public void onEnable() {
/*  41 */     saveDefaultConfig();
/*  42 */     this.cfg = getConfig();
/*  45 */     this.linkedFile = new File(getDataFolder(), "linked.yml");
/*  46 */     if (!this.linkedFile.exists()) {
/*  47 */       this.linkedFile.getParentFile().mkdirs();
/*     */       try {
/*  49 */         this.linkedFile.createNewFile();
/*  50 */       } catch (IOException e) {
/*  51 */         getLogger().warning("Could not create linked.yml: " + e.getMessage());
/*     */       } 
/*     */     } 
/*  54 */     this.linkedConfig = (FileConfiguration)YamlConfiguration.loadConfiguration(this.linkedFile);
/*     */     try {
/*  57 */       findAuthMeApi();
/*  58 */     } catch (Exception e) {
/*  59 */       getLogger().warning("AuthMe API not found via reflection. Plugin will still run but cannot force-login players until AuthMe is present.");
/*     */     } 
/*  62 */     getServer().getPluginManager().registerEvents(this, (Plugin)this);
/*  63 */     getLogger().info("PremiumAuthBypass enabled. Use /premiumbypass accept|revoke|status");
/*     */   }
/*     */   
/*     */   public void onDisable() {
/*  68 */     if (this.linkedConfig != null && this.linkedFile != null)
/*     */       try {
/*  70 */         this.linkedConfig.save(this.linkedFile);
/*  71 */       } catch (IOException e) {
/*  72 */         getLogger().warning("Could not save linked.yml: " + e.getMessage());
/*     */       }  
/*  75 */     getLogger().info("PremiumAuthBypass disabled.");
/*     */   }
/*     */   
/*     */   private void findAuthMeApi() {
/*     */     try {
/*  80 */       Class<?> c = Class.forName("fr.xephi.authme.api.v3.AuthMeApi");
/*  81 */       Method getInstance = c.getMethod("getInstance", new Class[0]);
/*  82 */       Object instance = getInstance.invoke(null, new Object[0]);
/*  83 */       Method isAuth = c.getMethod("isAuthenticated", new Class[] { Player.class });
/*  84 */       Method force = c.getMethod("forceLogin", new Class[] { Player.class });
/*  85 */       this.authMeApiInstance = instance;
/*  86 */       this.mIsAuthenticated = isAuth;
/*  87 */       this.mForceLogin = force;
/*  88 */       getLogger().info("Found AuthMe API: fr.xephi.authme.api.v3.AuthMeApi");
/*     */       return;
/*  90 */     } catch (Throwable throwable) {
/*     */       try {
/*  93 */         Class<?> c = Class.forName("fr.xephi.authme.api.API");
/*  94 */         Method getInstance = c.getMethod("getInstance", new Class[0]);
/*  95 */         Object instance = getInstance.invoke(null, new Object[0]);
/*  96 */         Method isAuth = c.getMethod("isAuthenticated", new Class[] { Player.class });
/*  97 */         Method force = c.getMethod("forceLogin", new Class[] { Player.class });
/*  98 */         this.authMeApiInstance = instance;
/*  99 */         this.mIsAuthenticated = isAuth;
/* 100 */         this.mForceLogin = force;
/* 101 */         getLogger().info("Found AuthMe API: fr.xephi.authme.api.API");
/*     */         return;
/* 103 */       } catch (Throwable throwable1) {
/*     */         try {
/* 106 */           Class<?> main = Class.forName("fr.xephi.authme.AuthMe");
/* 107 */           Method getInstance = main.getMethod("getInstance", new Class[0]);
/* 108 */           Object mainInstance = getInstance.invoke(null, new Object[0]);
/* 109 */           Method getAPI = main.getMethod("getAPI", new Class[0]);
/* 110 */           Object apiInstance = getAPI.invoke(mainInstance, new Object[0]);
/* 111 */           Class<?> apiClass = apiInstance.getClass();
/* 112 */           Method isAuth = apiClass.getMethod("isAuthenticated", new Class[] { Player.class });
/* 113 */           Method force = apiClass.getMethod("forceLogin", new Class[] { Player.class });
/* 114 */           this.authMeApiInstance = apiInstance;
/* 115 */           this.mIsAuthenticated = isAuth;
/* 116 */           this.mForceLogin = force;
/* 117 */           getLogger().info("Found AuthMe API via fr.xephi.authme.AuthMe.getInstance().getAPI()");
/*     */           return;
/* 119 */         } catch (Throwable throwable2) {
/*     */           return;
/*     */         } 
/*     */       } 
/*     */     } 
/*     */   }
/*     */   
/*     */   private boolean isAuthenticated(Player p) {
/* 124 */     if (this.mIsAuthenticated == null)
/* 124 */       return false; 
/*     */     try {
/* 126 */       Object res = this.mIsAuthenticated.invoke(this.authMeApiInstance, new Object[] { p });
/* 127 */       if (res instanceof Boolean)
/* 127 */         return ((Boolean)res).booleanValue(); 
/* 128 */     } catch (IllegalAccessException|java.lang.reflect.InvocationTargetException e) {
/* 129 */       getLogger().warning("Error invoking isAuthenticated via reflection: " + e.getMessage());
/*     */     } 
/* 131 */     return false;
/*     */   }
/*     */   
/*     */   private void forceLogin(Player p) {
/* 135 */     if (this.mForceLogin == null) {
/* 136 */       getLogger().warning("Cannot forceLogin - AuthMe API not found.");
/*     */       return;
/*     */     } 
/*     */     try {
/* 140 */       this.mForceLogin.invoke(this.authMeApiInstance, new Object[] { p });
/* 141 */     } catch (IllegalAccessException|java.lang.reflect.InvocationTargetException e) {
/* 142 */       getLogger().warning("Error invoking forceLogin via reflection: " + e.getMessage());
/*     */     } 
/*     */   }
/*     */   
/*     */   @EventHandler
/*     */   public void onPlayerJoin(PlayerJoinEvent event) {
/* 148 */     Player player = event.getPlayer();
/* 149 */     String name = player.getName();
/* 150 */     String lower = name.toLowerCase();
/* 152 */     String currentIp = (player.getAddress() != null) ? player.getAddress().getAddress().getHostAddress() : null;
/* 153 */     List<String> storedIps = this.linkedConfig.getStringList(lower + ".ips");
/* 155 */     if (currentIp != null && storedIps != null && storedIps.contains(currentIp)) {
/* 156 */       if (!isAuthenticated(player)) {
/* 157 */         forceLogin(player);
/* 158 */         player.sendMessage(ChatColor.translateAlternateColorCodes('&', this.cfg
/* 159 */               .getString("messages.auto_login_success", "&aAutomatic login detected (premium account) — authenticated.")));
/* 160 */         getLogger().info("Auto-logged (IP matched) player: " + name + " (" + currentIp + ")");
/*     */       } 
/*     */       return;
/*     */     } 
/* 165 */     if (isAuthenticated(player)) {
/* 166 */       boolean alreadyPrompted = this.linkedConfig.getBoolean(lower + ".prompted", false);
/* 167 */       if (!alreadyPrompted) {
/* 168 */         player.sendMessage(String.valueOf(ChatColor.YELLOW) + "Do you want to enable connection bypass for this IP address?");
/* 169 */         player.sendMessage(String.valueOf(ChatColor.YELLOW) + "Type " + String.valueOf(ChatColor.YELLOW) + "/premiumbypass accept" + String.valueOf(ChatColor.AQUA) + " to authorize this IP.");
/* 170 */         player.sendMessage(String.valueOf(ChatColor.YELLOW) + "Type " + String.valueOf(ChatColor.YELLOW) + "/premiumbypass revoke" + String.valueOf(ChatColor.AQUA) + " to withdraw authorization later.");
/* 171 */         this.linkedConfig.set(lower + ".prompted", Boolean.valueOf(true));
/*     */         try {
/* 173 */           this.linkedConfig.save(this.linkedFile);
/* 174 */         } catch (IOException e) {
/* 175 */           getLogger().warning("Could not save linked.yml: " + e.getMessage());
/*     */         } 
/*     */       } 
/*     */     } 
/*     */   }
/*     */   
/*     */   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
/* 183 */     if (!(sender instanceof Player)) {
/* 184 */       sender.sendMessage("This command is only for players.");
/* 185 */       return true;
/*     */     } 
/* 188 */     Player p = (Player)sender;
/* 189 */     String lower = p.getName().toLowerCase();
/* 190 */     String currentIp = (p.getAddress() != null) ? p.getAddress().getAddress().getHostAddress() : null;
/* 192 */     if (args.length == 0) {
/* 193 */       p.sendMessage(String.valueOf(ChatColor.YELLOW) + "Usage: /premium accept|revoke [all|<ip>]|status");
/* 194 */       return true;
/*     */     } 
/* 197 */     String sub = args[0].toLowerCase(Locale.ROOT);
/* 199 */     switch (sub) {
/*     */       case "accept":
/* 201 */         return handleAccept(p, lower, currentIp);
/*     */       case "revoke":
/* 203 */         return handleRevoke(p, lower, currentIp, args);
/*     */       case "status":
/* 205 */         return handleStatus(p, lower);
/*     */     } 
/* 207 */     p.sendMessage(String.valueOf(ChatColor.YELLOW) + "Usage: /premium accept|revoke [all|<ip>]|status");
/* 208 */     return true;
/*     */   }
/*     */   
/*     */   private boolean handleAccept(Player p, String lower, String currentIp) {
/* 214 */     if (!isAuthenticated(p)) {
/* 215 */       p.sendMessage(String.valueOf(ChatColor.RED) + "You must have authenticated (type /login <password>) before activating the bypass.");
/* 216 */       return true;
/*     */     } 
/* 218 */     if (currentIp == null) {
/* 219 */       p.sendMessage(String.valueOf(ChatColor.RED) + "Unable to retrieve your IP address.");
/* 220 */       return true;
/*     */     } 
/* 222 */     List<String> ips = new ArrayList<>(this.linkedConfig.getStringList(lower + ".ips"));
/* 223 */     if (!ips.contains(currentIp)) {
/* 224 */       ips.add(currentIp);
/* 225 */       this.linkedConfig.set(lower + ".ips", ips);
/* 226 */       this.linkedConfig.set(lower + ".prompted", Boolean.valueOf(true));
/*     */       try {
/* 228 */         this.linkedConfig.save(this.linkedFile);
/* 229 */         p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.cfg
/* 230 */               .getString("messages.premium_link_success", "&aIP address added and enabled for bypass!")));
/* 231 */         getLogger().info("Linked " + p.getName() + " to IP " + currentIp);
/* 232 */       } catch (IOException e) {
/* 233 */         p.sendMessage(String.valueOf(ChatColor.RED) + "Error during backup: " + String.valueOf(ChatColor.RED));
/*     */       } 
/*     */     } else {
/* 236 */       p.sendMessage(String.valueOf(ChatColor.GREEN) + "This IP address is already authorized for bypass.");
/*     */     } 
/* 238 */     return true;
/*     */   }
/*     */   
/*     */   private boolean handleRevoke(Player p, String lower, String currentIp, String[] args) {
/* 242 */     List<String> ips = new ArrayList<>(this.linkedConfig.getStringList(lower + ".ips"));
/* 243 */     if (args.length == 1) {
/* 244 */       if (currentIp == null) {
/* 245 */         p.sendMessage(String.valueOf(ChatColor.RED) + "Unable to retrieve your IP address.");
/* 246 */         return true;
/*     */       } 
/* 248 */       if (ips.remove(currentIp)) {
/* 249 */         this.linkedConfig.set(lower + ".ips", ips);
/* 250 */         if (ips.isEmpty())
/* 251 */           this.linkedConfig.set(lower + ".prompted", Boolean.valueOf(false)); 
/*     */         try {
/* 254 */           this.linkedConfig.save(this.linkedFile);
/* 255 */           p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.cfg
/* 256 */                 .getString("messages.premium_revoke", "&cBypass removed for this IP.")));
/* 257 */           getLogger().info("Revoked bypass for " + p.getName() + " IP: " + currentIp);
/* 258 */         } catch (IOException e) {
/* 259 */           p.sendMessage(String.valueOf(ChatColor.RED) + "Error during backup: " + String.valueOf(ChatColor.RED));
/*     */         } 
/*     */       } else {
/* 262 */         p.sendMessage(String.valueOf(ChatColor.YELLOW) + "Your current IP address is not in the list of allowed IP addresses.");
/*     */       } 
/* 264 */       return true;
/*     */     } 
/* 266 */     String opt = args[1].toLowerCase(Locale.ROOT);
/* 267 */     if (opt.equals("all")) {
/* 268 */       this.linkedConfig.set(lower + ".ips", new ArrayList());
/* 269 */       this.linkedConfig.set(lower + ".prompted", Boolean.valueOf(false));
/*     */       try {
/* 271 */         this.linkedConfig.save(this.linkedFile);
/* 272 */         p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.cfg
/* 273 */               .getString("messages.premium_revoke_all", "&cAll authorized IP addresses have been removed.")));
/* 274 */         getLogger().info("Revoked ALL bypass IPs for " + p.getName());
/* 275 */       } catch (IOException e) {
/* 276 */         p.sendMessage(String.valueOf(ChatColor.RED) + "Error during backup: " + String.valueOf(ChatColor.RED));
/*     */       } 
/* 278 */       return true;
/*     */     } 
/* 280 */     String ipToRemove = args[1];
/* 281 */     if (ips.remove(ipToRemove)) {
/* 282 */       this.linkedConfig.set(lower + ".ips", ips);
/* 283 */       if (ips.isEmpty())
/* 283 */         this.linkedConfig.set(lower + ".prompted", Boolean.valueOf(false)); 
/*     */       try {
/* 285 */         this.linkedConfig.save(this.linkedFile);
/* 286 */         p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.cfg
/* 287 */               .getString("messages.premium_revoke_specific", "&cIP removed from the allowlist.")));
/* 288 */         getLogger().info("Revoked IP " + ipToRemove + " for " + p.getName());
/* 289 */       } catch (IOException e) {
/* 290 */         p.sendMessage(String.valueOf(ChatColor.RED) + "Error during backup: " + String.valueOf(ChatColor.RED));
/*     */       } 
/*     */     } else {
/* 293 */       p.sendMessage(String.valueOf(ChatColor.YELLOW) + "The specified IP address is not in the list.");
/*     */     } 
/* 295 */     return true;
/*     */   }
/*     */   
/*     */   private boolean handleStatus(Player p, String lower) {
/* 301 */     List<String> ips = this.linkedConfig.getStringList(lower + ".ips");
/* 302 */     if (ips == null || ips.isEmpty()) {
/* 303 */       p.sendMessage(String.valueOf(ChatColor.YELLOW) + "No IP address is linked to your account.");
/*     */     } else {
/* 305 */       p.sendMessage(String.valueOf(ChatColor.GREEN) + "Linked IPs:");
/* 306 */       for (String ip : ips)
/* 307 */         p.sendMessage(String.valueOf(ChatColor.AQUA) + " - " + String.valueOf(ChatColor.AQUA)); 
/*     */     } 
/* 310 */     return true;
/*     */   }
/*     */   
/*     */   private boolean checkUsernameIsPremium(String username) {
/* 315 */     HttpURLConnection con = null;
/*     */     try {
/* 317 */       String url = "https://api.mojang.com/users/profiles/minecraft/" + username;
/* 318 */       con = (HttpURLConnection)(new URL(url)).openConnection();
/* 319 */       con.setConnectTimeout(3000);
/* 320 */       con.setReadTimeout(3000);
/* 321 */       con.setRequestMethod("GET");
/* 322 */       con.setRequestProperty("User-Agent", "PremiumAuthBypass/1.2");
/* 324 */       int code = con.getResponseCode();
/* 325 */       if (code == 200) {
/* 326 */         BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
/*     */         try {
/* 327 */           StringBuilder sb = new StringBuilder();
/*     */           String line;
/* 329 */           for (; (line = in.readLine()) != null; sb.append(line));
/* 330 */           boolean bool = (sb.length() > 0) ? true : false;
/* 331 */           in.close();
/*     */           return bool;
/*     */         } catch (Throwable throwable) {
/*     */           try {
/*     */             in.close();
/*     */           } catch (Throwable throwable1) {
/*     */             throwable.addSuppressed(throwable1);
/*     */           } 
/*     */           throw throwable;
/*     */         } 
/*     */       } 
/* 333 */       return false;
/* 334 */     } catch (Exception e) {
/* 335 */       getLogger().warning("Error checking Mojang API for " + username + ": " + e.getMessage());
/* 336 */       return false;
/*     */     } finally {
/* 338 */       if (con != null)
/* 338 */         con.disconnect(); 
/*     */     } 
/*     */   }
/*     */ }


/* Location:              C:\Users\night\Downloads\PremiumAuthBypass-1.1.1.jar!\com\example\premiumauthbypass\PremiumAuthBypass.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */
