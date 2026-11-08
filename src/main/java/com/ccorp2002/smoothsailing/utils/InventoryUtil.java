package com.ccorp2002.smoothsailing.utils;



import com.ccorp2002.smoothsailing.ship.ShipInstance;
import com.ccorp2002.smoothsailing.utils.objects.Pair;
import com.ccorp2002.smoothsailing.ship.MobileBlockState;
import com.ccorp2002.smoothsailing.nmsutils.NmsUtils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.TileState;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

/** Small inventory & itemstack helpers for use in a future fuel & fuel item system. */
public final class InventoryUtil {

    private InventoryUtil() {}
    public static final Map<String,FuelType> FUEL_TYPES = new HashMap<>();
    public static final String COAL_POWER_KEY = "__coal__"; // default fuel type
    public static final String FUEL_KEY = "__fuel__";
    static {
        FuelType COAL_POWER = new FuelType();
        COAL_POWER.id = COAL_POWER_KEY;
        COAL_POWER = COAL_POWER.addFuelItem(ItemStack.of(Material.COAL));
        COAL_POWER = COAL_POWER.addFuelItem(ItemStack.of(Material.CHARCOAL));
        FUEL_TYPES.put(COAL_POWER_KEY,COAL_POWER);

    }

    public static MobileBlockState.MobileStateSet getDefaultFuelBlocks(ShipInstance ship) {
        if (ship == null) return new MobileBlockState.MobileStateSet(0);
        return ship.getMaterial(Material.FURNACE);
    }

    public static FuelType getFuelType(String id) {
        if (id == null || id.isEmpty()) return null;
        for (FuelType ft : FUEL_TYPES.values()) {
            if (ft.id.equalsIgnoreCase(id)) return ft;
        }
        return null;
    }
    public static FuelType getFuelType(ItemStack data) {
        if (data == null || data.isEmpty()) return null;
        for (FuelType ft : FUEL_TYPES.values()) {
            if (ft.isItemFuel(data)) return ft;
        }
        return null;
    }

    public static Pair<FuelType,ItemStack> getFuelType(Inventory inv) {
        if (inv == null) return null;
        if (inv.isEmpty()) return null;
        for (ItemStack item : inv) {
            FuelType fuel = getFuelType(item);
            if (fuel != null) return new Pair<FuelType,ItemStack>(fuel,item);
        }
        return null;
    }

    public static boolean canBurnFuelType(ShipInstance ship, Pair<Pair<FuelType,ItemStack>,Inventory> fuelBurner) {
        if (ship == null) return false;
        if (ship.engineCount == 0) return false;
        if (ship.getGear() <= 1) return false;
        if (fuelBurner == null) fuelBurner = checkFuelTypeToBurn(ship);
        if (fuelBurner != null) {
            return true;
        } else return false;
    }

    public static boolean doBurnFuelType(ShipInstance ship, Pair<Pair<FuelType,ItemStack>,Inventory> fuelBurner) {
        return doBurnFuelType(ship,fuelBurner,false);
    }

    public static boolean doBurnFuelType(ShipInstance ship, Pair<Pair<FuelType,ItemStack>,Inventory> fuelBurner, boolean force) {
        if (ship == null) return false;
        if (ship.engineCount == 0) return false;
        if (ship.getGear() <= 1) return false;
        if (fuelBurner == null) fuelBurner = checkFuelTypeToBurn(ship);
        if (canBurnFuelType(ship,fuelBurner)) {
            Pair<FuelType,ItemStack> fuel = fuelBurner.getLeft();
            Inventory source = fuelBurner.getRight();
            ItemStack toBurn = fuel.getRight().asOne();
            FuelType type = fuel.getLeft();
            if (ShipInstance.RANDOM.nextDouble() > type.burnChance || force) {
                MobileBlockState STATE = MobileBlockState.getStateFromLoc(source.getLocation());
                boolean burnt = !reduceItemStack(source,toBurn,type.burnRate).isEmpty();
                if (STATE != null) {
                    Location pos = STATE.getLocation();
                    ShipInstance.spawnFuelBurnEffects(pos);
                    STATE.updateBlockState();
                }
                return burnt;
            } else {
                return true;
            }
        } else return false;
    }

    public static Pair<Pair<FuelType,ItemStack>,Inventory> checkFuelTypeToBurn(ShipInstance ship) {
        if (ship == null) return null;
        Pair<FuelType,ItemStack> fuel = null;
        if (ship.getGear() <= 1) return null;
        for (MobileBlockState state : getDefaultFuelBlocks(ship)) {
            if (state == null || state.isDead()) continue;
            if (state.getBlock().getState(false) instanceof Container holder) {
                final Inventory inv = holder.getInventory();
                fuel = getFuelType(inv);
                if (fuel != null) return new Pair(fuel,inv);
            }
        }
        return null;
    }
    public static ItemStack reduceItemStack(ItemStack stack, int amntChange) {
        if (stack == null || stack.isEmpty()) return ItemStack.empty();
        if (stack.getAmount() - amntChange > 0) {
            stack = stack.subtract(amntChange);
        } else {
            stack = ItemStack.empty();
        }
        return stack;
    }

    public static ItemStack reduceItemStack(final Inventory inv, ItemStack check, int amntChange) {
        if (inv == null || inv.isEmpty()) return check;
        else if (check == null || check.isEmpty()) return ItemStack.empty();
        int index = getItemIndex(inv,check);
        if (index == -1) return ItemStack.empty();
        ItemStack reduced = reduceItemStack(inv.getItem(index),amntChange);
        inv.setItem(index, reduced);
        //System.out.println("REDUCED: "+check.toString()+" BY "+amntChange + " INTO: "+reduced.toString());
        return reduced;
    }


