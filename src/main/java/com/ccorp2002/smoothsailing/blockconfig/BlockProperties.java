package com.ccorp2002.smoothsailing.blockconfig;

import com.ccorp2002.smoothsailing.ShipModel;
import org.bukkit.block.data.BlockData;

import java.util.List;
import java.util.ArrayList;

/**
 * Properties for a block type used in ship construction.
 * Includes weight, hitpoints, and other behaviors.
 */
public class BlockProperties {
    public boolean allowed;
    public Double baseHealth = null;  // null means health is determined by a function of density (weight).
    public Integer weight;  // null means block is excluded from density calculations
    public boolean seat;
    public double fuelCost = 0d;
    public boolean isTurretBase = false;
    public boolean isEngine = false;
    public final List<ConditionalRule> conditionalRules = new ArrayList<>();

    public BlockProperties(boolean allowed, List<ConditionalRule> conditionalRules) {
        this(allowed, 2, false, null, conditionalRules);
    }

    public BlockProperties(boolean allowed, Integer weight, boolean seat) {
        this(allowed, weight, seat, null, null);
    }

    public BlockProperties(boolean allowed, Integer weight, boolean seat, Double health) {
        this(allowed, weight, seat, health, null);
    }

    public BlockProperties(boolean allowed, Double health, List<ConditionalRule> conditionalRules) {
        this(allowed, 0, false, health, conditionalRules);
    }

    public BlockProperties(boolean allowed, boolean seat, List<ConditionalRule> conditionalRules) {
        this(allowed, 5, seat, null, conditionalRules);
    }
    public BlockProperties(boolean allowed, Integer weight, boolean seat, Double health, List<ConditionalRule> conditionalRules) {
        this.allowed = allowed;
        this.weight = weight;
        this.seat = seat;
        this.baseHealth = health;
        this.conditionalRules.clear();
        if (conditionalRules != null) this.conditionalRules.addAll(conditionalRules);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public boolean hasTurretBaseState() {
        boolean turretState = false;
        if (conditionalRules == null || conditionalRules.isEmpty()) {
            return isTurretBase();
        } else {
            for (ConditionalRule rule : conditionalRules) {
                turretState = turretState || rule.getProperties().isTurretBase();
                if (turretState) return turretState;
            }
        }
        return isTurretBase() || turretState;
    }

    public boolean hasAllowedState() {
        boolean allowedState = false;
        if (conditionalRules == null || conditionalRules.isEmpty()) {
            return isAllowed();
        } else {
            for (ConditionalRule rule : conditionalRules) {
                allowedState = allowedState || rule.getProperties().isAllowed();
                if (allowedState) return allowedState;
            }
        }
        return isAllowed() || allowedState;
    }

    public boolean isTurretBase() {
        return this.isTurretBase;
    }

    public void setIsTurretBase(boolean isTurretBase) {
        this.isTurretBase = isTurretBase;
    }

    public void setBaseHealth(Double health) {
        this.baseHealth = health;
    }

    public Double getBaseHealth() {
        return this.baseHealth;
    }

    public void setFuelCost(Double fuelCost) {
        if (fuelCost == null) this.fuelCost = 0d;
        else this.fuelCost = fuelCost;
    }

    public double getFuelCost() {
        return this.fuelCost;
    }

    public boolean doesCostFuel() {
        return allowed && this.fuelCost != 0;
    }

    public void setIsEngine(boolean engine) {
        this.isEngine = engine;
    }

    public boolean isEngine() {
        return allowed && this.isEngine;
    }

    public boolean hasEngineState() {
        boolean engineState = false;
        if (conditionalRules == null || conditionalRules.isEmpty()) {
            return isEngine();
        } else {
            for (ConditionalRule rule : conditionalRules) {
                engineState = engineState || rule.getProperties().isEngine();
                if (engineState) return engineState;
            }
        }
        return isEngine() || engineState;
    }

    /**
     * Returns whether this block has a weight (contributes to density calculations).
     * Blocks with null weight are excluded from buoyancy calculations.
     */
    public boolean hasWeight() {
        return weight != null;
    }

    /**
     * Returns the weight of this block, or null if excluded from density calculations.
     */
    public Integer getWeight() {
        return weight;
    }

    public boolean isSeat() {
        return seat;
    }


    /**
     * Get properties for a specific block state, applying conditional rules if any.
     */
    public BlockProperties getPropertiesForBlockData(BlockData blockData) {
        if (conditionalRules == null || conditionalRules.isEmpty()) {
            return this;
        }

        // Find matching rule
        for (ConditionalRule rule : conditionalRules) {
            if (rule.matches(blockData)) {
                return rule.getProperties();
            }
        }

        // No matching rule, return defaults
        return this;
    }

    /**
     * Represents a conditional rule that applies different properties based on BlockData.
     * For example: bottom slabs have different colliders than top slabs.
     */
    public static class ConditionalRule {
        public BlockDataMatcher matcher;
        public BlockProperties properties;

        public ConditionalRule(BlockDataMatcher matcher, BlockProperties properties) {
            this.matcher = matcher;
            this.properties = properties;
        }

        public ConditionalRule(BlockData blockData, BlockProperties properties) {
            this.matcher = BlockConfigManager.createMatcherForData(blockData);
            this.properties = properties;
        }

        public boolean matches(BlockData blockData) {
            return matcher.matches(blockData);
        }

        public BlockDataMatcher getMatcher() {
            return matcher;
        }

        public ConditionalRule setMatcher(BlockDataMatcher matcher) {
            this.matcher = matcher;
            return this;
        }

        public BlockProperties getProperties() {
            return properties;
        }

        public ConditionalRule setProperties(BlockProperties properties) {
            this.properties = properties;
            return this;
        }
    }

    /**
     * Matches BlockData against specific property values.
     * Example: {type: "BOTTOM", waterlogged: false}
     */
    public interface BlockDataMatcher {
        boolean matches(BlockData blockData);
    }
}
