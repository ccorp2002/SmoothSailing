package com.ccorp2002.smoothsailing.detection;

import org.bukkit.block.data.BlockData;
import org.bukkit.Material;
import org.bukkit.Bukkit;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Utility for matching materials against wildcard patterns.
 * Supports patterns like "*_fence", "oak_*", etc.
 */
public final class WildcardMatcher {

    /**
     * Check if a material name matches a wildcard pattern.
     * Patterns can have * at start, end, or both.
     * Examples:
     *   "*_fence" matches OAK_FENCE, SPRUCE_FENCE, etc.
     *   "oak_*" matches OAK_PLANKS, OAK_LOG, etc.
     *   "*_button" matches OAK_BUTTON, STONE_BUTTON, etc.
     */
    public static boolean matches(String materialName, String pattern) {
        if (!pattern.contains("*")) {
            return materialName.equalsIgnoreCase(pattern);
        }

        String regex = pattern
            .replace("*", ".*")
            .toLowerCase(Locale.ENGLISH);

        return materialName.toLowerCase(Locale.ENGLISH).matches(regex);
    }

    /**
     * Get all materials that match a wildcard pattern.
     */
    public static Set<Material> getMatchingMaterials(String pattern) {
        EnumSet<Material> matching = EnumSet.noneOf(Material.class);

        for (Material material : Material.values()) {
            if (matches(material.name(), pattern)) {
                matching.add(material);
            }
        }

        return matching;
    }

    /**
     * Get all materials that match a wildcard pattern.
     */
    public static BlockData getBlockDataFor(Material material, String blockData) {
        if (material == null || !material.isBlock() || material == Material.AIR) return Bukkit.createBlockData(blockData.toLowerCase(Locale.ENGLISH));
        if (blockData == null || blockData.isEmpty()) {
            return material.createBlockData();
        } else {
            try {
                material.createBlockData(blockData.toLowerCase(Locale.ENGLISH));
            } catch (Exception thr) {}
            return material.createBlockData();
        }
    }

    /**
     * Check if a pattern contains wildcards.
     */
    public static boolean isWildcard(String pattern) {
        return pattern.contains("*");
    }

    /**
     * Check if a pattern is a Minecraft tag reference (starts with #).
     */
    public static boolean isTag(String pattern) {
        return pattern.startsWith("#");
    }
}
