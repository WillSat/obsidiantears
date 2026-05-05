package com.obsidiantears.neoforge.network;

import com.obsidiantears.neoforge.ObsidianTears;
import com.obsidiantears.neoforge.screen.WaypointMenuScreen;
import com.obsidiantears.neoforge.waypoint.WaypointData;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record SyncWaypointsPacket(List<WaypointData> waypoints) implements CustomPacketPayload {
    public static final Type<SyncWaypointsPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ObsidianTears.MODID, "sync_waypoints"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncWaypointsPacket> CODEC = CustomPacketPayload.codec(SyncWaypointsPacket::write, SyncWaypointsPacket::read);

    public SyncWaypointsPacket(Collection<WaypointData> waypoints) {
        this(new ArrayList<>(waypoints));
    }

    private static void write(SyncWaypointsPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.waypoints.size());
        for (WaypointData waypoint : packet.waypoints) {
            buf.writeNbt(waypoint.save());
        }
    }

    private static SyncWaypointsPacket read(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<WaypointData> waypoints = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            CompoundTag tag = buf.readNbt();
            if (tag != null) {
                waypoints.add(WaypointData.load(tag));
            }
        }
        return new SyncWaypointsPacket(waypoints);
    }

    @Override
    public Type<SyncWaypointsPacket> type() {
        return TYPE;
    }

    public static void handle(SyncWaypointsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof WaypointMenuScreen screen) {
                screen.setWaypoints(packet.waypoints);
            }
        });
    }
}
