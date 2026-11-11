package com.ccorp2002.smoothsailing.event;

import java.util.List;
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

public class ShipPreProcessMoveEvent extends ShipActionEvent {
    protected Location loc;
    public ShipPreProcessMoveEvent(@NotNull ShipInstance ship, Location loc, Entity entity) {
        this(ship,loc,entity,false);
    }

    public ShipPreProcessMoveEvent(@NotNull ShipInstance ship, Location loc, Entity entity, boolean isAsync){
        super(ship,entity,isAsync);
        this.loc = loc;
    }
    public Location getLocation() {
        return this.loc;
    }
    public void setLocation(Location loc) {
        this.loc = loc;
    }
}
