package com.ccorp2002.smoothsailing.utils;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

import java.util.ArrayList;
import java.util.List;

public class CustomModelData {
    public static ItemStack addCustomModelData(String customModelData, ItemStack itemStack) {
        ItemStack item = itemStack.clone();
        ItemMeta itemMeta = item.getItemMeta();
        CustomModelDataComponent currentCustomModelData =  itemMeta.getCustomModelDataComponent();
        List<String> customModelDataList = new ArrayList<>();
        customModelDataList.add(customModelData);
        customModelDataList.addAll(currentCustomModelData.getStrings());
        currentCustomModelData.setStrings(customModelDataList);
        itemMeta.setCustomModelDataComponent(currentCustomModelData);
        item.setItemMeta(itemMeta);
        return item;
    }
}
