package com.ccorp2002.smoothsailing;

import com.ccorp2002.smoothsailing.blockconfig.BlockConfigManager;
import com.ccorp2002.smoothsailing.customships.ShipWheelData;
import com.ccorp2002.smoothsailing.customships.ShipWheelManager;
import com.ccorp2002.smoothsailing.utils.claims.IClaimInterface;
import com.ccorp2002.smoothsailing.utils.claims.WorldGuardInterface;
import com.ccorp2002.smoothsailing.utils.claims.TownyInterface;
import com.ccorp2002.smoothsailing.utils.CannonsUtils;
import com.ccorp2002.smoothsailing.nmsutils.ShipSteeringListener;
import com.ccorp2002.smoothsailing.nmsutils.NmsUtils;
import com.ccorp2002.smoothsailing.nmsutils.CEUtils;
import com.ccorp2002.smoothsailing.region.TaskUtils;
import com.ccorp2002.smoothsailing.ship.ShipInstance;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

public class SmoothSailingPlugin extends JavaPlugin {
    public static boolean DEBUG = false;
    public static boolean DEBUG_DUMP = false;
    public static boolean PACKET_DEBUG = false;
    public static boolean STEERING_DEBUG = false;
    public static boolean CANNONS_FOUND = false;
    public static boolean NMS_TELEPORT = false;
    public static SmoothSailingPlugin INSTANCE = null;
    public List<IClaimInterface> CLAIM_INTEFACES;
    public ShipListener shipListener;
    public ShipSteeringListener steeringListener = null;
    public ShipWheelManager shipWheelManager;
    public boolean GSIT_FOUND = false;
    public boolean CRAFT_ENGINE_FOUND = false;
    public boolean PROTOCOL = false;
    public static final double MAX_POSSIBLE_HEALTH = Bukkit.spigot().getSpigotConfig().getDouble("settings.attribute.maxHealth",1024.0d);

