package com.ccorp2002.smoothsailing.utils;

import com.ccorp2002.smoothsailing.*;
import com.ccorp2002.smoothsailing.ShipConfig;
import com.ccorp2002.smoothsailing.ShipListener;
import com.ccorp2002.smoothsailing.customships.*;
import com.ccorp2002.smoothsailing.utils.rotation.*;
import com.ccorp2002.smoothsailing.utils.movement.*;
import com.ccorp2002.smoothsailing.ship.*;
import com.ccorp2002.smoothsailing.nmsutils.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import java.util.*;

public class FuelType {
    public final List<ItemStack> FUEL_ITEMS = new ArrayList<>();
    public double burnChance = 0.75;
    public int burnRate = 1;
    public String id = "";

    public FuelType clearFuelTypes() {
        this.FUEL_ITEMS.clear();
        return this;
    }

    public FuelType addFuelItem(ItemStack item) {
        if (this.isItemFuel(item)) return this;
        this.FUEL_ITEMS.add(item);
        return this;
    }

    public List<ItemStack> getFuelItems() {
        return this.FUEL_ITEMS;
    }
    public boolean isItemFuel(ItemStack item) {
        if (FUEL_ITEMS.isEmpty()) return false;
        if (InventoryUtil.isSimilar(item,FUEL_ITEMS.getFirst())) return true; //quick check for common fuel type (index 0)
        for (ItemStack fuel : this.getFuelItems()) {
            if (InventoryUtil.isSimilar(item,fuel)) return true;
        }
        return false;
    }
    public static boolean isItemFuel(FuelType ft, ItemStack item) {
        return ft.isItemFuel(item);
    }
    public void unregister() {
        if (this.id == null || this.id.isEmpty()) return;
        InventoryUtil.FUEL_TYPES.remove(this.id);
    }
    public void registerOrUpdate() {
        if (this.id == null || this.id.isEmpty()) return;
        InventoryUtil.FUEL_TYPES.put(this.id,this);
    }
}