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
import com.ccorp2002.smoothsailing.customships.*;
import com.ccorp2002.smoothsailing.blockconfig.*;

public class ShipRotateEvent extends ShipMoveEvent {
    private final org.bukkit.block.structure.StructureRotation ROTATION;
    public ShipRotateEvent(@NotNull ShipInstance ship, Location loc, Entity entity, org.bukkit.block.structure.StructureRotation ROTATION) {
        this(ship,loc,entity,ROTATION,false);
    }

    public ShipRotateEvent(@NotNull ShipInstance ship, Location loc, Entity entity, org.bukkit.block.structure.StructureRotation ROTATION, boolean isAsync){
        super(ship,loc,entity,isAsync);
        this.ROTATION = ROTATION;
    }
    public org.bukkit.block.structure.StructureRotation getRotation() {
        return this.ROTATION;
    }
}
