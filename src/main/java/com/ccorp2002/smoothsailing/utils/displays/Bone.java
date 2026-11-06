package com.ccorp2002.smoothsailing.utils.displays;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.block.data.*;
import org.bukkit.util.Transformation;
import com.ccorp2002.smoothsailing.utils.movement.Teleport;
import com.ccorp2002.smoothsailing.utils.rotation.Rotate;
import com.ccorp2002.smoothsailing.utils.objects.Triple;
import org.joml.Vector3f;

import java.util.*;


/**
 *
 * A single bone of a model, these are the smallest parts that models are split into each bone is equal to either one or zero item displays.
 * Each bone has a parent bone (except the root bone), as well as children bones a bone is not guaranteed to have children bones.
 *
 */
public class Bone {

    public Triple relPivot;
    /**
     * This is the base rotation of this bone found from the .bbmodel file. This usually isn't what you want to change (look at currentRotation)
     */
    public Triple baseRotation;
    /**
     * This is the currentRotation of this bone as an euler angle in degrees, note that changing this won't actually change the rotation it will simply update the value.
     */
    public Triple currentRotation;

    public String UUID = "";
    public Display itemDisplay;
    public List<Bone> childrenBones;
    public Bone parentBone;
    public String modelName;
    public boolean hasElement = false;

    /**
     *
     * When this constructor is used we assume the bone doesn't have an element.
     *
     * @param relPivot The origin represented by 3 doubles in the form of a triple.
     * @param parentBone Parent of this bone
     * @param childrenBones All children direct child bones from this bone
     * @param UUID UUID of this bone
     */
    public Bone(Triple relPivot, Triple baseRotation, Bone parentBone, List<Bone> childrenBones, String UUID) {
        this.relPivot = relPivot;
        this.UUID = UUID;
        this.childrenBones = childrenBones;
        this.parentBone = parentBone;
        this.hasElement = false;
        this.baseRotation = baseRotation;
        this.currentRotation = baseRotation;
    }

    /**
     *
     * When this constructor is used we assume the bone doesn't have an element.
     *
     * @param relPivot The origin represented by 3 doubles in the form of a triple.
     * @param parentBone Parent of this bone
     * @param childrenBones All children direct child bones from this bone
     * @param UUID UUID of this bone
     */
    public Bone(Triple relPivot, Triple baseRotation, Display entity, List<Bone> childrenBones) {
        this.relPivot = relPivot;
        this.UUID = entity.getUniqueId().toString();
        this.childrenBones = childrenBones;
        this.parentBone = parentBone;
        this.hasElement = true;
        this.itemDisplay = entity;
        this.baseRotation = baseRotation;
        this.currentRotation = baseRotation;
    }
    /**
     *
     * When this constructor is used we assume the bone have an element.
     *
     * @param relPivot The origin represented by 3 doubles in the form of a triple.
     * @param parentBone Parent of this bone
     * @param childrenBones All children direct child bones from this bone
     * @param UUID UUID of this bone
     */
    public Bone(Triple relPivot, Triple baseRotation, Bone parentBone, List<Bone> childrenBones, String UUID, String modelName) {
        this.relPivot = relPivot;
        this.UUID = UUID;
        this.childrenBones = childrenBones;
        this.parentBone = parentBone;
        this.hasElement = true;
        this.modelName = modelName;
        this.baseRotation = baseRotation;
        this.currentRotation = baseRotation;
    }

    public static Bone from(Display root) {
        final Triple zerod = Triple.of(0d);
        final List<Bone> childrenBones = new ArrayList<>();
        for (Entity ent : root.getPassengers()) {
            if (ent instanceof Display d) {
                if (d.equals(root)) continue;
                childrenBones.add(Bone.from(d));
            }
        }
        Bone bone = new Bone(zerod, zerod, null, childrenBones, root.getUniqueId().toString());
        bone.hasElement = true;
        bone.setItemDisplay(root);
        return bone;
    }

    public static Bone from(Display root, Collection<Entity> others) {
        final Triple zerod = Triple.of(0d);
        final List<Bone> childrenBones = new ArrayList<>();
        for (Entity ent : others) {
            if (ent instanceof Display d) {
                if (d.equals(root)) continue;
                childrenBones.add(Bone.from(d));
            }
        }
        Bone bone = new Bone(zerod, zerod, null, childrenBones, root.getUniqueId().toString());
        bone.hasElement = true;
        bone.setItemDisplay(root);
        return bone;
    }


    /**
     *
     * Reset the bones location relative to the defined ItemDisplay
     *
     * @param display the ItemDisplay to reset relative to
     */
    public void resetLocation(ItemDisplayGroup display){
        Location location = display.getPivotPoint();
        location.set(
                location.getX() - (relPivot.x / 16),
                location.getY() + (relPivot.y / 16),
                location.getZ() - (relPivot.z / 16)
        );

        Teleport.teleportSingleBone(this, location);
        Rotate.SetSingleBoneRotation(this, baseRotation);
    }

    public void resetRotation(){
       Rotate.SetBoneRotationWithChildren(this,  baseRotation);
    }

    /**
     * Resets the bones location relative to the defined ItemDisplay with children
     *
     * @param display the ItemDisplay to reset relative to
     * @param withChildren Should the children bones also have their rotation reset
     */
    public void resetLocation(ItemDisplayGroup display, boolean withChildren){
        resetLocation(display);
        if(withChildren){
            Location location = display.getPivotPoint();
            location.set(
                    location.getX() - (relPivot.x / 16),
                    location.getY() + (relPivot.y / 16),
                    location.getZ() - (relPivot.z / 16)
            );

            Teleport.teleportSingleBone(this, location);
            Rotate.SetSingleBoneRotation(this, baseRotation);

            for(Bone bone : this.childrenBones){
                bone.resetLocation(display, true);
            }
        }
    }

    public Triple getRelOrigin() {
        return relPivot.clone();
    }

    public void setRelOrigin(Triple relOrigin) {
        this.relPivot = relOrigin;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public void setItemDisplay(Display display) {
        this.itemDisplay = display;
    }

    public Display getItemDisplay() {return this.itemDisplay;}

    public List<Bone> getChildrenBones() {
        return childrenBones;
    }

    public int size() {
        return getAllChildrenBones(true).size();
    }


    /**
     *
     * Gets all the children bones of this bone and optionally a reference to itself.
     *
     * @param includeSelf A boolean where true means that the bone this method is called on will be included in the returned list, false means it isn't.
     * @return The list of bones either with or without this bone itself depending on the includeSelf parameter, or an empty list if there's no results.
     */
    public List<Bone> getAllChildrenBones(Boolean includeSelf) {
        List<Bone> childrenBones = new ArrayList<>();

        if(includeSelf) childrenBones.add(this);

        for(Bone child : this.childrenBones){
            childrenBones.addAll(child.getAllChildrenBones(includeSelf));
        }

        return childrenBones;
    }

    public boolean hasElement() {return this.hasElement;}

    public boolean hasChildren() {return !this.childrenBones.isEmpty();}

    public Triple getBaseRotation() {
        return baseRotation;
    }

    public void setBaseRotation(Triple baseRotation) {
        this.baseRotation = baseRotation;
    }

    public Triple getCurrentRotation() {
        return currentRotation;
    }

    public void setChildrenBones(List<Bone> childrenBones) {
        this.childrenBones = childrenBones;
    }

    public void addChildrenBone(Bone childrenBone) {
        this.childrenBones.add(childrenBone);
    }

    public void setParentBone(Bone parentBone) {
        this.parentBone = parentBone;
    }

    public Bone getParentBone() {
        return parentBone;
    }
}
