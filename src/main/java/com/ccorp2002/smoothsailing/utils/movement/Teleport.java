package com.ccorp2002.smoothsailing.utils.movement;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import com.ccorp2002.smoothsailing.SmoothSailingPlugin;
import com.ccorp2002.smoothsailing.utils.Bone;
import com.ccorp2002.smoothsailing.utils.ItemDisplayGroup;
import com.ccorp2002.smoothsailing.utils.Triple;

public class Teleport {

    /**
     *
     * Teleports an entire itemDisplayGroup to some location
     *
     * @param itemDisplayGroup The itemDisplayGroup to teleport
     * @param location The location to teleport the itemDisplayGroup to.
     */
    public static void teleportItemDisplayGroup(ItemDisplayGroup itemDisplayGroup, Location location) {
        Bone rootBone = itemDisplayGroup.getRootBone();
        Location pivotPoint = itemDisplayGroup.getPivotPoint();
        Location teleportLocation = new Location(location.getWorld(), location.getX(), location.getY(), location.getZ(), 0, 0);
        for (Bone bone : rootBone.getAllChildrenBones(true)) {
            if (!bone.hasElement()) continue;
            Display itemDisplay = bone.getItemDisplay();
            Location itemDisplayLoc = itemDisplay.getLocation();

            Triple offset = new Triple(itemDisplayLoc.getX()-pivotPoint.getX(), itemDisplayLoc.getY()-pivotPoint.getY(), itemDisplayLoc.getZ()-pivotPoint.getZ());
            itemDisplay.teleport(teleportLocation.clone().add(offset.x, offset.y, offset.z));
            itemDisplay.setTeleportDuration(0);
        }
        itemDisplayGroup.setPivotPoint(teleportLocation);
    }

    /**
     *
     * Teleports an entire itemDisplayGroup to some location
     *
     * @param itemDisplayGroup The itemDisplayGroup to teleport
     * @param location The location to teleport the itemDisplayGroup to.
     */
    public static void teleportItemDisplayGroupWithRotation(ItemDisplayGroup itemDisplayGroup, Location location) {
        Bone rootBone = itemDisplayGroup.getRootBone();
        Location pivotPoint = itemDisplayGroup.getPivotPoint();
        Location teleportLocation = location.clone();
        for (Bone bone : rootBone.getAllChildrenBones(true)) {
            if (!bone.hasElement()) continue;
            Display itemDisplay = bone.getItemDisplay();
            Location itemDisplayLoc = itemDisplay.getLocation();

            Triple offset = new Triple(itemDisplayLoc.getX()-pivotPoint.getX(), itemDisplayLoc.getY()-pivotPoint.getY(), itemDisplayLoc.getZ()-pivotPoint.getZ());
            itemDisplay.teleport(teleportLocation.clone().add(offset.x, offset.y, offset.z));
            itemDisplay.setTeleportDuration(1);
        }
        itemDisplayGroup.setPivotPoint(teleportLocation);
    }


    public static void teleportItemDisplayGroupRelative(ItemDisplayGroup itemDisplayGroup, Triple relativeCoordinates) {
        Bone rootBone = itemDisplayGroup.getRootBone();
        Location pivotPoint = itemDisplayGroup.getPivotPoint();
        Location teleportLocation = new Location(pivotPoint.getWorld(), pivotPoint.getX()+relativeCoordinates.x, pivotPoint.getY()+relativeCoordinates.y, pivotPoint.getZ()+relativeCoordinates.z, 0, 0);
       Bukkit.getScheduler().runTaskLater(
               SmoothSailingPlugin.INSTANCE,
               () -> {
                   for (Bone bone : rootBone.getAllChildrenBones(true)) {
                       if (!bone.hasElement()) continue;

                       Display itemDisplay = bone.getItemDisplay();
                       Location itemDisplayLoc = itemDisplay.getLocation();
                       Triple offset = Triple.difference(pivotPoint, itemDisplayLoc);
                       itemDisplay.teleport(teleportLocation.clone().add(offset.x, offset.y, offset.z));
                       itemDisplay.setTeleportDuration(0);
                   }
                   itemDisplayGroup.setPivotPoint(teleportLocation);
               }, 1
       );

    }

