package com.ccorp2002.smoothsailing.ship;

import com.ccorp2002.smoothsailing.*;
import com.ccorp2002.smoothsailing.ShipConfig;
import com.ccorp2002.smoothsailing.ShipListener;
import com.ccorp2002.smoothsailing.detection.*;
import com.ccorp2002.smoothsailing.utils.vectors.*;
import com.ccorp2002.smoothsailing.utils.rotation.*;
import com.ccorp2002.smoothsailing.utils.movement.*;
import com.ccorp2002.smoothsailing.utils.objects.*;
import com.ccorp2002.smoothsailing.utils.*;
import com.ccorp2002.smoothsailing.event.*;
import com.ccorp2002.smoothsailing.nmsutils.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Rail;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.RedstoneWire;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.*;
import org.bukkit.util.Vector;
import org.bukkit.block.BlockFace;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.util.BoundingBox;
import org.bukkit.Tag;
import org.joml.Vector3f;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import java.util.stream.Collectors;
import java.util.concurrent.Callable;
import java.util.*;

/**
 * Handles movement and physics logic for a ship.
 * Owns velocity state and applies movement, drag, buoyancy, and rotation.
 */
public class ShipPhysics {
    public static final EnumSet<Material> FLUID = EnumSet.of(Material.WATER, Material.END_GATEWAY, Material.NETHER_PORTAL, Material.BUBBLE_COLUMN, Material.SEAGRASS, Material.TALL_SEAGRASS, Material.KELP, Material.KELP_PLANT, Material.SEA_PICKLE);
    public static final EnumSet<Material> DESTRUCTIBLE = EnumSet.of(Material.SHORT_GRASS, Material.SEAGRASS, Material.TALL_SEAGRASS, Material.KELP, Material.KELP_PLANT, Material.SEA_PICKLE);
    public static final EnumSet<Material> LANDCRAFT_PASSTHROUGH = EnumSet.of(Material.MUD);

    public static final EnumSet<Material> FRAGILE_MATERIALS = EnumSet.noneOf(Material.class);
    public static final Map<String,Medium> FLUID_MEDIUMS = new HashMap<>();
    public static final BlockData LAVA = Material.LAVA.createBlockData();
    public static final BlockData WATER = Material.WATER.createBlockData();
    public static final BlockData SOLID = Material.BARRIER.createBlockData();
    public static final BlockData VOID = Material.AIR.createBlockData();
    public static final Vector UP_VECTOR = new Vector(0,1,0);
    static {
        FLUID.addAll(Tag.FLOWERS.getValues());
        FLUID.addAll(Tag.CORAL_PLANTS.getValues());
        FLUID.addAll(Tag.LEAVES.getValues());
        FLUID.addAll(FragileBlocks.FRAGILE_BLOCKS);
        FLUID.remove(Material.LILY_PAD);
        FLUID.add(Material.CAVE_AIR);
        LANDCRAFT_PASSTHROUGH.addAll(Tag.LEAVES.getValues());
        LANDCRAFT_PASSTHROUGH.addAll(Tag.LOGS.getValues());
        DESTRUCTIBLE.addAll(FragileBlocks.FRAGILE_BLOCKS);
        DESTRUCTIBLE.addAll(Tag.CORAL_PLANTS.getValues());
        DESTRUCTIBLE.addAll(Tag.LOGS.getValues());
        DESTRUCTIBLE.addAll(Tag.LEAVES.getValues());
        DESTRUCTIBLE.add(Material.PALE_MOSS_CARPET);
        DESTRUCTIBLE.add(Material.MOSS_CARPET);
        DESTRUCTIBLE.removeAll(Set.of(
            Material.SCAFFOLDING,
            Material.LADDER,
            Material.PISTON,
            Material.STICKY_PISTON,
            Material.PISTON_HEAD,
            Material.MOVING_PISTON,
            Material.REPEATER,
            Material.COMPARATOR,
            Material.REDSTONE_WIRE)
        );
        DESTRUCTIBLE.add(Material.LILY_PAD);
        DESTRUCTIBLE.removeAll(Tag.DOORS.getValues());
        DESTRUCTIBLE.removeAll(Tag.CARPETS.getValues());
        DESTRUCTIBLE.removeAll(Tag.RAILS.getValues());
        DESTRUCTIBLE.removeAll(Tag.WOODEN_PRESSURE_PLATES.getValues());
        FRAGILE_MATERIALS.addAll(Set.of(
            Material.SCAFFOLDING,
            Material.LADDER,
            Material.PISTON,
            Material.STICKY_PISTON,
            Material.PISTON_HEAD,
            Material.MOVING_PISTON,
            Material.REPEATER,
            Material.COMPARATOR,
            Material.REDSTONE_WIRE)
        );
        FRAGILE_MATERIALS.add(Material.LILY_PAD);
        FRAGILE_MATERIALS.add(Material.PISTON_HEAD);
        FRAGILE_MATERIALS.add(Material.TORCH);
        FRAGILE_MATERIALS.add(Material.REDSTONE_WIRE);
        FRAGILE_MATERIALS.add(Material.LADDER);
        FRAGILE_MATERIALS.addAll(Tag.DOORS.getValues());
        FRAGILE_MATERIALS.addAll(Tag.CARPETS.getValues());
        FRAGILE_MATERIALS.addAll(Tag.RAILS.getValues());
        FRAGILE_MATERIALS.addAll(Tag.WOODEN_PRESSURE_PLATES.getValues());
        //LAVA SHIPS
        Medium LAVA_MEDIUM = new Medium();
        LAVA_MEDIUM.BLOCKS.add(0,LAVA);
        BlockType LAVA_TYPE = Material.LAVA.asBlockType();
        LAVA_MEDIUM.BLOCKS.addAll(LAVA_TYPE.createBlockDataStates());
        LAVA_MEDIUM.BLOCKS.add(Material.MAGMA_BLOCK.createBlockData());
        LAVA_MEDIUM.baseDensity = 25d;
        LAVA_MEDIUM.id = "lava";
        LAVA_MEDIUM.dragMultiplier = 0.275d;
        FLUID_MEDIUMS.put("lava",LAVA_MEDIUM);
        //WATER SHIPS
        Medium WATER_MEDIUM = new Medium();
        WATER_MEDIUM.BLOCKS.add(0,WATER);
        for (Material FLUID_MAT : FLUID) {
            BlockType WATER_TYPE = FLUID_MAT.asBlockType();
            WATER_MEDIUM.BLOCKS.addAll(WATER_TYPE.createBlockDataStates());
        }
        WATER_MEDIUM.baseDensity = SmoothSailingPlugin.getPlugin().getConfig().getDouble("buoyancy.water-density", 10d);
        WATER_MEDIUM.id = "water";
        WATER_MEDIUM.dragMultiplier = 0.325d;
        FLUID_MEDIUMS.put("water",WATER_MEDIUM);
        //AIR SHIPS
        Medium VOID_MEDIUM = new Medium();
        VOID_MEDIUM.BLOCKS.add(0,VOID);
        for (Material VOID_MAT : EnumSet.of(Material.END_GATEWAY, Material.NETHER_PORTAL, Material.AIR, Material.CAVE_AIR)) {
            BlockType VOID_TYPE = VOID_MAT.asBlockType();
            VOID_MEDIUM.BLOCKS.addAll(VOID_TYPE.createBlockDataStates());
        }
        VOID_MEDIUM.baseDensity = SmoothSailingPlugin.getPlugin().getConfig().getDouble("buoyancy.air-density", -0.25d);
        VOID_MEDIUM.id = "void";
        VOID_MEDIUM.dragMultiplier = 0.575d;
        FLUID_MEDIUMS.put("void",VOID_MEDIUM);
    }

    public static boolean isDataMedium(MobileBlockState block) {
        BlockData data = block.occupiedBlock;
        return isDataMedium(data);
    }

    public static boolean isDataMedium(Block block) {
        BlockData data = block.getBlockData();
        return isDataMedium(data);
    }

    public static boolean isDataMedium(BlockData data) {
        if (data == null) return true;
        for (Medium m : FLUID_MEDIUMS.values()) {
            if (Medium.isDataMedium(m, data)) return true;
        }
        return false;
    }

    public static Medium getMedium(String id) {
        if (id == null || id.isEmpty()) return null;
        for (Medium m : FLUID_MEDIUMS.values()) {
            if (m.id.equalsIgnoreCase(id)) return m;
        }
        return null;
    }
    public static Medium getMedium(MobileBlockState block) {
        if (block == null) return null;
        return getMedium(block.occupiedBlock);
        
    }
    public static Medium getMedium(BlockData data) {
        if (data == null) return null;
        for (Medium m : FLUID_MEDIUMS.values()) {
            if (Medium.isDataMedium(m, data)) return m;
        }
        return null;
    }
    public static BlockData getPrimaryMediumAtBelowY(final Collection<MobileBlockState> BLOCK_STATES, int y) {
        BLOCK_STATES.removeIf(s -> s.getBlockY() >= y);
        Map.Entry<String, Integer> maxEntry = getPrimaryMedium(BLOCK_STATES);
        if (maxEntry == null) return VOID;
        Medium medium = getMedium(maxEntry.getKey());
        if (medium == null) return VOID;
        return medium.getBlockDatas().getFirst();
    }

    public static BlockData getPrimaryMediumBlockAtBelowY(Collection<Block> BLOCKS, int y) {
        BLOCKS.removeIf(s -> s.getY() > y);
        Map.Entry<String, Integer> maxEntry = getPrimaryMediumBlock(BLOCKS);
        if (maxEntry == null) return VOID;
        Medium medium = getMedium(maxEntry.getKey());
        if (medium == null) return VOID;
        return medium.getBlockDatas().getFirst();
    }
    
    public static BlockData getPrimaryMediumBlockData(Collection<Block> BLOCK_STATES) {
        Map.Entry<String, Integer> maxEntry = getPrimaryMediumBlock(BLOCK_STATES);
        if (maxEntry == null) return VOID;
        Medium medium = getMedium(maxEntry.getKey());
        if (medium == null) return VOID;
        return medium.getBlockDatas().getFirst();
    }
    
    public static BlockData getPrimaryMediumData(Collection<MobileBlockState> BLOCK_STATES) {
        Map.Entry<String, Integer> maxEntry = getPrimaryMedium(BLOCK_STATES);
        if (maxEntry == null) return VOID;
        Medium medium = getMedium(maxEntry.getKey());
        if (medium == null) return VOID;
        return medium.getBlockDatas().getFirst();
    }

    public static Map.Entry<String, Integer> getPrimaryMediumBlock(Collection<Block> BLOCKS) {
        final Map<String,Integer> FLUID_COUNTER = new HashMap<>();
        for (Block STATE : BLOCKS) {
            Medium m = getMedium(STATE.getBlockData());
            if (m == null) continue;
            Integer mi = FLUID_COUNTER.getOrDefault(m.id,0);
            if (m.id.equalsIgnoreCase("void")) continue;
            mi += 1;
            FLUID_COUNTER.put(m.id,mi);
        }
        if (FLUID_COUNTER.isEmpty()) {
            return null;
        }
        Map.Entry<String, Integer> maxEntry = Collections.max(
            FLUID_COUNTER.entrySet(), 
            Map.Entry.comparingByValue()
        );
        FLUID_COUNTER.clear();
        return maxEntry;
    }
    public static Map.Entry<String, Integer> getPrimaryMedium(Collection<MobileBlockState> BLOCK_STATES) {
        final Map<String,Integer> FLUID_COUNTER = new HashMap<>();
        for (MobileBlockState STATE : BLOCK_STATES) {
            Medium m = getMedium(STATE);
            if (m == null) continue;
            Integer mi = FLUID_COUNTER.getOrDefault(m.id,0);
            if (m.id.equalsIgnoreCase("void")) continue;
            mi += 1;
            FLUID_COUNTER.put(m.id,mi);
        }
        if (FLUID_COUNTER.isEmpty()) {
            return null;
        }
        Map.Entry<String, Integer> maxEntry = Collections.max(
            FLUID_COUNTER.entrySet(), 
            Map.Entry.comparingByValue()
        );
        FLUID_COUNTER.clear();
        return maxEntry;
    }

    public float getFluidMediumDrag() {
        // Apply drag based on medium presence (unless actively pressing W/S)
        if (ship == null) return 0f;
        if (ship.isNotProcessing()) return 0.45f;
        ShipConfig config = ShipInstance.config;
        float dragMultiplier = 0f;
        Map.Entry<String, Integer> maxEntry = getPrimaryMedium(ship.BLOCK_STATES);
        if (maxEntry == null) return 0.25f;
        Integer maxValue = maxEntry.getValue();
        dragMultiplier = maxValue + 0.0f;
        if (Math.abs(dragMultiplier) >= 1) { //Fallback
            dragMultiplier = 0.125f;
        }
        return dragMultiplier;
    }

    public float getMaxEngineSpeedFactor() {
        if (ship == null) return 0.75f;
        if (ship.initialEngineCount == 0 || ship.engineCount == 0) return 0.75f;
        final double enginePercent = (ship.engineCount / ship.initialEngineCount);
        float speedFactor = this.PER_MAX_SPEED;
        if (ship.engineCount > 0 && enginePercent >= 0.25d) {
            speedFactor += (Math.abs(speedFactor) * (ship.engineCount / ship.blockCount)+0.5f);
            if (SmoothSailingPlugin.DEBUG) System.out.println("ENGINE BLOCK PERCENT: "+enginePercent+"; CURRENT ENGINE COUNT: "+ship.engineCount+"; NEW SPEED FACTOR: "+speedFactor);
        }
        if (speedFactor >= 25f) {
            speedFactor = 25f; //TEMPORARY; HARD CAP
        }
        return speedFactor;
    }

    public int getEngineGears() {
        final int enginePercent = (int)getEnginePercent();
        int engineGear = (enginePercent+1);
        return engineGear;
    }

