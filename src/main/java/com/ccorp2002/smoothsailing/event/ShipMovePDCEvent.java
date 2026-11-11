package com.ccorp2002.smoothsailing.event;

import java.util.List;
import org.bukkit.event.Event;
import org.bukkit.Location;
import org.bukkit.block.Block;
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

public class ShipMovePDCEvent extends ShipActionEvent {
    protected Block from;
    protected Block to;
    protected org.bukkit.persistence.PersistentDataContainer PDC;
    public ShipMovePDCEvent(@NotNull ShipInstance ship, Entity entity, org.bukkit.persistence.PersistentDataContainer PDC, Block from, Block to) {
        this(ship,entity,PDC,from,to,false);
    }

    public ShipMovePDCEvent(@NotNull ShipInstance ship, Entity entity, org.bukkit.persistence.PersistentDataContainer PDC, Block from, Block to, boolean isAsync){
        super(ship,entity,isAsync);
        this.PDC = PDC;
        this.from = from;
        this.to = to;
    }
    public org.bukkit.persistence.PersistentDataContainer getPDC() {
        return this.PDC;
    }
    public Block getFrom() {
        return this.from;
    }
    public Block getTo() {
        return this.to;
    }
}
