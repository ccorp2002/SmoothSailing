package com.ccorp2002.smoothsailing.blockconfig;
import com.ccorp2002.smoothsailing.SmoothSailingPlugin;
import com.ccorp2002.smoothsailing.nmsutils.NmsUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.*;

/**
 * Detects ship blocks using 6-direction flood fill algorithm.
 * Starts at a ship wheel and expands to all connected allowed blocks.
 */
public class ShipDetector {

    // 6 directions: +X, -X, +Y, -Y, +Z, -Z (no diagonals)
    public static final int[][] DIRECTIONS = {
        {1, 0, 0},   // East
        {-1, 0, 0},  // West
        {0, 1, 0},   // Up
        {0, -1, 0},  // Down
        {0, 0, 1},   // South
        {0, 0, -1}   // North
    };

    private final BlockConfigManager configManager;
    private final int maxShipSize;
    private final int maxScanSize;
    public static final int minSize = 9;

    public ShipDetector(int maxShipSize, int maxScanSize) {
        this.configManager = BlockConfigManager.getInstance();
        this.maxShipSize = maxShipSize;
        this.maxScanSize = maxScanSize;
    }

    /**
     * Internal result for detectShipInternal with scan state.
     */
    public static class InternalScanResult {
        public Set<Location> blocks;
        public boolean exceededLimit;
        public boolean scanLimitHit;

        public InternalScanResult(Set<Location> blocks, boolean exceededLimit,  boolean scanLimitHit) {
            this.blocks = blocks;
            this.exceededLimit = exceededLimit;
            this.scanLimitHit = scanLimitHit;
        }
    }
    public static Set<Block> detectBlocks(org.bukkit.entity.Player pilot, Location startLocation) {
        return detectBlocks(pilot, startLocation, 750, true);
    }

    public static Set<Block> detectBlocks(org.bukkit.entity.Player pilot, Location startLocation, int maxSize, boolean isTurret) {
        Set<Block> shipBlocks = new HashSet<>();
        if (startLocation == null || startLocation.getWorld() == null) return shipBlocks;
        Queue<Location> frontier = new LinkedList<>();
        boolean exceededLimit = false;
        // Start with the initial location
        frontier.add(startLocation.clone());
        shipBlocks.add(startLocation.getBlock());
        BlockConfigManager configManager = BlockConfigManager.getInstance();

        // BFS flood fill
        while (!frontier.isEmpty()) {
            Location current = frontier.poll();

            // Check all 6 adjacent blocks
            for (int[] direction : DIRECTIONS) {
                Location neighbor = current.clone().add(direction[0], direction[1], direction[2]);

                // Skip if already visited
                if (shipBlocks.contains(neighbor.getBlock())) continue;
                if (!(NmsUtils.withinWorldBorder(neighbor))) continue;

                // Check if we've hit the scan limit (hard stop)
                if (shipBlocks.size() >= maxSize) {
                    return shipBlocks;
                }
                // Check if the block is allowed
                BlockData blockData = neighbor.getBlock().getBlockData();
                Material material = blockData.getMaterial();
                if (material.isAir()) {
                    // Skip air blocks
                    continue;
                }

                boolean canBreakAt = true;
                if (pilot != null) {
                    for (final com.ccorp2002.smoothsailing.utils.claims.IClaimInterface ICLAIM : SmoothSailingPlugin.INSTANCE.CLAIM_INTEFACES) {
                        if (!canBreakAt) break;
                        if (!ICLAIM.canBreakAt(pilot,neighbor)) canBreakAt = false;
                        
                    }
                }
                if (!configManager.isAllowed(blockData) || (isTurret && configManager.isTurretBase(blockData)) || !canBreakAt) {
                    // Skip forbidden blocks (don't add to ship, but continue scanning)
                    continue;
                }

                // Valid block, add to ship and frontier
                shipBlocks.add(neighbor.clone().getBlock());
                frontier.add(neighbor.clone());
            }
        }
        return shipBlocks;
    }

