package com.ccorp2002.smoothsailing.ship;

import org.bukkit.entity.*;
import org.bukkit.block.*;
import org.bukkit.block.data.*;
import org.bukkit.util.Vector;
import org.bukkit.util.*;
import org.bukkit.*;
import org.joml.Matrix4f;
import com.ccorp2002.smoothsailing.*;
import com.ccorp2002.smoothsailing.ship.*;
import com.ccorp2002.smoothsailing.utils.*;
import com.ccorp2002.smoothsailing.nmsutils.*;

import java.lang.ref.WeakReference;
import it.unimi.dsi.fastutil.Hash;
import java.util.*;

public class TrackedLocation {

    protected final it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<String,Object> TRACKED = new it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<>(64,0.75f);
    
    public Map<String,Object> getTracked() {
        return this.TRACKED;
    }

    public void setTracked(Map<String,Object> NEW_TAGS) {
        TRACKED.clear();
        TRACKED.putAll(NEW_TAGS);
    }

    public Object addTracked(String key, Object TAG) {
        if (key == null) return null;
        return TRACKED.put(key,TAG);
    }
    
    public void clearTracked() {
        TRACKED.clear();
    }
    
    public Object removeTracked(String key) {
        if (key == null) return null;
        return TRACKED.remove(key);
    }
    
    public double x() {
      return this.getLocation().getX();
    }
    public double y() {
      return this.getLocation().getY();
    }
    public double z() {
      return this.getLocation().getZ();
    }
    public int getBlockX() {
      return (int)this.getLocation().getX();
    }
    public int getBlockY() {
      return (int)this.getLocation().getY();
    }
    public int getBlockZ() {
      return (int)this.getLocation().getZ();
    }

    public NmsUtils.WorldBlockVector offset;

    public NmsUtils.WorldBlockVector vector;

    public TrackedLocation(MobileBlockState state) {
        this(state.getShip(),state.getLocation());
    }

    public TrackedLocation(ShipInstance ship, Location location) {
        this(ship,new NmsUtils.WorldBlockVector(location));
    }

    public TrackedLocation(ShipInstance ship, Vector vector) {
        setOrigin(ship.getLocation());
        this.offset = new NmsUtils.WorldBlockVector(ship.getLocation().subtract(vector));
    }

    @Override
    public boolean equals(Object oth) {
        if (oth == null) return false;
        if (oth instanceof MobileBlockState state) {
            if (this.hashCode() == state.hashCode()) {
                return true;
            }
        } else if (oth instanceof Location loc) {
            if (MobileBlockState.hashCode(loc) == this.hashCode()) {
                return true;
            }
        } else if (oth instanceof Block block) {
            if (MobileBlockState.hashCode(block) == this.hashCode()) {
                return true;
            }
        } else if (oth instanceof org.bukkit.util.Vector vector) {
            if (MobileBlockState.hashCode(vector.toBlockVector()) == this.hashCode()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return MobileBlockState.hashCode(this.toRealLocation());
    }

    public Location getLocation() {
        return this.toRealLocation();
    }


    public TrackedLocation setOrigin(ShipInstance ship) {
        if (ship == null) return this;
        return setOrigin(ship.getLocation());
    }

    public TrackedLocation setOrigin(Location loc) {
        if (loc == null) return this;
        this.vector = new NmsUtils.WorldBlockVector(loc);
        return this;
    }

    public Location getOrigin(World world) {
        return vector.toLocation(world);
    }

    public TrackedLocation setWorld(World world) {
        this.vector.setWorld(world);
        return this;
    }

    public Location getOrigin() {
        return getOrigin(null);
    }

    public Location toRealLocation() {
        if (this.vector == null || this.offset == null) return null;
        return this.vector.toLocation().add(this.offset);
    }

    public NmsUtils.WorldBlockVector getOffset() {
        return this.offset;
    }

    public TrackedLocation setOffset(NmsUtils.WorldBlockVector offset) {
        this.offset = offset;
        return this;
    }

    public TrackedLocation setOffset(Location loc) {
        this.offset = new NmsUtils.WorldBlockVector(loc);
        return this;
    }

    public MobileBlockState toMobileState() {
        MobileBlockState state = new MobileBlockState(this.toRealLocation(),true);
        return state;
    }
}
