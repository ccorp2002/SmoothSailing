package com.ccorp2002.smoothsailing.utils.claims;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;

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

import java.util.*;

public class TownyInterface implements IClaimInterface {

    private final Towny PLUGIN;
    private final TownyAPI API;
    public static TownyInterface INTERFACE;

    public static boolean CAN_ENEMY_PILOT = false;
    public static boolean CAN_ALLIED_PILOT = false;
    public static boolean CAN_TRUSTED_PILOT = false;
    public static boolean CAN_OUTSIDER_PILOT = false;

    public static boolean CAN_ENEMY_MOVE = false;
    public static boolean CAN_ALLIED_MOVE = false;
    public static boolean CAN_TRUSTED_MOVE = false;
    public static boolean CAN_OUTSIDER_MOVE = false;


    public TownyInterface() {
        SmoothSailingPlugin.INSTANCE.getLogger().info("==================================================");
        SmoothSailingPlugin.INSTANCE.getLogger().info("Towny was found for Compatibility & Protections!");
        PLUGIN = Towny.getPlugin();
        API = TownyAPI.getInstance();
        INTERFACE = this;
        try {
            var townySection = SmoothSailingPlugin.INSTANCE.getConfig().getConfigurationSection(this.getConfigID());
            CAN_ENEMY_PILOT = townySection.getBoolean("enemy.can-pilot-blocks",false);
            CAN_ALLIED_PILOT = townySection.getBoolean("allied.can-pilot-blocks",true);
            CAN_TRUSTED_PILOT = townySection.getBoolean("trusted.can-pilot-blocks",true);
            CAN_OUTSIDER_PILOT = townySection.getBoolean("outsider.can-pilot-blocks",false);

            CAN_ENEMY_MOVE = townySection.getBoolean("enemy.can-passthrough",true);
            CAN_ALLIED_MOVE = townySection.getBoolean("allied.can-passthrough",true);
            CAN_TRUSTED_MOVE = townySection.getBoolean("trusted.can-passthrough",true);
            CAN_OUTSIDER_MOVE = townySection.getBoolean("outsider.can-passthrough",true);
        } catch (Throwable thr) {}
        SmoothSailingPlugin.INSTANCE.getLogger().info("MAY ENEMIES/OUTLAWS PILOT BLOCKS?: "+CAN_ENEMY_PILOT);
        SmoothSailingPlugin.INSTANCE.getLogger().info("MAY ENEMIES/OUTLAWS PASSTHROUGH TOWNS?: "+CAN_ENEMY_MOVE);
        SmoothSailingPlugin.INSTANCE.getLogger().info("MAY ALLIES PILOT BLOCKS?: "+CAN_ALLIED_PILOT);
        SmoothSailingPlugin.INSTANCE.getLogger().info("MAY ALLIES PASSTHROUGH TOWNS?: "+CAN_ALLIED_MOVE);
        SmoothSailingPlugin.INSTANCE.getLogger().info("MAY NEUTRAL/OUTSIDERS PILOT BLOCKS?: "+CAN_OUTSIDER_PILOT);
        SmoothSailingPlugin.INSTANCE.getLogger().info("MAY NEUTRAL/OUTSIDERS PASSTHROUGH TOWNS?: "+CAN_OUTSIDER_MOVE);
        SmoothSailingPlugin.INSTANCE.getLogger().info("MAY TRUSTED/NATIONMATES PILOT BLOCKS?: "+CAN_TRUSTED_PILOT);
        SmoothSailingPlugin.INSTANCE.getLogger().info("MAY TRUSTED/NATIONMATES PASSTHROUGH TOWNS?: "+CAN_TRUSTED_MOVE);
        SmoothSailingPlugin.INSTANCE.getLogger().info("==================================================");
    }

    @Override
    public String getConfigID() {
        return "towny";
    }

    public Resident getResident(OfflinePlayer player) {
        if (API == null) return null;
        if (player == null) return null;
        return getResident(player.getUniqueId());
    }

