package com.ccorp2002.smoothsailing.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import com.ccorp2002.smoothsailing.SmoothSailingPlugin;
import com.ccorp2002.smoothsailing.utils.Bone;
import com.ccorp2002.smoothsailing.utils.Animation;
import com.ccorp2002.smoothsailing.utils.KeyFrame;
import com.ccorp2002.smoothsailing.utils.movement.TeleportSmooth;
import com.ccorp2002.smoothsailing.utils.movement.Teleport;
import com.ccorp2002.smoothsailing.utils.rotation.Rotate;
import com.ccorp2002.smoothsailing.utils.rotation.RotateSmooth;
import com.ccorp2002.smoothsailing.utils.Triple;

import java.io.File;
import java.util.*;

/**
 * A group of ItemDisplays that are connected, that way you can move and rotate them around a center without their relative position to each other breaking.
 */
public class ItemDisplayGroup {

    public enum AnimationState{RUNNING, HOLDING, PAUSED, FREE}

    //Class variables
    public String groupUUID;
    public Location pivotPoint;
    public Bone rootBone;
    public double yaw;
    public double pitch;
    public double roll;
    public AnimationState animationState;

    /**
     * All the animations this ItemDisplayGroup has as a map where the key is a string name of the animation and the value is an Animation class.
     */
    public Map<String, Animation>  animations;

    //Constructor
    /**
     *
     * This constructor makes static itemDisplayGroups, as there's no animation references
     *
     * @param pivotPoint The location of the center which the itemDisplayGroup should be rotated around.
     * @param rootBone The bone at the top of the bone hiearchy, this is obviously also the bone with no parent and all other bones as children/grandchildren or any other generation under it.
     *
     */
    public ItemDisplayGroup(Location pivotPoint, Bone rootBone) {
        if (pivotPoint == null || rootBone == null) {
            throw new IllegalArgumentException("Display groups cannot be intialized with null or empty values.");
        }
        this.pivotPoint = pivotPoint;
        this.rootBone = rootBone;
        this.yaw = 0;
        this.pitch = 0;
        this.roll = 0;
        this.groupUUID = UUID.randomUUID().toString();
        this.animations = new HashMap<String, Animation>();
    }

    //Constructor
    /**
     *
     * This constructor makes static itemDisplayGroups, as there's no animation references
     *
     * @param pivotPoint The location of the center which the itemDisplayGroup should be rotated around.
     * @param rootBone The bone at the top of the bone hiearchy, this is obviously also the bone with no parent and all other bones as children/grandchildren or any other generation under it.
     *
     */
    public ItemDisplayGroup(Location pivotPoint, Display root) {
        if (pivotPoint == null || root == null || !root.isValid()) {
            throw new IllegalArgumentException("Display groups cannot be intialized with null or empty values.");
        }
        this.pivotPoint = pivotPoint;
        this.rootBone = Bone.from(root);
        this.yaw = 0;
        this.pitch = 0;
        this.roll = 0;
        this.groupUUID = UUID.randomUUID().toString();
        this.animations = new HashMap<String, Animation>();
    }

    //Constructor
    /**
     *
     * This constructor makes static itemDisplayGroups, as there's no animation references
     *
     * @param pivotPoint The location of the center which the itemDisplayGroup should be rotated around.
     * @param rootBone The bone at the top of the bone hiearchy, this is obviously also the bone with no parent and all other bones as children/grandchildren or any other generation under it.
     *
     */
    public ItemDisplayGroup(Location pivotPoint, Display root, Collection<Entity> others) {
        if (pivotPoint == null || root == null || !root.isValid()) {
            throw new IllegalArgumentException("Display groups cannot be intialized with null or empty values.");
        }
        this.pivotPoint = pivotPoint;
        this.rootBone = Bone.from(root,others);
        this.yaw = 0;
        this.pitch = 0;
        this.roll = 0;
        this.groupUUID = UUID.randomUUID().toString();
        this.animations = new HashMap<String, Animation>();
    }

    /**
     *
     * Spawn this itemDisplayGroup at its pivot point (the rotation doesn't matter)
     * //TODO make it so this doesn't always return true, or make it not return anything
     * @return Currently always returns true
     */
    public boolean spawn(){
        return true;
    }

    public void resetBonesPosAndRot(){
        rootBone.resetLocation(this, true);
    }

