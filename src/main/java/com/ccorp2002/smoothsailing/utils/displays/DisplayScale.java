package com.ccorp2002.smoothsailing.utils.displays;

import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

public final class DisplayScale {

    public static void addSize(Display itemDisplay, float x, float y, float z){
        Transformation itemDisplayTransformation = itemDisplay.getTransformation();
        Vector3f itemDisplayTransformationScale = itemDisplayTransformation.getScale().add(x, y, z);
        Transformation newItemDisplayTransformation = new Transformation(itemDisplayTransformation.getTranslation(),
                itemDisplayTransformation.getLeftRotation(),
                itemDisplayTransformationScale,
                itemDisplayTransformation.getRightRotation());
        itemDisplay.setTransformation(newItemDisplayTransformation);
    }

    public static void setSize(Display itemDisplay, float x, float y, float z){
        Transformation itemDisplayTransformation = itemDisplay.getTransformation();
        Transformation newItemDisplayTransformation = new Transformation(itemDisplayTransformation.getTranslation(),
                itemDisplayTransformation.getLeftRotation(),
                new Vector3f(x, y, z),
                itemDisplayTransformation.getRightRotation());
        itemDisplay.setTransformation(newItemDisplayTransformation);
    }
}