    public Resident getResident(UUID uuid) {
        if (API == null) return null;
        if (uuid == null) return null;
        return API.getResident(uuid);
    }

    public boolean canBreakAt(Player player, Location loc) {
        boolean DESTROY = isWilderness(loc);
        if (player == null) DESTROY = false;
        if (loc == null) DESTROY = true;
        if (API == null) DESTROY = true;
        else {
            Town townA = API.getTown(player);
            Town townB = API.getTown(loc);
            if (townB != null) {
                if (townB.hasOutlaw(getResident(player)) && CAN_ENEMY_PILOT) DESTROY = true;
                if (townB.hasTrustedResident(getResident(player)) && CAN_TRUSTED_PILOT) DESTROY = true;
                if (townA != null && CombatUtil.isSameNation(townA,townB) && CAN_ALLIED_PILOT) DESTROY = true;
                if (townA == null && CAN_OUTSIDER_PILOT) DESTROY = PlayerCacheUtil.getCachePermission(player, loc, NmsUtils.toBukkitBlockFast(loc), TownyPermission.ActionType.DESTROY);
            }
        }
        if (DEBUG) SmoothSailingPlugin.INSTANCE.getLogger().warning(player.getName()+" CAN DESTROY/PILOT BLOCKS IN TOWN @ "+loc.toBlockLocation().toVector().toString()+"?: "+DESTROY);
        return DESTROY;
    }

    public boolean canBuildAt(Player player, Location loc) {
        boolean BUILD = isWilderness(loc);
        if (player == null) BUILD = false;
        if (loc == null) BUILD = true;
        if (API == null) BUILD = true;
        else {
            Town townA = API.getTown(player);
            Town townB = API.getTown(loc);
            if (townB != null) {
                if (townB.hasOutlaw(getResident(player)) && CAN_ENEMY_PILOT) BUILD = true;
                if (townB.hasTrustedResident(getResident(player)) && CAN_TRUSTED_PILOT) BUILD = true;
                if (townA != null && CombatUtil.isSameNation(townA,townB) && CAN_ALLIED_PILOT) BUILD = true;
                if (townA == null && CAN_OUTSIDER_PILOT) BUILD = PlayerCacheUtil.getCachePermission(player, loc, NmsUtils.toBukkitBlockFast(loc), TownyPermission.ActionType.BUILD);
            }
        }
        if (DEBUG) SmoothSailingPlugin.INSTANCE.getLogger().warning(player.getName()+" CAN BUILD BLOCKS IN TOWN @ "+loc.toBlockLocation().toVector().toString()+"?: "+BUILD);
        return BUILD;
    }

    public boolean canEnterAt(Player player, Location loc) {
        boolean CAN_ENTER = isWilderness(loc);
        if (player == null) CAN_ENTER = false;
        if (loc == null) CAN_ENTER = true;
        if (API == null) CAN_ENTER = true;
        else {
            Town townA = API.getTown(player);
            Town townB = API.getTown(loc);
            if (townB != null) {
                if (townB.hasOutlaw(getResident(player)) && CAN_ENEMY_MOVE) CAN_ENTER = true;
                if (townB.hasTrustedResident(getResident(player)) && CAN_TRUSTED_MOVE) CAN_ENTER = true;
                if (townA != null && CombatUtil.isSameNation(townA,townB) && CAN_ALLIED_MOVE) CAN_ENTER = true;
                if (townA == null && CAN_OUTSIDER_MOVE) CAN_ENTER = PlayerCacheUtil.getCachePermission(player, loc, NmsUtils.toBukkitBlockFast(loc), TownyPermission.ActionType.SWITCH);
            }
        }
        if (DEBUG) SmoothSailingPlugin.INSTANCE.getLogger().warning(player.getName()+" CAN MOVE VEHICLE IN TOWN @ "+loc.toBlockLocation().toVector().toString()+"?: "+CAN_ENTER);
        return CAN_ENTER;
    }

    public boolean isWilderness(Location loc) {
        if (loc == null) return true;
        if (API == null) return true;
        return API.isWilderness(loc);
    }
}