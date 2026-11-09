package com.ccorp2002.smoothsailing.ship;

import com.ccorp2002.smoothsailing.*;
import com.ccorp2002.smoothsailing.utils.*;
import com.ccorp2002.smoothsailing.utils.objects.*;
import com.ccorp2002.smoothsailing.utils.vectors.*;
import com.ccorp2002.smoothsailing.event.*;
import com.ccorp2002.smoothsailing.nmsutils.*;
import com.ccorp2002.smoothsailing.customships.*;
import com.ccorp2002.smoothsailing.blockconfig.*;
import com.ccorp2002.smoothsailing.utils.displays.*;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.joml.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.*;

public class ShipInstance {
    // Global physics config (loaded once from physics section)
    public static final java.util.Random RANDOM = new java.util.Random();
    public static double MOVEMENT_THRESHOLD = 0.01;
    public static double ROTATION_THRESHOLD = 0.01;
    public static int IDLE_TICKS_BEFORE_STOP = 40;
    public static int IDLE_CHECK_INTERVAL = 20;
    public static float PLAYER_PROXIMITY_RADIUS = 8.0f;
    public static float PLAYER_PROXIMITY_RADIUS_SQ = 64.0f;  // Squared for fast distance checks
    public boolean isSinking = false;
    public static final String HITBOX_KEY = "BLOCK_STATES";
    public MobileBlockState.MobileStateSet BLOCK_STATES = null;
    public LocationBlockDataSet FLUIDS = null;

    public final Map<String, Object> DATA_TAGS = new Object2ObjectOpenHashMap<>(1024,0.75f);
    
    //public final Map<Object, MobileBlockState.MobileStateSet> TRACKED_STATE_MAP = new Object2ObjectOpenHashMap<>(1024,0.75f);
    public static float waterDensity() {
        return (float) ShipPhysics.getMedium("water").baseDensity;
    }
    public static float waterDensity = waterDensity();
    public static float airDensity() {
        return (float) ShipPhysics.getMedium("void").baseDensity;
    }
    public static float airDensity = airDensity();

    public static final org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason SPAWN_REASON = org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM;
    public static <T extends org.bukkit.entity.Entity> T addEntity(Location loc, Class<T> clazz, Consumer<? super T> function) {
      if (loc == null) return null;
      if (loc.getWorld() == null) return null;
      return loc.getWorld().spawn(loc,clazz,function,SPAWN_REASON);
    }

    /**
     * Loads global physics config values from plugin config.
     * Should be called once during plugin initialization.
     */
    public static void loadGlobalPhysicsConfig(JavaPlugin plugin) {
        var cfg = plugin.getConfig();
        ShipInstance.plugin = plugin;
        config = ShipConfig.load(ShipInstance.plugin);
        MOVEMENT_THRESHOLD = cfg.getDouble("physics.movement-threshold", 0.01);
        ROTATION_THRESHOLD = cfg.getDouble("physics.rotation-threshold", 0.01);
        IDLE_TICKS_BEFORE_STOP = cfg.getInt("physics.idle-ticks-before-stop", 40);
        IDLE_CHECK_INTERVAL = cfg.getInt("physics.idle-check-interval", 20);
        PLAYER_PROXIMITY_RADIUS = (float) cfg.getDouble("physics.player-proximity-radius", 10.0);
        PLAYER_PROXIMITY_RADIUS_SQ = PLAYER_PROXIMITY_RADIUS * PLAYER_PROXIMITY_RADIUS;
        Medium WATER_MEDIUM = ShipPhysics.getMedium("water");
        WATER_MEDIUM.baseDensity = cfg.getDouble("buoyancy.water-density", 10.0d);
        ShipPhysics.FLUID_MEDIUMS.put("water",WATER_MEDIUM);
        waterDensity = waterDensity();
        Medium VOID_MEDIUM = ShipPhysics.getMedium("void");
        VOID_MEDIUM.baseDensity = cfg.getDouble("buoyancy.air-density", -0.5d);
        ShipPhysics.FLUID_MEDIUMS.put("void",VOID_MEDIUM);
        airDensity = airDensity();
    }

