package com.ccorp2002.smoothsailing.ship;

import com.ccorp2002.smoothsailing.*;
import com.ccorp2002.smoothsailing.ShipConfig;
import com.ccorp2002.smoothsailing.ShipListener;
import com.ccorp2002.smoothsailing.customships.*;
import com.ccorp2002.smoothsailing.utils.rotation.*;
import com.ccorp2002.smoothsailing.utils.movement.*;
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
    public static BlockData getPrimaryMediumAtBelowY(Collection<MobileBlockState> BLOCK_STATES, int y) {
        BLOCK_STATES.removeIf(s -> s.getBlockY() >= y+1);
        Map.Entry<String, Integer> maxEntry = getPrimaryMedium(BLOCK_STATES);
        Medium medium = getMedium(maxEntry.getKey());
        if (medium == null) return ShipPhysics.VOID;
        return medium.getBlockDatas().getFirst();
    }
    public static BlockData getPrimaryMediumBlockAtBelowY(Collection<Block> BLOCKS, int y) {
        BLOCKS.removeIf(s -> s.getY() >= y+1);
        Map.Entry<String, Integer> maxEntry = getPrimaryMediumBlock(BLOCKS);
        Medium medium = getMedium(maxEntry.getKey());
        if (medium == null) return ShipPhysics.VOID;
        return medium.getBlockDatas().getFirst();
    }
    public static BlockData getPrimaryMediumData(Collection<MobileBlockState> BLOCK_STATES) {
        Map.Entry<String, Integer> maxEntry = getPrimaryMedium(BLOCK_STATES);
        Medium medium = getMedium(maxEntry.getKey());
        if (medium == null) return ShipPhysics.VOID;
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
            FLUID_COUNTER.put("water",5);
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
            FLUID_COUNTER.put("water",5);
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
        ShipConfig config = ShipInstance.config;
        float dragMultiplier = 0f;
        Map.Entry<String, Integer> maxEntry = getPrimaryMedium(ship.BLOCK_STATES);
        Integer maxValue = maxEntry.getValue();
        dragMultiplier = maxValue + 0.0f;
        if (Math.abs(dragMultiplier) >= 1) { //Fallback
            dragMultiplier = 0.75f;
        }
        return dragMultiplier;
    }

    public float getEngineSpeedFactor() {
        if (ship == null) return ShipPhysics.MAX_SPEED;
        float speedFactor = this.PER_MAX_SPEED;
        int engines = ship.engineCount;
        return speedFactor;
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
    public static float MAX_SPEED = 10f;
    public float PER_MAX_SPEED = MAX_SPEED;
    public volatile float currentSpeed = 0.0f;
    public volatile float currentYVelocity = 0.0f;
    public volatile float currentRotationVelocity = 0.0f;
    public int ROTATION_INCR = 0;
    public volatile boolean moving = false;
    public long MOVEMENT_COOLDOWN = 3l;
    public int GLOBAL_WATER_SURFACE = -127;
    public int CURRENT_FLUID_LEVEL = GLOBAL_WATER_SURFACE;

    public BlockData getVOID() {
        return ShipPhysics.VOID;
    }

    public ShipPhysics(ShipInstance ship) {
        this.ship = ship;
        this.CURRENT_FLUID_LEVEL = this.GLOBAL_WATER_SURFACE;
        this.GLOBAL_WATER_SURFACE = SmoothSailingPlugin.INSTANCE.getConfig().getInt("water-level",63);
        if (ship.isAirship) PER_MAX_SPEED = 7f;
        else if (ship.isLandcraft) PER_MAX_SPEED = 3f;
        else PER_MAX_SPEED = 5f;
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
            float accel = (float)0.475f+((PER_MAX_SPEED*0.625f));
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
        if (physicsInterval >= 30) {
            physicsInterval = 0;
            if (!ship.isSinking) handleBuoyancy(vehicleLoc);
        } else physicsInterval++;
        boolean hasHorizontalMovement = (Math.abs(currentSpeed)) >= 0.375f;
        boolean hasVerticalMovement = (Math.abs(currentYVelocity)) >= 0.395f;
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
        applyDeckPhysics();
        List<Entity> FILTERED_ENTS = ship.ENTITY_COMPOSITION.stream()
                .filter(p -> p.isValid())
                .collect(Collectors.toList());
        final Location ABS_CENTER = new Location(ship.vehicle.getWorld(),0,0,0);
        // Move the vehicle
        if ((ship.isLeftPressed || ship.isRightPressed) && (ship.isNotProcessing() && !this.moving)) {
            // Apply rotation
            Location newLoc = vehicleLoc.clone();
            Location CENTER_POINT = ship.vehicle.getLocation().toBlockLocation();
            newLoc.setYaw(ShipWheelData.blockFaceToYaw(ship.facing));
            //Runnable runMe = () -> {
            float newYaw = newLoc.getYaw();
            float rotation = 0.0f;
            final String LEFT_RIGHT;
            final org.bukkit.block.structure.StructureRotation TURN_BLOCK_DATA;
            if (ship.isLeftPressed) {rotation = -90.0f; TURN_BLOCK_DATA = org.bukkit.block.structure.StructureRotation.COUNTERCLOCKWISE_90; LEFT_RIGHT = "LEFT";}
            else if (ship.isRightPressed) {rotation = 90.0f; TURN_BLOCK_DATA = org.bukkit.block.structure.StructureRotation.CLOCKWISE_90; LEFT_RIGHT = "RIGHT";}
            else {LEFT_RIGHT = "NONE"; TURN_BLOCK_DATA = org.bukkit.block.structure.StructureRotation.NONE;}
            final float initYaw = ship.vehicle.getYaw();
            final float rotationF = rotation;
            final float rotationFinal = rotationF / ROTATION_INCR;
            Location wheelLoc = null;
            ship.isLeftPressed = false;
            ship.isRightPressed = false;
            this.moving = true;
            int collisionCount = 0;
            boolean canEnterAt = true;
            final MobileBlockState[] TEMPS = ship.BLOCK_STATES.toArray(MobileBlockState[]::new);
            for (MobileBlockState st : TEMPS) {
                if (st == null) continue;
                Location stloc = st.getLocation(ship.getWorld());
                Location updLoc = stloc.clone();
                Location rotatedLoc = Rotate2D.ROTATE_VEC(LEFT_RIGHT, updLoc.subtract(CENTER_POINT)).add(CENTER_POINT).toBlockLocation();
                if (pilot != null) {
                    for (final com.ccorp2002.smoothsailing.utils.claims.IClaimInterface ICLAIM : SmoothSailingPlugin.INSTANCE.CLAIM_INTEFACES) {
                        if (!canEnterAt) break;
                        if (!ICLAIM.canEnterAt(pilot,rotatedLoc)) canEnterAt = false;
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
            com.ccorp2002.smoothsailing.event.ShipMoveEvent mevent = new com.ccorp2002.smoothsailing.event.ShipMoveEvent(ship,newLoc,ship.vehicle);
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
            applyDeckPhysics();
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
                st.setBlockData(TURNED);
                NmsUtils.setBlockFast(updLoc,TURNED);
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
            for (Object KEY : this.ship.TRACKED_STATE_MAP.keySet()) {
                if (KEY == null) continue;
                boolean INTERIOR = false;
                if (KEY instanceof String key && key.equals("__interior__")) INTERIOR = true;
                Set<MobileBlockState> VALUE = ship.TRACKED_STATE_MAP.getOrDefault(KEY,new MobileBlockState.MobileStateSet(1));
                final MobileBlockState[] _COPY = VALUE.toArray(MobileBlockState[]::new);
                for (int indx = 0; indx < _COPY.length; indx ++) {
                    MobileBlockState st = _COPY[indx];
                    if (st == null) continue;
                    final Location stloc = st.getLocation(ship.getWorld());
                    Location updLoc = stloc.clone();
                    updLoc = Rotate2D.ROTATE_VEC(LEFT_RIGHT, stloc.subtract(CENTER_POINT)).add(CENTER_POINT).toBlockLocation();
                    BlockData occupiedBlock = VOID;
                    st.setLocation(updLoc);
                    st.setBlockData(NmsUtils.rotateBlockData(LEFT_RIGHT,st.getBlockData()));
                    if (INTERIOR) NmsUtils.setBlockFast(updLoc,st.state);
                    st.state = NmsUtils.getBlockFaster(updLoc);
                    st.tile = NmsUtils.createTileHolder(updLoc);
                    _COPY[indx] = st;
                }
                VALUE.clear();
                VALUE = new MobileBlockState.MobileStateSet(_COPY.length);
                ((MobileBlockState.MobileStateSet)VALUE).setAll(_COPY);
                this.ship.TRACKED_STATE_MAP.put(KEY,VALUE);
                if (INTERIOR) ship.sendSolids(VALUE);
            }
            ship.sendSolids();
            ship.calculateBoundingBox();
            if (!ship.CHANGE_WORLD && (this.moving || !ship.isNotProcessing())) {
                for (MobileBlockState st : ship.BLOCK_STATES) {
                    if (st == null) continue;
                    final Location stloc = st.getLocation(ship.getWorld());
                    NmsUtils.UPDATE_REDSTONE_AT(stloc);
                }
            }
            this.currentRotationVelocity = 0.0f;
            com.ccorp2002.smoothsailing.region.TaskUtils.getRegionalScheduler(ship.vehicle.getLocation()).runTaskLater(()->{
                currentYVelocity = yVelocity;
                currentSpeed = xzVelocity/2f;
                moving = false;
                ship.setProcessing(false);
            },(long)(MOVEMENT_COOLDOWN)+15L);
            return;
        }
        if ((hasHorizontalMovement || hasVerticalMovement) && !(ship.isLeftPressed || ship.isRightPressed) && (ship.isNotProcessing() && !this.moving)) {
            Location newLoc = vehicleLoc.clone();
            double fx = 0d;
            double fy = 0d;
            double fz = 0d;
            if (hasVerticalMovement) {
                if ((ship.isLandcraft || !ship.isAirship) && !ship.isSinking) currentYVelocity = CLAMP_VALUE(currentYVelocity, -3, 1);
                fy = CLAMP_VALUE(currentYVelocity, -((PER_MAX_SPEED/2)+2), ((PER_MAX_SPEED/2)+2));
            }
            if (hasHorizontalMovement) {
                if (ship.isLandcraft) currentSpeed = CLAMP_VALUE(currentSpeed, -3, 3);
                fx = forwardX * (currentSpeed);
                fz = forwardZ * (currentSpeed);
                if (Math.abs(fx)<1) fx = 0;
                if (Math.abs(fz)<1) fz = 0;
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
                ship.setProcessing(true);
                ship.isLeftPressed = false;
                ship.isRightPressed = false;
                ship.isForwardPressed = false;
                ship.isBackwardPressed = false;
                ship.isSprintPressed = false;
                ship.isSpacePressed = false;
                this.currentRotationVelocity = 0.0f;
                this.currentYVelocity = 0f;
                this.currentSpeed = 0f;
                this.moving = true;
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
                if (SmoothSailingPlugin.DEBUG_DUMP) {
                    System.out.println("MOVING STATE?: "+st.getBlockData().getMaterial().toString()+" TO POSITION?: "+updLoc.toVector().toString());
                    System.out.println("POSITION IS ACCESSIBLE?: "+isAccessible);
                }
                if (!isAccessible) {
                    NmsUtils.getChunkFastest(updLoc);
                    isAccessible = updLoc.isChunkLoaded() && updLoc.isGenerated();
                }
                if (!isAccessible) {
                    NmsUtils.getChunkFastest(updLoc);
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
            applyDeckPhysics();
            //System.out.println("STARTING BLOCK COUNT: "+TEMPS.length);
            for (int indx = 0; indx < TEMPS.length; indx ++) {
                MobileBlockState st = TEMPS[indx];
                if (st == null) continue;
                final Location stloc = st.getLocation(ship.getWorld());
                st.tile = NmsUtils.removeAndCreateTileHolder(stloc);
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
                updLoc.setWorld(offsetf.getWorld());
                updLoc = updLoc.add(offsetf);
                updLoc = updLoc.toBlockLocation();
                var TEMP_WHEEL = SmoothSailingPlugin.INSTANCE.shipWheelManager.getWheelAt(stloc);
                if (TEMP_WHEEL != null && SmoothSailingPlugin.INSTANCE.shipWheelManager != null) {
                    SmoothSailingPlugin.INSTANCE.shipWheelManager.updateWheelLocation(TEMP_WHEEL, updLoc, ship.facing);
                }
                st.occupiedBlock = NmsUtils.toBukkitBlockDataFast(updLoc);
                NmsUtils.setBlockFast(updLoc,st.state);
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
            } //DEBUG WTF 0.1565624714 IS?????
            for (Object KEY : this.ship.TRACKED_STATE_MAP.keySet()) {
                if (KEY == null) continue;
                boolean INTERIOR = false;
                if (KEY instanceof String key && key.equals("__interior__")) INTERIOR = true;
                Set<MobileBlockState> VALUE = ship.TRACKED_STATE_MAP.getOrDefault(KEY,new MobileBlockState.MobileStateSet(1));
                final MobileBlockState[] _COPY = VALUE.toArray(MobileBlockState[]::new);
                for (int indx = 0; indx < _COPY.length; indx ++) {
                    MobileBlockState st = _COPY[indx];
                    if (st == null) continue;
                    final Location stloc = st.getLocation(offsetf.getWorld());
                    Location updLoc = stloc.clone();
                    updLoc.setWorld(offsetf.getWorld());
                    updLoc = updLoc.add(offsetf).toBlockLocation();
                    st.setLocation(updLoc);
                    st.occupiedBlock = VOID;
                    if (INTERIOR) NmsUtils.setBlockFast(updLoc,st.state);
                    st.state = NmsUtils.getBlockFaster(updLoc);
                    st.tile = NmsUtils.createTileHolder(updLoc);
                    _COPY[indx] = st;
                }
                VALUE.clear();
                VALUE = new MobileBlockState.MobileStateSet(_COPY.length);
                ((MobileBlockState.MobileStateSet)VALUE).setAll(_COPY);
                this.ship.TRACKED_STATE_MAP.put(KEY,VALUE);
                if (INTERIOR) ship.sendSolids(VALUE);
            }
            ship.sendSolids(ship.BLOCK_STATES);
            ship.calculateBoundingBox();
            if (!ship.CHANGE_WORLD && (this.moving || !ship.isNotProcessing())) {
                for (MobileBlockState st : ship.BLOCK_STATES) {
                    if (st == null) continue;
                    final Location stloc = st.getLocation(ship.getWorld());
                    NmsUtils.UPDATE_REDSTONE_AT(stloc);
                }
            }
            this.currentRotationVelocity = 0.0f;
            com.ccorp2002.smoothsailing.region.TaskUtils.getRegionalScheduler(ship.vehicle.getLocation()).runTaskLater(()->{
                moving = false;
                ship.setProcessing(false);
            },(long)(MOVEMENT_COOLDOWN)+7L);
            return;
        }
    }


    public static boolean isMultipleOf22_5(double number) {
        // Multiply by 10 to handle one decimal place: 22.5 -> 225
        if (number == 0.0d) return false;
        int intNumber = (int) Math.round(number * 10);
        return intNumber % 225 == 0;
    }
    public static boolean isMultipleOf90_0(double number) {
        // Multiply by 10 to handle one decimal place: 90.0 -> 900
        if (number == 0.0d) return false;
        int intNumber = (int) Math.round(number * 10);
        return intNumber % 900 == 0;
    }
    public static int getMultipleOf90_0(double number) {
        // Multiply by 10 to handle one decimal place: 90.0 -> 900
        if (number == 0.0d) return -1;
        int intNumber = (int) Math.round(number * 10);
        int multiple = intNumber % 900;
        if (multiple < 0) return -1;
        return multiple;
    }
    public static int countDivisions(double a, double b) {
        if (b < 2d) return 0; // Avoid infinite loops
        int count = 0;
        while (a % b == 0) {
            a /= b;
            count++;
        }
        return count;
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
            applyDeckPhysics();
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
            applyDeckPhysics();
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
                NmsUtils.setBlockFast(updLoc,st.state);
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
            } //DEBUG WTF 0.1565624714 IS?????
            for (Object KEY : this.ship.TRACKED_STATE_MAP.keySet()) {
                if (KEY == null) continue;
                boolean INTERIOR = false;
                if (KEY instanceof String key && key.equals("__interior__")) INTERIOR = true;
                Set<MobileBlockState> VALUE = ship.TRACKED_STATE_MAP.getOrDefault(KEY,new MobileBlockState.MobileStateSet(1));
                final MobileBlockState[] _COPY = VALUE.toArray(MobileBlockState[]::new);
                for (int indx = 0; indx < _COPY.length; indx ++) {
                    MobileBlockState st = _COPY[indx];
                    if (st == null) continue;
                    final Location stloc = st.getLocation(ship.getWorld());
                    Location updLoc = stloc.clone();
                    updLoc = LocationLib.rotate(updLoc,CENTER_POINT,axis,amount);
                    updLoc.setWorld(newLoc.getWorld());
                    st.occupiedBlock = NmsUtils.toBukkitBlockDataFast(updLoc);
                    st.setLocation(updLoc);
                    if (INTERIOR) NmsUtils.setBlockFast(updLoc,st.state);
                    st.state = NmsUtils.getBlockFaster(updLoc);
                    st.tile = NmsUtils.createTileHolder(updLoc);
                    _COPY[indx] = st;
                }
                VALUE.clear();
                VALUE = new MobileBlockState.MobileStateSet(_COPY.length);
                ((MobileBlockState.MobileStateSet)VALUE).setAll(_COPY);
                this.ship.TRACKED_STATE_MAP.put(KEY,VALUE);
            }
            ship.sendSolids();
            ship.calculateBoundingBox();
            com.ccorp2002.smoothsailing.region.TaskUtils.getRegionalScheduler(ship.vehicle.getLocation()).runTaskLater(()->{
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
        applyDeckPhysics();
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
        applyDeckPhysics();
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
            NmsUtils.setBlockFast(updLoc,st.state);
            st.teleport(updLoc,false);
            st.setLocation(updLoc);
            TEMPS[indx] = st;
            //ship.BLOCK_STATES.add(st);
        }
        ship.BLOCK_STATES.addAll(TEMPS);
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
        applyDeckPhysics();
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
            NmsUtils.setBlockFast(updLoc,st.state);
            st.teleport(updLoc,false);
            st.setLocation(updLoc);
            TEMPS[indx] = st;
        }
        ship.BLOCK_STATES.addAll(TEMPS);
        ship.sendSolids();
        ship.calculateBoundingBox();
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
            moving = false;
            ship.setProcessing(false);
        },(long)(MOVEMENT_COOLDOWN)+5L);
    }

    public static Location rotateFlat90(final String rotation, final Location location) {
        double theta;
        int x = 0;
        int y = 0;
        int z = 0;
        if (rotation.equals("RIGHT")) {
            theta = 0.5 * Math.PI;
            x = (int) Math.round((location.getX() * Math.cos(theta)) + (location.getZ() * (-1 * Math.sin(theta))));
            y = (int) location.getY();
            z = (int) Math.round((location.getX() * Math.sin(theta)) + (location.getZ() * Math.cos(theta)));
        } else if (rotation.equals("LEFT")) {
            theta = -1 * 0.5 * Math.PI;
            x = (int) Math.round((location.getX() * Math.cos(theta)) + (location.getZ() * (-1 * Math.sin(theta))));
            y = (int) location.getY();
            z = (int) Math.round((location.getX() * Math.sin(theta)) + (location.getZ() * Math.cos(theta)));
        } else {
            x = (int)location.getX();
            y = (int)location.getY();
            z = (int)location.getZ();
        }
        return new Location(location.getWorld(),x,y,z);
    }
    public static double[] rotateFlat90NoRound(final String rotation, double x, double z) {
        double theta;
        if (rotation.equals("RIGHT")) {
            theta = 0.5 * Math.PI;
        } else if (rotation.equals("LEFT")) {
            theta = -1 * 0.5 * Math.PI;
        } else {
            return new double[]{x, z};

        }

        double newX = (x * Math.cos(theta)) + (z * (-1 * Math.sin(theta)));
        double newZ = (x * Math.sin(theta)) + (z * Math.cos(theta));

        return new double[]{newX, newZ};
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
        Set<Block> outsideBlocks = new HashSet<>();
        Set<Block> outerHull = new HashSet<>();
        Set<Block> innerShip = new HashSet<>();
        scanHull(outsideBlocks, outerHull, innerShip);
        return innerShip;
    }

    public Set<Block> getExterior() {
        World world = ship.getWorld();
        if (world == null) return new HashSet<Block>();
        Set<Block> outsideBlocks = new HashSet<>();
        Set<Block> outerHull = new HashSet<>();
        Set<Block> innerShip = new HashSet<>();
        scanHull(outsideBlocks, outerHull, innerShip);
        return outsideBlocks;
    }

    public boolean isUnderwater() {
        return isUnderwater(true);
    }

    public boolean isUnderwater(boolean truly) {
        World world = ship.getWorld();
        if (world == null) return false;
        Set<Block> outsideBlocks = new HashSet<>();
        Set<Block> outerHull = new HashSet<>();
        Set<Block> innerShip = new HashSet<>();
        scanHull(outsideBlocks, outerHull, innerShip);
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
        Set<Block> outsideBlocks = new HashSet<>();
        Set<Block> outerHull = new HashSet<>();
        Set<Block> innerShip = new HashSet<>();
        fluidY = -127;
        scanHull(outsideBlocks, outerHull, innerShip);
        for (Block b : outsideBlocks) {
            if (m.isDataMedium(b.getBlockData())) {
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
        World world = ship.getWorld();
        if (world == null) return;
        Set<Block> allBlocks = new HashSet<>();
        Set<Block> nextLayer = new HashSet<>();
        int x1 = ship.minX(); int y1 = ship.minY(); int z1 = ship.minZ();
        int x2 = ship.maxX(); int y2 = ship.maxY(); int z2 = ship.maxZ();
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
            Set<Block> thisLayer = new HashSet<>(nextLayer);
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
        if (ship.isAirship) return;
        if (ship.isLandcraft) return;
        if (ship.CHANGE_WORLD) return;
        int MIN_BY = ship.minY();
        if (MIN_BY < -127) return;
        ship.model.minY = MIN_BY;

        int MAX_BY = ship.maxY();
        if (MAX_BY < -127) return;
        ship.model.maxY = MAX_BY;
        ship.updateWaterLogging();
        Map<String,Number> DATA = ShipWheelManager.getStateBuoyancyData(vehicleLoc, ship.BLOCK_STATES, ship.model.totalWeight);
        this.CURRENT_FLUID_LEVEL = this.getFluidLevel(getPrimaryMediumData(ship.BLOCK_STATES));

        double waterSurfaceY = (double)this.CURRENT_FLUID_LEVEL;
        float heightY = MAX_BY - MIN_BY;
        Float floatOffset = -1f;
        Number floatOff = DATA.getOrDefault("surface_offset",null);
        if (floatOff == null) floatOffset = (float)(-Math.round(heightY/3.25f)) + (float)Math.round((heightY/3f));
        else floatOffset = -1*(floatOff.floatValue());
        // Target Y position: water surface + float offset
        
        double currentY = (double)MIN_BY;
        double targetY = CURRENT_FLUID_LEVEL + floatOffset;
        double yDifference = Math.abs(targetY - currentY);
        if (SmoothSailingPlugin.DEBUG_DUMP) {
            System.out.println("SHIP HEIGHT: "+heightY);
            System.out.println("WATER SURFACE Y: "+waterSurfaceY);

            System.out.println("WATER LEVEL DIFFERENCE: "+yDifference);

            System.out.println("FLOAT OFFSET: "+(double)floatOffset);
            System.out.println("TARGET FLOAT Y: "+targetY);
        }
        if (yDifference >= 1d) {
            if (MIN_BY > targetY) {
                currentYVelocity = -1f; // Gravity
            } else if (MIN_BY <= targetY+1) {
                currentYVelocity = 1f; // Floatation
            } else {
                currentYVelocity = 0f;
            }
        }
    }

    /**
     * Apply ship velocity to players standing on deck.
     * Called per-collider from updateCollisionPositions().
     */
    public static double NEARBY_DISTANCE_SQ = 4.5*4.5d;

    public void applyDeckPhysics() {
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

    public void snapToFineGrid() {
        alignToGrid();
    }
    public void alignToGrid() {
        ship.vehicle.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
        currentSpeed = 0.0f;
        currentRotationVelocity = 0.0f;
        currentYVelocity = 0.0f;
        applyDeckPhysics();
    }
}