    public static int getItemIndex(final Inventory inv, ItemStack check) {
        if (inv == null || inv.isEmpty()) return -1;
        else if (check == null || check.isEmpty()) return -1;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack == null || stack.isEmpty()) continue;
            if (isSimilar(stack,check)) {
                return i;
            }
        }
        return -1;
    }

    public static int countItemsInList(final Inventory inv, ItemStack check) {
        return countItemsInList(inv.getContents(), check);
    }
    
    public static int countItemsInList(final Collection<ItemStack> inv, ItemStack check) {
        return countItemsInList(inv.toArray(ItemStack[]::new), check);
    }
    public static int countItemsInList(final ItemStack[] inv, ItemStack check) {
        if (check == null || check.isEmpty()) return 0;
        return Arrays.stream(inv).filter(stack -> isSimilar(stack,check)).mapToInt(stack -> stack.getAmount()).sum();
    }
    
    public static ItemStack addItemModelData(String itemModelData, ItemStack itemStack) {
        ItemStack item = itemStack.clone();
        ItemMeta itemMeta = item.getItemMeta();
        CustomModelDataComponent currentCustomModelData =  itemMeta.getCustomModelDataComponent();
        List<String> customModelDataList = new ArrayList<>();
        customModelDataList.add(itemModelData);
        customModelDataList.addAll(currentCustomModelData.getStrings());
        currentCustomModelData.setStrings(customModelDataList);
        itemMeta.setCustomModelDataComponent(currentCustomModelData);
        item.setItemMeta(itemMeta);
        return item;
    }

    /**
     * Writes itemstack data into a Base64 string or producing a blank string if an error occurs or the itemstack is empty.
     */
    public static String encode(ItemStack item) {
        if (item == null || item.isEmpty()) return "";
        try {
            return java.util.Base64.getEncoder().encodeToString(item.serializeAsBytes());
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Restore itemstack data from a Base64 string produced by {@link #encode(ItemStack)}.
     * Returns an emtpy stackif {@code data} is null or any error occurs.
     */
    public static ItemStack decode(String data, Logger log) {
        if (data == null || data.isEmpty()) return ItemStack.empty();
        try {
            return ItemStack.deserializeBytes(java.util.Base64.getDecoder().decode(data));
        } catch (Exception e) {
            if (log != null) log.warning("Failed to load ItemStack, defaulting to empty stack: " + e.getMessage());
            return ItemStack.empty();
        }
    }

    /**
     * Serialize an inventory's contents to a Base64 string: a size-prefixed list of {@link ItemStack}s
     * via {@link org.bukkit.util.io.BukkitObjectOutputStream}. Holder-neutral — used to persist both
     * custom-block storage (block PDC) and cart inventories (entity PDC). Returns null on failure.
     */
    public static String encode(Inventory inv) {
        try {
            var stream = new java.io.ByteArrayOutputStream();
            var oos = new org.bukkit.util.io.BukkitObjectOutputStream(stream);
            oos.writeInt(inv.getSize());
            for (int i = 0; i < inv.getSize(); i++) {
                oos.writeObject(inv.getItem(i));
            }
            oos.close();
            return java.util.Base64.getEncoder().encodeToString(stream.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Restore inventory contents from a string produced by {@link #encode(Inventory)}. No-op if
     * {@code data} is null. Logs (via {@code log}) and swallows any failure, leaving {@code inv} as-is.
     */
    public static void decode(String data, Inventory inv, Logger log) {
        if (data == null || inv == null) return;
        try {
            byte[] bytes = java.util.Base64.getDecoder().decode(data);
            var stream = new java.io.ByteArrayInputStream(bytes);
            var ois = new org.bukkit.util.io.BukkitObjectInputStream(stream);
            int size = ois.readInt();
            for (int i = 0; i < size && i < inv.getSize(); i++) {
                inv.setItem(i, (ItemStack) ois.readObject());
            }
            ois.close();
        } catch (Exception e) {
            if (log != null) log.warning("Failed to load inventory: " + e.getMessage());
        }
    }
    public static boolean isSimilar(ItemStack i1, ItemStack i2) {
        if (i1 == null || i2 == null) return false;
        if (i1.getType() != i2.getType()) return false;
        if (i1.isEmpty() || i2.isEmpty()) return false;
        else {
            ItemMeta meta1 = i1.getItemMeta();
            ItemMeta meta2 = i2.getItemMeta();
            if (meta1 == null && meta2 == null) return true;
            else if (meta1 == null || meta2 == null) return false;
            if (meta1.hasItemModel() && meta2.hasItemModel()) {
                if (meta1.getItemModel().getKey().equalsIgnoreCase(meta2.getItemModel().getKey())) return true;
            }
            if (meta1.hasCustomModelData() && meta2.hasCustomModelData()) {
                if (meta1.getCustomModelData() == meta2.getCustomModelData()) return true;
            }
            if (meta1.hasItemName() && meta2.hasItemName()) {
                if (ChatColor.stripColor(meta1.getItemName()).equalsIgnoreCase(ChatColor.stripColor(meta2.getItemName()))) return true;
            } else if (meta1.hasDisplayName() && meta2.hasDisplayName()) {
                if (ChatColor.stripColor(meta1.getDisplayName()).equalsIgnoreCase(ChatColor.stripColor(meta2.getDisplayName()))) return true;
            }
        }
        return i1.isSimilar(i2);
    }
}
