package com.ccorp2002.smoothsailing.ship;

import com.ccorp2002.smoothsailing.*;
import com.ccorp2002.smoothsailing.ShipConfig;
import com.ccorp2002.smoothsailing.ShipRegistry;
import com.ccorp2002.smoothsailing.nmsutils.*;
import com.ccorp2002.smoothsailing.ShipListener;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles collision detection and response for a ship.
 * Detects terrain, entity, and ship-to-ship collisions and calculates appropriate forces.
 */
public class ShipCollision {
    private final ShipInstance ship;

    public ShipCollision(ShipInstance ship) {
        this.ship = ship;
    }
    public Location detect(Location offset) {
        return detect(offset,false);
    }

    /**
     * Detect all collisions and accumulate forces.
     * Only runs if ship is moving or has collision force from previous tick.
     */
    public Location detect(Location offset, boolean checkForLand) {
        final Location original = offset.clone();
        Location newOffset = original.clone();
        Player pilot = null;
        if (ship.driver instanceof Player) {
            pilot = (Player)ship.driver;
        }
        //Vector3f totalForce = new Vector3f(0, 0, 0);
        int xzCollisionCount = 0;
        int collisionCount = 0;
        int groundContact = 0;
        int xCollision = 0;
        int zCollision = 0;
        boolean canEnterAt = true;
        if (!ship.isLandcraft || !checkForLand) {
            int MIN_BY = ship.minY();
            if (MIN_BY <= ship.getWorld().getMinHeight()) {
                newOffset.setX(0);
                newOffset.setY(0);
                newOffset.setZ(0);
                return newOffset;
            }
            return newOffset;
        } else if (checkForLand && ship.isLandcraft) {
            int obx = offset.getBlockX();
            int obz = offset.getBlockZ();

            int MIN_BY = ship.minY();
            if (MIN_BY <= ship.getWorld().getMinHeight()) {
                newOffset.setX(0);
                newOffset.setY(0);
                newOffset.setZ(0);
                return newOffset;
            }
            List<MobileBlockState> FILTERED_LIST = ship.BLOCK_STATES.stream().parallel()
                .filter(p -> p.by() <= MIN_BY+2 && p.getBlockData().getMaterial() == Material.GRINDSTONE)
                .collect(Collectors.toList());
            for (MobileBlockState bs : FILTERED_LIST) {
                Location newLoc = bs.getLocation().clone().add(offset.toVector()).toBlockLocation();
                if (!newLoc.isChunkLoaded()) {
                    canEnterAt = false;
                    newOffset.setX(0);
                    newOffset.setY(0);
                    newOffset.setZ(0);
                    return newOffset;
                }
                Block block = NmsUtils.getBukkitBlockFast(newLoc);
                if (pilot != null) {
                    for (final com.ccorp2002.smoothsailing.utils.claims.IClaimInterface ICLAIM : SmoothSailingPlugin.INSTANCE.CLAIM_INTEFACES) {
                        if (!canEnterAt) break;
                        if (!ICLAIM.canEnterAt(pilot,newLoc)) canEnterAt = false;
                        if (!NmsUtils.withinWorldBorder(newLoc)) canEnterAt = false;
                        
                    }
                }
                if (!canEnterAt) {
                    newOffset.setX(0);
                    newOffset.setY(0);
                    newOffset.setZ(0);
                    return newOffset; 
                }
                if (MobileBlockState.doesShipHave(ship,newLoc)) {
                    continue;
                }
                if ((block.isSolid() && (!ShipPhysics.isDataMedium(block))) && !(ShipPhysics.LANDCRAFT_PASSTHROUGH.contains(block.getType()) && ship.isLandcraft)) {
                    if (ship.isLandcraft && offset.getBlockY() != 0 && (block.isSolid()) && (block.getX() == bs.bx() || block.getZ() == bs.bz()) && bs.getBlockData().getMaterial() == Material.GRINDSTONE) {
                        Block checked = bs.getLocation().getBlock().getRelative(0,offset.getBlockY(),0);
                        boolean foundGround = false;
                        for (int checkDist = 0; checkDist <= (Math.abs(offset.getBlockY())); checkDist++) {
                            if (!MobileBlockState.doesShipHave(ship,checked.getLocation().toBlockLocation()) && checked.isSolid() && !foundGround) {
                                foundGround = true;
                            } else if (!foundGround) checked = checked.getRelative(0,offset.getBlockY(),0);
                        }
                        if (foundGround) groundContact++;
                    }
                    if (ship.isLandcraft && offset.getBlockX() != 0 && (block.isSolid())) {
                        Block checked = bs.getLocation().getBlock().getRelative(offset.getBlockX(),0,0);
                        boolean foundGround = false;
                        for (int checkDist = 0; checkDist <= (Math.abs(offset.getBlockX())); checkDist++) {
                            if (!MobileBlockState.doesShipHave(ship,checked.getLocation().toBlockLocation()) && checked.isSolid() && !foundGround) {
                                foundGround = true;
                            } else if (!foundGround) checked = checked.getRelative(offset.getBlockX(),0,0);
                        }
                        if (foundGround) {
                            xCollision += offset.getBlockX();
                            xzCollisionCount++;
                        }
                    }
                    if (ship.isLandcraft && offset.getBlockZ() != 0 && (block.isSolid())) {
                        Block checked = bs.getLocation().getBlock().getRelative(0,0,offset.getBlockZ());
                        boolean foundGround = false;
                        for (int checkDist = 0; checkDist <= (Math.abs(offset.getBlockZ())); checkDist++) {
                            if (!MobileBlockState.doesShipHave(ship,checked.getLocation().toBlockLocation()) && checked.isSolid() && !foundGround) {
                                foundGround = true;
                            } else if (!foundGround) checked = checked.getRelative(0,0,offset.getBlockZ());
                        }
                        if (foundGround) {
                            zCollision += offset.getBlockZ();
                            xzCollisionCount++;
                        }
                    }
                }
            }
        }
        if (collisionCount > 0 || xzCollisionCount > 0) {
            if (ship.isLandcraft) {
                newOffset.setY(0d);
                if (groundContact >= 2) newOffset.setY(0);
                else newOffset.setY(-1);
                if (newOffset.getBlockX() != 0 || newOffset.getBlockZ() != 0) {
                    if (newOffset.getBlockX() != 0) newOffset.setX((double)ShipPhysics.CLAMP_VALUE((float)newOffset.getX(),-1,1));
                    if (newOffset.getBlockZ() != 0) newOffset.setZ((double)ShipPhysics.CLAMP_VALUE((float)newOffset.getZ(),-1,1));
                    newOffset.setY(1);
                    if (groundContact >= 2 && !(collisionCount > 0)) newOffset.setY(0);
                    else if (groundContact < 2 || collisionCount == 0) newOffset.setY(-1);
                    else if (collisionCount > 0 && offset.getBlockY() <= 0) {
                        newOffset.setX(0);
                        newOffset.setY(1);
                        newOffset.setZ(0);
                    }
                }
                if (groundContact >= 2) newOffset.setY(0d);
                else if (groundContact < 1) {
                    newOffset.setY(-1d);
                }
                if (xzCollisionCount >= 2) newOffset.setY(1d);
                newOffset.setX((double)ShipPhysics.CLAMP_VALUE((float)original.getX(),-1,1));
                newOffset.setZ((double)ShipPhysics.CLAMP_VALUE((float)original.getZ(),-1,1));
                ship.physics.currentSpeed = 0f; 
                ship.physics.currentYVelocity = 0f;
            } else {
                ship.physics.currentSpeed = 0f; 
                ship.physics.currentYVelocity = 0f;
            }
            if (SmoothSailingPlugin.DEBUG_DUMP) {
                System.out.println("NUM GROUND CONTACT: "+groundContact);
                System.out.println("NUM Y-COLLISION COUNT: "+collisionCount);
                System.out.println("NUM XZ-COLLISION COUNT: "+xzCollisionCount);
            }
            //System.out.println("x: "+newOffset.getBlockX()+" y: "+newOffset.getBlockY()+" z: "+newOffset.getBlockZ());
            //System.out.println("Collision: "+collisionCount);
            return newOffset;
        } else {
            return newOffset;
        }
    }
}
