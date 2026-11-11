package com.ccorp2002.smoothsailing.nmsutils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ScheduledTick;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Queue;
import java.util.stream.Stream;

//borrowed from Movecraft 8.x
public class NextTickProvider {
    @Nullable
    public ScheduledTick<?> getNextTick(@NotNull ServerLevel world, @NotNull BlockPos position){
        LevelChunkTicks<Block> tickList = (LevelChunkTicks<Block>) world
                .getChunk(position)
                .getBlockTicks();

        var box = BoundingBox.encapsulatingPositions(List.of(position));
        if(box.isEmpty()){
            return null;
        }

        Stream<ScheduledTick<Block>> ticks = tickList.getAll();

        for (var iter = ticks.iterator(); iter.hasNext(); ) {
            var next = iter.next();
            if (!next.pos().equals(position)) {
                continue;
            }
            return next;
        }
        return null;
    }


    public static class TickHolder {
        @NotNull
        public final ScheduledTick tick;
        @NotNull
        public final BlockPos tickPosition;

        public TickHolder(@NotNull ScheduledTick tick, @NotNull BlockPos tilePosition) {
            this.tick = tick;
            this.tickPosition = tilePosition;
        }


        @NotNull
        public ScheduledTick getTick() {
            return tick;
        }

        @NotNull
        public BlockPos getTickPosition() {
            return tickPosition;
        }
    }
}
