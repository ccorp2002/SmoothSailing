package com.ccorp2002.smoothsailing.detection;

import com.ccorp2002.smoothsailing.*;
import com.ccorp2002.smoothsailing.ship.*;
import com.ccorp2002.smoothsailing.utils.*;
import com.ccorp2002.smoothsailing.nmsutils.*;
import com.ccorp2002.smoothsailing.ShipModel;
import com.ccorp2002.smoothsailing.ShipRegistry;
import com.ccorp2002.smoothsailing.ShipListener;
import com.ccorp2002.smoothsailing.detection.BlockConfigManager;
import com.ccorp2002.smoothsailing.detection.BlockProperties;
import com.ccorp2002.smoothsailing.detection.ShipDetector;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

/**
 * Manages placed ship wheels in the world.
 * Handles the assembly/disassembly of custom ships from ship wheel blocks.
 */
public class ShipWheelManager {

    private static JavaPlugin plugin;
    public final Map<String, ShipWheelData> placedWheels;  // Location key -> wheel data

    // Particle colors for ship detection visualization
    private static final Color PARTICLE_WHITE = Color.fromRGB(255, 255, 255);
    private static final Color PARTICLE_ORANGE = Color.fromRGB(255, 165, 0);
    private static final Color PARTICLE_RED = Color.fromRGB(255, 50, 50);

