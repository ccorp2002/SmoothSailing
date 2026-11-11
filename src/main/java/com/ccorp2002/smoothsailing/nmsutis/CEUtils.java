package com.ccorp2002.smoothsailing.nmsutils;


import ca.spottedleaf.moonrise.common.util.WorldUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.bukkit.craftbukkit.util.RandomSourceWrapper;
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

import org.bukkit.util.BlockVector;

import com.ccorp2002.smoothsailing.*;
import com.ccorp2002.smoothsailing.ship.*;
import com.ccorp2002.smoothsailing.utils.*;
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

import java.lang.reflect.Field;




@SuppressWarnings("unused")
public class CEUtils {
    public static CEUtils INSTANCE;
    public CEUtils() {
        SmoothSailingPlugin.INSTANCE.getLogger().info("==================================================");
        SmoothSailingPlugin.INSTANCE.getLogger().info("CraftEngine was found for Compatibility!");
        SmoothSailingPlugin.INSTANCE.getLogger().info("Ships may now attempt to detect custom blocks if configured to do so.");
        SmoothSailingPlugin.INSTANCE.getLogger().info("==================================================");
        INSTANCE = this;
    }

    public net.momirealms.craftengine.core.world.chunk.CEChunk getCEChunkOrNull(net.momirealms.craftengine.core.world.CEWorld ceWorld, @NotNull BlockVector position) {
        if (!NmsUtils.HAS_CRAFT_ENGINE) return null;
        net.momirealms.craftengine.core.world.chunk.CEChunk ceChunk = ceWorld.getChunkAtIfLoaded((int)position.getX()>>4, (int)position.getZ()>>4);
        final net.momirealms.craftengine.core.world.BlockPos cebpos = net.momirealms.craftengine.core.world.BlockPos.of((int)position.getX(), (int)position.getY(), (int)position.getZ());
        if (ceChunk == null) {
            try {
                ceChunk = ceWorld.worldDataStorage().readChunkAt(ceWorld,new net.momirealms.craftengine.core.world.ChunkPos(cebpos),null);
            } catch (Throwable thr) {
                ceChunk = null;
            }

        }
        return ceChunk;
    }

    public net.momirealms.craftengine.core.world.chunk.CEChunk getCEChunkOrNull(net.momirealms.craftengine.core.world.CEWorld ceWorld, @NotNull BlockPos position) {
        if (!NmsUtils.HAS_CRAFT_ENGINE) return null;
        net.momirealms.craftengine.core.world.chunk.CEChunk ceChunk = ceWorld.getChunkAtIfLoaded(position.getX()>>4, position.getZ()>>4);
        final net.momirealms.craftengine.core.world.BlockPos cebpos = net.momirealms.craftengine.core.world.BlockPos.of(position.getX(), position.getY(), position.getZ());
        if (ceChunk == null) {
            try {
                ceChunk = ceWorld.worldDataStorage().readChunkAt(ceWorld,new net.momirealms.craftengine.core.world.ChunkPos(cebpos),null);
            } catch (Throwable thr) {
                ceChunk = null;
            }

        }
        return ceChunk;
    }


    public net.momirealms.craftengine.core.world.chunk.CEChunk getCEChunkOrNull(net.momirealms.craftengine.core.world.CEWorld ceWorld, @NotNull Location position) {
        if (!NmsUtils.HAS_CRAFT_ENGINE) return null;
        net.momirealms.craftengine.core.world.chunk.CEChunk ceChunk = ceWorld.getChunkAtIfLoaded(position.getBlockX()>>4, position.getBlockZ()>>4);
        final net.momirealms.craftengine.core.world.BlockPos cebpos = net.momirealms.craftengine.core.world.BlockPos.of(position.getBlockX(), position.getBlockY(), position.getBlockZ());
        if (ceChunk == null) {
            try {
                ceChunk = ceWorld.worldDataStorage().readChunkAt(ceWorld,new net.momirealms.craftengine.core.world.ChunkPos(cebpos),null);
            } catch (Throwable thr) {
                ceChunk = null;
            }

        }
        return ceChunk;
    }


