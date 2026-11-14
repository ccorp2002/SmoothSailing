package com.ccorp2002.smoothsailing.ship;

import org.bukkit.entity.*;
import org.bukkit.block.*;
import org.bukkit.block.data.*;
import org.bukkit.util.Vector;
import org.bukkit.util.*;
import org.bukkit.*;
import com.ccorp2002.smoothsailing.*;
import com.ccorp2002.smoothsailing.ship.*;
import com.ccorp2002.smoothsailing.event.*;
import com.ccorp2002.smoothsailing.utils.*;
import com.ccorp2002.smoothsailing.nmsutils.*;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ScheduledTick;

import org.bukkit.craftbukkit.util.RandomSourceWrapper;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.AbstractObjectSet;

import org.bukkit.OfflinePlayer;
import org.bukkit.util.BoundingBox;

import java.lang.ref.WeakReference;
import it.unimi.dsi.fastutil.Hash;
import java.lang.reflect.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.function.Predicate;
import java.util.*;
public class MobileBlockState extends BlockVector implements Comparable<MobileBlockState> {
    
    public static class MobileStateSet extends AbstractSet<MobileBlockState> {
        public final Long2ObjectOpenHashMap<MobileBlockState> internalMap;
        public final int initialMaxSize;
        public MobileStateSet() {
            this(7500);
        }
        public MobileStateSet(int maxSize) {
            this(maxSize,0.75f);
        }
        public MobileStateSet(int maxSize, float growthFactor) {
           this.initialMaxSize = maxSize;
           this.internalMap = new Long2ObjectOpenHashMap<>(this.initialMaxSize,growthFactor);
           internalMap.defaultReturnValue(null);
        }
        @Override
        public Iterator<MobileBlockState> iterator() {
            return internalMap.values().iterator();
        }

        public MobileBlockState get(Long k) {
            if (k == null) return null;
            else return this.internalMap.getOrDefault(k,null);
        }

        @Override
        public boolean add(MobileBlockState e) {
            if (e == null) return false;
            else {
                if (size()+5>=initialMaxSize) internalMap.ensureCapacity(size()+500);
                MobileBlockState COLLIDED = internalMap.putIfAbsent((long)pack(e),e);
                return COLLIDED == null;
            }
        }

        @Override
        public boolean addAll(Collection<? extends MobileBlockState> c) {
            return addAll(c.toArray(MobileBlockState[]::new));
        }
        public boolean addAll(MobileBlockState... c) {
            boolean modified = false;
            for (MobileBlockState e : c)
                if (add(e)) modified = true;
            return modified;
        }
        @Override
        public boolean contains(Object needle) {
            if (needle == null) return false;
            else if (needle instanceof MobileBlockState state) {
                return internalMap.containsKey((long)pack(state));
            } else if (needle instanceof Long logn) {
                return internalMap.containsKey(logn);
            } else if (needle instanceof Integer inte) {
                return internalMap.containsKey((long)inte);
            } else return false;
        }

        public void setAll(Collection<? extends MobileBlockState> e) {
            setAll(e.toArray(MobileBlockState[]::new));
        }

        public void setAll(MobileBlockState... e) {
            for (MobileBlockState s : e) {
                this.set(s);
            }
        }

        public Object set(MobileBlockState e) {
            if (e == null) return null;
            return set((long)pack(e),e);
        }

        public Object set(Long key, MobileBlockState e) {
            if (e == null) return null;
            else {
                if (key == null) key = pack(e);
                if (size()+5>=initialMaxSize) internalMap.ensureCapacity(size()+500);
                if (internalMap.containsKey(key)) return internalMap.replace(key,e,e);
                else return internalMap.putIfAbsent((long)pack(e),e);
            }
        }
        @Override
        public boolean remove(Object needle) {
            if (needle == null) return false;
            else if (needle instanceof MobileBlockState state) {
                return internalMap.remove((long)state.hashCode()) != null;
            } else if (needle instanceof Long logn) {
                return internalMap.remove(logn) != null;
            } else if (needle instanceof Integer inte) {
                return internalMap.remove((long)inte) != null;
            } else return false;
        }
        @Override
        public boolean isEmpty() {
            return this.internalMap.isEmpty();
        }
        @Override
        public int size() {
            return this.internalMap.values().size();
        }
        public boolean trim(int amount) {
            return this.internalMap.trim(amount);
        }
        public boolean trim() {
            return trim(this.initialMaxSize);
        }
        @Override
        public Object[] toArray() {
            return this.toArray(MobileBlockState[]::new);
        }
        @Override
        public <T> T[] toArray(T[] a) {
            List<MobileBlockState> list = new ArrayList<>(size());
            for (MobileBlockState e : this) list.add(e);
            return list.toArray(a);
        }

