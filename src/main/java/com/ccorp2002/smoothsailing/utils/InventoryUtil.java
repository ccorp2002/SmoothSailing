package com.ccorp2002.smoothsailing.utils;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

/** Small inventory & itemstack helpers for use in a future fuel system. */
public final class InventoryUtil {

    private InventoryUtil() {}

    public static ItemStack reduceItemStack(ItemStack stack, int amntChange) {
        if (stack.getAmount() > 1) {
            int amount = stack.getAmount();
            amntChange *= -1;
            if (amount + amntChange > 0) {
                stack.setAmount(amount + amntChange);
            } else {
                stack = ItemStack.empty();
            }
        } else {
            stack = ItemStack.empty();
        }
        return stack;
    }

    public static int countItemsInList(final Inventory inv, ItemStack check) {
        return countItemsInList(inv.getContents(), check);
    }
    
    public static int countItemsInList(final Collection<ItemStack> inv, ItemStack check) {
        return countItemsInList(inv.toArray(ItemStack[]::new), check);
    }
    public static int countItemsInList(final ItemStack[] inv, ItemStack check) {
        if (check == null || check.isEmpty()) return 0;
        return Arrays.stream(inv).filter(stack -> stack.isSimilar(check)).mapToInt(stack -> stack.getAmount()).sum();
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
}
