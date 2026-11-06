package com.ccorp2002.smoothsailing;

import com.ccorp2002.smoothsailing.ship.ShipInstance;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.*;

public final class ShipRegistry {
    private static final Map<UUID, ShipInstance> byId = new HashMap<>();
    private static final Map<UUID, ShipInstance> byVehicle = new HashMap<>();

    public static void register(ShipInstance s) {
        byId.put(s.id, s);
        byVehicle.put(s.vehicle.getUniqueId(), s);
    }

    public static ShipInstance byId(UUID id) {
        return byId.get(id);
    }

    public static ShipInstance byVehicle(Entity vehicle) {
        return byVehicle.get(vehicle.getUniqueId());
    }

    public static void unregister(ShipInstance s) {
        byId.remove(s.id);
        byVehicle.remove(s.vehicle.getUniqueId());
    }

    public static Collection<ShipInstance> getAllShips() {
        return new ArrayList<>(byId.values());
    }

    public static List<ShipInstance> getShipsInWorld(World world) {
        List<ShipInstance> result = new ArrayList<>();
        for (ShipInstance inst : byId.values()) {
            if (inst.vehicle == null) {
                byId.remove(inst.id);
                continue;
            }
            if (inst.vehicle.getWorld().equals(world)) result.add(inst);
        }
        return result;
    }

    public static List<ShipInstance> getShipsInChunk(Chunk chunk) {
        List<ShipInstance> result = new ArrayList<>();
        for (ShipInstance inst : byId.values()) {
            if (inst.vehicle == null) {
                byId.remove(inst.id);
                continue;
            }
            // Use block coordinates to determine chunk without loading it
            // This avoids issues with invalid entity references
            Location loc = inst.vehicle.getLocation();
            if (loc.getWorld() != null && loc.getWorld().equals(chunk.getWorld()) &&
                loc.getBlockX() >> 4 == chunk.getX() &&
                loc.getBlockZ() >> 4 == chunk.getZ()) {
                result.add(inst);
            }
        }
        return result;
    }

    public static void destroyAll() {
        new ArrayList<>(byId.values()).forEach(ShipInstance::destroy);
        byId.clear();
        byVehicle.clear();
    }
}
