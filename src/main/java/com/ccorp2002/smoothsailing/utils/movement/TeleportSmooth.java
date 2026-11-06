package com.ccorp2002.smoothsailing.utils.movement;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import com.ccorp2002.smoothsailing.SmoothSailingPlugin;
import com.ccorp2002.smoothsailing.utils.displays.*;
import com.ccorp2002.smoothsailing.utils.objects.*;
import org.joml.Matrix4f;

public class TeleportSmooth {

    /**
     *
     * Teleports an itemDisplayGroup to a location smoothly (with interpolation). This doesn't affect the rotation.
     *
     * @param itemDisplayGroup The itemDisplayGroup to teleport.
     * @param location The location to teleport the itemDisplayGroup to.
     * @param teleportDuration How long the teleport should take. (How long it should take for the itemDisplayGroup to get from start loc to end loc)
     */
    public static void TeleportItemDisplayGroupSmooth(ItemDisplayGroup itemDisplayGroup, Location location, int teleportDuration){

        for (Bone bone : itemDisplayGroup.getRootBone().getAllChildrenBones(true)){
            if(!bone.hasElement()) continue;
            bone.getItemDisplay().setTeleportDuration(teleportDuration);
        }

        itemDisplayGroup.teleport(location);
    }

    /**
     *
     * Teleports an itemDisplayGroup to a relative location smoothly (with interpolation). This doesn't affect the rotation.
     *
     * @param itemDisplayGroup The itemDisplayGroup to teleport.
     * @param relativeCoordinates The relative coordinates to teleport the itemDisplayGroup to.
     * @param teleportDuration How long the teleport should take. (How long it should take for the itemDisplayGroup to get from start loc to end loc)
     */
    public static void TeleportItemDisplayGroupRelativeSmooth(ItemDisplayGroup itemDisplayGroup, Triple relativeCoordinates, int teleportDuration){
        for (Bone bone : itemDisplayGroup.getRootBone().getAllChildrenBones(true)){
            if(!bone.hasElement()) continue;

            bone.getItemDisplay().setTeleportDuration(teleportDuration);
        }

        Teleport.teleportItemDisplayGroupRelative(itemDisplayGroup, relativeCoordinates);
    }

    /**
     *
     * Smoothly teleports a bone to a location, and all of its children by the same offset that the bone was teleported by. An example: The bone is at (0, 0, 0) and you input the location (0, 2, 0) in this situation all bones are teleported up by 2. This is what happens when changing a bone's position in a BlockBench animation.
     *
     * @param rootBone The bone that you want to teleport. (This doesn't have to be the rootBone of the itemDisplayGroup).
     * @param location The location you want to teleport the bone to.
     * @param teleportDuration How long it should take the bones to get from their start position to their end position.
     */
    public static void TeleportBoneWithChildrenSmooth(Bone rootBone, Location location,  int teleportDuration){
        for (Bone bone : rootBone.getAllChildrenBones(true)){
            if(!bone.hasElement()) continue;
            Display  itemDisplay = bone.getItemDisplay();
            itemDisplay.setTeleportDuration(teleportDuration);
        }
        Teleport.teleportBoneWithChildren(rootBone, location);
    }

    /**
     *
     * Smoothly teleports and all of its children to a relative position. The relativeCoords are applied relative to each bone, so every bone will NOT be teleported to the same location, but to the same relative position to themselves.
     *
     * @param rootBone The bone that you want to teleport. (This doesn't have to be the rootBone of the itemDisplayGroup).
     * @param relativeCoords The relative position you want to offset the bone and all of its children by.
     * @param teleportDuration How long it should take the bones to get from their start position to their end position.
     */
    public static void TeleportBoneRelativeWithChildrenSmooth(Bone rootBone, Triple relativeCoords,  int teleportDuration) {
        for (Bone bone : rootBone.getAllChildrenBones(true)) {
            if (!bone.hasElement()) continue;
            Display itemDisplay = bone.getItemDisplay();
            itemDisplay.setTeleportDuration(teleportDuration);
        }
        Teleport.teleportBoneRelativeWithChildren(rootBone, relativeCoords);
    }
}
