package com.ccorp2002.smoothsailing.event;

import java.util.List;
import org.bukkit.event.Event;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.block.BlockState;
import org.jetbrains.annotations.NotNull;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import com.ccorp2002.smoothsailing.*;
import com.ccorp2002.smoothsailing.ship.*;
import com.ccorp2002.smoothsailing.utils.*;
import com.ccorp2002.smoothsailing.customships.*;
import com.ccorp2002.smoothsailing.blockconfig.*;

public class ShipDetectFuelEvent extends ShipActionEvent {
    protected Block source;
    protected FuelType fuelType;
    public ShipDetectFuelEvent(@NotNull ShipInstance ship, Entity entity, FuelType fuelType, Block source) {
        this(ship,entity,fuelType,source,false);
    }

    public ShipDetectFuelEvent(@NotNull ShipInstance ship, Entity entity, FuelType fuelType, Block source, boolean isAsync){
        super(ship,entity,isAsync);
        this.fuelType = fuelType;
        this.source = source;
    }

    public FuelType getFuelType() {
        return this.fuelType;
    }

    public Block getBlock() {
        return this.source;
    }
    public void setBlock(Block block) {
        if (block.getState(false) instanceof TileState) this.source = block;
    }
}
