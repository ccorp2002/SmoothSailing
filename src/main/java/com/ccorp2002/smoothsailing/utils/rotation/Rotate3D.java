package com.ccorp2002.smoothsailing.utils.rotation;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import com.ccorp2002.smoothsailing.utils.Bone;
import com.ccorp2002.smoothsailing.utils.ItemDisplayGroup;
import com.ccorp2002.smoothsailing.utils.Triple;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Rotate3D {
    /**
     *
     * @param yaw The yaw (side to side) angle in Radians
     * @param pitch The pitch (up / down), angle in Radians
     * @param centerOfRotation The location to rotate the object around
     * @param object The object to rotate
     * @return Returns the new location of the object
     */
    public static Location SetRotation(double yaw, double pitch , Location centerOfRotation, Location object) {
        Location rotatedLoc = Rotate2D.SetRotation(yaw, centerOfRotation, object, 0);
        double relativeZ = rotatedLoc.getZ() - centerOfRotation.getZ();
        double relativeX = rotatedLoc.getX() - centerOfRotation.getX();
        double relativeY = rotatedLoc.getY() - centerOfRotation.getY();

        double sinZYaw = Math.sin(yaw);
        double cosXYaw = Math.cos(yaw);

        double radius2D = Math.sqrt(Math.pow(relativeX, 2) + Math.pow(relativeZ, 2));
        double radius = Math.sqrt(Math.pow(relativeY, 2) + Math.pow(radius2D, 2));

        object.setX(centerOfRotation.getX()+Math.cos(pitch)*radius*cosXYaw);
        object.setZ(centerOfRotation.getZ()+Math.cos(pitch)*radius*sinZYaw);
        object.setY(centerOfRotation.getY()+Math.sin(pitch)*radius);
        return object;
    }

    public static Location AddRotation(double yaw, double pitch, Location centerOfRotation, Location object) {
        Location rotatedLoc = Rotate2D.AddRotation(yaw, centerOfRotation, object);
        double totalYaw = yaw+Math.atan2(object.getZ()-centerOfRotation.getZ(), object.getX()-centerOfRotation.getX());
        double relativeZ = rotatedLoc.getZ() - centerOfRotation.getZ();
        double relativeX = rotatedLoc.getX() - centerOfRotation.getX();
        double relativeY = rotatedLoc.getY() - centerOfRotation.getY();

        double sinZYaw = Math.sin(totalYaw);
        double cosXYaw = Math.cos(totalYaw);

        double radius2D = Math.sqrt(Math.pow(relativeX, 2) + Math.pow(relativeZ, 2));
        double radius = Math.sqrt(Math.pow(relativeY, 2) + Math.pow(radius2D, 2));
        double totalPitch = pitch+Math.atan2(object.getY()-centerOfRotation.getY(), radius);

        World world = centerOfRotation.getWorld();
        //world.sendMessage(Component.text("pitch: "+Math.toDegrees(totalPitch)));
        //world.sendMessage(Component.text("yaw: "+Math.toDegrees(totalYaw)));
        //world.sendMessage(Component.text("2d radius: "+radius2D));
        //world.sendMessage(Component.text("3d radius: "+radius));

        object.setX(centerOfRotation.getX()+Math.cos(totalPitch)*radius*cosXYaw);
        object.setZ(centerOfRotation.getZ()+Math.cos(totalPitch)*radius*sinZYaw);
        object.setY(centerOfRotation.getY()+Math.sin(totalPitch)*radius);
        return object;
    }

    

    /**
     * Rotates an ItemDisplay around a center point in 3D.
     *
     * @param display   The ItemDisplay to rotate
     * @param center    The rotation center
     * @param yawDeg    Rotation around Y axis (in degrees)
     * @param pitchDeg  Rotation around X axis (in degrees)
     * @param rollDeg   Rotation around Z axis (in degrees)
     */
    public static Location rotateAroundCenter(Entity display, Location center,
                                          double yawDeg, double pitchDeg, double rollDeg) {
        Location loc = display.getLocation();

        // Vector from center to display
        Vector3f rel = new Vector3f(
                (float)(loc.getX() - center.getX()),
                (float)(loc.getY() - center.getY()),
                (float)(loc.getZ() - center.getZ())
        );

        // Convert to radians
        float yawRad   = (float) Math.toRadians(yawDeg);
        float pitchRad = (float) Math.toRadians(pitchDeg);
        float rollRad  = (float) Math.toRadians(rollDeg);

        // Build rotation matrix
        Matrix4f rot = new Matrix4f()
                .rotateY(yawRad)    // yaw
                .rotateX(pitchRad)  // pitch
                .rotateZ(rollRad);  // roll

        // Apply rotation
        rel.mulPosition(rot);

        // New world position = center + rotated vector
        Vector3f newPos = new Vector3f(
                (float) center.getX(),
                (float) center.getY(),
                (float) center.getZ()
        ).add(rel);

        loc.set(newPos.x, newPos.y, newPos.z);
        return loc;
    }

    

    

    /**
     * Rotates an ItemDisplay around a center point in 3D.
     *
     * @param display   The ItemDisplay to rotate
     * @param center    The rotation center
     * @param yawDeg    Rotation around Y axis (in degrees)
     * @param pitchDeg  Rotation around X axis (in degrees)
     * @param rollDeg   Rotation around Z axis (in degrees)
     */
    public static Location rotateAroundCenter(Location location, Location center,
                                          double yawDeg, double pitchDeg, double rollDeg) {
        Location loc = location.clone().toBlockLocation();

        // Vector from center to display
        Vector3f rel = new Vector3f(
                (float)(loc.getX() - center.getX()),
                (float)(loc.getY() - center.getY()),
                (float)(loc.getZ() - center.getZ())
        );

        // Convert to radians
        float yawRad   = (float) Math.toRadians(yawDeg);
        float pitchRad = (float) Math.toRadians(pitchDeg);
        float rollRad  = (float) Math.toRadians(rollDeg);

        // Build rotation matrix
        Matrix4f rot = new Matrix4f()
                .rotateY(yawRad)    // yaw
                .rotateX(pitchRad)  // pitch
                .rotateZ(rollRad);  // roll

        // Apply rotation
        rel.mulPosition(rot);

        // New world position = center + rotated vector
        Vector3f newPos = new Vector3f(
                (float) center.getX(),
                (float) center.getY(),
                (float) center.getZ()
        ).add(rel);

        loc.set(newPos.x, newPos.y, newPos.z);
        return loc;
    }
    

    

    /**
     * Rotates an ItemDisplay around a center point in 3D.
     *
     * @param display   The ItemDisplay to rotate
     * @param center    The rotation center
     * @param yawDeg    Rotation around Y axis (in degrees)
     * @param pitchDeg  Rotation around X axis (in degrees)
     * @param rollDeg   Rotation around Z axis (in degrees)
     */
    public static Location ROTATE_VEC(Location location,
                                          double yawDeg, double pitchDeg, double rollDeg) {
        Location loc = location.clone();

        // Vector from center to display
        Vector3f rel = new Vector3f(
                (float)(0d),
                (float)(1d),
                (float)(0d)
        );

        // Convert to radians
        float yawRad   = (float) Math.toRadians(yawDeg);
        float pitchRad = (float) Math.toRadians(pitchDeg);
        float rollRad  = (float) Math.toRadians(rollDeg);

        // Build rotation matrix
        Matrix4f rot = new Matrix4f()
                .rotateY(yawRad)    // yaw
                .rotateX(pitchRad)  // pitch
                .rotateZ(rollRad);  // roll

        // Apply rotation
        rel.mulPosition(rot);

        loc.set(rel.x, rel.y, rel.z);
        return loc;
    }
}
