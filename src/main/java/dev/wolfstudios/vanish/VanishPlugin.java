package dev.wolfstudios.vanish;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class VanishPlugin extends JavaPlugin implements Listener {

    private final Set<UUID> vanished = Collections.synchronizedSet(new HashSet<UUID>());
    private final Set<UUID> silentChestPlayers = Collections.synchronizedSet(new HashSet<UUID>());
    private final Set<UUID> savedVanish = Collections.synchronizedSet(new HashSet<UUID>());
    private final Map<UUID, Long> vanishCooldowns = Collections.synchronizedMap(new HashMap<UUID, Long>());
    private boolean folia;
    private File vanishDataFile;
    private FileConfiguration vanishData;
    private File langFile;
    private FileConfiguration lang;
    private File logFile;
    private String language = "en";
    private int cooldownMs = 1000;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadLang();
        cooldownMs = getConfig().getInt("settings.cooldown-ms", 1000);
        if (cooldownMs < 250) cooldownMs = 250;
        folia = detectFolia();
        loadVanishData();
        initLogFile();
        getServer().getPluginManager().registerEvents(this, this);

        List<String> loaded = vanishData.getStringList("vanished");
        int invalidCount = 0;
        for (String uuidStr : loaded) {
            try {
                UUID uuid = UUID.fromString(uuidStr.trim());
                if (uuid.version() == 4 || uuid.version() == 3) {
                    savedVanish.add(uuid);
                } else {
                    invalidCount++;
                }
            } catch (IllegalArgumentException e) {
                invalidCount++;
                logToFile("INVALID", "Carregamento", "UUID invalido descartado: " + uuidStr);
            }
        }
        if (invalidCount > 0) {
            getLogger().warning("[Vanish+] " + invalidCount + " UUID(s) invalido(s) descartado(s) de data.yml");
        }

        logToFile("SYSTEM", "Servidor", "Plugin ativado. Backend: " + (folia ? "Folia" : "Bukkit/Spigot/Paper/Purpur"));
        getLogger().info("Vanish+ ativado! Idioma: " + language + " | Backend: " + (folia ? "Folia" : "Bukkit/Spigot/Paper/Purpur"));
    }

    @Override
    public void onDisable() {
        for (UUID uuid : vanished) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) revealPlayer(p);
        }
        vanished.clear();
        saveVanishData();
        getLogger().info("Vanish+ desativado!");
    }

    // ==================== Security Log ====================

    private void initLogFile() {
        File logDir = new File(getDataFolder(), "logs");
        if (!logDir.exists()) logDir.mkdirs();
        logFile = new File(logDir, "vanish.log");
        if (!logFile.exists()) {
            try { logFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void logToFile(String type, String source, String message) {
        if (logFile == null) return;
        try (PrintWriter pw = new PrintWriter(new FileWriter(logFile, true))) {
            String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            pw.println("[" + ts + "] [" + type + "] [" + source + "] " + message);
        } catch (IOException e) {
            // silent
        }
    }

    private boolean isOnCooldown(UUID uuid) {
        synchronized (vanishCooldowns) {
            Long last = vanishCooldowns.get(uuid);
            if (last != null && System.currentTimeMillis() - last < cooldownMs) return true;
            vanishCooldowns.put(uuid, System.currentTimeMillis());
            return false;
        }
    }

    // ==================== Language ====================

    private static final Set<String> SUPPORTED_LANGUAGES = new HashSet<>(Arrays.asList("en", "pt", "es", "fr", "de"));

    private void loadLang() {
        language = getConfig().getString("language", "en");

        if (!SUPPORTED_LANGUAGES.contains(language)) {
            getLogger().warning("[Vanish+] Lingua '" + language + "' nao suportada. Usando 'en'.");
            logToFile("SYSTEM", "Language", "Linguagem invalida '" + language + "', fallback para 'en'");
            language = "en";
        }

        for (String langKey : SUPPORTED_LANGUAGES) {
            saveResource("lang/" + langKey + ".yml", false);
        }

        langFile = new File(getDataFolder(), "lang/" + language + ".yml");
        if (!langFile.exists()) {
            getLogger().warning("[Vanish+] Arquivo de linguagem '" + language + ".yml' nao encontrado. Usando 'en'.");
            langFile = new File(getDataFolder(), "lang/en.yml");
        }
        lang = YamlConfiguration.loadConfiguration(langFile);
    }

    private String lang(String key) {
        if (lang == null) return key;
        String prefix = lang.getString("prefix", "&a&lVanish+ &8> &r");
        String message = lang.getString(key, "Vanish+: " + key);
        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }

    private String langRaw(String key) {
        if (lang == null) return key;
        String message = lang.getString(key, "Vanish+: " + key);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    // ==================== Data ====================

    private void loadVanishData() {
        vanishDataFile = new File(getDataFolder(), "data.yml");
        if (!vanishDataFile.exists()) {
            try {
                vanishDataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        vanishData = YamlConfiguration.loadConfiguration(vanishDataFile);
    }

    private void saveVanishData() {
        List<String> list = new ArrayList<String>();
        for (UUID uuid : savedVanish) list.add(uuid.toString());
        for (UUID uuid : vanished) list.add(uuid.toString());
        vanishData.set("vanished", list);
        try {
            vanishData.save(vanishDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==================== Folia ====================

    private boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void scheduleNextTick(Player player, Runnable task) {
        if (folia) {
            try {
                Method getScheduler = player.getClass().getMethod("getScheduler");
                Object entityScheduler = getScheduler.invoke(player);
                Method run = entityScheduler.getClass().getMethod("run", org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class, Runnable.class);
                run.invoke(entityScheduler, this, (java.util.function.Consumer<Object>) (o -> task.run()), null);
                return;
            } catch (Exception e) {
                getLogger().severe("[Vanish+] Falha critica no scheduler Folia: " + e.getMessage());
                logToFile("ERROR", "FoliaScheduler", "Falha: " + e.getMessage());
                return;
            }
        }
        Bukkit.getScheduler().runTask(this, task);
    }

    private void hideFrom(Player viewer, Player target) {
        if (folia) {
            scheduleNextTick(viewer, () -> viewer.hidePlayer(VanishPlugin.this, target));
        } else {
            viewer.hidePlayer(VanishPlugin.this, target);
        }
    }

    private void showTo(Player viewer, Player target) {
        if (folia) {
            scheduleNextTick(viewer, () -> viewer.showPlayer(VanishPlugin.this, target));
        } else {
            viewer.showPlayer(VanishPlugin.this, target);
        }
    }

    // ==================== Commands ====================

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String name = cmd.getName().toLowerCase();

        if (name.equals("vanish")) {
            return handleVanish(sender, args);
        } else if (name.equals("vanishconfig") || name.equals("vcfg") || name.equals("vconfig")) {
            return handleVanishConfig(sender);
        }
        return false;
    }

    private boolean handleVanish(CommandSender sender, String[] args) {
        if (!getConfig().getBoolean("settings.vanish-enabled", true)) {
            sender.sendMessage(lang("vanish-disabled-server"));
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cUso: /vanish <jogador>");
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission("vanish.use")) {
                player.sendMessage(lang("no-permission"));
                return true;
            }
            toggleVanish(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("vanish.reload")) {
                sender.sendMessage(lang("no-permission"));
                return true;
            }
            reloadConfig();
            saveDefaultConfig();
            loadLang();
            cooldownMs = getConfig().getInt("settings.cooldown-ms", 1000);
            if (cooldownMs < 250) cooldownMs = 250;

            if (!getConfig().getBoolean("settings.vanish-enabled", true)) {
                List<UUID> toReveal = new ArrayList<>(vanished);
                for (UUID uuid : toReveal) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        revealPlayer(p);
                        p.sendMessage(lang("vanish-disabled-server"));
                    }
                    vanished.remove(uuid);
                }
                savedVanish.clear();
                logToFile("SYSTEM", "Reload", "Vanish desativado — " + toReveal.size() + " jogador(es) revelado(s)");
            }

            sender.sendMessage(lang("reload-success"));
            logToFile("SYSTEM", sender.getName(), "Config recarregada");
            return true;
        }

        if (args[0].equalsIgnoreCase("reveal")) {
            if (!sender.hasPermission("vanish.reveal")) {
                sender.sendMessage(lang("no-permission"));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§cUso: /vanish reveal <jogador>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(lang("player-not-found").replace("{player}", args[1]));
                return true;
            }
            if (!isVanished(target)) {
                sender.sendMessage(lang("reveal-not-vanished").replace("{player}", target.getName()));
                return true;
            }
            revealPlayer(target);
            target.sendMessage(lang("vanish-revealed-by-admin").replace("{admin}", sender.getName()));
            sender.sendMessage(lang("reveal-success").replace("{player}", target.getName()));
            logToFile("REVEAL", sender.getName(), "Revealed " + target.getName());
            return true;
        }

        if (args[0].equalsIgnoreCase("revealall")) {
            if (!sender.hasPermission("vanish.revealall")) {
                sender.sendMessage(lang("no-permission"));
                return true;
            }
            int count = 0;
            List<UUID> toReveal = new ArrayList<>(vanished);
            for (UUID uuid : toReveal) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    revealPlayer(p);
                    p.sendMessage(lang("vanish-revealed-by-admin").replace("{admin}", sender.getName()));
                    count++;
                }
            }
            if (sender.hasPermission("vanish.see")) {
                sender.sendMessage(lang("revealall-success").replace("{count}", String.valueOf(count)));
            }
            logToFile("REVEALALL", sender.getName(), "Revealed " + count + " jogadores");
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            if (!sender.hasPermission("vanish.see")) {
                sender.sendMessage(lang("no-permission"));
                return true;
            }
            sender.sendMessage(lang("vanished-list"));
            int i = 1;
            for (UUID uuid : vanished) {
                Player v = Bukkit.getPlayer(uuid);
                String name = v != null ? v.getName() : "???";
                sender.sendMessage("  §7" + i + ". §f" + name);
                i++;
            }
            if (vanished.isEmpty()) sender.sendMessage(lang("no-vanished"));
            return true;
        }

        if (!sender.hasPermission("vanish.others")) {
            sender.sendMessage(lang("no-permission"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(lang("player-not-found").replace("{player}", args[0]));
            return true;
        }
        toggleVanish(target);
        sender.sendMessage(lang("vanish-other")
                .replace("{player}", target.getName())
                .replace("{status}", isVanished(target) ? langRaw("status-off-vanish") : langRaw("status-on-vanish")));
        return true;
    }

    private boolean handleVanishConfig(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores!");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("vanish.config")) {
            player.sendMessage(lang("no-permission"));
            return true;
        }
        openConfigGUI(player);
        return true;
    }

    // ==================== GUI ====================

    private void openConfigGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.translateAlternateColorCodes('&', langRaw("config-gui-title")));

        boolean vanishEnabled = getConfig().getBoolean("settings.vanish-enabled", true);
        boolean saveOnQuit = getConfig().getBoolean("settings.save-vanish-on-quit", true);
        boolean silentChestEnabled = getConfig().getBoolean("protection.silent-chest", true);
        boolean blockInteract = getConfig().getBoolean("protection.block-interact", true);
        boolean blockBreak = getConfig().getBoolean("protection.block-break", true);
        boolean blockPlace = getConfig().getBoolean("protection.block-place", true);
        boolean itemDrop = getConfig().getBoolean("protection.item-drop", true);
        boolean itemPickup = getConfig().getBoolean("protection.item-pickup", true);
        boolean mobTarget = getConfig().getBoolean("protection.mob-target", true);
        boolean pvp = getConfig().getBoolean("protection.pvp", true);

        inv.setItem(10, toggleItem(vanishEnabled, langRaw("gui.vanish-enabled"), langRaw("gui.vanish-enabled-desc"), Material.ENDER_EYE));
        inv.setItem(11, toggleItem(saveOnQuit, langRaw("gui.save-vanish"), langRaw("gui.save-vanish-desc"), Material.BOOK));
        inv.setItem(12, toggleItem(silentChestEnabled, langRaw("gui.silent-chest"), langRaw("gui.silent-chest-desc"), Material.CHEST));
        inv.setItem(13, toggleItem(blockInteract, langRaw("gui.block-interact"), "", Material.LEVER));
        inv.setItem(14, toggleItem(blockBreak, langRaw("gui.block-break"), "", Material.DIAMOND_PICKAXE));
        inv.setItem(15, toggleItem(blockPlace, langRaw("gui.block-place"), "", Material.GRASS_BLOCK));
        inv.setItem(16, toggleItem(itemDrop, langRaw("gui.item-drop"), "", Material.DROPPER));
        inv.setItem(19, toggleItem(itemPickup, langRaw("gui.item-pickup"), "", Material.HOPPER));
        inv.setItem(20, toggleItem(mobTarget, langRaw("gui.mob-target"), "", Material.ZOMBIE_HEAD));
        inv.setItem(21, toggleItem(pvp, langRaw("gui.pvp"), "", Material.DIAMOND_SWORD));

        boolean playerSilent = silentChestPlayers.contains(player.getUniqueId());
        inv.setItem(23, toggleItem(playerSilent, langRaw("gui.my-silent-chest"), langRaw("gui.silent-chest-desc"), Material.CHEST));

        ItemStack gray = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, gray);
        }

        player.openInventory(inv);
    }

    private ItemStack toggleItem(boolean enabled, String name, String desc, Material mat) {
        ItemStack item = new ItemStack(enabled ? Material.LIME_DYE : Material.RED_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(enabled ? ChatColor.GREEN + "✔ " + name : ChatColor.RED + "✘ " + name);
        List<String> lore = new ArrayList<String>();
        if (desc != null && !desc.isEmpty()) lore.add(ChatColor.GRAY + desc);
        lore.add("");
        lore.add(enabled ? ChatColor.GREEN + "" + ChatColor.BOLD + "ATIVADO" : ChatColor.RED + "" + ChatColor.BOLD + "DESATIVADO");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Clique para alternar!");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    // ==================== Vanish Core ====================

    public void toggleVanish(Player player) {
        if (isCooldown(player.getUniqueId())) {
            player.sendMessage(lang("cooldown"));
            return;
        }
        if (isVanished(player)) {
            revealPlayer(player);
            player.sendMessage(lang("vanish-disabled-msg"));
        } else {
            vanishPlayer(player);
            player.sendMessage(lang("vanish-enabled-msg"));
        }
    }

    public boolean isCooldown(UUID uuid) {
        return isOnCooldown(uuid);
    }

    public Set<UUID> getVanishedPlayers() {
        return Collections.unmodifiableSet(vanished);
    }

    public void vanishPlayer(Player player) {
        vanished.add(player.getUniqueId());
        scheduleNextTick(player, () -> {
            player.setInvisible(true);
            player.setAllowFlight(true);
            player.setFlying(player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR);
        });

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission("vanish.see")) {
                hideFrom(online, player);
            } else if (!online.getUniqueId().equals(player.getUniqueId())) {
                online.sendMessage(lang("vanish-staff-notify").replace("{player}", player.getName()).replace("{status}", langRaw("status-on")));
            }
        }

        logToFile("VANISH", player.getName(), "Vanished (entrou em vanish)");
    }

    public void revealPlayer(Player player) {
        vanished.remove(player.getUniqueId());
        savedVanish.remove(player.getUniqueId());
        scheduleNextTick(player, () -> {
            player.setInvisible(false);
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setFlying(false);
                player.setAllowFlight(false);
            }
        });

        for (Player online : Bukkit.getOnlinePlayers()) {
            showTo(online, player);
        }

        logToFile("VANISH", player.getName(), "Revealed (saiu do vanish)");
    }

    public boolean isVanished(Player player) {
        return vanished.contains(player.getUniqueId());
    }

    public boolean hasSilentChest(Player player) {
        return silentChestPlayers.contains(player.getUniqueId());
    }

    public void toggleSilentChest(Player player) {
        if (silentChestPlayers.contains(player.getUniqueId())) {
            silentChestPlayers.remove(player.getUniqueId());
        } else {
            silentChestPlayers.add(player.getUniqueId());
        }
    }

    // ==================== Events ====================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();

        for (UUID uuid : vanished) {
            Player v = Bukkit.getPlayer(uuid);
            if (v != null && !player.hasPermission("vanish.see")) {
                hideFrom(player, v);
            }
        }

        if (getConfig().getBoolean("settings.save-vanish-on-quit", true)) {
            if (savedVanish.contains(player.getUniqueId())) {
                vanishPlayer(player);
                savedVanish.remove(player.getUniqueId());
            }
        }

        if (player.hasPermission("vanish.see") && !vanished.isEmpty()) {
            player.sendMessage(lang("vanished-count").replace("{count}", String.valueOf(vanished.size())));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        if (isVanished(player)) {
            savedVanish.add(player.getUniqueId());
        } else {
            savedVanish.remove(player.getUniqueId());
        }
        saveVanishData();
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        Player player = (Player) e.getPlayer();
        if (!isVanished(player)) return;
        if (!hasSilentChest(player)) return;
        if (!getConfig().getBoolean("protection.silent-chest", true)) return;

        String type = e.getInventory().getType().name();
        // Silent chest handled server-side
    }

    // ==================== Protection Events ====================

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getPlayer().hasPermission("vanish.bypass.protection")) return;
        if (getConfig().getBoolean("protection.block-interact", true) && isVanished(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (e.getPlayer().hasPermission("vanish.bypass.protection")) return;
        if (getConfig().getBoolean("protection.block-break", true) && isVanished(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (e.getPlayer().hasPermission("vanish.bypass.protection")) return;
        if (getConfig().getBoolean("protection.block-place", true) && isVanished(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (e.getPlayer().hasPermission("vanish.bypass.protection")) return;
        if (getConfig().getBoolean("protection.item-drop", true) && isVanished(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        if (p.hasPermission("vanish.bypass.protection")) return;
        if (getConfig().getBoolean("protection.item-pickup", true) && isVanished(p)) e.setCancelled(true);
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent e) {
        if (getConfig().getBoolean("protection.mob-target", true) && e.getTarget() instanceof Player && isVanished((Player) e.getTarget())) e.setCancelled(true);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        Player p = (Player) e.getDamager();
        if (p.hasPermission("vanish.bypass.protection")) return;
        if (getConfig().getBoolean("protection.pvp", true) && isVanished(p)) e.setCancelled(true);
    }
}
