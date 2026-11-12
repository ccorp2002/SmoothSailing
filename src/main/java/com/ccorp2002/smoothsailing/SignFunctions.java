package com.ccorp2002.smoothsailing;

import com.ccorp2002.smoothsailing.detection.BlockConfigManager;
import com.ccorp2002.smoothsailing.detection.BlockProperties;
import com.ccorp2002.smoothsailing.detection.*;
import com.ccorp2002.smoothsailing.ship.*;
import com.ccorp2002.smoothsailing.utils.*;
import com.ccorp2002.smoothsailing.nmsutils.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.entity.Projectile;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.stream.Collectors;

public class SignFunctions implements Listener {
    public SignFunctions() {}

    public static String TURRET_HEADER = "[turret]";

    public static String HELM_HEADER = "[helm]";

    public static String THRUST_HEADER = "[thrust]";

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH, ignoreCancelled = true)
    public void onUseShipSignFunction(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (block.getState(false) instanceof org.bukkit.block.Sign signState) {
            final String HEADER = ChatColor.stripColor(signState.getLine(0));
            if ((!TURRET_HEADER.isEmpty() && TURRET_HEADER != null) && HEADER.equalsIgnoreCase(TURRET_HEADER)) {
                RUN_TURRET_AT(event);
            } else if ((!HELM_HEADER.isEmpty() && HELM_HEADER != null) && HEADER.equalsIgnoreCase(HELM_HEADER)) {
                RUN_HELM_AT(event);
            } else if ((!THRUST_HEADER.isEmpty() && THRUST_HEADER != null) && HEADER.equalsIgnoreCase(THRUST_HEADER)) {
                CHANGE_GEAR_AT(event, signState);
            }
        }
    }

    public static void CHANGE_GEAR_AT(final PlayerInteractEvent event, org.bukkit.block.Sign signState) {
        Block block = event.getClickedBlock();
        final Player player = event.getPlayer();
        ShipInstance ship = MobileBlockState.getShipFromLoc(block.getLocation());
        if (ship == null) {
            if (signState != null) signState.setLine(1,"");
            return;
        }
        event.setCancelled(true);
        int UP_DOWN = 0;
        if (event.getAction() == Action.LEFT_CLICK_BLOCK || player.isSneaking()) UP_DOWN = -1;
        else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) UP_DOWN = 1;
        else return;
        ship.cycleGears(UP_DOWN);
        if (signState != null) {
            String line = ChatColor.BLACK+"["+ChatColor.RESET+ ChatColor.BOLD + ChatColor.YELLOW + ship.getGear() + ChatColor.GOLD + "/" + ChatColor.RED + ship.getMaxGear() + ChatColor.RESET+ChatColor.BLACK+"]";
            signState.setLine(1,line);
        }

    }

    public static void RUN_HELM_AT(final PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        final Player player = event.getPlayer();
        ShipInstance ship = MobileBlockState.getShipFromLoc(block.getLocation());
        if (player.isSneaking() || ship == null) return;
        event.setCancelled(true);
        Boolean LEFT_RIGHT = null;
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) LEFT_RIGHT = Boolean.FALSE;
        else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) LEFT_RIGHT = Boolean.TRUE;
        else return;
        ship.physics.rotate(player,LEFT_RIGHT);
    }

    public static void RUN_TURRET_AT(final PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        final Player player = event.getPlayer();
        ShipInstance ship = MobileBlockState.getShipFromLoc(block.getLocation());
        if (player.isSneaking() || ship == null) return;
        event.setCancelled(true);
        RUN_TURRET_AT(event.getAction(), ship, player, block);
    }

    public static void RUN_TURRET_AT(final Action action, ShipInstance ship, Player player, Block block) {
        String LEFT_RIGHT = "NONE";
        if (action == Action.LEFT_CLICK_BLOCK) LEFT_RIGHT = "LEFT";
        else if (action == Action.RIGHT_CLICK_BLOCK) LEFT_RIGHT = "RIGHT";
        else return;
        Location CENTER = block.getLocation().toBlockLocation();
        if (ship.physics.moving || !ship.isNotProcessing()) return;
        Set<Block> FOUND_BLOCKS = com.ccorp2002.smoothsailing.detection.ShipDetector.detectBlocks(player,CENTER);
        FOUND_BLOCKS.add(block);
        if (ship.driver != null && ship.driver instanceof Player driver) {
            ShipPhysics.rotateSubship2d(ship,CENTER,LEFT_RIGHT,FOUND_BLOCKS);
        } else {
            ShipPhysics.rotateSubship2d(ship,CENTER,LEFT_RIGHT,FOUND_BLOCKS);
        }
    }
}
