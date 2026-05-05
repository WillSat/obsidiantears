package com.obsidiantears.neoforge.network;

import com.obsidiantears.neoforge.ObsidianTears;
import com.obsidiantears.neoforge.waypoint.WaypointManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RequestWaypointsPacket() implements CustomPacketPayload {
    public static final Type<RequestWaypointsPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ObsidianTears.MODID, "request_waypoints"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestWaypointsPacket> CODEC = StreamCodec.unit(new RequestWaypointsPacket());

    @Override
    public Type<RequestWaypointsPacket> type() {
        return TYPE;
    }

    public static void handle(RequestWaypointsPacket packet, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        WaypointManager manager = WaypointManager.get(player.level());
        PacketDistributor.sendToPlayer(player, new SyncWaypointsPacket(manager.getAllWaypoints()));
    }
}