    /**
     * Safely parses a BlockFace from a YAML map, returning a default value on failure.
     * Handles null values and invalid enum names gracefully.
     */
    public static BlockFace safeBlockFace(Map<?, ?> yaml, String key, BlockFace defaultValue) {
        Object val = yaml.get(key);
        if (val == null) return defaultValue;
        try {
            return BlockFace.valueOf(val.toString().toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    public static JavaPlugin plugin;
    public final ShipModel model;
    public final String shipType = "custom";
    public org.bukkit.entity.Entity vehicle;  // Ship Wheel
    public int driverSeatIndex;  // Index of driver seat (always 0)
    public UUID id;  // Ship UUID - generated on spawn or restored from state

    public org.bukkit.entity.Entity parent;
    public List<Entity> HIDDEN_ENTITIES = new ArrayList<>();
    public org.bukkit.entity.Entity driver = null;// Seat shulkers in order (index 0 = driver)
    public com.ccorp2002.smoothsailing.region.scheduler.task.Task<?> task;
    public BukkitRunnable idleCheckTask;
    public BlockFace facing = null;
    public org.bukkit.util.Vector forwards = new org.bukkit.util.Vector();

    public float weightCalc = 0;
    public int ticksSinceLastMovement = 0;
    public int blockCount = 0;
    public boolean taskStopped = false;
    public boolean firstTick = true; // Force first tick to update positions
    public BoundingBox bbox = null;

    public void calculateBoundingBox() {
        if (BLOCK_STATES.isEmpty()) this.bbox = new BoundingBox(0, 0, 0, 0, 0, 0);
        this.bbox = new BoundingBox(minX(), minY(), minZ(), maxX(), maxY()+1.5, maxZ());
    }
    public World getWorld() {
        if (this.vehicle == null) return null;
        return this.vehicle.getWorld();
    }
    public Location getLocation() {
        if (this.vehicle == null) return null;
        return this.vehicle.getLocation();
    }

    public BoundingBox getBoundingBox() {
        if (bbox == null) calculateBoundingBox();
        return bbox;
    }

    // Speed display optimization - only update action bar when speed changes significantly
    public static final float SPEED_DISPLAY_THRESHOLD = 0.025f;
    public float previousDisplayedSpeed = 0f;

    // All config values loaded from config.yml
    public static ShipConfig config = null;

    // Delegate instances for physics and collision
    public ShipPhysics physics;
    public ShipCollision collision;

    // Driver and player tracking (public for delegates)
    public boolean hasDriver = false;
    public boolean hasPlayersNearby = false;

    // Input state tracking (set by ShipSteeringListener, read by ShipPhysics)
    public boolean isForwardPressed = false;
    public boolean isBackwardPressed = false;
    public boolean isLeftPressed = false;
    public boolean isRightPressed = false;

    // Vertical input state for airships (set by ShipSteeringListener, read by ShipPhysics)
    public boolean isSpacePressed = false;
    public boolean isSprintPressed = false;

    // Airship mode flag - determined by density at construction
    public boolean isAirship = false;

    // Landcraft mode flag - determined by future mechanics.
    public boolean isLandcraft = false;


    public int maxGear = 1;
    public int currentGear = 1;
    public int engineCount = 0;
    public int initialEngineCount = engineCount;
    public float maxHealth = 12.5f;
    public float currentHealth = maxHealth;

    public int getMaxGear() {
        return this.maxGear;
    }
    public void setMaxGear(int maxGear) {
        this.maxGear = maxGear;
    }
    public int getGear() {
        return this.currentGear;
    }
    public void setGear(int currentGear) {
        this.currentGear = java.lang.Math.max(1, java.lang.Math.min(maxGear, currentGear));
        if (driver instanceof Player) displayGears();
    }

    public void cycleGears() {
        if (engineCount != 0) {
            if (getGear() + 1 > getMaxGear()) {
                setGear(1);
            } else {
                setGear(getGear() + 1);
            }
        }
    }

    public void displayGears() {
        StringBuilder bar = new StringBuilder();
        String color = "§5";

        bar.append("§c§lGears: §r").append(color).append(" §e[");
        bar.append(color).append(""+getGear()).append("§r§6§o/§d§l"+getMaxGear());
        bar.append("§r§e]§r");
        // Send to action bar
        final net.kyori.adventure.text.Component BAR = net.kyori.adventure.text.Component.text(bar.toString());
        ENTITY_COMPOSITION.forEach(ent -> {if (ent instanceof Player passenger && passenger.isConnected()) passenger.sendActionBar(BAR);});

    }

    public float getHealth() {
        return this.currentHealth;
    }
    public float getMaxHealth() {
        return this.maxHealth;
    }
    
    public void setHealth(double health) {
        setHealth((float)health);
    }
    
    public void setMaxHealth(double health) {
        setMaxHealth((float)health);
    }
    
    
    public void setHealth(float health) {
        if (health > 0.255f) this.currentHealth = health;
        else this.currentHealth = 0.25f;
    }
    public void setMaxHealth(float health) {
        if (health > 0.255f) this.maxHealth = health;
    }


    public LocationBlockDataSet getFluidBlocks() {
        return this.FLUIDS;
    }

    
    /**
     * public constructor for creating ShipInstance from vehicle-entity and detected blocks.
     */

    public ShipInstance(JavaPlugin plugin, Entity vehicle, ShipModel model, UUID existingId) {
        this(plugin,vehicle,model,existingId,false);
    }
    public ShipInstance(JavaPlugin plugin, Entity vehicle, ShipModel model, UUID existingId, boolean dryFire) {
        plugin = plugin;
        this.model = model;
        this.driverSeatIndex = 0;
        this.id = existingId;
        this.vehicle = vehicle;
        this.parent = this.vehicle;
        // Determine if this is an airship
        // Initialize delegates
        this.collision = new ShipCollision(this);
         // 
        this.BLOCK_STATES = new MobileBlockState.MobileStateSet(model.blocks.size()+1250,0.75f); // new it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet<>(model.blocks.size()+1250,0.75f,MobileBlockState.HASH_STRAT);
        this.ENTITY_COMPOSITION = new HashSet<>((model.blocks.size()/5)+100,0.75f);
        this.FLUIDS = new LocationBlockDataSet(model.blocks.size()+2500,0.75f);
        // Create root vehicle ArmorStand (for physics, health, display mounting)
        // Players never ride this directly - they ride seat ArmorStands instead

        this.model.blockCount = (int)blockCount;
        World w = vehicle.getWorld();
        VOID_BLOCKS = true;
        PDCUtils.setPDC(vehicle,"parent","true");
        PDCUtils.setPDC(vehicle,"shipType","custom");
        PDCUtils.setPDC(vehicle,"ship_id",this.id.toString());
        PDCUtils.setPDC(vehicle,"destroyable","false");
        if (this.vehicle != null) {
            this.vehicle.setNoPhysics(true);
            this.vehicle.setInvisible(true);
            this.vehicle.setGravity(false);
            this.vehicle.setPersistent(true);
            this.vehicle.setCustomNameVisible(true);
            if (this.vehicle.getCustomName() == null) {
                this.vehicle.setCustomName("ShipRoot");
                this.vehicle.setCustomNameVisible(false);
            }
            if (this.vehicle instanceof ArmorStand as) {
                as.setRemoveWhenFarAway(false);
                as.setCollidable(false);
                as.setInvisible(false);
                as.setNoPhysics(true);
                as.setMarker(true);
                as.setSmall(true);
            }
            if (this.vehicle instanceof org.bukkit.entity.HappyGhast ghast) {
                var scaleAttr = ghast.getAttribute(org.bukkit.attribute.Attribute.SCALE);
                if (scaleAttr != null) scaleAttr.setBaseValue(0.25);
                ghast.setCollidable(false);
                ghast.setNoPhysics(true);
                ghast.setAI(false);
            }
            if (this.vehicle instanceof org.bukkit.entity.Ghast ghast) {
                var scaleAttr = ghast.getAttribute(org.bukkit.attribute.Attribute.SCALE);
                if (scaleAttr != null) scaleAttr.setBaseValue(0.25);
                ghast.setCollidable(false);
                ghast.setNoPhysics(true);
                ghast.setAI(false);
            }
            if (this.vehicle instanceof org.bukkit.entity.Shulker shulker) {
                shulker.setAI(false);
                shulker.setPeek(0);
            }
            if (this.vehicle instanceof org.bukkit.entity.Slime slime) {
                slime.setAI(false);
                slime.setWander(false);
                slime.setSize(1);
            }
            if (this.vehicle instanceof Horse hs) {
                hs.setCollidable(false);
                hs.setNoPhysics(true);
                hs.setInvisible(true);
            }
            if (this.vehicle instanceof LivingEntity le) {
                le.setRemoveWhenFarAway(false);
                le.setInvulnerable(true);
                le.setCollidable(true);
                le.setInvisible(true);
                le.setGlowing(true);
                le.setSilent(true);
            }
        }
        this.parent = this.vehicle;
        ENTITY_COMPOSITION.add(vehicle);
        if (this.id == null && this.vehicle != null) {
            this.id = this.vehicle.getUniqueId();
        }
        if (this.parent != null) {
            this.parent.setPersistent(true);
            ShipListener.addTag(parent,ShipListener.shipTag(this.id));
            ShipListener.addTag(parent,ShipListener.PARENT_TAG);
        }
        PDCUtils.setPDC(this.vehicle,"is_seat","true");
        PDCUtils.setPDC(this.parent,"is_seat","true");
        ENTITY_COMPOSITION.add(parent);
        this.HIDDEN_ENTITIES = new ArrayList<>(512);
        int seatIndex = 0;
        int wheelCount = 0;
        // Spawn each block display part as a child
        for (int blockIndex = 0; blockIndex < model.blocks.size(); blockIndex++) {
            Location blockLoc = model.blocks.get(blockIndex);
            float fbannerYaw = vehicle.getYaw();
            final int currentBlockIndex = blockIndex;  // For use in lambda
            Block block = blockLoc.getBlock();
            BlockData blockData = block.getBlockData();
            String block_id = blockData.getMaterial().toString();
            final boolean isSeat = (block_id.contains("PLAYER_") || block_id.contains("TARGET") || block_id.contains("PISTON") || block_id.contains("_CARPET"));
            BlockState state = block.getState(true);
            boolean isRollingWheel = false;
            if (block.getType() == Material.GRINDSTONE) {
                if (block.getRelative(BlockFace.UP).getType().toString().contains("CAULDRON")) {
                    Block checked = block.getRelative(BlockFace.DOWN);
                    boolean foundGround = false;
                    isRollingWheel = true;
                    for (int checkDist = 0; checkDist < 256; checkDist++) {
                        if (checked.isSolid() && !foundGround) {
                            foundGround = true;
                        } else if (!foundGround) checked = checked.getRelative(BlockFace.DOWN);
                        else continue;
                    }
                    if (foundGround) wheelCount++;
                }
            }
            // Check if this part needs special rendering (player head or banner)
            boolean hasSkullProfile = block_id.toUpperCase().contains("PLAYER_") && block_id.toUpperCase().contains("HEAD"); //(state instanceof org.bukkit.block.Skull);
            boolean hasBannerPatterns = block_id.toUpperCase().contains("BANNER"); //(state instanceof org.bukkit.block.Banner);

            Entity shulker = null;
            blockLoc.setYaw(fbannerYaw);
            Location loc = blockLoc;

            float shulkerSize = 1f;
            double health = 2.5d;
            final BlockProperties props = BlockConfigManager.getInstance().getProperties(block.getType(), blockData);
            ShipInstance.addBlock(this,block,true);
            if (health <= 1d) health = 1d;
            final double fHealth = health;
            final int finalBlockIndex = currentBlockIndex;
            if (dryFire) continue;
            if (isSeat) {
                seatIndex++;
                final int fseatIndex = seatIndex;
                Location seatLoc = loc.clone();
                if (block_id.contains("TARGET") || block_id.contains("PISTON")) seatLoc.setY(seatLoc.getY()+1.0625d);
                else if (block_id.contains("PLAYER_")) {
                    seatLoc = seatLoc.toCenterLocation();
                }
                else seatLoc.setY(seatLoc.getY()+0.0125d);
                loc = seatLoc;
                shulker = ShipInstance.addEntity(seatLoc, org.bukkit.entity.ItemDisplay.class, s -> {
                    if (s instanceof LivingEntity m) {
                        m.setAI(false);
                        m.setSilent(true);
                        if (s instanceof Mob mb) mb.setAware(false);
                        m.setCollidable(false);
                        m.setInvulnerable(true);
                    }
                    s.setGravity(false);
                    s.setInvisible(true);
                    s.setPersistent(true);
                    s.setCustomNameVisible(false);
                    s.setCustomName("[Ship Seat]");
                    PDCUtils.setPDC(s,"destroyable","false");
                    //s.setPeek(0);  // Prevent shulker from peeking/moving up
                    ShipListener.addTag(s,ShipListener.shipTag(this.id));
                    PDCUtils.setPDC(s,"is_seat","true");
                    if (s instanceof ArmorStand as) {
                        var scaleAttr = as.getAttribute(org.bukkit.attribute.Attribute.SCALE);
                        if (scaleAttr != null) scaleAttr.setBaseValue(0.25);
                        as.setDisabledSlots(org.bukkit.inventory.EquipmentSlot.values());
                        as.getEquipment().setHelmet(ItemStack.of(Material.TARGET));
                        as.setInvulnerable(true);
                        as.setMarker(true);
                        as.setSmall(true);
                        ShipListener.addTag(s,ShipListener.seatTag(fseatIndex));
                    } else if (s instanceof ItemDisplay its) {
                        if (!block_id.contains("PLAYER_")) {
                            its.setItemStack(ItemStack.of(Material.TARGET));
                            DisplayScale.setSize(its,0.0625f,0.0625f,0.0625f);
                            its.setInvisible(true);
                            ShipListener.addTag(s,ShipListener.seatTag(fseatIndex));
                        } else {
                            PDCUtils.setPDC(its,"is_seat","false");
                            its.setItemStack(ItemStack.of(Material.BLACK_WOOL));
                            DisplayScale.setSize(its,0.0625f,0.0625f,0.0625f);
                            its.setInvisible(true);
                        }
                        its.setViewRange(8f);
                        its.setInterpolationDuration(0);
                        its.setTeleportDuration(1);
                        its.setShadowRadius(0f);
                        its.setShadowStrength(0f);
                    }
                    // Apply scale attribute to change collision box size
                    if (s instanceof LivingEntity le) {
                        org.bukkit.attribute.AttributeInstance scaleAttr = le.getAttribute(org.bukkit.attribute.Attribute.SCALE);
                        if (scaleAttr != null) scaleAttr.setBaseValue(shulkerSize);
                    }
                    if (s instanceof org.bukkit.entity.Slime slime) {
                        var scaleAttr = slime.getAttribute(org.bukkit.attribute.Attribute.SCALE);
                        if (scaleAttr != null) scaleAttr.setBaseValue(0.25);
                        slime.setNoPhysics(true);
                        slime.setWander(false);
                        slime.setGlowing(false);  // Glow if debug mode enabled
                        slime.setSize(1);
                    }
                    if (s instanceof org.bukkit.entity.MagmaCube slime) {
                        var scaleAttr = slime.getAttribute(org.bukkit.attribute.Attribute.SCALE);
                        if (scaleAttr != null) scaleAttr.setBaseValue(1);
                        slime.setNoPhysics(true);
                        slime.setWander(false);
                        slime.setGlowing(false);  // Glow if debug mode enabled
                        slime.setSize(1);
                    }
                    if (s instanceof org.bukkit.entity.HappyGhast happy) {
                        var scaleAttr = happy.getAttribute(org.bukkit.attribute.Attribute.SCALE);
                        if (scaleAttr != null) scaleAttr.setBaseValue(0.25);
                        happy.setNoPhysics(true);
                    }
                    if (s instanceof LivingEntity le) {
                        le.setRemoveWhenFarAway(false);
                        org.bukkit.attribute.AttributeInstance maxHealthAttr = le.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                        if (maxHealthAttr != null) maxHealthAttr.setBaseValue(fHealth);
                        le.setHealth(fHealth);
                    }
                    s.setVisibleByDefault(true);
                });
            } else if (isRollingWheel) {
                shulker = ShipInstance.addEntity(loc, org.bukkit.entity.Minecart.class, s -> {
                    s.setSilent(true);
                    if (s instanceof Mob m) {
                        m.setAI(false);
                        m.setAware(false);
                        m.setCollidable(false);
                    }
                    s.setGravity(false);
                    s.setInvisible(true);
                    s.setPersistent(true);
                    s.setInvulnerable(true);
                    s.setCustomNameVisible(true);
                    s.setCustomName("[Rolling Wheel]");
                    PDCUtils.setPDC(s,"destroyable","true");
                    //s.setPeek(0);  // Prevent shulker from peeking/moving up
                    ShipListener.addTag(s,ShipListener.shipTag(this.id));
                    if (s instanceof ArmorStand as) {
                        as.setInvulnerable(true);
                        as.setMarker(true);
                        as.setSmall(true);
                    }
                    PDCUtils.setPDC(s,"is_seat","false");
                    // Apply scale attribute to change collision box size
                    if (s instanceof LivingEntity le) {
                        org.bukkit.attribute.AttributeInstance scaleAttr = le.getAttribute(org.bukkit.attribute.Attribute.SCALE);
                        if (scaleAttr != null) scaleAttr.setBaseValue(shulkerSize);
                    }
                    if (s instanceof org.bukkit.entity.Slime slime) {
                        var scaleAttr = slime.getAttribute(org.bukkit.attribute.Attribute.SCALE);
                        if (scaleAttr != null) scaleAttr.setBaseValue(0.625);
                        slime.setNoPhysics(true);
                        slime.setWander(false);
                        slime.setGlowing(false);  // Glow if debug mode enabled
                        slime.setSize(1);
                    }
                    if (s instanceof org.bukkit.entity.MagmaCube slime) {
                        var scaleAttr = slime.getAttribute(org.bukkit.attribute.Attribute.SCALE);
                        if (scaleAttr != null) scaleAttr.setBaseValue(1);
                        slime.setNoPhysics(true);
                        slime.setWander(false);
                        slime.setGlowing(false);  // Glow if debug mode enabled
                        slime.setSize(1);
                    }
                    if (s instanceof org.bukkit.entity.HappyGhast happy) {
                        var scaleAttr = happy.getAttribute(org.bukkit.attribute.Attribute.SCALE);
                        if (scaleAttr != null) scaleAttr.setBaseValue(0.475);
                        happy.setNoPhysics(true);
                    }
                    if (s instanceof LivingEntity le) {
                        le.setRemoveWhenFarAway(false);
                        org.bukkit.attribute.AttributeInstance maxHealthAttr = le.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                        if (maxHealthAttr != null) maxHealthAttr.setBaseValue(fHealth);
                        le.setHealth(fHealth);
                    }
                    s.setVisibleByDefault(false);
                });
            }
            if (dryFire) {
                if (shulker != null) shulker.remove();
                continue;
            }
            if (shulker != null) {
                shulker.setCustomNameVisible(false);
                ENTITY_COMPOSITION.add(shulker);
                Location pos = shulker.getLocation().toBlockLocation().add(0.5,0.0,0.5);
                pos.setY(loc.getY()+0.0125d);
                NmsUtils.teleportSilentCurrentThread(pos,shulker);
                shulker.setRotation(fbannerYaw,0f);
                if (isSeat) {
                    pos = shulker.getLocation();
                    if (shulker instanceof HappyGhast happy) {
                        pos = pos.toCenterLocation();
                        pos.setY((pos.getY()+0.0625d));
                    }
                    shulker.setInvisible(true);

                    for (org.bukkit.entity.Entity entity : shulker.getWorld().getNearbyEntities(blockLoc, 0.75, 1.25, 0.75)) {
                        if (ENTITY_COMPOSITION.contains(entity)) continue;
                        if (ShipListener.isFunctional(entity)) continue;
                        if (entity instanceof ArmorStand) {
                            ENTITY_COMPOSITION.add(entity);
                            continue;
                        }
                        if (entity instanceof HappyGhast) continue;
                        if (entity instanceof Endermite) continue;
                        if (entity instanceof MagmaCube) continue;
                        if (entity instanceof Mannequin) {
                            ENTITY_COMPOSITION.add(entity);
                            continue;
                        }
                        if (entity instanceof Player) {
                            ENTITY_COMPOSITION.add(entity);
                            continue;
                        }
                        if (entity instanceof Ghast) continue;
                        if (entity instanceof Slime) continue;
                        if (entity.equals(shulker)) continue;
                        shulker.setRotation(fbannerYaw,vehicle.getPitch());
                        entity.setRotation(fbannerYaw,vehicle.getPitch());
                        ENTITY_COMPOSITION.add(entity);
                        if (entity instanceof Display || block_id.contains("PLAYER_") || block_id.contains("_CARPET")) continue;
                        shulker.addPassenger(entity);
                    }
                    PDCUtils.setPDC(shulker,"destroyable","false");
                    PDCUtils.setPDC(shulker,"display_id",shulker.getUniqueId().toString());
                    if (!block_id.contains("PLAYER_")) PDCUtils.setPDC(shulker,"is_seat","true");
                    ENTITY_COMPOSITION.add(vehicle);
                    HIDDEN_ENTITIES.remove(shulker);
                } else {
                    HIDDEN_ENTITIES.add(shulker);
                    PDCUtils.setPDC(shulker,"destroyable","true");
                    PDCUtils.setPDC(shulker,"display_id",shulker.getUniqueId().toString());
                }
                if (shulker instanceof org.bukkit.entity.Shulker shulk) {
                    shulk.setPeek(0);
                }
                if (shulker instanceof org.bukkit.entity.Slime slim) {
                    slim.setSize(1);
                }

                // Mount shulker on carrier
                //carrier.addPassenger(shulker);
                //if (!carrier.equals(shulker)) carrier.remove();
                // Store leadable shulker reference for prefab ship lead attachment (single lead point)
                // Custom ships use per-fence attachment via leadable tags instead
                PDCUtils.setPDC(shulker,"ship_id",this.id.toString());
                if (isRollingWheel) PDCUtils.setPDC(shulker,"is_rolling_wheel","true");
                else PDCUtils.setPDC(shulker,"is_rolling_wheel","false");
                PDCUtils.setPDC(shulker,"ship_id",this.id.toString());
                ENTITY_COMPOSITION.add(shulker);
                PDCUtils.setPDC(shulker,"display_id",shulker.getUniqueId().toString());
            }
        }
        this.model.blockCount = (int)this.blockCount;
        this.model.totalWeight = (int)this.weightCalc;
        this.isAirship = (model.getDensity() < airDensity());

        if (wheelCount >= 2) {
            isLandcraft = true;
            isAirship = false;
        }
        this.physics = new ShipPhysics(this);
        if (vehicle instanceof LivingEntity le) {
            float tempMaxHealth = (float)java.lang.Math.min(SmoothSailingPlugin.MAX_POSSIBLE_HEALTH, java.lang.Math.max(25.0, maxHealth));
            org.bukkit.attribute.AttributeInstance maxHealthAttr = le.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) maxHealthAttr.setBaseValue(tempMaxHealth);
            le.setHealth(tempMaxHealth);
            setMaxHealth(maxHealth);
        }
        if (dryFire) {
            destroy();
            return;
        }
        Set<Location> INTERIOR = this.physics.getInteriorLocs();
        boolean TEMP_DEBUG = SmoothSailingPlugin.DEBUG_DUMP;
        SmoothSailingPlugin.DEBUG_DUMP = false;
        MobileBlockState.MobileStateSet INTERIOR_BLOCKS = new MobileBlockState.MobileStateSet((int)(INTERIOR.size()*1.75));
        INTERIOR.forEach(blk -> {
            if (blk == null) return;
            MobileBlockState movingState = new MobileBlockState(blk.toBlockLocation(),false);
            movingState.resetHealth(1);
            movingState.shipRef = new java.lang.ref.WeakReference<>(this);
            INTERIOR_BLOCKS.add(movingState);
        });
        SmoothSailingPlugin.DEBUG_DUMP = TEMP_DEBUG;
        if (this.vehicle instanceof org.bukkit.entity.LivingEntity s) {
            s.setCollidable(false);
            s.setGravity(false);
            s.setSilent(true);
            s.setAI(false);
            // Force rotation to match spawn location (Bukkit doesn't auto-apply yaw from Location)
            s.setInvulnerable(false);
        }
        this.currentHealth = this.maxHealth;
        this.ENTITY_COMPOSITION.add(vehicle);
        ENTITY_COMPOSITION.addAll(HIDDEN_ENTITIES);
        calculateBoundingBox();
        if (facing == null) facing = vehicle.getFacing();
        if (forwards == null || forwards.isZero()) forwards = vehicle.getLocation().getDirection();
        SmoothSailingPlugin.getInstance().getLogger().info("Piloted Ship: ("+this.id.toString()+") @ ("+LocationLib.readLocationToString(vehicle.getLocation())+") With Size: "+this.BLOCK_STATES.size());
        this.BLOCK_STATES.addAll(INTERIOR_BLOCKS);
        INTERIOR_BLOCKS.clear();
        ShipRegistry.register(this);
        Runnable RUN_ME = new Runnable() {
            @Override
            public void run() {
                Location bottomLoc = vehicle.getLocation().clone();
                Map<String,Number> DATA = ShipWheelManager.getStateBuoyancyData(bottomLoc,ShipInstance.this.BLOCK_STATES,ShipInstance.this.model.totalWeight);
                Float floatOffset = -1f;
                Number floatOff = DATA.getOrDefault("surface_offset",null);
                float heightY = maxY() - minY();
                if (floatOff == null) floatOffset = (float)(-java.lang.Math.round(heightY/3.75f)) + (float)java.lang.Math.round((heightY/3f));
                else floatOffset = -1*floatOff.floatValue();
                setDataTag("cemc:float_offset",floatOffset);
                setDataTag("cemc:center_of_volume_y",DATA.getOrDefault("center_of_volume_y",null));
                PDCUtils.setPDC(vehicle.getPersistentDataContainer(),"cemc:float_offset",""+floatOffset);
                PDCUtils.setPDC(vehicle.getPersistentDataContainer(),"cemc:center_of_volume_y",""+DATA.getOrDefault("center_of_volume_y",null));
                DATA.clear();
                if (ShipInstance.this.physics != null) {
                    ShipInstance.this.physics.handleBuoyancy(ShipInstance.this.getLocation());
                    ShipInstance.this.physics.moving = false;
                    updateWaterLogging(true);
                }
                ShipInstance.this.setProcessing(false);
                if (ShipInstance.this.engineCount <= 0) {
                    ShipInstance.this.engineCount = 0;
                    if (ShipInstance.this.isAirship && ShipInstance.this.physics != null) ShipInstance.this.physics.PER_MAX_SPEED = 0.25f;
                } else {
                    ShipInstance.this.initialEngineCount = engineCount;
                }
                if (getMaxGear() < physics.getEngineGears()) {
                    setMaxGear(physics.getEngineGears());
                    displayGears();
                }
                Runnable RUN_ME_2 = new Runnable() {
                    @Override
                    public void run() {
                        // Check if chunk is loaded - if not, skip tick but don't destroy
                        if (vehicle.isDead() || !vehicle.isValid()) {
                            destroy();
                            return;
                        } else {
                            Location loc = vehicle.getLocation();
                            if (!loc.isChunkLoaded()) {
                                destroy();
                                return; // Chunk unloaded, suspend ship but don't destroy
                            }
                        }
                        tick();
                    }
                };
                task = com.ccorp2002.smoothsailing.region.TaskUtils.getEntityScheduler(vehicle).runTaskTimer(RUN_ME_2, 1L, 1L);
            }
        };
        com.ccorp2002.smoothsailing.region.TaskUtils.getRegionalScheduler(vehicle.getLocation()).runTaskLater(RUN_ME, 7L);
        startIdleCheckTask();
    }
    
    /**
     * Builds a rotation matrix that combines the vehicle's current orientation with the model's initial rotation.
     * This matrix should be used for positioning both collision boxes AND display entities.
     *
     * Note: Display entities do NOT inherit yaw rotation from their parent vehicle - only position is inherited.
     * Therefore, we must explicitly apply the vehicle's rotation to display transforms.
     */
    public void buildRotationMatrix() {}

    public static void addBlock(final ShipInstance ship, Block block) {
        addBlock(ship,block,false);
    }
    public static void addBlock(final ShipInstance ship, Block block, boolean piloting) {
        if (ship == null || block == null) return;
        if (!block.getLocation().getWorld().equals(ship.getWorld())) return;
        final BlockProperties props = BlockConfigManager.getInstance().getProperties(block.getType(), block.getBlockData());
        if (!props.isAllowed()) {
            return;
        }
        if (ship.physics != null) ship.physics.moving = true;
        ship.setProcessing(true);
        MobileBlockState movingState = new MobileBlockState(block.getLocation().toBlockLocation(),false);
        movingState.shipRef = new java.lang.ref.WeakReference<>(ship);
        double health = 3d;
        double blockHealth = 2d;
        if (props.baseHealth != null && props.baseHealth >= 1d) {
            health = props.baseHealth;
            blockHealth = props.baseHealth+2d;
            ship.weightCalc += props.weight;
            ship.blockCount++;
            if (piloting) ship.maxHealth+=health;
        } else if (props.weight > 7.25d) {
            ship.weightCalc += props.weight;
            if (props.weight >= 10.0d) {
                health = (props.weight*0.95d)+5d;
            } else {
                health = (java.lang.Math.abs(props.weight)*0.9785d)+3.5d;
            }
            blockHealth = health;
            if (props.baseHealth == null || props.baseHealth <= 1d) blockHealth += 12.5d;
            ship.blockCount++;
            if (piloting) ship.maxHealth+=health;
        } else {
            if (java.lang.Math.abs(props.weight) >= 20d) {
                health = (java.lang.Math.abs(props.weight)*0.675d);
                if (props.weight <= 0.125) ship.weightCalc += props.weight;
                else ship.weightCalc += (props.weight*1.0625);
                blockHealth = health;
                if (props.baseHealth == null || blockHealth <= 1d) blockHealth += 6.75d;
            } else {
                health = (java.lang.Math.abs(props.weight)*0.375d);
                if (props.weight <= 0.125) ship.weightCalc += props.weight;
                else ship.weightCalc += (props.weight*1.125);
                blockHealth = health;
                if (props.baseHealth == null || blockHealth <= 1d) blockHealth += 2.5d;
            }
            ship.blockCount++;
            if (piloting) ship.maxHealth+=health;
        }
        movingState.state = NmsUtils.getBlockFaster(block.getLocation());
        if (!piloting) {
            movingState.resetHealth((long)(blockHealth/2.5d)+2l);
            if (ship.physics != null) ship.physics.moving = false;
            ship.setProcessing(false);
        } else {
            movingState.resetHealth((long)(blockHealth));
            ship.currentHealth = ship.maxHealth;
            if (props.isEngine()) ship.engineCount+=1;
        }
        ship.BLOCK_STATES.set(movingState);
    }

    public static void updateDamagedBlock(final ShipInstance ship, MobileBlockState state) {
        if (ship == null || state == null) return;
        BlockData blockData = state.getBlockData();
        final BlockProperties props = BlockConfigManager.getInstance().getProperties(blockData.getMaterial(), blockData);
        if (props.weight > 7.25d) {
            ship.weightCalc -= props.weight;
        } else {
            if (java.lang.Math.abs(props.weight) >= 10d) {
                if (props.weight <= 0.125) ship.weightCalc += java.lang.Math.abs(props.weight);
                else ship.weightCalc -= (props.weight*1.0625);
            } else {
                if (props.weight <= 0.125) ship.weightCalc += java.lang.Math.abs(props.weight);
                else ship.weightCalc -= (props.weight*1.25);
            }
        }
        if (props.isEngine()) {
            ship.engineCount -= 1;
            if (ship.engineCount <= 0) {
                ship.engineCount = 0;
                ship.physics.PER_MAX_SPEED = 0.75f;
                ship.physics.MOVEMENT_COOLDOWN += 12; //1 Second minimum
            } else {
                ship.physics.MOVEMENT_COOLDOWN += 2;
            }
            ship.displayEngineStatus();
        }
        ship.model.blockCount -= 1;
        ship.blockCount = ship.model.blockCount;
        ship.model.totalWeight = (int)ship.weightCalc;
        if (ship.isAirship) {
            ship.isAirship = (ship.model.getDensity() < airDensity());
        }
        double currentHealth = ship.getHealth();
        double newHealth = currentHealth - (double)(state.getMaxHealth()+5d);
        if (newHealth > 0d) ship.setHealth(newHealth);
        else {
            ShipSinkEvent event = new ShipSinkEvent(ship, ship.vehicle.getLocation(), ship.vehicle);
            event.callEvent();
            if (event.isCancelled()) return;
            ship.setHealth(0.25f);
            ship.sinkAndExplode();
        }
    }
    public void updateRedstone() {
        if (!this.CHANGE_WORLD) {
            updateRedstone(this.BLOCK_STATES);
        }
    }

    public void updateRedstone(final Collection<MobileBlockState> BLOCK_STATES) {
        com.ccorp2002.smoothsailing.region.TaskUtils.getRegionalScheduler(vehicle.getLocation()).runTask(()->{
            for (MobileBlockState st : BLOCK_STATES) {
                if (st == null) continue;
                final Location stloc = st.getLocation(getWorld());
                NmsUtils.UPDATE_REDSTONE_AT(stloc);
            }
        });
    }

    public void updateWaterLogging() {
        updateWaterLogging(false);
    }

    public void updateWaterLogging(boolean init) {
        if (this.physics == null || this.CHANGE_WORLD) return;
        var floating = ShipInstance.this.getDataTag("cemc:float_offset");
        Integer floatOffset = 0;
        if (floating != null && floating instanceof Double ddbl) floatOffset = (int)java.lang.Math.floor(ddbl);
        final int checkY = (int)(ShipInstance.this.physics.CURRENT_FLUID_LEVEL+floatOffset);
        final BlockData FLUID_BLOCK = (!isAirship) ? ShipPhysics.getPrimaryMediumBlockAtBelowY(physics.getExterior(0,0), checkY) : ShipPhysics.VOID;
        if (SmoothSailingPlugin.DEBUG) {
            System.out.println("CURRENT FLUID LEVEL: "+ShipInstance.this.physics.CURRENT_FLUID_LEVEL);
            System.out.println("CHECKING FLUID LEVEL: "+checkY);
            System.out.println("PRIMARY FLUID BLOCK: "+FLUID_BLOCK.getAsString(false));
        }
        if (init) {
            final int shipHeight = maxY() - minY();
            this.BLOCK_STATES.forEach(st -> {
                Medium BELOW_MEDIUM = ShipPhysics.getMedium(NmsUtils.toBukkitBlockDataFast(st.getLocation().clone().add(0,-1,0)));
                if (BELOW_MEDIUM != null && BELOW_MEDIUM.id.equalsIgnoreCase("void")) {
                    if (st.getBlockData() instanceof org.bukkit.block.data.Waterlogged waterlogged) {
                        waterlogged.setWaterlogged(false);
                        st.setBlockData(waterlogged);
                        st.teleport(st.getLocation());
                    }
                    st.occupiedBlock = ShipPhysics.VOID;
                } else if (st.getBlockY() <= ShipInstance.this.physics.CURRENT_FLUID_LEVEL) {
                    if (st.getBlockY() <= (shipHeight / 3.75)+minY()) {
                        if (st.getBlockData() instanceof org.bukkit.block.data.Waterlogged waterlogged) {
                            waterlogged.setWaterlogged(st.getBlockY() <= ShipInstance.this.physics.CURRENT_FLUID_LEVEL);
                            st.setBlockData(waterlogged);
                            st.teleport(st.getLocation());
                        }
                    }
                    st.occupiedBlock = FLUID_BLOCK;
                    FLUIDS.set(st.getLocation(),FLUID_BLOCK);
                } else {
                    st.occupiedBlock = ShipPhysics.VOID;
                }
                ShipInstance.this.BLOCK_STATES.set(st);

            });
        }
    }

    public static final Set<org.bukkit.block.BlockFace> HORIZONTAL_FACES = Set.of(org.bukkit.block.BlockFace.NORTH,org.bukkit.block.BlockFace.SOUTH,org.bukkit.block.BlockFace.EAST,org.bukkit.block.BlockFace.WEST);

    public boolean HIDE_ENTITES = true;
    public boolean VOID_BLOCKS = false;
    public boolean CHANGE_WORLD = false;

    public AtomicBoolean processing = new AtomicBoolean();

	public boolean isNotProcessing() {
		return !processing.get();
	}

	public void setProcessing(boolean processing) {
		this.processing.set(processing);
	}

    public void teleport(final Location dest) {
        if (!vehicle.isValid() || vehicle.isDead() || CHANGE_WORLD) return;
        if (dest.getWorld() == null) return;
        final Location vehicleLoc = vehicle.getLocation();
        final Location center = vehicleLoc.clone();
        sendVoids();
        CHANGE_WORLD = !center.getWorld().equals(dest.getWorld());
        int dx, dy, dz = 0;
        dx = dest.getBlockX() - center.getBlockX();
        dy = dest.getBlockY() - center.getBlockY();
        dz = dest.getBlockZ() - center.getBlockZ();
        final Location offsetf = new Location(dest.getWorld(),dx,dy,dz);
        final MobileBlockState[] TEMPS = this.BLOCK_STATES.toArray(MobileBlockState[]::new);
        physics.moving = true;
        NmsUtils.runOnMain(()->{
            physics.moving = true;
            physics.detectNearbyEntities();
            physics.currentRotationVelocity = 0.0f;
            physics.currentYVelocity = 0f;
            physics.currentSpeed = 0f;
            physics.moving = true;
            isLeftPressed = false;
            isRightPressed = false;
            isForwardPressed = false;
            isBackwardPressed = false;
            isSprintPressed = false;
            isSpacePressed = false;
            physics.moving = true;
            for (int indx = 0; indx < TEMPS.length; indx ++) {
                MobileBlockState st = TEMPS[indx];
                if (st == null) continue;
                final Location stloc = st.getLocation();
                st.tile = NmsUtils.createTileHolder(stloc);
                NmsUtils.setBlockFast(stloc,ShipPhysics.VOID);
                TEMPS[indx] = st;
            }
            this.sendVoids(Arrays.asList(TEMPS));
            NmsUtils.loadChunksIn(dest.getWorld(),this.BLOCK_STATES);
            for (int indx = 0; indx < TEMPS.length; indx ++) {
                MobileBlockState st = TEMPS[indx];
                if (st == null) continue;
                final Location stloc = st.getLocation();
                Location updLoc = stloc.clone();
                updLoc.setWorld(offsetf.getWorld());
                updLoc = updLoc.add(offsetf);
                var TEMP_WHEEL = SmoothSailingPlugin.INSTANCE.shipWheelManager.getWheelAt(stloc);
                if (TEMP_WHEEL != null && SmoothSailingPlugin.INSTANCE.shipWheelManager != null) {
                    SmoothSailingPlugin.INSTANCE.shipWheelManager.updateWheelLocation(TEMP_WHEEL, updLoc, this.facing);
                }
                BlockData occupiedBlock = NmsUtils.toBukkitBlockDataFast(updLoc);
                st.occupiedBlock = occupiedBlock;
                st.teleport(updLoc,false);
                st.setLocation(updLoc);
                getFluidBlocks().set(updLoc,st.occupiedBlock);
                TEMPS[indx] = st;
            }
            this.BLOCK_STATES.clear();
            this.BLOCK_STATES.setAll(TEMPS);
            Location moveVehicleLoc = this.vehicle.getLocation().clone();
            moveVehicleLoc.setWorld(offsetf.getWorld());
            moveVehicleLoc = moveVehicleLoc.add(offsetf);
            NmsUtils.loadChunksIn(moveVehicleLoc.getWorld(),Arrays.asList(TEMPS));
            NmsUtils.teleportSilentCurrentThread(moveVehicleLoc, this.vehicle);
            NmsUtils.loadChunksIn(center.getWorld(),this.BLOCK_STATES);
            NmsUtils.loadChunksIn(dest.getWorld(),this.BLOCK_STATES);
            physics.moving = true;
            for (Entity entity : this.ENTITY_COMPOSITION) {
                if (entity == null) continue;
                if (entity.getUniqueId().equals(vehicle.getUniqueId())) continue;
                if (entity instanceof Player pl && !pl.isConnected()) continue;
                Location entLoc = entity.getLocation().clone();
                //if (SmoothSailingPlugin.DEBUG_DUMP && entity.isEmpty()) System.out.println("DEBUG ENTITY Y0: "+entLoc.getY());
                entLoc.setWorld(offsetf.getWorld());
                entLoc = entLoc.add(offsetf);
                //if (SmoothSailingPlugin.DEBUG_DUMP && entity.isEmpty()) System.out.println("DEBUG ENTITY Y1: "+entLoc.getY());
                if (entity instanceof Player player) {
                    entLoc.setYaw(player.getEyeLocation().getYaw());
                    entLoc.setPitch(player.getEyeLocation().getPitch());
                } else {
                    entLoc.setYaw(entity.getYaw());
                    entLoc.setPitch(entity.getPitch());
                }
                if (entity.getVehicle() == null) NmsUtils.teleportSilentCurrentThread(entLoc, entity);
                //if (SmoothSailingPlugin.DEBUG_DUMP && entity.isEmpty()) System.out.println("DEBUG ENTITY Y2: "+entity.getLocation().getY());
            }
            setProcessing(true);
            physics.currentRotationVelocity = 0.0f;
            physics.currentYVelocity = 0f;
            physics.currentSpeed = 0f;
            isLeftPressed = false;
            isRightPressed = false;
            isForwardPressed = false;
            isBackwardPressed = false;
            isSprintPressed = false;
            isSpacePressed = false;
            sendVoids();
            NmsUtils.loadChunksIn(dest.getWorld(),this.BLOCK_STATES);
            this.model.minY = minY();
            this.model.maxY = maxY();
            Runnable RUN_ME = ()->{
                if (CHANGE_WORLD) CHANGE_WORLD = false;
                ShipInstance.this.calculateBoundingBox();
                physics.moving = false;
                setProcessing(false);
                updateRedstone();
                sendSolids();
            };
            com.ccorp2002.smoothsailing.region.TaskUtils.getRegionalScheduler(offsetf).runTaskLater(RUN_ME, 3L);
        });
    }

    public void sendSolids(Collection<MobileBlockState> SEND_STATES) {
        if (CHANGE_WORLD) return;
        NmsUtils.resetBlocks(vehicle.getWorld(),SEND_STATES.stream().map(
            MobileBlockState::getLocation
        ).collect(Collectors.toCollection(
            ArrayList::new              // Supplier: create a HashMap
        )));
    }


    public void sendSolids() {
        if (CHANGE_WORLD) return;
        NmsUtils.resetBlocks(vehicle.getWorld(),BLOCK_STATES.stream().map(
            MobileBlockState::getLocation
        ).collect(Collectors.toCollection(
            ArrayList::new              // Supplier: create a HashMap
        )));
    }

    public void sendVoids(Collection<MobileBlockState> SEND_STATES) {
        if (CHANGE_WORLD) return;
        NmsUtils.sendBlockChanges(vehicle.getWorld(),SEND_STATES.stream().collect(Collectors.toMap(
            MobileBlockState::getLocation,      // Key: the string itself
            MobileBlockState::getOccupied,           // Value: length of the string
            (existing, replacement) -> replacement, // Merge: keep existing on collision
            HashMap::new              // Supplier: create a HashMap
        )));
    }

    public void sendVoids() {
        if (CHANGE_WORLD) return;
        NmsUtils.sendBlockChanges(vehicle.getWorld(),BLOCK_STATES.stream().collect(Collectors.toMap(
            MobileBlockState::getLocation,      // Key: the string itself
            MobileBlockState::getOccupied,           // Value: length of the string
            (existing, replacement) -> replacement, // Merge: keep existing on collision
            HashMap::new              // Supplier: create a HashMap
        )));
    }

    public int maxX() {
        return (BLOCK_STATES.stream().parallel().mapToInt(MobileBlockState::bx).max().orElse(0));
    }
    public int minX() {
        return (BLOCK_STATES.stream().parallel().mapToInt(MobileBlockState::bx).min().orElse(0));
    }
    public int maxY() {
        return (BLOCK_STATES.stream().parallel().mapToInt(MobileBlockState::by).max().orElse(0));
    }
    public int minY() {
        return (BLOCK_STATES.stream().parallel().mapToInt(MobileBlockState::by).min().orElse(0));
    }
    public int maxZ() {
        return (BLOCK_STATES.stream().parallel().mapToInt(MobileBlockState::bz).max().orElse(0));
    }
    public int minZ() {
        return (BLOCK_STATES.stream().parallel().mapToInt(MobileBlockState::bz).min().orElse(0));
    }

    public Set<Entity> ENTITY_COMPOSITION = new HashSet<>(27550);
    public void tick() {
        if (vehicle != null && vehicle.isValid() && !vehicle.isDead()) {
            com.ccorp2002.smoothsailing.region.TaskUtils.getEntityScheduler(vehicle).runTask(()->{
                ShipInstance.this.doTick();
            });
        }
    }
    public void doTick() {
        if (firstTick) {
            physics.detectNearbyEntities();
            firstTick = false;
        }
        if (CHANGE_WORLD) {
            NmsUtils.loadChunksIn(this.vehicle.getWorld(),this.BLOCK_STATES);
            setInputState(false, false, false, false);
            setVerticalInputState(false, false);

            this.physics.currentSpeed = 0.0f;
            this.physics.currentYVelocity = 0.0f;
            this.physics.currentRotationVelocity = 0.0f;
            return;
        }
        if (driver != null && driver instanceof Player player && player.getVehicle() != null) {
            org.bukkit.Input input = player.getCurrentInput();
            setInputState(input.isForward(), input.isBackward(), input.isLeft(), input.isRight());
            setVerticalInputState(input.isJump(), input.isSprint());
        } else {
            setInputState(false, false, false, false);
            setVerticalInputState(false, false);
        }
        if (!CHANGE_WORLD && driver != null && driver instanceof Player player) (new org.bukkit.event.player.PlayerMoveEvent(player,player.getLocation(),player.getLocation())).callEvent();
        final ShipTickEvent ticking = new ShipTickEvent(this,this.vehicle);
        ticking.callEvent();
        handleSteeringInput();
        if (ticking.isCancelled()) return;
        Location currentVehicleLoc = vehicle.getLocation();
        float yaw = vehicle.getYaw();
        float pitch = vehicle.getPitch();
        com.ccorp2002.smoothsailing.region.TaskUtils.getRegionalScheduler(currentVehicleLoc).runTask(physics::update);    // Apply physics (movement, rotation, buoyancy)
    }

    public MobileBlockState.MobileStateSet getMaterial(Material TEST) {
        final MobileBlockState.MobileStateSet BLOCKS = new MobileBlockState.MobileStateSet(2500,0.75f);
        if (TEST == null) return BLOCKS;
        this.BLOCK_STATES.forEach(state -> {
            if (!state.isDead() && state.getBlockData().getMaterial() == TEST) BLOCKS.add(state);
        });
        return BLOCKS;
    }

    public MobileBlockState.MobileStateSet getBlockData(BlockData TEST) {
        final MobileBlockState.MobileStateSet BLOCKS = new MobileBlockState.MobileStateSet(2500,0.75f);
        if (TEST == null) return BLOCKS;
        this.BLOCK_STATES.forEach(state -> {
            if (!state.isDead() && state.getBlockData().matches(TEST)) BLOCKS.add(state);
        });
        return BLOCKS;
    }


    public void displayHealth() {
        displayHealth(this.currentHealth);
    }


    public void displayHealth(double currentHealth) {
        displayHealth((float)currentHealth);
    }

    public void displayHealth(float currentHealth) {
        Player player = null;
        for (Entity passenger : vehicle.getPassengers()) {
            if (player != null) break;
            if (passenger instanceof Player plyr) player = plyr;
        }
        if (player != null) {
            hasDriver = true;
            driver = player;
        } else {
            hasDriver = false;
        }

        // NOTE: Actual WASD input detection is handled by ShipSteeringListener (ProtocolLib)
        // This method just displays current speed to the player

        // Only update action bar if speed changed significantly (optimization)
        double healthPercent = currentHealth / this.maxHealth;  // -1.0 to 1.0

        // Display current speed as action bar with visual progress bar
        int barLength = 20;
        int filledBars = (int) (java.lang.Math.abs(healthPercent) * barLength);

        // Build progress bar
        StringBuilder bar = new StringBuilder();
        String color = "§c";

        bar.append("§6Ship Health: ").append(color).append(" §e[");
        for (int i = 0; i < barLength; i++) {
            if (i < filledBars) {
                bar.append(color).append("§4|");
            } else {
                bar.append("§7|");
            }
        }
        bar.append("§e]");
        // Send to action bar
        final net.kyori.adventure.text.Component BAR = net.kyori.adventure.text.Component.text(bar.toString());
        ENTITY_COMPOSITION.forEach(ent -> {if (ent instanceof Player passenger && passenger.isConnected()) passenger.sendActionBar(BAR);});
    }

    public void displayEngineStatus() {
        Player player = null;
        for (Entity passenger : vehicle.getPassengers()) {
            if (player != null) break;
            if (passenger instanceof Player plyr) player = plyr;
        }
        if (player != null) {
            hasDriver = true;
            driver = player;
        } else {
            hasDriver = false;
        }

        // NOTE: Actual WASD input detection is handled by ShipSteeringListener (ProtocolLib)
        // This method just displays current speed to the player

        // Only update action bar if speed changed significantly (optimization)
        if (engineCount == 0 || initialEngineCount == 0) return;
        double enginePercent = this.engineCount / this.initialEngineCount;  // -1.0 to 1.0

        // Display current speed as action bar with visual progress bar
        int barLength = 20;
        int filledBars = (int) (java.lang.Math.abs(enginePercent) * barLength);

        // Build progress bar
        StringBuilder bar = new StringBuilder();
        String color = "§4";

        bar.append("§6Engine Health: §r").append(color).append(" §r§e[§r");
        for (int i = 0; i < barLength; i++) {
            if (i < filledBars) {
                bar.append(color).append("§l|");
            } else {
                bar.append("§r§7|");
            }
        }
        bar.append("§r§e]§r");
        // Send to action bar
        final net.kyori.adventure.text.Component BAR = net.kyori.adventure.text.Component.text(bar.toString());
        ENTITY_COMPOSITION.forEach(ent -> {if (ent instanceof Player passenger && passenger.isConnected()) passenger.sendActionBar(BAR);});
    }

    public void handleSteeringInput() {
        Player player = null;
        if (driver instanceof Player) player = (Player) driver;

        if (player == null) {
            hasDriver = false;
            return;
        }

        hasDriver = true;

        // NOTE: Actual WASD input detection is handled by ShipSteeringListener (ProtocolLib)
        // This method just displays current speed to the player

        // Only update action bar if speed changed significantly (optimization)
        float speedPercent = physics.currentSpeed / physics.PER_MAX_SPEED;  // -1.0 to 1.0
        if (java.lang.Math.abs(speedPercent - previousDisplayedSpeed) < SPEED_DISPLAY_THRESHOLD) {
            return;  // Speed hasn't changed enough, skip update
        }
        previousDisplayedSpeed = speedPercent;

        // Display current speed as action bar with visual progress bar
        int barLength = 20;
        int filledBars = (int) (java.lang.Math.abs(speedPercent) * barLength);

        // Build progress bar
        StringBuilder bar = new StringBuilder();
        String direction = speedPercent >= 0 ? ">" : "<";
        String color = speedPercent >= 0 ? "§a" : "§c";  // Green for forward, red for reverse

        bar.append("§eSpeed: ").append(color).append(direction).append(" §7[");
        for (int i = 0; i < barLength; i++) {
            if (i < filledBars) {
                bar.append(color).append("|");
            } else {
                bar.append("§8|");
            }
        }
        bar.append("§7]");

        // Send to action bar
        player.sendActionBar(net.kyori.adventure.text.Component.text(bar.toString()));
    }
    public static boolean getLookingAt(Player player, Entity entity) {
        if (player == null || entity == null) return false;
        if (!player.getWorld().equals(entity.getWorld())) return false;
        return getEntitiesInCone(player.getEyeLocation(),128d,97.5d).contains(entity);
    }

    public static java.util.Set<Entity> getEntitiesOutisdeCone(Location initial, double maxDist, double dfov) {
        double maxDistance = maxDist+16d;
        double fov = java.lang.Math.toRadians(dfov); // 90 degrees FOV

        Location eyeLoc = initial.clone();
        final org.bukkit.util.Vector viewDirection = eyeLoc.getDirection(); // Player's looking direction
        Location startLoc = eyeLoc.clone().add(initial.getDirection().multiply(-1d));
        Set<Entity> invisibles = new HashSet<>();
        final double maxDistSquared = maxDistance * maxDistance;
        for (Entity entity : initial.getWorld().getEntities()) {
            if (entity instanceof Player) continue; // Don't check self
            if (entity.getLocation().distanceSquared(startLoc) >= maxDistSquared) continue; // Don't check too far away; hide them all

            Location entityLoc = entity.getLocation();
            org.bukkit.util.Vector toEntity = entityLoc.toVector().subtract(startLoc.toVector()); // Vector to entity

            double distance = toEntity.length();
            if (distance == 0) continue; // Avoid division by zero

            toEntity = toEntity.normalize(); // Normalize vector to get direction

            // Calculate angle between view direction and direction to entity
            double angle = java.lang.Math.acos(viewDirection.dot(toEntity));

            // Check if within FOV and distance
            if (angle < fov / 2 && distance < maxDistance) continue; // Using fov/2 for cone angle
            invisibles.add(entity);
        }
        return invisibles;
    }

    public static java.util.Set<Entity> getEntitiesInCone(Location initial, double maxDist, double dfov) {
        double maxDistance = maxDist+16d;
        double fov = java.lang.Math.toRadians(dfov); // 90 degrees FOV

        Location eyeLoc = initial.clone();
        final org.bukkit.util.Vector viewDirection = eyeLoc.getDirection(); // Player's looking direction
        Location startLoc = eyeLoc.clone().add(initial.getDirection().multiply(-1d));
        Set<Entity> visibles = new HashSet<>();
        final double maxDistSquared = maxDistance * maxDistance;
        for (Entity entity : initial.getWorld().getEntities()) {
            if (entity instanceof Player) continue; // Don't check self
            if (entity.getLocation().distanceSquared(startLoc) >= maxDistSquared) continue; // Don't check too far away; hide them all

            Location entityLoc = entity.getLocation();
            org.bukkit.util.Vector toEntity = entityLoc.toVector().subtract(startLoc.toVector()); // Vector to entity

            double distance = toEntity.length();
            if (distance == 0) continue; // Avoid division by zero

            toEntity = toEntity.normalize(); // Normalize vector to get direction

            // Calculate angle between view direction and direction to entity
            double angle = java.lang.Math.acos(viewDirection.dot(toEntity));

            // Check if within FOV and distance
            if (angle < fov / 2 && distance < maxDistance) { // Using fov/2 for cone angle
                visibles.add(entity);
            }
        }
        return visibles;
    }

    // Set input state from ShipSteeringListener
    public void setInputState(boolean forward, boolean backward, boolean left, boolean right) {
        this.isForwardPressed = forward;
        this.isBackwardPressed = backward;
        this.isLeftPressed = left;
        this.isRightPressed = right;
    }
    public boolean hasInputState() {
        return this.isForwardPressed || this.isBackwardPressed || this.isSpacePressed || this.isSprintPressed;
    }

    // Set vertical input state from ShipSteeringListener (for airships)
    public void setVerticalInputState(boolean space, boolean sprint) {
        this.isSpacePressed = space;
        this.isSprintPressed = sprint;
    }

    // Helper method to normalize angle differences to -180 to 180 range
    public static float normalizeAngle(float angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    // Helper method to convert BlockFace to yaw angle for banner rotation
    public static float getYawFromBlockFace(BlockFace face) {
        switch (face) {
            case SOUTH: return 0.0f;
            case SOUTH_SOUTH_WEST: return 22.5f;
            case SOUTH_WEST: return 45.0f;
            case WEST_SOUTH_WEST: return 67.5f;
            case WEST: return 90.0f;
            case WEST_NORTH_WEST: return 112.5f;
            case NORTH_WEST: return 135.0f;
            case NORTH_NORTH_WEST: return 157.5f;
            case NORTH: return 180.0f;
            case NORTH_NORTH_EAST: return 202.5f;
            case NORTH_EAST: return 225.0f;
            case EAST_NORTH_EAST: return 247.5f;
            case EAST: return 270.0f;
            case EAST_SOUTH_EAST: return 292.5f;
            case SOUTH_EAST: return 315.0f;
            case SOUTH_SOUTH_EAST: return 337.5f;
            default: return 0.0f;
        }
    }

    // Start a slower-polling task to check for movement when ship is idle
    public void startIdleCheckTask() {
        if (idleCheckTask != null) {
            idleCheckTask.cancel();
        }

        idleCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Check if chunk is loaded - if not, skip check but don't destroy
                Location loc = vehicle.getLocation();
                if (!loc.isChunkLoaded()) {
                    destroy();
                    cancel();
                    return; // Chunk unloaded, suspend ship but don't destroy
                }
                if (vehicle.isDead() || !vehicle.isValid()) {
                    destroy();
                    cancel();
                    return;
                }
                if (physics.customMoveTicks >= 256) {
                    physics.customMoveTicks = 0;
                    if (!physics.moving) {
                        physics.detectNearbyEntities();
                    }
                }
                physics.customMoveTicks++;
            }
        };
        // Check periodically for movement based on config
        idleCheckTask.runTaskTimer(plugin, (long) IDLE_CHECK_INTERVAL*2, (long) IDLE_CHECK_INTERVAL*2);
    }
    
    public void sinkAndExplode() {
        this.isSinking = true;

        Location dropLocation = vehicle.getLocation();
        World world = dropLocation.getWorld();

        ENTITY_COMPOSITION.forEach(ent -> {
            if (ent != null && ent instanceof Player player) {
                player.sendMessage("§6!!§cYOUR SHIP HAS BEEN DESTROYED§6!!");
                player.sendTitle("§e[§4§l!§r§e] §cWARNING §e[§4§l!§r§e]", "§6§oABANDON SHIP", 10, 100, 20);
            }
        });
        
        ENTITY_COMPOSITION.removeIf(ent -> ent instanceof Player);
        for (Entity entity : ENTITY_COMPOSITION) {
            if (entity == null) continue;
            if (entity.equals(vehicle) || entity.equals(parent)) continue;
            if (!ShipListener.isShipEntity(entity.getPersistentDataContainer())) continue;
            if (entity instanceof Mob m && m.hasAI()) continue;
            if (entity instanceof Player) continue;
            entity.remove();
        }
        // Custom ships: force disassemble, break wheel, spawn explosions
        int mx = 64;
        int yOff = -5;
        boolean wasAirship = this.isAirship;
        this.isAirship = false;
        this.isLandcraft = false;
        if (wasAirship) {
            mx = 196;
            yOff = -10;
        }
        final int MAX = mx;
        final Location OFFSET = new Location(world,0,yOff,0);
        for (int i = 0; i <= MAX; i++) {
            if (!vehicle.isValid() || vehicle.isDead() || (minY() <= physics.GLOBAL_WATER_SURFACE-24)) {
                NmsUtils.refreshChunksIn(this.vehicle.getWorld(),this.BLOCK_STATES);
                i = MAX;
            }
            final com.ccorp2002.smoothsailing.event.ShipMoveEvent mevent = new com.ccorp2002.smoothsailing.event.ShipMoveEvent(this,OFFSET,this.vehicle);
            mevent.callEvent();
            this.physics.currentYVelocity = -5f;
            if (mevent.isCancelled()) i = MAX;
            final int INDEX = i;
            this.setVerticalInputState(false, INDEX+1 >= MAX);
            com.ccorp2002.smoothsailing.region.TaskUtils.getRegionalScheduler(this.vehicle.getLocation()).runTaskLater(()->{
                Location pos = mevent.getLocation();
                this.physics.currentYVelocity = -5f;
                this.setVerticalInputState(false, INDEX+1 >= MAX);
                Set<Location> explosionLocations = new HashSet<>();
                //if (pos != null && (pos.getBlockY() >= -24 || vehicle.getLocation().getY() >= -24) && !mevent.isCancelled()) this.teleport(pos); 
                //System.out.println("SUNK Y: "+pos.getBlockY());
                //System.out.println("SINKING ITER: "+INDEX);
                for (MobileBlockState state : BLOCK_STATES) {
                    if (state != null && !state.isDead()) {
                        if (RANDOM.nextDouble() < 0.125) { // 12.5% chance
                            explosionLocations.add(state.getLocation().clone());
                        }
                    }
                }
                if (INDEX+1 >= MAX) {
                    ShipWheelManager manager = SmoothSailingPlugin.INSTANCE.getShipWheelManager();
                    ShipWheelData wheelData = manager.getWheelByShipUUID(this.id);
                    //System.out.println("FINAL SINKING ITER: "+MAX);
                    //System.out.println("WHEEL DATA: "+wheelData);
                    boolean disassembled = false;
                    if (wheelData != null) {
                        explosionLocations.add(vehicle.getLocation().clone()); // Always include root/wheel location
                        // 4. Break the ship wheel block
                        Location wheelLoc = wheelData.getBlockLocation();
                        manager.removeWheel(wheelLoc);

                        // 5. Spawn explosions at saved locations
                        spawnSinkDestructionExplosions(world, explosionLocations);

                        manager.disassembleShip(null, wheelData, true);
                        return;
                    } else {
                        explosionLocations.add(vehicle.getLocation().clone()); // Always include root/wheel location
                        spawnSinkDestructionExplosions(world, explosionLocations);
                        destroy();
                        return;
                    }
                }
            }, (i*7L));
            if (!vehicle.isValid() || vehicle.isDead() || INDEX+1 >= MAX) {
                NmsUtils.refreshChunksIn(this.vehicle.getWorld(),this.BLOCK_STATES);
                return;
            }
        }
        // Fallback if disassembly failed - use old behavior below
    }

    /**
     * Spawns destruction explosions at the given locations.
     * Used when a custom ship is destroyed - causes block, fire and entity damage.
     */
    public void spawnSinkDestructionExplosions(World world, Collection<Location> locations) {
        for (Location loc : locations) {
            loc.setWorld(world);
            world.createExplosion(loc, 2.125f, true, true);
        }
    }

    /**
     * Spawns destruction explosions at the given locations.
     * Used when a custom ship is destroyed - causes entity damage but no block damage.
     */
    public void spawnDestructionExplosions(World world, Collection<Location> locations) {
        for (Location loc : locations) {
            loc.setWorld(world);
            world.createExplosion(loc, 1.5f, false, false);
        }
    }

    /**
     * Suspends ship for chunk unload - cancels tasks but keeps entities.
     * Entity references become stale but will be recovered on chunk load.
     */
    public void suspendForChunkUnload() {}

    /**
     * Returns true if this ship needs its entities respawned.
     * This happens after chunk unload removes non-root entities.
     * @deprecated Use needsEntityRecovery() instead
     */
    @Deprecated
    public boolean needsEntityRespawn() {
        return parent == null || !parent.isValid();
    }

    /**
     * Removes all ship entities except the root vehicle ArmorStand.
     * Called on chunk unload to prevent stale entities from persisting.
     * The ship will respawn these entities when the chunk loads again.
     */
    public void removeNonRootEntities() {}

    public void destroy() {
        SmoothSailingPlugin.getInstance().getLogger().info("Unpiloted Ship: ("+this.id.toString()+") @ ("+LocationLib.readLocationToString(vehicle.getLocation())+") With Size: "+this.BLOCK_STATES.size());
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (idleCheckTask != null) {
            idleCheckTask.cancel();
            idleCheckTask = null;
        }
        // Remove all collision shulkers and their carriers
        // Note: Seats are now the shulkers themselves (no separate seat ArmorStands)
        for (Entity entity : ENTITY_COMPOSITION) {
            if (entity == null) continue;
            if (!ShipListener.isShipEntity(entity.getPersistentDataContainer())) continue;
            if (entity instanceof Mob m && m.hasAI()) continue;
            if (entity instanceof Player) continue;
            if (entity.isValid() || !entity.isDead()) entity.remove();
        }
        vehicle.remove();
        driver = null;
        for (MobileBlockState state : BLOCK_STATES) {
            if (state.shipRef != null) state.shipRef.clear();
            state.shipRef = null;
        }
        DATA_TAGS.clear();
        BLOCK_STATES.clear();
        ENTITY_COMPOSITION.clear();
        ShipRegistry.unregister(this);
    }

    /**
     * Spawns smoke particles at cannon location when fired.
     */
    public static void spawnFuelBurnEffects(Location loc) {
        if (loc == null) return;
        World world = loc.getWorld();
        if (world == null) return;
        loc = loc.toCenterLocation();
        world.spawnParticle(Particle.FLAME, loc, 10, 0.2, 0.2, 0.2, 0.05);
        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 3, 0.05, 0.05, 0.05, 0.01);

        // Sound effect
        world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 0.45f, 0.35f);
    }

    /**
     * Spawns smoke particles at cannon location when fired.
     */
    public static void spawnCannonEffects(Location loc) {
        if (loc == null) return;
        World world = loc.getWorld();
        if (world == null) return;
        loc = loc.toCenterLocation();
        world.spawnParticle(Particle.SMOKE, loc, 10, 0.1, 0.1, 0.1, 0.05);
        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 3, 0.05, 0.05, 0.05, 0.01);

        // Sound effect
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);
    }

    public void setDataTag(String key, Object data) {
        if (key == null) return;
        if (data == null) return;
        key = key.toLowerCase(Locale.ENGLISH);
        this.DATA_TAGS.put(key, data);
    }

    public Object removeDataTag(String key) {
        if (key == null) return null;
        key = key.toLowerCase(Locale.ENGLISH);
        Object tag = this.DATA_TAGS.getOrDefault(key, null);
        this.DATA_TAGS.remove(key);
        return tag;
    }

    public <V> V getDataTag(String key) {
        if (key == null) return null;
        key = key.toLowerCase(Locale.ENGLISH);
        return (V) this.DATA_TAGS.getOrDefault(key, null);
    }
}
