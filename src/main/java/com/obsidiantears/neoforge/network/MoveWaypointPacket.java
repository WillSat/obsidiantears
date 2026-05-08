package com.obsidiantears.neoforge.network;

import com.obsidiantears.neoforge.ObsidianTears;
import com.obsidiantears.neoforge.event.BlockEventHandler;
import com.obsidiantears.neoforge.waypoint.WaypointManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MoveWaypointPacket(Identifier dimension, BlockPos pos, boolean up) implements CustomPacketPayload {
    public static final Type<MoveWaypointPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ObsidianTears.MODID, "move_waypoint"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MoveWaypointPacket> CODEC = CustomPacketPayload.codec(MoveWaypointPacket::write, MoveWaypointPacket::read);

    private static void write(MoveWaypointPacket packet, FriendlyByteBuf buf) {
        buf.writeIdentifier(packet.dimension);
        buf.writeBlockPos(packet.pos);
        buf.writeBoolean(packet.up);
    }

    private static MoveWaypointPacket read(FriendlyByteBuf buf) {
        return new MoveWaypointPacket(buf.readIdentifier(), buf.readBlockPos(), buf.readBoolean());
    }

    @Override
    public Type<MoveWaypointPacket> type() {
        return TYPE;
    }

    public static void handle(MoveWaypointPacket packet, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        WaypointManager manager = WaypointManager.get(player.level());
        if (packet.up) {
            manager.moveUp(packet.dimension, packet.pos);
        } else {
            manager.moveDown(packet.dimension, packet.pos);
        }

        ServerLevel targetLevel = player.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, packet.dimension));
        if (targetLevel != null) {
            BlockEventHandler.refreshDimensionLabels(targetLevel, packet.dimension, manager);
        }

        PacketDistributor.sendToPlayer(player, new SyncWaypointsPacket(manager.getAllWaypoints()));
    }
}
