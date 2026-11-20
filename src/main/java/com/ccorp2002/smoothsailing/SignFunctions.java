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
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;

public class SignFunctions implements Listener {
    public SignFunctions() {}

    public static String TURRET_HEADER = "[turret]";

    public static String HELM_HEADER = "[helm]";

    public static String THRUST_HEADER = "[thrust]";

    public static String REMOTE_HEADER = "[remote]";

    public static String EXTEND_HEADER = "[extend]";

    public static String DETACH_HEADER = "[detach]";

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH, ignoreCancelled = true)
    public void onUseShipSignFunction(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (block.getState(false) instanceof Sign signState) {
            final String HEADER = ChatColor.stripColor(signState.getLine(0));
            if ((!TURRET_HEADER.isEmpty() && TURRET_HEADER != null) && HEADER.equalsIgnoreCase(TURRET_HEADER)) {
                RUN_TURRET_AT(event);
            } else if ((!HELM_HEADER.isEmpty() && HELM_HEADER != null) && HEADER.equalsIgnoreCase(HELM_HEADER)) {
                RUN_HELM_AT(event);
            } else if ((!THRUST_HEADER.isEmpty() && THRUST_HEADER != null) && HEADER.equalsIgnoreCase(THRUST_HEADER)) {
                CHANGE_GEAR_AT(event, signState);
            } else if ((!EXTEND_HEADER.isEmpty() && EXTEND_HEADER != null) && HEADER.equalsIgnoreCase(EXTEND_HEADER)) {
                RUN_EXTEND_AT(event);
            } else if ((!DETACH_HEADER.isEmpty() && DETACH_HEADER != null) && HEADER.equalsIgnoreCase(DETACH_HEADER)) {
                RUN_DETACH_AT(event);
            }
        }
    }

    public static void REMOTE_SIGN_AT(final PlayerInteractEvent event, Sign signState) {
        Block block = event.getClickedBlock();
        final Player player = event.getPlayer();
        ShipInstance ship = MobileBlockState.getShipFromLoc(signState.getLocation());
        if (ship == null) {
            return;
        }
        event.setCancelled(true);
        Collection<MobileBlockState> STATES = ship.getBlockName("SIGN");
        String targetText = "";
        for (MobileBlockState st : STATES) {
            Block b = st.getBlock();
            if (!(b.getState(false) instanceof Sign ts)) continue;
            for (int i = 0; i < 4; i++) {
                if (targetText.isEmpty()) targetText = ChatColor.stripColor(signState.getLine(i));
                if (isEqualSign(ts, targetText)) {
                    if (!isForbidden(ts)) {
                        final PlayerInteractEvent clicked = new PlayerInteractEvent(event.getPlayer(), event.getAction(), ItemStack.empty(), b, event.getBlockFace());
                        if (i == 0) {
                            if ((!TURRET_HEADER.isEmpty() && TURRET_HEADER != null) && targetText.equalsIgnoreCase(TURRET_HEADER)) {
                                RUN_TURRET_AT(clicked);
                            } else if ((!EXTEND_HEADER.isEmpty() && EXTEND_HEADER != null) && targetText.equalsIgnoreCase(EXTEND_HEADER)) {
                                RUN_EXTEND_AT(clicked);
                            } else if ((!DETACH_HEADER.isEmpty() && DETACH_HEADER != null) && targetText.equalsIgnoreCase(DETACH_HEADER)) {
                                RUN_DETACH_AT(clicked);
                            } else {
                                Bukkit.getServer().getPluginManager().callEvent(clicked);
                            }
                        } else {
                            Bukkit.getServer().getPluginManager().callEvent(clicked);
                        }
                    }
                }

            }
        }
    }

    public static boolean isForbidden(Sign test) {
        for (int i = 0; i < 4; i++) {
            String t = ChatColor.stripColor(test.getLine(i));
            if (t.isEmpty()) continue;
            else if (t.equalsIgnoreCase(REMOTE_HEADER)) return true;   
            else if (t.equalsIgnoreCase(HELM_HEADER)) return true;
        }
        return false;
    }

    private static boolean isEqualSign(Sign test, String target) {
        if (target == null || target.isEmpty()) return false;
        return (!ChatColor.stripColor(test.getLine(0)).equalsIgnoreCase(REMOTE_HEADER)) && (ChatColor.stripColor(test.getLine(0)).equalsIgnoreCase(target)
                || ChatColor.stripColor(test.getLine(1)).equalsIgnoreCase(target)
                || ChatColor.stripColor(test.getLine(2)).equalsIgnoreCase(target)
                || ChatColor.stripColor(test.getLine(3)).equalsIgnoreCase(target));
    }

    public static void CHANGE_GEAR_AT(final PlayerInteractEvent event, Sign signState) {
        Block block = event.getClickedBlock();
        final Player player = event.getPlayer();
        ShipInstance ship = MobileBlockState.getShipFromLoc(signState.getLocation());
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
            signState.update(false,false);
        }
    }

    public static void RUN_HELM_AT(final PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        final Player player = event.getPlayer();
        ShipInstance ship = MobileBlockState.getShipFromLoc(block.getLocation());
        if (ship == null) return;
        event.setCancelled(true);
        Boolean LEFT_RIGHT = null;
        if (event.getAction() == Action.LEFT_CLICK_BLOCK || player.isSneaking()) LEFT_RIGHT = Boolean.FALSE;
        else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) LEFT_RIGHT = Boolean.TRUE;
        else return;
        ship.physics.rotate(player,LEFT_RIGHT);
    }

    public static void RUN_TURRET_AT(final PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        final Player player = event.getPlayer();
        ShipInstance ship = MobileBlockState.getShipFromLoc(block.getLocation());
        if (ship == null) return;
        event.setCancelled(true);
        RUN_TURRET_AT(event.getAction(), ship, player, block);
    }

    public static void RUN_TURRET_AT(final Action action, ShipInstance ship, Player player, Block block) {
        String LEFT_RIGHT = "NONE";
        if (action == Action.LEFT_CLICK_BLOCK || player.isSneaking()) LEFT_RIGHT = "LEFT";
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

    public static void RUN_EXTEND_AT(final PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        final Player player = event.getPlayer();
        ShipInstance ship = MobileBlockState.getShipFromLoc(block.getLocation());
        if (ship == null || player.isSneaking()) return;
        if (!(block.getState(false) instanceof Sign signState)) return;
        event.setCancelled(true);
        RUN_EXTEND_AT(event.getAction(), ship, player, signState);
    }

    public static void RUN_EXTEND_AT(final Action action, ShipInstance ship, Player player, Sign signState) {
        if (ship.physics.moving || !ship.isNotProcessing()) return;
        Location CENTER = signState.getLocation().toBlockLocation();
        Vector AXIS = GET_SUBAXIS(signState,signState.getLine(1),0);
        if (action == Action.LEFT_CLICK_BLOCK) AXIS = AXIS.multiply(new Vector(-1,-1,-1));
        else if (action == Action.RIGHT_CLICK_BLOCK) AXIS = AXIS.multiply(new Vector(1,1,1));
        else return;
        Set<Block> FOUND_BLOCKS = com.ccorp2002.smoothsailing.detection.ShipDetector.detectBlocks(player,CENTER,7500,false);
        if (ship.driver != null && ship.driver instanceof Player driver) {
            ShipPhysics.extendSubship2d(ship,CENTER,AXIS,FOUND_BLOCKS,true);
        } else {
            ShipPhysics.extendSubship2d(ship,CENTER,AXIS,FOUND_BLOCKS,true);
        }
    }

    public static void RUN_DETACH_AT(final PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        final Player player = event.getPlayer();
        ShipInstance ship = MobileBlockState.getShipFromLoc(block.getLocation());
        if (ship == null || player.isSneaking()) return;
        if (!(block.getState(false) instanceof Sign signState)) return;
        event.setCancelled(true);
        RUN_DETACH_AT(event.getAction(), ship, player, signState);
    }

    public static void RUN_DETACH_AT(final Action action, ShipInstance ship, Player player, Sign signState) {
        if (ship.physics.moving || !ship.isNotProcessing()) return;
        Location CENTER = signState.getLocation().toBlockLocation();
        Vector AXIS = GET_SUBAXIS(signState,signState.getLine(1),15);
        if (action == Action.LEFT_CLICK_BLOCK) AXIS = AXIS.multiply(new Vector(-3,-3,-3));
        else if (action == Action.RIGHT_CLICK_BLOCK) AXIS = AXIS.multiply(new Vector(3,3,3));
        else return;
        Set<Block> FOUND_BLOCKS = com.ccorp2002.smoothsailing.detection.ShipDetector.detectBlocks(player,CENTER,7500,false);
        if (ship.driver != null && ship.driver instanceof Player driver) {
            ShipPhysics.extendSubship2d(ship,CENTER,AXIS,FOUND_BLOCKS,false);
        } else {
            ShipPhysics.extendSubship2d(ship,CENTER,AXIS,FOUND_BLOCKS,false);
        }
    }

    public static Vector GET_SUBAXIS(Sign sign, String axis, int maxMove) {
        Vector vector = new Vector(0, 0, 0);
        String[] coords = (ChatColor.stripColor(axis)).split(",");
        if (maxMove <= 0) maxMove = 5;
        if (coords.length == 0 || coords[0].length() <= 0) {
            return vector;
        }
        try {
            if (coords.length == 1) {
                vector = vector.setY(Integer.parseInt(coords[0]));
            }
        } catch (Throwable thr) {}
        try {
            if (coords.length == 2) {
                vector = vector.setX(Integer.parseInt(coords[0]));
                vector = vector.setZ(Integer.parseInt(coords[1]));
            }
        } catch (Throwable thr) {}
        try {
            if (coords.length >= 3) {
                vector = vector.setX(Integer.parseInt(coords[0]));
                vector = vector.setY(Integer.parseInt(coords[1]));
                vector = vector.setZ(Integer.parseInt(coords[2]));
            }
        } catch (Throwable thr) {}
        int dLeftRight = vector.getBlockX();
        int dBackwardForward = vector.getBlockZ();
        if (dLeftRight > maxMove)
            dLeftRight = maxMove;
        if (dLeftRight < -maxMove)
            dLeftRight = -maxMove;
        if (vector.getY() > maxMove)
            vector = vector.setY(maxMove);
        if (vector.getY() < -maxMove)
            vector = vector.setY(-maxMove);
        if (dBackwardForward > maxMove)
            dBackwardForward = maxMove;
        if (dBackwardForward < -maxMove)
            dBackwardForward = -maxMove;
        switch (sign.getRawData()) {
            case 0x3:
                // North
                vector = vector.setX(dLeftRight);
                vector = vector.setZ(-dBackwardForward);
                break;
            case 0x2:
                // South
                vector = vector.setX(-dLeftRight);
                vector = vector.setZ(dBackwardForward);
                break;
            case 0x4:
                // East
                vector = vector.setX(dBackwardForward);
                vector = vector.setZ(dLeftRight);
                break;
            case 0x5:
                // West
                vector = vector.setX(-dBackwardForward);
                vector = vector.setZ(-dLeftRight);
                break;
        }
        return vector;
    }
}
