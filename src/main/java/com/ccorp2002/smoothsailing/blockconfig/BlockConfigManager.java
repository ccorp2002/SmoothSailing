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

import java.io.File;
import java.util.*;
import java.util.logging.Logger;
import org.joml.Vector3f;

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

    public void parseBlockEntry(final String key, ConfigurationSection config) {
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
        Double fuelCost;
        if (config.contains("fuel_cost") && config.get("fuel_cost") == null) {
            // Explicit null in YAML
            fuelCost = null;
        } else {
            fuelCost = config.getDouble("fuel_cost", 0d);
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

        List<BlockProperties.ConditionalRule> RULES = new ArrayList<>();
        ConfigurationSection blockDatas = config.getConfigurationSection("block_data");
        if (blockDatas != null) {
            Material material =  Material.AIR;
            try {
                material = Material.valueOf(key.toUpperCase()); 
            } catch (Throwable thr) {
                logger.warning("Unknown Material for a BlockData-config, the base must be an exact Bukkit Material name, not a Tag or Wildcard: " + key);
            }
            if (!material.isAir()) {
                for (String blockData : blockDatas.getKeys(false)) {
                    ConfigurationSection dataConfig = blockDatas.getConfigurationSection(blockData);
                    if (SmoothSailingPlugin.DEBUG) logger.warning("Found Possible BlockData Sub-Properties under: "+blockData);
                    if (dataConfig == null) continue;
                    boolean allowedData = dataConfig.getBoolean("allowed", allowed);

                    // Parse weight - null means block is excluded from density calculations
                    Integer weightData = weight;
                    if (dataConfig.contains("weight") && dataConfig.get("weight") == null) {
                        // Explicit null in YAML
                        weightData = null;
                    } else {
                        weightData = dataConfig.getInt("weight", 0);
                    }
                    Double baseHealthData;
                    if (dataConfig.contains("health") && dataConfig.get("health") == null) {
                        // Explicit null in YAML;
                        baseHealthData = null;
                    } else {
                        baseHealthData = dataConfig.getDouble("health",baseHealth);
                        if (baseHealthData <= 0d) baseHealthData = null;
                    }

                    Double fuelCostData;
                    if (dataConfig.contains("fuel_cost") && dataConfig.get("fuel_cost") == null) {
                        // Explicit null in YAML
                        fuelCostData = null;
                    } else {
                        fuelCostData = dataConfig.getDouble("fuel_cost", fuelCost);
                    }
                    boolean turretBaseData = dataConfig.getBoolean("turret_base", turretBase);
                    boolean engineData = dataConfig.getBoolean("engine", engine);
                    boolean seatData = dataConfig.getBoolean("seat", seat);
                    BlockData blockDataType = WildcardMatcher.getBlockDataFor(material, blockData);
                    if (blockDataType.getMaterial().isAir()) {
                        logger.warning("Unknown/Invalid BlockData; this Section's Key must be a valid/parsable BlockData-String (Within Quotes if needed): " + blockData);
                    } else {
                        BlockProperties dataProps = new BlockProperties(allowedData, weightData, seatData, baseHealthData, new ArrayList<BlockProperties.ConditionalRule>());
                        dataProps.setIsEngine(engineData);
                        dataProps.setIsTurretBase(turretBaseData);
                        dataProps.setFuelCost(fuelCostData);
                        RULES.add(new BlockProperties.ConditionalRule(blockDataType, dataProps));
                        if (SmoothSailingPlugin.DEBUG) logger.info("Added BlockData Sub-Properties of: " + blockData + " for Material: "+key);
                    }
                }
            }
            logger.info("Current BlockData Sub-Properties counted for: " + key + " x"+RULES.size());
        }
        BlockProperties props = new BlockProperties(allowed, weight, seat, baseHealth, RULES);
        props.setFuelCost(fuelCost);
        props.setIsEngine(engine);
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
            BlockProperties props = new BlockProperties(allowed, weight, false, null, new ArrayList<BlockProperties.ConditionalRule>());

            rules.add(new BlockProperties.ConditionalRule(blockData, props));
        }

        return rules;
    }

    public static BlockProperties.ConditionalRule createConditionalRule(BlockData blockData, boolean allowed, Integer weight) {
        return createConditionalRule(blockData, allowed, weight, 1d);
    }

    public static BlockProperties.ConditionalRule createConditionalRule(BlockData blockData, boolean allowed, Integer weight, Double health) {
        if (blockData == null) return null;
        // Create properties (inherit weight/allowed from parent)
        BlockProperties props = new BlockProperties(allowed, weight, false, health, new ArrayList<BlockProperties.ConditionalRule>());

        return new BlockProperties.ConditionalRule(blockData, props);
    }

    public static BlockProperties.ConditionalRule createConditionalRule(BlockData blockData, BlockProperties props) {
        if (blockData == null || props == null) return null;
        return new BlockProperties.ConditionalRule(blockData, props);
    }


    public static BlockProperties.BlockDataMatcher createMatcherForData(BlockData data) {
        return blockData -> {
            if (data == null) return false;
            return data.matches(blockData) || blockData.matches(data);
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
                if (key.contains(":") && key.contains("[") && key.contains("]")) {
                    BlockData data = WildcardMatcher.getBlockDataFor(Material.AIR,key);
                    Material material = data.getMaterial();
                    addConditionalToBlock(material,createConditionalRule(data,properties));
                } else {
                    Material material = Material.valueOf(key.toUpperCase());
                    blockPropertiesCache.putIfAbsent(material, properties);
                }
            } catch (Exception e) {
                logger.warning("Unknown material or block data: " + key);
            }
        }
    }

    private Set<Material> resolveTag(String tagName) {
        try {
            NamespacedKey key;
            if (tagName.contains(":")) key = NamespacedKey.fromString(tagName.toLowerCase(Locale.ENGLISH));
            else key = NamespacedKey.minecraft(tagName.toLowerCase(Locale.ENGLISH));
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
    public boolean isAllowed(BlockData data) {
        if (data == null) return false;
        BlockProperties props = getProperties(data.getMaterial(),data);
        return props != null && props.isAllowed();
    }

    /**
     * Check if a material is allowed for ship construction.
     */
    public boolean isAllowed(Material material) {
        if (material == null) return false;
        BlockProperties props = getProperties(material);
        return props != null && props.isAllowed();
    }


    /**
     * Check if a material is disallowed for turrets/subsections, but allowed on ships.
     */
    public boolean isTurretBase(Material material) {
        if (material == null) return false;
        BlockProperties props = blockPropertiesCache.get(material);
        return props != null && props.isTurretBase();
    }

    public boolean isTurretBase(BlockData data) {
        if (data == null) return false;
        BlockProperties props = getProperties(data.getMaterial(),data);
        return props != null && props.isTurretBase();
    }


    /**
     * Check if a material is allowed for turret/subsections.
     */
    public boolean isTurretAllowed(BlockData data) {
        if (data == null) return false;
        BlockProperties props = getProperties(data.getMaterial(),data);
        if (props.isTurretBase()) return false;
        else return props.isAllowed();
    }


    /**
     * Add and update properties for a specific material, considering block datas.
     */
    public BlockProperties addConditionalToBlock(Material material, BlockProperties.ConditionalRule conditional) {
        BlockProperties oldProps = getProperties(material);
        oldProps.conditionalRules.add(conditional);
        return updateProperties(material,oldProps);
    }


    /**
     * Update properties for a specific block (replacing existing properties).
     */
    public BlockProperties updateProperties(Material material, BlockProperties newProps) {
        BlockProperties oldProps = getProperties(material);
        blockPropertiesCache.put(material,newProps);
        return getProperties(material);
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
