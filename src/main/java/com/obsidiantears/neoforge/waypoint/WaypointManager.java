package com.obsidiantears.neoforge.waypoint;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class WaypointManager extends SavedData {
    private static final Identifier ID = Identifier.fromNamespaceAndPath("obsidiantears", "waypoints");
    private static final Codec<WaypointManager> CODEC = CompoundTag.CODEC.xmap(WaypointManager::fromTag, WaypointManager::toTag);
    private static final SavedDataType<WaypointManager> TYPE = new SavedDataType<>(ID, WaypointManager::new, CODEC);

    private final Map<String, WaypointData> waypoints = new LinkedHashMap<>();
    private int nextSequence = 1;

    public static WaypointManager get(LevelAccessor level) {
        ServerLevel serverLevel = (ServerLevel) level;
        return serverLevel.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public WaypointData createWaypoint(String playerName, ServerLevel level, BlockPos pos) {
        String key = key(level.dimension().identifier(), pos);
        WaypointData existing = waypoints.get(key);
        if (existing != null) {
            return existing;
        }

        int sequence = nextSequence++;
        WaypointData waypoint = new WaypointData(playerName, level.dimension().identifier(), pos, sequence, "Unnamed Monument");
        waypoints.put(key, waypoint);
        setDirty();
        return waypoint;
    }

    public void removeWaypoint(ServerLevel level, BlockPos pos) {
        removeWaypoint(level.dimension().identifier(), pos);
    }

    public void removeWaypoint(Identifier dimension, BlockPos pos) {
        if (waypoints.remove(key(dimension, pos)) != null) {
            setDirty();
        }
    }

    public void updateWaypointName(Identifier dimension, BlockPos pos, String name) {
        WaypointData waypoint = getWaypoint(dimension, pos);
        if (waypoint != null) {
            waypoint.setDisplayName(name);
            setDirty();
        }
    }

    public boolean hasWaypoint(ServerLevel level, BlockPos pos) {
        return hasWaypoint(level.dimension().identifier(), pos);
    }

    public boolean hasWaypoint(Identifier dimension, BlockPos pos) {
        return waypoints.containsKey(key(dimension, pos));
    }

    public WaypointData getWaypoint(Identifier dimension, BlockPos pos) {
        return waypoints.get(key(dimension, pos));
    }

    public Collection<WaypointData> getAllWaypoints() {
        return new ArrayList<>(waypoints.values());
    }

    private CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (WaypointData waypoint : waypoints.values()) {
            list.add(waypoint.save());
        }
        tag.put("Waypoints", list);
        tag.putInt("NextSequence", nextSequence);
        return tag;
    }

    private static WaypointManager fromTag(CompoundTag tag) {
        WaypointManager manager = new WaypointManager();
        ListTag list = tag.getListOrEmpty("Waypoints");
        for (Tag entry : list) {
            entry.asCompound().map(WaypointData::load).ifPresent(waypoint -> manager.waypoints.put(key(waypoint.getDimension(), waypoint.getPos()), waypoint));
        }
        manager.nextSequence = Math.max(tag.getIntOr("NextSequence", manager.waypoints.size() + 1), manager.waypoints.size() + 1);
        return manager;
    }

    private static String key(Identifier dimension, BlockPos pos) {
        return dimension + "|" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