    /**
     *
     * Makes this itemDisplayGroup play an animation with the given name.
     *
     * @param animationName The name of the animation, this is the same as it's called in BlockBench.
     * @return False if the animation couldn't be found, or there's already an animation running, otherwise true.
     */


//    public boolean playAnimation(String animationName){
//        if (!this.animations.containsKey(animationName)) return false;
//        if (animationState == AnimationState.RUNNING) return false;
//
//        if(animationState == AnimationState.PAUSED || animationState == AnimationState.HOLDING){
//            resetBonesPosAndRot();
//        }
//
//        animationState = AnimationState.RUNNING;
//
//        Animation animation = this.animations.get(animationName);
//        Map<Bone, List<KeyFrame>> keyframes = animation.getKeyFrames();
//
//        Animation.LoopMode mode = animation.getLoopMode();
//        Bone[] boneArray = keyframes.keySet().toArray(new Bone[0]);
//
//        for (int i = 0; i < boneArray.length; i++) {
//            List<KeyFrame> allBoneKeyFrames = keyframes.get(boneArray[i]);
//            List<List<KeyFrame>> sortedKeyframes = new ArrayList<>();
//
//            float lastTiming = 0;
//            List<KeyFrame> tempKeyframeList = new ArrayList<>();
//            for(KeyFrame keyFrame : allBoneKeyFrames){
//                if(keyFrame.getTimeStamp() > lastTiming){
//                    if(!tempKeyframeList.isEmpty()) sortedKeyframes.add(tempKeyframeList);
//                    tempKeyframeList = new ArrayList<>();
//                    lastTiming = keyFrame.getTimeStamp();
//                    tempKeyframeList.add(keyFrame);
//                }
//                else {
//                    tempKeyframeList.add(keyFrame);
//                }
//            }
//
//            if(!tempKeyframeList.isEmpty()) sortedKeyframes.add(tempKeyframeList);
//
//            int finalI = i;
//            int finalAnimationDuration = Math.round(animation.getLength()) * 20;
//            new BukkitRunnable(){
//                int j = 0;
//                int currentDuration = 0;
//                boolean stop;
//
//                int tickDelayRotation = 0;
//                int tickDelayPosition = 0;
//                int tickDelayStop = 0;
//                @Override
//                public void run() {
//                    if(tickDelayStop != 0){
//                        tickDelayStop--;
//                        return;
//                    };
//                    if(stop){
//                        cancel();
//                        if(mode == Animation.LoopMode.LOOP){
//                            if(finalI == boneArray.length-1){
//                                animationState = AnimationState.FREE;
//
//                                resetBonesPosAndRot();
//                                playAnimation(animationName);
//                            }
//                        }
//                        else if (mode == Animation.LoopMode.ONCE){
//                            animationState = AnimationState.FREE;
//                            resetBonesPosAndRot();
//                        }
//                        else if (mode == Animation.LoopMode.HOLD){
//                            animationState = AnimationState.HOLDING;
//                        }
//                        return;
//                    }
//                    if (j < sortedKeyframes.size()) {
//                        if(tickDelayPosition == 0 || tickDelayRotation == 0){
//                            int loopDuration = 0;
//                            if (j < sortedKeyframes.size() - 1) {
//                                KeyFrame keyFrame = sortedKeyframes.get(j).getFirst();
//                                loopDuration = Math.round((sortedKeyframes.get(j + 1).getFirst().getTimeStamp() - keyFrame.getTimeStamp()) * 20);
//                            }
//                            boolean updated = false;
//                            for (KeyFrame keyFrame : sortedKeyframes.get(j)) {
//
//                                }
//                            }
//
//                            if(updated){
//                                currentDuration += loopDuration;
//                                j++;
//                            }
//                        }
//                        if(tickDelayPosition > 0) tickDelayPosition -= 1;
//                        if(tickDelayRotation > 0) tickDelayRotation -= 1;
//
//                    } else {
//                        tickDelayStop = (finalAnimationDuration - currentDuration) + 1;
//                        stop = true;
//                    }
//
//                }
//            }.runTaskTimer(SmoothSailingPlugin.INSTANCE, 0L, 1);
//        }
//
//        return true;
//    }

    /**
     *
     * Teleports this itemDisplayGroup to some Location, this doesn't affect the rotation of the itemDisplayGroup
     *
     * @param location The location to teleport the itemDisplayGroup to.
     */
    public void teleport(Location location){
        Teleport.teleportItemDisplayGroup(this, location);
    }

    /**
     *
     * Teleports this itemDisplayGroup by coordinates (x, y, z) relative to the pivotPoint of this itemDisplayGroup.
     *
     * @param relativeCoordinates A triple containing the relative coordinates you want to teleport the ItemDisplayGroup to.
     */
    public void teleportRelative(Triple relativeCoordinates){
        Teleport.teleportItemDisplayGroupRelative(this, relativeCoordinates);
    }

