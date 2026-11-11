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
import com.ccorp2002.smoothsailing.detection.*;
import com.ccorp2002.smoothsailing.detection.*;
import com.ccorp2002.smoothsailing.detection.ShipDetector.InternalScanResult;

public class BlockFinishScanEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    protected boolean cancel;
    protected InternalScanResult result;

    public BlockFinishScanEvent(InternalScanResult result) {
        this(result, false);
    }

    public BlockFinishScanEvent(InternalScanResult result, boolean isAsync){
        super(isAsync);
        this.result = result;
    }
    @Override
    public boolean isCancelled() {
        return cancel;
    }
    public InternalScanResult getResult() {
        return this.result;
    }
    public void setResult(InternalScanResult result) {
        this.result = result;
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
