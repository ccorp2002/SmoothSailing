package com.ccorp2002.smoothsailing.utils.claims;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.*;
import com.sk89q.worldguard.protection.flags.registry.*;
import com.sk89q.worldguard.protection.flags.*;
import com.sk89q.worldguard.protection.regions.*;
import com.sk89q.worldguard.protection.managers.*;
import com.sk89q.worldedit.bukkit.BukkitAdapter;

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

public class WorldGuardInterface implements IClaimInterface {

    private static final String ignore_region = "__global__";
    private final WorldGuardPlugin PLUGIN;
    private final WorldGuard API;
    public static WorldGuardInterface INTERFACE;
    public enum State {
        DENY,
        ALLOW,
        NONE
    }

    public WorldGuardInterface() {
        SmoothSailingPlugin.INSTANCE.getLogger().info("==================================================");
        SmoothSailingPlugin.INSTANCE.getLogger().info("WorldGuard was found for Compatibility & Protections!");
        SmoothSailingPlugin.INSTANCE.getLogger().info("==================================================");
        PLUGIN = WorldGuardPlugin.inst();
        API = WorldGuard.getInstance();
        INTERFACE = this;
    }
    public static StateFlag ALLOW_CRAFT_PILOT = new StateFlag("allow-ship-pilot", false);
    public static StateFlag ALLOW_CRAFT_TRANSLATE = new StateFlag("allow-ship-move", false);

    public static void registerFlags() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            registry.register(ALLOW_CRAFT_PILOT);
            registry.register(ALLOW_CRAFT_TRANSLATE);
        }
        catch (Exception e) {
          Flag<?> pilot = registry.get("allow-ship-pilot");
          if (pilot instanceof StateFlag statePilot) {
              ALLOW_CRAFT_PILOT = statePilot;
          }
          Flag<?> move = registry.get("allow-ship-move");
          if (move instanceof StateFlag stateMove) {
              ALLOW_CRAFT_TRANSLATE = stateMove;
          }
        }
    }

    @Override
    public String getConfigID() {
        return "worldguard";
    }
    public ApplicableRegionSet getApplicableRegions(Location loc) {
        RegionQuery query = API.getPlatform().getRegionContainer().createQuery();
        ApplicableRegionSet regions = query.getApplicableRegions(BukkitAdapter.adapt(loc));
        return regions;
    }

    public boolean canBreakAt(Player player, Location loc) {
        if (player == null) return false;
        if (loc == null) return true;
        if (API == null) return true;
        LocalPlayer localPlayer = PLUGIN.wrapPlayer(player);
        boolean BYPASS = API.getPlatform().getSessionManager().hasBypass(localPlayer, localPlayer.getWorld());
        return BYPASS || (getState(player, loc, ALLOW_CRAFT_PILOT) != State.DENY);
    }

    public boolean canBuildAt(Player player, Location loc) {
        if (player == null) return false;
        if (loc == null) return true;
        if (API == null) return true;
        LocalPlayer localPlayer = PLUGIN.wrapPlayer(player);
        boolean BYPASS = API.getPlatform().getSessionManager().hasBypass(localPlayer, localPlayer.getWorld());
        return BYPASS || (getState(player, loc, ALLOW_CRAFT_PILOT) != State.DENY);
    }

    public boolean canEnterAt(Player player, Location loc) {
        if (player == null) return false;
        if (loc == null) return true;
        if (API == null) return true;
        LocalPlayer localPlayer = PLUGIN.wrapPlayer(player);
        boolean BYPASS = API.getPlatform().getSessionManager().hasBypass(localPlayer, localPlayer.getWorld());
        return BYPASS || (getState(player, loc, ALLOW_CRAFT_TRANSLATE) != State.DENY);
    }
    public boolean isWilderness(Location loc) {
        return true;
    }

    public Set<String> getRegions(Location loc) {
        ApplicableRegionSet regionSet = getApplicableRegions(loc);
        HashSet<String> stringSet = new HashSet<>();
        for (ProtectedRegion r : regionSet) {
            stringSet.add(r.getId());
        }
        return stringSet;
    }

    public boolean isInRegion(Location loc) {
        return getApplicableRegions(loc).size() > 0;
    }

    public boolean regionExists(String regionName, World w) {
        return getRegion(regionName, w) != null;
    }
    public ProtectedRegion getRegion(String regionName, World w) {
        RegionManager regions = API.getPlatform().getRegionContainer().get(BukkitAdapter.adapt(w));
        if (regions == null) return null;

        return regions.getRegion(regionName);
    }

    public boolean regionContains(String regionName,  Location l) {
        ProtectedRegion r = getRegion(regionName, l.getWorld());
        if (r == null) return false;
        return r.contains(l.getBlockX(), l.getBlockY(), l.getBlockZ());
    }
    public State getState(Player p, Location loc,  StateFlag flag) {
        RegionQuery query = API.getPlatform().getRegionContainer().createQuery();
        LocalPlayer localPlayer = null;
        if (p != null) {
            localPlayer = PLUGIN.wrapPlayer(p);
        }
        StateFlag.State state = query.queryState(BukkitAdapter.adapt(loc), localPlayer, flag);
        if (state == null)
            return State.NONE;
        switch (state) {
            case ALLOW:
                return State.ALLOW;
            case DENY:
                return State.DENY;
            default:
                return State.NONE;
        }
    }

}