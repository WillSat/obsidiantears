package com.obsidiantears.neoforge.network;

import com.obsidiantears.neoforge.ObsidianTears;
import com.obsidiantears.neoforge.waypoint.WaypointManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WaypointNamingPacket(Identifier dimension, BlockPos pos, String name) implements CustomPacketPayload {
    public static final Type<WaypointNamingPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ObsidianTears.MODID, "waypoint_naming"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WaypointNamingPacket> CODEC = CustomPacketPayload.codec(WaypointNamingPacket::write, WaypointNamingPacket::read);

    private static void write(WaypointNamingPacket packet, FriendlyByteBuf buf) {
        buf.writeIdentifier(packet.dimension);
        buf.writeBlockPos(packet.pos);
        buf.writeUtf(packet.name, 50);
    }

    private static WaypointNamingPacket read(FriendlyByteBuf buf) {
        return new WaypointNamingPacket(buf.readIdentifier(), buf.readBlockPos(), buf.readUtf(50));
    }

    @Override
    public Type<WaypointNamingPacket> type() {
        return TYPE;
    }

    public static void handle(WaypointNamingPacket packet, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        String trimmed = packet.name.trim();
        if (trimmed.isEmpty()) {
            return;
        }

        WaypointManager manager = WaypointManager.get(player.level());
        manager.updateWaypointName(packet.dimension, packet.pos, trimmed);
        var waypoint = manager.getWaypoint(packet.dimension, packet.pos);
        if (waypoint == null) {
            return;
        }

        ServerLevel labelLevel = player.level().getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, packet.dimension));
        if (labelLevel == null) {
            return;
        }

        labelLevel.getEntitiesOfClass(ArmorStand.class, labelArea(packet.pos)).forEach(stand -> {
            if (stand.entityTags().contains(ObsidianTears.MODID + "_waypoint_label")) {
                stand.setCustomName(Component.literal(waypoint.getDisplayName() + " #" + waypoint.getSequence()));
            }
        });
    }

    private static AABB labelArea(BlockPos pos) {
        return new AABB(pos.getX() - 0.5, pos.getY(), pos.getZ() - 0.5, pos.getX() + 1.5, pos.getY() + 3.0, pos.getZ() + 1.5);
    }
}
