package com.obsidiantears.neoforge.network;

import com.obsidiantears.neoforge.ObsidianTears;
import com.obsidiantears.neoforge.event.BlockEventHandler;
import com.obsidiantears.neoforge.waypoint.WaypointData;
import com.obsidiantears.neoforge.waypoint.WaypointManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record TeleportRequestPacket(Identifier targetDimension, BlockPos targetPos) implements CustomPacketPayload {
    public static final Type<TeleportRequestPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ObsidianTears.MODID, "teleport_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TeleportRequestPacket> CODEC = CustomPacketPayload.codec(TeleportRequestPacket::write, TeleportRequestPacket::read);

    private static void write(TeleportRequestPacket packet, FriendlyByteBuf buf) {
        buf.writeIdentifier(packet.targetDimension);
        buf.writeBlockPos(packet.targetPos);
    }

    private static TeleportRequestPacket read(FriendlyByteBuf buf) {
        return new TeleportRequestPacket(buf.readIdentifier(), buf.readBlockPos());
    }

    @Override
    public Type<TeleportRequestPacket> type() {
        return TYPE;
    }

    public static void handle(TeleportRequestPacket packet, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        WaypointData waypoint = WaypointManager.get(player.level()).getWaypoint(packet.targetDimension, packet.targetPos);
        if (waypoint == null) {
            return;
        }

        ServerLevel sourceLevel = (ServerLevel) player.level();
        ServerLevel targetLevel = player.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, waypoint.getDimension()));
        if (targetLevel == null) {
            return;
        }

        BlockPos pos = waypoint.getPos();
        double targetX = pos.getX() + 0.5;
        double targetY = pos.getY() + 1.0;
        double targetZ = pos.getZ() + 0.5;

        double sourceX = player.getX();
        double sourceY = player.getY();
        double sourceZ = player.getZ();

        Entity rootVehicle = player.getRootVehicle();
        List<LeashedEntity> leashedEntities = collectLeashedEntities(player, rootVehicle);

        if (rootVehicle == player) {
            player.teleportTo(targetLevel, targetX, targetY, targetZ, Set.of(), player.getYRot(), player.getXRot(), true);
        } else {
            rootVehicle.teleportTo(targetLevel, targetX, targetY, targetZ, Set.of(), rootVehicle.getYRot(), rootVehicle.getXRot(), true);
        }

        restoreLeashedEntities(player, targetLevel, targetX, targetY, targetZ, leashedEntities);

        spawnTeleportParticles(sourceLevel, sourceX, sourceY, sourceZ);
        spawnTeleportParticles(targetLevel, targetX, targetY, targetZ);

        // Gather biome, time, weather for the feedback overlay
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
            biomeKey, waypoint.getDisplayName(), waypoint.getQualifiedSequence(),
            WaypointData.sequenceColor(waypoint.getDimension(), waypoint.getSequence()),
            timeText, weather, isOverworld));
    }

    private static List<LeashedEntity> collectLeashedEntities(ServerPlayer player, Entity rootVehicle) {
        List<LeashedEntity> result = new ArrayList<>();
        for (Leashable leashable : Leashable.leashableLeashedTo(player)) {
            if (leashable instanceof Entity entity && entity != rootVehicle && !rootVehicle.hasIndirectPassenger(entity)) {
                result.add(new LeashedEntity(entity.getUUID(), leashOffset(player, entity), entity.getYRot(), entity.getXRot()));
            }
        }
        return result;
    }

    private static Vec3 leashOffset(ServerPlayer player, Entity entity) {
        Vec3 offset = entity.position().subtract(player.position());
        if (offset.lengthSqr() > 16.0) {
            return offset.normalize().scale(2.0);
        }
        return offset;
    }

    private static void restoreLeashedEntities(ServerPlayer player, ServerLevel targetLevel, double targetX, double targetY, double targetZ, List<LeashedEntity> leashedEntities) {
        for (LeashedEntity leashed : leashedEntities) {
            Entity entity = findEntity(player.level(), targetLevel, leashed.uuid());
            if (entity == null) {
                continue;
            }

            Vec3 offset = leashed.offset();
            boolean teleported = entity.teleportTo(targetLevel, targetX + offset.x(), targetY + offset.y(), targetZ + offset.z(), Set.of(), leashed.yRot(), leashed.xRot(), true);
            Entity movedEntity = teleported ? findEntity(targetLevel, targetLevel, leashed.uuid()) : entity;
            if (movedEntity instanceof Leashable movedLeashable) {
                movedLeashable.setLeashedTo(player, true);
            }
        }
    }

    private static Entity findEntity(ServerLevel sourceLevel, ServerLevel targetLevel, UUID uuid) {
        Entity entity = targetLevel.getEntity(uuid);
        return entity != null ? entity : sourceLevel.getEntity(uuid);
    }

    private static void spawnTeleportParticles(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.PORTAL, x, y, z, 40, 0.4, 1.2, 0.4, 0.05);
    }

    private record LeashedEntity(UUID uuid, Vec3 offset, float yRot, float xRot) {
    }
}