        @Override
        public void clear() {
            internalMap.clear();
            trim();
        }
    }
    public static final MobileBlockState$HashStrategy HASH_STRAT = new MobileBlockState$HashStrategy();

    public static class MobileBlockState$HashStrategy implements Hash.Strategy<Object> {
        @Override
        public int hashCode(Object l) {
            if (l instanceof TrackedLocation rbs) {
                return rbs.hashCode();
            } else if (l instanceof MobileBlockState mbs) {
                return mbs.hashCode();
            } else if (l instanceof Location loc) {
                return MobileBlockState.hashCode(loc);
            } else if (l instanceof Block blok) {
                return MobileBlockState.hashCode(blok);
            } else if (l instanceof org.bukkit.util.Vector vec) {
                return MobileBlockState.hashCode(vec);
            } else {
                return Objects.hashCode(l);
            }
        }

        @Override
        public boolean equals(Object a, Object b) {
            if (a == null || b == null) return false;
            else return hashCode(a) == hashCode(b);
        }
        public boolean equals(MobileBlockState a, MobileBlockState b) {
            if (a == null || b == null) return false;
            else return a.equals(b);
        }
    }
    @Override
    public boolean equals(Object oth) {
        return this.equals(oth,true);
    }
    public boolean equals(Object oth, boolean fuzzyCheck) {
        if (oth == null) return false;
        if (oth instanceof MobileBlockState state) {
            if (this.hashCode() == state.hashCode()) {
                if (fuzzyCheck) return true;
                if (state.state == null || this.state == null) return true;
                else if (this.getBlockData().matches(state.getBlockData())) return true;
                else if ((state.getBlockData()).matches(this.getBlockData())) return true;
                else return false;
            }
        } else if (oth instanceof Location loc) {
            if (hashCode(loc) == hashCode(this)) {
                return true;
            }
        } else if (oth instanceof Block block) {
            if (hashCode(block) == hashCode(this)) {
                return true;
            }
        } else if (oth instanceof org.bukkit.util.Vector vector) {
            if (hashCode(vector.toBlockVector()) == hashCode(this)) {
                return true;
            }
        }
        return false;
    }
    @Override
    public int compareTo(MobileBlockState other) {
        if (other == null) return -1;
        if(this.bx() != other.bx()){
            return this.bx() - other.bx();
        }
        if(this.by() != other.by()){
            return this.by() - other.by();
        }
        if(this.bz() != other.bz()){
            return this.bz() - other.bz();
        }
        return 0;
    }
    public double x() {
      return getX();
    }
    public double y() {
      return getY();
    }
    public double z() {
      return getZ();
    }
    public int bx() {
      return getBlockX();
    }
    public int by() {
      return getBlockY();
    }
    public int bz() {
      return getBlockZ();
    }
    public WeakReference<ShipInstance> shipRef = null;

    public static MobileBlockState getStateFromLoc(final Location loc) {
        if (loc == null) return null;
        if (loc.getWorld() == null) return null;
        ShipInstance ship = getShipFromLoc(loc);
        MobileBlockState state = getStateFromShipLoc(ship,loc);
        if (state == null || ship == null) return FALLBACK$getStateFromLoc(loc);
        return state;
    }

    public static MobileBlockState FALLBACK$getStateFromLoc(final Location loc) {
        if (loc == null) return null;
        if (loc.getWorld() == null) return null;
        MobileBlockState TEST = new MobileBlockState(loc,true);
        for (ShipInstance ship : ShipRegistry.getAllShips()) {
            Set<MobileBlockState> states = ship.BLOCK_STATES;
            if (states == null || states.isEmpty()) continue;
            else {
                TEST.shipRef = new java.lang.ref.WeakReference<>(ship);
                MobileBlockState mstate = states.stream().filter(state -> state.equals(TEST)).findFirst().orElse(null);
                if (mstate == null) continue;
                if (mstate.shipRef == null) mstate.shipRef = new java.lang.ref.WeakReference<>(ship);
                return mstate;
            }
        }
        return null;
    }

    public volatile NextTickProvider.TickHolder TICK_TO_MOVE = null;

