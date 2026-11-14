package com.ccorp2002.smoothsailing.nmsutils;


import ca.spottedleaf.moonrise.common.util.WorldUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.bukkit.craftbukkit.util.RandomSourceWrapper;
import org.bukkit.craftbukkit.entity.*;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.piston.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.portal.*;
import ca.spottedleaf.moonrise.common.util.WorldUtil;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.world.entity.*;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.InventoryView;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.craftbukkit.util.Waitable;

import io.papermc.paper.FeatureHooks;
import io.papermc.paper.util.MCUtil;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Future;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Callable;

import java.lang.reflect.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.lang.reflect.Field;
import java.util.function.Predicate;
import java.util.*;

import org.bukkit.util.Vector;
import org.bukkit.util.BlockVector;

import com.ccorp2002.smoothsailing.*;
import com.ccorp2002.smoothsailing.ship.*;
import com.ccorp2002.smoothsailing.utils.*;
import com.ccorp2002.smoothsailing.utils.objects.*;
import com.ccorp2002.smoothsailing.nmsutils.NmsUtils;
import com.ccorp2002.smoothsailing.detection.ShipWheelData;
import com.ccorp2002.smoothsailing.detection.ShipWheelManager;

import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.core.block.AbstractBlockManager;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.bukkit.plugin.network.BukkitNetworkManager;
import net.momirealms.craftengine.core.block.parser.BlockStateParser;
import net.momirealms.craftengine.bukkit.block.behavior.SimpleStorageBlockBehavior;
import net.momirealms.craftengine.bukkit.block.entity.SimpleStorageBlockEntityController;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Future;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Callable;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.lang.reflect.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.lang.reflect.Field;
import java.util.function.Predicate;
import java.util.*;
import sun.misc.Unsafe;

import com.google.common.primitives.UnsignedInteger;
import java.lang.reflect.Field;




@SuppressWarnings("unused")
public final class NmsUtils {
    private NmsUtils() {}
    private static @Nullable Unsafe unsafe;
    static {
        Unsafe defered;
        try{
            var field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            defered = (Unsafe) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            defered = null;
        }
        unsafe = defered;
    }
    public static final NextTickProvider TICK_PROVIDER = new NextTickProvider();

    public static int mask(int bits) {
        return ~(~0 << bits);
    }

    public static int unpackX(long packed) {
        return extractSigned(packed, 0, 26);
    }

    public static int unpackZ(long packed) {
        return extractSigned(packed, 26, 26);
    }

    public static int unpackY(long packed) {
        return extractSigned(packed, 26 + 26, 12);
    }

    public static int extractSigned(long i, int shift, int bits) {
        return fixSign((int) (i >> shift) & mask(bits), bits);
    }

    public static int fixSign(int i, int bits) {
        // Using https://stackoverflow.com/a/29266331/436524
        return i << (32 - bits) >> (32 - bits);
    }

    public static void setField(Field field, Object holder, Object value){
        if(unsafe == null){
            return;
        }
        unsafe.putObject(holder, unsafe.objectFieldOffset(field), value);
    }

