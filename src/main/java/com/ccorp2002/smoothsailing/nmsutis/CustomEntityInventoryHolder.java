package com.ccorp2002.smoothsailing.nmsutils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Chest;
import org.bukkit.block.Furnace;
import org.bukkit.block.Container;
import org.bukkit.block.TileState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.plugin.Plugin;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.Comparator;
import org.bukkit.block.data.type.Piston;
import org.bukkit.block.data.type.RedstoneWire;
import org.bukkit.block.data.type.Repeater;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Dropper;
import org.bukkit.inventory.meta.*;
import org.bukkit.inventory.*;

import com.ccorp2002.smoothsailing.*;
import com.ccorp2002.smoothsailing.ship.*;
import com.ccorp2002.smoothsailing.utils.*;
import com.ccorp2002.smoothsailing.detection.*;



import com.jeff_media.morepersistentdatatypes.datatypes.*;
import com.jeff_media.morepersistentdatatypes.*;
import org.bukkit.persistence.*;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;


import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.craftbukkit.CraftWorld;

import ca.spottedleaf.moonrise.common.util.TickThread;
import org.bukkit.craftbukkit.util.Waitable;
import io.papermc.paper.util.MCUtil;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Future;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

import java.lang.reflect.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.lang.reflect.Field;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.*;

public class CustomEntityInventoryHolder implements InventoryHolder {
    public static SmoothSailingPlugin INSTANCE = SmoothSailingPlugin.INSTANCE;
    public static final NamespacedKey STORAGE_KEY = NamespacedKey.fromString("stored_items",INSTANCE);

    public Inventory inventory;

    public Entity entity;

    public static final BiMap<Entity,CustomEntityInventoryHolder> CUSTOM_INVENTORY_CACHE = HashBiMap.create(8_000);

    public static CustomEntityInventoryHolder build(Entity entity, Integer size, String name, Inventory newInv) {
        CustomEntityInventoryHolder invHolder = build(entity,size,name);
        invHolder = invHolder.replaceInventory(newInv);
        CUSTOM_INVENTORY_CACHE.forcePut(entity,invHolder);
        return invHolder;
    }

    public static CustomEntityInventoryHolder build(Entity entity, Integer size, String name) {
        return new CustomEntityInventoryHolder(entity,size,name);
    }

    public CustomEntityInventoryHolder(Entity entity, Integer size, String name) {
        // Create an Inventory with 9 slots, `this` here is our InventoryHolder.
        if (size == null || size < 9) size = 9;
        if (name == null) name = "Inventory";
        this.entity = entity;
        this.inventory = Bukkit.getServer().createInventory(this, size, name);
        if (this.entity != null) {
            ItemStack[] items = PDCUtils.getPDCItems(this.entity,"stored_items");
            if (items != null && !CUSTOM_INVENTORY_CACHE.containsKey(this.entity)) {
                try {
                    this.inventory.setContents(items);
                } catch (Throwable thr) {}
            }
            CUSTOM_INVENTORY_CACHE.forcePut(this.entity,this);
        }
    }

    @Override
    public Inventory getInventory() {
        if (this.entity != null) {
            ItemStack[] items = PDCUtils.getPDCItems(this.entity,"stored_items");
            if (items != null && !CUSTOM_INVENTORY_CACHE.containsKey(this.entity)) {
                try {
                    this.inventory.setContents(items);
                } catch (Throwable thr) {}
            }
            CUSTOM_INVENTORY_CACHE.forcePut(this.entity,this);
        }
        return this.inventory;
    }

    public void saveInventory() {
        if (this.entity != null && !isMissing()) {
            PersistentDataContainer pdc = this.entity.getPersistentDataContainer();
            PDCUtils.setPDCItems(this.entity,"stored_items",inventory.getContents());
            CUSTOM_INVENTORY_CACHE.forcePut(this.entity,this);
        }
    }

    public Inventory setInventory(Inventory newInv) {
        return this.setInventory(newInv,true);
    }

    public Inventory setInventory(Inventory newInv, boolean update) {
        if (newInv == null) return inventory;
        this.inventory.setContents(newInv.getContents());
        if (!this.hasEntity()) return inventory;
        if (this.entity != null) {
            saveInventory();
        }
        if (update) {
            for (HumanEntity humEnt : this.inventory.getViewers()) {
                if (humEnt instanceof Player player) {
                    player.updateInventory();
                }
            }
        }
        return this.inventory;
    }

    public CustomEntityInventoryHolder replaceInventory(Inventory newInv) {
        if (newInv == null) return this;
        if (!this.hasEntity()) return this;
        if (this.entity != null) {
            PDCUtils.setPDCItems(this.entity,"stored_items",newInv.getContents());
        }
        for (HumanEntity humEnt : this.inventory.getViewers()) {
            if (humEnt instanceof Player player) {
                player.closeInventory();
            }
        }
        this.inventory = newInv;
        if (this.entity != null) CUSTOM_INVENTORY_CACHE.forcePut(this.entity,this);
        return this;
    }
    public boolean isMissing() {
        if (this.entity == null) return true;
        if (!this.entity.getLocation().isChunkLoaded()) return false;
        if (this.entity.isDead()) return true;
        if (!this.entity.isValid()) return true;
        return false;
    }
    public boolean remove(boolean removeBlock) {
        if (this.entity == null) return false;
        else {
            for (HumanEntity humEnt : this.inventory.getViewers()) {
                if (humEnt instanceof Player player) {
                    player.closeInventory();
                }
            }
            CUSTOM_INVENTORY_CACHE.remove(this);
            this.entity.remove();
            this.entity = null;
            this.inventory = null;
        }
        return true;
    }
    public static boolean remove(Entity entity) {
        CustomEntityInventoryHolder holder = CUSTOM_INVENTORY_CACHE.getOrDefault(entity,null);
        if (holder == null) return false;
        return holder.remove(true);
    }
    public static CustomEntityInventoryHolder get(Entity entity) {
        CustomEntityInventoryHolder holder = CUSTOM_INVENTORY_CACHE.getOrDefault(entity,null);
        return holder;
    }
    public boolean remove() {
        return remove(false);
    }

    public boolean hasEntity() {
        return (this.entity != null && !this.entity.isDead());
    }

    public Entity getEntity() {
        if (!hasEntity()) return null;
        return this.entity;
    }
}