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

public class ShipTickEvent extends ShipActionEvent {
    public ShipTickEvent(@NotNull ShipInstance ship, Entity entity) {
        this(ship,entity,false);
    }

    public ShipTickEvent(@NotNull ShipInstance ship, Entity entity, boolean isAsync){
        super(ship,entity,isAsync);
    }
}