    public ShipWheelManager(JavaPlugin plugin) {
        ShipWheelManager.plugin = plugin;
        this.placedWheels = new HashMap<>();
    }
    /**
     * Creates a stable string key from a Location using block coordinates.
     * Avoids floating-point precision issues with Location as HashMap key.
     */
    public static String locationKey(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    /**
     * Registers a ship wheel at the given location with its facing direction.
     * The block itself should already be placed by the event handler.
     */
    public boolean placeWheel(Location location, BlockFace facing) {
        // Create and store wheel data
        ShipWheelData wheelData = new ShipWheelData(location, facing);
        placedWheels.put(locationKey(location), wheelData);
        return true;
    }

    /**
     * Removes a ship wheel at the given location.
     */
    public void removeWheel(Location location) {
        ShipWheelData wheelData = placedWheels.remove(locationKey(location));
        if (wheelData != null) {
            // If assembled, destroy the ship too
            if (wheelData.isAssembled()) {
                ShipInstance ship = ShipRegistry.byId(wheelData.getAssembledShipUUID());
                if (ship != null) {
                    ship.destroy();
                }
            }
        }
    }

    /**
     * Removes a ship wheel at the given location.
     */
    public void clearWheels() {
        placedWheels.clear();
    }

    /**
     * Breaks a ship wheel block after the ship has already been disassembled.
     * Removes from tracking, drops the wheel item, and sets block to air.
     * Use this instead of removeWheel() when the ship is already destroyed/disassembled.
     */
    public void breakWheelBlock(Location location) {
        ShipWheelData wheelData = placedWheels.remove(locationKey(location));
        if (wheelData == null) return;

        // Drop ship wheel item
    }

    /**
     * Gets wheel data at a location, if it exists.
     */
    public ShipWheelData getWheelAt(Location location) {
        return placedWheels.get(locationKey(location));
    }

    /**
     * Gets all placed wheels.
     */
    public Collection<ShipWheelData> getWheels() {
        return placedWheels.values();
    }

    /**
     * Gets wheel data by assembled ship UUID.
     * Used to find the wheel by the Ship Entity
     */
    public ShipWheelData getWheelByShipUUID(UUID shipUUID) {
        for (ShipWheelData wheelData : placedWheels.values()) {
            if (shipUUID.equals(wheelData.getAssembledShipUUID())) {
                return wheelData;
            }
        }
        return null;
    }

    /**
     * Updates the tracked location of a wheel after disassembly at a new position.
     * Removes old map entry and adds new one.
     */
    public void updateWheelLocation(ShipWheelData wheelData, Location newLocation, BlockFace newFacing) {
        if (wheelData == null) return;
        Location oldLocation = wheelData.getBlockLocation();
        placedWheels.remove(locationKey(oldLocation));
        wheelData.updateBlockLocation(newLocation, newFacing);
        placedWheels.put(locationKey(newLocation), wheelData);
    }

    public ShipInstance dryFireAssemble(ShipWheelData wheelData) {
        return dryFireAssemble(null,wheelData);   
    }

    public ShipInstance dryFireAssemble(Player pilot, ShipWheelData wheelData) {
        if (wheelData == null) return null;
        if (wheelData.isAssembled()) return null;
        Location wheelLoc = wheelData.getBlockLocation();
        BlockFace facing = DirectionalUtils.getBlockDataFace(wheelLoc.getBlock().getBlockData());
        if (facing == BlockFace.SELF) facing = wheelData.getFacing();
        wheelData.facing = facing;
        // Scan the structure
        ShipModel model;
        if (pilot != null) model = BlockStructureScanner.scanForStructure(pilot, wheelLoc, facing); //BlockStructureScanner.scanStructure(wheelLoc, facing);
        else model = BlockStructureScanner.scanForStructure(wheelLoc, facing);
        if (model == null || model.blocks.isEmpty()) {;
            return null;
        }

        // Set spawn location yaw to match wheel facing direction
        // This ensures the vehicle spawns facing the correct direction
        float assemblyYaw = BlockStructureScanner.blockFaceToYaw(facing);
        wheelLoc.setYaw(assemblyYaw);
        org.bukkit.entity.Entity vehicle = null;
        if (vehicle == null) {
            Location driverSeat = wheelLoc.clone().toBlockLocation();
            // Move one block behind the wheel (opposite of facing direction)
            //driverSeat.add(facing.getOppositeFace().getModX(), -0.25, facing.getOppositeFace().getModZ());
            driverSeat.add(0.5,-0.75,0.5);

            vehicle = ShipInstance.addEntity(driverSeat, org.bukkit.entity.Slime.class, s -> {
                s.setAI(false);
                s.setSilent(true);
                if (s instanceof Mob m) m.setAware(false);
                s.setGravity(false);
                s.setInvisible(true);
                s.setPersistent(true);
                s.setCollidable(false);
                s.setInvulnerable(true);
                s.setCustomNameVisible(true);
                s.setCustomName("[Command Seat]");
                PDCUtils.setPDC(s,"destroyable","false");
                //s.setPeek(0);  // Prevent shulker from peeking/moving up
                if (s instanceof ArmorStand as) {
                    as.setInvulnerable(true);
                    as.setMarker(true);
                    as.setSmall(true);
                }
                PDCUtils.setPDC(s,"is_seat","true");
                // Apply scale attribute to change collision box size
                if (s instanceof LivingEntity le) {
                    le.setRemoveWhenFarAway(false);
                    org.bukkit.attribute.AttributeInstance scaleAttr = le.getAttribute(org.bukkit.attribute.Attribute.SCALE);
                    if (scaleAttr != null) scaleAttr.setBaseValue(1);
                }
                if (s instanceof org.bukkit.entity.Slime slime) {
                    var scaleAttr = slime.getAttribute(org.bukkit.attribute.Attribute.SCALE);
                    if (scaleAttr != null) scaleAttr.setBaseValue(1);
                    slime.setNoPhysics(true);
                    slime.setWander(false);
                    slime.setGlowing(true);  // Glow if debug mode enabled
                    slime.setSize(1);
                }
                if (s instanceof org.bukkit.entity.MagmaCube slime) {
                    var scaleAttr = slime.getAttribute(org.bukkit.attribute.Attribute.SCALE);
                    if (scaleAttr != null) scaleAttr.setBaseValue(1);
                    slime.setNoPhysics(true);
                    slime.setWander(false);
                    slime.setGlowing(true);  // Glow if debug mode enabled
                    slime.setSize(1);
                }
                if (s instanceof org.bukkit.entity.HappyGhast happy) {
                    var scaleAttr = happy.getAttribute(org.bukkit.attribute.Attribute.SCALE);
                    if (scaleAttr != null) scaleAttr.setBaseValue(0.25);
                    happy.setNoPhysics(true);
                }
                ShipListener.addTag(s,ShipListener.seatTag(0));
                s.setVisibleByDefault(true);
                s.setInvisible(true);
            });
        }
        ShipInstance ship = new ShipInstance(plugin, vehicle, model, vehicle.getUniqueId(), true);
        wheelData.setLastDetectedBlocks(new HashSet<Location>(model.blocks));
        wheelData.lastDetectedWeight = (int)ship.weightCalc;

        com.ccorp2002.smoothsailing.region.TaskUtils.getRegionalScheduler(ship.vehicle.getLocation()).runTaskLater(()->{
            ship.destroy();
        },60L);
        return ship;
    }

    public ShipInstance assembleFromBlocks(Player pilot, Location CENTER_POINT, Collection<Block> BLOCKS) {
        Location wheelLoc = CENTER_POINT.toCenterLocation();
        BlockFace facing = DirectionalUtils.getBlockDataFace(NmsUtils.toBukkitBlockDataFast(CENTER_POINT));
        if (facing == BlockFace.SELF) facing = DirectionalUtils.yawToBlockFace(CENTER_POINT.getYaw());
        List<Location> POSITS = new ArrayList<>(BLOCKS.size());
        BLOCKS.forEach(b ->{ 
            POSITS.add(b.getLocation());
        });
        // Scan the structure
        ShipModel model = new ShipModel(
            POSITS,
            0f,
            1,
            CENTER_POINT.getBlockY() - 2,
            CENTER_POINT.getBlockY() + 2
        );
        if (model == null || model.blocks.isEmpty()) {;
            return null;
        }

        // Set spawn location yaw to match wheel facing direction
        // This ensures the vehicle spawns facing the correct direction
        float assemblyYaw = BlockStructureScanner.blockFaceToYaw(facing);
        wheelLoc.setYaw(assemblyYaw);
        org.bukkit.entity.Entity vehicle = null;
        if (vehicle == null) {

            vehicle = ShipInstance.addEntity(wheelLoc, org.bukkit.entity.Slime.class, s -> {
                s.setAI(false);
                s.setSilent(true);
                if (s instanceof Mob m) m.setAware(false);
                s.setGravity(false);
                s.setInvisible(true);
                s.setPersistent(true);
                s.setCollidable(false);
                s.setInvulnerable(true);
                s.setCustomNameVisible(true);
                s.setCustomName("[Command Seat]");
                PDCUtils.setPDC(s,"destroyable","false");
                //s.setPeek(0);  // Prevent shulker from peeking/moving up
                if (s instanceof ArmorStand as) {
                    as.setInvulnerable(true);
                    as.setMarker(true);
                    as.setSmall(true);
                }
                PDCUtils.setPDC(s,"is_seat","true");
                // Apply scale attribute to change collision box size
                if (s instanceof LivingEntity le) {
                    le.setRemoveWhenFarAway(false);
                    org.bukkit.attribute.AttributeInstance scaleAttr = le.getAttribute(org.bukkit.attribute.Attribute.SCALE);
                    if (scaleAttr != null) scaleAttr.setBaseValue(1);
                }
                if (s instanceof org.bukkit.entity.Slime slime) {
                    var scaleAttr = slime.getAttribute(org.bukkit.attribute.Attribute.SCALE);
                    if (scaleAttr != null) scaleAttr.setBaseValue(1);
                    slime.setNoPhysics(true);
                    slime.setWander(false);
                    slime.setGlowing(true);  // Glow if debug mode enabled
                    slime.setSize(1);
                }
                if (s instanceof org.bukkit.entity.MagmaCube slime) {
                    var scaleAttr = slime.getAttribute(org.bukkit.attribute.Attribute.SCALE);
                    if (scaleAttr != null) scaleAttr.setBaseValue(1);
                    slime.setNoPhysics(true);
                    slime.setWander(false);
                    slime.setGlowing(true);  // Glow if debug mode enabled
                    slime.setSize(1);
                }
                if (s instanceof org.bukkit.entity.HappyGhast happy) {
                    var scaleAttr = happy.getAttribute(org.bukkit.attribute.Attribute.SCALE);
                    if (scaleAttr != null) scaleAttr.setBaseValue(0.25);
                    happy.setNoPhysics(true);
                }
                ShipListener.addTag(s,ShipListener.seatTag(0));
                s.setVisibleByDefault(true);
                s.setInvisible(true);
            });
        }
        ShipInstance ship = new ShipInstance(plugin, vehicle, model, vehicle.getUniqueId(), false);
        return ship;
    }

    /**
     * Assembles a custom ship from blocks around the wheel.
     */
    public boolean assembleShip(final Player player, ShipWheelData wheelData) {
        return assembleShip(player,wheelData,false);
    }

    /**
     * Assembles a custom ship from blocks around the wheel.
     */
    public boolean assembleShip(final Player player, ShipWheelData wheelData, boolean dryFire) {
        if (wheelData.isAssembled()) {
            if (player != null) player.sendMessage("§cThis wheel already has an assembled ship!");
            return false;
        }
        final Location wheelLoc = wheelData.getBlockLocation();
        Location tempLoc = wheelLoc.clone();
        if (MobileBlockState.getShipFromLoc(tempLoc) != null) {
            if (player != null) player.sendMessage("§cThis wheel already has an assembled ship!");
            return false;
        }
        tempLoc.setY(wheelLoc.getY()+1);
        if (MobileBlockState.getShipFromLoc(tempLoc) != null) {
            if (player != null) player.sendMessage("§cThis wheel already has an assembled ship!");
            return false;
        }
        tempLoc.setY(wheelLoc.getY()-1);
        if (MobileBlockState.getShipFromLoc(tempLoc) != null) {
            if (player != null) player.sendMessage("§cThis wheel already has an assembled ship!");
            return false;
        }
        org.bukkit.block.data.BlockData blockdata = wheelLoc.getBlock().getBlockData();
        BlockFace facing = DirectionalUtils.getBlockDataFace(blockdata);
        if (blockdata.getMaterial() == Material.PLAYER_HEAD) facing = facing.getOppositeFace();
        if (facing == BlockFace.SELF) facing = wheelData.getFacing();
        wheelData.facing = facing.getOppositeFace();
        // Scan the structure
        final float airDensity = (float) ShipInstance.airDensity();
        ShipModel model = BlockStructureScanner.scanForStructure(player, tempLoc, facing); //BlockStructureScanner.scanStructure(wheelLoc, facing);
        if (model == null || model.blocks.isEmpty()) {
            if (player != null) player.sendMessage("§cNo valid ship structure found!");
            return false;
        }

        // Set spawn location yaw to match wheel facing direction
        // This ensures the vehicle spawns facing the correct direction
        float assemblyYaw = BlockStructureScanner.blockFaceToYaw(wheelData.facing);
        wheelLoc.setYaw(assemblyYaw);
        org.bukkit.entity.Entity vehicle = null;
        boolean earlyIsAirship = (model.getDensity() < airDensity);

        // Create a ShipInstance from the model BEFORE removing blocks
        // This allows us to transfer leads while the fence blocks (and LeashHitch) still exist
        // shipType is "custom" for custom block ships
        // Pass empty customization - custom ships use scanned blocks as-is (no wood type replacement)
        // Display/collision offsets are applied inside ShipInstance based on config
        if (vehicle == null) {
            Location driverSeat = wheelLoc.clone().toBlockLocation();
            // Move one block behind the wheel (opposite of facing direction)
            driverSeat.add(wheelData.facing.getOppositeFace().getModX(), -0.125, wheelData.facing.getOppositeFace().getModZ());
            driverSeat.add(0.5,-0.25,0.5);
            Class<? extends org.bukkit.entity.Entity> clazz = null;
            if (earlyIsAirship) clazz = org.bukkit.entity.Slime.class;
            else clazz = org.bukkit.entity.Slime.class;
            vehicle = ShipInstance.addEntity(driverSeat, clazz, s -> {
                s.setSilent(true);
                if (s instanceof Mob m) m.setAware(false);
                s.setGravity(false);
                s.setInvisible(true);
                s.setPersistent(true);
                s.setInvulnerable(true);
                s.setCustomNameVisible(true);
                s.setCustomName("[Command Seat]");
                PDCUtils.setPDC(s,"destroyable","false");
                //s.setPeek(0);  // Prevent shulker from peeking/moving up
                if (s instanceof ArmorStand as) {
                    as.setInvulnerable(true);
                    as.setMarker(true);
                    as.setSmall(true);
                    var scaleAttr = as.getAttribute(org.bukkit.attribute.Attribute.SCALE);
                    if (scaleAttr != null) scaleAttr.setBaseValue(1.5);
                    as.setDisabledSlots(org.bukkit.inventory.EquipmentSlot.values());
                    as.getEquipment().setHelmet(ItemStack.of(Material.ORANGE_STAINED_GLASS));
                }
                PDCUtils.setPDC(s,"is_seat","true");
                // Apply scale attribute to change collision box size
                if (s instanceof LivingEntity le) {
                    le.setAI(false);
                    le.setCollidable(false);
                    org.bukkit.attribute.AttributeInstance scaleAttr = le.getAttribute(org.bukkit.attribute.Attribute.SCALE);
                    if (scaleAttr != null) scaleAttr.setBaseValue(1);
                    le.setRemoveWhenFarAway(false);
                    le.setGlowing(true);
                }
                if (s instanceof org.bukkit.entity.Slime slime) {
                    var scaleAttr = slime.getAttribute(org.bukkit.attribute.Attribute.SCALE);
                    if (scaleAttr != null) scaleAttr.setBaseValue(1.5);
                    slime.setNoPhysics(true);
                    slime.setWander(false);
                    slime.setGlowing(true);  // Glow if debug mode enabled
                    slime.setSize(1);
                }
                if (s instanceof org.bukkit.entity.MagmaCube slime) {
                    var scaleAttr = slime.getAttribute(org.bukkit.attribute.Attribute.SCALE);
                    if (scaleAttr != null) scaleAttr.setBaseValue(1.5);
                    slime.setNoPhysics(true);
                    slime.setWander(false);
                    slime.setGlowing(true);  // Glow if debug mode enabled
                    slime.setSize(1);
                }
                if (s instanceof org.bukkit.entity.HappyGhast happy) {
                    var scaleAttr = happy.getAttribute(org.bukkit.attribute.Attribute.SCALE);
                    if (scaleAttr != null) scaleAttr.setBaseValue(0.25);
                    happy.setNoPhysics(true);
                }
                ShipListener.addTag(s,ShipListener.seatTag(0));
                s.setCustomNameVisible(false);
                s.setVisibleByDefault(true);
                s.setInvisible(true);
            });
        }
        Entity seat = vehicle;
        final ShipInstance ship = new ShipInstance(plugin, vehicle, model, vehicle.getUniqueId());
        ShipListener.addTag(seat,(ShipListener.shipTag(ship.id)));
        ShipListener.addTag(seat,(ShipListener.COLLIDER_TAG));
        ship.vehicle.setRotation(assemblyYaw,0f);
        ship.parent.setRotation(assemblyYaw,0f);
        ShipListener.addTag(vehicle,(ShipListener.shipTag(ship.id)));
        wheelData.setLastDetectedBlocks(new HashSet<Location>(model.blocks));
        wheelData.lastDetectedWeight = (int)ship.weightCalc;

        // Register the ship
        ShipRegistry.register(ship);
        // Link the wheel to the ship
        Entity actor = ship.vehicle;
        if (player != null) {
            actor = player;
        }
        com.ccorp2002.smoothsailing.event.ShipAssembleEvent asevent = new com.ccorp2002.smoothsailing.event.ShipAssembleEvent(ship,model.blocks,ship.vehicle.getLocation().toBlockLocation(),actor);
        asevent.callEvent();
        //System.out.println("INI: Y "+newLoc.getYaw()+" / P"+newLoc.getPitch());
        if (asevent.isCancelled()) {
            if (asevent.getEntity() instanceof Player playr) playr.sendMessage("§cShip assembly action denied!");
            disassembleShip(player,wheelData,true);
            return false;
        }
        seat.setCustomNameVisible(true);
        ship.ENTITY_COMPOSITION.add(seat);
        wheelData.setAssembledShipUUID(ship.id);
        
        if (asevent.getEntity() instanceof Player playr) ship.driver = playr;

        if (player != null) {
            ship.driver = player;
            com.ccorp2002.smoothsailing.region.TaskUtils.getGlobalScheduler().runTaskLater(()->{
                player.sendMessage("§aShip assembled! Found " + model.blocks.size() + " blocks.");
                Location loc = player.getLocation();
                if (ship.isAirship && !ship.isLandcraft) {
                    player.sendMessage("§b✦ This vehicle is lighter than air - it will fly as an Airship!");
                    player.sendMessage("§7  Controls: Jump = Ascend | Sprint = Descend | W = Forwards | S = Backwards | A = Left | D = Right");
                } else if (ship.isLandcraft) {
                    player.sendMessage("§b✦ This vehicle is equipped with atleast 2 wheels - it will drive like a Car or Tank!");
                    player.sendMessage("§7  Controls: W = Forwards | S = Backwards | A = Left | D = Right");
                } else {
                    player.sendMessage("§b✦ This vehicle is a normal ship - it will sail at waterlevel!");
                    player.sendMessage("§7  Controls: W = Forwards | S = Backwards | A = Left | D = Right");
                }
                NmsUtils.teleportSilentCurrentThread(loc,player);
            },5L);
        }
        return true;
    }

    /**
     * Disassembles a ship back into blocks.
     */
    public boolean disassembleShip(@Nullable Player player, ShipWheelData wheelData) {
        return disassembleShip(player, wheelData, false);
    }

    /**
     * Disassembles a ship back into blocks.
     *
     * @param player The player disassembling the ship
     * @param wheelData The ship wheel data
     * @param force If true, destroys fragile blocks (grass, flowers, etc.) in the way.
     *              Hard conflicts will cause those ship blocks to be lost.
     * @return true if disassembly succeeded, false otherwise
     */
    public boolean disassembleShip(@Nullable Player player, ShipWheelData wheelData, boolean force) {
        ShipInstance ship = ShipRegistry.byId(wheelData.getAssembledShipUUID());
        if (ship == null && player != null) {
            ship = SmoothSailingPlugin.shipListener().getShipFromEntity(player);
        }
        if (!wheelData.isAssembled() && ship == null) {
            if (player != null) player.sendMessage("§cNo ship to disassemble!");
            return false;
        }

        if (ship == null) {
            if (player != null) player.sendMessage("§cShip not found!");
            wheelData.setAssembledShipUUID(null);
            return false;
        }
        Entity actor = ship.vehicle;
        if (player != null) {
            player.closeInventory();
            actor = player;
        }
        com.ccorp2002.smoothsailing.event.ShipDisassembleEvent disevent = new com.ccorp2002.smoothsailing.event.ShipDisassembleEvent(ship,ship.vehicle.getLocation(),player);
        disevent.callEvent();
        if (disevent.isCancelled()) {
            if (player != null) player.sendMessage("§cDisassembly action denied!");
            return false;
        }
        for (CustomEntityInventoryHolder holder : CustomEntityInventoryHolder.CUSTOM_INVENTORY_CACHE.values()) {
            holder.saveInventory();
        }

        // Get the ship's current location and rotation
        Location shipLoc = ship.getLocation();
        float currentYaw = shipLoc.getYaw();

        // Place the blocks back (with rotation)
        final Location shipLocf = shipLoc;

        // Update wheel tracking to new location
        Location newWheelLocation = shipLocf.toBlockLocation();
        updateWheelLocation(wheelData, newWheelLocation, ship.facing);


        // Remove ship from per-world storage (delete file and chunk index)
        if (!ship.vehicle.isDead() && !force) {
            if (plugin instanceof SmoothSailingPlugin bsp) {
                org.bukkit.World world = shipLocf.getWorld();
                if (world != null) {
                    shipLoc = ship.vehicle.getLocation();
                    shipLoc.setY(shipLoc.getY());
                }
            }
        }
        ShipInstance fship = ship;
        com.ccorp2002.smoothsailing.region.TaskUtils.getGlobalScheduler().runTaskLater(()->{
            ShipRegistry.unregister(fship);
            // Destroy the ship
            // Unlink from wheel
            wheelData.setAssembledShipUUID(null);
            fship.destroy();
        },3L);

        if (player != null) player.sendMessage("§aShip disassembled!");
        return true;
    }
    /**
     * Detects and previews which blocks would be included in a ship.
     * Shows block count, total weight, and spawns particles to visualize the ship.
     */
    public boolean detectShip(Player player, ShipWheelData wheelData) {
        return detectShip(player, wheelData, true);
    }

    /**
     * Detects and previews which blocks would be included in a ship.
     * Shows block count, total weight, and optionally spawns particles to visualize the ship.
     *
     * @param player The player to send messages to
     * @param wheelData The ship wheel data
     * @param showParticles Whether to show particle visualization
     */
    public boolean detectShip(Player player, ShipWheelData wheelData, boolean showParticles) {

        // Cancel any existing particle task
        wheelData.cancelParticleTask();

        // If ship is assembled, get stats from the ship instance instead of world detection
        // (world blocks are removed when ship is assembled)
        if (wheelData.isAssembled()) {
            ShipInstance ship = ShipRegistry.byId(wheelData.getAssembledShipUUID());
            if (ship == null && player != null) {
                ship = SmoothSailingPlugin.shipListener().getShipFromEntity(player);
            }
            if (ship != null && ship.vehicle != null && ship.vehicle.isValid()) {
                int blockCount = ship.blockCount;
                double maxHealth = ship.maxHealth;
                double currentHealth = ship.currentHealth;
                wheelData.setLastDetectedStats(blockCount, ship.model.totalWeight, (int)maxHealth);
                wheelData.setLastHealth(currentHealth, maxHealth);
                // Store buoyancy data from ship model
                wheelData.lastMinY = ship.model.minY;
                wheelData.lastSurfaceOffset = ship.model.waterFloatOffset;
                return true;
            }
        }

        // Run ship detection
        ShipInstance ship = dryFireAssemble(player, wheelData);
        if (ship == null) {
            if (player != null) player.sendMessage("§cNo valid blocks found for ship!");
            return false;
        }
        Location wheelLoc = wheelData.getBlockLocation();
        BlockFace facing = DirectionalUtils.getBlockDataFace(wheelLoc.getBlock().getBlockData());
        if (facing == BlockFace.SELF) facing = wheelData.getFacing();
        wheelData.facing = facing;

        int maxShipSize = plugin.getConfig().getInt("max-ship-size", 1000);
        int maxScanSize = plugin.getConfig().getInt("max-scan-size", 5000);
        float meanDensity = ship.model.getDensity();
        int weightedBlockCount = ship.model.blockCount;
        int totalWeight = (int)ship.weightCalc;
        ShipConfig config = ShipInstance.config;
        boolean isAirship = ship.isAirship;
        boolean isLandcraft = ship.isLandcraft;
        Set<Location> shipBlocks = new HashSet<>(ship.model.blocks);
        wheelData.lastMaxHealth = ship.maxHealth;
        wheelData.lastCurrentHealth = ship.maxHealth;
        Location driverSeat = wheelLoc.clone().toBlockLocation();
        wheelData.setLastHealth(ship.maxHealth, ship.maxHealth);
        // Move one block behind the wheel (opposite of facing direction)
        driverSeat.add(facing.getOppositeFace().getModX(), -0.25, facing.getOppositeFace().getModZ());
        driverSeat.add(0.5,-0.25,0.5);


        // Send success messages
        player.sendMessage("§aShip detected successfully!");
        player.sendMessage("§7Blocks: §f" + weightedBlockCount + " §7/ §f" + maxShipSize);
        player.sendMessage("§7Total Weight: §f" + totalWeight);
        player.sendMessage("§7Density: §f" + String.format("%.2f", meanDensity) + " §7(Air: " + config.airDensity + ", Water: " + config.waterDensity + ")");
        if (isAirship && !isLandcraft) {
            player.sendMessage("§b✦ This vehicle is lighter than air - it will fly as an Airship!");
            player.sendMessage("§7  Controls: Jump = Ascend | Sprint = Descend | W = Forwards | S = Backwards | A = Left | D = Right");
        } else if (isLandcraft) {
            player.sendMessage("§b✦ This vehicle is equipped with atleast 2 wheels - it will drive like a Car or Tank!");
            player.sendMessage("§7  Controls: W = Forwards | S = Backwards | A = Left | D = Right");
        } else {
            player.sendMessage("§b✦ This vehicle is a normal ship - it will sail at waterlevel!");
            player.sendMessage("§7  Controls: W = Forwards | S = Backwards | A = Left | D = Right");
        }

        // Store detected blocks and stats for Ship Info display
        int positiveWeight = calculatePositiveWeight(shipBlocks);
        wheelData.setLastDetectedBlocks(shipBlocks);
        wheelData.setLastDetectedStats(weightedBlockCount, totalWeight, positiveWeight);
        wheelData.setLastDetectedBlockCategories(shipBlocks, new HashSet<Location>(0), driverSeat);

        // Calculate and store buoyancy data for Ship Info display
        calculateAndStoreBuoyancyData(wheelData, shipBlocks, totalWeight, weightedBlockCount);

        if (showParticles) {
            player.sendMessage("§7(Showing particles for ~5 seconds...)");

            // Start particle visualization
            startParticleVisualization(shipBlocks);
        }
        if (plugin instanceof SmoothSailingPlugin bsp) {
            org.bukkit.World world = ship.vehicle.getWorld();
        }
        ship.destroy();

        return true;
    }

    /**
     * Calculate the total weight of all blocks in the ship.
     */
    private int calculateTotalWeight(Set<Location> blocks) {
        BlockConfigManager configManager = BlockConfigManager.getInstance();
        int totalWeight = 0;

        for (Location loc : blocks) {
            Block block = loc.getBlock();
            BlockProperties props = configManager.getProperties(block.getType(), block.getBlockData());
            totalWeight += props.getWeight();
        }

        return totalWeight;
    }

    /**
     * Count blocks that have a defined weight (used for density calculation).
     */
    private int countWeightedBlocks(Set<Location> blocks) {
        BlockConfigManager configManager = BlockConfigManager.getInstance();
        int count = 0;

        for (Location loc : blocks) {
            Block block = loc.getBlock();
            BlockProperties props = configManager.getProperties(block.getType(), block.getBlockData());
            if (props.hasWeight()) {
                count++;
            }
        }

        return count;
    }

    /**
     * Calculate the sum of positive weights (used for health calculation).
     * Blocks with negative or zero weight contribute nothing to health.
     */
    private int calculatePositiveWeight(Set<Location> blocks) {
        BlockConfigManager configManager = BlockConfigManager.getInstance();
        int positiveWeight = 0;

        for (Location loc : blocks) {
            Block block = loc.getBlock();
            BlockProperties props = configManager.getProperties(block.getType(), block.getBlockData());
            if (props.hasWeight()) {
                int w = props.getWeight();
                if (w > 0) {
                    positiveWeight += w;
                }
            }
        }

        return positiveWeight;
    }
    /**
     * Calculate and store buoyancy data (centerOfVolumeY, minY, surfaceOffset) for Ship Info display.
     */
    public static Map<String,Number> getBuoyancyData(Location vehicleLoc, Set<Location> shipBlocks, int totalWeight) {
        BlockConfigManager configManager = BlockConfigManager.getInstance();
        vehicleLoc = vehicleLoc.toBlockLocation();
        float minY = Float.MAX_VALUE;
        float sumY = 0;
        int weightedCount = 0;

        for (Location bstate : shipBlocks) {
            BlockProperties props = configManager.getProperties(bstate.getBlock().getType(), bstate.getBlock().getBlockData());
            if (!props.isAllowed()) continue;

            float blockY = (float) (bstate.getBlockY() - vehicleLoc.getY());
            if (blockY < minY) minY = blockY;

            if (props.hasWeight()) {
                weightedCount++;
                sumY += blockY;
            }
        }

        if (minY == Float.MAX_VALUE) minY = 0;
        Map<String,Number> DATA = new HashMap<>();
        float centerOfVolumeY = weightedCount > 0 ? sumY / weightedCount : 0;

        // Calculate surface offset using same formula as ShipPhysics
        float surfaceOffset;
        if (weightedCount > 0) {
            float meanDensity = (float) totalWeight / weightedCount;
            Float airDensity = (float)ShipInstance.airDensity();
            Float waterDensity = (float)ShipInstance.waterDensity();
            if (airDensity == null) airDensity = 0f;
            if (waterDensity == null) waterDensity = 2.5f;

            float t = (meanDensity - airDensity) / (waterDensity - airDensity);
            float referenceY = minY;
            float waterlineY = referenceY + t * (centerOfVolumeY - referenceY);
            surfaceOffset = (float)-Math.ceil(waterlineY);
        } else {
            surfaceOffset = 0;
        }
        DATA.put("center_of_volume_y",centerOfVolumeY);
        DATA.put("min_y",minY);
        DATA.put("surface_offset",surfaceOffset);
        return DATA;
    }

    /**
     * Calculate and store buoyancy data (centerOfVolumeY, minY, surfaceOffset) for Ship Info display.
     */
    public static Map<String,Number> getStateBuoyancyData(Location vehicleLoc, Set<MobileBlockState> shipBlocks, int totalWeight) {
        BlockConfigManager configManager = BlockConfigManager.getInstance();
        vehicleLoc = vehicleLoc.toBlockLocation();
        float minY = Float.MAX_VALUE;
        float sumY = 0;
        int weightedCount = 0;
        for (MobileBlockState bstate : shipBlocks) {
            BlockProperties props = configManager.getProperties(bstate.getBlockData().getMaterial(), bstate.getBlockData());
            if (!props.isAllowed()) continue;
            float blockY = (float) (bstate.getBlockY() - vehicleLoc.getY());
            if (blockY < minY) minY = blockY;

            if (props.hasWeight()) {
                weightedCount++;
                sumY += blockY;
            }
        }

        if (minY == Float.MAX_VALUE) minY = 0;
        Map<String,Number> DATA = new HashMap<>();
        float centerOfVolumeY = weightedCount > 0 ? sumY / weightedCount : 0;

        // Calculate surface offset using same formula as ShipPhysics
        float surfaceOffset;
        if (weightedCount > 0) {
            float meanDensity = (float) totalWeight / weightedCount;
            Float airDensity = (float)ShipInstance.airDensity();
            Float waterDensity = (float)ShipInstance.waterDensity();
            if (airDensity == null) airDensity = 0f;
            if (waterDensity == null) waterDensity = 2.5f;

            float t = (meanDensity - airDensity) / (waterDensity - airDensity);
            float referenceY = minY;
            float waterlineY = referenceY + t * (centerOfVolumeY - referenceY);
            surfaceOffset = (float)-Math.ceil(waterlineY);
        } else {
            surfaceOffset = 0;
        }
        DATA.put("center_of_volume_y",centerOfVolumeY);
        DATA.put("min_y",minY);
        DATA.put("surface_offset",surfaceOffset);
        return DATA;
    }

    /**
     * Calculate and store buoyancy data (centerOfVolumeY, minY, surfaceOffset) for Ship Info display.
     */
    public static void calculateAndStoreBuoyancyData(ShipWheelData wheelData, Set<Location> shipBlocks, int totalWeight, int weightedBlockCount) {
        BlockConfigManager configManager = BlockConfigManager.getInstance();
        Location wheelLoc = wheelData.getBlockLocation().toBlockLocation();
        float minY = Float.MAX_VALUE;
        float sumY = 0;
        int weightedCount = 0;

        for (Location bstate : shipBlocks) {
            BlockProperties props = configManager.getProperties(bstate.getBlock().getType(), bstate.getBlock().getBlockData());
            if (!props.isAllowed()) continue;
            float blockY = (float) (bstate.getBlockY() - wheelLoc.getY());
            if (blockY < minY) minY = blockY;

            if (props.hasWeight()) {
                weightedCount++;
                sumY += blockY;
            }
        }

        if (minY == Float.MAX_VALUE) minY = 0;
        Map<String,Number> DATA = new HashMap<>();
        float centerOfVolumeY = weightedCount > 0 ? sumY / weightedCount : 0;

        // Calculate surface offset using same formula as ShipPhysics
        float surfaceOffset;
        if (weightedCount > 0) {
            float meanDensity = (float) totalWeight / weightedCount;
            Float airDensity = (float)ShipInstance.airDensity();
            Float waterDensity = (float)ShipInstance.waterDensity();
            if (airDensity == null) airDensity = 0f;
            if (waterDensity == null) waterDensity = 2.5f;

            float t = (meanDensity - airDensity) / (waterDensity - airDensity);
            float referenceY = minY;
            float waterlineY = referenceY + t * (centerOfVolumeY - referenceY);
            surfaceOffset = (float)-Math.ceil(waterlineY);
        } else {
            surfaceOffset = 0;
        }

        wheelData.lastMinY = minY;
        wheelData.lastSurfaceOffset = surfaceOffset;
    }

    /**
     * Starts a repeating task to spawn particles on detected blocks.
     * Runs for 5 seconds (10 iterations × 0.5s).
     * Uses different colors: white for regular blocks, orange for passenger seats, red for driver seat.
     */
    public void startParticleVisualization(final Set<Location> shipBlocks) {
        final int iterationsLeft = 15;  // 25 iterations × 10 ticks = 5.5 seconds

        for (int iter = 0; iter <= iterationsLeft; iter++) {
            final int iteration = iterationsLeft - iter;
            BukkitRunnable particleTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (iteration <= 0) {
                        // Done, clean up

                        // Spawn white particles on regular blocks
                        NmsUtils.sendBlockDataChanges(shipBlocks,null);
                        for (Location blockLoc : shipBlocks) {
                            NmsUtils.sendBlockChange(blockLoc,blockLoc.getBlock().getBlockData());
                            spawnBlockParticles(blockLoc, PARTICLE_WHITE);
                        }
                        this.cancel();
                        return;
                    }
                    NmsUtils.sendBlockDataChanges(shipBlocks,Material.LIME_STAINED_GLASS.createBlockData());
                }
            };
            if (SmoothSailingPlugin.getPlugin() != null) {
                particleTask.runTaskLater(SmoothSailingPlugin.getPlugin(), (long)(20L)+(iter*iterationsLeft));
            }
        }
    }

    /**
     * Spawns particles at the 8 corners of a block with the specified color.
     */
    public static void spawnBlockParticles(Location blockLoc, Color color) {
        if (blockLoc.getWorld() == null) {
            return;
        }

        // 8 corners of a block
        double[][] corners = {
            {0, 0, 0},    // Bottom corners
            {1, 0, 0},
            {0, 0, 1},
            {1, 0, 1},
            {0, 1, 0},    // Top corners
            {1, 1, 0},
            {0, 1, 1},
            {1, 1, 1}
        };

        Particle.DustOptions dustOptions = new Particle.DustOptions(color, 1.0f);

        for (double[] corner : corners) {
            Location particleLoc = blockLoc.clone().add(corner[0], corner[1], corner[2]);

            // Spawn colored dust particle
            blockLoc.getWorld().spawnParticle(
                Particle.DUST,
                particleLoc,
                1,                      // Count
                0.05,                   // X spread
                0.05,                   // Y spread
                0.05,                   // Z spread
                0,                      // Speed (not used for DUST)
                dustOptions
            );
        }
    }
}
