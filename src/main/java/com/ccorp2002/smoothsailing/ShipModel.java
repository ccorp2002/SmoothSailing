package com.ccorp2002.smoothsailing;

import com.ccorp2002.smoothsailing.nmsutils.*;
import com.ccorp2002.smoothsailing.ship.*;

import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.joml.*;

import java.util.*;

public class ShipModel {
    public final List<Location> blocks;
    public float waterFloatOffset;   // Y-position offset from water surface where ship floats (for prefab ships)

    // Buoyancy system (for custom block ships)
    public int totalWeight;               // Sum of all block weights (excluding null-weight blocks)
    public int blockCount;                // Number of blocks with weight (for density calculation)
    public float minY;                    // Bottom of ship (relative to origin)
    public float maxY;                    // Top of ship (relative to origin)
    // Assembly rotation (for custom block ships disassembly)

    public ShipModel(List<Location> blocks, float waterFloatOffset, int totalWeight, float minY, float maxY) {
        this.blocks = blocks;
        this.waterFloatOffset = waterFloatOffset;
        this.totalWeight = totalWeight;
        this.blockCount = this.blocks.size();
        this.minY = minY;
        this.maxY = maxY;
    }

    /**
     * Calculates the ship's density (weight / block count).
     */
    public float getDensity() {
        if (blockCount == 0) return 0;
        return (float) totalWeight / blockCount;
    }
}