    /**
     *
     * Teleports a single bone without the children.
     *
     * @param bone The bone to teleport
     * @param location The location to teleport the bone to
     */
    public static void teleportSingleBone(Bone bone, Location location) {
        if (!bone.hasElement()) return;
        Display itemDisplay = bone.getItemDisplay();
        itemDisplay.teleport(location);
        itemDisplay.setTeleportDuration(0);
    }

    /**
     *
     * Teleports a single bone without the children using a relative position.
     *
     * @param bone The bone to teleport
     * @param location The relative location to teleport the bone to (the teleport location will inherit the bone from this location. So to teleport the bone to its coordinates + 4 on the x axis in the overworld you'd make a location like this: new Location(Bukkit.getWorlds.getFirst, 4, 0, 0))
     */
    public static void teleportSingleBoneRelative(Bone bone, Location location) {
        if (!bone.hasElement()) return;
        Display itemDisplay = bone.getItemDisplay();
        itemDisplay.teleport(location.add(bone.getItemDisplay().getLocation()));
        itemDisplay.setTeleportDuration(0);
    }

    /**
     *
     * Teleports a bone to a location, and all of its children by the same offset that the bone was teleported by. An example: The bone is at (0, 0, 0) and you input the location (0, 2, 0) in this situation all bones are teleported up by 2. This is what happens when changing a bone's position in a BlockBench animation.
     *
     * @param rootBone The bone that you want to teleport. (This doesn't have to be the rootBone of the itemDisplayGroup).
     * @param location The location you want to teleport the bone to.
     */
    public static void teleportBoneWithChildren(Bone rootBone, Location location) {
        Display rootBoneDisplay = rootBone.getItemDisplay();
        Location rootBoneLoc = rootBoneDisplay.getLocation();
        rootBoneDisplay.teleport(location);
        rootBoneDisplay.setTeleportDuration(0);
        Triple offset = new Triple(rootBoneLoc.getX()-location.getX(), rootBoneLoc.getY()-location.getY(), rootBoneLoc.getZ()-location.getZ());

        for (Bone bone : rootBone.getAllChildrenBones(false)) {
            if (!bone.hasElement()) continue;
            Display itemDisplay = bone.getItemDisplay();
            Location itemDisplayLoc = itemDisplay.getLocation();
            itemDisplay.teleport(itemDisplayLoc.clone().add(offset.x, offset.y, offset.z));
            itemDisplay.setTeleportDuration(0);
        }
    }

    /**
     *
     * Teleports and all of its children to a relative position. The relativeCoords are applied relative to each bone, so every bone will NOT be teleported to the same location, but to the same relative position to themselves.
     *
     * @param rootBone The bone that you want to teleport. (This doesn't have to be the rootBone of the itemDisplayGroup).
     * @param relativeCoords The relative position you want to offset the bone and all of its children by.
     */
    public static void teleportBoneRelativeWithChildren(Bone rootBone, Triple relativeCoords) {

        Bukkit.getScheduler().runTaskLater(
                SmoothSailingPlugin.INSTANCE,
                () -> {
                    for (Bone bone : rootBone.getAllChildrenBones(true)) {
                        if (!bone.hasElement()) continue;

                        Display itemDisplay = bone.getItemDisplay();
                        Location itemDisplayLoc = itemDisplay.getLocation();
                        itemDisplay.teleport(itemDisplayLoc.clone().add(relativeCoords.x, relativeCoords.y, relativeCoords.z));
                        SmoothSailingPlugin.INSTANCE.getLogger().info("Teleport duration: "+itemDisplay.getTeleportDuration());

                        itemDisplay.setTeleportDuration(0);
                    }
                }, 1
        );
    }
}