    public ShipInstance getShip() {
        if (this.shipRef != null) return this.shipRef.get();
        return null;
    }
    public void setShip(ShipInstance ship) {
        if (ship == null) return;
        this.shipRef = new java.lang.ref.WeakReference<>(ship);
    }

    public static MobileBlockState getStateFromShipLoc(final ShipInstance ship, final Location loc) {
        if (ship == null) return null;
        if (loc == null) return null;
        MobileBlockState TEST = new MobileBlockState(loc,true);
        TEST.shipRef = new java.lang.ref.WeakReference<>(ship);
        return ship.BLOCK_STATES.stream().parallel().filter(state -> state.equals(TEST)).findFirst().orElse(null);
    }

    public static boolean doesShipHave(final ShipInstance ship, final Location TEST) {
        if (ship == null) return false;
        if (TEST == null) return false;
        if (TEST.getWorld() == null) return false;
        return doesShipHave(ship,new MobileBlockState(TEST,true));
    }

    public static boolean doesShipHave(final ShipInstance ship, final Block TEST) {
        if (ship == null) return false;
        if (TEST == null) return false;
        return doesShipHave(ship,new MobileBlockState(TEST.getLocation(),true));
    }

    public static boolean doesShipHave(final ShipInstance ship, final Long TEST) {
        if (ship == null) return false;
        if (TEST == null) return false;
        boolean retVal = ship.BLOCK_STATES.contains(TEST);
        return retVal;
    }

    public static boolean doesShipHave(final ShipInstance ship, final Vector TEST) {
        if (ship == null) return false;
        if (TEST == null) return false;
        boolean retVal = ship.BLOCK_STATES.contains(TEST);
        return retVal;
    }


    public static ShipInstance getShipFromLoc(final Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        MobileBlockState TEST = new MobileBlockState(loc,true);
        for (ShipInstance ship : ShipRegistry.getShipsInWorld(loc.getWorld())) {
            if (TEST.shipRef != null) TEST.shipRef.clear();
            if (doesShipHave(ship,TEST)) {
                return ship;
            }
        }
        return null;
    }

    public boolean isSameShip(final MobileBlockState state) {
        if (state == null || (state.shipRef == null || this.shipRef == null)) return false;
        return state.getShip().id.equals(this.getShip().id);
    }
    public boolean isSameShip(final Location loc) {
        if (loc == null || (loc.getWorld() == null || this.shipRef == null)) return false;
        ShipInstance other = getShipFromLoc(loc);
        if (other == null) return false;
        else return other.id.equals(this.getShip().id);
    }

    public BlockData occupiedBlock = ShipPhysics.VOID;

    public boolean isDead() {
        return this.health <= 0l;
    }

    public long maxHealth = 15l;
    public long health = 15l;


    public BlockState state = NmsUtils.AIR;

    public NmsUtils.TileHolder tile = null;

    public MobileBlockState(Location location, boolean check) {
        super(location.getX(),location.getY(),location.getZ());
        if (!check) {
            this.state = NmsUtils.getBlockFaster(location);
            this.tile = NmsUtils.createTileHolder(location);
        } else {
            this.state = null;
            this.tile = null;
        }
    }

    public MobileBlockState(Block block) {
        this(block.getLocation(),false);
    }

    public MobileBlockState(Location location) {
        this(location,false);
    }

    public MobileBlockState(Vector vector) {
        this(vector,null,null);
    }

    public MobileBlockState(Vector vector, BlockState state) {
        this(vector,state,null);
    }

    public MobileBlockState(Vector vector, BlockState state, BlockEntity tile) {
        super(vector.getX(),vector.getY(),vector.getZ());
        this.state = state;
        this.tile = NmsUtils.createTileHolder(tile);
    }

    public MobileBlockState teleport(Location loc, BlockData data) {
        if (data == null) return this;
        if (loc == null) return this;
        this.state = NmsUtils.getBlockState(data);
        return this.teleport(loc,false);
    }

    public MobileBlockState teleport(Location loc) {
        return this.teleport(loc,false);
    }

