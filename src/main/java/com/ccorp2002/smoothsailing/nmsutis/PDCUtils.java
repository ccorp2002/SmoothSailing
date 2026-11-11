package com.ccorp2002.smoothsailing.nmsutils;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.bukkit.craftbukkit.util.RandomSourceWrapper;

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
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.InventoryView;

import io.papermc.paper.FeatureHooks;
import io.papermc.paper.util.MCUtil;
import org.bukkit.Bukkit;

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

import com.ccorp2002.smoothsailing.*;
import com.ccorp2002.smoothsailing.ship.*;
import com.ccorp2002.smoothsailing.utils.*;
import com.ccorp2002.smoothsailing.detection.*;

import org.bukkit.util.BlockVector;

import org.bukkit.inventory.*;
import com.jeff_media.morepersistentdatatypes.datatypes.*;
import com.jeff_media.morepersistentdatatypes.*;
import org.bukkit.persistence.*;
import org.bukkit.entity.*;
import org.bukkit.block.*;
import org.bukkit.*;

public final class PDCUtils {
	public static SmoothSailingPlugin INSTANCE = SmoothSailingPlugin.INSTANCE;

    public static boolean containsIgnoreCase(final String a, final String b) {
        return org.apache.commons.lang3.StringUtils.containsIgnoreCase(a,b);
    }
    
    public static boolean stringsContain(String id, Collection<String> searchingFor) {
        if (id == null) return false;
        if (id.isEmpty()) return false;
        if (id.contains("&") || id.contains("§")) id = ChatColor.stripColor(id);
        for (String search : searchingFor) {
            if (containsIgnoreCase(search,id) || containsIgnoreCase(id,search)) return true;
        }
        return false;
    }

    public static boolean isInStrings(String csvList, String string) {
        if (string == null || csvList == null) return false;
        if (string.isEmpty()) return false;
        if (csvList.contains("&") || csvList.contains("§")) csvList = ChatColor.stripColor(csvList);
        if (csvList.contains(",")) {
            return isInStrings(Arrays.asList(csvList.split(",")), string);
        } else return false;
    }

    public static boolean isInStrings(final Collection<String> list, String string) {
        if (string == null) return false;
        if (string.isEmpty()) return false;
        if (list == null) return false;
        for (String str : list) {
            if (str.equalsIgnoreCase(string)) {
                return true;
            }
        } 
        if (stringsContain(string, list)) return true;
        return false;
    }
	public static boolean hasPDC(Block block, String key) {
		if (INSTANCE == null) return false;
		if (block == null) return false;
		if (key == null) return false;
		key = key.toLowerCase();
		NamespacedKey nmskey;
		if (key.contains(":")) nmskey = NamespacedKey.fromString(key);
		else nmskey = NamespacedKey.fromString(key,INSTANCE);
		if (!(block.getState(false) instanceof TileState state)) return false;
		else {
			PersistentDataContainer pdcC = state.getPersistentDataContainer();
			return pdcC.has(nmskey);
		}
	}
	public static String getPDC(Block block, String key) {
		if (INSTANCE == null) return "";
		if (block == null) return "";
		if (key == null) return "";
		key = key.toLowerCase();
		NamespacedKey nmskey;
		if (key.contains(":")) nmskey = NamespacedKey.fromString(key);
		else nmskey = NamespacedKey.fromString(key,INSTANCE);
		if (!(block.getState(false) instanceof TileState state)) return "";
		else {
			PersistentDataContainer pdcC = state.getPersistentDataContainer();
			return pdcC.getOrDefault(nmskey, PersistentDataType.STRING, "");
		}
	}
	public static void setPDC(Block block, String key, String value) {
		if (INSTANCE == null) return;
		if (block == null) return;
		if (key == null) return;
		if (value == null) return;
		key = key.toLowerCase();
		value = value.toLowerCase();
		NamespacedKey nmskey;
		if (key.contains(":")) nmskey = NamespacedKey.fromString(key);
		else nmskey = NamespacedKey.fromString(key,INSTANCE);
		if (!(block.getState(false) instanceof TileState state)) return;
		else {
			PersistentDataContainer pdcC = state.getPersistentDataContainer();
			pdcC.set(nmskey, PersistentDataType.STRING, value);
			state.update(false,false);
		}
	}

	public static boolean hasPDC(Entity block, String key) {
		if (INSTANCE == null) return false;
		if (block == null) return false;
		if (key == null) return false;
		key = key.toLowerCase();
		NamespacedKey nmskey;
		if (key.contains(":")) nmskey = NamespacedKey.fromString(key);
		else nmskey = NamespacedKey.fromString(key,INSTANCE);
		if (block.isDead()) return false;
		else {
			PersistentDataContainer pdcC = block.getPersistentDataContainer();
			return pdcC.has(nmskey);
		}
	}
	public static String getPDC(Entity block, String key) {
		if (INSTANCE == null) return "";
		if (block == null) return "";
		if (key == null) return "";
		key = key.toLowerCase();
		NamespacedKey nmskey;
		if (key.contains(":")) nmskey = NamespacedKey.fromString(key);
		else nmskey = NamespacedKey.fromString(key,INSTANCE);
		if (block.isDead()) return "";
		else {
			PersistentDataContainer pdcC = block.getPersistentDataContainer();
			return pdcC.getOrDefault(nmskey, PersistentDataType.STRING,"");
		}
	}
	public static void setPDC(Entity block, String key, String value) {
		if (INSTANCE == null) return;
		if (block == null) return;
		if (key == null) return;
		if (value == null) return;
		key = key.toLowerCase();
		value = value.toLowerCase();
		NamespacedKey nmskey;
		if (key.contains(":")) nmskey = NamespacedKey.fromString(key);
		else nmskey = NamespacedKey.fromString(key,INSTANCE);
		if (block.isDead()) return;
		else {
			PersistentDataContainer pdcC = block.getPersistentDataContainer();
			pdcC.set(nmskey, PersistentDataType.STRING, value);
		}
	}

