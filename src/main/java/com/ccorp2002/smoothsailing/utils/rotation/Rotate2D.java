package com.ccorp2002.smoothsailing.utils.rotation;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import com.ccorp2002.smoothsailing.utils.displays.*;
import com.ccorp2002.smoothsailing.utils.objects.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;

public class Rotate2D {

    public static Location ROTATE_VEC(final String rotation, final Location location) {
        double theta;
        int x = 0;
        int y = 0;
        int z = 0;
        if (rotation.equalsIgnoreCase("RIGHT")) {
            theta = 0.5 * Math.PI;
            x = (int) Math.round((location.getBlockX() * Math.cos(theta)) + (location.getBlockZ() * (-1 * Math.sin(theta))));
            y = location.getBlockY();
            z = (int) Math.round((location.getBlockX() * Math.sin(theta)) + (location.getBlockZ() * Math.cos(theta)));
        } else if (rotation.equalsIgnoreCase("LEFT")) {
            theta = -1 * 0.5 * Math.PI;
            x = (int) Math.round((location.getBlockX() * Math.cos(theta)) + (location.getBlockZ() * (-1 * Math.sin(theta))));
            y = location.getBlockY();
            z = (int) Math.round((location.getBlockX() * Math.sin(theta)) + (location.getBlockZ() * Math.cos(theta)));
        } else {
            x = location.getBlockX();
            y = location.getBlockY();
            z = location.getBlockZ();
        }
        return new Location(location.getWorld(),x, y, z, location.getYaw(),location.getPitch());
    }
    /**
     *
     * @param angle How many radians the object should be rotated around the center, in degrees (360).
     * @param centerOfRotation The point the object should be rotated around
     * @param object The object that is rotated
     */
    public static Location AddRotation(double angle, Location centerOfRotation, Location object) {
        angle = Math.toRadians(angle);
        double radius = Math.sqrt(Math.pow((centerOfRotation.getX()-object.getX()), 2) + Math.pow((centerOfRotation.getZ()-object.getZ()), 2));
        double totalAngle = angle+Math.atan2(object.getZ()-centerOfRotation.getZ(), object.getX()-centerOfRotation.getX());
        object.setX(centerOfRotation.getX()+Math.cos(totalAngle)*radius);
        object.setZ(centerOfRotation.getZ()+Math.sin(totalAngle)*radius);
        object.setYaw((float) (object.getYaw()+Math.toDegrees(angle)));
        return object;
    }
    /**
     *
     * @param angle How many radians the object should be rotated around the center, in degrees (360).
     * @param centerOfRotation The point the object should be rotated around
     * @param object The object that is rotated
     */
    public static Location AddRotationBlock(double angle, Location centerOfRotation, Location object) {
        centerOfRotation = centerOfRotation.toBlockLocation();
        object = object.toBlockLocation();
        angle = Math.toRadians(angle);
        double radius = Math.sqrt(Math.pow((centerOfRotation.getX()-object.getX()), 2) + Math.pow((centerOfRotation.getZ()-object.getZ()), 2));
        double totalAngle = angle+Math.atan2(object.getZ()-centerOfRotation.getZ(), object.getX()-centerOfRotation.getX());
        object.setX(centerOfRotation.getX()+Math.cos(totalAngle)*radius);
        object.setZ(centerOfRotation.getZ()+Math.sin(totalAngle)*radius);
        return object;
    }

    /**
     *
     * @param angle The angle that the object should be at compared to the center, in degrees (360).
     * @param centerOfRotation The point the object should be rotated around
     * @param object The object that is rotated
     * @param offset The offset the returned location should have in degrees (360)
     *
     * @return The Location
     */
    public static Location SetRotation(double angle, Location centerOfRotation, Location object, double offset) {
        angle = Math.toRadians(angle);
        double radius = Math.sqrt(Math.pow((centerOfRotation.getX()-object.getX()), 2) + Math.pow((centerOfRotation.getZ()-object.getZ()), 2));
        offset = Math.toRadians(offset);
        object.setX(centerOfRotation.getX()+Math.cos(angle+offset)*radius);
        object.setZ(centerOfRotation.getZ()+Math.sin(angle+offset)*radius);
        object.setYaw((float) (Math.toDegrees(angle)));
        return object;
    }
}