    public MobileBlockState teleport(Location loc, boolean reAdd) {
        if (loc == null) return this;
        if (loc.getWorld() == null) return this;
        final Location floc = loc;
        final ShipInstance ship = this.getShip();
        if (ship != null) {
            if (!NmsUtils.withinWorldBorder(floc) && ship.isSinking) {
                this.damage(this.health);
                return this;
            }
        }
        final Location origin = toLocation(ship.getWorld());
        final ServerLevel oldLevel = (ServerLevel)NmsUtils.getLevel(origin);
        final ServerLevel newLevel = (ServerLevel)NmsUtils.getLevel(loc);
        BlockPos position = NmsUtils.locationToPosition(origin);
        BlockPos newPosition = NmsUtils.locationToPosition(floc);
        final org.bukkit.inventory.Inventory inv;
        if (NmsUtils.HAS_CRAFT_ENGINE && this.tile instanceof CEUtils.CraftEngineTileHolder) {
            inv = CEUtils.INSTANCE.getCEBlockInv(origin);
        } else {
            inv = null;
        }
        /*
        if (NmsUtils.HAS_CRAFT_ENGINE && ship != null) {
            TaskUtils.runTask(()->{
                final org.bukkit.persistence.PersistentDataContainer pdcC = CEUtils.getCEBlockPDC(origin);
                ShipMovePDCEvent pdcEvent = new ShipMovePDCEvent(ship,ship.vehicle,pdcC,origin.getBlock(),floc.getBlock());
                pdcEvent.callEvent();
            });
        }
        */
        NmsUtils.setBlockFast(floc,this.state);
        if (this.isDead()) {
            if (NmsUtils.HAS_CRAFT_ENGINE) CEUtils.INSTANCE.removeCEBlock(floc,false,true);
            NmsUtils.setBlockFast(floc,ShipPhysics.VOID);
        }
        if (this.tile != null) {
            this.tile.tilePosition = position;
            this.tile.newPosition = newPosition;
            final NmsUtils.TileHolder mutated = NmsUtils.moveReturnBlockEntity(floc,this.tile,this.state);
            if (mutated != null) this.tile = mutated;
            else this.tile = NmsUtils.createTileHolder(floc);
        }
        if (this.TICK_TO_MOVE != null) {
            NmsUtils.MOVE_TICK_TO(this.TICK_TO_MOVE,floc);
            this.TICK_TO_MOVE = null;
        }
        if (NmsUtils.HAS_CRAFT_ENGINE && inv != null && this.tile instanceof CEUtils.CraftEngineTileHolder) {
            org.bukkit.inventory.Inventory other = CEUtils.INSTANCE.getCEBlockInv(floc);
            try {
                if (inv != null && other != null) other.setContents(inv.getContents());
            } catch (Throwable thr) {}
        }
        this.x = (floc.getX());
        this.y = (floc.getY());
        this.z = (floc.getZ());
        if (ship != null && reAdd) {
            ship.BLOCK_STATES.set(this);
            if (SmoothSailingPlugin.DEBUG_DUMP) System.out.println("RE-ADDING: ("+super.toString()+") TO SHIP? "+ship);
        }
        return this;
    }

    @Override
    public int hashCode() {
        return hashCode(this);
    }

    public static int hashCode(Location loc) {
        if (loc == null) return 0;
        int result = hashCode(loc.toVector());
        return result;
    }
    private static final long BITS_26 = NmsUtils.mask(26);
    private static final long BITS_12 = NmsUtils.mask(12);

    public static int hashCode(org.bukkit.util.Vector vec) {
        long packed = pack(vec);
        return (int) (packed ^ (packed >>> 32));
    }

    public static int hashCode(Block block) {
        long packed = packBlock(block);
        return (int) (packed ^ (packed >>> 32));
    }

    public static long pack(Object obj) {
        if (obj instanceof MobileBlockState state) {
            return packVector(state);
        } else if (obj instanceof Block block) {
            return packBlock(block);
        } else if (obj instanceof Location loc) {
            return packVector(loc.toVector());
        } else if (obj instanceof org.bukkit.util.Vector vec) {
            return packVector(vec);
        } else if (obj instanceof Long logn) {
            return logn;
        } else if (obj instanceof Integer inte) {
            return (long)inte;
        }
        else return 0l;
    }

    public static long packBlock(Block block) {
        if (block == null) return 0;
        return (block.getX() & BITS_26) | ((block.getZ() & BITS_26) << 26) | (((block.getY() & (long) BITS_12) << (26 + 26)));
    }

    public static long pack(int x, int y, int z) {
        return ((int)x & BITS_26) | (((int)z & BITS_26) << 26) | ((((int)y & (long) BITS_12) << (26 + 26)));
    }

    public static long packVector(org.bukkit.util.Vector vec) {
        if (vec == null) return 0;
        return ((int)vec.getX() & BITS_26) | (((int)vec.getZ() & BITS_26) << 26) | ((((int)vec.getY() & (long) BITS_12) << (26 + 26)));
    }

