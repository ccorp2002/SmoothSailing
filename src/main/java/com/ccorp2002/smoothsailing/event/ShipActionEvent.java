package com.ccorp2002.smoothsailing.event;

import java.util.List;
import org.bukkit.event.Event;
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

public abstract class ShipActionEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    @NotNull protected final ShipInstance ship;
    protected Entity entity;
    protected boolean cancel;

    public ShipActionEvent(@NotNull ShipInstance ship, Entity entity) {
        this(ship,entity,false);
    }

    public ShipActionEvent(@NotNull ShipInstance ship, Entity entity, boolean isAsync){
        super(isAsync);
        this.ship = ship;
        this.entity = entity;
    }

    @NotNull
    public final ShipInstance getShip() {
        return this.ship;
    }
    public Entity getEntity() {
        return this.entity;
    }
    @Override
    public boolean isCancelled() {
        return cancel;
    }
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancel = cancelled;
    }
    @Override
    public boolean callEvent() {
        Bukkit.getPluginManager().callEvent(this);
        return !this.isCancelled();
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