    public double getEnginePercent() {
        if (ship == null) return 1d;
        if (ship.initialEngineCount == 0 || ship.engineCount == 0) return 1d;
        final double enginePercent = ((ship.engineCount) / ((ship.initialEngineCount*0.675f)));
        return enginePercent;
    }

    public void processFluidMediumDrag() {
        if (ship == null) return;
        ShipConfig config = ShipInstance.config;
        if (Math.abs(this.currentSpeed) >= 0.275f) {
            float dragMultiplier = getFluidMediumDrag();

            // Apply extra drag in fluid mediums
            if (this.currentSpeed > 0.5f) {
                this.currentSpeed *= dragMultiplier;
            } else if (this.currentSpeed < -0.5f) {
                this.currentSpeed *= dragMultiplier;
            } else {
                this.currentSpeed = 0.0f;
            }
        } else {
            this.currentSpeed = 0.0f;
        }
        if (Math.abs(this.currentYVelocity) >= 0.275f) {
            if (this.currentYVelocity > 0.5f) {
                this.currentYVelocity -= 0.5f;
            } else if (this.currentYVelocity < -0.5f) {
                this.currentYVelocity += 0.5f;
            } else {
                this.currentYVelocity = 0.0f;
            }
        } else {
            this.currentYVelocity = 0.0f;
        }
    }


    public final ShipInstance ship;
    // Grid snap resolution (4.0 = quarter block, i.e. 1/4 = 0.25)
    public volatile int customRotationTicks = 0;
    public volatile int customMoveTicks = 0;
    public volatile int physicsInterval = 0;
    public volatile int loadTicks = 0;
    // Velocity state
    public static float MAX_SPEED = 3f;
    public float PER_MAX_SPEED = MAX_SPEED;
    public volatile float currentSpeed = 0.0f;
    public volatile float currentYVelocity = 0.0f;
    public volatile float currentRotationVelocity = 0.0f;
    public volatile boolean moving = false;
    public long MOVEMENT_COOLDOWN = 8l;
    public int GLOBAL_WATER_SURFACE = -127;
    public int CURRENT_FLUID_LEVEL = GLOBAL_WATER_SURFACE;
    private int ROTATION_INCR = 1;

    public BlockData getVOID() {
        return ShipPhysics.VOID;
    }
    public void setRotationIncr(int new_incr) {
        this.ROTATION_INCR = Math.max(-1, Math.min(4, new_incr));
    }
    public int getRotationIncr() {
        return this.ROTATION_INCR;
    }

    public ShipPhysics(ShipInstance ship) {
        this.ship = ship;
        this.GLOBAL_WATER_SURFACE = SmoothSailingPlugin.INSTANCE.getConfig().getInt("water-level",63);
        this.CURRENT_FLUID_LEVEL = this.GLOBAL_WATER_SURFACE;
        if (ship.isAirship) PER_MAX_SPEED = 5f;
        else if (ship.isLandcraft) PER_MAX_SPEED = 2f;
        else PER_MAX_SPEED = 3f;
    }

    public static double ROUND_VALUE(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }
    public static float CLAMP_VALUE(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }
    /**
     * Main physics update. Applies acceleration, drag, buoyancy, and movement.
     * Called from ShipInstance.tick()
     */