    public net.momirealms.craftengine.core.world.chunk.CEChunk getCEChunkOrNull(net.momirealms.craftengine.core.world.CEWorld ceWorld, @NotNull net.momirealms.craftengine.core.world.BlockPos position) {
        if (!NmsUtils.HAS_CRAFT_ENGINE) return null;
        net.momirealms.craftengine.core.world.chunk.CEChunk ceChunk = ceWorld.getChunkAtIfLoaded(position.x()>>4, position.z()>>4);
        if (ceChunk == null) {
            try {
                ceChunk = ceWorld.worldDataStorage().readChunkAt(ceWorld,new net.momirealms.craftengine.core.world.ChunkPos(position),null);
            } catch (Throwable thr) {
                ceChunk = null;
            }
        }
        return ceChunk;
    }

    public void removeCEBlock(Location pos) {
        removeCEBlock(pos,false,false);
    }

    public void removeCEBlock(Location pos, boolean toAir) {
        removeCEBlock(pos,toAir,false);
    }

    public void removeCEBlock(Location pos, boolean toAir, boolean delBE) {
        if (NmsUtils.HAS_CRAFT_ENGINE) {
            final net.momirealms.craftengine.core.world.CEWorld ceWorld = net.momirealms.craftengine.bukkit.world.BukkitWorldManager.instance().getWorld(pos.getWorld());
            final net.momirealms.craftengine.core.world.BlockPos cebpos = net.momirealms.craftengine.core.world.BlockPos.of(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());
            net.momirealms.craftengine.core.world.chunk.CEChunk ceChunk = getCEChunkOrNull(ceWorld,pos);
            if (ceChunk != null) {
                net.momirealms.craftengine.core.block.entity.render.ConstantBlockEntityRenderer removed = ceChunk.constantBlockEntityRenderers.remove(cebpos);
                if (removed != null) {
                    for (net.momirealms.craftengine.core.entity.player.Player player : ceChunk.getTrackedBy()) {
                        player.removeTrackedBlockEntities(cebpos);
                        removed.hide(player);
                    }
                    removed.deactivate();
                }
                ceChunk.removeConstantBlockEntityRenderer(cebpos,true);
                ceChunk.removeDynamicBlockEntityRenderer(cebpos);
                ceChunk.setBlockState(cebpos,net.momirealms.craftengine.core.block.EmptyBlockDefinition.STATE);
                if (delBE) {
                    ceChunk.removeBlockEntityTicker(cebpos);
                    net.momirealms.craftengine.core.block.entity.BlockEntity blockEntity = ceChunk.blockEntities.remove(cebpos.asLong());
                    if (blockEntity != null) blockEntity.setValid(false);
                }
                if (toAir) NmsUtils.setBlockFast(pos,air);
                ceChunk.setUnsaved(true);

            }
        } else {
            if (toAir) NmsUtils.setBlockFast(pos,air);
            return;
        }
    }
    public static final BlockState AIR = NmsUtils.AIR;
    public static final int UPDATE = 1;
    public static final int NOTIFY = 2;
    public static final int UPDATE_NOTIFY = 3;
    public static final BlockData air = NmsUtils.air;
    public static Plugin PLUGIN = NmsUtils.PLUGIN;
    public static final RandomSource RANDOM = NmsUtils.RANDOM;
    public static final int UPDATE_OPTS = net.momirealms.craftengine.core.block.UpdateFlags.UPDATE_NONE;

    public org.bukkit.inventory.Inventory getCEBlockInv(Location location) {
        if (!NmsUtils.HAS_CRAFT_ENGINE) return null;
        if (location == null) return null;
        if (location.getWorld() == null) return null;
        net.momirealms.craftengine.core.block.entity.BlockEntity blockEnt = getCEBlockEntity(location);
        if (blockEnt == null) return null;
        org.bukkit.inventory.Inventory inv = null;
        if (blockEnt.controller instanceof SimpleStorageBlockEntityController ssbe) {
            inv = ssbe.inventory();
        }
        return inv;
    }