    /**
     *
     * Teleports this itemDisplayGroup to some Location smoothly (interpolates the teleport), this doesn't affect the rotation of the itemDisplayGroup
     *
     * @param location The location to teleport the itemDisplayGroup to.
     * @param teleportDuration How long the teleport should last (how long the ItemDisplayGroup takes to get from start loc to end loc.)
     */
    public void teleportSmooth(Location location, int teleportDuration){
        TeleportSmooth.TeleportItemDisplayGroupSmooth(this, location, teleportDuration);
    }

    /**
     *
     * Smoothly (interpolates) teleports this itemDisplayGroup by coordinates (x, y, z) relative to the pivotPoint of this itemDisplayGroup.
     *
     * @param relativeCoordinates A triple containing the relative coordinates you want to teleport the ItemDisplayGroup to.
     * @param teleportDuration How long the teleport should last (how long the ItemDisplayGroup takes to get from start loc to end loc.)
     */
    public void teleportRelativeSmooth(Triple relativeCoordinates, int teleportDuration){
        TeleportSmooth.TeleportItemDisplayGroupRelativeSmooth(this, relativeCoordinates, teleportDuration);
    }

    /**
     *
     * Sets the rotation of the whole itemDisplayGroup, using 4x4 matrices.
     *
     * @param rotation The rotation as an euler angle (3 angles) in degrees (360)
     */
    public void setRotation(Triple rotation){
        Rotate.SetRotationItemDisplayGroup(this, rotation);
        this.setRotationInEulerAngles(rotation);
    }

    /**
     *
     * Adds some rotation to the whole itemDisplayGroup, using 4x4 matrices.
     *
     * @param rotation The rotation to add as an euler angle (3 angles) in degrees (360)
     */
    public void addRotation(Triple rotation){
        Rotate.AddRotationItemDisplayGroup(this, rotation);
        Triple oldRotation = this.getRotationInEulerAngles();
        this.setRotationInEulerAngles(new Triple(oldRotation.x+ rotation.x, oldRotation.y+rotation.y, oldRotation.z+rotation.z));
    }

    /**
     *
     * Sets the rotation smoothly of the whole itemDisplayGroup, using 4x4 matrices.
     *
     * @param rotation The rotation as an euler angle (3 angles) in degrees (360)
     * @param interpolationDuration How long it should take the itemDisplayGroup to get from its start rotation to the rotation given.
     */
    public void setRotationSmooth(Triple rotation, int interpolationDuration){
        RotateSmooth.SetRotationItemDisplayGroupSmooth(this, rotation, interpolationDuration);
        this.setRotationInEulerAngles(rotation);
    }

    /**
     *
     * Smoothly adds some rotation to the whole itemDisplayGroup, using 4x4 matrices.
     *
     * @param rotation The rotation to add as an euler angle (3 angles) in degrees (360)
     */
    public void addRotationSmooth(Triple rotation, int interpolationDuration){
        ItemDisplayGroup itemDisplayGroup = this;
        Triple subdividedRotation = rotation.clone().divide(interpolationDuration);
        new BukkitRunnable() {

            int counter = 0;

            @Override
            public void run() {
                if(counter >= interpolationDuration){
                    this.cancel();
                    return;
                }
                counter++;
                RotateSmooth.AddRotationItemDisplayGroupSmooth(itemDisplayGroup, subdividedRotation, 1);
                Triple oldRotation = itemDisplayGroup.getRotationInEulerAngles();
                itemDisplayGroup.setRotationInEulerAngles(new Triple(oldRotation.x+ subdividedRotation.x, oldRotation.y+subdividedRotation.y, oldRotation.z+subdividedRotation.z));
            }
        }.runTaskTimer(SmoothSailingPlugin.INSTANCE, 0L, 1);
    }

    public double GetYaw() {
        return this.yaw;
    }

    public double GetPitch() {
        return this.pitch;
    }

    public double GetRoll() {
        return this.roll;
    }

    /**
     * Gets the rotation as a triple in the following order RPY (Yaw, Pitch, Roll)
     * @return The triple holding the euler angles (in degrees)
     */
    public Triple getRotationInEulerAngles() {
        return new Triple(this.yaw, this.pitch, this.roll);
    }

    /**
     * Sets the stored rotation as a triple in the following order RPY (Yaw, Pitch, Roll)
     *
     * @param rotation The rotation as an euler angle in the following order (Yaw, Pitch, Roll) in degrees.
     */
    public void setRotationInEulerAngles(Triple rotation) {
        this.yaw = rotation.x;
        this.pitch = rotation.y;
        this.roll = rotation.z;
    }

    public void setPivotPoint(Location pivotPoint) {
        this.pivotPoint = pivotPoint;
    }

    public Location getPivotPoint() {
        return this.pivotPoint;
    }

    public Bone getRootBone() {return this.rootBone;}

    public AnimationState getAnimationState() {return this.animationState;}

    public void setAnimationState(AnimationState animationState) {
        this.animationState = animationState;
    }
}