    public void update() {
        if (ship.vehicle == null || ship.vehicle.isDead()) return;
        this.loadTicks++;
        if (this.loadTicks <= 7 && this.loadTicks >= 5) {
            final ShipVelocityTickEvent velocityEvent = new ShipVelocityTickEvent(ship,ship.vehicle);
            velocityEvent.callEvent();
        }
        if (this.loadTicks <= 25 && this.loadTicks >= 20) {
            if (ship.vehicle.isValid() && ship.vehicle != null && !ship.isSinking) {
                ship.displayHealth();
            }
        }
        if (!ship.hasInputState()) processFluidMediumDrag();
        if (this.loadTicks >= 120) {
            this.loadTicks = 0;
        }
        if (this.loadTicks == 0) NmsUtils.loadChunksIn(ship.vehicle.getWorld(),ship.BLOCK_STATES);
        if (!ship.isNotProcessing() || this.moving) return;
        BlockFace facing = ship.facing;
        if (facing == null) {
            facing = ship.vehicle.getFacing();
            ship.facing = facing;
        }
        Vector forwards = ship.forwards;
        if (forwards == null) {
            forwards = ship.vehicle.getLocation().getDirection();
            ship.forwards = forwards;
        }
        if (ship.isSinking) {
            ship.driver = ship.vehicle;
            ship.hasDriver = true;
        }
        Player pilot = null;
        ShipConfig config = ShipInstance.config;
        Location vehicleLoc = ship.vehicle.getLocation();

        double forwardX = ROUND_VALUE(forwards.getX()*2,2);
        double forwardZ = ROUND_VALUE(forwards.getZ()*2,2);
        // Apply acceleration/deceleration based on input state
        if (!ship.isSinking) {
            if (ship.driver != null && ship.driver instanceof Player) {
                pilot = (Player)ship.driver;
                if (pilot.getFacing() == ship.facing) {
                    facing = ship.facing;
                } else if (pilot.getFacing().getOppositeFace() == ship.facing) {
                    facing = ship.facing;
                } else {
                    Vector pilotFacing = pilot.getLocation().getDirection();
                    forwardX += pilotFacing.getX();
                    forwardZ += pilotFacing.getZ();
                    forwardX = ROUND_VALUE(forwardX,2);
                    forwardZ = ROUND_VALUE(forwardZ,2);
                }
            }
            if (ship.hasInputState()) {
                float accel = (float)0.475f+((getMaxEngineSpeedFactor()*0.625f));
                if (ship.isForwardPressed) {
                    currentSpeed = Math.min(currentSpeed + accel, PER_MAX_SPEED);
                } else if (ship.isBackwardPressed) {
                    if (currentSpeed > 0) {
                        currentSpeed = Math.max(currentSpeed - accel, 0.0f);
                    } else {
                        currentSpeed = Math.max(currentSpeed - accel, -PER_MAX_SPEED);
                    }
                }
                this.currentSpeed = ShipPhysics.CLAMP_VALUE(this.currentSpeed,-this.PER_MAX_SPEED,this.PER_MAX_SPEED);
                if (ship.isSpacePressed) this.currentYVelocity += (accel/1.125f)+0.25f;
                else if (ship.isSprintPressed) this.currentYVelocity -= (accel/1.125f)+0.25f;
                this.currentYVelocity = ShipPhysics.CLAMP_VALUE(this.currentYVelocity,-this.PER_MAX_SPEED,this.PER_MAX_SPEED);
                if (ship.getGear() > ship.getMaxGear()) {
                    ship.setGear(ship.getMaxGear());
                    ship.displayGears();
                }
            } else {
                this.currentSpeed = ShipPhysics.CLAMP_VALUE(this.currentSpeed,-this.PER_MAX_SPEED/2,this.PER_MAX_SPEED/2);
                this.currentYVelocity = ShipPhysics.CLAMP_VALUE(this.currentYVelocity,-this.PER_MAX_SPEED/2,this.PER_MAX_SPEED/2);
            }
        } else {
            this.currentYVelocity = -0.75f;
        }

        // Stop if speed is very small
        if (!ship.isAirship) this.currentYVelocity = ShipPhysics.CLAMP_VALUE(this.currentYVelocity,-2f,2f);
        var manager = SmoothSailingPlugin.INSTANCE.getShipWheelManager();
        ShipWheelData wheelData = manager.getWheelByShipUUID(ship.id);

        // Update rotation based on input state
        if (ship.isLeftPressed) {
            currentRotationVelocity = -1f;
        } else if (ship.isRightPressed) {
            currentRotationVelocity = 1f;
        } else {
            currentRotationVelocity = 0f;
        }
        if (ship.isSinking) currentYVelocity = -1.5f;
        if (physicsInterval >= 15) {
            physicsInterval = 0;
            handleBuoyancy(ship.getLocation());
        } else physicsInterval++;
        if (SmoothSailingPlugin.INSTANCE == null) {
            ship.setProcessing(false);
            ship.isLeftPressed = false;
            ship.isRightPressed = false;
            ship.isForwardPressed = false;
            ship.isBackwardPressed = false;
            ship.isSprintPressed = false;
            ship.isSpacePressed = false;
            this.currentRotationVelocity = 0.0f;
            this.currentYVelocity = 0f;
            this.currentSpeed = 0f;
            this.moving = false;
            return;
        }
        detectNearbyEntities();
        List<Entity> FILTERED_ENTS = ship.ENTITY_COMPOSITION.stream().filter(p -> p.isValid()).collect(Collectors.toList());
        final Location ABS_CENTER = new Location(ship.vehicle.getWorld(),0,0,0);
        // Move the vehicle
        if ((ship.isLeftPressed || ship.isRightPressed) && (ship.isNotProcessing() && !this.moving)) {
            // Apply rotation
            Location newLoc = vehicleLoc.clone();
            Location CENTER_POINT = ship.vehicle.getLocation().toBlockLocation();
            newLoc.setYaw(DirectionalUtils.blockFaceToYaw(ship.facing));
            float newYaw = newLoc.getYaw();
            float rotation = 0.0f;
            final String LEFT_RIGHT;
            final org.bukkit.block.structure.StructureRotation TURN_BLOCK_DATA;
            if (ship.isLeftPressed) {rotation = -90.0f; TURN_BLOCK_DATA = org.bukkit.block.structure.StructureRotation.COUNTERCLOCKWISE_90; LEFT_RIGHT = "LEFT";}
            else if (ship.isRightPressed) {rotation = 90.0f; TURN_BLOCK_DATA = org.bukkit.block.structure.StructureRotation.CLOCKWISE_90; LEFT_RIGHT = "RIGHT";}
            else {LEFT_RIGHT = "NONE"; TURN_BLOCK_DATA = org.bukkit.block.structure.StructureRotation.NONE;}
            final float initYaw = ship.vehicle.getYaw();
            final float rotationF = rotation;
            Location wheelLoc = null;
            ship.isLeftPressed = false;
            ship.isRightPressed = false;
            final Player pilotF = pilot;
            final Runnable ROTATE_SHIP = () -> {
                this.moving = true;
                int collisionCount = 0;
                boolean canEnterAt = true;
                final MobileBlockState[] TEMPS = ship.BLOCK_STATES.toArray(MobileBlockState[]::new);
                for (MobileBlockState st : TEMPS) {
                    if (st == null) continue;
                    Location stloc = st.getLocation(ship.getWorld());
                    Location updLoc = stloc.clone();
                    Location rotatedLoc = Rotate2D.ROTATE_VEC(LEFT_RIGHT, updLoc.subtract(CENTER_POINT)).add(CENTER_POINT).toBlockLocation();
                    if (pilotF != null) {
                        for (final com.ccorp2002.smoothsailing.utils.claims.IClaimInterface ICLAIM : SmoothSailingPlugin.INSTANCE.CLAIM_INTEFACES) {
                            if (!canEnterAt) break;
                            if (!ICLAIM.canEnterAt(pilotF,rotatedLoc)) canEnterAt = false;
                            if (!NmsUtils.withinWorldBorder(rotatedLoc)) canEnterAt = false;
                            
                        }
                    }
                    if (!canEnterAt) {
                        collisionCount++;
                        if (SmoothSailingPlugin.DEBUG_DUMP) System.out.println("ROTATION COLLISION COUNT: "+collisionCount);
                        ship.isLeftPressed = false;
                        ship.isRightPressed = false;
                        ship.isForwardPressed = false;
                        ship.isBackwardPressed = false;
                        ship.isSprintPressed = false;
                        ship.isSpacePressed = false;
                        ship.setProcessing(false);
                        this.currentRotationVelocity = 0.0f;
                        this.currentYVelocity = 0f;
                        this.currentSpeed = 0f;
                        this.moving = false;
                        return;
                    }
                    Block block = rotatedLoc.getBlock();
                    if (DESTRUCTIBLE.contains(block.getType()) && !MobileBlockState.doesShipHave(ship,rotatedLoc) && !FRAGILE_MATERIALS.contains(block.getType())) {
                        if (SmoothSailingPlugin.DEBUG_DUMP) System.out.println("COLLIDED & BROKE?: "+block.getType()+" LOCATION?: "+rotatedLoc.toVector().toString());
                        block.breakNaturally(true);
                        continue;
                    }
                    if (MobileBlockState.doesShipHave(ship,rotatedLoc)) {
                        continue;
                    }
                    if (block.isSolid() && !isDataMedium(block) && !(LANDCRAFT_PASSTHROUGH.contains(block.getType()) && ship.isLandcraft)) {
                        if (SmoothSailingPlugin.DEBUG_DUMP) System.out.println("COLLIDED?: "+!MobileBlockState.doesShipHave(ship,rotatedLoc)+" LOCATION?: "+rotatedLoc.toVector().toString()+" BLOCKDATA?:\n >"+block.getBlockData().getAsString(true));
                        collisionCount++;
                    }
                }
                if (collisionCount > 0) {
                    if (SmoothSailingPlugin.DEBUG_DUMP) System.out.println("ROTATION COLLISION COUNT: "+collisionCount);
                    ship.isLeftPressed = false;
                    ship.isRightPressed = false;
                    ship.isForwardPressed = false;
                    ship.isBackwardPressed = false;
                    ship.isSprintPressed = false;
                    ship.isSpacePressed = false;
                    ship.setProcessing(false);
                    this.currentRotationVelocity = 0.0f;
                    this.currentYVelocity = 0f;
                    this.currentSpeed = 0f;
                    this.moving = false;
                    return;
                }
                newLoc.setPitch(ship.vehicle.getPitch());
                com.ccorp2002.smoothsailing.event.ShipRotateEvent mevent = new com.ccorp2002.smoothsailing.event.ShipRotateEvent(ship,newLoc,ship.vehicle,TURN_BLOCK_DATA);
                mevent.callEvent();
                if (mevent.isCancelled()) {
                    ship.isLeftPressed = false;
                    ship.isRightPressed = false;
                    ship.isForwardPressed = false;
                    ship.isBackwardPressed = false;
                    ship.isSprintPressed = false;
                    ship.isSpacePressed = false;
                    ship.setProcessing(false);
                    this.currentRotationVelocity = 0.0f;
                    this.currentYVelocity = 0f;
                    this.currentSpeed = 0f;
                    this.moving = false;
                    return;
                }
                ship.isLeftPressed = false;
                ship.isRightPressed = false;
                this.currentRotationVelocity = 0.0f;
                final float xzVelocity = this.currentSpeed;
                final float yVelocity = this.currentYVelocity;
                //NmsUtils.refreshChunksIn(ship.vehicle.getWorld(),TEMPS);
                ship.setProcessing(true);
                this.moving = true;
                detectNearbyEntities();
                this.currentSpeed = 0f;
                this.currentYVelocity = 0f;
                //System.out.println("STARTING BLOCK COUNT: "+TEMPS.length);
                for (int indx = 0; indx < TEMPS.length; indx ++) {
                    MobileBlockState st = TEMPS[indx];
                    if (st == null) continue;
                    final Location stloc = st.getLocation(ship.getWorld());
                    st.tile = NmsUtils.createTileHolder(stloc);
                    NmsUtils.setBlockFast(stloc,st.occupiedBlock);
                    TEMPS[indx] = st;
                }
                ship.sendVoids(Arrays.asList(TEMPS));
                //System.out.println("MID ROTATION BLOCK COUNT: "+TEMPS.length);
                for (int indx = 0; indx < TEMPS.length; indx ++) {
                    MobileBlockState st = TEMPS[indx];
                    if (st == null) continue;
                    final Location stloc = st.getLocation(ship.getWorld());
                    Location updLoc = stloc.clone();
                    updLoc = Rotate2D.ROTATE_VEC(LEFT_RIGHT, stloc.clone().subtract(CENTER_POINT)).add(CENTER_POINT).toBlockLocation();
                    updLoc.setYaw(0.0f);
                    st.occupiedBlock = NmsUtils.toBukkitBlockDataFast(updLoc);
                    BlockData TURNED = st.getBlockData().clone();
                    TURNED = NmsUtils.rotateBlockData(LEFT_RIGHT,TURNED);
                    ship.FLUIDS.set(updLoc,st.occupiedBlock);
                    st.setBlockData(TURNED);
                    st.teleport(updLoc,false);
                    st.setLocation(updLoc);
                    var TEMP_WHEEL = SmoothSailingPlugin.INSTANCE.shipWheelManager.getWheelAt(stloc);
                    if (TEMP_WHEEL != null && SmoothSailingPlugin.INSTANCE.shipWheelManager != null) {
                        SmoothSailingPlugin.INSTANCE.shipWheelManager.updateWheelLocation(TEMP_WHEEL, updLoc, ship.facing);
                    }
                    TEMPS[indx] = st;
                }
                ship.BLOCK_STATES.clear();
                ship.BLOCK_STATES.setAll(TEMPS);
                //System.out.println("POST ROTATION BLOCK COUNT: "+ship.BLOCK_STATES.size());
                for (Entity entity : FILTERED_ENTS) {
                    if (entity == null) continue;
                    if (entity instanceof Player pl && !pl.isConnected()) {
                        ship.ENTITY_COMPOSITION.remove(entity);
                        continue;
                    }
                    Location entityLoc = entity.getLocation();
                    Location rotatedLoc = Rotate2D.ROTATE_VEC(LEFT_RIGHT, entityLoc.subtract(CENTER_POINT)).add(CENTER_POINT).toBlockLocation();
                    rotatedLoc = rotatedLoc.add(0.5,0.0,0.5);
                    rotatedLoc.setPitch(entity.getPitch());
                    rotatedLoc.setYaw(entity.getLocation().getYaw()+rotationF);
                    rotatedLoc.setY(entity.getLocation().getY());
                    if (entity.getVehicle() == null) NmsUtils.teleportSilentCurrentThread(rotatedLoc, entity);
                    if (!ship.CHANGE_WORLD && entity instanceof Player player) {
                        (new org.bukkit.event.player.PlayerMoveEvent(player,rotatedLoc,rotatedLoc)).callEvent();
                    }
                    if (ShipListener.isWheel(entity)) {
                        ShipListener.addTag(entity,ShipListener.wheelTag(entity.getLocation()));
                    }
                }
                ship.model.minY = (ship.BLOCK_STATES.stream().mapToInt(MobileBlockState::by).min().orElse(-129));
                ship.model.maxY = (ship.BLOCK_STATES.stream().mapToInt(MobileBlockState::by).max().orElse(-129));
                if (ship.model.minY < CURRENT_FLUID_LEVEL+15 && !ship.isAirship) {
                    this.currentYVelocity = 0f;
                }
                ship.isLeftPressed = false;
                ship.isRightPressed = false;
                ship.vehicle.setRotation(DirectionalUtils.faceToYaw(ship.facing)+rotationF,0f);
                ship.facing = DirectionalUtils.yawToFace(ship.vehicle.getYaw(),true);
                Vector newForwards = VectorMath.rotY(ship.forwards.clone(),rotationF).multiply(new Vector(-1,1,-1));
                newForwards = newForwards.setX(ROUND_VALUE(newForwards.getX(),2));
                newForwards = newForwards.setZ(ROUND_VALUE(newForwards.getZ(),2));
                ship.forwards = newForwards;
                this.currentSpeed = this.currentSpeed - this.currentSpeed/2f;
                this.currentRotationVelocity = 0.0f;
                this.currentYVelocity = 0f;
                ship.model.minY = ship.minY();
                ship.model.maxY = ship.maxY();
                ship.sendSolids();
                ship.calculateBoundingBox();
                ship.updateRedstone();
                this.currentRotationVelocity = 0.0f;
                com.ccorp2002.smoothsailing.region.TaskUtils.getRegionalScheduler(ship.vehicle.getLocation()).runTaskLater(()->{
                    processFluidRestore();
                    currentYVelocity = yVelocity/3f;
                    currentSpeed = xzVelocity/2f;
                    moving = false;
                    ship.setProcessing(false);
                },(long)(MOVEMENT_COOLDOWN));
            };
            moving = true;
            ship.setProcessing(true);
            if (this.ROTATION_INCR <= 1) {
                ROTATE_SHIP.run();
            } else {
                this.moving = true;
                final double rotationFinal = rotationF / ROTATION_INCR;
                final LocationBlockDataSet PACKET_CHANGES = new LocationBlockDataSet((ship.BLOCK_STATES.size()*ROTATION_INCR)+1000,0.75f);
                for (int incr = 1; incr <= ROTATION_INCR; incr++) {
                    if (incr == ROTATION_INCR) {
                        for (Long key : PACKET_CHANGES.keySet()) {
                            if (key == null) continue;
                            PACKET_CHANGES.put(key,ShipPhysics.VOID);
                        }
                        NmsUtils.sendBlockChanges(ship.getWorld(),PACKET_CHANGES.toMap(ship.getWorld()));
                        com.ccorp2002.smoothsailing.region.TaskUtils.getRegionalScheduler(ship.vehicle.getLocation()).runTaskLater(()->{
                            PACKET_CHANGES.clear();
                            ship.sendVoids();
                            ROTATE_SHIP.run();
                            ship.sendSolids();
                        },(long)(MOVEMENT_COOLDOWN+(ROTATION_INCR*ROTATION_INCR)+(17l*ROTATION_INCR)));
                        break;
                    } else {
                        final int rotationIncr = incr;
                        com.ccorp2002.smoothsailing.region.TaskUtils.getRegionalScheduler(ship.vehicle.getLocation()).runTaskLater(()->{
                            final MobileBlockState[] CLEAN_COPY = ship.BLOCK_STATES.toArray(MobileBlockState[]::new);
                            int collisionCount = 0;
                            com.ccorp2002.smoothsailing.event.ShipRotateEvent mevent = new com.ccorp2002.smoothsailing.event.ShipRotateEvent(ship,newLoc,ship.vehicle,TURN_BLOCK_DATA);
                            mevent.callEvent();
                            if (mevent.isCancelled()) {
                                ship.isLeftPressed = false;
                                ship.isRightPressed = false;
                                ship.isForwardPressed = false;
                                ship.isBackwardPressed = false;
                                ship.isSprintPressed = false;
                                ship.isSpacePressed = false;
                                ship.setProcessing(false);
                                this.currentRotationVelocity = 0.0f;
                                this.currentYVelocity = 0f;
                                this.currentSpeed = 0f;
                                this.moving = false;
                                return;
                            }
                            for (Long key : PACKET_CHANGES.keySet()) {
                                if (key == null) continue;
                                PACKET_CHANGES.set(key,ShipPhysics.VOID);
                            }
                            NmsUtils.sendBlockChanges(ship.getWorld(),PACKET_CHANGES.toMap(ship.getWorld()));
                            for (MobileBlockState st : CLEAN_COPY) {
                                if (st == null) continue;
                                Location stloc = st.getLocation(ship.getWorld());
                                Location updLoc = stloc.clone();
                                Location rotatedLoc = Rotate3D.rotateAroundCenter(updLoc,CENTER_POINT,rotationFinal*rotationIncr,0d,0d).toBlockLocation();
                                PACKET_CHANGES.set(rotatedLoc,st.getBlockData());
                            }
                            NmsUtils.sendBlockChanges(ship.getWorld(),PACKET_CHANGES.toMap(ship.getWorld()));
                       },(long)(MOVEMENT_COOLDOWN+((incr*ROTATION_INCR)+(15l*incr))));

                    }
                }
            }
            return;
        }
        boolean hasHorizontalMovement = (Math.abs(currentSpeed)) >= 0.375f;
        boolean hasVerticalMovement = (Math.abs(currentYVelocity)) >= 0.45f;
        if ((hasHorizontalMovement || hasVerticalMovement) && !(ship.isLeftPressed || ship.isRightPressed) && (ship.isNotProcessing() && !this.moving)) {
            Location newLoc = vehicleLoc.clone();
            double fx = 0d;
            double fy = 0d;
            double fz = 0d;
            boolean toBurnFuel = InventoryUtil.canBurnFuelType(ship,null);
            if (!toBurnFuel) {
                if (ship.isAirship) {
                    if (ship.getGear() > 1) currentYVelocity += -0.75;
                    else currentYVelocity += -0.25;
                }
                if (ship.doGearsRequireFuel()) ship.setGear(1);
            }
            handleBuoyancy(ship.getLocation());
            if (hasHorizontalMovement) {
                fx = (forwardX * (currentSpeed));
                fz = (forwardZ * (currentSpeed));
                if (ship.getGear() > 1) {
                    if (toBurnFuel) {
                        fx *= ship.getGear();
                        fz *= ship.getGear();
                    }
                    fx = (double)CLAMP_VALUE((float)fx, (float)-((PER_MAX_SPEED/1.25)+ship.getGear()+2), (float)((PER_MAX_SPEED/1.25)+ship.getGear()+2));
                    fz = (double)CLAMP_VALUE((float)fz, (float)-((PER_MAX_SPEED/1.25)+ship.getGear()+2), (float)((PER_MAX_SPEED/1.25)+ship.getGear()+2));
                } else {
                    fx = (double)CLAMP_VALUE((float)fx, -2f, 2f);
                    fz = (double)CLAMP_VALUE((float)fz, -2f, 2f);
                }
                if (Math.abs(fx)<1) fx = 0;
                if (Math.abs(fz)<1) fz = 0;
                hasVerticalMovement = (Math.abs(currentYVelocity)) >= 0.45f;
            }
            if (hasVerticalMovement) {
                fy = Math.round(CLAMP_VALUE(currentYVelocity, (float)-((PER_MAX_SPEED/1.25)+2d), (float)((PER_MAX_SPEED/1.25)+2d)));
            }
            if (ship.isSinking) {
                fy = -2d;
            } else if (!ship.isLandcraft && !ship.isAirship) {
                var floating = ship.getDataTag("cemc:float_offset");
                Double floatOffset = 0d;
                if (floating != null && floating instanceof Double ddbl) floatOffset = ddbl;
                boolean isInAir = (getMedium(ShipPhysics.getPrimaryMediumBlockAtBelowY(getExterior(0,0,(int)Math.abs(floatOffset)), CURRENT_FLUID_LEVEL)).id.equalsIgnoreCase("void"));
                if (fy < 0d) {
                    if (ship.model.minY >= CURRENT_FLUID_LEVEL + floatOffset) {
                        fy = -1;
                    } else if (ship.model.minY - floatOffset < CURRENT_FLUID_LEVEL) {
                        fy = 1;
                    } else {
                        if (isInAir) fy = -1;
                        else fy = 0;
                    }
                } else if (fy > 0d) {
                    if (ship.model.minY > CURRENT_FLUID_LEVEL + floatOffset) {
                        if (isInAir) fy = -1;
                        else fy = 0;
                    } else if (ship.model.minY - (floatOffset+2) < CURRENT_FLUID_LEVEL) {
                        fy = 1;
                    } else {
                        if (isInAir) fy = -1;
                        else fy = 0;
                    }
                }
            }
            Location offset = new Location(ship.vehicle.getWorld(),fx,fy,fz);
            com.ccorp2002.smoothsailing.event.ShipMoveEvent mevent = new com.ccorp2002.smoothsailing.event.ShipMoveEvent(ship,offset,ship.vehicle);
            mevent.callEvent();
            if (mevent.isCancelled()) {
                ship.setProcessing(false);
                ship.isLeftPressed = false;
                ship.isRightPressed = false;
                ship.isForwardPressed = false;
                ship.isBackwardPressed = false;
                ship.isSprintPressed = false;
                ship.isSpacePressed = false;
                this.currentRotationVelocity = 0.0f;
                this.currentYVelocity = 0f;
                this.currentSpeed = 0f;
                this.moving = false;
                return;
            }
            offset = mevent.getLocation();
            ship.CHANGE_WORLD = !offset.getWorld().equals(ship.vehicle.getWorld());
            if (ship.CHANGE_WORLD) {
                ship.isLeftPressed = false;
                ship.isRightPressed = false;
                ship.isForwardPressed = false;
                ship.isBackwardPressed = false;
                ship.isSprintPressed = false;
                ship.isSpacePressed = false;
                this.currentRotationVelocity = 0.0f;
                this.currentYVelocity = 0f;
                ship.setProcessing(false);
                this.currentSpeed = 0f;
                this.moving = false;
                return;
            } else {
                com.ccorp2002.smoothsailing.event.ShipPreProcessMoveEvent collideEvent = new com.ccorp2002.smoothsailing.event.ShipPreProcessMoveEvent(ship,offset,ship.vehicle);
                collideEvent.callEvent();
                if (collideEvent.isCancelled()) {
                    offset = collideEvent.getLocation();  // Forcibly alter course
                } else offset = ship.collision.detect(offset);  // Check and update course (figure out)
                ship.CHANGE_WORLD = !offset.getWorld().equals(ship.vehicle.getWorld());
                if (offset.getBlockX() == 0 && offset.getBlockY() == 0 && offset.getBlockZ() == 0 || ship.CHANGE_WORLD) {
                    ship.setInputState(false, false, false, false);
                    ship.setVerticalInputState(false, false);
                    this.currentRotationVelocity = 0.0f;
                    this.currentYVelocity = 0f;
                    ship.setProcessing(false);
                    this.currentSpeed = 0f;
                    this.moving = false;
                    return;
                }
            }
            fx = (offset.getBlockX());
            fy = (offset.getBlockY());
            fz = (offset.getBlockZ());
            double ffx = fx;
            double ffy = fy;
            double ffz = fz;
            final Location offsetf = new Location(offset.getWorld(),ffx,ffy,ffz);
            if (offsetf.getBlockX() == 0 && offsetf.getBlockY() == 0 && offsetf.getBlockZ() == 0 || ship.CHANGE_WORLD) {
                ship.setInputState(false, false, false, false);
                ship.setVerticalInputState(false, false);
                this.currentRotationVelocity = 0.0f;
                this.currentYVelocity = 0f;
                ship.setProcessing(false);
                this.currentSpeed = 0f;
                this.moving = false;
                return;
            }
            int collisionCount = 0;
            final MobileBlockState[] TEMPS = ship.BLOCK_STATES.toArray(MobileBlockState[]::new);
            if (ship.model.minY < CURRENT_FLUID_LEVEL+15 && !ship.isAirship) {
                this.currentYVelocity = 0f;
            }
            for (MobileBlockState st : TEMPS) {
                if (st == null) continue;
                Location stloc = st.getLocation(offsetf.getWorld());
                Location updLoc = stloc.clone();
                updLoc.setWorld(offsetf.getWorld());
                updLoc = updLoc.add(offsetf);
                updLoc = updLoc.toBlockLocation();
                boolean isAccessible = updLoc.isChunkLoaded() && updLoc.isGenerated();
                if (!isAccessible) {
                    NmsUtils.getChunkFastest(updLoc);
                    isAccessible = updLoc.isChunkLoaded() && updLoc.isGenerated();
                }
                if (!isAccessible) {
                    NmsUtils.getChunkFastest(updLoc);
                    isAccessible = updLoc.isChunkLoaded() && updLoc.isGenerated();
                    if (SmoothSailingPlugin.DEBUG_DUMP) {
                        System.out.println("MOVING STATE?: "+st.getBlockData().getMaterial().toString()+" TO POSITION?: "+updLoc.toVector().toString());
                        System.out.println("POSITION IS ACCESSIBLE?: "+isAccessible);
                    }
                    collisionCount++;
                    continue;
                }
                Block block = updLoc.getBlock();
                if (DESTRUCTIBLE.contains(block.getType()) && !MobileBlockState.doesShipHave(ship,updLoc) && !FRAGILE_MATERIALS.contains(block.getType())) {
                    block.breakNaturally(true);
                }
                if (MobileBlockState.doesShipHave(ship,updLoc)) {
                    continue;
                }
                if (block.isSolid() && !isDataMedium(block) && !(LANDCRAFT_PASSTHROUGH.contains(block.getType()) && ship.isLandcraft)) {
                    if (SmoothSailingPlugin.DEBUG_DUMP) System.out.println("COLLIDED?: "+!MobileBlockState.doesShipHave(ship,updLoc)+" LOCATION?: "+updLoc.toVector().toString()+"\nBLOCKDATA?: >"+block.getBlockData().getAsString(false));
                    collisionCount++;
                }
            }
            if (collisionCount > 0 || ship.CHANGE_WORLD) {
                if (SmoothSailingPlugin.DEBUG_DUMP) System.out.println("MOVEMENT COLLISION COUNT: "+collisionCount);
                ship.setInputState(false, false, false, false);
                ship.setVerticalInputState(false, false);
                this.currentRotationVelocity = 0.0f;
                this.currentYVelocity = 0f;
                ship.setProcessing(false);
                this.currentSpeed = 0f;
                this.moving = false;
                return;
            }
            if (SmoothSailingPlugin.DEBUG_DUMP) System.out.println("MOVEMENT VECTOR?: "+offsetf.toVector().toString());
            ship.setProcessing(true);
            this.moving = true;
            //Runnable runMe = () -> {
            detectNearbyEntities();
            //boolean isShipBobbingUp = offsetf.getBlockY() > 0 && !ship.isAirship;
            for (int indx = 0; indx < TEMPS.length; indx ++) {
                MobileBlockState st = TEMPS[indx];
                if (st == null) continue;
                final Location stloc = st.getLocation(ship.getWorld());
                st.tile = NmsUtils.removeAndCreateTileHolder(stloc);
                NmsUtils.setBlockFast(stloc,st.occupiedBlock);
                TEMPS[indx] = st;
            }
            ship.sendVoids(Arrays.asList(TEMPS));
            for (int indx = 0; indx < TEMPS.length; indx ++) {
                MobileBlockState st = TEMPS[indx];
                if (st == null) continue;
                final Location stloc = st.getLocation(ship.getWorld());
                Location updLoc = stloc.clone();
                updLoc.setWorld(offsetf.getWorld());
                updLoc = updLoc.add(offsetf);
                updLoc = updLoc.toBlockLocation();
                var TEMP_WHEEL = SmoothSailingPlugin.INSTANCE.shipWheelManager.getWheelAt(stloc);
                if (TEMP_WHEEL != null && SmoothSailingPlugin.INSTANCE.shipWheelManager != null) {
                    SmoothSailingPlugin.INSTANCE.shipWheelManager.updateWheelLocation(TEMP_WHEEL, updLoc, ship.facing);
                }
                st.occupiedBlock = NmsUtils.toBukkitBlockDataFast(updLoc);
                ship.FLUIDS.set(updLoc,st.occupiedBlock);
                
                st.teleport(updLoc,false);
                st.setLocation(updLoc);
                TEMPS[indx] = st;
            }
            //MobileBlockState[] FILTERED_TEMPS = Arrays.stream(TEMPS).filter(Objects::nonNull).filter(temp -> temp.getBlockData().getMaterial().isSolid()).toArray(MobileBlockState[]::new);
            //System.out.println("FILTERED BLOCK COUNT: "+FILTERED_TEMPS.length);
            ship.BLOCK_STATES.clear();
            ship.BLOCK_STATES.setAll(TEMPS);
            //System.out.println("POST MOVEMENT BLOCK COUNT: "+ship.BLOCK_STATES.size());
            ship.model.minY = ship.minY();
            ship.model.maxY = ship.maxY();
            for (Entity entity : FILTERED_ENTS) {
                if (entity == null) continue;
                if (entity instanceof Player pl && !pl.isConnected()) continue;
                Location entLoc = entity.getLocation().clone();
                //if (SmoothSailingPlugin.DEBUG_DUMP && entity.isEmpty()) System.out.println("DEBUG ENTITY Y0: "+entLoc.getY());
                entLoc.setWorld(offsetf.getWorld());
                entLoc = entLoc.add(offsetf);
                //if (SmoothSailingPlugin.DEBUG_DUMP && entity.isEmpty()) System.out.println("DEBUG ENTITY Y1: "+entLoc.getY());
                if (entity instanceof Player player) {
                    entLoc.setYaw(player.getEyeLocation().getYaw());
                    entLoc.setPitch(player.getEyeLocation().getPitch());
                    if (!ship.CHANGE_WORLD) (new org.bukkit.event.player.PlayerMoveEvent(player,entLoc,entLoc)).callEvent();
                } else {
                    entLoc.setYaw(entity.getYaw());
                    entLoc.setPitch(entity.getPitch());
                }
                if (entity.getVehicle() == null) NmsUtils.teleportSilentCurrentThread(entLoc, entity);
                if (ShipListener.isWheel(entity)) {
                    ShipListener.addTag(entity,ShipListener.wheelTag(entity.getLocation()));
                }
                //if (SmoothSailingPlugin.DEBUG_DUMP && entity.isEmpty()) System.out.println("DEBUG ENTITY Y2: "+entity.getLocation().getY());
            }
            ship.sendSolids();
            ship.calculateBoundingBox();
            ship.updateRedstone();
            this.currentRotationVelocity = 0.0f;
            if (toBurnFuel) {
                int burnRate = (int)(ship.getFuelCost()+(getEngineGears()/getEnginePercent()));
                InventoryUtil.doBurnFuelType(ship,null,false,burnRate);
            }
            com.ccorp2002.smoothsailing.region.TaskUtils.getRegionalScheduler(ship.vehicle.getLocation()).runTaskLater(()->{
                processFluidRestore();
                moving = false;
                ship.setProcessing(false);
            },(long)(MOVEMENT_COOLDOWN));
            return;
        }
    }
    public void rotateSegment3AxisIncr(Vector axis, double amount) {
        rotateSegment3AxisIncr(axis,null,amount);
    }
    public void rotateSegment3AxisIncr(Vector axis, Player pilot, double amount) {
        DO$rotateSegment3AxisIncr(axis,pilot,amount);
    }

