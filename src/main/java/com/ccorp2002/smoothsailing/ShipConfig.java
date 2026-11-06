package com.ccorp2002.smoothsailing;

import com.ccorp2002.smoothsailing.ship.ShipInstance;
import com.ccorp2002.smoothsailing.ship.ShipPhysics;
import org.bukkit.plugin.Plugin;
import org.joml.Vector3f;

/**
 * Encapsulates all ship configuration values loaded from config.yml.
 * Reduces boilerplate in ShipInstance constructor.
 */
public class ShipConfig {
    public final boolean collisionDebugGlow = false;

    // Buoyancy
    public final float airDensity;
    public final float waterDensity;

    private ShipConfig(Builder b) {
        this.airDensity = b.airDensity;
        this.waterDensity = b.waterDensity;
    }

    /**
     * Loads ship configuration from config.yml for a specific ship type.
     */
    public static ShipConfig load(Plugin plugin) {
        String shipType = "custom";
        String p = "ships." + shipType + ".";
        var cfg = plugin.getConfig();

        return new Builder()
            // Weight-based buoyancy (from custom-ships section)
            .airDensity((float) cfg.getDouble("buoyancy.air-density", ShipInstance.airDensity()))
            .waterDensity((float) cfg.getDouble("buoyancy.water-density", ShipInstance.waterDensity()))
            .build();
    }

    private static class Builder {
        public float airDensity = -0.25f;
        public float waterDensity = 10.0f;
        Builder airDensity(float v) { airDensity = v; return this; }
        Builder waterDensity(float v) { waterDensity = v; return this; }

        ShipConfig build() { return new ShipConfig(this); }
    }
}
