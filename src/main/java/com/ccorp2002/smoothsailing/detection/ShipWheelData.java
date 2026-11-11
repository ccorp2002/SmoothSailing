package com.ccorp2002.smoothsailing.detection;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Slime;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks data for a placed ship wheel block.
 * Associates a player head block with its orientation and optional assembled ship.
 */
public class ShipWheelData {
    /**
     * Safely parses a BlockFace from a map, returning a default value on failure.
     */
    public static BlockFace safeBlockFace(Map<?, ?> map, String key, BlockFace defaultValue) {
        Object val = map.get(key);
        if (val == null) return defaultValue;
        try {
            return BlockFace.valueOf(val.toString().toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    public Location blockLocation;
    public BlockFace facing;
    public UUID assembledShipUUID;  // UUID of ship if assembled, null if not

    // Detection preview data
    public Set<Location> lastDetectedBlocks;  // Blocks from last detection preview
    public BukkitTask particleTask;  // Active particle visualization task
    public int lastDetectedBlockCount;  // Block count from last detection
    public int lastDetectedWeight;  // Total weight from last detection
    public int lastDetectedPositiveWeight;  // Positive weight sum (for health calculation)

    // Categorized blocks for colored particle visualization
    public Set<Location> lastDetectedRegularBlocks;  // Non-seat blocks (white particles)
    public Set<Location> lastDetectedSeatBlocks;     // Passenger seat blocks (orange particles)
    public Location lastDetectedDriverSeat;          // Driver seat block (red particles)
    public int lastDetectedSeatCount;                // Total seat count
    // Flag to prevent clearing conflicts when menu is about to reopen
    public boolean pendingMenuReopen;

    // Health tracking for assembled ships (for Ship Info display)
    public double lastCurrentHealth;  // Current health when assembled
    public double lastMaxHealth;      // Max health when assembled

    // Buoyancy calculation data (for Ship Info display)
    public float lastMinY;             // Bottom of ship relative to wheel
    public float lastSurfaceOffset;    // Calculated surface offset

    public ShipWheelData(Location blockLocation, BlockFace facing) {
        this.blockLocation = blockLocation.clone();
        this.facing = facing;
        this.assembledShipUUID = null;
        this.lastDetectedBlocks = null;
        this.particleTask = null;
        this.lastDetectedBlockCount = 0;
        this.lastDetectedWeight = 0;
    }

    public Location getBlockLocation() {
        return blockLocation.clone();
    }

    public BlockFace getFacing() {
        return facing;
    }

    /**
     * Updates the wheel's block location and facing direction.
     * Used when a ship is disassembled at a different location than where it was assembled.
     */
    public void updateBlockLocation(Location newLocation, BlockFace newFacing) {
        this.blockLocation = newLocation.clone();
        this.facing = newFacing;
    }

    public UUID getAssembledShipUUID() {
        return assembledShipUUID;
    }

    public void setAssembledShipUUID(UUID shipUUID) {
        this.assembledShipUUID = shipUUID;
    }

    public boolean isAssembled() {
        return assembledShipUUID != null;
    }

    public Set<Location> getLastDetectedBlocks() {
        return lastDetectedBlocks;
    }

    public void setLastDetectedBlocks(Set<Location> blocks) {
        this.lastDetectedBlocks = blocks;
    }

    public int getLastDetectedBlockCount() {
        return lastDetectedBlockCount;
    }

    public int getLastDetectedWeight() {
        return lastDetectedWeight;
    }

    public int getLastDetectedPositiveWeight() {
        return lastDetectedPositiveWeight;
    }

    public void setLastDetectedStats(int blockCount, int totalWeight, int positiveWeight) {
        this.lastDetectedBlockCount = blockCount;
        this.lastDetectedWeight = totalWeight;
        this.lastDetectedPositiveWeight = positiveWeight;
    }

    public Set<Location> getLastDetectedRegularBlocks() {
        return lastDetectedRegularBlocks;
    }

    public Set<Location> getLastDetectedSeatBlocks() {
        return lastDetectedSeatBlocks;
    }

    public Location getLastDetectedDriverSeat() {
        return lastDetectedDriverSeat;
    }

    public int getLastDetectedSeatCount() {
        return lastDetectedSeatCount;
    }

    public void setLastDetectedBlockCategories(Set<Location> regularBlocks, Set<Location> seatBlocks, Location driverSeat) {
        this.lastDetectedRegularBlocks = regularBlocks;
        this.lastDetectedSeatBlocks = seatBlocks;
        this.lastDetectedDriverSeat = driverSeat;
        this.lastDetectedSeatCount = 1;
    }

    public double getLastCurrentHealth() {
        return lastCurrentHealth;
    }

    public double getLastMaxHealth() {
        return lastMaxHealth;
    }

    public void setLastHealth(double currentHealth, double maxHealth) {
        this.lastCurrentHealth = currentHealth;
        this.lastMaxHealth = maxHealth;
    }

    public BukkitTask getParticleTask() {
        return particleTask;
    }

    public void setParticleTask(BukkitTask task) {
        // Cancel existing task if any
        if (this.particleTask != null && !this.particleTask.isCancelled()) {
            this.particleTask.cancel();
        }
        this.particleTask = task;
    }

    public void cancelParticleTask() {
        if (this.particleTask != null && !this.particleTask.isCancelled()) {
            this.particleTask.cancel();
        }
        this.particleTask = null;
        this.lastDetectedBlocks = null;
    }

    /**
     * Checks if force disassembly is available (failed disassembly with conflicts).
     */
    public boolean canForceDisassemble() {
        return false;
    }

    public boolean isPendingMenuReopen() {
        return pendingMenuReopen;
    }

    public void setPendingMenuReopen(boolean pending) {
        this.pendingMenuReopen = pending;
    }
}
