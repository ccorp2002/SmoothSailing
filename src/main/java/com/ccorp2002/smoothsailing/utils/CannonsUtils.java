package com.ccorp2002.smoothsailing.utils;


import com.ccorp2002.smoothsailing.*;
import com.ccorp2002.smoothsailing.ship.*;
import com.ccorp2002.smoothsailing.utils.*;
import com.ccorp2002.smoothsailing.event.*;
import com.ccorp2002.smoothsailing.nmsutils.*;
import com.ccorp2002.smoothsailing.detection.*;
import com.ccorp2002.smoothsailing.detection.*;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;
import org.bukkit.entity.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;


import at.pavlov.cannons.Cannons;
import at.pavlov.cannons.cannon.Cannon;
import at.pavlov.cannons.API.CannonsAPI;
import at.pavlov.cannons.Enum.BreakCause;
import at.pavlov.cannons.cannon.CannonDesign;
import at.pavlov.cannons.cannon.CannonManager;


import java.util.*;

public class CannonsUtils implements Listener {
    private final Cannons PLUGIN;
    private final CannonsAPI API;
    public static CannonsUtils INTERFACE;

    public CannonsUtils() {
        SmoothSailingPlugin.INSTANCE.getLogger().info("==================================================");
        SmoothSailingPlugin.INSTANCE.getLogger().info("Cannons was found for Compatibility!");
        SmoothSailingPlugin.INSTANCE.getLogger().info("==================================================");
        PLUGIN = Cannons.getPlugin();
        API = PLUGIN.getCannonsAPI();
        INTERFACE = this;
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void rotateListener(ShipRotateEvent e) {
        ShipInstance ship = e.getShip();
        rotateCannons(ship,e.getRotation());
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void moveListener(ShipPreProcessMoveEvent e) {
        ShipInstance ship = e.getShip();
        moveCannons(ship,e.getLocation().toBlockLocation().toVector());
    }

    public static void sinkCannons(ShipInstance ship) {
        if (ship == null) return;
        Set<Cannon> cannons = getCannons(ship);
        if (cannons.isEmpty()) return;
        cannons.forEach(cannon -> CannonManager.getInstance().removeCannon(cannon.getUID(), false, true, BreakCause.Explosion));
    }


    public static void moveCannons(ShipInstance ship, Vector MOVEMENT) {
        if (ship == null) return;
        if (MOVEMENT == null) return;
        Set<Cannon> cannons = getCannons(ship);
        if (cannons.isEmpty()) return;
        for (Cannon c : cannons) {
            c.move(MOVEMENT);
        }
    }

    public static void rotateCannons(ShipInstance ship, StructureRotation ROTATION) {
        if (ship == null) return;
        if (ROTATION == null) return;
        Vector v = ship.vehicle.getLocation().toBlockLocation().toVector();
        Set<Cannon> cannons = getCannons(ship);
        if (cannons.isEmpty()) return;
        for (Cannon c : cannons) {
            switch (ROTATION) {
                case CLOCKWISE_90 -> c.rotateRight(v);
                case COUNTERCLOCKWISE_90 -> c.rotateLeft(v);
            }
        }
    }

    public static Set<Cannon> getCannons(ShipInstance ship) {
        if (ship == null) return new HashSet<Cannon>(0);
        List<Location> shipLocations = new ArrayList<>(ship.BLOCK_STATES.size());
        for (MobileBlockState loc : ship.BLOCK_STATES) {
            shipLocations.add(loc.getLocation());
        }
        return CannonManager.getCannonsByLocations(shipLocations);
    }

    public static Set<Cannon> getCannons(Collection<MobileBlockState> STATES) {
        if (STATES == null || STATES.isEmpty()) return new HashSet<Cannon>(0);
        List<Location> shipLocations = new ArrayList<>(STATES.size());
        for (MobileBlockState loc : STATES) {
            shipLocations.add(loc.getLocation());
        }
        return CannonManager.getCannonsByLocations(shipLocations);
    }

    public static void rotateCannons(Location center, StructureRotation ROTATION, Collection<MobileBlockState> STATES) {
        if (center == null) return;
        if (center.getWorld() == null) return;
        if (ROTATION == null) return;
        Vector v = center.toBlockLocation().toVector();
        Set<Cannon> cannons = getCannons(STATES);
        if (cannons.isEmpty()) return;
        for (Cannon c : cannons) {
            switch (ROTATION) {
                case CLOCKWISE_90 -> c.rotateRight(v);
                case COUNTERCLOCKWISE_90 -> c.rotateLeft(v);
            }
        }
    }
}