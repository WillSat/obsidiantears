package com.obsidiantears.neoforge.event;

import com.obsidiantears.neoforge.ObsidianTears;
import com.obsidiantears.neoforge.network.NamingPacket;
import com.obsidiantears.neoforge.waypoint.WaypointData;
import com.obsidiantears.neoforge.waypoint.WaypointManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.Consumer;

public class BlockEventHandler {
    private static final String LABEL_TAG = ObsidianTears.MODID + "_waypoint_label";

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player) || event.getPlacedBlock().getBlock() != Blocks.REDSTONE_BLOCK) {
            return;
        }

        BlockPos redstonePos = event.getPos();
        if (level.getBlockState(redstonePos.below()).getBlock() != Blocks.CRYING_OBSIDIAN) {
            return;
        }

        WaypointData waypoint = WaypointManager.get(level).createWaypoint(player.getName().getString(), level, redstonePos);
        createWaypointLabel(level, redstonePos, waypoint);

        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new NamingPacket(waypoint.getDimension(), redstonePos, waypoint.getSequence()));
        }
    }

    public static void onBlockBreak(BlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.isClientSide()) {
            return;
        }

        BlockPos pos = event.getPos();
        WaypointManager manager = WaypointManager.get(level);
        Identifier dim = level.dimension().identifier();

        if (event.getState().getBlock() == Blocks.REDSTONE_BLOCK && manager.hasWaypoint(level, pos)) {
            manager.removeWaypoint(level, pos);
            removeWaypointLabel(level, pos);
            refreshDimensionLabels(level, dim, manager);
            return;
        }

        BlockPos above = pos.above();
        if (event.getState().getBlock() == Blocks.CRYING_OBSIDIAN && manager.hasWaypoint(level, above)) {
            manager.removeWaypoint(level, above);
            removeWaypointLabel(level, above);
            refreshDimensionLabels(level, dim, manager);
        }
    }

    public static void refreshDimensionLabels(ServerLevel level, Identifier dimension, WaypointManager manager) {
        for (WaypointData wp : manager.getAllWaypoints()) {
            if (!wp.getDimension().equals(dimension)) continue;
            removeWaypointLabel(level, wp.getPos());
            createWaypointLabel(level, wp.getPos(), wp);
        }
    }

    private static void createWaypointLabel(Level level, BlockPos pos, WaypointData waypoint) {
        removeWaypointLabel(level, pos);

        ArmorStand label = new ArmorStand(level, pos.getX() + 0.5, pos.getY() + 1.25, pos.getZ() + 0.5);
        label.setCustomName(WaypointData.buildLabelComponent(waypoint));
        label.setCustomNameVisible(true);
        label.setInvulnerable(true);
        label.setNoGravity(true);
        label.setNoBasePlate(true);
        label.setInvisible(true);
        label.setSilent(true);
        label.addTag(LABEL_TAG);
        level.addFreshEntity(label);

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                15, 0.3, 0.3, 0.3, 0.03);
        }
    }

    private static void removeWaypointLabel(Level level, BlockPos pos) {
        for (ArmorStand stand : level.getEntitiesOfClass(ArmorStand.class, labelArea(pos))) {
            if (stand.entityTags().contains(LABEL_TAG)) {
                stand.remove(Entity.RemovalReason.DISCARDED);
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 30 != 0) return;

        WaypointManager manager = WaypointManager.get(server.overworld());
        for (WaypointData waypoint : manager.getAllWaypoints()) {
            ServerLevel targetLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, waypoint.getDimension()));
            if (targetLevel == null || !targetLevel.isLoaded(waypoint.getPos())) continue;

            BlockPos pos = waypoint.getPos();
            targetLevel.sendParticles(ParticleTypes.PORTAL,
                pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                2, 0.15, 0.15, 0.15, 0.02);
        }
    }

    private static AABB labelArea(BlockPos pos) {
        return new AABB(pos.getX() - 0.5, pos.getY(), pos.getZ() - 0.5, pos.getX() + 1.5, pos.getY() + 3.0, pos.getZ() + 1.5);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerBreakListener(net.neoforged.bus.api.IEventBus eventBus) {
        Class<? extends BlockEvent> eventType = findBreakEventType();
        if (eventType == null) {
            ObsidianTears.LOGGER.warn("No compatible block break event found; waypoint removal on block break is disabled.");
            return;
        }
        eventBus.addListener((Class) eventType, (Consumer<BlockEvent>) BlockEventHandler::onBlockBreak);
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends BlockEvent> findBreakEventType() {
        String[] candidates = {
            "net.neoforged.neoforge.event.level.block.BreakBlockEvent",
            "net.neoforged.neoforge.event.level.BlockEvent$BreakEvent"
        };

        for (String candidate : candidates) {
            try {
                Class<?> eventClass = Class.forName(candidate);
                if (BlockEvent.class.isAssignableFrom(eventClass)) {
                    return (Class<? extends BlockEvent>) eventClass;
                }
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }
}
