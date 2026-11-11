package com.ccorp2002.smoothsailing.utils.claims;

import com.ccorp2002.smoothsailing.*;
import com.ccorp2002.smoothsailing.utils.*;
import com.ccorp2002.smoothsailing.nmsutils.*;
import com.ccorp2002.smoothsailing.detection.*;
import com.ccorp2002.smoothsailing.detection.*;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;


public interface IClaimInterface {

    public static final boolean DEBUG = false;

    default String getConfigID() {
        return "none";
    }

    boolean canBreakAt(Player player, Location loc);

    boolean canBuildAt(Player player, Location loc);

    boolean canEnterAt(Player player, Location loc);

    boolean isWilderness(Location loc);
}