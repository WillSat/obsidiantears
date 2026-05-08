package com.obsidiantears.neoforge.network;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class PacketHelper {
    private PacketHelper() {
    }

    public static void requestWaypoints() {
        ClientPacketDistributor.sendToServer(new RequestWaypointsPacket());
    }

    public static void requestTeleport(Identifier dimension, BlockPos pos) {
        ClientPacketDistributor.sendToServer(new TeleportRequestPacket(dimension, pos));
    }

    public static void nameWaypoint(Identifier dimension, BlockPos pos, String name) {
        ClientPacketDistributor.sendToServer(new WaypointNamingPacket(dimension, pos, name));
    }

    public static void moveWaypointUp(Identifier dimension, BlockPos pos) {
        ClientPacketDistributor.sendToServer(new MoveWaypointPacket(dimension, pos, true));
    }

    public static void moveWaypointDown(Identifier dimension, BlockPos pos) {
        ClientPacketDistributor.sendToServer(new MoveWaypointPacket(dimension, pos, false));
    }
}
