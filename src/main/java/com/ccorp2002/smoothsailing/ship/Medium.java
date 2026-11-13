package com.ccorp2002.smoothsailing.ship;

import com.ccorp2002.smoothsailing.*;
import com.ccorp2002.smoothsailing.ShipConfig;
import com.ccorp2002.smoothsailing.ShipListener;
import com.ccorp2002.smoothsailing.detection.*;
import com.ccorp2002.smoothsailing.utils.rotation.*;
import com.ccorp2002.smoothsailing.utils.movement.*;
import com.ccorp2002.smoothsailing.utils.*;
import com.ccorp2002.smoothsailing.nmsutils.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Rail;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.RedstoneWire;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;
import org.bukkit.block.BlockFace;
import org.bukkit.Bukkit;
import org.bukkit.World;
import java.util.*;

public class Medium {
    public List<BlockData> BLOCKS = new ArrayList<>();
    public HashMap<Integer, Double> densityMap = new HashMap<>();
    public double buoyancyMultiplier = 0.0f;
    public double dragMultiplier = 0.0f;
    public double baseDensity = 0.25f;
    public String id = "";
    
    public double getDensity(int y){
        if(densityMap.containsKey(y)) return densityMap.get(y);
        int lowerPos = 0, upperPos = 0;
        double lower = Double.NaN;
        double upper = Double.NaN;
        for(int i : densityMap.keySet()){
            if(i<y){
                if(i>lowerPos||Double.isNaN(lower)){
                    lowerPos = i;
                    lower = densityMap.getOrDefault(i,baseDensity);
                }
            }
            if(i>y){
                if(i>upperPos||Double.isNaN(upper)){
                    upperPos = i;
                    upper = densityMap.getOrDefault(i,baseDensity);
                }
            }
        }
        if(Double.isNaN(lower))return upper; //if both nan, return nan, already done by this
        if(Double.isNaN(upper))return lower;
        double percent = ((double)y-lowerPos)/((double)upperPos-lowerPos);
        return lower+percent*(upper-lower);
    }
    public List<BlockData> getBlockDatas() {
        return BLOCKS;
    }
    public boolean isDataMedium(BlockData data) {
        if (this.equals(ShipPhysics.MEDIUM_MATERIALS.get(data.getMaterial()))) return true;
        else if (data.matches(BLOCKS.getFirst())) return true; //quick check for common fluid types (index 0)
        else return BLOCKS.contains(data);
    }
    public static boolean isDataMedium(Medium m, BlockData data) {
        return m.isDataMedium(data);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof Medium oth)) return false;
        else return this.id.equalsIgnoreCase(oth.id);
    }
}