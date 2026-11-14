package com.ccorp2002.smoothsailing.detection;

import com.ccorp2002.smoothsailing.SmoothSailingPlugin;
import com.ccorp2002.smoothsailing.ShipModel;
import com.ccorp2002.smoothsailing.nmsutils.*;
import com.ccorp2002.smoothsailing.ship.*;
import com.ccorp2002.smoothsailing.detection.BlockConfigManager;
import com.ccorp2002.smoothsailing.detection.BlockProperties;
import com.ccorp2002.smoothsailing.detection.ShipDetector;
import org.bukkit.entity.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.*;

/**
 * Scans blocks using flood fill and converts them into a ShipModel.
 * Uses BlockConfigManager to determine which blocks are allowed.
 */
public class BlockStructureScanner {

    // ========== Rotation Utility Methods ==========

    /**
     * Converts a BlockFace to Minecraft yaw angle.
     * Minecraft yaw: 0=South, 90=West, 180=North, 270=East
     */
    public static float blockFaceToYaw(BlockFace face) {
        switch (face) {
            case SOUTH: return 0.0f;
            case WEST: return 90.0f;
            case NORTH: return 180.0f;
            case EAST: return 270.0f;
            default: return 0.0f;
        }
    }

    /**
     * Converts yaw angle to BlockFace.
     */
    public static BlockFace yawToBlockFace(float yaw) {
        // Normalize to 0-360
        yaw = yaw % 360;
        if (yaw < 0) yaw += 360;

        // Round to nearest 90 degrees
        int rounded = Math.round(yaw / 90.0f) * 90;
        rounded = rounded % 360;

        if (rounded == 0) return BlockFace.SOUTH;
        if (rounded == 90) return BlockFace.WEST;
        if (rounded == 180) return BlockFace.NORTH;
        return BlockFace.EAST;
    }

    /**
     * Rotates a 3D position around the Y-axis.
     * @param pos The position to rotate
     * @param yawDegrees The rotation angle in degrees
     * @return A new rotated position
     */
    public static Vector3f rotatePosition(Vector3f pos, float yawDegrees) {
        float rad = (float) Math.toRadians(yawDegrees);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);
        return new Vector3f(
            pos.x * cos - pos.z * sin,
            pos.y,
            pos.x * sin + pos.z * cos
        );
    }

    /**
     * Rotates a BlockFace by a yaw offset.
     * Only handles horizontal faces (NORTH/SOUTH/EAST/WEST).
     */
    public static BlockFace rotateBlockFace(BlockFace face, float yawDegrees) {
        // Only handle horizontal faces
        if (face == BlockFace.UP || face == BlockFace.DOWN) {
            return face;
        }

        float baseYaw = blockFaceToYaw(face);
        float newYaw = (baseYaw + yawDegrees) % 360;
        if (newYaw < 0) newYaw += 360;

        return yawToBlockFace(newYaw);
    }

    public static ShipModel scanForStructure(org.bukkit.entity.Player pilot, Location wheelLocation, BlockFace facing) {
        // Get max ship size from config
        SmoothSailingPlugin plugin = SmoothSailingPlugin.getInstance();
        int maxShipSize = 1000; // Default
        int maxScanSize = 5000; // Default
        if (plugin != null) {
            maxShipSize = plugin.getConfig().getInt("max-ship-size", 1000);
            maxScanSize = plugin.getConfig().getInt("max-scan-size", 5000);
        }

        // Use ShipDetector to flood fill and find all ship blocks
        ShipDetector detector = new ShipDetector(maxShipSize, maxScanSize);
        ShipDetector.ShipDetectionResult result = detector.detectShipDetailed(pilot, wheelLocation);

        if (!result.isSuccess()) {
            return null;
        }

        Set<Location> shipBlocks = result.getBlocks();
        if (shipBlocks == null || shipBlocks.isEmpty()) {
            return null;
        }
        BlockConfigManager configManager = BlockConfigManager.getInstance();

        int blockIndex = 0;
        Location wheelOrigin = wheelLocation.clone();
        NmsUtils.loadChunksIn(wheelOrigin.getWorld(),shipBlocks.toArray(Location[]::new));

        // Track weight and center of volume (only for blocks with weight)
        int totalWeight = 0;
        int totalPositiveWeight = 0;  // For health calculation: sum of max(0, weight)
        int weightedBlockCount = 0;
        float sumX = 0, sumY = 0, sumZ = 0;

        // Track ship bounds (for all blocks)
        float minY = Float.MAX_VALUE;
        float maxY = Float.MIN_VALUE;
        float foundMin = minY;
        float foundMax = maxY;
        // Process each detected block
        for (Location blockLoc : shipBlocks) {
            Block block = blockLoc.getBlock();
            BlockData blockData = block.getBlockData();

            // Get block properties from config
            BlockProperties props = configManager.getProperties(blockData.getMaterial(), blockData);


            // Calculate position relative to wheel
            double dx = blockLoc.getX() - wheelOrigin.getX();
            double dy = blockLoc.getY() - wheelOrigin.getY();
            double dz = blockLoc.getZ() - wheelOrigin.getZ();

            // Track ship bounds (all blocks contribute)
            float blockY = (float) block.getLocation().getBlockY();
            if (blockY < foundMin) foundMin = (float)blockY;
            if (blockY > foundMax) foundMax = (float)blockY;

            // Only accumulate weight and center of volume for blocks with weight
            // Blocks with null weight are excluded from density calculations
            if (props.hasWeight()) {
                int weight = props.getWeight();
                totalWeight += weight;
                if (weight > 0) {
                    totalPositiveWeight += weight;
                }
                weightedBlockCount++;
                sumX += (float) dx;
                sumY += (float) dy;
                sumZ += (float) dz;
            }

            // Check if this block is a seat (all detected seats are passenger seats)
            // Driver seat is always at the wheel location (added after scanning)

            blockIndex++;
        }
        
        // Must have at least one block
        if (shipBlocks.isEmpty()) {
            return null;
        }
        BlockFace opposite = facing.getOppositeFace();

        // Create ShipModel with default settings
        // Calculate assembly yaw for disassembly rotation tracking
        float assemblyYaw = blockFaceToYaw(facing);

        // Calculate health from positive block weights (heavier blocks = more health)
        // Blocks with negative/zero weight don't reduce health, just contribute nothing

        // Calculate center of volume (only from blocks with weight)
        minY = foundMin;
        maxY = foundMax;
        // Default bounds if no blocks found (shouldn't happen, but be safe)
        if (minY == Float.MAX_VALUE) minY = 0;
        if (maxY == Float.MIN_VALUE) maxY = 0;
        float heightY = maxY - minY;
        Float waterFloatOffset = -1f*(float)((ShipWheelManager.getBuoyancyData(wheelLocation, shipBlocks, totalWeight)).getOrDefault("surface_offset",null));
        if (waterFloatOffset == null) waterFloatOffset = (float)(-Math.round(heightY/3.75f)) + (float)Math.round((heightY/3f));
        if (Math.abs(waterFloatOffset) >= 10) Math.ceil(waterFloatOffset *= 0.5f);
        ShipModel model = new ShipModel(
            new ArrayList<Location>(shipBlocks),
            waterFloatOffset,
            totalWeight,
            minY,
            maxY
        );
        return model;
    }

    public static ShipModel scanForStructure(Location wheelLocation, BlockFace facing) {
        return scanForStructure(null,wheelLocation,facing);
    }
}
