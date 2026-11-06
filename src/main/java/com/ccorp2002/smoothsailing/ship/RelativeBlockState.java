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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

import java.lang.ref.WeakReference;
import it.unimi.dsi.fastutil.Hash;
import java.util.*;
public class RelativeBlockState extends MobileBlockState {

    public double x() {
      return this.getLocation().getX();
    }
    public double y() {
      return this.getLocation().getY();
    }
    public double z() {
      return this.getLocation().getZ();
    }
    public int bx() {
      return (int)this.getLocation().getX();
    }
    public int by() {
      return (int)this.getLocation().getY();
    }
    public int bz() {
      return (int)this.getLocation().getZ();
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
    public org.bukkit.util.Vector offset;

    public RelativeBlockState(ShipInstance ship, Location location, boolean check) {
        super(location,check);
        this.setShip(ship);
    }

    public RelativeBlockState(ShipInstance ship, Location location) {
        this(ship,location,false);
    }

    public RelativeBlockState(ShipInstance ship, NmsUtils.WorldBlockVector vector, BlockState state) {
        super(vector,state);
        this.setShip(ship);
    }

    public RelativeBlockState(ShipInstance ship, NmsUtils.WorldBlockVector vector, BlockState state, BlockEntity tile) {
        super(vector,state,tile);
        this.setShip(ship);
    }
    @Override
    public void setShip(ShipInstance ship) {
        if (ship == null) return;
        super.shipRef = new java.lang.ref.WeakReference<>(ship);
        Location center = ship.vehicle.getLocation().toBlockLocation();
        Location offset = super.getLocation().subtract(center.toVector());
        super.setLocation(center);
        this.setOffset(offset);
    }

    @Override
    public int hashCode() {
        return this.getLocation().hashCode();
    }

    @Override
    public Location getLocation() {
        return toRealLocation();
    }

    protected void reinit(Vector vec) {
        reinit(vec.getBlockX(), vec.getBlockY(), vec.getBlockZ());
    }

    protected void reinit(final int x, final int y, final int z) {
        if (super.vector == null) return;
        this.vector.setX(x - super.vector.getBlockX());
        this.vector.setY(y - super.vector.getBlockY());
        this.vector.setZ(z - super.vector.getBlockZ());
    }

    /**
     * NEVER USE THIS UNLESS ABSOLUTELY NECESSARY
     * @param ship
     * @param location
     */
    public void reset(ShipInstance ship, Vector vec) {
        if (ship == null) return;
        if (vec == null) return;
        super.setShip(ship);
        reinit(vec);
    }

    public RelativeBlockState setOrigin(Location loc) {
        if (getShip() == null) return this;
        super.setLocation(loc.toBlockLocation());
        return this;
    }

    public RelativeBlockState setOrigin(ShipInstance ship) {
        if (ship == null) return this;
        super.setLocation(ship.vehicle.getLocation().toBlockLocation());
        this.setShip(ship);
        return this;
    }

    public Location getOrigin() {
        return super.getLocation();
    }

    public Location toRealLocation() {
        return this.getLocation().add(this.offset);
    }

    public Vector getOffset() {
        return this.offset;
    }

    public void setOffset(Location loc) {
        this.offset = loc.toBlockLocation().toVector();
    }
    @Override
    public void teleport(Location loc) {
        if (loc == null) return;
        loc = loc.toBlockLocation();
        Location origin = vector.toLocation().toBlockLocation();
        NmsUtils.setBlockFast(loc,this.state);
        if (this.tile != null) {
            this.tile.tilePosition = NmsUtils.locationToPosition(origin);
            this.tile.newPosition = NmsUtils.locationToPosition(loc);
            NmsUtils.moveBlockEntity(loc,this.tile,this.state);
        }
        if (this.tile == null) {
            this.tile = NmsUtils.createTileHolder(loc);
        }
        this.vector = new NmsUtils.WorldBlockVector(loc);
        if (this.isDead()) NmsUtils.setBlockFast(loc,this.occupiedBlock);
        
    }

    public MobileBlockState toMobileState() {
        MobileBlockState state = new MobileBlockState(this.toRealLocation(),false);
        state.setShip(this.getShip());
        return state;
    }
}