    @Override
    public void onLoad() {
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            if (this.getConfig().getConfigurationSection("worldguard").getBoolean("enabled",false)) {
                WorldGuardInterface.registerFlags();
            }
        }
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        TaskUtils.initialize(INSTANCE,true);
        this.shipWheelManager = new ShipWheelManager(INSTANCE);
        if ((Bukkit.getMinecraftVersion().contains("26.") || Bukkit.getMinecraftVersion().contains("1.21.")) && !Bukkit.getMinecraftVersion().contains("26.3")) {
            NMS_TELEPORT = true;
        } else {
            NMS_TELEPORT = false;
        }
        try {
            this.GSIT_FOUND = Bukkit.getPluginManager().getPlugin("GSit") != null;
        } catch (Throwable thr) {}
        try {
            this.CRAFT_ENGINE_FOUND = Bukkit.getPluginManager().getPlugin("CraftEngine") != null;
            com.ccorp2002.smoothsailing.nmsutils.NmsUtils.HAS_CRAFT_ENGINE = this.CRAFT_ENGINE_FOUND;
            if (this.CRAFT_ENGINE_FOUND) {
                com.ccorp2002.smoothsailing.nmsutils.NmsUtils.SENT_BLOCKS.clear();
                com.ccorp2002.smoothsailing.nmsutils.NmsUtils.CE_UTILS = new com.ccorp2002.smoothsailing.nmsutils.CEUtils();
            }
        } catch (Throwable thr) {}
        this.CLAIM_INTEFACES = new ArrayList<>();
        if (Bukkit.getPluginManager().getPlugin("Towny") != null) {
            this.CLAIM_INTEFACES.add(new TownyInterface());
        }
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            if (this.getConfig().getConfigurationSection("worldguard").getBoolean("enabled",false)) {
                this.CLAIM_INTEFACES.add(new WorldGuardInterface());
            }
        }
        DEBUG = this.getConfig().getConfigurationSection("debug").getBoolean("enabled",false);
        saveDefaultConfig();

        // Load global physics config
        ShipInstance.loadGlobalPhysicsConfig(INSTANCE);

        // Initialize block configuration manager
        BlockConfigManager.initialize(this);
        BlockConfigManager.getInstance().loadConfig();

        // Check for ProtocolLib for WASD input detection
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") == null || !PROTOCOL) {
            if (PROTOCOL) {
                getLogger().warning("==================================================");
                getLogger().warning("ProtocolLib was not found! WASD ship controls may be less responsive,");
                getLogger().warning("Although the plugin will continue to load.");
                getLogger().warning("==================================================");
            } 
        } else {
            if (PROTOCOL) steeringListener = new ShipSteeringListener(INSTANCE);
            // Initialize steering listener (ProtocolLib WASD detection)
        }

        // Initialize and register ShipListener
        shipListener = new ShipListener(INSTANCE);
        Bukkit.getPluginManager().registerEvents(shipListener, INSTANCE);
        try {
            CANNONS_FOUND = Bukkit.getPluginManager().getPlugin("Cannons") != null;
            if (CANNONS_FOUND) Bukkit.getPluginManager().registerEvents(new CannonsUtils(), INSTANCE);
        } catch (Throwable thr) {}

        // Initialize ShipWheelManager for custom block ships and load saved wheels
        getLogger().info("SmoothSailing enabled.");

        // First destroy all registered ships (cleans up properly)
        int shipCount = ShipRegistry.getAllShips().size();
        ShipRegistry.destroyAll();

        // Then clean up any orphaned entities with ship tags
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (ShipListener.isShipEntity(entity.getPersistentDataContainer())) {
                    entity.remove();
                }
                if (ShipListener.isChair(entity)) {
                    entity.remove();
                }
            }
        }
        getLogger().info("Cleared " + shipCount + " stale Ship(s) & Ship-Entities");

        int removedCount = 0;
        for (World world : Bukkit.getWorlds()) {
            removedCount+=world.getForceLoadedChunks().size();
            NmsUtils.removeChunksToLoad(world,world.getForceLoadedChunks());
        }
        getLogger().info("Cleared " + removedCount + " force-loaded chunks");
    }

    @Override
    public void onDisable() {
        ShipRegistry.destroyAll();
        
        int removedCount = 0;

        for (World world : Bukkit.getWorlds()) {
            removedCount+=world.getForceLoadedChunks().size();
            NmsUtils.removeChunksToLoad(world,world.getForceLoadedChunks());
        }

        TaskUtils.cancelTasks();

        getLogger().info("Cleared " + removedCount + " force-loaded chunks");
        
        if (this.CLAIM_INTEFACES != null) this.CLAIM_INTEFACES.clear();
        
        for (ShipInstance ship : ShipRegistry.getAllShips()) {
            ship.destroy();
            ShipRegistry.unregister(ship);
        }

        if (shipWheelManager != null) {
            shipWheelManager.placedWheels.clear();
        }
        if (shipListener != null) {
            ShipListener.shutdown();
        }
        steeringListener = null;
        shipWheelManager = null;
        shipListener = null;
        getLogger().info("SmoothSailing disabled.");
        com.ccorp2002.smoothsailing.nmsutils.NmsUtils.SENT_BLOCKS.clear();
        INSTANCE = null;
    }

    public static SmoothSailingPlugin getInstance() {
        return getPlugin();
    }

    public static SmoothSailingPlugin getPlugin() {
        return INSTANCE;
    }

    public ShipListener getShipListener() {
        return shipListener;
    }

    public static ShipListener shipListener() {
        return getPlugin().getShipListener();
    }

    public ShipWheelManager getShipWheelManager() {
        return shipWheelManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("release") || command.getName().equalsIgnoreCase("unpilot")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can use this command.");
                return true;
            }
            if (!sender.hasPermission("smoothsailing.release")) {
                sender.sendMessage("You don't have permission to use this command.");
                return true;
            }

            int removedCount = 0;

            // First destroy all registered ships (cleans up properly)
            for (ShipInstance SHIP : ShipRegistry.getAllShips()) {
                if (player.equals(SHIP.driver)) {
                    SHIP.destroy();
                    removedCount++;
                }
            }
            sender.sendMessage("Unpiloted " + removedCount + " registered Ship(s)");
            return true;
        }
        if (command.getName().equalsIgnoreCase("smoothsailing")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("smoothsailing.reload") && !sender.hasPermission("smoothsailing.admin")) {
                    sender.sendMessage("You don't have permission to reload this plugin.");
                    return true;
                }
                reloadConfig();
                // Reload global physics config
                ShipInstance.loadGlobalPhysicsConfig(this);
                DEBUG = this.getConfig().getConfigurationSection("debug").getBoolean("enabled",false);
                // Reload block configuration
                BlockConfigManager.getInstance().reloadConfig();
                sender.sendMessage("SmoothSailing config reloaded!");
                return true;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("release")) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Only players can use this command.");
                    return true;
                }
                if (!sender.hasPermission("smoothsailing.release")) {
                    sender.sendMessage("You don't have permission to use this command.");
                    return true;
                }

                int removedCount = 0;

                // First destroy all registered ships (cleans up properly)
                for (ShipInstance SHIP : ShipRegistry.getAllShips()) {
                    if (player.equals(SHIP.driver)) {
                        SHIP.destroy();
                        removedCount++;
                    }
                }
                sender.sendMessage("Unpiloted " + removedCount + " registered Ship(s)");
                return true;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("cruise")) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Only players can use this command.");
                    return true;
                }
                if (!player.hasPermission("smoothsailing.admin.cruise")) {
                    sender.sendMessage("You don't have permission to use this command.");
                    return true;
                }
                ShipInstance ship = ShipListener.findShipByPlayer(player);
                for (int i = 0; i < 50; i++) {
                    com.ccorp2002.smoothsailing.region.TaskUtils.getRegionalScheduler(ship.vehicle.getLocation()).runTaskLater(()->{
                        ship.isForwardPressed = true;
                        ship.physics.currentSpeed = 2;
                        ship.physics.update();
                    }, (Math.abs(i)*(ship.physics.MOVEMENT_COOLDOWN+7L))-Math.abs(i));
                }
                return true;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("releaseall")) {
                if (!sender.hasPermission("smoothsailing.admin")) {
                    sender.sendMessage("You don't have permission to use this command.");
                    return true;
                }
                if (!sender.hasPermission("smoothsailing.releaseall")) {
                    sender.sendMessage("You don't have permission to use this command.");
                    return true;
                }


                int removedCount = 0;

                // First destroy all registered ships (cleans up properly)
                int shipCount = ShipRegistry.getAllShips().size();
                for (ShipWheelData wheelData : new java.util.ArrayList<>(shipWheelManager.getWheels())) {
                    if (wheelData.isAssembled()) {
                        // Pass null for player - messages not needed for batch operation
                        shipWheelManager.disassembleShip(null, wheelData, true);
                    }
                }
                ShipRegistry.destroyAll();

                // Then clean up any orphaned entities with ship tags
                for (World world : Bukkit.getWorlds()) {
                    for (Entity entity : world.getEntities()) {
                        if (ShipListener.isShipEntity(entity.getPersistentDataContainer())) {
                            entity.remove();
                            removedCount++;
                        }
                        if (ShipListener.isChair(entity)) {
                            entity.remove();
                            removedCount++;
                        }
                    }
                }

                sender.sendMessage("Unpiloted " + shipCount + " registered Ship(s)");
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("smoothsailing")) {
            return null;
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Complete subcommands based on permissions
            List<String> subcommands = new ArrayList<>();
            if (sender.hasPermission("smoothsailing.reload")) subcommands.add("reload");
            if (sender.hasPermission("smoothsailing.release")) subcommands.add("release");
            if (sender.hasPermission("smoothsailing.cruise")) subcommands.add("cruise");
            if (sender.hasPermission("smoothsailing.admin")) subcommands.add("releaseall");

            for (String sub : subcommands) {
                if (sub.toLowerCase(Locale.ENGLISH).startsWith(args[0].toLowerCase(Locale.ENGLISH))) {
                    completions.add(sub);
                }
            }
        }

        return completions;
    }
}
