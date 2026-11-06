package com.ccorp2002.smoothsailing.event;

import java.util.List;
import org.bukkit.event.Event;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.jetbrains.annotations.NotNull;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import com.ccorp2002.smoothsailing.*;
import com.ccorp2002.smoothsailing.ship.*;
import com.ccorp2002.smoothsailing.customships.*;
import com.ccorp2002.smoothsailing.blockconfig.*;

public class ShipBurnFuelEvent extends ShipActionEvent {
    protected Block block;
    public ShipBurnFuelEvent(@NotNull ShipInstance ship, Block block, Entity entity) {
        this(ship,block,entity,false);
    }

    public ShipBurnFuelEvent(@NotNull ShipInstance ship, Block block, Entity entity, boolean isAsync){
        super(ship,entity,isAsync);
        this.block = block;
    }
    public Block getFuelHolder() {
        return this.block;
    }
    public void setFuelHolder(Block block) {
        if (block.getState(false) instanceof TileState) this.block = block;
    }
}