    public void DO$rotateSegment3AxisIncr(Vector axis, Player pilot, double amount) {
        if ((ship.isNotProcessing() && !this.moving) && !ship.CHANGE_WORLD) {
            // Apply 3d rotation
            int collisionCount = 0;
            Location newLoc = ship.vehicle.getLocation();
            Location CENTER_POINT = newLoc.clone().toBlockLocation();
            ship.setProcessing(true);
            this.moving = true;
            final MobileBlockState[] TEMPS = ship.BLOCK_STATES.toArray(MobileBlockState[]::new);
            for (MobileBlockState st : TEMPS) {
                if (st == null) continue;
                Location stloc = st.getLocation(ship.getWorld());
                Location updLoc = stloc.clone();
                updLoc = LocationLib.rotate(updLoc,CENTER_POINT,axis,amount);
                updLoc = updLoc.toBlockLocation();
                updLoc.setWorld(newLoc.getWorld());
                boolean isAccessible = updLoc.isChunkLoaded() && updLoc.isGenerated();
                if (SmoothSailingPlugin.DEBUG_DUMP) {
                    System.out.println("3D ROTATING STATE?: "+st.getBlockData().getMaterial().toString()+" TO POSITION?: "+updLoc.toVector().toString());
                    System.out.println("POSITION IS ACCESSIBLE?: "+isAccessible);
                }
                if (!isAccessible) {
                    NmsUtils.getChunkFastest(updLoc);
                    isAccessible = updLoc.isChunkLoaded() && updLoc.isGenerated();
                }
                if (!isAccessible) {
                    if (SmoothSailingPlugin.DEBUG_DUMP) System.out.println("POSITION IS NOT ACCESSIBLE: "+updLoc.toVector().toString());
                    collisionCount++;
                    continue;
                }
                Block block = updLoc.getBlock();
                if (DESTRUCTIBLE.contains(block.getType()) && !MobileBlockState.doesShipHave(ship,updLoc) && !FRAGILE_MATERIALS.contains(block.getType())) {
                    if (SmoothSailingPlugin.DEBUG_DUMP) System.out.println("COLLIDED & BROKE?: "+block.getType()+" LOCATION?: "+updLoc.toVector().toString());
                    block.breakNaturally(true);
                }
                if (MobileBlockState.doesShipHave(ship,updLoc)) {
                    continue;
                }
                if (block.isSolid() && !isDataMedium(block) && !(LANDCRAFT_PASSTHROUGH.contains(block.getType()) && ship.isLandcraft)) {
                    if (SmoothSailingPlugin.DEBUG_DUMP) System.out.println("COLLIDED?: "+!MobileBlockState.doesShipHave(ship,updLoc)+" LOCATION?: "+updLoc.toVector().toString()+"\nBLOCKDATA?: >"+block.getBlockData().getAsString(false));
                    collisionCount++;
                }
            }
            detectNearbyEntities();
            List<Entity> FILTERED_ENTS = ship.ENTITY_COMPOSITION.stream()
                    .filter(p -> p.isValid() && !ship.vehicle.equals(p) && (p.getWorld().equals(CENTER_POINT.getWorld())))
                    .collect(Collectors.toList());
            if (collisionCount > 0 || ship.CHANGE_WORLD) {
                if (SmoothSailingPlugin.DEBUG_DUMP) System.out.println("3D ROTATING COLLISION COUNT: "+collisionCount);
                ship.setInputState(false, false, false, false);
                ship.setVerticalInputState(false, false);
                this.currentRotationVelocity = 0.0f;
                this.currentYVelocity = 0f;
                ship.setProcessing(false);
                this.currentSpeed = 0f;
                this.moving = false;
                return;
            }
            if (SmoothSailingPlugin.DEBUG_DUMP) System.out.println("3D ROTATING VECTOR?: "+axis.toString());
            ship.setProcessing(true);
            this.moving = true;
            //Runnable runMe = () -> {
            detectNearbyEntities();
            //System.out.println("STARTING BLOCK COUNT: "+TEMPS.length);
            for (int indx = 0; indx < TEMPS.length; indx ++) {
                MobileBlockState st = TEMPS[indx];
                if (st == null) continue;
                final Location stloc = st.getLocation(ship.getWorld());
                st.tile = NmsUtils.createTileHolder(stloc);
                NmsUtils.setBlockFast(stloc,st.occupiedBlock);
                TEMPS[indx] = st;
            }
            ship.sendVoids(Arrays.asList(TEMPS));
            //System.out.println("MID MOVEMENT BLOCK COUNT: "+TEMPS.length);
            for (int indx = 0; indx < TEMPS.length; indx ++) {
                MobileBlockState st = TEMPS[indx];
                if (st == null) continue;
                final Location stloc = st.getLocation(ship.getWorld());
                Location updLoc = stloc.clone();
                updLoc = LocationLib.rotate(updLoc,CENTER_POINT,axis,amount);
                updLoc = updLoc.toBlockLocation();
                updLoc.setWorld(newLoc.getWorld());
                var TEMP_WHEEL = SmoothSailingPlugin.INSTANCE.shipWheelManager.getWheelAt(stloc);
                if (TEMP_WHEEL != null && SmoothSailingPlugin.INSTANCE.shipWheelManager != null) {
                    SmoothSailingPlugin.INSTANCE.shipWheelManager.updateWheelLocation(TEMP_WHEEL, updLoc, ship.facing);
                }
                st.occupiedBlock = NmsUtils.toBukkitBlockDataFast(updLoc);
                ship.FLUIDS.set(updLoc,st.occupiedBlock);
                st.teleport(updLoc,false);
                st.setLocation(updLoc);
                TEMPS[indx] = st;
            }
            //MobileBlockState[] FILTERED_TEMPS = Arrays.stream(TEMPS).filter(Objects::nonNull).filter(temp -> temp.getBlockData().getMaterial().isSolid()).toArray(MobileBlockState[]::new);
            //System.out.println("FILTERED BLOCK COUNT: "+FILTERED_TEMPS.length);
            ship.BLOCK_STATES.clear();
            ship.BLOCK_STATES.setAll(TEMPS);
            //System.out.println("POST MOVEMENT BLOCK COUNT: "+ship.BLOCK_STATES.size());
            ship.model.minY = ship.minY();
            ship.model.maxY = ship.maxY();
            for (Entity entity : FILTERED_ENTS) {
                if (entity == null) continue;
                if (entity instanceof Player pl && !pl.isConnected()) continue;
                Location entLoc = entity.getLocation().clone();
                //if (SmoothSailingPlugin.DEBUG_DUMP && entity.isEmpty()) System.out.println("DEBUG ENTITY Y0: "+entLoc.getY());
                entLoc = LocationLib.rotate(entLoc,CENTER_POINT,axis,amount);
                entLoc.setWorld(newLoc.getWorld());
                //if (SmoothSailingPlugin.DEBUG_DUMP && entity.isEmpty()) System.out.println("DEBUG ENTITY Y1: "+entLoc.getY());
                if (entity instanceof Player player) {
                    entLoc.setYaw(player.getEyeLocation().getYaw());
                    entLoc.setPitch(player.getEyeLocation().getPitch());
                    if (!ship.CHANGE_WORLD) (new org.bukkit.event.player.PlayerMoveEvent(player,entLoc,entLoc)).callEvent();
                } else {
                    entLoc.setYaw(entity.getYaw());
                    entLoc.setPitch(entity.getPitch());
                }
                if (entity.getVehicle() == null) NmsUtils.teleportSilentCurrentThread(entLoc, entity);
                if (ShipListener.isWheel(entity)) {
                    ShipListener.addTag(entity,ShipListener.wheelTag(entity.getLocation()));
                }
                //if (SmoothSailingPlugin.DEBUG_DUMP && entity.isEmpty()) System.out.println("DEBUG ENTITY Y2: "+entity.getLocation().getY());
            }
            ship.sendSolids();
            ship.calculateBoundingBox();
            com.ccorp2002.smoothsailing.region.TaskUtils.getRegionalScheduler(ship.vehicle.getLocation()).runTaskLater(()->{
                processFluidRestore();
                moving = false;
                ship.setProcessing(false);
            },(long)(MOVEMENT_COOLDOWN)+5L);
        }
    }