    /**
     * Detect all ship blocks starting from the ship wheel location.
     * Uses BFS flood fill with 6-direction expansion.
     * Continues scanning past maxShipSize up to maxScanSize to report actual size.
     *
     * @param startLocation The ship wheel location
     * @return Internal result with blocks and status flags
     */
    public InternalScanResult detectShipInternal(Location startLocation) {
        return detectShipInternal(null,startLocation);
    }

    /**
     * Detect all ship blocks starting from the ship wheel location.
     * Uses BFS flood fill with 6-direction expansion.
     * Continues scanning past maxShipSize up to maxScanSize to report actual size.
     *
     * @param startLocation The ship wheel location
     * @return Internal result with blocks and status flags
     */
    public InternalScanResult detectShipInternal(org.bukkit.entity.Player pilot, Location startLocation) {
        Set<Location> shipBlocks = new HashSet<>();
        Queue<Location> frontier = new LinkedList<>();
        boolean exceededLimit = false;

        // Start with the initial location
        frontier.add(startLocation.clone());
        shipBlocks.add(startLocation.clone());

        // BFS flood fill
        while (!frontier.isEmpty()) {
            Location current = frontier.poll();

            // Check all 6 adjacent blocks
            for (int[] direction : DIRECTIONS) {
                Location neighbor = current.clone().add(direction[0], direction[1], direction[2]);

                // Skip if already visited
                if (shipBlocks.contains(neighbor)) continue;
                if (!(NmsUtils.withinWorldBorder(neighbor))) continue;

                // Check if we've hit the scan limit (hard stop)
                if (shipBlocks.size() >= maxScanSize) {
                    return new InternalScanResult(shipBlocks, true, true);
                }

                // Check if we've exceeded the ship size limit (continue scanning but mark as exceeded)
                if (shipBlocks.size() >= maxShipSize) {
                    exceededLimit = true;
                }

                // Check if the block is allowed
                BlockData blockData = neighbor.getBlock().getBlockData();
                Material material = blockData.getMaterial();
                if (material.isAir()) {
                    // Skip air blocks
                    continue;
                }
                boolean canBreakAt = true;
                if (pilot != null) {
                    for (final com.ccorp2002.smoothsailing.utils.claims.IClaimInterface ICLAIM : SmoothSailingPlugin.INSTANCE.CLAIM_INTEFACES) {
                        if (!canBreakAt) break;
                        if (!ICLAIM.canBreakAt(pilot,neighbor)) canBreakAt = false;
                        
                    }
                }
                if (material.toString().endsWith("ANVIL")) {
                    shipBlocks.clear();
                    frontier.clear();
                    canBreakAt = false;
                    return new InternalScanResult(shipBlocks, true, true);
                }

                if (!configManager.isAllowed(blockData) || !canBreakAt) {
                    // Skip forbidden blocks (don't add to ship, but continue scanning)
                    continue;
                }

                // Valid block, add to ship and frontier
                shipBlocks.add(neighbor.clone());
                frontier.add(neighbor.clone());
            }
        }
        /**
            if (pilot != null) {
                for (final com.ccorp2002.smoothsailing.utils.claims.IClaimInterface ICLAIM : SmoothSailingPlugin.INSTANCE.CLAIM_INTEFACES) {
                    shipBlocks.removeIf(pos -> 
                        !ICLAIM.canBreakAt(pilot,pos)
                    );
                }
            }
        */
        return new InternalScanResult(shipBlocks, exceededLimit, false);
    }

    /**
     * Detect all ship blocks starting from the ship wheel location.
     * Uses BFS flood fill with 6-direction expansion.
     *
     * @param startLocation The ship wheel location
     * @return Set of all blocks that are part of the ship, or null if detection failed
     */
    public Set<Location> detectShip(Location startLocation) {
        InternalScanResult result = detectShipInternal(startLocation);
        com.ccorp2002.smoothsailing.event.BlockFinishScanEvent devent = new com.ccorp2002.smoothsailing.event.BlockFinishScanEvent(result);
        devent.callEvent();
        if (devent.isCancelled()) {
            return null;
        }
        result = devent.getResult();
        if (result.exceededLimit) {
            return null;
        }
        return result.blocks;
    }

