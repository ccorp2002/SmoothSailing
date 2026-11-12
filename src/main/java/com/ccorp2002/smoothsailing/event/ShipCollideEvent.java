package com.ccorp2002.smoothsailing.event;

import java.util.*;
import org.bukkit.event.Event;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import com.ccorp2002.smoothsailing.*;
import com.ccorp2002.smoothsailing.ship.*;
import com.ccorp2002.smoothsailing.detection.*;
import com.ccorp2002.smoothsailing.detection.*;

public class ShipCollideEvent extends ShipActionEvent {
    protected Map<Location,Location> blocks;
    public ShipCollideEvent(@NotNull ShipInstance ship, Map<Location,Location> blocks) {
        this(ship,blocks,false);
    }

    public ShipCollideEvent(@NotNull ShipInstance ship, Map<Location,Location> blocks, boolean isAsync){
        super(ship,ship.vehicle,isAsync);
        this.setBlocks(blocks);
    }
    public Map<Location,Location> getBlocks() {
        return this.blocks;
    }
    public void setBlocks(Map<Location,Location> blocks) {
        this.blocks.clear();
        this.blocks = blocks;
    }
}
