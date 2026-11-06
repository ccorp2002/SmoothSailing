package com.ccorp2002.smoothsailing.utils;

import com.ccorp2002.smoothsailing.utils.Triple;

public class KeyFrame {

    /**
     * The animation that has this keyFrame
     */
    private final Animation parentAnimation;
    /**
     * The time stamp of this keyFrame in seconds, (when in the animation the bone is at the specified location)
     */
    private final float timeStamp;
    /**
     * The 'channel' of this keyFrame, which is basically just what it changes, so will most likely always be either 'rotation' or 'position'
     */
    private final String type;

    /**
     * The xyz values of this keyFrame, if it's a rotation keyFrame this holds the rotation if it's a position keyFrame this holds the position.
     */
    private final Triple xyz;

    /**
     *
     * Makes a new keyFrame with the specified values
     *
     * @param parentAnimation The animation that this keyFrame is part of
     * @param timeStamp The timeStamp (when in the animation the bone is at the specified location)
     * @param type The 'channel', which is basically just what it changes, so will most likely always be either 'rotation' or 'position'
     * @param xyz The xyz values, if it's a rotation keyFrame this holds the rotation if it's a position keyFrame this holds the position
     */
    public KeyFrame(Animation parentAnimation, float timeStamp, String type, Triple xyz) {
        this.parentAnimation = parentAnimation;
        this.timeStamp = timeStamp;
        this.type = type;
        this.xyz = xyz;
    }

    public Animation getParentAnimation() {
        return parentAnimation;
    }

    public Triple getXyz() {
        return xyz.clone();
    }

    public String getType() {
        return type;
    }

    public float getTimeStamp() {
        return timeStamp;
    }
}
