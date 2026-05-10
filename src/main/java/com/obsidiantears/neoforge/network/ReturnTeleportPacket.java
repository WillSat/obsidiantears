package com.obsidiantears.neoforge.network;

import com.obsidiantears.neoforge.ObsidianTears;
import com.obsidiantears.neoforge.event.BlockEventHandler;
import com.obsidiantears.neoforge.waypoint.WaypointData;
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
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Set;

public record ReturnTeleportPacket(Identifier targetDimension, BlockPos targetPos) implements CustomPacketPayload {
    public static final Type<ReturnTeleportPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ObsidianTears.MODID, "return_teleport"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReturnTeleportPacket> CODEC = CustomPacketPayload.codec(ReturnTeleportPacket::write, ReturnTeleportPacket::read);

    private static void write(ReturnTeleportPacket packet, FriendlyByteBuf buf) {
        buf.writeIdentifier(packet.targetDimension);
        buf.writeBlockPos(packet.targetPos);
    }

    private static ReturnTeleportPacket read(FriendlyByteBuf buf) {
        return new ReturnTeleportPacket(buf.readIdentifier(), buf.readBlockPos());
    }

    @Override
    public Type<ReturnTeleportPacket> type() {
        return TYPE;
    }

    public static void handle(ReturnTeleportPacket packet, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();

        ServerLevel targetLevel = player.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, packet.targetDimension));
        if (targetLevel == null) {
            return;
        }

        BlockPos pos = packet.targetPos;
        double targetX = pos.getX() + 0.5;
        double targetY = pos.getY() + 1.0;
        double targetZ = pos.getZ() + 0.5;

        ServerLevel sourceLevel = (ServerLevel) player.level();
        double sourceX = player.getX();
        double sourceY = player.getY();
        double sourceZ = player.getZ();

        Entity rootVehicle = player.getRootVehicle();
        var leashedEntities = TeleportRequestPacket.collectLeashedEntities(player, rootVehicle);

        if (rootVehicle == player) {
            player.teleportTo(targetLevel, targetX, targetY, targetZ, Set.of(), player.getYRot(), player.getXRot(), true);
        } else {
            rootVehicle.teleportTo(targetLevel, targetX, targetY, targetZ, Set.of(), rootVehicle.getYRot(), rootVehicle.getXRot(), true);
        }

        TeleportRequestPacket.restoreLeashedEntities(player, targetLevel, targetX, targetY, targetZ, leashedEntities);

        TeleportRequestPacket.spawnTeleportParticles(sourceLevel, sourceX, sourceY, sourceZ);
        TeleportRequestPacket.spawnTeleportParticles(targetLevel, targetX, targetY, targetZ);

        String rawBiome = targetLevel.getBiome(pos).getRegisteredName();
        int colon = rawBiome.indexOf(':');
        String biomeKey = colon >= 0 ? rawBiome.substring(colon + 1) : rawBiome;

        BlockEventHandler.updateLastBiome(player, targetLevel.getBiome(pos));

        boolean isOverworld = targetLevel.dimension().equals(net.minecraft.world.level.Level.OVERWORLD);
        long dayTime = targetLevel.getGameTime() % 24000;
        int totalMinutes = (int) ((dayTime + 6000) % 24000 * 60 / 1000);
        int hours = (totalMinutes / 60) % 24;
        int minutes = totalMinutes % 60;
        String ampm = hours >= 12 ? "PM" : "AM";
        int displayHour = hours % 12;
        if (displayHour == 0) displayHour = 12;
        String timeText = String.format("%02d:%02d %s", displayHour, minutes, ampm);
        String weather;
        if (targetLevel.isThundering()) {
            weather = "thunder";
        } else if (targetLevel.isRaining()) {
            weather = "rain";
        } else {
            weather = "clear";
        }

        PacketDistributor.sendToPlayer(player, new TeleportFeedbackPacket(
            biomeKey, "", "", WaypointData.sequenceColor(packet.targetDimension, 1),
            timeText, weather, isOverworld));
    }
}
