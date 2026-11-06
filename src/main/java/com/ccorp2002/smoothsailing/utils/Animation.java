package com.ccorp2002.smoothsailing.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ccorp2002.smoothsailing.utils.Bone;
import com.ccorp2002.smoothsailing.utils.Triple;

import java.util.*;

public class Animation {

    /**
     * Every keyframe for every bone, this is used to quickly identify what a bone should be doing in an animation.
     */
    private final Map<Bone, List<KeyFrame>> keyFrames;
    /**
     * The name of this animation
     */
    private final String name;
    /**
     * The uuid of this animation
     */
    private final String uuid;

    /**
     * The loopMode of an animation, this can have 1 of 3 values:
     *   <ul>
     *    <li>ONCE: Play this animation once, then reset to start and stop</li>
     *    <li>LOOP: Loop this animation indefinitely </li>
     *    <li>HOLD: Play this animation once, then pause on last frame</li>
     *   </ul>
     */
    public enum LoopMode{ONCE, LOOP, HOLD}

    /**
     * The loopMode of this animation, this can have 1 of 3 values:
     *   <ul>
     *    <li>ONCE: Play this animation once, then reset to start and stop</li>
     *    <li>LOOP: Loop this animation indefinitely </li>
     *    <li>HOLD: Play this animation once, then pause on last frame</li>
     *   </ul>
     */
    private final LoopMode loopMode;
    // TODO maybe implement missing features: Anim Time Update, Blend Weight, Start Delay, Loop Delay, Override, Snapping and interpolation type.
    /**
     * How long the animation is in seconds
     */
    private final float length;


    /**
     *
     * @param name The name of this animation
     * @param uuid The uuid of this animation
     * @param loopMode      The loopMode of this animation, this can have 1 of 3 values:
     *        <ul>
     *         <li>ONCE: Play this animation once, then reset to start and stop</li>
     *         <li>LOOP: Loop this animation indefinitely </li>
     *          <li>HOLD: Play this animation once, then pause on last frame</li>
     *         </ul>
     * @param keyFrames A map of keyframes indexed by bone, this should contain every keyframe for every bone.
     */
    public Animation(String name, String uuid, LoopMode loopMode, Map<Bone, List<KeyFrame>> keyFrames, float length) {
        this.name = name;
        this.uuid = uuid;
        this.loopMode = loopMode;
        this.keyFrames = keyFrames;
        this.length = length;
    }

    /**
     *
     * @param rootBone The rootBone, meaning the bone at the top of the bone hierarchy aka the bone with no parent.
     * @param animation The animation JsonObject, with the animation to read from in the .bbmodel file
     */
    public Animation(JsonObject animation, Bone rootBone) {

        // Get and set uuid
        this.uuid = animation.get("uuid").getAsString();

        this.length = animation.get("length").getAsFloat();

        // Get and set loop mode
        String loopMode = animation.get("loop").getAsString();
        this.loopMode = LoopMode.valueOf(loopMode.toUpperCase(Locale.ROOT));

        // Set name
        this.name = animation.get("name").getAsString();

        if (!animation.has("animators")) {
            this.keyFrames = new HashMap<>();
            return;
        }

        // Get the JsonObject containing all keyFrames
        JsonObject animators = animation.getAsJsonObject("animators");

        Map<Bone, List<KeyFrame>> keyFrames = new HashMap<>();

        for (Bone bone : rootBone.getAllChildrenBones(true)){
            if (!animators.has(bone.getUUID())){continue;}

            JsonObject animator = animators.get(bone.getUUID()).getAsJsonObject();

            List<KeyFrame> animatorKeyFrames = new ArrayList<>();

            if(!animator.has("keyframes")){continue;}

            for (JsonElement keyframeElement : animator.get("keyframes").getAsJsonArray()){
                if (!(keyframeElement instanceof JsonObject keyframe)) {continue;}
                JsonArray dataPoints = keyframe.getAsJsonArray("data_points");
                JsonObject xyz = dataPoints.get(0).getAsJsonObject();
                addSortedKeyFrame(animatorKeyFrames, new KeyFrame(this, keyframe.get("time").getAsFloat(), keyframe.get("channel").getAsString(), new Triple(xyz.get("x").getAsDouble(), xyz.get("y").getAsDouble(), xyz.get("z").getAsDouble())), animatorKeyFrames.size());
            }
            keyFrames.put(bone, animatorKeyFrames);
        }

        this.keyFrames = keyFrames;

    }

    /**
     *
     * Adds a single keyFrame to a list of keyFrames, and sorts it so the new keyFrame is placed chronologically, assuming the list is already chronological.
     *
     * @param keyFrames The chronological list of keyFrames
     * @param keyFrame The keyFrame to add in the right position
     * @param index The index to try to add it to, the item will always be added to either the specified index or a lower one, if negative it will be set to the last index. This should be set to keyFrames.size()
     * @return The chronological list with the keyFrame added.
     */
    private List<KeyFrame> addSortedKeyFrame(List<KeyFrame> keyFrames, KeyFrame keyFrame, int index) {
        if (index > keyFrames.size() || index < 0) {index = keyFrames.size();}

        if (keyFrames.isEmpty()) {
            keyFrames.add(keyFrame);
            return keyFrames;
        }

        if (index == 0) {
            keyFrames.add(0, keyFrame);
            return keyFrames;
        }

        if (keyFrames.get(index-1).getTimeStamp() <= keyFrame.getTimeStamp()) {
            keyFrames.add(index, keyFrame);
            return keyFrames;
        }
        else  {
            return addSortedKeyFrame(keyFrames, keyFrame, index-1);
        }
    }

    public String getName() {
        return name;
    }

    public LoopMode getLoopMode() {
        return loopMode;
    }

    public String getUuid() {
        return uuid;
    }

    public float getLength() {
        return length;
    }

    /**
     * Gets a map of keyframes where the key is the bone that the value applies to
     * @return list of key frames
     */
    public Map<Bone, List<KeyFrame>> getKeyFrames() {
        return keyFrames;
    }
}
