package com.ccorp2002.smoothsailing.utils.rotation;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import com.ccorp2002.smoothsailing.utils.displays.*;
import com.ccorp2002.smoothsailing.utils.objects.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Rotate {

    /**
     *
     * Sets the rotation of an ItemDisplayGroup around all 3 axis using a 4x4 matrix.
     *
     * @param itemDisplayGroup The itemDisplayGroup to rotate
     * @param rotation The rotation which is an euler angle using degrees (360)
     */
    public static void SetRotationItemDisplayGroup(ItemDisplayGroup itemDisplayGroup, Triple rotation) {
        rotation.x = rotation.x % 360;
        rotation.y = rotation.y % 360;
        rotation.z = rotation.z % 360;

        Location pivotPoint = itemDisplayGroup.getPivotPoint();

        Bone rootBone = itemDisplayGroup.getRootBone();

        // Loop through all bones
        for (Bone bone : rootBone.getAllChildrenBones(true)){
            if (!bone.hasElement()) continue;
            Display itemDisplay = bone.getItemDisplay();

            // Get offset to translate by.
            Location itemDisplayLoc = itemDisplay.getLocation();
            Triple offset = Triple.difference(itemDisplayLoc, pivotPoint);
            Vector3f translation = offset.toVector3f();
            Vector3f negTranslation = offset.invert().toVector3f();

            // Create Matrix and apply rotation.
            Matrix4f currentMatrix = new Matrix4f();
            currentMatrix.translate(translation);
            currentMatrix.rotateXYZ(new Vector3f((float) Math.toRadians(rotation.x), (float) Math.toRadians(rotation.y), (float) Math.toRadians(rotation.z)));
            currentMatrix.translate(negTranslation);

            // Apply matrix
            itemDisplay.setTransformationMatrix(currentMatrix);
        }
    }

    /**
     *
     * Sets the rotation of an ItemDisplayGroup around all 3 axis using a 4x4 matrix.
     *
     * @param itemDisplayGroup The itemDisplayGroup to rotate
     * @param rotation The rotation which is an euler angle using degrees (360)
     */
    public static void SetRotationItemDisplayGroupViaTeleport(ItemDisplayGroup itemDisplayGroup, Triple rotation) {
        //rotation.x = rotation.x % 360;
        //rotation.y = rotation.y % 360;
        //rotation.z = rotation.z % 360;

        Location pivotPoint = itemDisplayGroup.getPivotPoint();

        Bone rootBone = itemDisplayGroup.getRootBone();

        // Loop through all bones
        for (Bone bone : rootBone.getAllChildrenBones(true)){
            Display itemDisplay = bone.getItemDisplay();
            if (itemDisplay == null) continue;

            // Get offset to translate by.
            Location itemDisplayLoc = itemDisplay.getLocation();
            System.out.println("INI LOC: "+itemDisplayLoc.toString());
            System.out.println("INI: Y "+itemDisplayLoc.getYaw()+" / P"+itemDisplayLoc.getPitch());
            Location newLoc = Rotate3D.rotateAroundCenter(itemDisplay,pivotPoint,rotation.x,rotation.y,rotation.z);
            System.out.println("NEW: Y "+newLoc.getYaw()+" / P"+newLoc.getPitch());
            System.out.println("NEW LOC: "+newLoc.toString());
            itemDisplay.teleport(newLoc);
            //Triple offset = Triple.difference(itemDisplayLoc, pivotPoint);
            //Vector3f translation = offset.toVector3f();
            //Vector3f negTranslation = offset.invert().toVector3f();

            //Create Matrix and apply rotation.
            //Matrix4f currentMatrix = new Matrix4f();
            //currentMatrix.translate(translation);
            //currentMatrix.rotateXYZ(new Vector3f((float) Math.toRadians(rotation.x), (float) Math.toRadians(rotation.y), (float) Math.toRadians(rotation.z)));
            //currentMatrix.translate(negTranslation);

            // Apply matrix
            //itemDisplay.setTransformationMatrix(currentMatrix);
        }
    }

    /**
     *
     * Rotates an ItemDisplayGroup around all 3 axis using the itemDisplay's transformation.
     *
     * @param itemDisplayGroup The itemDisplayGroup to rotate
     * @param rotation The rotation to add which is an euler angle (3 angles) using degrees (360)
     */
    public static void AddRotationItemDisplayGroup(ItemDisplayGroup itemDisplayGroup, Triple rotation) {
        Triple oldRotation = itemDisplayGroup.getRotationInEulerAngles();
        Triple newRotation = new Triple(oldRotation.x + rotation.x, oldRotation.y + rotation.y, oldRotation.z + rotation.z);
        SetRotationItemDisplayGroup(itemDisplayGroup, newRotation);
    }


    /**
     *
     * Sets the rotation of a single bone around some external pivot point.
     *
     * @param bone The bone to rotate
     * @param pivotPoint The pivotPoint to rotate around (use SetBoneRotationAroundRelative to use a relative Triple)
     * @param rotation The rotation as a triple representing euler angles in degrees.
     */
    public static void SetBoneRotationAround(Bone bone, Location pivotPoint, Triple rotation) {
        if (!bone.hasElement()) return;

        rotation.x = rotation.x % 360;
        rotation.y = rotation.y % 360;
        rotation.z = rotation.z % 360;

        Display itemDisplay = bone.getItemDisplay();

        Matrix4f currentMatrix = new Matrix4f();
        Location currentLocation = itemDisplay.getLocation();
        Vector3f translation = new Vector3f((float) (pivotPoint.x()-currentLocation.x()), (float) (pivotPoint.y()-currentLocation.y()), (float) (pivotPoint.z()-currentLocation.z()));
        currentMatrix.translate(translation);
        currentMatrix.rotateXYZ(new Vector3f((float) Math.toRadians(rotation.x), (float) Math.toRadians(rotation.y), (float) Math.toRadians(rotation.z)));
        currentMatrix.translate(new Vector3f(-translation.x(), -translation.y(), -translation.z()));
        itemDisplay.setTransformationMatrix(currentMatrix);
    }

    /**
     *
     * Sets the rotation of a single bone.
     *
     * @param bone The bone to rotate
     * @param rotation The rotation as a triple representing euler angles in degrees.
     */
    public static void SetSingleBoneRotation(Bone bone, Triple rotation) {
        if (!bone.hasElement()) return;

        rotation.x = rotation.x % 360;
        rotation.y = rotation.y % 360;
        rotation.z = rotation.z % 360;

        Display itemDisplay = bone.getItemDisplay();

        Matrix4f currentMatrix = new Matrix4f();
        currentMatrix.rotateXYZ(new Vector3f((float) Math.toRadians(rotation.x), (float) Math.toRadians(rotation.y), (float) Math.toRadians(rotation.z)));
        itemDisplay.setTransformationMatrix(currentMatrix);
    }

    /**
     *
     * Sets the rotation of a single bone around some relative point.
     *
     * @param bone The bone to rotate
     * @param relPoint The relative point to rotate around
     * @param rotation The rotation as a triple represented as an euler angles in degrees.
     */
    public static void SetBoneRotationAroundRelative(Bone bone, Triple relPoint, Triple rotation) {
        if (!bone.hasElement()) return;

        rotation.x = rotation.x % 360;
        rotation.y = rotation.y % 360;
        rotation.z = rotation.z % 360;

        Display itemDisplay = bone.getItemDisplay();

        Matrix4f currentMatrix = new Matrix4f();
        Vector3f translation = new Vector3f((float) relPoint.x, (float) relPoint.y, (float) relPoint.z);
        currentMatrix.translate(translation);
        currentMatrix.rotateXYZ(new Vector3f((float) Math.toRadians(rotation.x), (float) Math.toRadians(rotation.y), (float) Math.toRadians(rotation.z)));
        currentMatrix.translate(new Vector3f(-translation.x(), -translation.y(), -translation.z()));
        itemDisplay.setTransformationMatrix(currentMatrix);
    }



    /**
     *
     * Adds some rotation to a single bone around some external pivot point.
     *
     * @param bone The bone to rotate
     * @param pivotPoint The pivotPoint to rotate around (use AddBoneRotationAroundRelative to use a relative Triple)
     * @param rotation The rotation as a triple representing euler angles in degrees.
     */
    public static void AddBoneRotationAround(Bone bone, Location pivotPoint, Triple rotation) {
        if (!bone.hasElement()) return;
        Triple baseRotation = bone.getBaseRotation();
        Triple oldRotation = bone.getCurrentRotation();
        Triple newRotation = new Triple(oldRotation.x + rotation.x + baseRotation.x, oldRotation.y + rotation.y + baseRotation.y, oldRotation.z + rotation.z  + baseRotation.z);
        SetBoneRotationAround(bone, pivotPoint, newRotation);
    }

    /**
     *
     * Adds some rotation to a single bone around some external pivot point.
     *
     * @param bone The bone to rotate
     * @param relPoint The relative point to rotate around
     * @param rotation The rotation as a triple representing euler angles in degrees.
     */
    public static void AddBoneRotationAroundRelative(Bone bone, Triple relPoint, Triple rotation) {
        if (!bone.hasElement()) return;
        Triple oldRotation = bone.getBaseRotation();
        Triple newRotation = new Triple(oldRotation.x + rotation.x, oldRotation.y + rotation.y, oldRotation.z + rotation.z);
        SetBoneRotationAroundRelative(bone, relPoint, newRotation);
    }


    /**
     *
     * Sets a bones rotation while also changing the rotation of all children bones it has to match. (this is what happens when rotating bones BlockBench animations)
     *
     * @param rootBone The bone you want to rotate.
     * @param rotation The rotation as an euler angle in degrees (xyz)
     */
    public static void SetBoneRotationWithChildren(Bone rootBone, Triple rotation) {
        Triple rootOrigin = rootBone.getRelOrigin().clone();
        rootOrigin.invert();
        rootOrigin.setY(-rootOrigin.y);
        rootOrigin.divide(16);
        for (Bone bone : rootBone.getAllChildrenBones(true)) {
            if (!bone.hasElement()) {continue;}
            Display itemDisplay = bone.getItemDisplay();
            Location currentLocation = itemDisplay.getLocation();
            Triple translation = new Triple(rootOrigin.x-currentLocation.x(),rootOrigin.y-currentLocation.y(),rootOrigin.z-currentLocation.z());
            SetBoneRotationAroundRelative(bone, translation, rotation);
        }
    }

    /**
     *
     * Adds a rotation to a bone while also changing the rotation of all children bones it has to match. (this is what happens when rotating bones BlockBench animations)
     *
     * @param rootBone The bone you want to rotate.
     * @param rotation The rotation as an euler angle in degrees (xyz)
     */
    public static void AddBoneRotationWithChildren(Bone rootBone, Triple rotation) {
        if (!rootBone.hasElement()) return;
        Triple oldRotation = rootBone.getBaseRotation();
        Triple newRotation = new Triple(oldRotation.x + rotation.x, oldRotation.y + rotation.y, oldRotation.z + rotation.z);
        SetBoneRotationWithChildren(rootBone, newRotation);
    }

    public static org.bukkit.util.EulerAngle angleToEulerAngle(int degrees){
        return angleToEulerAngle(Math.toRadians(degrees));
    }
    public static org.bukkit.util.EulerAngle angleToEulerAngle(double radians){
        double x = Math.cos(radians);
        double z = Math.sin(radians);
        return new org.bukkit.util.EulerAngle(x,0,z);
    }
    public static org.bukkit.util.Vector fromYawAndPitch(float yaw, float pitch) {
        double X = Math.sin(pitch) * Math.cos(yaw);
        double Y = Math.sin(pitch) * Math.sin(yaw);
        double Z = Math.cos(pitch);
        org.bukkit.util.Vector vector = new org.bukkit.util.Vector(Math.toDegrees(X), Math.toDegrees(Y), Math.toDegrees(Z));
        return vector;
    }
}
