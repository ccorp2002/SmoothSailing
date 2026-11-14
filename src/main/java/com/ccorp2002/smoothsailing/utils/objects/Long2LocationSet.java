package com.ccorp2002.smoothsailing.utils.objects;

import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.block.Block;
import com.ccorp2002.smoothsailing.*;
import com.ccorp2002.smoothsailing.ship.*;
import com.ccorp2002.smoothsailing.utils.*;
import com.ccorp2002.smoothsailing.nmsutils.*;

import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.*;
import java.lang.ref.WeakReference;
import it.unimi.dsi.fastutil.Hash;
import java.lang.reflect.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.function.Predicate;
import java.util.*;

public class Long2LocationSet extends AbstractLongSet {
    public final LongOpenHashSet internalMap;
    public final int initialMaxSize;
    public Long2LocationSet() {
        this(7500);
    }
    public Long2LocationSet(int maxSize) {
        this(maxSize,0.75f);
    }
    public Long2LocationSet(int maxSize, float growthFactor) {
        this.initialMaxSize = maxSize;
        this.internalMap = new LongOpenHashSet(this.initialMaxSize,growthFactor);
    }
    
    public LongIterator iterator() {
        return internalMap.iterator();
    }
    
    @Override
    public boolean add(long value) {
        if (size()+5>=initialMaxSize) internalMap.ensureCapacity(size()+500);
        return internalMap.add(value);
    }

    public boolean contains(Object needle) {
        if (needle == null) return false;
        else if (needle instanceof Location state) {
            return internalMap.contains((long)MobileBlockState.pack(state));
        } else if (needle instanceof Long logn) {
            return internalMap.contains((long)logn);
        } else if (needle instanceof Integer inte) {
            return internalMap.contains((long)inte);
        } else return false;
    }
    @Override
    public boolean remove(long k) {
        return this.internalMap.remove(k);
    }
    @Override
    public boolean rem(long k) {
        return remove(k);
    }

    public boolean remove(Object needle) {
        if (needle == null) return false;
        else if (needle instanceof Location state) {
            return internalMap.remove((long)MobileBlockState.pack(state));
        } else if (needle instanceof Long logn) {
            return internalMap.remove((long)logn);
        } else if (needle instanceof Integer inte) {
            return internalMap.remove((long)inte);
        } else return false;
    }
    @Override
    public boolean isEmpty() {
        return this.internalMap.isEmpty();
    }
    @Override
    public int size() {
        return this.internalMap.size();
    }
    public boolean trim(int amount) {
        return this.internalMap.trim(amount);
    }
    public boolean trim() {
        return trim(this.initialMaxSize);
    }
    
    public Set<Location> toSet(final World world) {
        if (world == null) return new HashSet<Location>(0,0.75f);
        ObjectOpenCustomHashSet<Location> returnMap = new ObjectOpenCustomHashSet<>(this.size()+50,0.75f,MobileBlockState.HASH_STRAT);
        for (long key : this.internalMap) {
            returnMap.add(MobileBlockState.unpackLocation(key, world));
        }
        return returnMap;
    }

    @Override
    public void clear() {
        internalMap.clear();
        trim();
    }
}