    public static org.bukkit.persistence.PersistentDataContainer getCEBlockPDC(Location location) {
        if (!NmsUtils.HAS_CRAFT_ENGINE) return null;
        if (location == null) return null;
        if (location.getWorld() == null) return null;
        return CraftEngineBlocks.getBlockPDCAtIfLoaded(location);
    }


  public net.momirealms.craftengine.core.block.entity.BlockEntity getCEBlockEntity(Location location) {
    if (location == null) return null;
    if (!NmsUtils.HAS_CRAFT_ENGINE) return null;
    final net.momirealms.craftengine.core.world.CEWorld ceWorld = net.momirealms.craftengine.bukkit.world.BukkitWorldManager.instance().getWorld(location.getWorld());
    final net.momirealms.craftengine.core.world.chunk.CEChunk ceChunk = ceWorld.getChunkAtIfLoaded(location.getBlockX()>>4, location.getBlockZ()>>4);
    if (ceChunk != null) {
        final net.momirealms.craftengine.core.world.BlockPos cebpos = net.momirealms.craftengine.core.world.BlockPos.of(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        return ceChunk.getBlockEntity(cebpos,false);
    }
    return null;
  }

    @Nullable
    public NmsUtils.TileHolder createTileHolder(@NotNull Location pos){
        return createTileHolder((ServerLevel)((CraftWorld) pos.getWorld()).getHandle(),NmsUtils.locationToPosition(pos));
    }

    @Nullable
    public NmsUtils.TileHolder createTileHolder(@NotNull Level world, @NotNull BlockPos position) {
        Object tile = null;
        NmsUtils.TileHolder holder = null;
        if (NmsUtils.HAS_CRAFT_ENGINE) {
            final net.momirealms.craftengine.core.world.CEWorld ceWorld = net.momirealms.craftengine.bukkit.world.BukkitWorldManager.instance().getWorld(world.getWorld());
            final net.momirealms.craftengine.core.world.BlockPos cebpos = net.momirealms.craftengine.core.world.BlockPos.of(position.getX(), position.getY(), position.getZ());
            net.momirealms.craftengine.core.world.chunk.CEChunk ceChunk = getCEChunkOrNull(ceWorld,position);
            if (ceChunk != null) {
                tile = ceChunk.getBlockEntity(cebpos,false);
            }
            if (NmsUtils.HAS_CRAFT_ENGINE && tile instanceof net.momirealms.craftengine.core.block.entity.BlockEntity ceBe) {
                holder = new CraftEngineTileHolder(ceBe,position);
        }
        }
        return holder;
    }

    @Nullable
    public NmsUtils.TileHolder removeAndCreateTileHolder(@NotNull Location pos){
        return removeAndCreateTileHolder((ServerLevel)((CraftWorld) pos.getWorld()).getHandle(),NmsUtils.locationToPosition(pos));
    }

    @Nullable
    public NmsUtils.TileHolder removeAndCreateTileHolder(@NotNull Level world, @NotNull BlockPos position){
        Object tile = null;
        NmsUtils.TileHolder holder = null;
        if (NmsUtils.HAS_CRAFT_ENGINE) {
                final net.momirealms.craftengine.core.world.CEWorld ceWorld = net.momirealms.craftengine.bukkit.world.BukkitWorldManager.instance().getWorld(world.getWorld());
                final net.momirealms.craftengine.core.world.BlockPos cebpos = net.momirealms.craftengine.core.world.BlockPos.of(position.getX(), position.getY(), position.getZ());
                net.momirealms.craftengine.core.world.chunk.CEChunk ceChunk = getCEChunkOrNull(ceWorld,position);
                if (ceChunk != null) {
                    tile = ceChunk.getBlockEntity(cebpos,false);
                }
                if (NmsUtils.HAS_CRAFT_ENGINE && tile instanceof net.momirealms.craftengine.core.block.entity.BlockEntity ceBe) {
                    holder = new CraftEngineTileHolder(ceBe,position);
            }
        }
        return holder;
    }
    @SuppressWarnings("all")
    public void moveBlockEntity(@NotNull Level nativeWorld, @NotNull BlockPos newPosition, @NotNull net.momirealms.craftengine.core.block.entity.BlockEntity tile) {
        net.momirealms.craftengine.core.world.chunk.CEChunk oldChunk = getCEChunkOrNull(tile.world(),tile.pos());
        final net.momirealms.craftengine.core.world.CEWorld ceWorld = net.momirealms.craftengine.bukkit.world.BukkitWorldManager.instance().getWorld(nativeWorld.getWorld());
        net.momirealms.craftengine.core.world.chunk.CEChunk ceChunk = getCEChunkOrNull(ceWorld,newPosition);
        final net.momirealms.craftengine.core.world.BlockPos cebpos = net.momirealms.craftengine.core.world.BlockPos.of(newPosition.getX(), newPosition.getY(), newPosition.getZ());
        net.momirealms.craftengine.core.item.Item tint_source = null;
        if (tile.controller instanceof net.momirealms.craftengine.bukkit.block.entity.TintSourceBlockEntityController tint) tint_source = tint.tintSource();
        List<net.momirealms.craftengine.core.entity.player.Player> trackedBy = ceChunk.getTrackedBy();
        ceChunk.removeConstantBlockEntityRenderer(cebpos,true);
        ceChunk.removeDynamicBlockEntityRenderer(cebpos);
        var TAG = tile.saveAsTag();
        if (oldChunk != null) {
            oldChunk.activated = true;
            List<net.momirealms.craftengine.core.entity.player.Player> oldTrackedBy = oldChunk.getTrackedBy();

            final net.momirealms.craftengine.core.world.BlockPos oldPos = tile.pos();
            oldChunk.removeConstantBlockEntityRenderer(oldPos,true);
            oldChunk.removeDynamicBlockEntityRenderer(oldPos);
            var syncTicker = oldChunk.tickingSyncBlockEntitiesByPos.remove(oldPos);
            if (syncTicker != null && syncTicker instanceof net.momirealms.craftengine.core.block.entity.tick.TickingBlockEntity ticker) {
                //oldChunk.world().pendingAsyncTickingBlockEntities.remove(ticker);
                oldChunk.removeSyncBlockEntityTicker(oldPos);
            }
            var asyncTicker = oldChunk.tickingAsyncBlockEntitiesByPos.remove(oldPos);
            if (asyncTicker != null && asyncTicker instanceof net.momirealms.craftengine.core.block.entity.tick.TickingBlockEntity ticker) {
                //oldChunk.world().pendingAsyncTickingBlockEntities.remove(ticker);
                oldChunk.removeAsyncBlockEntityTicker(oldPos);
            }
            var constRender = oldChunk.constantBlockEntityRenderers.remove(oldPos);
            var dynRender = oldChunk.dynamicBlockEntityRenderers.remove(oldPos);
            if (constRender != null) {
                for (var player : oldTrackedBy) {
                    player.removeTrackedBlockEntities(oldPos);
                    constRender.hide(player);
                }
                constRender.deactivate();
            }
            if (dynRender != null) {
                for (var player : oldTrackedBy) {
                    player.removeTrackedBlockEntities(oldPos);
                    dynRender.hide(player);
                }
                dynRender.deactivate();

            }
            tile.setPos(cebpos);
            tile.setWorld(ceWorld);
            oldChunk.blockEntities.remove(oldPos.asLong());
            oldChunk.setUnsaved(true);
            if (constRender != null && ceChunk != null) {
                ceChunk.constantBlockEntityRenderers.put(cebpos,constRender);
            }
            if (dynRender != null && ceChunk != null) {
                ceChunk.dynamicBlockEntityRenderers.put(cebpos,dynRender);
                dynRender.activate();
            }
            tile.setValid(true);
        } else {
            tile.setPos(cebpos);
            tile.setWorld(ceWorld);
        }
        tile.setValid(true);
        if (ceChunk != null) {
            var other = ceChunk.blockEntities.put(cebpos.asLong(),tile);
            ceChunk.addConstantBlockEntityRenderer(cebpos,tile.blockState(),null);
            tile.updateConstantRenderers();
            try {
                if (tile.controller != null && tile.controller.hasElement()) {
                    for (int j = 0; j < tile.renderer.elements().length; j++) {
                        net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement previousElement = tile.renderer.elements()[j];
                        if (previousElement != null) trackedBy.forEach(player -> previousElement.hide(player));
                    }
                    List<net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement> elements = new ArrayList<>(4);
                    tile.controller.gatherElements(elements::add);
                    net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement[] elemArr = elements.toArray(new net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement[0]);
                    if (tile.renderer.elements() != null && tile.renderer.elements().length == elemArr.length) {
                        Arrays.setAll(tile.renderer.elements(), idx -> elemArr[idx]);
                    }
                }
                var constRender = ceChunk.constantBlockEntityRenderers.get(cebpos);
                if (constRender != null) {
                    for (var player : trackedBy) {
                        constRender.show(player);
                    }
                    constRender.activate();
                }
            } catch (Throwable e) {
                if (SmoothSailingPlugin.DEBUG) e.printStackTrace();
            }
            if (other != null) other.setValid(true);
            if (tint_source != null && tile.controller instanceof net.momirealms.craftengine.bukkit.block.entity.TintSourceBlockEntityController tint) tint.loadCustomDataFromItem(tint_source);
            tile.setValid(true);
            net.momirealms.craftengine.core.block.entity.tick.BlockEntityTicker<net.momirealms.craftengine.core.block.entity.BlockEntityController> asyncTicker = tile.controller.createAsyncBlockEntityTicker(ceWorld, tile.blockState());
            if (asyncTicker != null) {
                ceChunk.tickingAsyncBlockEntitiesByPos.compute(tile.pos(), ((pos, previousTicker) -> {
                    net.momirealms.craftengine.core.block.entity.tick.TickingBlockEntity newTicker = new net.momirealms.craftengine.core.block.entity.tick.DefaultTickingBlockEntity<>(ceChunk, tile, asyncTicker);
                    if (previousTicker != null) {
                        previousTicker.setTicker(newTicker);
                        return previousTicker;
                    } else {
                        net.momirealms.craftengine.core.block.entity.tick.ReplaceableTickingBlockEntity replaceableTicker = new net.momirealms.craftengine.core.block.entity.tick.ReplaceableTickingBlockEntity(newTicker);
                        ceChunk.world.addAsyncBlockEntityTicker(replaceableTicker);
                        return replaceableTicker;
                    }
                }));
            }
            try {
                if (TAG != null) {
                    TAG.putInt("x",cebpos.x());
                    TAG.putInt("y",cebpos.y());
                    TAG.putInt("z",cebpos.z());
                    tile.loadCustomData(TAG);
                }
            } catch (Throwable thr) {
                if (SmoothSailingPlugin.DEBUG) thr.printStackTrace();
            }
            ceChunk.activated = true;
            tile.setValid(true);
            ceChunk.addBlockEntity(tile);
        }
        ((ServerLevel)nativeWorld).getChunkSource().blockChanged(newPosition);
    }

    public static class CraftEngineTileHolder extends NmsUtils.TileHolder {

        public net.momirealms.craftengine.core.block.entity.BlockEntity tile;
        public CraftEngineTileHolder(@NotNull net.momirealms.craftengine.core.block.entity.BlockEntity tile, @NotNull BlockPos tilePosition) {
            super(tilePosition);
            this.tile = tile;
        }

        @Override
        public net.momirealms.craftengine.core.block.entity.BlockEntity getTile() {
            return this.tile;
        }
    }
}