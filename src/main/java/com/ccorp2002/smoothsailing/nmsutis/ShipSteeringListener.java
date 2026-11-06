package com.ccorp2002.smoothsailing.nmsutils;

import com.ccorp2002.smoothsailing.SmoothSailingPlugin;
import com.ccorp2002.smoothsailing.ShipListener;
import com.ccorp2002.smoothsailing.ship.*;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.*;

import java.util.*;

/**
 * Listens to player input packets to detect WASD controls for ship steering.
 * W/S control forward/backward speed, A/D control left/right rotation.
 */
public class ShipSteeringListener {
    private static JavaPlugin plugin;
    private final ProtocolManager protocolManager;
    public ShipSteeringListener(JavaPlugin plugin) {
        ShipSteeringListener.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        registerListener();
    }

    public void registerListener() {
        protocolManager.addPacketListener(
            new PacketAdapter(plugin, ListenerPriority.LOW,
                             com.comphenix.protocol.PacketType.Play.Client.STEER_VEHICLE) {
                @Override
                public void onPacketReceiving(com.comphenix.protocol.events.PacketEvent event) {
                    handleSteeringPacket(event);
                }
            }
        );
        plugin.getLogger().info("Ship steering listener registered (ProtocolLib WASD detection)");
    }

    public void handleSteeringPacket(com.comphenix.protocol.events.PacketEvent event) {
        Player player = event.getPlayer();
        PacketContainer packet = event.getPacket();

        try {
            // Find ship instance for this player (only returns if player is in driver seat)
            ShipInstance ship = ShipListener.findShipByPlayer(player);
            if (ship == null) {
                return;
            }

            // Read the Input object (field 0)
            StructureModifier<Object> modifier = packet.getModifier();
            if (modifier.size() < 1) {
                return;
            }

            Object inputObj = modifier.read(0);
            if (inputObj == null) {
                return;
            }

            // Use reflection to read the boolean fields from the Input record
            try {
                Class<?> inputClass = inputObj.getClass();
                boolean forward = (boolean) inputClass.getMethod("forward").invoke(inputObj);
                boolean backward = (boolean) inputClass.getMethod("backward").invoke(inputObj);
                boolean left = (boolean) inputClass.getMethod("left").invoke(inputObj);
                boolean right = (boolean) inputClass.getMethod("right").invoke(inputObj);
                boolean jump = (boolean) inputClass.getMethod("jump").invoke(inputObj);
                boolean sprint = (boolean) inputClass.getMethod("sprint").invoke(inputObj);

                // Update ship input state (physics will apply every tick)

                // Update vertical input for all ships (custom airships use base class method)
                if (SmoothSailingPlugin.STEERING_DEBUG) {
                    System.out.println("SHIP:"+ship);
                    System.out.println("UP:"+jump);
                    System.out.println("FORWARD:"+forward);
                    System.out.println("DOWN:"+sprint);
                    System.out.println("BACKWARD:"+backward);
                    System.out.println("LEFT:"+left);
                    System.out.println("RIGHT:"+right);
                    System.out.println("HAS VALID VEHICLE?:"+ship.vehicle.isValid());
                }
                if (forward || backward || jump || sprint || left || right) { //
                    ship.setInputState(forward, backward, left, right);
                    ship.setVerticalInputState(jump, sprint);
                    ship.hasDriver = true;
                    ship.driver = player;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to read Input fields: " + e.getMessage());
            }

        } catch (Exception ex) {
            plugin.getLogger().warning("Error handling steering packet: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}


