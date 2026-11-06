package com.ccorp2002.smoothsailing.utils.rotation;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.scheduler.BukkitRunnable;
import com.ccorp2002.smoothsailing.SmoothSailingPlugin;
import com.ccorp2002.smoothsailing.utils.Bone;
import com.ccorp2002.smoothsailing.utils.ItemDisplayGroup;
import com.ccorp2002.smoothsailing.utils.Triple;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class RotateSmooth {

    /**
     *
     * Smoothly sets the rotation of an ItemDisplayGroup using 4x4 matrices.
     *
     * @param itemDisplayGroup The ItemDisplayGroup to rotate
     * @param rotation The rotation as an euler angle in degrees
     * @param interpolationDuration The time it takes the itemDisplayGroup to get from its start rotation to the given rotation
     */
    public static void SetRotationItemDisplayGroupSmooth(ItemDisplayGroup itemDisplayGroup, Triple rotation, int interpolationDuration) {
        for (Bone bone : itemDisplayGroup.getRootBone().getAllChildrenBones(true)){
            if(!bone.hasElement()) continue;
            bone.getItemDisplay().setInterpolationDuration(interpolationDuration);
            bone.getItemDisplay().setInterpolationDelay(0);
        }

        Rotate.SetRotationItemDisplayGroup(itemDisplayGroup, rotation);
    }

    /**
     *
     * Smoothly adds some rotation to an ItemDisplayGroup using 4x4 matrices.
     *
     * @param itemDisplayGroup The ItemDisplayGroup to rotate
     * @param rotation The rotation to add as an euler angle in degrees
     * @param interpolationDuration The time it takes the itemDisplayGroup to get from its start rotation to the end rotation
     */
    public static void AddRotationItemDisplayGroupSmooth(ItemDisplayGroup itemDisplayGroup, Triple rotation, int interpolationDuration) {
        for (Bone bone : itemDisplayGroup.getRootBone().getAllChildrenBones(true)){
            if(!bone.hasElement()) continue;
            bone.getItemDisplay().setInterpolationDelay(0);
            bone.getItemDisplay().setInterpolationDuration(interpolationDuration);
        }

        Rotate.AddRotationItemDisplayGroup(itemDisplayGroup, rotation);
    }

    /**
     *
     * Smoothly sets the rotation of a singular bone around an external point.
     *
     * @param bone The bone to rotate
     * @param pivotPoint The external point to rotate around (use SetBoneRotationAroundRelativeSmooth to use relative coordinates)
     * @param rotation The rotation as an euler angle in degrees (XYZ)
     * @param interpolationDuration The time it takes the itemDisplayGroup to get from its start rotation to the end rotation
     */
    public static void SetBoneRotationAroundSmooth(Bone bone, Location pivotPoint, Triple rotation,  int interpolationDuration) {
        if (!bone.hasElement()) return;

        rotation.x = rotation.x % 360;
        rotation.y = rotation.y % 360;
        rotation.z = rotation.z % 360;

        Display itemDisplay = bone.getItemDisplay();

        itemDisplay.setInterpolationDelay(0);
        itemDisplay.setInterpolationDuration(interpolationDuration);

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
     * Smoothly sets the rotation of a singular bone around an relative point.
     *
     * @param bone The bone to rotate
     * @param relPoint The relative point to rotate around
     * @param rotation The rotation as an euler angle in degrees (XYZ)
     * @param interpolationDuration The time it takes the itemDisplayGroup to get from its start rotation to the end rotation
     */
    public static void SetBoneRotationAroundRelativeSmooth(Bone bone, Triple relPoint, Triple rotation,  int interpolationDuration) {
        if (!bone.hasElement()) return;

        rotation = rotation.clone().modulo(360);

        Display itemDisplay = bone.getItemDisplay();

        itemDisplay.setInterpolationDelay(0);
        itemDisplay.setInterpolationDuration(interpolationDuration);
        SmoothSailingPlugin.INSTANCE.getLogger().info("InterpolationDuration: " + interpolationDuration);

        Triple finalRotation = rotation;
        new BukkitRunnable() {
            @Override
            public void run() {
                Vector3f translation = relPoint.toVector3f();
                Vector3f negTranslation = relPoint.clone().invert().toVector3f();
                Matrix4f currentMatrix = new Matrix4f();
                currentMatrix.translate(negTranslation);
                currentMatrix.rotateXYZ(finalRotation.clone().toRadians().toVector3f());
                currentMatrix.translate(translation);
                itemDisplay.setTransformationMatrix(currentMatrix);

            }
        }.runTaskLater(SmoothSailingPlugin.INSTANCE, 1);
    }


    /**
     *
     * Smoothly adds some rotation around an external point to a singular bone.
     *
     * @param bone The bone to rotate
     * @param pivotPoint The external point to rotate around (use AddBoneRotationAroundRelativeSmooth to use relative coordinates)
     * @param rotation The rotation as an euler angle in degrees (XYZ)
     * @param interpolationDuration The time it takes the itemDisplayGroup to get from its start rotation to the end rotation
     */
    public static void AddBoneRotationAroundSmooth(Bone bone, Location pivotPoint, Triple rotation, int interpolationDuration) {
        if (!bone.hasElement()) return;
        Triple oldRotation = bone.getBaseRotation();
        Triple newRotation = new Triple(oldRotation.x + rotation.x, oldRotation.y + rotation.y, oldRotation.z + rotation.z);
        SetBoneRotationAroundSmooth(bone, pivotPoint, newRotation, interpolationDuration);
    }

    /**
     *
     * Smoothly adds some rotation around an relative point to a singular bone.
     *
     * @param bone The bone to rotate
     * @param relPoint The relative point to rotate around
     * @param rotation The rotation as an euler angle in degrees (XYZ)
     * @param interpolationDuration The time it takes the itemDisplayGroup to get from its start rotation to the end rotation
     */
    public static void AddBoneRotationAroundRelativeSmooth(Bone bone, Triple relPoint, Triple rotation, int interpolationDuration) {
        if (!bone.hasElement()) return;
        Triple oldRotation = bone.getBaseRotation();
        Triple newRotation = new Triple(oldRotation.x + rotation.x, oldRotation.y + rotation.y, oldRotation.z + rotation.z);
        SetBoneRotationAroundRelativeSmooth(bone, relPoint, newRotation, interpolationDuration);
    }

    /**
     *
     * Smoothly sets a bones rotation while also changing the rotation of all children bones it has to match. (this is what happens when rotating bones BlockBench animations)
     *
     * @param rootBone The bone you want to rotate.
     * @param rotation The rotation as an euler angle in degrees (xyz)
     * @param interpolationDuration The time it takes the itemDisplayGroup to get from its start rotation to the end rotation
     */
    public static void SetBoneRotationWithChildrenSmooth(Bone rootBone, Triple rotation, int interpolationDuration) {

        Triple rootOrigin = rootBone.getRelOrigin().clone();
        rootOrigin.invert();
        rootOrigin.setY(-rootOrigin.y);
        rootOrigin.divide(16);

        SmoothSailingPlugin.INSTANCE.getLogger().info("Rotation: " + rotation.toString());

        for (Bone bone : rootBone.getAllChildrenBones(true)) {
            if (!bone.hasElement()) {continue;}

            Triple currentRelOrigin = bone.getRelOrigin();
            currentRelOrigin.invert();
            currentRelOrigin.setY(-rootOrigin.y);
            currentRelOrigin.divide(16);

            Triple translation = Triple.difference(currentRelOrigin, rootOrigin);
            SetBoneRotationAroundRelativeSmooth(bone, translation, rotation, interpolationDuration);
        }
    }

    /**
     *
     * Smoothly adds a rotation to a bone while also changing the rotation of all children bones it has to match. (this is what happens when rotating bones BlockBench animations)
     *
     * @param rootBone The bone you want to rotate.
     * @param rotation The rotation as an euler angle in degrees (xyz)
     * @param interpolationDuration The time it takes the itemDisplayGroup to get from its start rotation to the end rotation
     */
    public static void AddBoneRotationWithChildrenSmooth(Bone rootBone, Triple rotation, int interpolationDuration) {
        if (!rootBone.hasElement()) return;
        Triple oldRotation = rootBone.getBaseRotation();
        Triple newRotation = new Triple(oldRotation.x + rotation.x, oldRotation.y + rotation.y, oldRotation.z + rotation.z);
        SetBoneRotationWithChildrenSmooth(rootBone, newRotation, interpolationDuration);
    }

    /**
     *
     * Smoothly sets the rotation of a single bone.
     *
     * @param bone The bone to rotate
     * @param rotation The rotation as a triple representing euler angles in degrees.
     * @param interpolationDuration The time it should take the bone to get from its start rotation to the end rotation
     */
    public static void SetSingleBoneRotationSmooth(Bone bone, Triple rotation, int interpolationDuration){
        if (!bone.hasElement()) return;

        bone.getItemDisplay().setInterpolationDelay(0);
        bone.getItemDisplay().setTeleportDuration(interpolationDuration);
        Rotate.SetSingleBoneRotation(bone, rotation);
    }
}