    public static @Nullable Field getFieldOfType(@NotNull Class<?> type, @Nullable Class<?> clazz) {
        if (clazz == null)
            return null;
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getType() == type) {
                return field;
            }
        }
        return getFieldOfType(type, clazz.getSuperclass());
    }

    public static void trySetFieldOfType(@NotNull Class<?> type, @NotNull Object holder, @NotNull Object value) {
        Field field = getFieldOfType(type, holder.getClass());
        if (field != null) {
            setField(field, holder, value);
        }
    }

    public static boolean withinWorldBorder(Location location) {
        if (location == null) return false;
        return withinWorldBorder(location.getWorld(),location);
    }

    public static boolean withinWorldBorder(World world, Location location) {
        if (world == null) return false;
        if (location == null) return false;
        org.bukkit.WorldBorder border = world.getWorldBorder();
        int radius = (int) Math.floor(border.getSize() / 2.0);
        //The visible border will always end at 29,999,984 blocks, despite being larger
        Location center = border.getCenter();
        int minX = center.getBlockX() - radius;
        int maxX = center.getBlockX() + radius;
        int minZ = center.getBlockZ() - radius;
        int maxZ = center.getBlockZ() + radius;
        if (location.getY() >= world.getMaxHeight() - 3) return false;
        else if (location.getY() <= world.getMinHeight() + 3) return false;
        else return border.isInside(location);
    }

    public static void moveBlocks(@NotNull Pair<Location,Location> locationPair, @NotNull final org.bukkit.World world) {
        runTaskInWorld(()->{
            final ServerLevel oldNativeWorld = ((CraftWorld) world).getHandle();
            Location location = locationPair.getKey();
            final BlockPos position = locationToPosition(location);
            final BlockPos newPosition = locationToPosition(locationPair.getValue());
            final BlockState oldState = getBlockFastest(oldNativeWorld, position);
            final Pair<Long,BlockState> newBlock = new Pair(newPosition.asLong(), oldState);
            final TileHolder holder = removeAndCreateTileHolder(oldNativeWorld, position);
            if (holder != null) {
                holder.newPosition = newPosition;
            }
            final BlockPos pos = BlockPos.of(newBlock.getKey());
            final BlockState data = newBlock.getValue();
            setBlockFaster(oldNativeWorld,pos,data,true);
            moveBlockEntity(locationPair.getValue(),holder);
        },world);
    }


    public static void moveBlocks(@NotNull Map<Location,Location> locationMap, @NotNull final org.bukkit.World world) {
        runTaskInWorld(()->{
            final ServerLevel oldNativeWorld = ((CraftWorld) world).getHandle();
            final Long2ObjectMap<Pair<Long,BlockState>> blockData = new Long2ObjectOpenHashMap<>(locationMap.size());
            final it.unimi.dsi.fastutil.objects.ObjectOpenHashSet<TileHolder> tiles = new it.unimi.dsi.fastutil.objects.ObjectOpenHashSet<>((int)locationMap.size()/2);
            for (Location location : locationMap.keySet()) {
                final BlockPos position = locationToPosition(location);
                final BlockPos newPosition = locationToPosition(locationMap.get(location));
                final BlockState oldState = getBlockFastest(oldNativeWorld, position);
                final Pair<Long,BlockState> newBlock = new Pair(newPosition.asLong(), oldState);
                blockData.put(position.asLong(),newBlock);
                final TileHolder holder = removeAndCreateTileHolder(oldNativeWorld, position);
                if (holder != null) {
                    holder.newPosition = newPosition;
                    tiles.add(holder);
                }
            }
            for(Pair<Long,BlockState> newBlock : blockData.values()) {
                final BlockPos pos = BlockPos.of(newBlock.getKey());
                final BlockState data = newBlock.getValue();
                setBlockFaster(oldNativeWorld,pos,data,true);
            }
            for (TileHolder tileHolder : tiles) {
                if (tileHolder.newPosition == null) continue;
                
                moveBlockEntity(positionToLocation(tileHolder.newPosition,oldNativeWorld),tileHolder);
            }
            blockData.clear();
            tiles.clear();
        },world);
    }

    public static net.momirealms.craftengine.core.world.chunk.CEChunk getCEChunkOrNull(net.momirealms.craftengine.core.world.CEWorld ceWorld, @NotNull BlockVector position) {
        if (CE_UTILS == null) return null;
        return CE_UTILS.getCEChunkOrNull(ceWorld,position);
    }

    public static net.momirealms.craftengine.core.world.chunk.CEChunk getCEChunkOrNull(net.momirealms.craftengine.core.world.CEWorld ceWorld, @NotNull BlockPos position) {
        if (CE_UTILS == null) return null;
        return CE_UTILS.getCEChunkOrNull(ceWorld,position);
    }


    public static net.momirealms.craftengine.core.world.chunk.CEChunk getCEChunkOrNull(net.momirealms.craftengine.core.world.CEWorld ceWorld, @NotNull Location position) {
        if (CE_UTILS == null) return null;
        return CE_UTILS.getCEChunkOrNull(ceWorld,position);
    }


    public static net.momirealms.craftengine.core.world.chunk.CEChunk getCEChunkOrNull(net.momirealms.craftengine.core.world.CEWorld ceWorld, @NotNull net.momirealms.craftengine.core.world.BlockPos position) {
        if (CE_UTILS == null) return null;
        return CE_UTILS.getCEChunkOrNull(ceWorld,position);
    }

    public static void removeCEBlock(Location pos) {
        removeCEBlock(pos,false);
    }

    public static void removeCEBlock(Location pos, boolean toAir) {
        if (CE_UTILS != null) CE_UTILS.removeCEBlock(pos,toAir);
    }
    public static final Vector EMPTY_VEC = new Vector();
    public static CEUtils CE_UTILS = null;
    public static final BlockState AIR = Blocks.AIR.defaultBlockState();
    public static final int UPDATE = Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS | Block.UPDATE_SKIP_ON_PLACE;
    public static final int NOTIFY = Block.UPDATE_NONE;
    public static final int UPDATE_NOTIFY = 3;
    public static final BlockData air = Bukkit.getServer().createBlockData("minecraft:air");
    public static Plugin PLUGIN = Bukkit.getPluginManager().getPlugin("SmoothSailing");
    public static final RandomSource RANDOM = new RandomSourceWrapper(ShipInstance.RANDOM);
    public static final net.minecraft.server.MinecraftServer MCS = net.minecraft.server.MinecraftServer.getServer();
    public static boolean HAS_CRAFT_ENGINE = false;

  public static org.bukkit.World getExecutedWorld(org.bukkit.event.Event event) {
      org.bukkit.World world = null;
      if (event != null) {
          if (event instanceof org.bukkit.event.world.WorldEvent worldEvent) {
              world = worldEvent.getWorld();
          } else if (event instanceof org.bukkit.event.player.PlayerEvent playerEvent) {
              //world = playerEvent.getPlayer().getWorld();
          } else if (event instanceof org.bukkit.event.entity.EntityEvent entityEvent) {
              //world = entityEvent.getEntity().getWorld();
          } else if (event instanceof org.bukkit.event.hanging.HangingEvent entityEvent) {
              world = entityEvent.getEntity().getWorld();
          } else if (event instanceof org.bukkit.event.block.BlockEvent blockEvent) {
              world = blockEvent.getBlock().getWorld();
          } else if (event instanceof org.bukkit.event.weather.WeatherEvent weatherEvent) {
              world = weatherEvent.getWorld();
          } else if (event instanceof org.bukkit.event.vehicle.VehicleEvent vehicleEvent) {
              world = vehicleEvent.getVehicle().getWorld();
          } else if (event instanceof org.bukkit.event.raid.RaidEvent raidEvent) {
              world = raidEvent.getWorld();
          } else {
              world = null;
          }
      }
      return world;
  }

    @Nullable
    public static TileHolder createBukkitTileHolder(@NotNull Location pos){
        Object tile = null;
        TileHolder holder = null;        
        if (tile == null) {
            tile = getBukkitStateCopy(pos);
            if (tile != null && tile instanceof org.bukkit.block.TileState tileSt) {
                holder = new BukkitTileHolder(tileSt,locationToPosition(pos));
            }
        }
        return holder;
    }

    @Nullable
    public static TileHolder createTileHolder(@NotNull BlockEntity be){
        return createTileHolder(be.getLevel(),be.getBlockPos());
    }


    @Nullable
    public static TileHolder createTileHolder(@NotNull Location pos){
        return createTileHolder((ServerLevel)((CraftWorld) pos.getWorld()).getHandle(),locationToPosition(pos));
    }

    @Nullable
    public static TileHolder createTileHolder(@NotNull Level world, @NotNull BlockPos position){
        Object tile = null;
        TileHolder holder = null;
        if (CE_UTILS != null) {
            tile = CE_UTILS.getCEBlockEntity(positionToLocation(position,world));
            holder = CE_UTILS.createTileHolder(world,position);
        }
        if (tile == null && holder == null) {
            tile = getBlockEntity(world,position);
            if (tile != null && tile instanceof BlockEntity tileSt) {
                holder = new VanillaTileHolder(tileSt,position);
            }
        }
        /*
        if (tile == null && holder == null) {
            tile = getBukkitStateCopy(positionToLocation(position,world));
            if (tile instanceof org.bukkit.block.TileState tileSt) {
                holder = new BukkitTileHolder(tileSt,position);
            }
        }
        */
        return holder;
    }

    @Nullable
    public static TileHolder removeAndCreateTileHolder(@NotNull BlockEntity be){
        return removeAndCreateTileHolder(be.getLevel(),be.getBlockPos());
    }


    @Nullable
    public static TileHolder removeAndCreateTileHolder(@NotNull Location pos){
        return removeAndCreateTileHolder((ServerLevel)((CraftWorld) pos.getWorld()).getHandle(),locationToPosition(pos));
    }

    @Nullable
    public static TileHolder removeAndCreateTileHolder(@NotNull Level world, @NotNull BlockPos position){
        Object tile = null;
        TileHolder holder = null;
        if (CE_UTILS != null) {
            tile = CE_UTILS.getCEBlockEntity(positionToLocation(position,world));
            holder = CE_UTILS.removeAndCreateTileHolder(world,position);
        }
        if (tile == null && holder == null) {
            tile = removeBlockEntity(world,position);
            if (tile != null && tile instanceof BlockEntity tileSt) {
                holder = new VanillaTileHolder(tileSt,position);
            }
        }
        /*
        if (tile == null && holder == null) {
            tile = getBukkitStateCopy(positionToLocation(position,world));
            if (tile instanceof org.bukkit.block.TileState tileSt) {
                holder = new BukkitTileHolder(tileSt,position);
            }
        }
        */
        return holder;
    }


  public static Location positionToLocation(@NotNull TileHolder holder, @NotNull World world) {
    return org.bukkit.craftbukkit.util.CraftLocation.toBukkit(holder.tilePosition,(ServerLevel)((CraftWorld) world).getHandle());
  }



  public static Location positionToLocation(@NotNull BlockEntity be) {
    return org.bukkit.craftbukkit.util.CraftLocation.toBukkit(be.getBlockPos(),be.getLevel());
  }


  public static Location positionToLocation(@NotNull BlockPos pos, Level world) {
    return org.bukkit.craftbukkit.util.CraftLocation.toBukkit(pos,world);
  }

  public static BlockPos locationToPosition(@NotNull Location loc) {
      return new BlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
  }

  public static void setBlockFastest(Level world, BlockPos position, BlockState data) {
      setBlockFaster(world,position,data,true);
  }

  public static BlockState getBlockFaster(Location location) {
      if (location == null) return AIR;
      if (location.getWorld() == null) return AIR;
      ServerLevel level = ((CraftWorld) location.getWorld()).getHandle();
      BlockPos position = locationToPosition(location);
      BlockState data = getBlockFaster(level,position);
      return data;
  }

  public static BlockState getBlockFaster(@NotNull Level world, @NotNull BlockPos position) {
      LevelChunk chunk = (LevelChunk) world.getChunkIfLoaded(position);
      if (chunk == null) chunk = (LevelChunk) world.getChunkAt(position);
      if (chunk == null) return AIR;
      int chunkSection = world.getSectionIndex(position.getY());
      if (chunkSection < 0) return AIR;
      LevelChunkSection section = chunk.getSections()[chunkSection];
      if (section == null) return AIR;
      BlockState data = getBlockFaster(world,section,position);
      return data;
  }
  public static BlockState getBlockFaster(@NotNull Level world, @Nullable LevelChunkSection section, @NotNull BlockPos pos) {
      BlockState state = null;
      if (section != null) {
          int x = pos.getX();
          int y = pos.getY();
          int z = pos.getZ();
          try {
              int index = (y & 15) << 8 | (z & 15) << 4 | x & 15;
              state = section.getBlockState(x & 15,y & 15,z & 15);
              //if (state.equals(AIR)) {
                  //state = section.getBlockStateUnsafe(x & 15,y & 15,z & 15);
              //}
          } catch (Throwable thr) {
              state = section.getBlockState(x & 15,y & 15,z & 15);
          }
      }
      if (state == null) state = AIR;
      return state;
  }

    public static boolean isRedstoneComponent(BlockState state) {
        return isRedstoneComponent(state.getBlock());

    }

    public static boolean isRedstoneComponent(Block block) {
        return block.defaultBlockState().hasAnalogOutputSignal() || block.defaultBlockState().isSignalSource() ||
                block instanceof FurnaceBlock ||
                block instanceof BlastFurnaceBlock ||
                block instanceof SmokerBlock ||
                block instanceof DiodeBlock ||
                block instanceof TargetBlock ||
                block instanceof PressurePlateBlock ||
                block instanceof ButtonBlock ||
                block instanceof BasePressurePlateBlock ||
                block instanceof LeverBlock ||
                block instanceof HopperBlock ||
                block instanceof ObserverBlock ||
                block instanceof DaylightDetectorBlock ||
                block instanceof DispenserBlock ||
                block instanceof DropperBlock ||
                block instanceof RedstoneLampBlock ||
                block instanceof RedstoneTorchBlock ||
                block instanceof ComparatorBlock ||
                block instanceof SculkSensorBlock ||
                block instanceof PistonBaseBlock ||
                block instanceof MovingPistonBlock ||
                block instanceof CopperBulbBlock ||
                block instanceof CrafterBlock || isToggleableRedstoneComponent(block);
    }
    public static boolean isToggleableRedstoneComponent(Block block) {
        return block instanceof PressurePlateBlock ||
                block instanceof ButtonBlock ||
                block instanceof BasePressurePlateBlock ||
                block instanceof LeverBlock ||
                block instanceof HopperBlock;
    }

  @Nullable
  public static BlockState getBlockFastest(@NotNull Level world, @NotNull BlockPos position) {
      LevelChunk chunk = (LevelChunk) world.getChunkIfLoaded(position);
      if (chunk == null) chunk = (LevelChunk) world.getChunkAt(position);
      if (chunk == null) return AIR;
      BlockState data = getBlockFaster(world,position);
      return data;
  }

  @Nullable
  public static org.bukkit.block.BlockState getBukkitState(@NotNull Location location) {
      org.bukkit.block.Block data = location.getBlock();
      if (data == null) return null;
      return data.getState(false);
  }

  @Nullable
  public static org.bukkit.block.BlockState getBukkitStateCopy(@NotNull Location location) {
      org.bukkit.block.Block data = location.getBlock();
      if (data == null) return null;
      return data.getState(true);
  }

  @Nullable
  public static org.bukkit.Material toBukkitBlockFast(@NotNull Location location) {
      if (location.getWorld() == null) return null;
      ServerLevel world = ((CraftWorld) location.getWorld()).getHandle();
      BlockPos position = locationToPosition(location);
      BlockState data = getBlockFastest(world,position);
      if (data == null) return Material.AIR;
      return data.getBukkitMaterial();
  }

  @Nullable
  public static org.bukkit.block.data.BlockData toBukkitBlockDataFast(@NotNull Location location) {
      if (location.getWorld() == null) return null;
      ServerLevel world = ((CraftWorld) location.getWorld()).getHandle();
      BlockPos position = locationToPosition(location);
      BlockState data = getBlockFastest(world,position);
      return BlockDataGetter.get(data);
  }
  @Nullable
  public static org.bukkit.block.Block getBukkitBlockFast(@NotNull Location location) {
      return getBukkitBlockFaster(location);
  }
  public static boolean isMainThread() {
    return Bukkit.isPrimaryThread();
  }
  public static boolean isMainThreadNotLevel() {
    return isMainThread();
  }

  @Nullable
  public static org.bukkit.block.Block getBukkitBlockFaster(@NotNull final Location location) {
      if (location.getWorld() == null) return null;
      ServerLevel level = ((CraftWorld) location.getWorld()).getHandle();
      if (!isMainThread())  {
        WrappedCallable<org.bukkit.block.Block> runMe = new WrappedCallable<>(() ->{
            BlockPos position = locationToPosition(location);
            LevelChunk chunk = (LevelChunk) level.getChunkIfLoaded(position);
            if (chunk == null) chunk = (LevelChunk) level.getChunkAt(position);
            org.bukkit.block.Block block = (org.bukkit.block.Block)CraftBlock.at(level, position);
            if (block == null || !block.isSolid()) block = location.getBlock();
            return block;
        });
        runTaskInWorld(runMe,location);
        return runMe.get();
      } else {
        BlockPos position = locationToPosition(location);
        LevelChunk chunk = (LevelChunk) level.getChunkIfLoaded(position);
        if (chunk == null) chunk = (LevelChunk) level.getChunkAt(position);
        org.bukkit.block.Block block = (org.bukkit.block.Block)CraftBlock.at(level, position);
        if (block == null || !block.isSolid()) block = location.getBlock();
        return block;
      }
  }
    public static org.bukkit.Chunk getChunkFastest(Location loc) {
        if (loc == null) return null;
        if (loc.getWorld() == null) return null;
        final ServerLevel world = ((CraftWorld) loc.getWorld()).getHandle();
        LevelChunk chunk = (LevelChunk) world.getChunkIfLoaded(loc.getBlockX()>>4,loc.getBlockZ()>>4);
        if (chunk == null) chunk = (LevelChunk) world.getChunk(loc.getBlockX()>>4,loc.getBlockZ()>>4);
        if (chunk == null) return null;
        final org.bukkit.Chunk bukkitChunk = new org.bukkit.craftbukkit.CraftChunk(chunk);
        return bukkitChunk;
    }
    public static net.minecraft.world.level.ChunkPos getChunkPos(int blockX, int blockZ) {
        final net.minecraft.world.level.ChunkPos chunk = new net.minecraft.world.level.ChunkPos(SectionPos.blockToSectionCoord(blockX), SectionPos.blockToSectionCoord(blockZ));
        return chunk;
    }
    public static net.minecraft.world.level.ChunkPos getChunkPos(Location loc) {
        final BlockPos pos = locationToPosition(loc);
        final net.minecraft.world.level.ChunkPos chunk = new net.minecraft.world.level.ChunkPos(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
        return chunk;
    }
    public static Set<net.minecraft.world.level.ChunkPos> getChunkSet(Collection<Location> locs) {
        final Set<net.minecraft.world.level.ChunkPos> chunks = new HashSet<>((int)(locs.size()/2));
        for (Location loc : locs) {
            if (loc == null) continue;
            chunks.add(getChunkPos(loc));
        }
        return chunks;
    }
    public static void loadChunksIn(org.bukkit.World world, Location... states) {
        runTaskInWorld(world,()->{
            final Set<net.minecraft.world.level.ChunkPos> chunks = new HashSet<>((int)(states.length/2)+25);
            for (Location loc : states) {
                if (loc == null) continue;
                chunks.add(getChunkPos(loc));
            }
            chunks.addAll(addSurroundingChunks(chunks,2));
            for (ChunkPos pos : chunks) {
                if (pos != null && pos.isValid()) world.getChunkAtAsync((int)pos.getMinBlockX()>>4,(int)pos.getMinBlockZ()>>4,true).thenAcceptAsync(chunke -> chunke.addPluginChunkTicket(SmoothSailingPlugin.getPlugin()));
            }
            com.ccorp2002.smoothsailing.region.TaskUtils.getGlobalScheduler().runTaskLaterAsync(()->{
                removeChunksToLoad(world,chunks);
                chunks.clear();
            }, 600L);
        });
    }
    public static void loadChunksIn(org.bukkit.World world, Collection<MobileBlockState> states) {
        runTaskInWorld(world,()->{
            final Set<net.minecraft.world.level.ChunkPos> chunks = new HashSet<>((int)(states.size()/2)+25);
            for (MobileBlockState loc : states) {
                if (loc == null) continue;
                chunks.add(getChunkPos(loc.toLocation(world)));
            }
            chunks.addAll(addSurroundingChunks(chunks,2));
            for (ChunkPos pos : chunks) {
                if (pos != null && pos.isValid()) world.getChunkAtAsync((int)pos.getMinBlockX()>>4,(int)pos.getMinBlockZ()>>4,true).thenAcceptAsync(chunke -> chunke.addPluginChunkTicket(SmoothSailingPlugin.getPlugin()));
            }
            com.ccorp2002.smoothsailing.region.TaskUtils.getGlobalScheduler().runTaskLaterAsync(()->{
                removeChunksToLoad(world,chunks);
                chunks.clear();
            }, 600L);
        });
    }
    public static void refreshChunksIn(org.bukkit.World world, Collection<MobileBlockState> states) {
        runTaskInWorld(world,()->{
            final Set<net.minecraft.world.level.ChunkPos> chunks = new HashSet<>((int)(states.size()/2)+25);
            for (MobileBlockState loc : states) {
                if (loc == null) continue;
                chunks.add(getChunkPos(loc.toLocation(world)));
            }
            chunks.addAll(addSurroundingChunks(chunks,2));
            for (ChunkPos pos : chunks) {
                if (pos != null && pos.isValid()) world.refreshChunk((int)pos.getMinBlockX()>>4,(int)pos.getMinBlockZ()>>4);
            }
            com.ccorp2002.smoothsailing.region.TaskUtils.getGlobalScheduler().runTaskLaterAsync(()->{
                removeChunksToLoad(world,chunks);
                chunks.clear();
            }, 600L);
        });
    }

    public static Set<net.minecraft.world.level.ChunkPos> addSurroundingChunks(Set<net.minecraft.world.level.ChunkPos> chunks, int radius) {
        if (radius > 0) {
            List<net.minecraft.world.level.ChunkPos> tmps = new ArrayList<>(chunks);
            for (net.minecraft.world.level.ChunkPos chunk : tmps) {
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        net.minecraft.world.level.ChunkPos c = getChunkPos((((int)chunk.getMinBlockX()>>4)*16),(((int)chunk.getMinBlockZ()>>4)*16));
                        chunks.add(c);
                    }
                }
            }
        }
        return chunks;
    }

    public static void removeChunksToLoad(org.bukkit.World world, Collection<org.bukkit.Chunk> list) {
        if (world == null) return;
        if (list == null) return;
        for (org.bukkit.Chunk chunk : list) {
            removeChunkToLoad(world, chunk);
        }
    }

    public static void removeChunkToLoad(org.bukkit.World world, org.bukkit.Chunk pos) {
        if (world == null) return;
        if (pos == null) return;
        if (world.isChunkLoaded((int)pos.getX(),(int)pos.getZ()) || world.isChunkForceLoaded((int)pos.getX(),(int)pos.getZ())) {
            world.removePluginChunkTicket((int)pos.getX(),(int)pos.getZ(),SmoothSailingPlugin.getPlugin());
            pos.setForceLoaded(false);
            pos.unload(true);
        }
    }


    public static void removeChunksToLoad(org.bukkit.World world, Iterable<net.minecraft.world.level.ChunkPos> list) {
        if (world == null) return;
        if (list == null) return;
        for (net.minecraft.world.level.ChunkPos chunk : list) {
            removeChunkToLoad(world, chunk);
        }
    }

    public static void removeChunkToLoad(org.bukkit.World world, net.minecraft.world.level.ChunkPos pos) {
        if (world == null) return;
        if (pos == null) return;
        if (world.isChunkLoaded((int)pos.getMinBlockX()>>4,(int)pos.getMinBlockZ()>>4) || world.isChunkForceLoaded((int)pos.getMinBlockX()>>4,(int)pos.getMinBlockZ()>>4)) {
            world.removePluginChunkTicket((int)pos.getMinBlockX()>>4,(int)pos.getMinBlockZ()>>4,SmoothSailingPlugin.getPlugin());
            world.setChunkForceLoaded((int)pos.getMinBlockX()>>4,(int)pos.getMinBlockZ()>>4,false);
        }
    }

    @Nullable
    public static BlockEntity removeBlockEntity(@NotNull Location location) {
        if (location.getWorld() == null) return null;
        if (NmsUtils.HAS_CRAFT_ENGINE) CEUtils.INSTANCE.removeCEBlock(location,false,false);
        ServerLevel world = ((CraftWorld) location.getWorld()).getHandle();
        return removeBlockEntity(world,locationToPosition(location));
    }
    @Nullable
    public static BlockEntity removeBlockEntity(@NotNull Location location, boolean deleteBlock) {
        if (location.getWorld() == null) return null;
        if (NmsUtils.HAS_CRAFT_ENGINE) CEUtils.INSTANCE.removeCEBlock(location,false,false);
        ServerLevel world = ((CraftWorld) location.getWorld()).getHandle();
        return removeBlockEntity(world,locationToPosition(location),deleteBlock);
    }
    @Nullable
    public static BlockEntity removeBlockEntity(@NotNull Level world, @NotNull BlockPos position) {
        return removeBlockEntity(world,position,false);
    }
    @Nullable
    public static BlockEntity removeBlockEntity(@NotNull Level world, @NotNull BlockPos position, boolean deleteBlock) {
        BlockEntity testEntity = ((LevelChunk) world.getChunkAt(position)).getBlockEntity(position); //world.getChunkAt(position);
        if (testEntity instanceof PistonMovingBlockEntity) //attempt to prevent piston bug
        {
            BlockState oldState;
            if (((PistonMovingBlockEntity) testEntity).isSourcePiston() && testEntity.getBlockState().getBlock() instanceof PistonBaseBlock) {
                if (((PistonMovingBlockEntity) testEntity).getMovedState().is(Blocks.PISTON))
                    oldState = Blocks.PISTON.defaultBlockState()
                            .setValue(PistonBaseBlock.FACING, ((PistonMovingBlockEntity) testEntity).getMovedState().getValue(PistonBaseBlock.FACING));
                else
                    oldState = Blocks.STICKY_PISTON.defaultBlockState()
                            .setValue(PistonBaseBlock.FACING, ((PistonMovingBlockEntity) testEntity).getMovedState().getValue(PistonBaseBlock.FACING));
            } else
                oldState = ((PistonMovingBlockEntity) testEntity).getMovedState();
            ((PistonMovingBlockEntity) testEntity).finalTick();
            setBlockFastest(world, position, oldState);
            testEntity = world.getBlockEntity(position);
        }
        try {
            ((LevelChunk) world.getChunkAt(position)).removeBlockEntity(position); //world.getChunkAt(position);
            if (deleteBlock) setBlockFastest(world,position,AIR);
            return testEntity;
        } catch (Throwable exc) {
            exc.printStackTrace();
        }
        return null;
        }

    @Nullable
    public static BlockEntity getBlockEntity(@NotNull Location location) {
        if (location.getWorld() == null) return null;
        ServerLevel world = ((CraftWorld) location.getWorld()).getHandle();
        return getBlockEntity(world,locationToPosition(location));
    }
    @Nullable
    public static BlockEntity getBlockEntity(@NotNull Level world, @NotNull BlockPos position){
        BlockEntity testEntity = ((LevelChunk) world.getChunkAt(position)).getBlockEntity(position, LevelChunk.EntityCreationType.IMMEDIATE); //world.getChunkAt(position);
        if (testEntity instanceof PistonMovingBlockEntity) //attempt to prevent piston bug
        {
            BlockState oldState;
            if (((PistonMovingBlockEntity) testEntity).isSourcePiston() && testEntity.getBlockState().getBlock() instanceof PistonBaseBlock) {
                if (((PistonMovingBlockEntity) testEntity).getMovedState().is(Blocks.PISTON))
                    oldState = Blocks.PISTON.defaultBlockState()
                            .setValue(PistonBaseBlock.FACING, ((PistonMovingBlockEntity) testEntity).getMovedState().getValue(PistonBaseBlock.FACING));
                else
                    oldState = Blocks.STICKY_PISTON.defaultBlockState()
                            .setValue(PistonBaseBlock.FACING, ((PistonMovingBlockEntity) testEntity).getMovedState().getValue(PistonBaseBlock.FACING));
            } else
                oldState = ((PistonMovingBlockEntity) testEntity).getMovedState();
            ((PistonMovingBlockEntity) testEntity).finalTick();
            setBlockFastest(world, position, oldState);
        }
        try {
            return testEntity;
        } catch (Throwable exc) {
            exc.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("all")
    public static void moveBlockEntity(@NotNull Location loc, @NotNull TileHolder tile) {
        if (loc.getWorld() == null) return;
        moveBlockEntity(loc,tile,null);
    }

    @SuppressWarnings("all")
    public static void moveBlockEntity(@NotNull Location loc, @NotNull TileHolder tile, BlockState newTileState) {
        moveReturnBlockEntity(loc,tile,newTileState);
    }

    @SuppressWarnings("all")
    public static TileHolder moveReturnBlockEntity(@NotNull Location loc, @NotNull TileHolder tile, BlockState newTileState) {
        if (loc.getWorld() == null) return tile;
        ServerLevel world = ((CraftWorld) loc.getWorld()).getHandle();
        tile.newPosition = locationToPosition(loc);
        if (tile instanceof BukkitTileHolder bth) {
            if (tile.newPosition == null) return bth;
            if (newTileState != null && newTileState.hasBlockEntity()) {
                BlockData oldBlockData = bth.tile.getBlockData();
                BlockData newBlockData = BlockDataGetter.get(newTileState);
                if (oldBlockData.getMaterial() == newBlockData.getMaterial()) bth.tile.setBlockData(newBlockData);
                else bth.tile.setBlockData(oldBlockData);
                moveTileState(world, tile.newPosition, bth.getTile());
                tile.tilePosition = tile.newPosition;
                bth.tile = getBukkitStateCopy(loc);
                return bth;
            } else {
                if (SmoothSailingPlugin.DEBUG) SmoothSailingPlugin.getPlugin().getLogger().warning("BUKKIT TILE COPY @ "+tile.newPosition.toString()+" MISSING VALID BLOCK STATE");
            }
        } else if (tile instanceof VanillaTileHolder vth) {
            if (tile.newPosition == null) return vth;
            if (newTileState != null && newTileState.hasBlockEntity() && vth.getTile().getType().isValid(newTileState)) {
                vth.getTile().setBlockState(newTileState);
                moveBlockEntity(world, tile.newPosition, vth.getTile());
                tile.tilePosition = tile.newPosition;
                vth.tile = getBlockEntity(world,tile.tilePosition);
                return vth;
            } else if (vth.getTile().getType().isValid(getBlockFaster(world,tile.newPosition))) {
                vth.getTile().setBlockState(getBlockFaster(world,tile.newPosition));
                moveBlockEntity(world, tile.newPosition, vth.getTile());
                tile.tilePosition = tile.newPosition;
                vth.tile = getBlockEntity(world,tile.tilePosition);
                return vth;
            } else {
                if (SmoothSailingPlugin.DEBUG) SmoothSailingPlugin.getPlugin().getLogger().warning("NMS TILE COPY @ "+tile.newPosition.toString()+" MISSING VALID BLOCK STATE");
            }
        } else if (CE_UTILS != null && tile instanceof CEUtils.CraftEngineTileHolder ceth) {
            if (tile.newPosition == null) return ceth;
            CE_UTILS.moveBlockEntity(world, tile.newPosition, ceth.getTile());
            tile.tilePosition = tile.newPosition;
            ceth.tile = CE_UTILS.getCEBlockEntity(loc);
            return ceth;
        }
        return tile;
    }

  @SuppressWarnings("all")
  public static void moveBlockEntity(@NotNull Location loc, @NotNull BlockEntity tile) {
    if (loc.getWorld() == null) return;
    ServerLevel world = ((CraftWorld) loc.getWorld()).getHandle();
    moveBlockEntity(world,locationToPosition(loc),tile);
  }
    @SuppressWarnings("all")
    public static void moveBlockEntity(@NotNull Level nativeWorld, @NotNull BlockPos newPosition, @NotNull BlockEntity tile) {
        final BlockPos oldPos = tile.getBlockPos();
        final BlockState state = tile.getBlockState();
        final Level oldLevel = tile.getLevel();
        try {
            LevelChunk chunk = (LevelChunk) nativeWorld.getChunkAt(newPosition);
            if (chunk == null) chunk = (LevelChunk) nativeWorld.getChunk(newPosition.getX() >> 4, newPosition.getZ() >> 4, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, true); //world.getChunkAt(position);
            try {
                var positionField = BlockEntity.class.getDeclaredField("worldPosition"); // o is obfuscated worldPosition
                positionField.setAccessible(true);
                setField(positionField, tile, newPosition);
            } catch (Throwable e) {
                try {
                    tile.worldPosition = newPosition;
                } catch (Throwable thr) {if (SmoothSailingPlugin.DEBUG) thr.printStackTrace();}
                if (SmoothSailingPlugin.DEBUG) e.printStackTrace();
            }
            if (nativeWorld.captureBlockStates) {
                oldLevel.capturedBlockEntities.remove(oldPos);
            }
            tile.setLevel(nativeWorld);
            try {
                nativeWorld.setBlockEntity(tile);
            } catch (Throwable thr) {}
            tile.setChanged();
            tile.clearRemoved();
            chunk.blockEntities.put(newPosition.immutable(), tile);
        } catch (Throwable thr) {
            thr.printStackTrace();
        }
    }

    @SuppressWarnings("all")
    public static void moveTileState(@NotNull Location loc, @NotNull org.bukkit.block.BlockState tile) {
        if (loc.getWorld() == null) return;
        ServerLevel world = ((CraftWorld) loc.getWorld()).getHandle();
        moveTileState(world,locationToPosition(loc),tile);
    }
    @SuppressWarnings("all")
    public static void moveTileState(@NotNull Level nativeWorld, @NotNull BlockPos newPosition, @NotNull org.bukkit.block.BlockState tile) {
        final Location loc = positionToLocation(newPosition,nativeWorld);
        try {
            org.bukkit.craftbukkit.block.CraftBlockState placeMe = (org.bukkit.craftbukkit.block.CraftBlockState)tile.copy(loc);
            placeMe.revertPlace(true);
        } catch (Error thr) {
            thr.printStackTrace();
        }
    }

  public static BlockState getBlockState(@NotNull BlockData data) {
      BlockState blockData;
      if (data instanceof CraftBlockData) {
          blockData = ((CraftBlockData) data).getState();
      }
      else {
          blockData = (BlockState) data;
      }
      return blockData;
  }

  public static void setBlockFast(@NotNull Location location, @NotNull BlockState blockData) {
      Level world = ((CraftWorld) (location.getWorld())).getHandle();
      BlockPos blockPos = locationToPosition(location);
      setBlockFaster(world, blockPos, blockData, false);
  }

  public static void setBlockFast(@NotNull Location location, @NotNull BlockData data) {
      BlockState blockData;
      if (data instanceof CraftBlockData) {
          blockData = ((CraftBlockData) data).getState();
      } else {
          blockData = (BlockState) data;
      }
      setBlockFast(location, blockData);
  }

  public static void setBlockFaster(@NotNull Level world, @NotNull BlockPos position, @NotNull BlockState data) {
    setBlockFaster(world,position,data,false);
  }

  public static void setBlockFaster(@NotNull Level world, @NotNull BlockPos position, @NotNull BlockState data, boolean sendChanges) {
      LevelChunk chunk = (LevelChunk) world.getChunkIfLoaded(position);
      if (chunk == null) chunk = (LevelChunk) world.getChunkAt(position);
      int chunkSection = (position.getY() >> 4) - WorldUtil.getMinSection(world);
      LevelChunkSection section = chunk.getSections()[chunkSection];
      if (section == null) {
          chunk.setBlockState(position, Blocks.GLASS.defaultBlockState(), 0);
          section = chunk.getSections()[chunkSection];
      }
      if (getBlockFastest(world,position) != data) setBlockPaletteState(world,section,position,data);
      chunk.persistentDataContainer.dirty(true);
      chunk.setLightCorrect(true);
      if (sendChanges) {
        renderBlockChanges(world,position);
      }
      chunk.markUnsaved();
  }

    public static void renderBlockChanges(@NotNull Location pos) {
        if (pos.getWorld() == null) return;
        ServerLevel world = (ServerLevel)((CraftWorld) pos.getWorld()).getHandle();
        renderBlockChanges(world,locationToPosition(pos));
        sendBlockChange(pos,null);
    }

    public static void renderBlockChanges(@NotNull Level world, @NotNull BlockPos position) {
        ((ServerLevel)world).getChunkSource().blockChanged(position);

    }

    public static void setChunkBlockState(Level world, LevelChunk chunk, BlockPos pos, BlockState state) {
        if (chunk == null) {
            world.setBlock(pos,state,1,0);
            if (!NmsUtils.HAS_CRAFT_ENGINE) return;
            final net.momirealms.craftengine.core.world.CEWorld ceWorld = net.momirealms.craftengine.bukkit.world.BukkitWorldManager.instance().getWorld(world.getWorld());
            final net.momirealms.craftengine.core.world.BlockPos cebpos = net.momirealms.craftengine.core.world.BlockPos.of(pos.getX(), pos.getY(), pos.getZ());
            net.momirealms.craftengine.core.world.chunk.CEChunk ceChunk = getCEChunkOrNull(ceWorld,pos);
            var imm = net.momirealms.craftengine.bukkit.util.BlockStateUtils.getOptionalCustomBlockState(state).orElse(null);
            if (ceChunk != null) {
                ceChunk.setBlockState(cebpos,net.momirealms.craftengine.core.block.EmptyBlockDefinition.STATE);
                if (imm != null) {
                    ceChunk.setBlockState(cebpos,imm);
                    if (imm.hasBlockEntity()) {
                        var ceBe = ceChunk.getBlockEntity(cebpos, false);
                        if (ceBe != null) {
                            ceBe.setBlockState(imm);
                        }
                    }
                } else {
                    ceChunk.removeConstantBlockEntityRenderer(cebpos);
                    ceChunk.removeDynamicBlockEntityRenderer(cebpos);
                }
            }
        } else {
            try {
                BlockState oldState = AIR;
                if (NmsUtils.HAS_CRAFT_ENGINE) {
                    //var oldImm = net.momirealms.craftengine.bukkit.util.BlockStateUtils.getOptionalCustomBlockState(oldState).orElse(null);
                    final net.momirealms.craftengine.core.world.CEWorld ceWorld = net.momirealms.craftengine.bukkit.world.BukkitWorldManager.instance().getWorld(world.getWorld());
                    final net.momirealms.craftengine.core.world.BlockPos cebpos = net.momirealms.craftengine.core.world.BlockPos.of(pos.getX(), pos.getY(), pos.getZ());
                    net.momirealms.craftengine.core.world.chunk.CEChunk ceChunk = getCEChunkOrNull(ceWorld,pos);
                    var imm = net.momirealms.craftengine.bukkit.util.BlockStateUtils.getOptionalCustomBlockState(state).orElse(null);
                    if (ceChunk != null) {
                        ceChunk.setBlockState(cebpos,net.momirealms.craftengine.core.block.EmptyBlockDefinition.STATE);
                        if (imm != null) {
                            ceChunk.setBlockState(cebpos,imm);
                        } else {
                            ceChunk.removeConstantBlockEntityRenderer(cebpos);
                            ceChunk.removeDynamicBlockEntityRenderer(cebpos);
                        }
                    }
                }
                oldState = chunk.setBlockState(pos, state, NOTIFY);
            } catch (Throwable thr1) {
                chunk.setBlockState(pos, state, NOTIFY);
            }
        }
    }

    public static void setBlockPaletteState(Level world, LevelChunkSection chunkSection, BlockPos pos, BlockState state) {
        if (chunkSection == null) {
            //Location loc = org.bukkit.craftbukkit.util.CraftLocation.toBukkit(pos,world);
            //net.momirealms.craftengine.bukkit.api.CraftEngineBlocks.place(loc,net.momirealms.craftengine.core.block.EmptyBlockDefinition.STATE,UPDATE_OPTS,false);
            if (NmsUtils.HAS_CRAFT_ENGINE) {
                //var oldImm = net.momirealms.craftengine.bukkit.util.BlockStateUtils.getOptionalCustomBlockState(oldState).orElse(null);
                final net.momirealms.craftengine.core.world.CEWorld ceWorld = net.momirealms.craftengine.bukkit.world.BukkitWorldManager.instance().getWorld(world.getWorld());
                final net.momirealms.craftengine.core.world.BlockPos cebpos = net.momirealms.craftengine.core.world.BlockPos.of(pos.getX(), pos.getY(), pos.getZ());
                net.momirealms.craftengine.core.world.chunk.CEChunk ceChunk = getCEChunkOrNull(ceWorld,pos);
                var imm = net.momirealms.craftengine.bukkit.util.BlockStateUtils.getOptionalCustomBlockState(state).orElse(null);
                if (ceChunk != null) {
                    if (imm != null) {
                        var ceBe = ceChunk.blockEntities.remove(cebpos.asLong());
                        if (ceBe != null) {
                            ceBe.setBlockState(imm);
                        }
                        ceChunk.removeBlockEntityTicker(cebpos);
                        ceChunk.removeDynamicBlockEntityRenderer(cebpos);
                        ceChunk.setBlockState(cebpos,imm);
                    } else {
                        ceChunk.removeBlockEntityTicker(cebpos);
                        ceChunk.removeConstantBlockEntityRenderer(cebpos);
                        ceChunk.removeDynamicBlockEntityRenderer(cebpos);
                    }
                    ceChunk.setBlockState(cebpos,net.momirealms.craftengine.core.block.EmptyBlockDefinition.STATE);
                }
            }
            world.setBlock(pos,state,1,0);
        } else {
            try {
                //Location loc = org.bukkit.craftbukkit.util.CraftLocation.toBukkit(pos,world);
                //net.momirealms.craftengine.bukkit.api.CraftEngineBlocks.place(loc,net.momirealms.craftengine.core.block.EmptyBlockDefinition.STATE,UPDATE_OPTS,false);
                BlockState oldState = AIR;
                if (NmsUtils.HAS_CRAFT_ENGINE) {
                    //var oldImm = net.momirealms.craftengine.bukkit.util.BlockStateUtils.getOptionalCustomBlockState(oldState).orElse(null);
                    final net.momirealms.craftengine.core.world.CEWorld ceWorld = net.momirealms.craftengine.bukkit.world.BukkitWorldManager.instance().getWorld(world.getWorld());
                    final net.momirealms.craftengine.core.world.BlockPos cebpos = net.momirealms.craftengine.core.world.BlockPos.of(pos.getX(), pos.getY(), pos.getZ());
                    net.momirealms.craftengine.core.world.chunk.CEChunk ceChunk = getCEChunkOrNull(ceWorld,pos);
                    var imm = net.momirealms.craftengine.bukkit.util.BlockStateUtils.getOptionalCustomBlockState(state).orElse(null);
                    if (ceChunk != null) {
                        if (imm != null) {
                            var ceBe = ceChunk.blockEntities.remove(cebpos.asLong());
                            if (ceBe != null) {
                                ceBe.setBlockState(imm);
                            }
                            ceChunk.removeBlockEntityTicker(cebpos);
                            ceChunk.removeDynamicBlockEntityRenderer(cebpos);
                            ceChunk.setBlockState(cebpos,imm);
                        } else {
                            ceChunk.removeBlockEntityTicker(cebpos);
                            ceChunk.removeConstantBlockEntityRenderer(cebpos);
                            ceChunk.removeDynamicBlockEntityRenderer(cebpos);
                        }
                        ceChunk.setBlockState(cebpos,net.momirealms.craftengine.core.block.EmptyBlockDefinition.STATE);
                    }
                }
                oldState = chunkSection.setBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, state, false);
            } catch (Throwable thr1) {
                chunkSection.setBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, state, false);
            }
        }
    }

    public static void UPDATE_LIGHT_AT(Location loc) {
        if (loc == null) return;
        if (loc.getWorld() == null) return;
        BlockPos pos = locationToPosition(loc);
        ServerLevel world = getLevel(getLevel(loc));
        world.getLightEngine().checkBlock(pos);
    }


    public static final int UPDATE_REDSTONE_AT_FLAG = Block.UPDATE_SKIP_ALL_SIDEEFFECTS;

    public static void UPDATE_REDSTONE_AT(BlockState data, BlockPos pos, Level world) {
        if (pos == null) return;
        if (data == null) return;
        if (world == null) return;
        if (!isMainThread())  {
            runOnMain(()->{
                if (isRedstoneComponent(data.getBlock())) {
                    world.setBlock(pos,data,UPDATE,128);
                    if (isToggleableRedstoneComponent(data.getBlock())) {
                        data.tick((ServerLevel)world,pos,RANDOM);
                    }
                }
            });
            return;
        } else {
            if (isRedstoneComponent(data.getBlock())) {
                world.setBlock(pos,data,UPDATE,128);
                if (isToggleableRedstoneComponent(data.getBlock())) {
                    data.tick((ServerLevel)world,pos,RANDOM);
                }
            }
        }
    }
    public static void UPDATE_REDSTONE_AT(Location loc) {
        if (loc == null) return;
        if (loc.getWorld() == null) return;
        BlockState data = getBlockFaster(loc);
        if (data == null) return;
        BlockPos pos = locationToPosition(loc);
        ServerLevel world = getLevel(getLevel(loc));
        UPDATE_REDSTONE_AT(data,pos,world);
    }
    public static void UPDATE_REDSTONE_AT(BlockPos pos, Level world) {
        BlockState data = getBlockFastest(world,pos);
        if (data == null) return;
        UPDATE_REDSTONE_AT(data,pos,world);
    }
    public static ServerLevel getLevel(Level world) {
        if (world == null) return null;
        else return (ServerLevel)world;
    }
    public static Level getLevel(World world) {
        if (world == null) return null;
        else return (ServerLevel)((CraftWorld) world).getHandle();
    }
    public static Level getLevel(Location loc) {
        if (loc == null) return null;
        else if (loc.getWorld() == null) return null;
        else return (ServerLevel)((CraftWorld) loc.getWorld()).getHandle();
    }
  public static BlockEntity readTagIntoTileEntity(net.minecraft.nbt.CompoundTag tag, BlockEntity tileEntity) {
    if (!tileEntity.hasLevel()) return tileEntity;
      tileEntity.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, tileEntity.getLevel().registryAccess(), tag));
      tileEntity.setChanged();
      return tileEntity;
  }
  public static net.minecraft.nbt.CompoundTag readTileEntityIntoTag(BlockEntity tileEntity, Level level) {
    if (!tileEntity.hasLevel() && level != null) tileEntity.setLevel(level);
    net.minecraft.nbt.CompoundTag tag = tileEntity.saveWithFullMetadata(org.bukkit.craftbukkit.CraftRegistry.getMinecraftRegistry());
    tag.putBoolean("keepPacked", false);
    return tag;
  }

  public static String toTitleCase(String input) {
      StringBuilder titleCase = new StringBuilder(input.length());
      boolean nextTitleCase = true;

      for (char c : input.toCharArray()) {
          if (Character.isSpaceChar(c)) {
              nextTitleCase = true;
          } else if (nextTitleCase) {
              c = Character.toTitleCase(c);
              nextTitleCase = false;
          }

          titleCase.append(c);
      }

      return titleCase.toString();
  }

    public static boolean runTaskInWorld(org.bukkit.World world, Runnable runMe) {
        return runTaskInWorld(runMe,world);
    }

    public static boolean runTaskInWorld(Runnable runMe, org.bukkit.World world) {
        boolean ran = false;
        if (runMe == null) return ran;
        if (world != null) {
            return runTaskInWorld(runMe, new Location(world,0,120,0));
        } else {
            ran = runOnMain(runMe);
        }
        return ran;
    }

    public static boolean runTaskInWorld(Location pos, Runnable runMe) {
        return runTaskInWorld(runMe,pos);
    }

    public static boolean runTaskInWorld(Runnable runMe, Location pos) {
        boolean ran = false;
        if (runMe == null) return ran;
        if (pos != null && pos.getWorld() != null) {
            com.ccorp2002.smoothsailing.region.TaskUtils.getRegionalScheduler(pos).runTask(runMe);
            return true;
        } else {
            ran = runOnMain(runMe);
        }
        return ran;
    }
    public static boolean runOnMain(Runnable runMe) {
        try {
            net.serlith.fish.async.AsyncWorldTicking.scheduleVoidForEndOfTick(runMe);
        } catch (Error err) {
            com.ccorp2002.smoothsailing.region.TaskUtils.getGlobalScheduler().runTask(runMe);
        }
        return true;
    }

  
  public static class WorldBlockVector extends BlockVector {
    public String worldName;
    public java.lang.ref.WeakReference<World> worldRef = null;
    public WorldBlockVector(Location loc) {
      super(loc.toVector());
      this.setWorld(loc.getWorld());
    }
    
    public boolean setWorld(World w) {
      if (w == null) return false;
      this.worldRef = new java.lang.ref.WeakReference<>(w);
      return setWorldName(w.getName());
    }
    public boolean setWorldName(World w) {
      if (w == null) return false;
      this.worldRef = new java.lang.ref.WeakReference<>(w);
      return setWorldName(w.getName());
    }
    public String getWorldName() {
      if (this.worldName != null) return this.worldName;
      return "";
    }
    public boolean setWorldName(String w) {
      if (this.worldName != null) return false;
      this.worldName = w;
      return true;
    }
    public Location toLocation() {
      if (this.worldName == null) return null;
      if (this.worldRef == null || this.worldRef.get() == null) return null;
      return this.toLocation(worldRef.get()).toBlockLocation();
    }
    @Override
    public Location toLocation(World world) {
      if (world == null) return null;
      return super.toLocation(world).toBlockLocation();
    }
    @Override
    public boolean equals(Object oth) {
      if (oth == null) return false;
      if (oth instanceof Location loc) {
        return this.toLocation().toBlockLocation().equals(loc.toBlockLocation());
      } else if (oth instanceof BlockVector bv) {
        return ((int)this.getBlockX() == (int)bv.getBlockX() && (int)this.getBlockZ() == (int)bv.getBlockZ() && (int)this.getBlockY() == (int)bv.getBlockY());
      } else if (oth instanceof Vector bv) {
        return ((int)this.getBlockX() == (int)bv.getBlockX() && (int)this.getBlockZ() == (int)bv.getBlockZ() && (int)this.getBlockY() == (int)bv.getBlockY());
      }
      return false;
    }
    public int getXi() {
      return (int)this.getBlockX();
    }
    public int getYi() {
      return (int)this.getBlockY();
    }
    public int getZi() {
      return (int)this.getBlockZ();
    }
  }

    public static void removeWorldVector(Location loc) {
        final WorldBlockVector wv = new WorldBlockVector(loc);
        SENT_BLOCKS.remove(wv);
    }
    public static Rotation getRotation(String ROT) {
        if (ROT == null) return Rotation.NONE;
        else if (ROT.isEmpty()) return Rotation.NONE;
        else if (ROT.equalsIgnoreCase("LEFT") || PDCUtils.containsIgnoreCase(ROT,"ANTICLOCK") || PDCUtils.containsIgnoreCase(ROT,"COUNTERCLOCK")) return Rotation.COUNTERCLOCKWISE_90;
        else if (ROT.equalsIgnoreCase("RIGHT") || PDCUtils.containsIgnoreCase(ROT,"CLOCKWISE")) return Rotation.CLOCKWISE_90;
        else return Rotation.NONE;
    }
    public static BlockData rotateBlockData(String ROT, BlockData input) {
        if (ROT == null) return input;
        else if (ROT.isEmpty()) return input;
        else return getBlockData(getBlockState(input).rotate(getRotation(ROT)));
    }

  public static void removeEntity(org.bukkit.entity.Entity entity) {
    if (entity == null) return;
    try {
        ServerLevel nmsWorld = (ServerLevel)(((org.bukkit.craftbukkit.CraftWorld) entity.getWorld()).getHandle());
        Entity nmsEntity = ((org.bukkit.craftbukkit.entity.CraftEntity) entity).getHandle();
        nmsEntity.remove(Entity.RemovalReason.KILLED);
        nmsEntity.kill(nmsWorld);
        nmsEntity.discard();
        entity.remove();
    } catch (Throwable thr) {
        entity.remove();
    }
  }

  public static void hideEntity(org.bukkit.entity.Player player, org.bukkit.entity.Entity entity) {
    hideEntities(player,List.of(entity));
  }

  public static void hideEntities(org.bukkit.entity.Player player, Collection<org.bukkit.entity.Entity> entities) {
    try {
      it.unimi.dsi.fastutil.ints.IntArrayList idList = new it.unimi.dsi.fastutil.ints.IntArrayList(entities.size());
      net.minecraft.server.level.ServerPlayer srvp = null;
      if (player != null) srvp = ((org.bukkit.craftbukkit.entity.CraftPlayer) player).getHandle();
      for (org.bukkit.entity.Entity entity : entities) {
        if (entity instanceof org.bukkit.entity.Player) continue;
        if (PDCUtils.getPDC(entity,"is_seat").equals("true")) continue;
        if (!player.canSee(entity)) continue;
        if (!entity.isTrackedBy(player)) continue;
        idList.add(entity.getEntityId());
        Entity nmsEntity = ((org.bukkit.craftbukkit.entity.CraftEntity) entity).getHandle();
        final ChunkMap tracker = ((ServerLevel)nmsEntity.level()).getChunkSource().chunkMap;
        ChunkMap.TrackedEntity tracked = tracker.entityMap.get(nmsEntity.getId());
      }
      if (player != null && srvp != null) srvp.connection.send(new net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket(idList));
    } catch (Throwable thr) {
      thr.printStackTrace();
    }
  }
  public static void teleportSilent(final Location loc, org.bukkit.entity.Entity entity) {
    if (loc == null) return;
    if (entity == null) return;
    if (loc.getWorld() == null) return;
    boolean GSIT = entity.getScoreboardTags().contains("GSit_sit") || entity.getScoreboardTags().contains("GSit_SeatEntity");;
    if (GSIT) {
        entity.removeScoreboardTag("GSit_sit");
        entity.removeScoreboardTag("GSit_SeatEntity");
    }
    for (org.bukkit.entity.Entity passeng : entity.getPassengers()) {
        if (passeng == null) continue;
        passeng.removeScoreboardTag("GSit_sit");
        passeng.removeScoreboardTag("GSit_SeatEntity");
        if (passeng instanceof Player player) continue;
        passeng.setRotation(loc.getYaw(),passeng.getLocation().getPitch());
    }
    if (!loc.getWorld().equals(entity.getWorld())) loadChunksIn(loc.getWorld(),loc);
    if (entity instanceof Player player) {
        player.teleportAsync(
                loc, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.UNKNOWN,
                io.papermc.paper.entity.TeleportFlag.Relative.X,
                io.papermc.paper.entity.TeleportFlag.Relative.Y,
                io.papermc.paper.entity.TeleportFlag.Relative.Z,
                io.papermc.paper.entity.TeleportFlag.Relative.PITCH,
                io.papermc.paper.entity.TeleportFlag.Relative.YAW,
                io.papermc.paper.entity.TeleportFlag.EntityState.RETAIN_OPEN_INVENTORY,
                io.papermc.paper.entity.TeleportFlag.EntityState.RETAIN_VEHICLE,
                io.papermc.paper.entity.TeleportFlag.EntityState.RETAIN_PASSENGERS);
        player.setVelocity(EMPTY_VEC);
    } else {
        entity.teleport(
                loc, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.UNKNOWN,
                io.papermc.paper.entity.TeleportFlag.Relative.X,
                io.papermc.paper.entity.TeleportFlag.Relative.Y,
                io.papermc.paper.entity.TeleportFlag.Relative.Z,
                io.papermc.paper.entity.TeleportFlag.Relative.PITCH,
                io.papermc.paper.entity.TeleportFlag.Relative.YAW,
                io.papermc.paper.entity.TeleportFlag.EntityState.RETAIN_OPEN_INVENTORY,
                io.papermc.paper.entity.TeleportFlag.EntityState.RETAIN_VEHICLE,
                io.papermc.paper.entity.TeleportFlag.EntityState.RETAIN_PASSENGERS);
    }
    if (GSIT) {
        entity.addScoreboardTag("GSit_sit");
        entity.addScoreboardTag("GSit_SeatEntity");
    }
  }

  public static void teleportSilentCurrentThread(TeleportTransition teleportTransition, Entity nmsEntity) throws Throwable {
        if (nmsEntity == null) return;
        if (teleportTransition == null) return;
        Method method = Entity.class.getDeclaredMethod("calculatePassengerTransition", TeleportTransition.class, Entity.class);
        method.setAccessible(true);
        for (Entity passenger : nmsEntity.getPassengers()) {
            TeleportTransition next = (TeleportTransition) method.invoke(nmsEntity, teleportTransition, passenger);
            if (passenger.getBukkitEntity() instanceof org.bukkit.entity.LivingEntity le) le.setInvulnerable(false);
            teleportSilentCurrentThread(next,passenger);
        }
        Method method2 = Entity.class.getDeclaredMethod("sendTeleportTransitionToRidingPlayers", TeleportTransition.class);
        method2.setAccessible(true);
        nmsEntity.teleportSetPosition(net.minecraft.world.entity.PositionMoveRotation.of(teleportTransition), teleportTransition.relatives());
        if (!teleportTransition.asPassenger()) {
            method2.invoke(nmsEntity, teleportTransition);
        }
        teleportTransition.postTeleportTransition().onTransition(nmsEntity);
  }

  public static void teleportSilentCurrentThread(Location loc, Entity nmsEntity) {
    try {
        if (loc == null) return;
        if (nmsEntity == null) return;
        if (loc.getWorld() == null) return;
        if (!loc.getWorld().equals(nmsEntity.getBukkitEntity().getWorld())) {
            if (SmoothSailingPlugin.DEBUG) System.out.println("NMS MOVEMENT FAILED (DIFF WORLDS)");
            runOnMain(()->{
                teleportSilent(loc,nmsEntity.getBukkitEntity());
            });
            return;
        } else {
            if (nmsEntity.getBukkitEntity().getVehicle() != null) return;
            net.minecraft.world.phys.Vec3 pos = org.bukkit.craftbukkit.util.CraftLocation.toVec3(loc);
            ServerLevel nmsWorld = (ServerLevel)(((org.bukkit.craftbukkit.CraftWorld) loc.getWorld()).getHandle());
            net.minecraft.world.phys.Vec3 vel = net.minecraft.world.phys.Vec3.ZERO;
            TeleportTransition teleport = new TeleportTransition(nmsWorld,pos,vel,loc.getYaw(),nmsEntity.getBukkitEntity().getPitch(),TeleportTransition.DO_NOTHING);
            teleport = teleport.withRelatives(RELATIVE_ROTS);
            teleportSilentCurrentThread(teleport,nmsEntity);
            if (nmsEntity instanceof net.minecraft.server.level.ServerPlayer srvp) {
                srvp.connection.internalTeleport(net.minecraft.world.entity.PositionMoveRotation.of(srvp), RELATIVE_ROTS);
                srvp.connection.resetPosition();
            }
        }
    } catch (Throwable thr) {
        if (SmoothSailingPlugin.DEBUG) System.out.println("NMS MOVEMENT FAILED (ERROR)");
        thr.printStackTrace();
        runOnMain(()->{
            teleportSilent(loc,nmsEntity.getBukkitEntity());
        });
    }
  }

  public static final Set<net.minecraft.world.entity.Relative> RELATIVE_ROTS = Set.of(net.minecraft.world.entity.Relative.ROTATE_DELTA); //net.minecraft.world.entity.Relative.X_ROT,net.minecraft.world.entity.Relative.Y_ROT

  public static void teleportSilentCurrentThread(Location loc, org.bukkit.entity.Entity entity) {
    try {
        if (loc == null) return;
        if (entity == null) return;
        if (loc.getWorld() == null) return;
        if (!SmoothSailingPlugin.NMS_TELEPORT) {
            teleportSilent(loc,entity);
            return;
        }
        if (!loc.getWorld().equals(entity.getWorld())) {
            if (SmoothSailingPlugin.DEBUG) System.out.println("NMS MOVEMENT FAILED (DIFF WORLDS)");
            runOnMain(()->{
                if (loc == null) return;
                if (entity == null) return;
                if (loc.getWorld() == null) return;
                teleportSilent(loc,entity);
            });
            return;
        } else {
            if (entity.getVehicle() != null) return;

            if (entity instanceof Display && !ShipListener.isSeat(entity)) {
                loc.setPitch(entity.getPitch());
                loc.setYaw(entity.getYaw());
            }
            net.minecraft.world.phys.Vec3 pos = org.bukkit.craftbukkit.util.CraftLocation.toVec3(loc);
            ServerLevel nmsWorld = (ServerLevel)(((org.bukkit.craftbukkit.CraftWorld) loc.getWorld()).getHandle());
            Entity nmsEntity = ((org.bukkit.craftbukkit.entity.CraftEntity) entity).getHandle();
            net.minecraft.world.phys.Vec3 vel = net.minecraft.world.phys.Vec3.ZERO;

            if (entity instanceof Player) {
                var vec = entity.getVelocity();
                vec = vec.multiply(0.5d);
                vel = org.bukkit.craftbukkit.util.CraftLocation.toVec3(vec.toLocation(loc.getWorld()));
            }
            TeleportTransition teleport = new TeleportTransition(nmsWorld,pos,vel,loc.getYaw(),entity.getPitch(),TeleportTransition.DO_NOTHING);
            teleport = teleport.withRelatives(RELATIVE_ROTS);
            teleportSilentCurrentThread(teleport,nmsEntity);
            if (entity instanceof Player player) {
                net.minecraft.server.level.ServerPlayer srvp = ((org.bukkit.craftbukkit.entity.CraftPlayer) player).getHandle();
                srvp.connection.internalTeleport(net.minecraft.world.entity.PositionMoveRotation.of(srvp),RELATIVE_ROTS);
                srvp.connection.resetPosition();
            }
            //final List<org.bukkit.entity.Entity> passengers = entity.getPassengers();
            //org.bukkit.entity.Entity bukkit = nmsEntity.getBukkitEntity();
            //passengers.forEach(p -> {bukkit.addPassenger(p);});
            //passengers.clear();
        }
    } catch (Throwable thr) {
        if (SmoothSailingPlugin.DEBUG) System.out.println("NMS MOVEMENT FAILED (ERROR)");
        thr.printStackTrace();
        runOnMain(()->{
            if (loc == null) return;
            if (entity == null) return;
            if (loc.getWorld() == null) return;
            teleportSilent(loc,entity);
        });
    }
  }

  public static final it.unimi.dsi.fastutil.objects.Object2ObjectMap<WorldBlockVector,BlockData> SENT_BLOCKS = new it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<>(12500,0.75f);

  public static void sendBlockChange(Location loc, BlockData data) {
    if (loc == null) return;
    if (loc.getWorld() == null) return;
    if (data == null) data = loc.getBlock().getBlockData();
    final BlockData temp = data;
    runTaskInWorld(loc,()->{
        for (Player player : loc.getWorld().getPlayers()) {
            if (player.getLocation().distance(loc) >= 480) continue;
            player.sendBlockChange(loc,temp);
        }
    });
  }

  public static void resetBlocks(World world) {
    if (world == null) return;
    try {
        Map<Location,BlockData> batch = new HashMap<>(SENT_BLOCKS.size());
        for (WorldBlockVector wv : SENT_BLOCKS.keySet()) {
            final Location loc = wv.toLocation(world);
            if (loc.isChunkLoaded()) sendBlockChange(loc,loc.getBlock().getBlockData());
            SENT_BLOCKS.remove(wv);
        }
    } catch (Throwable thr) {}
  }

  public static void resetBlocks(World world, Collection<Location> locs) {
    if (world != null) {
        runTaskInWorld(world,()->{
            for (Location loc : locs) {
                if (loc.isChunkLoaded()) renderBlockChanges(loc);
            }
        });
    } else {
        for (Location loc : locs) {
            if (loc.isChunkLoaded()) renderBlockChanges(loc);
        }
    }
  }

  public static void sendBlockChanges(World world, Map<Location,BlockData> batch) {
    if (batch == null || batch.isEmpty()) return;
    if (world != null) {
        runTaskInWorld(world,()->{
            for (Player player : world.getPlayers()) {
                Set<org.bukkit.block.BlockState> STATES = new HashSet<>(batch.size());
                for (final Location loc : batch.keySet()) {
                    if (loc == null) continue;
                    if (loc.getWorld() == null) continue;
                    if (!loc.getWorld().equals(world)) continue;
                    BlockData data = batch.getOrDefault(loc,BlockDataGetter.get(getBlockFaster(loc)));
                    org.bukkit.block.BlockState state = data.createBlockState();
                    state = state.copy(loc);
                    STATES.add(state);
                }
                sendBlockChanges(STATES, world);
            }
        });
    } else {
      for (Player player : Bukkit.getOnlinePlayers()) {
        player.sendMultiBlockChange(batch);
      }
    }
  }

    public static void sendBlockChanges(Set<org.bukkit.block.BlockState> STATES, World world) {
        if (world == null) return;
        for (Player player : world.getPlayers()) {
            player.sendBlockChanges(STATES);
        }
    }

    public static void sendBlockDataChanges(Collection<Location> locs, BlockData data) {
        if (locs == null || locs.isEmpty()) return;
        World world = null;
        for (Location loc : locs) {
        if (loc != null) {
            if (world == null) world = loc.getWorld();
            sendBlockChange(loc,data);
        }
        }
    }
    public static BlockData getBlockData(BlockState state) {
        return BlockDataGetter.get(state);
    }

    public static NextTickProvider.TickHolder GET_TICK_FROM(Location from) {
        if (from == null) return null;
        else if (from.getWorld() == null) return null;
        BlockPos fromPos = locationToPosition(from);
        ServerLevel fromLvl = getLevel(getLevel(from));
        final ScheduledTick TICK = TICK_PROVIDER.getNextTick(fromLvl, fromPos);
        if (TICK != null) {
            NextTickProvider.TickHolder MOVING_TICK = new NextTickProvider.TickHolder(TICK, fromPos);
            return MOVING_TICK;
        } else {
            return null;
        }
    }

    public static boolean MOVE_TICK_TO(NextTickProvider.TickHolder MOVING_TICK, Location to) {
        if (to == null) return false;
        else if (to.getWorld() == null) return false;
        if (MOVING_TICK != null) {
            BlockPos toPos = locationToPosition(to);
            ServerLevel toLvl = getLevel(getLevel(to));
            ((LevelChunkTicks) ((LevelChunk) toLvl.getChunk(toPos.getX() >> 4, toPos.getZ() >> 4, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, true)).getBlockTicks()).removeIf(
                    (Predicate<ScheduledTick>) scheduledTick -> scheduledTick.equals(MOVING_TICK.getTick()));
            final long currentTime = toLvl.getGameTime();
            toLvl.getBlockTicks().schedule(new ScheduledTick<>((Block) MOVING_TICK.getTick().type(), toPos, MOVING_TICK.getTick().triggerTick() - currentTime, MOVING_TICK.getTick().priority(), MOVING_TICK.getTick().subTickOrder()));
            return true;
        } else {
            return false;
        }
    }


    public static abstract class BlockDataGetter {
        BlockDataGetter() {}
        public static BlockDataGetter INSTANCE = null;
        public static BlockDataGetter INSTANCE() {
            if (INSTANCE != null) return INSTANCE;
            if (Bukkit.getServer().getMinecraftVersion().contains("1.21") || Bukkit.getServer().getMinecraftVersion().contains("1.20") || Bukkit.getServer().getMinecraftVersion().contains("1.19") || Bukkit.getServer().getMinecraftVersion().contains("1.18")) {
                INSTANCE = new BlockDataGetter_21_x();
            } else {
                INSTANCE = new BlockDataGetter_26_x();
            }
            if (INSTANCE == null) INSTANCE = new BlockDataGetter_21_x();
            return INSTANCE;
        }
        public static BlockData get(BlockState state) {
            return BlockDataGetter.INSTANCE().getBlockData(state);
        }
        public abstract BlockData getBlockData(BlockState state);
    }


    public static class BlockDataGetter_21_x extends BlockDataGetter {

        public BlockDataGetter_21_x() {}
        public BlockData getBlockData(BlockState state) {
            try {
                Method method = BlockState.class.getMethod("createCraftBlockData");
                Object result = method.invoke(state);
                return (BlockData)result;
            } catch (Throwable thr) {
                return ShipPhysics.VOID;
            }
        }
    }


    public static class BlockDataGetter_26_x extends BlockDataGetter {

        public BlockDataGetter_26_x() {}
        public BlockData getBlockData(BlockState state) {
            try {
                //Method method = BlockState.class.getMethod("asBlockData");
                //Object result = method.invoke(state);
                return (BlockData)(state.asBlockData());
            } catch (Throwable thr) {
                return ShipPhysics.VOID;
            }
        }
    }



    public static abstract class TileHolder {
        public BlockPos newPosition = null;
        public BlockPos tilePosition;

        public TileHolder(@NotNull BlockPos tilePosition) {
            this.tilePosition = tilePosition;
            this.newPosition = null;
        }
        public abstract <V> V getTile();

        public BlockPos getTilePosition() {
            return tilePosition;
        }
        public BlockPos getNewPosition() {
            return newPosition;
        }
    }

    public static class VanillaTileHolder extends TileHolder {
        public BlockEntity tile;
        public VanillaTileHolder(@NotNull BlockEntity tile, @NotNull BlockPos tilePosition) {
            super(tilePosition);
            this.tile = tile;
        }

        @Override
        public BlockEntity getTile() {
            return this.tile;
        }
    }

    public static class BukkitTileHolder extends TileHolder {
        public org.bukkit.block.BlockState tile;
        public BukkitTileHolder(@NotNull org.bukkit.block.BlockState tile, @NotNull BlockPos tilePosition) {
            super(tilePosition);
            this.tile = tile;
        }

        @Override
        public org.bukkit.block.BlockState getTile() {
            return this.tile;
        }
    }
}