    public static org.bukkit.util.Vector unpack(long l){
        return new org.bukkit.util.Vector((int) (l << 38 >> 38),(int) (l >> 52),(int) (l << 12 >> 38));
    }
    public static Location unpackLocation(long l, World world){
        return new org.bukkit.util.Vector((int) (l << 38 >> 38),(int) (l >> 52),(int) (l << 12 >> 38)).toLocation(world);
    }
    

    public long getMaxHealth() {
        return this.maxHealth;
    }

    public long getHealth() {
        return this.health;
    }
    public void resetHealth(long hp) {
        this.maxHealth = hp;
        this.health = hp;
    }

    public void setMaxHealth(long max) {
        if (max <= -1) max = 0;
        this.maxHealth = max;
    }

    public void setHealth(long hp) {
        if (hp <= -1) hp = 0;
        ShipInstance ship = getShip();
        if (ship == null) return;
        if (isDead() && hp >= 1) {
            NmsUtils.setBlockFast(this.toLocation(),this.state);
        } else if (!isDead() && hp > 0) {
            NmsUtils.setBlockFast(this.toLocation(),this.state);
        }
        this.health = hp;
        if (isDead()) {
            if (SmoothSailingPlugin.shipListener().isShipWheelBlock(this.getBlock()) && ship != null) {
                getShip().destroy();
                return;
            }
            if (ship != null) {
                ShipInstance.updateDamagedBlock(ship,this);
            }
            if (this.tile != null) {
                this.getBlock().breakNaturally(true);
                this.tile = null;
            }
            NmsUtils.setBlockFast(this.toLocation(),ShipPhysics.VOID);
        }
    }

    public void damage(long amnt) {
        setHealth(this.health - amnt);
    }

    public void setBlockData(BlockData data) {
        setBlockData(data,false);
        
    }

    public void setBlockData(BlockData data, boolean updateShip) {
        if (data == null) return;
        if (!data.getMaterial().isAir() || updateShip) {
            this.state = ((CraftBlockData) data).getState();
        }
        if (updateShip) {
            ShipInstance ship = this.getShip();
            if (ship != null && !ship.CHANGE_WORLD) {
                ShipInstance.updateBlockStateOnShip(ship,this);
            }
        }
    }

    public Block getBlock() {
        return getLocation().getBlock();
    }

    @Override
    public Location toLocation(World world) {
        if (world != null) return new Location(world,this.getX(),this.getY(),this.getZ()); 
        else if (getShip() == null) return null;
        else return this.toLocation(getShip().getWorld());
    }

    public Location toLocation() {
        return getLocation(null);
    }

    public Location getLocation() {
        return getLocation(null);
    }

    public Location getLocation(World world) {
        return toLocation(world);
    }

    public World getWorld() {
        if (getShip() != null) return getShip().getWorld();
        else return null;
    }

    public BlockData getVOID() {
        return ShipPhysics.VOID;
    }

    public BlockData getOccupied() {
        if (this.occupiedBlock == null) return ShipPhysics.VOID;
        return this.occupiedBlock;
    }

    public BlockData getBlockData() {
        if (this.state == null) return ShipPhysics.VOID;
        return NmsUtils.BlockDataGetter.get(this.state);
    }

    public void updateBlockState() {
        ShipInstance ship = this.getShip();
        if (ship == null) return;
        Location pos = this.getLocation();
        if (this.isDead() && pos != null) {
            NmsUtils.setBlockFast(pos,ShipPhysics.VOID);
            return;
        }
        if (ship != null && !ship.CHANGE_WORLD && pos != null) {
            this.state = NmsUtils.getBlockFaster(pos);
            this.tile = NmsUtils.createTileHolder(pos);
            ShipInstance.updateBlockStateOnShip(ship,this);
        }
    }

    public BlockData getAndUpdateBlockData() {
        if (this.state == null) return ShipPhysics.VOID;
        if (getShip() != null && !getShip().CHANGE_WORLD) {
            NmsUtils.runTaskInWorld(this.getLocation(),()->{
                getBlock().getState(false).update(false,false);
            });
        }
        BlockState otherState = NmsUtils.getBlockFaster(this.getLocation());
        if (otherState.equals(this.state)) return NmsUtils.BlockDataGetter.get(state);
        else return NmsUtils.BlockDataGetter.get(otherState);
    }

    public TrackedLocation toRelativeState() {
        return new TrackedLocation(this.getShip(), this);
    }
}
