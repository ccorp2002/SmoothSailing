package com.ccorp2002.smoothsailing.utils.objects;

import org.bukkit.Location;
import org.joml.Vector3f;
import org.bukkit.util.Vector;

public class Triple {
    public double x;
    public double y;
    public double z;

    public Triple(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Triple remove(Triple triple) {
        this.x -= triple.x;
        this.y -= triple.y;
        this.z -= triple.z;
        return new Triple(x, y, z);
    }

    @Override
    public String toString() {
        return "Triple{" + "x=" + x + ", y=" + y + ", z=" + z + '}';
    }

    /**
     *
     * Converts all values in this to Radians assuming they're degrees (multiplies by pi/180).
     *
     * @return A new triple with the converted values.
     */
    public Triple toRadians() {
        x = Math.toRadians(x);
        y = Math.toRadians(y);
        z = Math.toRadians(z);
        return new Triple(x, y, z);
    }

    /**
     *
     * Uses the modulo operator on each double in this triple using a double m
     *
     * @param m The number modulo by.
     * @return The remainder as a new Triple. (the result)
     */
    public Triple modulo(double m){
        this.x = (x % m);
        this.y = (y % m);
        this.z = (z % m);
        return new Triple(x, y, z);
    }

    /**
     *
     * Makes a new triple of the x, y and z coordinates in a location
     *
     * @param location The location to get the coordinates from.
     */
    public Triple(Location location) {
         this.x = location.getX();
         this.y = location.getY();
         this.z = location.getZ();
    }

    /**
     *
     * Makes a new triple of the x, y and z coordinates in a vector
     *
     * @param location The location to get the coordinates from.
     */
    public Triple(Vector vector) {
         this.x = vector.getX();
         this.y = vector.getY();
         this.z = vector.getZ();
    }

    /**
     *
     * Makes a new triple of the x, y and z coordinates in a single double
     *
     * @param location The location to get the coordinates from.
     */
    public static Triple of(double d) {
        return new Triple(d,d,d);
    }

    /**
     *
     * Makes a new triple of the x, y and z coordinates in a single float
     *
     * @param location The location to get the coordinates from.
     */
    public static Triple of(float f) {
        final double d = (double)f;
        return new Triple(d,d,d);
    }


    /**
     *
     * Makes a new triple of the x, y and z coordinates in a single int
     *
     * @param location The location to get the coordinates from.
     */
    public static Triple of(int i) {
        final double d = (double)i;
        return new Triple(d,d,d);
    }

    /**
     *
     * Makes a new Triple equal to the translation (distance on three axes / displacement) from a to b, or more formally the translation which 'from' needs to be translated to be at the exact position of 'to'.
     * The output is calculated following the following formula: A + T = B, T = B - A. Where A is 'from', B is 'to' and T is the returned Triple.
     *
     * @param from The Triple you're coming from
     * @param to The Triple you're going to.
     * @return A new triple with all three values being equal to the translation from a (from) to b (to).
     */
    public static Triple difference(Triple from, Triple to) {
        return new Triple(to.x - from.x, to.y - from.y, to.z - from.z);
    }

    /**
     *
     * Makes a new Triple equal to the translation (distance on three axes / displacement) from a to b, or more formally the translation which 'from' needs to be translated to be at the exact position of 'to'.
     * The output is calculated following the following formula: A + T = B, T = B - A. Where A is 'from', B is 'to' and T is the returned Triple.
     *
     * @param from The Location you're coming from
     * @param to The Location you're going to.
     * @return A new triple with all three values being equal to the translation from a (from) to b (to).
     */
    public static Triple difference(Location from,  Location to) {
        return new Triple(to.x() - from.x(), to.y() - from.y(), to.z() - from.z());
    }


    /**
     *
     * Makes all values in the triple inverted (negative to positive, positive to negative)
     *
     * @return A new triple also with the inverted values.
     */
    public Triple invert() {
        this.x = -this.x;
        this.y = -this.y;
        this.z = -this.z;
        return new Triple(x, y, z);
    }

    /**
     *
     * Makes a copy of this Triple.
     *
     * @return The copy
     */
    public Triple clone(){
        return new Triple(x, y, z);
    }

    /**
     *
     * Converts to a Vector3f which is often used but offers less accuracy (float vs double), therefore this conversion is inherently imprecise
     *
     * @return The Vector3F that the Triple was converted to.
     */
    public Vector3f toVector3f() {
        return new Vector3f((float) x, (float) y, (float) z);
    }


    
    /**
     *
     * Divides each double in this triple by a double d
     *
     * @param d The number to divide by
     * @return The divided Triple
     */
    public Triple divide(double d) {
        this.x /= d;
        this.y /= d;
        this.z /= d;
        return new Triple(x, y, z);
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
    }
}
