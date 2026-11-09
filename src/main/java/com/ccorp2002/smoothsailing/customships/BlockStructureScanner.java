package com.ccorp2002.smoothsailing.customships;

import com.ccorp2002.smoothsailing.SmoothSailingPlugin;
import com.ccorp2002.smoothsailing.ShipModel;
import com.ccorp2002.smoothsailing.nmsutils.*;
import com.ccorp2002.smoothsailing.ship.*;
import com.ccorp2002.smoothsailing.blockconfig.BlockConfigManager;
import com.ccorp2002.smoothsailing.blockconfig.BlockProperties;
import com.ccorp2002.smoothsailing.blockconfig.ShipDetector;
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

    /**
     * Rotates BlockData properties (stairs, chests, doors, etc.) by a yaw offset.
     * @param originalData The original block data
     * @param yawDegrees The rotation angle in degrees (should be multiple of 90)
     * @return A new rotated BlockData
     */
    public static BlockFace getBlockDataFace(BlockData originalData) {
        BlockData rotated = originalData;

        // Handle Directional blocks (stairs, chests, doors, etc.)
        if (rotated instanceof org.bukkit.block.data.Directional) {
            org.bukkit.block.data.Directional directional = (org.bukkit.block.data.Directional) rotated;
            BlockFace originalFacing = directional.getFacing();
            return originalFacing;
        }

        // Handle Rotatable blocks (player heads on floor, banners on floor)
        if (rotated instanceof org.bukkit.block.data.Rotatable) {
            org.bukkit.block.data.Rotatable rotatable = (org.bukkit.block.data.Rotatable) rotated;
            BlockFace currentRot = rotatable.getRotation();
            return currentRot;
        }

        return BlockFace.SELF;
    }

    /**
     * Rotates BlockData properties (stairs, chests, doors, etc.) by a yaw offset.
     * @param originalData The original block data
     * @param yawDegrees The rotation angle in degrees (should be multiple of 90)
     * @return A new rotated BlockData
     */
    public static BlockData rotateBlockData(BlockData originalData, float yawDegrees) {
        BlockData rotated = originalData.clone();

        // Round to nearest 90 degrees
        int rotationSteps = Math.round(yawDegrees / 90.0f) % 4;
        if (rotationSteps < 0) rotationSteps += 4;
        if (rotationSteps == 0) return rotated;

        // Handle Directional blocks (stairs, chests, doors, etc.)
        if (rotated instanceof org.bukkit.block.data.Directional) {
            org.bukkit.block.data.Directional directional = (org.bukkit.block.data.Directional) rotated;
            BlockFace originalFacing = directional.getFacing();
            BlockFace newFacing = rotateBlockFace(originalFacing, yawDegrees);

            if (directional.getFaces().contains(newFacing)) {
                directional.setFacing(newFacing);
            }
            return directional;
        }

        // Handle Orientable blocks (logs, pillars, hay bales, etc.)
        if (rotated instanceof org.bukkit.block.data.Orientable orientable) {
            org.bukkit.Axis currentAxis = orientable.getAxis();

            // Y-axis stays Y (vertical), X and Z swap on 90/270 degree rotations
            if (currentAxis != org.bukkit.Axis.Y && rotationSteps % 2 == 1) {
                org.bukkit.Axis newAxis = (currentAxis == org.bukkit.Axis.X)
                    ? org.bukkit.Axis.Z
                    : org.bukkit.Axis.X;
                orientable.setAxis(newAxis);
            }
            return orientable;
        }

        // Handle Rotatable blocks (player heads on floor, banners on floor)
        if (rotated instanceof org.bukkit.block.data.Rotatable) {
            org.bukkit.block.data.Rotatable rotatable = (org.bukkit.block.data.Rotatable) rotated;
            BlockFace currentRot = rotatable.getRotation();

            // Rotatable uses 16 directions (each 22.5 degrees)
            // 90 degrees = 4 steps in the rotation system
            int currentStep = rotationToStep(currentRot);
            int newStep = (currentStep + (rotationSteps * 4)) % 16;
            BlockFace newRot = stepToRotation(newStep);

            rotatable.setRotation(newRot);
            return rotatable;
        }

        // Handle MultipleFacing blocks (fences, walls, etc.)
        if (rotated instanceof org.bukkit.block.data.MultipleFacing) {
            org.bukkit.block.data.MultipleFacing mf = (org.bukkit.block.data.MultipleFacing) rotated;
            Set<BlockFace> originalFaces = new HashSet<>(mf.getFaces());

            // Clear all faces first
            for (BlockFace face : originalFaces) {
                mf.setFace(face, false);
            }

            // Set rotated faces
            for (BlockFace face : originalFaces) {
                BlockFace newFace = rotateBlockFace(face, yawDegrees);
                if (mf.getAllowedFaces().contains(newFace)) {
                    mf.setFace(newFace, true);
                }
            }
            return mf;
        }

        return rotated;
    }

    /**
     * Converts a Rotatable BlockFace to step (0-15).
     * Step 0 = SOUTH, 4 = WEST, 8 = NORTH, 12 = EAST
     */
    public static int rotationToStep(BlockFace face) {
        switch (face) {
            case SOUTH: return 0;
            case SOUTH_SOUTH_WEST: return 1;
            case SOUTH_WEST: return 2;
            case WEST_SOUTH_WEST: return 3;
            case WEST: return 4;
            case WEST_NORTH_WEST: return 5;
            case NORTH_WEST: return 6;
            case NORTH_NORTH_WEST: return 7;
            case NORTH: return 8;
            case NORTH_NORTH_EAST: return 9;
            case NORTH_EAST: return 10;
            case EAST_NORTH_EAST: return 11;
            case EAST: return 12;
            case EAST_SOUTH_EAST: return 13;
            case SOUTH_EAST: return 14;
            case SOUTH_SOUTH_EAST: return 15;
            default: return 0;
        }
    }

    /**
     * Converts a step (0-15) to Rotatable BlockFace.
     */
    public static BlockFace stepToRotation(int step) {
        step = step % 16;
        if (step < 0) step += 16;
        switch (step) {
            case 0: return BlockFace.SOUTH;
            case 1: return BlockFace.SOUTH_SOUTH_WEST;
            case 2: return BlockFace.SOUTH_WEST;
            case 3: return BlockFace.WEST_SOUTH_WEST;
            case 4: return BlockFace.WEST;
            case 5: return BlockFace.WEST_NORTH_WEST;
            case 6: return BlockFace.NORTH_WEST;
            case 7: return BlockFace.NORTH_NORTH_WEST;
            case 8: return BlockFace.NORTH;
            case 9: return BlockFace.NORTH_NORTH_EAST;
            case 10: return BlockFace.NORTH_EAST;
            case 11: return BlockFace.EAST_NORTH_EAST;
            case 12: return BlockFace.EAST;
            case 13: return BlockFace.EAST_SOUTH_EAST;
            case 14: return BlockFace.SOUTH_EAST;
            case 15: return BlockFace.SOUTH_SOUTH_EAST;
            default: return BlockFace.SOUTH;
        }
    }

    /**
     * Checks if a material is an attachable block (banners, signs, torches, etc.).
     * These should be removed after their support blocks.
     */
    public static boolean isAttachable(Material type) {
        String name = type.name();
        return name.contains("BANNER") || name.contains("SIGN") ||
               name.contains("TORCH") || name.contains("BUTTON") ||
               name.contains("LEVER") || name.contains("_DOOR") || name.contains("CARPET") ||
               name.contains("PRESSURE_PLATE") || 
               name.contains("LANTERN") || FragileBlocks.isFragile(type);
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
        System.out.println("FOUND BLOCK COUNT: "+shipBlocks.size());

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