    public void rotateSegment3Axis(Location CENTER_POINT, String LEFT_RIGHT, double yawDeg, double pitchDeg, double rollDeg, Collection<Block> BLOCKS) {
        if (LEFT_RIGHT == null || LEFT_RIGHT.isEmpty()) {
            LEFT_RIGHT = "NONE";
        }
        float rotation = 0.0f;
        final org.bukkit.block.structure.StructureRotation TURN_BLOCK_DATA;
        if (LEFT_RIGHT.equalsIgnoreCase("LEFT")) {rotation = -90.0f; TURN_BLOCK_DATA = org.bukkit.block.structure.StructureRotation.COUNTERCLOCKWISE_90; LEFT_RIGHT = "LEFT";}
        else if (LEFT_RIGHT.equalsIgnoreCase("RIGHT")) {rotation = 90.0f; TURN_BLOCK_DATA = org.bukkit.block.structure.StructureRotation.CLOCKWISE_90; LEFT_RIGHT = "RIGHT";}
        else {TURN_BLOCK_DATA = org.bukkit.block.structure.StructureRotation.NONE;}
        final float initYaw = 0.0f;
        final float rotationF = rotation;
        if (BLOCKS.isEmpty() || BLOCKS.size()<=0) return;
        Set<MobileBlockState> MOVE_STATES = new HashSet<>(BLOCKS.size()+5);
        for (Block block : BLOCKS) {
            MOVE_STATES.add(MobileBlockState.getStateFromLoc(block.getLocation()));
        }
        MOVE_STATES.add(MobileBlockState.getStateFromLoc(CENTER_POINT.toBlockLocation()));
        Player pilot = null;
        int collisionCount = 0;
        boolean canEnterAt = true;
        if (ship.driver instanceof Player player) pilot = player;
        detectNearbyEntities();
        List<Entity> FILTERED_ENTS = ship.ENTITY_COMPOSITION.stream()
                .filter(p -> p.isValid() && !ship.vehicle.equals(p) && (p.getWorld().equals(CENTER_POINT.getWorld()) && p.getLocation().distance(CENTER_POINT) <= 7.5d))
                .collect(Collectors.toList());
        final MobileBlockState[] TEMPS = MOVE_STATES.toArray(MobileBlockState[]::new);
        for (MobileBlockState st : TEMPS) {
            if (st == null) continue;
            Location stloc = st.getLocation(ship.getWorld());
            Location updLoc = stloc.clone();
            updLoc.setYaw(initYaw);
            Location rotatedLoc = Rotate3D.rotateAroundCenter(updLoc,CENTER_POINT,yawDeg,pitchDeg,rollDeg); 
            rotatedLoc.setYaw(0.0f);
            if (pilot != null) {
                for (final com.ccorp2002.smoothsailing.utils.claims.IClaimInterface ICLAIM : SmoothSailingPlugin.INSTANCE.CLAIM_INTEFACES) {
                    if (!canEnterAt) break;
                    if (!ICLAIM.canEnterAt(pilot,rotatedLoc)) canEnterAt = false;
                    if (!NmsUtils.withinWorldBorder(rotatedLoc)) canEnterAt = false;
                    
                }
            }
            if (!canEnterAt) {
                collisionCount++;
                if (SmoothSailingPlugin.DEBUG_DUMP) System.out.println("SEGMENT-ROTATION COLLISION COUNT: "+collisionCount);
                ship.isLeftPressed = false;
                ship.isRightPressed = false;
                ship.isForwardPressed = false;
                ship.isBackwardPressed = false;
                ship.isSprintPressed = false;
                ship.isSpacePressed = false;
                this.currentRotationVelocity = 0.0f;
                this.currentYVelocity = 0f;
                this.currentSpeed = 0f;
                this.moving = false;
                return;
            }
            Block block = rotatedLoc.getBlock();
            if (DESTRUCTIBLE.contains(block.getType()) && !MobileBlockState.doesShipHave(ship,rotatedLoc) && !FRAGILE_MATERIALS.contains(block.getType())) {
                if (SmoothSailingPlugin.DEBUG_DUMP) System.out.println("SEGMENT-COLLIDED & BROKE?: "+block.getType()+" LOCATION?: "+rotatedLoc.toVector().toString());
                block.breakNaturally(true);
                continue;
            }
            if (MobileBlockState.doesShipHave(ship,rotatedLoc)) {
                continue;
            }
            if (block.isSolid() && !isDataMedium(block) && !(LANDCRAFT_PASSTHROUGH.contains(block.getType()) && ship.isLandcraft)) {
                if (SmoothSailingPlugin.DEBUG_DUMP) System.out.println("SEGMENT-COLLIDED?: "+!MobileBlockState.doesShipHave(ship,rotatedLoc)+" LOCATION?: "+rotatedLoc.toVector().toString()+" BLOCKDATA?:\n >"+block.getBlockData().getAsString(true));
                collisionCount++;
            }
        }
        if (collisionCount > 0) {
            if (SmoothSailingPlugin.DEBUG_DUMP) System.out.println("SEGMENT-ROTATION COLLISION COUNT: "+collisionCount);
            ship.isLeftPressed = false;
            ship.isRightPressed = false;
            ship.isForwardPressed = false;
            ship.isBackwardPressed = false;
            ship.isSprintPressed = false;
            ship.isSpacePressed = false;
            this.currentRotationVelocity = 0.0f;
            this.currentYVelocity = 0f;
            this.currentSpeed = 0f;
            this.moving = false;
            return;
        }
        //NmsUtils.refreshChunksIn(ship.vehicle.getWorld(),TEMPS);
        ship.setProcessing(true);
        this.moving = true;
        detectNearbyEntities();
        for (int indx = 0; indx < TEMPS.length; indx ++) {
            MobileBlockState st = TEMPS[indx];
            ship.BLOCK_STATES.remove(st);
            if (st == null) continue;
            final Location stloc = st.getLocation(ship.getWorld());
            st.tile = NmsUtils.createTileHolder(stloc);
            NmsUtils.setBlockFast(stloc,st.occupiedBlock);
            TEMPS[indx] = st;
        }
        ship.sendVoids(Arrays.asList(TEMPS));
        if (SmoothSailingPlugin.CANNONS_FOUND) {
            CannonsUtils.rotateCannons(CENTER_POINT,TURN_BLOCK_DATA,Arrays.asList(TEMPS));
        }
        for (int indx = 0; indx < TEMPS.length; indx ++) {
            MobileBlockState st = TEMPS[indx];
            if (st == null) continue;
            final Location stloc = st.getLocation(ship.getWorld());
            Location updLoc = stloc.clone();
            updLoc = Rotate3D.rotateAroundCenter(updLoc,CENTER_POINT,yawDeg,pitchDeg,rollDeg); 
            //Rotate2D.AddRotationBlock((double)rotationF, CENTER_POINT, origLoc);
            updLoc.setYaw(0.0f);
            
            st.occupiedBlock = NmsUtils.toBukkitBlockDataFast(updLoc);
            BlockData TURNED = st.getBlockData().clone();
            TURNED = NmsUtils.rotateBlockData(LEFT_RIGHT,TURNED);
            st.setBlockData(TURNED);
            if (SmoothSailingPlugin.DEBUG_DUMP) {
                System.out.println(st.getBlockData().getAsString(false)+":  ORIGIN: "+stloc.toVector().toString()+" ");
                System.out.println("  SEGMENT-ROTATED: "+updLoc.toVector().toString()+" ");
                System.out.println("  SEGMENT-ROTATION TYPE: "+LEFT_RIGHT);
                System.out.println("  SEGMENT-NEW BLOCK DATA: "+NmsUtils.BlockDataGetter.get(NmsUtils.getBlockState(TURNED)).getAsString(false));
            }
            ship.FLUIDS.set(updLoc,st.occupiedBlock);
            st.teleport(updLoc,false);
            st.setLocation(updLoc);
            TEMPS[indx] = st;
            //ship.BLOCK_STATES.add(st);
        }
        ship.BLOCK_STATES.addAll(TEMPS);
        ship.updateRedstone(Arrays.asList(TEMPS));
        ship.calculateBoundingBox();
        ship.sendSolids();
        for (Entity entity : FILTERED_ENTS) {
            if (entity == null) continue;
            if (entity instanceof Player pl && !pl.isConnected()) {
                ship.ENTITY_COMPOSITION.remove(entity);
                continue;
            }
            Location ENTITY_POS = entity.getLocation().clone();
            Location rotatedLoc = Rotate3D.ROTATE_VEC(ENTITY_POS.subtract(CENTER_POINT),yawDeg,pitchDeg,rollDeg).toBlockLocation().add(CENTER_POINT);
            rotatedLoc = rotatedLoc.add(0.5,0.0,0.5);
            rotatedLoc.setPitch(entity.getPitch());
            rotatedLoc.setYaw(entity.getLocation().getYaw()+rotationF);
            rotatedLoc.setY(entity.getLocation().getY());
            if (entity.getVehicle() == null) NmsUtils.teleportSilentCurrentThread(rotatedLoc, entity);
            if (!ship.CHANGE_WORLD && entity instanceof Player player) {
                (new org.bukkit.event.player.PlayerMoveEvent(player,rotatedLoc,rotatedLoc)).callEvent();
            }
            if (ShipListener.isWheel(entity)) {
                ShipListener.addTag(entity,ShipListener.wheelTag(entity.getLocation()));
            }
        }
        com.ccorp2002.smoothsailing.region.TaskUtils.getRegionalScheduler(ship.vehicle.getLocation()).runTaskLater(()->{
            processFluidRestore();
            moving = false;
            ship.setProcessing(false);
        },(long)(MOVEMENT_COOLDOWN)+5L);
    }

