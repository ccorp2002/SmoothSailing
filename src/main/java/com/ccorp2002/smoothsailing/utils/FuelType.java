package com.ccorp2002.smoothsailing.utils;

import com.ccorp2002.smoothsailing.*;
import com.ccorp2002.smoothsailing.ShipConfig;
import com.ccorp2002.smoothsailing.ShipListener;
import com.ccorp2002.smoothsailing.detection.*;
import com.ccorp2002.smoothsailing.utils.rotation.*;
import com.ccorp2002.smoothsailing.utils.movement.*;
import com.ccorp2002.smoothsailing.ship.*;
import com.ccorp2002.smoothsailing.nmsutils.*;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import java.util.*;

public class FuelType implements ConfigurationSerializable {
    public static final String CLASS_ALIAS = "fuel-type";
    static {
        ConfigurationSerialization.registerClass(FuelType.class, CLASS_ALIAS);
    }
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
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>(4);
        List<String> BASE64_FUEL = new ArrayList<>();
        for (ItemStack stack : FUEL_ITEMS) {
            BASE64_FUEL.add(InventoryUtil.encode(stack));
        }
        data.put("id", this.id);
        data.put("item", BASE64_FUEL);
        data.put("chance", this.burnChance);
        data.put("rate", this.burnRate);
        return data;
    }

    public static FuelType deserialize(ConfigurationSection args) {
        FuelType type = new FuelType();
        type.id = args.getString("id","");
        for (String base64 : args.getStringList("item")) {
            ItemStack item = InventoryUtil.decode(base64,null);
            if (!item.isEmpty()) type.addFuelItem(item);
        }
        type.burnChance = (double)args.getDouble("chance");
        type.burnRate = (int) args.getInt("rate");
        return type;
    }

    public static FuelType deserialize(Map<String, Object> args) {
        FuelType type = new FuelType();
        type.id = (String)args.getOrDefault("id","");
        for (String base64 : (List<String>)args.getOrDefault("item",new ArrayList<String>())) {
            ItemStack item = InventoryUtil.decode(base64,null);
            if (!item.isEmpty()) type.addFuelItem(item);
        }
        type.burnChance = (double)args.get("chance");
        type.burnRate = (int) args.get("rate");
        return type;
    }
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof FuelType oth)) return false;
        else return this.id.equalsIgnoreCase(oth.id);
    }
}