	public static boolean hasPDC(PersistentDataContainer pdcC, String key) {
		if (INSTANCE == null) return false;
		if (pdcC == null) return false;
		if (key == null) return false;
		key = key.toLowerCase();
		NamespacedKey nmskey;
		if (key.contains(":")) nmskey = NamespacedKey.fromString(key);
		else nmskey = NamespacedKey.fromString(key,INSTANCE);
		return pdcC.has(nmskey);
	}
	public static String getPDC(PersistentDataContainer pdcC, String key) {
		if (INSTANCE == null) return "";
		if (pdcC == null) return "";
		if (key == null) return "";
		key = key.toLowerCase();
		NamespacedKey nmskey;
		if (key.contains(":")) nmskey = NamespacedKey.fromString(key);
		else nmskey = NamespacedKey.fromString(key,INSTANCE);
		return pdcC.getOrDefault(nmskey, PersistentDataType.STRING,"");
	}
	public static void setPDC(PersistentDataContainer pdcC, String key, String value) {
		if (INSTANCE == null) return;
		if (pdcC == null) return;
		if (key == null) return;
		if (value == null) return;
		key = key.toLowerCase();
		value = value.toLowerCase();
		NamespacedKey nmskey;
		if (key.contains(":")) nmskey = NamespacedKey.fromString(key);
		else nmskey = NamespacedKey.fromString(key,INSTANCE);
		pdcC.set(nmskey, PersistentDataType.STRING, value);
	}
	public static Map<String,String> getPDCMap(Entity block, String key) {
		if (INSTANCE == null) return new HashMap<String,String>();
		if (block == null) return new HashMap<String,String>();
		if (key == null) return new HashMap<String,String>();
		key = key.toLowerCase();
		NamespacedKey nmskey;
		if (key.contains(":")) nmskey = NamespacedKey.fromString(key);
		else nmskey = NamespacedKey.fromString(key,INSTANCE);
		if (block.isDead()) return new HashMap<String,String>();
		else {
			PersistentDataContainer pdcC = block.getPersistentDataContainer();
			return pdcC.getOrDefault(nmskey, DataType.asMap(PersistentDataType.STRING, PersistentDataType.STRING),new HashMap<String,String>());
		}
	}
	public static void setPDCMap(Entity block, String key, Map<String,String> value) {
		if (INSTANCE == null) return;
		if (block == null) return;
		if (key == null) return;
		if (value == null) return;
		key = key.toLowerCase();
		NamespacedKey nmskey;
		if (key.contains(":")) nmskey = NamespacedKey.fromString(key);
		else nmskey = NamespacedKey.fromString(key,INSTANCE);
		if (block.isDead()) return;
		else {
			PersistentDataContainer pdcC = block.getPersistentDataContainer();
			pdcC.set(nmskey, DataType.asMap(PersistentDataType.STRING, PersistentDataType.STRING), value);
		}
	}

	public static ItemStack[] getPDCItems(Entity block, String key) {
		if (INSTANCE == null) return null;
		if (block == null) return null;
		if (key == null) return null;
		key = key.toLowerCase();
		NamespacedKey nmskey;
		if (key.contains(":")) nmskey = NamespacedKey.fromString(key);
		else nmskey = NamespacedKey.fromString(key,INSTANCE);
		if (block.isDead()) return null;
		else {
			PersistentDataContainer pdcC = block.getPersistentDataContainer();
			return pdcC.getOrDefault(nmskey, DataType.ITEM_STACK_ARRAY, new ItemStack[]{});
		}
	}

	public static void setPDCItems(Entity block, String key, ItemStack[] value) {
		if (INSTANCE == null) return;
		if (block == null) return;
		if (key == null) return;
		if (value == null) return;
		key = key.toLowerCase();
		NamespacedKey nmskey;
		if (key.contains(":")) nmskey = NamespacedKey.fromString(key);
		else nmskey = NamespacedKey.fromString(key,INSTANCE);
		if (block.isDead()) return;
		else {
			PersistentDataContainer pdcC = block.getPersistentDataContainer();
			pdcC.set(nmskey, DataType.ITEM_STACK_ARRAY, value);
		}
	}

	public static void copyTo(Block block, Entity entity) {
		if (INSTANCE == null) return;
		if (block == null) return;
		if (entity == null) return;
		if (entity.isDead()) return;
		if (!(block.getState(false) instanceof TileState state)) return;
		else {
			PersistentDataContainer pdcC = state.getPersistentDataContainer();
			pdcC.copyTo(entity.getPersistentDataContainer(),true);
		}
	}

	public static void copyTo(PersistentDataContainer pdcC, Block block) {
		if (INSTANCE == null) return;
		if (block == null) return;
		if (pdcC == null) return;
		if (!(block.getState(false) instanceof TileState state)) {
			if (!NmsUtils.HAS_CRAFT_ENGINE) return; //Shortcut fail if CraftEngine isn't installed
			PersistentDataContainer other = CEUtils.getCEBlockPDC(block.getLocation());
			if (other == null) return;
			pdcC.copyTo(other,true);
		} else {
			pdcC.copyTo(state.getPersistentDataContainer(),true);
		}
	}

	public static void copyTo(Entity entity, Block block) {
		copyTo(entity.getPersistentDataContainer(),block);
	}
}