    public void rotateSegment2d(Location CENTER_POINT, String LEFT_RIGHT, Collection<Block> BLOCKS) {
        if (LEFT_RIGHT == null || LEFT_RIGHT.isEmpty()) {
            LEFT_RIGHT = "NONE";
        }
        float rotation = 0.0f;
        final org.bukkit.block.structure.StructureRotation TURN_BLOCK_DATA;
        if (LEFT_RIGHT.equalsIgnoreCase("LEFT")) {rotation = -90.0f; TURN_BLOCK_DATA = org.bukkit.block.structure.StructureRotation.COUNTERCLOCKWISE_90; LEFT_RIGHT = "LEFT";}
        else if (LEFT_RIGHT.equalsIgnoreCase("RIGHT")) {rotation = 90.0f; TURN_BLOCK_DATA = org.bukkit.block.structure.StructureRotation.CLOCKWISE_90; LEFT_RIGHT = "RIGHT";}
        else {TURN_BLOCK_DATA = org.bukkit.block.structure.StructureRotation.NONE;}
        final float initYaw = 0.0f;
        final float rotationF = rotation;
        if (LEFT_RIGHT.equalsIgnoreCase("NONE") || BLOCKS.isEmpty() || BLOCKS.size()<=0) return;
        Set<MobileBlockState> MOVE_STATES = new HashSet<>(BLOCKS.size()+5);
        for (Block block : BLOCKS) {
            MOVE_STATES.add(MobileBlockState.getStateFromLoc(block.getLocation()));
        }
        MOVE_STATES.add(MobileBlockState.getStateFromLoc(CENTER_POINT.toBlockLocation()));
        Player pilot = null;
        int collisionCount = 0;
        boolean canEnterAt = true;
        if (ship.driver instanceof Player player) pilot = player;
        List<Entity> FILTERED_ENTS = ship.ENTITY_COMPOSITION.stream()
                .filter(p -> p.isValid() && !ship.vehicle.equals(p) && (p.getWorld().equals(CENTER_POINT.getWorld()) && p.getLocation().distance(CENTER_POINT) <= 7.5d))
                .collect(Collectors.toList());
        

        final MobileBlockState[] TEMPS = MOVE_STATES.toArray(MobileBlockState[]::new);
        for (MobileBlockState st : TEMPS) {
            if (st == null) continue;
            Location stloc = st.getLocation(ship.getWorld());
            Location updLoc = stloc.clone();
            updLoc.setYaw(initYaw);
            Location rotatedLoc = Rotate2D.ROTATE_VEC(LEFT_RIGHT, updLoc.subtract(CENTER_POINT)).add(CENTER_POINT); 
            rotatedLoc.setYaw(0.0f);
            if (pilot != null) {
                for (final com.ccorp2002.smoothsailing.utils.claims.IClaimInterface ICLAIM : SmoothSailingPlugin.INSTANCE.CLAIM_INTEFACES) {
                    if (!canEnterAt) break;
                    if (!ICLAIM.canEnterAt(pilot,rotatedLoc)) canEnterAt = false;
                    if (!NmsUtils.withinWorldBorder(rotatedLoc)) canEnterAt = false;
                    
                }
            }
            if (!canEnterAt) {
                collisionCount++;
                if (SmoothSailingPlugin.DEBUG_DUMP) System.out.println("SEGMENT-ROTATION COLLISION COUNT: "+collisionCount);
                ship.isLeftPressed = false;
                ship.isRightPressed = false;
                ship.isForwardPressed = false;
                ship.isBackwardPressed = false;
                ship.isSprintPressed = false;
                ship.isSpacePressed = false;
                this.currentRotationVelocity = 0.0f;
                this.currentYVelocity = 0f;
                this.currentSpeed = 0f;
                this.moving = false;
                return;
            }
            Block block = rotatedLoc.getBlock();
            if (DESTRUCTIBLE.contains(block.getType()) && !MobileBlockState.doesShipHave(ship,rotatedLoc) && !FRAGILE_MATERIALS.contains(block.getType())) {
                if (SmoothSailingPlugin.DEBUG_DUMP) System.out.println("SEGMENT-COLLIDED & BROKE?: "+block.getType()+" LOCATION?: "+rotatedLoc.toVector().toString());
                block.breakNaturally(true);
                continue;
            }
            if (MobileBlockState.doesShipHave(ship,rotatedLoc)) {
                continue;
            }
            if (block.isSolid() && !isDataMedium(block) && !(LANDCRAFT_PASSTHROUGH.contains(block.getType()) && ship.isLandcraft)) {
                if (SmoothSailingPlugin.DEBUG_DUMP) System.out.println("SEGMENT-COLLIDED?: "+!MobileBlockState.doesShipHave(ship,rotatedLoc)+" LOCATION?: "+rotatedLoc.toVector().toString()+" BLOCKDATA?:\n >"+block.getBlockData().getAsString(true));
                collisionCount++;
            }
        }
        if (collisionCount > 0) {
            if (SmoothSailingPlugin.DEBUG_DUMP) System.out.println("SEGMENT-ROTATION COLLISION COUNT: "+collisionCount);
            ship.isLeftPressed = false;
            ship.isRightPressed = false;
            ship.isForwardPressed = false;
            ship.isBackwardPressed = false;
            ship.isSprintPressed = false;
            ship.isSpacePressed = false;
            this.currentRotationVelocity = 0.0f;
            this.currentYVelocity = 0f;
            this.currentSpeed = 0f;
            this.moving = false;
            return;
        }
        //NmsUtils.refreshChunksIn(ship.vehicle.getWorld(),TEMPS);
        ship.setProcessing(true);
        this.moving = true;
        detectNearbyEntities();
        for (int indx = 0; indx < TEMPS.length; indx ++) {
            MobileBlockState st = TEMPS[indx];
            ship.BLOCK_STATES.remove(st);
            if (st == null) continue;
            final Location stloc = st.getLocation(ship.getWorld());
            st.tile = NmsUtils.removeAndCreateTileHolder(stloc);
            NmsUtils.setBlockFast(stloc,st.occupiedBlock);
        }
        ship.sendVoids(Arrays.asList(TEMPS));
        if (SmoothSailingPlugin.CANNONS_FOUND) {
            CannonsUtils.rotateCannons(CENTER_POINT,TURN_BLOCK_DATA,Arrays.asList(TEMPS));
        }
        for (int indx = 0; indx < TEMPS.length; indx ++) {
            MobileBlockState st = TEMPS[indx];
            if (st == null) continue;
            final Location stloc = st.getLocation(ship.getWorld());
            Location updLoc = stloc.clone();
            updLoc = Rotate2D.ROTATE_VEC(LEFT_RIGHT, stloc.subtract(CENTER_POINT)).add(CENTER_POINT); 
            //Rotate2D.AddRotationBlock((double)rotationF, CENTER_POINT, origLoc);
            updLoc.setYaw(0.0f);
            
            st.occupiedBlock = NmsUtils.toBukkitBlockDataFast(updLoc);
            BlockData TURNED = st.getBlockData().clone();
            TURNED = NmsUtils.rotateBlockData(LEFT_RIGHT,TURNED);
            st.setBlockData(TURNED);
            if (SmoothSailingPlugin.DEBUG_DUMP) {
                System.out.println(st.getBlockData().getAsString(false)+":  ORIGIN: "+stloc.toVector().toString()+" ");
                System.out.println("  SEGMENT-ROTATED: "+updLoc.toVector().toString()+" ");
                System.out.println("  SEGMENT-ROTATION TYPE: "+LEFT_RIGHT);
                System.out.println("  SEGMENT-NEW BLOCK DATA: "+NmsUtils.BlockDataGetter.get(NmsUtils.getBlockState(TURNED)).getAsString(false));
            }
            st.teleport(updLoc,false);
            ship.FLUIDS.set(updLoc,st.occupiedBlock);
            st.setLocation(updLoc);
            TEMPS[indx] = st;
        }
        ship.BLOCK_STATES.addAll(TEMPS);
        ship.sendSolids();
        ship.calculateBoundingBox();
        ship.updateRedstone(Arrays.asList(TEMPS));
        for (Entity entity : FILTERED_ENTS) {
            if (entity == null) continue;
            if (entity instanceof Player pl && !pl.isConnected()) {
                ship.ENTITY_COMPOSITION.remove(entity);
                continue;
            }
            Location entityLoc = entity.getLocation();
            Location rotatedLoc = Rotate2D.ROTATE_VEC(LEFT_RIGHT, entityLoc.subtract(CENTER_POINT)).add(CENTER_POINT).toBlockLocation();
            rotatedLoc = rotatedLoc.add(0.5,0.0,0.5);
            rotatedLoc.setPitch(entity.getPitch());
            rotatedLoc.setYaw(entity.getLocation().getYaw()+rotationF);
            rotatedLoc.setY(entity.getLocation().getY());
            if (entity.getVehicle() == null) NmsUtils.teleportSilentCurrentThread(rotatedLoc, entity);
            if (!ship.CHANGE_WORLD && entity instanceof Player player) {
                (new org.bukkit.event.player.PlayerMoveEvent(player,rotatedLoc,rotatedLoc)).callEvent();
            }
            if (ShipListener.isWheel(entity)) {
                ShipListener.addTag(entity,ShipListener.wheelTag(entity.getLocation()));
            }
        }
        com.ccorp2002.smoothsailing.region.TaskUtils.getRegionalScheduler(ship.vehicle.getLocation()).runTaskLater(()->{
            processFluidRestore();
            moving = false;
            ship.setProcessing(false);
        },(long)(MOVEMENT_COOLDOWN)+5L);
    }

    public Set<Location> getInteriorLocs() {
        World world = ship.getWorld();
        if (world == null) return new HashSet<Location>();
        Set<Location> outsideBlocks = new it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet<>(ship.BLOCK_STATES.size(),0.75f,MobileBlockState.HASH_STRAT);
        Set<Location> outerHull = new it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet<>(ship.BLOCK_STATES.size(),0.75f,MobileBlockState.HASH_STRAT);
        Set<Location> innerShip = new it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet<>(ship.BLOCK_STATES.size(),0.75f,MobileBlockState.HASH_STRAT);
        scanHullLocations(outsideBlocks, outerHull, innerShip);
        return innerShip;
    }


    public Set<Block> getInterior() {
        World world = ship.getWorld();
        if (world == null) return new HashSet<Block>();
        Set<Block> outsideBlocks = new it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet<>(ship.BLOCK_STATES.size(),0.75f,MobileBlockState.HASH_STRAT);
        Set<Block> outerHull = new it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet<>(ship.BLOCK_STATES.size(),0.75f,MobileBlockState.HASH_STRAT);
        Set<Block> innerShip = new it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet<>(ship.BLOCK_STATES.size(),0.75f,MobileBlockState.HASH_STRAT);
        scanHull(outsideBlocks, outerHull, innerShip, 0, 0, 0);
        return innerShip;
    }

    public Set<Block> getExterior() {
        return getExterior(0, 0, 0);
    }

    public Set<Block> getExterior(Integer houtset, Integer voutset) {
        return getExterior(houtset,voutset,0);
    }

    public Set<Block> getExterior(Integer houtset, Integer voutset, Integer scanDown) {
        World world = ship.getWorld();
        if (world == null) return new HashSet<Block>();
        Set<Block> outsideBlocks = new it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet<>(ship.BLOCK_STATES.size(),0.75f,MobileBlockState.HASH_STRAT);
        Set<Block> outerHull = new it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet<>(ship.BLOCK_STATES.size(),0.75f,MobileBlockState.HASH_STRAT);
        Set<Block> innerShip = new it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet<>(ship.BLOCK_STATES.size(),0.75f,MobileBlockState.HASH_STRAT);
        scanHull(outsideBlocks, outerHull, innerShip, houtset, voutset, scanDown);
        return outsideBlocks;
    }

    public boolean isUnderwater() {
        return isUnderwater(true);
    }

    public boolean isUnderwater(boolean truly) {
        World world = ship.getWorld();
        if (world == null) return false;
        Set<Block> outsideBlocks = new it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet<>(ship.BLOCK_STATES.size(),0.75f,MobileBlockState.HASH_STRAT);
        Set<Block> outerHull = new it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet<>(ship.BLOCK_STATES.size(),0.75f,MobileBlockState.HASH_STRAT);
        Set<Block> innerShip = new it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet<>(ship.BLOCK_STATES.size(),0.75f,MobileBlockState.HASH_STRAT);
        scanHull(outsideBlocks, outerHull, innerShip, 0, 0, 0);
        boolean foundOne = false;
        BoundingBox bbox = ship.getBoundingBox();
        for (Block b : outsideBlocks) {
            if (b.getType().isAir()) continue;
            if (truly) {
                if (!bbox.contains(b.getLocation().toVector())) continue;
            }
            foundOne = true;
            if (isDataMedium(b.getBlockData()) || ((b.getBlockData() instanceof Waterlogged) && ((Waterlogged) b.getBlockData()).isWaterlogged())) return true;
        }
        if (truly && !foundOne) return isUnderwater(false);
        return false;
    }

    public int getFluidLevel(Medium m) {
        World world = ship.getWorld();
        int fluidY = this.GLOBAL_WATER_SURFACE;
        if (world == null) return fluidY;
        Set<Block> outsideBlocks = new it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet<>(ship.BLOCK_STATES.size(),0.75f,MobileBlockState.HASH_STRAT);
        Set<Block> outerHull = new it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet<>(ship.BLOCK_STATES.size(),0.75f,MobileBlockState.HASH_STRAT);
        Set<Block> innerShip = new it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet<>(ship.BLOCK_STATES.size(),0.75f,MobileBlockState.HASH_STRAT);
        fluidY = Math.max(world.getMinHeight(), (int)(ship.model.minY - 2f));
        if (m.id.equalsIgnoreCase("void")) return fluidY;
        scanHull(outsideBlocks, outerHull, innerShip, 0, 0, 3);
        for (Block b : outsideBlocks) {
            if (MobileBlockState.getShipFromLoc(b.getLocation()) != null) continue;
            else if (m.isDataMedium(b.getBlockData())) {
                fluidY = Math.max(b.getY(), fluidY);
            }
        }
        return fluidY;
    }

