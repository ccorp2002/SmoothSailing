package com.ccorp2002.smoothsailing;

import com.ccorp2002.smoothsailing.detection.BlockConfigManager;
import com.ccorp2002.smoothsailing.detection.BlockProperties;
import com.ccorp2002.smoothsailing.detection.*;
import com.ccorp2002.smoothsailing.ship.*;
import com.ccorp2002.smoothsailing.utils.*;
import com.ccorp2002.smoothsailing.nmsutils.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.entity.Projectile;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;


import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class ShipListener implements Listener {
    
    public static final String SHIP_PREFIX = "shiptag";
    public static final String SEAT_PREFIX = "shipseat";
    public static final String WHEEL_PREFIX = "shipwheel";
    public static final String PARENT_TAG = "shipparent";
    public static final String COLLIDER_TAG = "shipcollider";

    public static JavaPlugin plugin;

    public ShipListener(JavaPlugin plugin) {
        ShipListener.plugin = plugin;
    }

    // Tag creation helpers
    public static String shipTag(UUID id) {
        return SHIP_PREFIX +":"+ id.toString();
    }

    public static String seatTag(int index) {
        return SEAT_PREFIX +":"+ index;
    }

    public static String wheelTag(Location loc) {
        return WHEEL_PREFIX +":"+ loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    public static void addTag(Entity entity, String tag) {
        String keyParts[] = tag.split(":");
        String key = keyParts[0];
        if (key == null) {
            key = tag;
        }
        key = key.replace(":","_");
        PDCUtils.setPDC(entity,key,tag);
    }

    public static ShipInstance getShipFromEntity(Entity entity) {
        ShipInstance ship = null;
        ship = ShipRegistry.byId(entity.getUniqueId());
        if (ship == null) {
            for (ShipInstance other : ShipRegistry.getAllShips()) {
                if (other.ENTITY_COMPOSITION.contains(entity)) {
                    ship = other;
                    break;
                }
            }
        }
        if (SmoothSailingPlugin.DEBUG_DUMP && ship != null) System.out.println("GOT SHIP "+ship+" BY ENTITY "+entity.getType());
        return ship;
    }

    public static boolean hasWheelLocation(PersistentDataContainer tags) {
        String tag = PDCUtils.getPDC(tags,WHEEL_PREFIX);
        if (tag.startsWith(WHEEL_PREFIX)) {
            return true;
        }
        return false;
    }

    public static boolean isShipEntity(PersistentDataContainer tags) {
        String tag = PDCUtils.getPDC(tags,SHIP_PREFIX);
        if (tag.startsWith(SHIP_PREFIX)) {
            return true;
        }
        return false;
    }

    public static boolean isFunctional(Entity entity) {
        return isSeat(entity) || isWheel(entity) || isChair(entity);
    }

    public static boolean isChair(Entity entity) {
        return PDCUtils.getPDC(entity,"is_chair").equals("true");
    }

    public static boolean isSeat(Entity entity) {
        return PDCUtils.getPDC(entity,"is_seat").equals("true");
    }

    public static boolean isWheel(Entity entity) {
        return hasWheelLocation(entity.getPersistentDataContainer());
    }

    public static void shutdown() {
        ShipRegistry.destroyAll();
    }
    @EventHandler
    public void onWorldUnload(org.bukkit.event.world.WorldUnloadEvent event) {
        ShipRegistry.destroyAllInWorld(event.getWorld());
    }
    /**
     * Handles chunk unload events.
     * Suspends ship tasks and unregisters from ShipRegistry.
     * Entities persist naturally in the chunk - we just lose our Java references.
     */
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        org.bukkit.Chunk chunk = event.getChunk();
        for (ShipInstance ship : ShipRegistry.getShipsInChunk(chunk)) {
            if (!ship.vehicle.getWorld().equals(chunk.getWorld())) {
                continue;
            } else if (ship.vehicle == null || ship.vehicle.isDead()) {
                ShipRegistry.unregister(ship);
                ship.destroy();
                plugin.getLogger().warning("Released ship " + ship.id + " for chunk unload at " + chunk.getX() + "," + chunk.getZ() + "," + chunk.getWorld().getName());
                continue;
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerInput(org.bukkit.event.player.PlayerInputEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();
        Entity vehicle = player.getVehicle();
        if (vehicle != null) {
            org.bukkit.Input input = event.getInput();
            ShipInstance ship = MobileBlockState.getShipFromLoc(vehicle.getLocation().toBlockLocation());
            if (ship != null && ship.equals(findShipByPlayer(player))) {
                if (input.isSneak()) {
                    vehicle.removePassenger(player);
                    return;
                }
                if (ship.hasDriver && player.equals(ship.driver)) {
                    ship.setInputState(input.isForward(), input.isBackward(), input.isLeft(), input.isRight());
                    ship.setVerticalInputState(input.isJump(), input.isSprint());
                    ship.hasDriver = true;
                } else if (ship.driver == null) {
                    ship.driver = player;
                    ship.hasDriver = true;
                }
            }
        }
    }

    /**
     * Find the ship instance the player is currently riding and is the driver of.
     * @param player The player
     * @return ShipInstance if player is riding a ship as driver, null otherwise
     */
    public static ShipInstance findShipByPlayer(Player player) {
        ShipInstance ship = null;
        // Check if player is riding a ship seat shulker (Shulker with displayship:{uuid} and shipseat:{index} tags)
        if (player.getVehicle() instanceof org.bukkit.entity.Entity seat) {
            // Parse tags: displayship:{uuid} and shipseat:{index}
            // Tag creation: ShipInstance constructor (lines 290-300)
            ship = ShipListener.getShipFromEntity(seat);
            // Only return ship if player is in driver seat (index 0)
            if (ship != null && player.equals(ship.driver)) return ship;
            final MobileBlockState state = MobileBlockState.getStateFromLoc(seat.getLocation().toBlockLocation());
            if (state != null) ship = state.getShip();
            if (ship != null && player.equals(ship.driver)) return ship;
        } else {
            final MobileBlockState state = MobileBlockState.getStateFromLoc(player.getLocation().clone().add(0,-1,0));
            if (state != null) ship = state.getShip();
            if (ship != null && player.equals(ship.driver)) return ship;
        }
        return null;
    }

    /**
     * Find the ship instance the player is currently riding and is the driver of.
     * @param player The player
     * @return ShipInstance if player is riding a ship as driver, null otherwise
     */
    public static Collection<ShipInstance> getAllShipsByEntity(Entity entity) {
        Set<ShipInstance> ships = new HashSet<>();
        for (ShipInstance ship : ShipRegistry.getAllShips()) {
            if (ship.ENTITY_COMPOSITION.contains(entity)) ships.add(ship);
        }
        return ships;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(org.bukkit.event.player.PlayerInteractEntityEvent e) {
        Entity entity = e.getRightClicked();
        if (entity == null || !isShipEntity(entity.getPersistentDataContainer())) return;
        Location clickedLoc = entity.getLocation().toCenterLocation();
        Player player = e.getPlayer();
        ShipInstance ship = null;
        if (ship == null) ship = getShipFromEntity(entity);
        if (SmoothSailingPlugin.DEBUG_DUMP && ship != null) System.out.println("GOT SHIP "+ship+" FROM ENTITY "+entity.getType());
        if (ShipListener.isSeat(entity) && !player.isSneaking()) {
            entity.addPassenger(player);
            return;
        } else if (player.isSneaking()) {
            ShipWheelManager manager = ((SmoothSailingPlugin) plugin).getShipWheelManager();
            ShipWheelData wheelData = manager.getWheelByShipUUID(ship.id);
            if (wheelData != null) {
                ShipWheelMenu.openMenu(player, wheelData);
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.RIGHT_CLICK_AIR) return;
        Block clickedBlock = e.getClickedBlock();
        if (clickedBlock == null) return;
        Location clickedLoc = clickedBlock.getLocation().toCenterLocation();
        Player player = e.getPlayer();
        MobileBlockState state = MobileBlockState.getStateFromLoc(clickedLoc);
        ShipInstance ship = null;
        for (org.bukkit.entity.Entity entity : clickedLoc.getWorld().getNearbyEntities(clickedBlock.getBoundingBox().expand(0.275))) {
            if (entity instanceof Player || !isShipEntity(entity.getPersistentDataContainer())) continue;
            if (state != null) {
                if (ship == null) ship = state.getShip();
                if (ship == null) ship = getShipFromEntity(entity);
                if (SmoothSailingPlugin.DEBUG_DUMP && ship != null) System.out.println("GOT SHIP "+ship+" FROM NEAR ENTITY "+entity.getType());
                if (ShipListener.isSeat(entity) && !player.isSneaking()) {
                    entity.addPassenger(player);
                    return;
                } else if (player.isSneaking()) {
                    ShipWheelManager manager = ((SmoothSailingPlugin) plugin).getShipWheelManager();
                    ShipWheelData wheelData = manager.getWheelByShipUUID(ship.id);
                    if (wheelData != null) {
                        ShipWheelMenu.openMenu(player, wheelData);
                        e.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityMount(org.bukkit.event.entity.EntityMountEvent e) {
        Entity clicked = e.getMount();
        Entity player = e.getEntity();
        final MobileBlockState state = MobileBlockState.getStateFromLoc(clicked.getLocation().toBlockLocation());
        if (state == null) return;
        final ShipInstance ship = state.getShip();
        if (ship == null) return;
        ship.ENTITY_COMPOSITION.add(player);
        ship.ENTITY_COMPOSITION.add(clicked);
    }

    @EventHandler
    public void onEntityDismount(org.bukkit.event.entity.EntityDismountEvent e) {
        Entity clicked = e.getDismounted();
        Entity player = e.getEntity();
        final MobileBlockState state = MobileBlockState.getStateFromLoc(clicked.getLocation().toBlockLocation());
        if (ShipListener.isChair(clicked)) {
            Runnable RUN_ME = new BukkitRunnable() {
                @Override
                public void run() {
                    Location loc = player.getLocation();
                    player.setVelocity(new org.bukkit.util.Vector(
                        0,
                        0.25,
                        0
                    ));
                    loc.setY(loc.getY() + 0.25);
                    NmsUtils.teleportSilentCurrentThread(loc,player);
                }
            };
            com.ccorp2002.smoothsailing.region.TaskUtils.getRegionalScheduler(player.getLocation()).runTaskLater(RUN_ME, 5L);
        }
        if (state == null) return;
        final ShipInstance ship = state.getShip();
        if (ship == null) return;
        ship.ENTITY_COMPOSITION.add(player);
        ship.ENTITY_COMPOSITION.add(clicked);
    }

    /**
     * Handles detaching leads from a leadable shulker (fence block on assembled ship).
     * Mimics vanilla fence behavior: first entity attaches to player, rest drop as items.
     */
    public void handleLeadDetachment(Player player, org.bukkit.entity.Entity shulker, List<Entity> leashedEntities) {
        boolean firstEntity = true;
        Entity playerLeadingEntity = findEntityBeingLedByPlayer(player);
        boolean playerAlreadyLeading = (playerLeadingEntity != null);

        for (Entity entity : leashedEntities) {
            if (!(entity instanceof io.papermc.paper.entity.Leashable leashable)) continue;

            // Detach from shulker
            leashable.setLeashHolder(null);

            // First entity attaches to player (if player isn't already leading something)
            if (firstEntity && !playerAlreadyLeading) {
                leashable.setLeashHolder(player);
                firstEntity = false;
            } else {
                // Drop lead as item at entity's location
                entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.LEAD));
            }
        }

        // Play lead break sound
        shulker.getWorld().playSound(shulker.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1.0f, 1.0f);
    }

    /**
     * Finds an entity that the player is currently leading with a lead.
     * Searches within 10 blocks (Minecraft's lead range limit).
     */
    public Entity findEntityBeingLedByPlayer(Player player) {
        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 10, 10, 10)) {
            if (entity instanceof io.papermc.paper.entity.Leashable leashable) {
                if (leashable.isLeashed() && player.equals(leashable.getLeashHolder())) {
                    return entity;
                }
            }
        }
        return null;
    }

    @EventHandler
    public void onPlayerExitVehicle(final VehicleExitEvent e) {
        // Check if player is exiting a ship seat shulker
        if (!(e.getExited() instanceof Player player)) return;
        if (!(e.getVehicle() instanceof org.bukkit.entity.Mob shulker)) return;
        if (ShipListener.isChair(e.getVehicle())) {
            Runnable RUN_ME = new BukkitRunnable() {
                @Override
                public void run() {
                    Location loc = e.getExited().getLocation();
                    e.getExited().setVelocity(new org.bukkit.util.Vector(
                        0,
                        0.25,
                        0
                    ));
                    loc.setY(loc.getY() + 0.25);
                    NmsUtils.teleportSilentCurrentThread(loc,e.getExited());

                }
            };
            com.ccorp2002.smoothsailing.region.TaskUtils.getRegionalScheduler(player.getLocation()).runTaskLater(RUN_ME, 2L);
        }

        // Parse tags: displayship:{uuid} and shipseat:{index}
        // Tag creation: ShipInstance constructor (lines 285-297)
        ShipInstance inst = ShipListener.getShipFromEntity(shulker);
        if (inst != null) {
            // Speed persists - don't reset currentSpeed

            // Transfer ship velocity to exiting player
            //if (inst.driver != null && inst.driver.equals(player)) inst.driver = null;
            float currentSpeed = inst.physics.currentSpeed;
            float currentYVelocity = inst.physics.currentYVelocity;

            // Calculate ship velocity from currentSpeed and yaw (needed if ship is moving)
            float yawRad = (float) Math.toRadians(-inst.vehicle.getYaw());
            double forwardX = Math.sin(yawRad) * currentSpeed;
            double forwardZ = Math.cos(yawRad) * currentSpeed;
            boolean shipIsMoving = Math.abs(currentSpeed) > 1 || Math.abs(currentYVelocity) > 1;
            inst.physics.currentSpeed = inst.physics.currentSpeed / 4f;
            // Delay by 2 ticks to ensure Minecraft's dismount logic completes
        }
    }

    /**
     * Handle damage to collision shulkers and apply it to the ship's health.
     */
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH, ignoreCancelled = false)
    public void onSlimeDamage(org.bukkit.event.entity.SlimeSplitEvent e) {
        Entity shulker = e.getEntity();
        ShipInstance inst = ShipListener.getShipFromEntity(shulker);
        if (inst == null) return;
        e.setCancelled(true);
        e.setCount(0);
    }

    /**
     * Handle damage to collision shulkers and apply it to the ship's health.
     */
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH, ignoreCancelled = false)
    public void onShulkerKB(io.papermc.paper.event.entity.EntityKnockbackEvent e) {
        // Only handle shulker damage
        Entity shulker = e.getEntity();
        if (shulker instanceof Player) return;
        ShipInstance inst = ShipListener.getShipFromEntity(shulker);
        if (inst == null) return;
        e.setCancelled(true);
    }

    /**
     * Handle projectile hits on ship collision shulkers.
     * Arrows bounce off shulkers and fireballs do nothing by default,
     * so we manually apply damage here.
     */
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileBlockHitShip(ProjectileHitEvent e) {
        final Block block = e.getHitBlock();
        if (block == null) return;
        final Location loc = block.getLocation();
        final MobileBlockState state = MobileBlockState.getStateFromLoc(loc);
        if (state == null) return;
        final ShipInstance ship = state.getShip();
        if (state != null) {
            if (state.isDead()) return;
            if (e.getEntity().getShooter().equals(ship.driver)) {
                e.setCancelled(true);
                return;
            }
            long projDamage = (long)(getProjectileDamage(e.getEntity()) * 0.25d)+2l;
            state.damage(projDamage);
            if (!state.isDead()) NmsUtils.setBlockFast(block.getLocation(),state.state);
            else if (state.isDead()) {
                state.updateBlockState();
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPhys(org.bukkit.event.block.BlockPhysicsEvent e) {
        var blockData = e.getChangedBlockData();
        if (NmsUtils.isRedstoneComponent(NmsUtils.getBlockState(blockData))) return;
        final Block block = e.getBlock();
        if (block == null) return;
        final Location loc = block.getLocation();
        MobileBlockState state = MobileBlockState.getStateFromLoc(loc);
        if (state == null) return;
        ShipInstance ship = state.getShip();
        if (ship != null) {
            state.updateBlockState();
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonMoveShip(org.bukkit.event.block.BlockPistonRetractEvent e) {
        final Block block = e.getBlock();
        if (block == null) return;
        Location loc = block.getLocation();
        MobileBlockState state = MobileBlockState.getStateFromLoc(loc);
        if (state == null) return;
        ShipInstance ship = state.getShip();
        if (state != null) {
            e.setCancelled(true);
            return;
        }
        for (Block b : e.getBlocks()) {
            loc = b.getLocation();
            state = MobileBlockState.getStateFromLoc(loc);
            if (state == null) continue;
            ship = state.getShip();
            if (state != null) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPistonMoveShip(org.bukkit.event.block.BlockPistonExtendEvent e) {
        final Block block = e.getBlock();
        if (block == null) return;
        Location loc = block.getLocation();
        MobileBlockState state = MobileBlockState.getStateFromLoc(loc);
        if (state == null) return;
        ShipInstance ship = state.getShip();
        if (state != null && ship != null) {
            e.setCancelled(true);
            return;
        }
        for (Block b : e.getBlocks()) {
            loc = b.getLocation();
            state = MobileBlockState.getStateFromLoc(loc);
            if (state == null) continue;
            ship = state.getShip();
            if (state != null && ship != null) {
                e.setCancelled(true);
                return;
            }
        }
    }
    
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFlowShip(org.bukkit.event.block.BlockSpreadEvent e) {
        final Block block = e.getBlock();
        if (block == null) return;
        Location loc = block.getLocation();
        MobileBlockState state = MobileBlockState.getStateFromLoc(loc);
        if (state == null) return;
        ShipInstance ship = state.getShip();
        if (state != null && ship != null) {
            e.setCancelled(true);
            return;
        }
        loc = e.getSource().getLocation();
        state = MobileBlockState.getStateFromLoc(loc);
        if (state == null) return;
        ship = state.getShip();
        if (state != null && ship != null) {
            e.setCancelled(true);
            return;
        }
    }

    
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreakBlockShip(org.bukkit.event.block.BlockFadeEvent e) {
        final Block block = e.getBlock();
        if (block == null) return;
        final Location loc = block.getLocation();
        final MobileBlockState state = MobileBlockState.getStateFromLoc(loc);
        if (state == null) return;
        final ShipInstance ship = state.getShip();
        if (state != null && ship != null) {
            e.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreakShip(org.bukkit.event.block.BlockBreakEvent e) {
        final Block block = e.getBlock();
        if (block == null) return;
        final Location loc = block.getLocation();
        final MobileBlockState state = MobileBlockState.getStateFromLoc(loc);
        if (state == null) return;
        final ShipInstance ship = state.getShip();
        if (ship == null) return;
        if (state != null) {
            if (state.isDead()) {
                state.setBlockData(ShipPhysics.VOID);
                state.updateBlockState();
                return;
            }
            long projDamage = state.health;
            state.damage(projDamage+2l);
            if (!state.isDead()) NmsUtils.setBlockFast(block.getLocation(),state.state);
            else {
                state.tile = null;
                state.setBlockData(ShipPhysics.VOID);
            }
            state.updateBlockState();
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH, ignoreCancelled = false)
    public void onEntityBlast(org.bukkit.event.entity.EntityExplodeEvent e) {
        Set<Block> blockSet = Set.<Block>copyOf(e.blockList());
        Entity ent = e.getEntity();
        float BLAST_YIELD = 1f;
        if (!(ent instanceof Explosive TNT)){
            BLAST_YIELD = 3f;
        } else {
            BLAST_YIELD += TNT.getYield();
        }
        Location CENTER = ent.getLocation();
        boolean wasInFluid = false;
        if (ShipPhysics.getMedium("water").isDataMedium(CENTER.getBlock().getBlockData())) {
            wasInFluid = true;
        } else if (ShipPhysics.getMedium("lava").isDataMedium(CENTER.getBlock().getBlockData())) {
            wasInFluid = true;
        }
        for (Block block : blockSet) {
            final MobileBlockState state = MobileBlockState.getStateFromLoc(block.getLocation());
            if (state == null) continue;
            final ShipInstance ship = state.getShip();
            if (ship != null) {
                if (state.isDead()) continue;
                int DISTANCE = (int)Math.round(block.getLocation().distance(ent.getLocation()));
                long projDamage = (long)(Math.abs(BLAST_YIELD+2 - (DISTANCE-1l)));
                if (!wasInFluid) state.damage(projDamage);
                e.blockList().remove(block);
                if (!state.isDead()) NmsUtils.setBlockFast(block.getLocation(),state.state);
                state.updateBlockState();
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH, ignoreCancelled = false)
    public void onBlockBlast(org.bukkit.event.block.BlockExplodeEvent e) {
        Set<Block> blockSet = Set.<Block>copyOf(e.blockList());
        for (Block block : blockSet) {
            final MobileBlockState state = MobileBlockState.getStateFromLoc(block.getLocation());
            if (state == null) continue;
            final ShipInstance ship = state.getShip();
            if (ship != null) {
                if (state.isDead()) continue;
                if (ShipInstance.RANDOM.nextDouble() < 0.625) state.damage(3l);
                else state.damage(1l);
                e.blockList().remove(block);
                if (!state.isDead()) NmsUtils.setBlockFast(block.getLocation(),state.state);
                state.updateBlockState();
            }
        }
    }
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBurn(org.bukkit.event.block.BlockBurnEvent e){
        Block block = e.getBlock();
        final MobileBlockState state = MobileBlockState.getStateFromLoc(block.getLocation());
        if (state == null) return;
        final ShipInstance ship = state.getShip();
        if (ship != null) {
            if (state.isDead()) return;
            if (ShipInstance.RANDOM.nextDouble() < 0.625) state.damage(3l);
            else state.damage(7l);
            if (!state.isDead()) {
                NmsUtils.setBlockFast(block.getLocation(),state.state);
                e.setCancelled(true);
            } else NmsUtils.setBlockFast(block.getLocation(),ShipPhysics.VOID);
            state.updateBlockState();
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH, ignoreCancelled = false)
    public void onShulkerDeath(org.bukkit.event.entity.EntityDeathEvent e) {
        Entity shulker = e.getEntity();
        if (shulker instanceof Player) return;
        ShipInstance inst = ShipListener.getShipFromEntity(shulker);
        if (inst == null) return;
        if (shulker instanceof LivingEntity le && PDCUtils.getPDC(le,"destroyable").equals("true")) {
            e.setCancelled(true);
            inst.setHealth(0.25f);
        }
    }
    /**
     * Handle damage to collision shulkers and apply it to the ship's health.
     */
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH, ignoreCancelled = false)
    public void onShulkerDamage(EntityDamageEvent e) {
        // Only handle shulker damage
        Entity shulker = e.getEntity();
        if (shulker instanceof Player) return;
        double damage = e.getDamage();
        ShipInstance inst = ShipListener.getShipFromEntity(shulker);
        if (inst == null) return;


        // Get the damage amount and apply directly to ship health
        if (shulker.equals(inst.vehicle) || ShipListener.isSeat(shulker) || e.getCause() == EntityDamageEvent.DamageCause.CRAMMING || e.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION) {
            shulker.setInvulnerable(true);
            e.setDamage(0);
            return;
        }
    }

    /**
     * Calculate damage for different projectile types.
     */
    public static double getProjectileDamage(Entity projectile) {
        if (projectile instanceof AbstractWindCharge arrow) {
            return 2d;
        } else if (projectile instanceof AbstractArrow arrow) { // Arrow damage scales with velocity (max ~10 at full draw)
            return arrow.getDamage() * 0.125d;
        } else if (projectile instanceof Fireball) {
            return 2.75d; // Ghast fireball
        } else if (projectile instanceof SmallFireball) {
            return 1.5d; // Blaze fireball / fire charge
        } else if (projectile instanceof ThrownPotion || projectile instanceof Egg) {
            return 0d; // No impact damage
        } else return 0.125d; // Fallback for other projectiles
    }

    /**
     * Helper: Check if a block is a placed ship wheel
     */
    public boolean isShipWheelBlock(Block block) {
        Material type = block.getType();
        ShipWheelManager manager = ((SmoothSailingPlugin) plugin).getShipWheelManager();
        return manager.getWheelAt(block.getLocation()) != null || isShipWheelBlockState(block.getState(false)) || type == Material.LECTERN;
    }

    /**
     * Helper: Check if a blockstate is a ship wheel
     */
    public boolean isShipWheelBlockState(org.bukkit.block.BlockState block) {
        Material type = block.getType();
        if (block instanceof org.bukkit.block.Lectern lect) {
            return PDCUtils.getPDC(lect.getPersistentDataContainer(),"custom_item_id").equalsIgnoreCase("ship_wheel");
        }
        return false;
    }
    // 6 directions: +X, -X, +Y, -Y, +Z, -Z (no diagonals)
    public static final int[][] DIRECTIONS = {
        {1, 0, 0},   // East
        {-1, 0, 0},  // West
        {0, 1, 0},   // Up
        {0, -1, 0},  // Down
        {0, 0, 1},   // South
        {0, 0, -1}   // North
    };
    
    /**
     * Event: Place ship wheel item as a block
     */
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlaceBlockShip(org.bukkit.event.block.BlockPlaceEvent event) {
        Block block = event.getBlock();
        ShipInstance found = null;
        
        for (int[] direction : DIRECTIONS) {
            Location neighbor = block.getLocation().clone().add(direction[0], direction[1], direction[2]);
            found = MobileBlockState.getShipFromLoc(neighbor);
            if (found != null) break;
        }
        if (found != null) {
            final ShipInstance ship = found;
            ship.physics.moving = true;
            ship.setProcessing(true);
            if (block.getType().toString().endsWith("ANVIL")) {
                event.setCancelled(true);
                ship.setProcessing(false);
                ship.physics.moving = false;
                return;
            }
            ShipInstance.addBlock(ship,block,true);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = false)
    public void onUseShipWheel(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) return;
        Player player = event.getPlayer();
        if (isShipWheelBlock(block)) {
            if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;
            BlockFace facing = DirectionalUtils.getBlockDataFace(block.getBlockData());
            if (facing == BlockFace.SELF) return;
            // Cancel event to prevent other plugins (like HeadSmith) from also handling this
            if (player.isSneaking() && block.getType() == Material.LECTERN) return;
            ShipWheelManager manager = ((SmoothSailingPlugin) plugin).getShipWheelManager();
            ShipWheelData wheelData = manager.getWheelAt(block.getLocation());
            if (wheelData != null) {
                wheelData.blockLocation.setYaw(BlockStructureScanner.blockFaceToYaw(facing));
                ShipWheelMenu.openMenu(player, wheelData);
                event.setCancelled(true);
            } else {
                boolean success = manager.placeWheel(block.getLocation(), facing);
                if (success) {
                    wheelData = manager.getWheelAt(block.getLocation());
                    wheelData.blockLocation.setYaw(BlockStructureScanner.blockFaceToYaw(facing));
                    ShipWheelMenu.openMenu(player, wheelData);
                }
                event.setCancelled(true);
            }
        } else if (block.getState(false) instanceof org.bukkit.block.Sign signState) {
            if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
            if (signState.getLine(0).equalsIgnoreCase("[turret]")) {
                Location CENTER = block.getLocation().toBlockLocation();
                String LEFT_RIGHT = "NONE";
                double yawDeg = 0.0d;
                if (event.getAction() == Action.LEFT_CLICK_BLOCK) {LEFT_RIGHT = "LEFT"; yawDeg = -90.0d;}
                else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {LEFT_RIGHT = "RIGHT"; yawDeg = 90.0d;}
                else return;
                ShipInstance ship = MobileBlockState.getShipFromLoc(CENTER);
                if (ship == null) return;
                event.setCancelled(true);
                if (ship.driver != null && ship.driver instanceof Player driver) {
                    Set<Block> FOUND_BLOCKS = com.ccorp2002.smoothsailing.detection.ShipDetector.detectBlocks(driver,CENTER);
                    FOUND_BLOCKS.add(block);
                    ship.physics.rotateSegment3Axis(CENTER,LEFT_RIGHT,yawDeg,0d,0d,FOUND_BLOCKS);
                } else {
                    Set<Block> FOUND_BLOCKS = com.ccorp2002.smoothsailing.detection.ShipDetector.detectBlocks(player,CENTER);
                    FOUND_BLOCKS.add(block);
                    ship.physics.rotateSegment3Axis(CENTER,LEFT_RIGHT,yawDeg,0d,0d,FOUND_BLOCKS);
                }
            }
        }
    }
    /**
     * Event: Handle ship wheel menu clicks
     */
    @EventHandler
    public void onShipWheelMenuClick(InventoryClickEvent event) {
        // Check if this is a ship wheel menu by checking the inventory holder
        if (!(event.getInventory().getHolder() instanceof ShipWheelMenu.ShipWheelMenuHolder)) {
            return;
        }

        event.setCancelled(true); // Prevent item removal

        if (!(event.getWhoClicked() instanceof Player)) return;
        final Player player = (Player) event.getWhoClicked();

        int slot = event.getRawSlot();
        ShipWheelMenu.MenuAction action = ShipWheelMenu.getActionFromSlot(slot);

        if (action == ShipWheelMenu.MenuAction.NONE) return;

        // Get wheel data from the custom inventory holder
        ShipWheelMenu.ShipWheelMenuHolder holder = (ShipWheelMenu.ShipWheelMenuHolder) event.getInventory().getHolder();
        ShipWheelData wheelData = holder.getWheelData();

        if (wheelData == null) {
            player.sendMessage("§cShip wheel data not found!");
            player.closeInventory();
            return;
        }

        ShipWheelManager manager = ((SmoothSailingPlugin) plugin).getShipWheelManager();

        boolean stateChanged = false;

        switch (action) {
            case DETECT:
                NmsUtils.runTaskInWorld(()->{
                    manager.detectShip(player, wheelData, true);
                }, player.getLocation());
                // Refresh menu to update ship info lore
                stateChanged = true;
                break;
            case ASSEMBLE:
                NmsUtils.runTaskInWorld(()->{
                    manager.assembleShip(player, wheelData);
                }, player.getLocation());
                stateChanged = true;
                break;
            case DISASSEMBLE:
                stateChanged = manager.disassembleShip(player, wheelData, true);
                break;
            case INFO:
                // Run ship detection and update the info item in place (no particles)
                NmsUtils.runTaskInWorld(()->{
                    manager.detectShip(player, wheelData, false);
                    ShipWheelMenu.updateInfoItem(event.getInventory(), wheelData);
                }, player.getLocation());
                break;
        }

        // Close and reopen menu if state changed (for assemble/disassemble)
        if (stateChanged) {
            player.closeInventory();
        }
    }

    /**
     * Event: Clear force disassembly state when ship wheel menu is closed
     */
    @EventHandler
    public void onShipWheelMenuClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof ShipWheelMenu.ShipWheelMenuHolder)) {
            return;
        }

        ShipWheelMenu.ShipWheelMenuHolder holder = (ShipWheelMenu.ShipWheelMenuHolder) event.getInventory().getHolder();
        ShipWheelData wheelData = holder.getWheelData();
        if (wheelData != null) {
            wheelData.setPendingMenuReopen(false);
        }
    }

    /**
     * Event: Handle ship wheel block breaking
     */
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShipWheelBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        ShipWheelManager manager = ((SmoothSailingPlugin) plugin).getShipWheelManager();
        if (!isShipWheelBlockState(block.getState()) && !isShipWheelBlock(block)) {
            manager.removeWheel(block.getLocation());
            return;
        }
        if (!PDCUtils.getPDC(block,"custom_item_id").equalsIgnoreCase("ship_wheel")) return;

        // Cancel event to prevent other plugins (like HeadSmith) from also handling this

        ShipWheelData wheelData = manager.getWheelAt(block.getLocation());

        if (wheelData != null) {
            ShipInstance ship = MobileBlockState.getShipFromLoc(block.getLocation());
            // Check if ship is assembled - warn player
            if (ship != null) {
                wheelData.assembledShipUUID = ship.id;
            }
            if (wheelData.isAssembled()) {
                Player player = event.getPlayer();
                player.sendMessage("§cWarning: Breaking this Lectern would destroy the assembled ship!");
                event.setCancelled(true);
                return;
            }
            block.setType(Material.AIR,false);
            // Remove wheel (this also destroys the ship if assembled)
            manager.removeWheel(block.getLocation());

            // Manually remove the block since we cancelled the event

            // Drop ship wheel item
        }
    }
}
