package com.ccorp2002.smoothsailing.event;

import java.util.List;
import org.bukkit.event.Event;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import com.ccorp2002.smoothsailing.*;
import com.ccorp2002.smoothsailing.ship.*;
import com.ccorp2002.smoothsailing.utils.*;
import com.ccorp2002.smoothsailing.detection.*;
import com.ccorp2002.smoothsailing.detection.*;
import org.jetbrains.annotations.NotNull;

public class ShipBurnFuelEvent extends ShipActionEvent {
    protected FuelType toBurn;
    public ShipBurnFuelEvent(@NotNull ShipInstance ship, Entity entity, FuelType toBurn) {
        this(ship,entity,toBurn,false);
    }

    public ShipBurnFuelEvent(@NotNull ShipInstance ship, Entity entity, FuelType toBurn, boolean isAsync){
        super(ship,entity,isAsync);
        this.toBurn = toBurn;
    }
    public FuelType getFuelType() {
        return this.toBurn;
    }
    public void setFuelItem(FuelType newToBurn) {
        this.toBurn = newToBurn;
    }
}
