package com.ccorp2002.smoothsailing.blockconfig;

import com.ccorp2002.smoothsailing.SmoothSailingPlugin;
import com.ccorp2002.smoothsailing.ShipModel;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.joml.Vector3f;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

/**
 * Manages block configuration for custom ships.
 * Parses blocks.yml and provides fast lookups.
 */
public class BlockConfigManager {
    public static BlockConfigManager instance;
    public final Map<Material, BlockProperties> blockPropertiesCache = new EnumMap<>(Material.class);
    public final SmoothSailingPlugin plugin;
    public final Logger logger;

    private BlockConfigManager(SmoothSailingPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public static void initialize(SmoothSailingPlugin plugin) {
        if (instance == null) {
            instance = new BlockConfigManager(plugin);
        }
    }

    public static BlockConfigManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("BlockConfigManager not initialized! Call initialize() first.");
        }
        return instance;
    }

    /**
     * Load block configuration from blocks.yml
     */
    public void loadConfig() {
        blockPropertiesCache.clear();

        // Load blocks.yml
        File blocksFile = new File(plugin.getDataFolder(), "blocks.yml");

        // Save default blocks.yml if it doesn't exist
        if (!blocksFile.exists()) {
            plugin.saveResource("blocks.yml", false);
        }

        FileConfiguration blocksConfig = YamlConfiguration.loadConfiguration(blocksFile);

        // Parse all block entries from root level
        for (String key : blocksConfig.getKeys(false)) {
            ConfigurationSection blockConfig = blocksConfig.getConfigurationSection(key);
            if (blockConfig == null) {
                continue;
            }

            try {
                parseBlockEntry(key, blockConfig);
            } catch (Exception e) {
                logger.warning("Failed to parse block config for '" + key + "': " + e.getMessage());
                e.printStackTrace();
            }
        }

        logger.info("Loaded block configuration for " + blockPropertiesCache.size() + " materials from blocks.yml");
    }

    /**
     * Reload block configuration from blocks.yml
     */
    public void reloadConfig() {
        loadConfig();
    }

    public static BlockProperties addBlockEntry(String key, boolean allowed, Integer weight, boolean seat, BlockData... blockDatas) {
        BlockProperties prop = null;
        try {
            prop = addBlockEntry(key,Arrays.asList(blockDatas),allowed,weight,seat);
        } catch (Error err) {
            err.printStackTrace();
        }
        return prop;
    }

    public static BlockProperties addBlockEntry(String key, Collection<BlockData> blockDatas, boolean allowed, Integer weight, boolean seat) {
        BlockProperties prop = null;
        try {
            prop = getInstance().createBlockEntry(key,blockDatas,allowed,weight,seat);
        } catch (Error err) {
            err.printStackTrace();
        }
        return prop;
    }

    public BlockProperties createBlockEntry(String key, Collection<BlockData> blockDatas, boolean allowed, Integer weight, boolean seat) {
        List<BlockProperties.ConditionalRule> conditionalRules = createConditionalRules(blockDatas, allowed, weight);
        BlockProperties baseProps = new BlockProperties(allowed, weight, seat, null, conditionalRules);
        return baseProps;
    }

    public void parseBlockEntry(String key, ConfigurationSection config) {
        // Parse base properties
        boolean allowed = config.getBoolean("allowed", false);

        // Parse weight - null means block is excluded from density calculations
        Integer weight;
        if (config.contains("weight") && config.get("weight") == null) {
            // Explicit null in YAML
            weight = null;
        } else {
            weight = config.getInt("weight", 0);
        }
        Double baseHealth;
        if (config.contains("health") && config.get("health") == null) {
            // Explicit null in YAML;
            baseHealth = null;
        } else {
            baseHealth = config.getDouble("health",-1d);
            if (baseHealth <= 0d) baseHealth = null;
        }

        boolean turretBase = config.getBoolean("turret_base", false);
        boolean engine = config.getBoolean("engine", false);
        boolean seat = config.getBoolean("seat", false);
        boolean leadable = false;
        BlockProperties props = new BlockProperties(allowed, weight, seat, baseHealth, new ArrayList<BlockProperties.ConditionalRule>());
        props.isEngine = engine;
        props.setIsTurretBase(turretBase);
        applyToMaterials(key, props);
    }

    public static List<BlockProperties.ConditionalRule> createConditionalRules(Integer weight, BlockData... blockDatas) {
        return createConditionalRules(Arrays.asList(blockDatas),true,weight);
    }

    public static List<BlockProperties.ConditionalRule> createConditionalRules(Collection<BlockData> blockDatas, Integer weight) {
        return createConditionalRules(blockDatas,true,weight);
    }

    public static List<BlockProperties.ConditionalRule> createConditionalRules(Collection<BlockData> blockDatas, boolean allowed) {
        return createConditionalRules(blockDatas,allowed,null);
    }

    public static List<BlockProperties.ConditionalRule> createConditionalRules(Collection<BlockData> blockDatas, boolean allowed, Integer weight) {
        List<BlockProperties.ConditionalRule> rules = new ArrayList<>();
        if (blockDatas == null) return rules;

        for (BlockData blockData : blockDatas) {
            if (blockData == null) continue;

            // Create properties (inherit weight/allowed from parent)
            BlockProperties props = new BlockProperties(allowed, weight, false);

            rules.add(new BlockProperties.ConditionalRule(createMatcherForData(blockData), props));
        }

        return rules;
    }

    public static BlockProperties.ConditionalRule createConditionalRule(BlockData blockData, boolean allowed, Integer weight) {
        return createConditionalRule(blockData, allowed, weight, null);
    }

    public static BlockProperties.ConditionalRule createConditionalRule(BlockData blockData, boolean allowed, Integer weight, Double health) {
        if (blockData == null) return null;
        // Create properties (inherit weight/allowed from parent)
        BlockProperties props = new BlockProperties(allowed, weight, false);
        if (health == null || health <= 1d) props.baseHealth = 1d;
        else props.baseHealth = health;

        return new BlockProperties.ConditionalRule(createMatcherForData(blockData), props);
    }

    private List<BlockProperties.ConditionalRule> parseConditionalRules(List<?> rulesConfigs) {
        List<BlockProperties.ConditionalRule> rules = new ArrayList<>();
        if (rulesConfigs == null) return rules;

        for (Object ruleObj : rulesConfigs) {
            if (!(ruleObj instanceof Map<?, ?> ruleMap)) continue;

            Object conditionObj = ruleMap.get("condition");
            if (!(conditionObj instanceof Map<?, ?> conditionMap)) continue;

            // Parse condition
            BlockProperties.BlockDataMatcher matcher = createMatcherFromMap(conditionMap);

            // Parse properties for this condition
            boolean seat = ruleMap.containsKey("seat") && Boolean.TRUE.equals(ruleMap.get("seat"));

            // Create properties (inherit weight/allowed from parent)
            BlockProperties props = new BlockProperties(true, 0, seat);

            rules.add(new BlockProperties.ConditionalRule(matcher, props));
        }

        return rules;
    }

    private BlockProperties.BlockDataMatcher createMatcher(ConfigurationSection conditionSection) {
        Map<String, String> conditions = new HashMap<>();
        for (String key : conditionSection.getKeys(false)) {
            conditions.put(key, conditionSection.getString(key));
        }

        return blockData -> {
            for (Map.Entry<String, String> condition : conditions.entrySet()) {
                String property = condition.getKey();
                String expectedValue = condition.getValue().toUpperCase();

                // Check different block data types
                if (blockData instanceof Slab slab) {
                    if (property.equals("type") && !slab.getType().name().equals(expectedValue)) {
                        return false;
                    }
                } else if (blockData instanceof Stairs stairs) {
                    if (property.equals("half") && !stairs.getHalf().name().equals(expectedValue)) {
                        return false;
                    }
                    if (property.equals("shape") && !stairs.getShape().name().equals(expectedValue)) {
                        return false;
                    }
                    if (property.equals("facing") && !stairs.getFacing().name().equals(expectedValue)) {
                        return false;
                    }
                } else if (blockData instanceof Orientable orientable) {
                    if (property.equals("axis") && !orientable.getAxis().name().equals(expectedValue)) {
                        return false;
                    }
                }
            }
            return true;
        };
    }

    public static BlockProperties.BlockDataMatcher createMatcherForData(BlockData data) {
        return blockData -> {
            return data.matches(blockData) || blockData.matches(data);
        };
    }

    public static BlockProperties.BlockDataMatcher createMatcherFromMap(Map<?, ?> conditionMap) {
        Map<String, String> conditions = new HashMap<>();
        for (Map.Entry<?, ?> entry : conditionMap.entrySet()) {
            conditions.put(entry.getKey().toString(), entry.getValue().toString());
        }

        return blockData -> {
            for (Map.Entry<String, String> condition : conditions.entrySet()) {
                String property = condition.getKey();
                String expectedValue = condition.getValue().toUpperCase();

                // Check different block data types
                if (blockData instanceof Slab slab) {
                    if (property.equals("type") && !slab.getType().name().equals(expectedValue)) {
                        return false;
                    }
                } else if (blockData instanceof Stairs stairs) {
                    if (property.equals("half") && !stairs.getHalf().name().equals(expectedValue)) {
                        return false;
                    }
                    if (property.equals("shape") && !stairs.getShape().name().equals(expectedValue)) {
                        return false;
                    }
                    if (property.equals("facing") && !stairs.getFacing().name().equals(expectedValue)) {
                        return false;
                    }
                } else if (blockData instanceof Orientable orientable) {
                    if (property.equals("axis") && !orientable.getAxis().name().equals(expectedValue)) {
                        return false;
                    }
                }
            }
            return true;
        };
    }

    public void applyToMaterials(String key, BlockProperties properties) {
        if (WildcardMatcher.isTag(key)) {
            // Minecraft tag reference
            Set<Material> materials = resolveTag(key.substring(1));
            for (Material material : materials) {
                blockPropertiesCache.putIfAbsent(material, properties);
            }
        } else if (WildcardMatcher.isWildcard(key)) {
            // Wildcard pattern
            Set<Material> materials = WildcardMatcher.getMatchingMaterials(key);
            for (Material material : materials) {
                blockPropertiesCache.putIfAbsent(material, properties);
            }
        } else {
            // Specific material
            try {
                Material material = Material.valueOf(key.toUpperCase());
                blockPropertiesCache.putIfAbsent(material, properties);
            } catch (IllegalArgumentException e) {
                logger.warning("Unknown material: " + key);
            }
        }
    }

    private Set<Material> resolveTag(String tagName) {
        try {
            NamespacedKey key = NamespacedKey.minecraft(tagName.toLowerCase());
            Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, key, Material.class);

            if (tag != null) {
                return tag.getValues();
            } else {
                logger.warning("Unknown tag: #" + tagName);
                return EnumSet.noneOf(Material.class);
            }
        } catch (Exception e) {
            logger.warning("Failed to resolve tag #" + tagName + ": " + e.getMessage());
            return EnumSet.noneOf(Material.class);
        }
    }

    /**
     * Check if a material is allowed for ship construction.
     */
    public boolean isAllowed(Material material) {
        if (material == null) return false;
        BlockProperties props = blockPropertiesCache.get(material);
        return props != null && props.isAllowed();
    }

    /**
     * Check if a material is allowed for ship construction.
     */
    public boolean isTurretBase(Material material) {
        if (material == null) return false;
        BlockProperties props = blockPropertiesCache.get(material);
        return props != null && props.isTurretBase();
    }

    public boolean isTurretAllowed(org.bukkit.block.data.BlockData data) {
        if (data == null) return false;
        final BlockProperties props = getProperties(data.getMaterial(), data);
        if (props.isTurretBase()) return false;
        else return props.isAllowed();
    }


    /**
     * Update properties for a specific block (considering block state).
     */
    public BlockProperties updateProperties(Material material, BlockProperties newProps) {
        BlockProperties oldProps = getProperties(material);
        blockPropertiesCache.put(material,newProps);
        if (oldProps.isAllowed()) return oldProps;
        else return getProperties(material);
    }

    /**
     * Get properties for a specific block (considering block state).
     */
    public BlockProperties getProperties(Material material, BlockData blockData) {
        BlockProperties baseProps = blockPropertiesCache.get(material);
        if (baseProps == null) {
            // Not in config = forbidden
            return new BlockProperties(false, 0, false);
        }

        // Apply conditional rules if any
        return baseProps.getPropertiesForBlockData(blockData);
    }

    /**
     * Get base properties for a material (without considering block state).
     */
    public BlockProperties getProperties(Material material) {
        BlockProperties props = blockPropertiesCache.get(material);
        if (props == null) {
            return new BlockProperties(false, 0, false);
        }
        return props;
    }
}