    public int getFluidLevel(BlockData data) {
        return getFluidLevel(getMedium(data));
    }

    public int getFluidLevel() {
        return getFluidLevel(getMedium("water"));
    }

    /**
     * Scans the ship hull for airtightness. Feed in empty lists to get filled
     *
     * @param outsideBlocks All blocks that are outside of the ship within one block
     *                      of its bounding box. This includes ship hull blocks that
     *                      are exterior-waterloggable
     * @param outerHull     All blocks that are part of the outer hull of the ship
     * @param innerShip     All that are part of the ship's interior (including air
     *                      blocks) and/or are not on the outside of the ship
     */
    public void scanHullLocations(Set<Location> outsideBlocks, Set<Location> outerHull, Set<Location> innerShip) {
        World world = ship.getWorld();
        if (world == null) return;
        Set<Location> allBlocks = new it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet<>(ship.BLOCK_STATES.size(),0.75f,MobileBlockState.HASH_STRAT);
        Set<Location> nextLayer = new it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet<>(ship.BLOCK_STATES.size(),0.75f,MobileBlockState.HASH_STRAT);
        int x1 = ship.minX(); int y1 = ship.minY(); int z1 = ship.minZ();
        int x2 = ship.maxX(); int y2 = ship.maxY(); int z2 = ship.maxZ();
        for (int x = x1 - 1; x <= x2 + 1; x++) {
            for (int y = y1 - 1; y <= y2 + 1; y++) {
                for (int z = z1 - 1; z <= z2 + 1; z++) {
                    allBlocks.add(new Location(world,x, y, z));
                    if (x == x1 - 1 || y == y1 - 1 || z == z1 - 1 || x == x2 + 1 || y == y2 + 1 || z == z2 + 1) {
                        nextLayer.add(new Location(world,x, y, z));
                    }
                }
            }
        }
        while (!nextLayer.isEmpty()) {
            Set<Location> thisLayer = new it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet<>(ship.BLOCK_STATES.size(),0.75f,MobileBlockState.HASH_STRAT);
            thisLayer.addAll(nextLayer);
            outsideBlocks.addAll(nextLayer);
            nextLayer.clear();
            for (Location b : thisLayer) {
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            if (Math.abs(x) + Math.abs(y) + Math.abs(z) > 1) continue;
                            Location newBlock = b.clone().add(x, y, z);
                            if (thisLayer.contains(newBlock) || outsideBlocks.contains(newBlock)
                                    || (newBlock.getBlockX() < x1 || newBlock.getBlockY() < y1 || newBlock.getBlockZ() < z1
                                            || newBlock.getBlockX() > x2 || newBlock.getBlockY() > y2 || newBlock.getBlockZ() > z2)
                                    || nextLayer.contains(newBlock)) {
                                continue;
                            }
                            if (MobileBlockState.doesShipHave(ship,b) || MobileBlockState.doesShipHave(ship,newBlock)) {
                                if (!MobileBlockState.doesShipHave(ship,b)) outerHull.add(newBlock);
                            } else {
                                nextLayer.add(newBlock);
                            }
                        }
                    }
                }
            }
        }
        innerShip.addAll(allBlocks);
        innerShip.removeAll(outsideBlocks);
        innerShip.removeAll(outerHull);
    }

    /**
     * Scans the ship hull for airtightness. Feed in empty lists to get filled
     *
     * @param outsideBlocks All blocks that are outside of the ship within one block
     *                      of its bounding box. This includes ship hull blocks that
     *                      are exterior-waterloggable
     * @param outerHull     All blocks that are part of the outer hull of the ship
     * @param innerShip     All that are part of the ship's interior (including air
     *                      blocks) and/or are not on the outside of the ship
     */
    public void scanHull(Set<Block> outsideBlocks, Set<Block> outerHull, Set<Block> innerShip) {
        scanHull(outsideBlocks,outerHull,innerShip,1,1,0);
    }
    public void scanHull(Set<Block> outsideBlocks, Set<Block> outerHull, Set<Block> innerShip, Integer vhoutset) {
        scanHull(outsideBlocks,outerHull,innerShip,vhoutset,vhoutset,0);
    }
    public void scanHull(Set<Block> outsideBlocks, Set<Block> outerHull, Set<Block> innerShip, Integer houtset, Integer voutset, Integer scanDown) {
        World world = ship.getWorld();
        if (world == null) return;
        if (voutset == null) voutset = 0;
        if (houtset == null) houtset = 0;
        if (scanDown == null) scanDown = 0;
        Set<Block> allBlocks = new it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet<>(ship.BLOCK_STATES.size(),0.75f,MobileBlockState.HASH_STRAT);
        Set<Block> nextLayer = new it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet<>(ship.BLOCK_STATES.size(),0.75f,MobileBlockState.HASH_STRAT);
        int x1 = ship.minX()-houtset; int y1 = ship.minY()-(voutset+scanDown); int z1 = ship.minZ()-houtset;
        int x2 = ship.maxX()+houtset; int y2 = ship.maxY()+voutset; int z2 = ship.maxZ()+houtset;
        for (int x = x1 - 1; x <= x2 + 1; x++) {
            for (int y = y1 - 1; y <= y2 + 1; y++) {
                for (int z = z1 - 1; z <= z2 + 1; z++) {
                    allBlocks.add(world.getBlockAt(x, y, z));
                    if (x == x1 - 1 || y == y1 - 1 || z == z1 - 1 || x == x2 + 1 || y == y2 + 1 || z == z2 + 1) {
                        nextLayer.add(world.getBlockAt(x, y, z));
                    }
                }
            }
        }
        while (!nextLayer.isEmpty()) {
            Set<Block> thisLayer = new it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet<>(ship.BLOCK_STATES.size(),0.75f,MobileBlockState.HASH_STRAT);
            thisLayer.addAll(nextLayer);
            outsideBlocks.addAll(nextLayer);
            nextLayer.clear();
            for (Block b : thisLayer) {
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            if (Math.abs(x) + Math.abs(y) + Math.abs(z) > 1)
                                continue;
                            Block newBlock = b.getRelative(x, y, z);
                            if (thisLayer.contains(newBlock) || outsideBlocks.contains(newBlock)
                                    || (newBlock.getX() < x1 || newBlock.getY() < y1 || newBlock.getZ() < z1
                                            || newBlock.getX() > x2 || newBlock.getY() > y2 || newBlock.getZ() > z2)
                                    || nextLayer.contains(newBlock)) {
                                continue;
                            }
                            if (MobileBlockState.doesShipHave(ship,b) || MobileBlockState.doesShipHave(ship,newBlock)) {
                                if (isFluidConnected(b, newBlock)) {
                                    nextLayer.add(newBlock);
                                    if (MobileBlockState.doesShipHave(ship,b))
                                        outerHull.add(newBlock);
                                }
                                if (!MobileBlockState.doesShipHave(ship,b))
                                    outerHull.add(newBlock);
                            } else {
                                nextLayer.add(newBlock);
                            }
                        }
                    }
                }
            }
        }
        innerShip.addAll(allBlocks);
        innerShip.removeAll(outsideBlocks);
        innerShip.removeAll(outerHull);
    }

    public static boolean isFluidConnected(Block a, Block b) {
        BlockFace face = a.getFace(b);
        int A = isFluidConnected(a, face);
        int B = isFluidConnected(b, face.getOppositeFace());
        if (A + B >= 3)
            return true;
        if (A == 0 || B == 0)
            return false;
        boolean anw = false;
        boolean ane = false;
        boolean asw = false;
        boolean ase = false;
        boolean bnw = false;
        boolean bne = false;
        boolean bsw = false;
        boolean bse = false;
        if (a.getBlockData() instanceof Slab) {
            Slab s = (Slab) a.getBlockData();
            anw = ane = s.getType() == Slab.Type.TOP;
            asw = ase = s.getType() == Slab.Type.BOTTOM;
        }
        if (b.getBlockData() instanceof Slab) {
            Slab s = (Slab) b.getBlockData();
            bnw = bne = s.getType() == Slab.Type.TOP;
            bsw = bse = s.getType() == Slab.Type.BOTTOM;
        }
        if (a.getBlockData() instanceof Stairs) {
            Stairs s = (Stairs) a.getBlockData();
            if (face == BlockFace.UP || face == BlockFace.DOWN) {
                switch (s.getFacing()) {
                    case NORTH:
                        switch (s.getShape()) {
                            case INNER_LEFT:
                                anw = ane = asw = true;
                                break;
                            case INNER_RIGHT:
                                anw = ane = ase = true;
                                break;
                            case OUTER_LEFT:
                                anw = true;
                                break;
                            case OUTER_RIGHT:
                                ane = true;
                                break;
                            case STRAIGHT:
                                anw = ane = true;
                                break;
                        }
                        break;
                    case SOUTH:
                        switch (s.getShape()) {
                            case INNER_LEFT:
                                asw = ase = ane = true;
                                break;
                            case INNER_RIGHT:
                                anw = asw = ase = true;
                                break;
                            case OUTER_LEFT:
                                ase = true;
                                break;
                            case OUTER_RIGHT:
                                asw = true;
                                break;
                            case STRAIGHT:
                                asw = ase = true;
                                break;
                        }
                        break;
                    case EAST:
                        switch (s.getShape()) {
                            case INNER_LEFT:
                                anw = ane = ase = true;
                                break;
                            case INNER_RIGHT:
                                asw = ase = ane = true;
                                break;
                            case OUTER_LEFT:
                                ane = true;
                                break;
                            case OUTER_RIGHT:
                                ase = true;
                                break;
                            case STRAIGHT:
                                ane = ase = true;
                                break;
                        }
                        break;
                    case WEST:
                        switch (s.getShape()) {
                            case INNER_LEFT:
                                anw = asw = ase = true;
                                break;
                            case INNER_RIGHT:
                                ane = anw = asw = true;
                                break;
                            case OUTER_LEFT:
                                asw = true;
                                break;
                            case OUTER_RIGHT:
                                anw = true;
                                break;
                            case STRAIGHT:
                                anw = asw = true;
                                break;
                        }
                        break;
                }
            } else {
                if (s.getHalf() == Bisected.Half.TOP) {
                    anw = ane = true;
                }
                if (s.getHalf() == Bisected.Half.TOP) {
                    asw = ase = true;
                }
                int side = 0;// 0 is same, then clockwise
                switch (face) {
                    case NORTH:
                        side = 0;
                        break;
                    case EAST:
                        side = 1;
                        break;
                    case SOUTH:
                        side = 2;
                        break;
                    case WEST:
                        side = 3;
                        break;
                }
                switch (s.getFacing()) {
                    case NORTH:
                        break;
                    case EAST:
                        side--;
                        break;
                    case WEST:
                        side--;
                        break;
                    case SOUTH:
                        side--;
                        break;
                }
                if (side < 0)
                    side += 4;
                switch (s.getShape()) {
                    case INNER_LEFT:
                        if (side == 1)
                            anw = true;
                        if (side == 2)
                            ane = true;
                        break;
                    case INNER_RIGHT:
                        if (side == 2)
                            anw = true;
                        if (side == 3)
                            ane = true;
                        break;
                    case OUTER_LEFT:
                        if (side == 0)
                            anw = true;
                        if (side == 3)
                            ane = true;
                        break;
                    case OUTER_RIGHT:
                        if (side == 1)
                            anw = true;
                        if (side == 0)
                            ane = true;
                        break;
                    case STRAIGHT:
                        if (side == 1)
                            anw = true;
                        if (side == 3)
                            ane = true;
                        break;
                }
            }
        }
        return !((anw || bnw) && (ane || bne) && (asw || bsw) && (ase || bse));
    }
    public static int isFluidConnected(Block b, BlockFace face) {
        switch (b.getType()) {
            case BUBBLE_COLUMN:
            case CHEST:
            case TRAPPED_CHEST:
            case CONDUIT:
            case TUBE_CORAL:
            case TUBE_CORAL_FAN:
            case TUBE_CORAL_WALL_FAN:
            case BRAIN_CORAL:
            case BRAIN_CORAL_FAN:
            case BRAIN_CORAL_WALL_FAN:
            case BUBBLE_CORAL:
            case BUBBLE_CORAL_FAN:
            case BUBBLE_CORAL_WALL_FAN:
            case FIRE_CORAL:
            case FIRE_CORAL_FAN:
            case FIRE_CORAL_WALL_FAN:
            case HORN_CORAL:
            case HORN_CORAL_FAN:
            case HORN_CORAL_WALL_FAN:
            case DEAD_TUBE_CORAL:
            case DEAD_TUBE_CORAL_FAN:
            case DEAD_TUBE_CORAL_WALL_FAN:
            case DEAD_BRAIN_CORAL:
            case DEAD_BRAIN_CORAL_FAN:
            case DEAD_BRAIN_CORAL_WALL_FAN:
            case DEAD_BUBBLE_CORAL:
            case DEAD_BUBBLE_CORAL_FAN:
            case DEAD_BUBBLE_CORAL_WALL_FAN:
            case DEAD_FIRE_CORAL:
            case DEAD_FIRE_CORAL_FAN:
            case DEAD_FIRE_CORAL_WALL_FAN:
            case DEAD_HORN_CORAL:
            case DEAD_HORN_CORAL_FAN:
            case DEAD_HORN_CORAL_WALL_FAN:
            case ENDER_CHEST:
            case OAK_FENCE:
            case BIRCH_FENCE:
            case SPRUCE_FENCE:
            case JUNGLE_FENCE:
            case ACACIA_FENCE:
            case DARK_OAK_FENCE:
            case NETHER_BRICK_FENCE:
            case IRON_BARS:
            case KELP:
            case LADDER:
            case SEAGRASS:
            case OAK_SIGN:
            case OAK_WALL_SIGN:
            case BIRCH_SIGN:
            case BIRCH_WALL_SIGN:
            case SPRUCE_SIGN:
            case SPRUCE_WALL_SIGN:
            case JUNGLE_SIGN:
            case JUNGLE_WALL_SIGN:
            case ACACIA_SIGN:
            case ACACIA_WALL_SIGN:
            case DARK_OAK_SIGN:
            case DARK_OAK_WALL_SIGN:
            case TALL_SEAGRASS:
            case COBBLESTONE_WALL:
            case MOSSY_COBBLESTONE_WALL:
            case BRICK_WALL:
            case PRISMARINE_WALL:
            case RED_SANDSTONE_WALL:
            case MOSSY_STONE_BRICK_WALL:
            case GRANITE_WALL:
            case STONE_BRICK_WALL:
            case NETHER_BRICK_WALL:
            case ANDESITE_WALL:
            case RED_NETHER_BRICK_WALL:
            case SANDSTONE_WALL:
            case END_STONE_BRICK_WALL:
            case DIORITE_WALL:
                return 2;
            case CAMPFIRE:// insulated on the bottom only
                if (face == BlockFace.DOWN)
                    return 0;
                return 2;
            case GLASS_PANE:// depends on blockstate..???
                MultipleFacing pane = (MultipleFacing) b.getBlockData();
                switch (face) {
                    case EAST:
                    case WEST:
                        if (pane.hasFace(BlockFace.NORTH) || pane.hasFace(BlockFace.SOUTH))
                            return 2;
                        return 0;
                    case NORTH:
                    case SOUTH:
                        if (pane.hasFace(BlockFace.EAST) || pane.hasFace(BlockFace.WEST))
                            return 2;
                        return 0;
                }
                return 0;
            case OAK_SLAB:
            case SPRUCE_SLAB:
            case BIRCH_SLAB:
            case JUNGLE_SLAB:
            case ACACIA_SLAB:
            case DARK_OAK_SLAB:
            case STONE_SLAB:
            case SMOOTH_STONE_SLAB:
            case SANDSTONE_SLAB:
            case CUT_SANDSTONE_SLAB:
            case PETRIFIED_OAK_SLAB:
            case COBBLESTONE_SLAB:
            case BRICK_SLAB:
            case STONE_BRICK_SLAB:
            case NETHER_BRICK_SLAB:
            case QUARTZ_SLAB:
            case RED_SANDSTONE_SLAB:
            case CUT_RED_SANDSTONE_SLAB:
            case PURPUR_SLAB:
            case PRISMARINE_SLAB:
            case PRISMARINE_BRICK_SLAB:
            case DARK_PRISMARINE_SLAB:
            case POLISHED_GRANITE_SLAB:
            case SMOOTH_RED_SANDSTONE_SLAB:
            case MOSSY_STONE_BRICK_SLAB:
            case POLISHED_DIORITE_SLAB:
            case MOSSY_COBBLESTONE_SLAB:
            case END_STONE_BRICK_SLAB:
            case SMOOTH_SANDSTONE_SLAB:
            case SMOOTH_QUARTZ_SLAB:
            case GRANITE_SLAB:
            case ANDESITE_SLAB:
            case RED_NETHER_BRICK_SLAB:
            case POLISHED_ANDESITE_SLAB:
            case DIORITE_SLAB:
                Slab slab = (Slab) b.getBlockData();
                if (slab.getType() == Slab.Type.DOUBLE)
                    return 0;
                if (face == BlockFace.UP) {
                    if (slab.getType() == Slab.Type.TOP)
                        return 0;
                    if (slab.getType() == Slab.Type.BOTTOM)
                        return 2;
                }
                if (face == BlockFace.DOWN) {
                    if (slab.getType() == Slab.Type.BOTTOM)
                        return 0;
                    if (slab.getType() == Slab.Type.TOP)
                        return 2;
                }
                return 1;
            case PURPUR_STAIRS:
            case OAK_STAIRS:
            case COBBLESTONE_STAIRS:
            case BRICK_STAIRS:
            case STONE_BRICK_STAIRS:
            case NETHER_BRICK_STAIRS:
            case SANDSTONE_STAIRS:
            case SPRUCE_STAIRS:
            case BIRCH_STAIRS:
            case JUNGLE_STAIRS:
            case QUARTZ_STAIRS:
            case ACACIA_STAIRS:
            case DARK_OAK_STAIRS:
            case PRISMARINE_STAIRS:
            case PRISMARINE_BRICK_STAIRS:
            case DARK_PRISMARINE_STAIRS:
            case RED_SANDSTONE_STAIRS:
            case POLISHED_GRANITE_STAIRS:
            case SMOOTH_RED_SANDSTONE_STAIRS:
            case MOSSY_STONE_BRICK_STAIRS:
            case POLISHED_DIORITE_STAIRS:
            case MOSSY_COBBLESTONE_STAIRS:
            case END_STONE_BRICK_STAIRS:
            case STONE_STAIRS:
            case SMOOTH_SANDSTONE_STAIRS:
            case SMOOTH_QUARTZ_STAIRS:
            case GRANITE_STAIRS:
            case ANDESITE_STAIRS:
            case RED_NETHER_BRICK_STAIRS:
            case POLISHED_ANDESITE_STAIRS:
            case DIORITE_STAIRS:
                Stairs stairs = (Stairs) b.getBlockData();
                if (face == BlockFace.UP) {
                    return stairs.getHalf() == Bisected.Half.TOP ? 0 : 1;
                }
                if (face == BlockFace.DOWN) {
                    return stairs.getHalf() == Bisected.Half.BOTTOM ? 0 : 1;
                }
                if (stairs.getShape() == Stairs.Shape.OUTER_LEFT || stairs.getShape() == Stairs.Shape.OUTER_RIGHT)
                    return 1;
                if (stairs.getShape() == Stairs.Shape.STRAIGHT)
                    return stairs.getFacing() == face ? 0 : 1;
                if (stairs.getShape() == Stairs.Shape.INNER_LEFT) {
                    switch (stairs.getFacing()) {
                        case NORTH:
                            return face == BlockFace.NORTH || face == BlockFace.WEST ? 0 : 1;
                        case EAST:
                            return face == BlockFace.EAST || face == BlockFace.NORTH ? 0 : 1;
                        case SOUTH:
                            return face == BlockFace.SOUTH || face == BlockFace.EAST ? 0 : 1;
                        case WEST:
                            return face == BlockFace.WEST || face == BlockFace.SOUTH ? 0 : 1;
                    }
                }
                if (stairs.getShape() == Stairs.Shape.INNER_RIGHT) {
                    switch (stairs.getFacing()) {
                        case NORTH:
                            return face == BlockFace.NORTH || face == BlockFace.EAST ? 0 : 1;
                        case EAST:
                            return face == BlockFace.EAST || face == BlockFace.SOUTH ? 0 : 1;
                        case SOUTH:
                            return face == BlockFace.SOUTH || face == BlockFace.WEST ? 0 : 1;
                        case WEST:
                            return face == BlockFace.WEST || face == BlockFace.NORTH ? 0 : 1;
                    }
                }
                return 1;
            case OAK_TRAPDOOR:
            case SPRUCE_TRAPDOOR:
            case BIRCH_TRAPDOOR:
            case JUNGLE_TRAPDOOR:
            case ACACIA_TRAPDOOR:
            case DARK_OAK_TRAPDOOR:
            case IRON_TRAPDOOR:
                TrapDoor door = (TrapDoor) b.getBlockData();
                if (face == BlockFace.UP && !door.isOpen() && door.getHalf() == Bisected.Half.TOP)
                    return 0;
                if (face == BlockFace.DOWN && !door.isOpen() && door.getHalf() == Bisected.Half.BOTTOM)
                    return 0;
                if (door.isOpen() && face == door.getFacing().getOppositeFace())
                    return 0;
                return 2;
        }
        return 0;
    }
    /**
     * Handle buoyancy physics for water-based ships.
     */
    public void handleBuoyancy(Location vehicleLoc) {
        if (ship.isAirship || ship.isLandcraft || ship.CHANGE_WORLD) return;
        int MIN_BY = ship.minY();
        if (MIN_BY < ship.getWorld().getMinHeight()) return;
        ship.model.minY = MIN_BY;

        int MAX_BY = ship.maxY();
        if (MAX_BY >= ship.getWorld().getMaxHeight()) return;
        ship.model.maxY = MAX_BY;
        Set<Block> EXT = getExterior(0,0);
        float heightY = MAX_BY - MIN_BY;
        EXT.removeIf(b -> b.getY() > (heightY / 2.5)+MIN_BY);
        BlockData MEDIUM = getPrimaryMediumBlockData(EXT);
        Medium MEDIUM_TYPE = getMedium(MEDIUM);
        int FLUID_LEVEL = this.getFluidLevel(MEDIUM);

        int airPockets = 0;
        int airPocketY = MIN_BY + ((int)heightY + 1);
        for (Block block : EXT) {
            int checkDist = 0;
            boolean foundAir = false;
            Block checked = block.getRelative(BlockFace.DOWN);
            for (checkDist = 0; checkDist < 10; checkDist++) {
                if (MobileBlockState.doesShipHave(ship,checked)) continue;
                else if (!NmsUtils.withinWorldBorder(checked.getLocation())) {
                    foundAir = false;
                    break;
                } else if (checked.isSolid() && !foundAir) {
                    foundAir = false;
                    break;
                } else if (checked.getType().isAir() && !foundAir) {
                    foundAir = true;
                } else if (!foundAir) checked = checked.getRelative(BlockFace.DOWN);
            }
            if (foundAir) {
                airPockets++;
                airPocketY = Math.min(airPocketY, checked.getY());
            }
        }
        if (airPockets > EXT.size()+2) this.CURRENT_FLUID_LEVEL = Math.min(FLUID_LEVEL,airPocketY);
        else this.CURRENT_FLUID_LEVEL = FLUID_LEVEL;
        Map<String,Number> DATA = ShipWheelManager.getStateBuoyancyData(vehicleLoc, ship.BLOCK_STATES, ship.model.totalWeight);
        double waterSurfaceY = (double)this.CURRENT_FLUID_LEVEL;
        Float floatOffset = -1f;
        Number floatOff = DATA.getOrDefault("surface_offset",null);
        if (floatOff == null) floatOffset = (float)(-Math.round(heightY/3.75f)) + (float)Math.round((heightY/3f));
        else floatOffset = -1*(floatOff.floatValue());
        // Target Y position: water surface + float offset
        
        double currentY = (double)MIN_BY;
        double targetY = waterSurfaceY + floatOffset;
        double yDifference = Math.abs(targetY - currentY);
        if (SmoothSailingPlugin.DEBUG) {
            System.out.println("SHIP HEIGHT: "+heightY);
            System.out.println("AIR POCKET Y: "+airPocketY);
            System.out.println("AIR POCKET SIZE: "+airPockets);
            System.out.println("WATER SURFACE Y: "+waterSurfaceY);

            System.out.println("WATER LEVEL DIFFERENCE: "+yDifference);

            System.out.println("FLOAT OFFSET: "+(double)floatOffset);
            System.out.println("TARGET FLOAT Y: "+targetY);
        }
        if (yDifference >= 1d) {
            if (MIN_BY > targetY) {
                currentSpeed = 0f;
                currentYVelocity = -1f; // Gravity
            } else if (MIN_BY <= targetY+1) {
                currentYVelocity = 1f; // Floatation
            } else {
                currentYVelocity = 0f;
            }
        }
    }

    public void processFluidRestore() {
        if (ship.CHANGE_WORLD) return;
        else if (ship.vehicle == null) return;
        else if (ship.vehicle.isDead()) return;
        var ENTRY_SET = ship.FLUIDS.internalMap.long2ObjectEntrySet();
        ENTRY_SET.fastForEach(entry -> {
            long key = entry.getLongKey();
            BlockData data = entry.getValue();
            Location loc = MobileBlockState.unpackLocation(key,ship.getWorld());
            if (MobileBlockState.doesShipHave(ship,loc)) return;
            if (data != null && !data.getMaterial().isAir()) {
                NmsUtils.setBlockFast(loc,data);
                NmsUtils.renderBlockChanges(loc);
                if (SmoothSailingPlugin.DEBUG) {
                    System.out.println("LOCATION KEY: "+key);
                    System.out.println("ATTEMPTING TO RESTORE FLUID @ " + LocationLib.readLocationToString(loc) + " W/BLOCK DATA:" + data);
                }
            }
        });
        ship.FLUIDS.internalMap.keySet().removeIf(key -> !(MobileBlockState.doesShipHave(ship,MobileBlockState.unpackLocation(key,ship.getWorld()))));
    }

    public void detectNearbyEntities() {
        if (ship.CHANGE_WORLD) return;
        else if (ship.vehicle == null) return;
        else if (ship.vehicle.isDead()) return;
        com.ccorp2002.smoothsailing.region.TaskUtils.getEntityScheduler(ship.vehicle).runTask(()->{
            final List<Player> players = new ArrayList<>();
            ship.ENTITY_COMPOSITION.removeAll(ship.vehicle.getWorld().getPlayers());
            ship.ENTITY_COMPOSITION.removeIf(ent -> (!ShipListener.isSeat(ent) && !(ent instanceof Display)) && (!ent.isValid() || !ent.getWorld().equals(ship.vehicle.getWorld())));
            for (Entity entity : ship.ENTITY_COMPOSITION) {
                if (entity == null) continue;
                if (entity instanceof Player player) continue;
                if (!ShipListener.isSeat(entity) && !(entity instanceof Display)) continue;
                for (Player player : ship.vehicle.getWorld().getPlayers()) {
                    if (!player.getWorld().equals(entity.getWorld())) continue;
                    if (!ship.vehicle.getWorld().equals(entity.getWorld())) continue;
                    if (!ship.vehicle.getWorld().equals(player.getWorld())) continue;
                    if (ship.getBoundingBox().overlaps(player.getBoundingBox()) && entity.getLocation().distance(player.getLocation()) <= 12.5d) players.add(player);
                }
            }
            ship.ENTITY_COMPOSITION.add(ship.vehicle);
            ship.ENTITY_COMPOSITION.addAll(players);
            players.clear();
        });
    }
}
