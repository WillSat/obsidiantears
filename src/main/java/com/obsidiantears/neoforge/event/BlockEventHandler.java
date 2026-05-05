package com.obsidiantears.neoforge.event;

import com.obsidiantears.neoforge.ObsidianTears;
import com.obsidiantears.neoforge.network.NamingPacket;
import com.obsidiantears.neoforge.waypoint.WaypointData;
import com.obsidiantears.neoforge.waypoint.WaypointManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
            serverPlayer.sendSystemMessage(Component.literal("传送碑已创建，请为它命名。"));
        }
    }

    public static void onBlockBreak(BlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.isClientSide()) {
            return;
        }

        BlockPos pos = event.getPos();
        WaypointManager manager = WaypointManager.get(level);
        if (event.getState().getBlock() == Blocks.REDSTONE_BLOCK && manager.hasWaypoint(level, pos)) {
            manager.removeWaypoint(level, pos);
            removeWaypointLabel(level, pos);
            return;
        }

        BlockPos above = pos.above();
        if (event.getState().getBlock() == Blocks.CRYING_OBSIDIAN && manager.hasWaypoint(level, above)) {
            manager.removeWaypoint(level, above);
            removeWaypointLabel(level, above);
        }
    }

    private static void createWaypointLabel(Level level, BlockPos pos, WaypointData waypoint) {
        removeWaypointLabel(level, pos);

        ArmorStand label = new ArmorStand(level, pos.getX() + 0.5, pos.getY() + 1.25, pos.getZ() + 0.5);
        label.setCustomName(Component.literal(waypoint.getDisplayName() + " #" + waypoint.getSequence()));
        label.setCustomNameVisible(true);
        label.setInvulnerable(true);
        label.setNoGravity(true);
        label.setNoBasePlate(true);
        label.setInvisible(true);
        label.addTag(LABEL_TAG);
        level.addFreshEntity(label);
    }

    private static void removeWaypointLabel(Level level, BlockPos pos) {
        for (ArmorStand stand : level.getEntitiesOfClass(ArmorStand.class, labelArea(pos))) {
            if (stand.entityTags().contains(LABEL_TAG)) {
                stand.remove(Entity.RemovalReason.DISCARDED);
            }
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
