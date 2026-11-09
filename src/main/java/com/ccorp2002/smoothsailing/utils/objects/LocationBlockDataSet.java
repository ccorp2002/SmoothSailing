package com.ccorp2002.smoothsailing.utils.objects;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import com.ccorp2002.smoothsailing.*;
import com.ccorp2002.smoothsailing.ship.*;
import com.ccorp2002.smoothsailing.utils.*;
import com.ccorp2002.smoothsailing.nmsutils.*;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.AbstractObjectSet;
import java.lang.ref.WeakReference;
import it.unimi.dsi.fastutil.Hash;
import java.lang.reflect.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.function.Predicate;
import java.util.*;

public class LocationBlockDataSet extends AbstractMap<Long,BlockData> {
    public final Long2ObjectOpenHashMap<BlockData> internalMap;
    public final int initialMaxSize;
    public LocationBlockDataSet() {
        this(7500);
    }
    public LocationBlockDataSet(int maxSize) {
        this(maxSize,0.75f);
    }
    public LocationBlockDataSet(int maxSize, float growthFactor) {
        this.initialMaxSize = maxSize;
        this.internalMap = new Long2ObjectOpenHashMap<>(this.initialMaxSize,growthFactor);
        internalMap.defaultReturnValue(null);
    }
    @Override
    public Set<Map.Entry<Long, BlockData>> entrySet() {
        return internalMap.entrySet();
    }
    public Iterator<Long> iterator() {
        return internalMap.keySet().iterator();
    }
    
    @Override
    public Collection<BlockData> values() {
        return internalMap.values();
    }

    @Override
    public Set<Long> keySet() {
        return internalMap.keySet();
    }
    @Override
    public BlockData put(Long key, BlockData value) {
        if (key == null) return null;
        else if (value == null) return null;
        else {
            if (size()+5>=initialMaxSize) internalMap.ensureCapacity(size()+500);
            BlockData COLLIDED = internalMap.put(key,value);
            return COLLIDED;
        }
    }

    public boolean contains(Object needle) {
        if (needle == null) return false;
        else if (needle instanceof Location state) {
            return internalMap.containsKey((long)MobileBlockState.pack(state));
        } else if (needle instanceof Long logn) {
            return internalMap.containsKey(logn);
        } else if (needle instanceof Integer inte) {
            return internalMap.containsKey((long)inte);
        } else return false;
    }

    public Object set(Block block) {
        if (block == null) return null;
        return put((long)MobileBlockState.pack(block),block.getBlockData());
    }

    public Object set(MobileBlockState block) {
        if (block == null) return null;
        else return put(MobileBlockState.pack(block),block.occupiedBlock);
    }

    public Object set(Location key, BlockData e) {
        if (key == null) return null;
        else if (e == null) return null;
        else return put(MobileBlockState.pack(key),e);
    }

    public Object set(Long key, BlockData e) {
        if (e == null) return null;
        else if (key == null) return null;
        else {
            if (size()+5>=initialMaxSize) internalMap.ensureCapacity(size()+500);
            if (internalMap.containsKey(key)) return internalMap.replace(key,e,e);
            else return internalMap.put((long)key,e);
        }
    }
    @Override
    public BlockData remove(Object needle) {
        if (needle == null) return null;
        else if (needle instanceof Location state) {
            return internalMap.remove((long)MobileBlockState.pack(state));
        } else if (needle instanceof Long logn) {
            return internalMap.remove(logn);
        } else if (needle instanceof Integer inte) {
            return internalMap.remove((long)inte);
        } else return null;
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
    
    public Object[] toArray() {
        List<Long> list = new ArrayList<>(size());
        for (Long e : this.keySet()) list.add(e);
        return list.toArray();
    }
    public <T> T[] toArray(T[] a) {
        List<Long> list = new ArrayList<>(size());
        for (Long e : this.keySet()) list.add(e);
        return list.toArray(a);
    }

    @Override
    public void clear() {
        internalMap.clear();
        trim();
    }
}