    /**
     * Detect ship and return detailed information about the ship.
     */
    public ShipDetectionResult detectShipDetailed(org.bukkit.entity.Player pilot, Location startLocation) {
        InternalScanResult result = detectShipInternal(pilot, startLocation);
        com.ccorp2002.smoothsailing.event.BlockFinishScanEvent devent = new com.ccorp2002.smoothsailing.event.BlockFinishScanEvent(result);
        devent.callEvent();
        if (devent.isCancelled()) result.blocks.clear();
        else result = devent.getResult();
        

        if (result.blocks.isEmpty()) {
            return new ShipDetectionResult(false, "No valid blocks found for ship", null, 0, false);
        }

        int blockCount = result.blocks.size();
        if (blockCount <= 16) return new ShipDetectionResult(false, "Ship is too small", null, blockCount, false);

        if (result.scanLimitHit) {
            // Ship so big we stopped counting
            String message = "Ship has at least " + blockCount + " blocks (stopped scanning), maximum is " + maxShipSize;
            return new ShipDetectionResult(false, message, null, blockCount, true);
        }

        if (result.exceededLimit) {
            // Ship over limit but fully scanned
            String message = "Ship has " + blockCount + " blocks which exceeds the maximum of " + maxShipSize;
            return new ShipDetectionResult(false, message, null, blockCount, false);
        }
        return new ShipDetectionResult(true, "Successfully detected ship with " + blockCount + " blocks", result.blocks, blockCount, false);
    }

    /**
     * Detect ship and return detailed information about the ship.
     */
    public ShipDetectionResult detectShipDetailed(Location startLocation) {
        InternalScanResult result = detectShipInternal(startLocation);
        com.ccorp2002.smoothsailing.event.BlockFinishScanEvent devent = new com.ccorp2002.smoothsailing.event.BlockFinishScanEvent(result);
        devent.callEvent();
        if (devent.isCancelled()) result.blocks.clear();
        else result = devent.getResult();
        

        if (result.blocks.isEmpty()) {
            return new ShipDetectionResult(false, "No valid blocks found for ship", null, 0, false);
        }

        int blockCount = result.blocks.size();
        if (blockCount <= 16) return new ShipDetectionResult(false, "Ship is too small", null, blockCount, false);

        if (result.scanLimitHit) {
            // Ship so big we stopped counting
            String message = "Ship has at least " + blockCount + " blocks (stopped scanning), maximum is " + maxShipSize;
            return new ShipDetectionResult(false, message, null, blockCount, true);
        }

        if (result.exceededLimit) {
            // Ship over limit but fully scanned
            String message = "Ship has " + blockCount + " blocks which exceeds the maximum of " + maxShipSize;
            return new ShipDetectionResult(false, message, null, blockCount, false);
        }
        return new ShipDetectionResult(true, "Successfully detected ship with " + blockCount + " blocks", result.blocks, blockCount, false);
    }

    /**
     * Result of ship detection.
     */
    public static class ShipDetectionResult {
        public boolean success;
        public String message;
        public Set<Location> blocks;
        public int blockCount;
        public boolean scanLimitHit;

        public ShipDetectionResult(boolean success, String message, Set<Location> blocks, int blockCount, boolean scanLimitHit) {
            this.success = success;
            this.message = message;
            this.blocks = blocks;
            this.blockCount = blockCount;
            this.scanLimitHit = scanLimitHit;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Set<Location> getBlocks() {
            return blocks;
        }

        public int getBlockCount() {
            return blockCount;
        }

        public boolean isScanLimitHit() {
            return scanLimitHit;
        }
    }
}
