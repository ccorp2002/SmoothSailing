package com.ccorp2002.smoothsailing.nmsutils;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.decoration.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.bukkit.craftbukkit.util.RandomSourceWrapper;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.AABB;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.craftbukkit.util.Waitable;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.InventoryView;

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

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.lang.reflect.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.lang.reflect.Field;
import java.util.function.Predicate;
import java.util.*;

import com.ccorp2002.smoothsailing.*;
import com.ccorp2002.smoothsailing.ship.*;
import com.ccorp2002.smoothsailing.utils.*;
import com.ccorp2002.smoothsailing.nmsutils.NmsUtils;
import com.ccorp2002.smoothsailing.customships.ShipWheelData;
import com.ccorp2002.smoothsailing.customships.ShipWheelManager;


public final class CollisionEntity {
    public static final org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason REASON = org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM;
    public static <T extends org.bukkit.entity.Entity> T addEntitySilent(Location loc, Class<T> clazz, Consumer<? super T> function) {
      return addEntitySilent(loc,clazz,function,true);
    }
    
    public static <T extends org.bukkit.entity.Entity> T addEntitySilent(Location loc, Class<T> clazz, boolean noTick, Consumer<? super T> function) {
      return addEntitySilent(loc,clazz,function,noTick);
    }
    
    public static <T extends org.bukkit.entity.Entity> T addEntitySilent(Location loc, Class<T> clazz, Consumer<? super T> function, boolean noTick) {
      if (loc == null) return null;
      if (loc.getWorld() == null) return null;
      try {
        CraftWorld cworld = ((CraftWorld) loc.getWorld());
        net.minecraft.world.entity.Entity entity = cworld.createEntity(loc, clazz, false);
        if (entity == null) return null;
        if (function != null) function.accept((T) entity.getBukkitEntity());
        entity.spawnReason = REASON;
        cworld.addEntityToWorld(entity, null);
        return (T) entity.getBukkitEntity();
      } catch (Error err) {
        return loc.getWorld().spawn(loc,clazz,function,REASON);
      }
    }
    
    public static void setNoTick(org.bukkit.entity.Entity ent, boolean noTick) {
      setNoTick(ent,noTick,noTick);
    }
    
    public static boolean getNoTick(org.bukkit.entity.Entity ent) {
      if (ent == null) return false;
      return false;
    }
    
    public static void setNoTick(org.bukkit.entity.Entity ent, boolean noTick, boolean ignoreNoPlayers) {}
}