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

import java.util.*;
import java.util.stream.Collectors;

public class WaypointManager extends SavedData {
    private static final int MAX_PER_DIMENSION = 1024;
    private static final Identifier ID = Identifier.fromNamespaceAndPath("obsidiantears", "waypoints");
    private static final Codec<WaypointManager> CODEC = CompoundTag.CODEC.xmap(WaypointManager::fromTag, WaypointManager::toTag);
    private static final SavedDataType<WaypointManager> TYPE = new SavedDataType<>(ID, WaypointManager::new, CODEC);

    private final Map<String, WaypointData> waypoints = new LinkedHashMap<>();

    public static WaypointManager get(LevelAccessor level) {
        ServerLevel serverLevel = (ServerLevel) level;
        return serverLevel.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public Set<Identifier> getActiveDimensions() {
        return waypoints.values().stream()
            .map(WaypointData::getDimension)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public WaypointData createWaypoint(String playerName, ServerLevel level, BlockPos pos) {
        String key = key(level.dimension().identifier(), pos);
        WaypointData existing = waypoints.get(key);
        if (existing != null) {
            return existing;
        }

        Identifier dim = level.dimension().identifier();
        if (countInDimension(dim) >= MAX_PER_DIMENSION) {
            return null;
        }
        int sequence = nextSequenceForDimension(dim);
        WaypointData waypoint = new WaypointData(playerName, dim, pos, sequence, "Unnamed Monument");
        waypoints.put(key, waypoint);
        setDirty();
        return waypoint;
    }

    public void removeWaypoint(ServerLevel level, BlockPos pos) {
        removeWaypoint(level.dimension().identifier(), pos);
    }

    public void removeWaypoint(Identifier dimension, BlockPos pos) {
        if (waypoints.remove(key(dimension, pos)) != null) {
            renumberDimension(dimension);
            setDirty();
        }
    }

    private long countInDimension(Identifier dimension) {
        return waypoints.values().stream()
            .filter(wp -> wp.getDimension().equals(dimension))
            .count();
    }

    private int nextSequenceForDimension(Identifier dimension) {
        return (int) countInDimension(dimension) + 1;
    }

    private void renumberDimension(Identifier dimension) {
        List<WaypointData> dimWaypoints = waypoints.values().stream()
            .filter(wp -> wp.getDimension().equals(dimension))
            .sorted(Comparator.comparingInt(WaypointData::getSequence))
            .toList();
        int seq = 1;
        for (WaypointData wp : dimWaypoints) {
            wp.setSequence(seq++);
        }
    }

    public void moveUp(Identifier dimension, BlockPos pos) {
        WaypointData target = getWaypoint(dimension, pos);
        if (target == null || target.getSequence() <= 1) return;

        int targetSeq = target.getSequence();
        WaypointData above = waypoints.values().stream()
            .filter(wp -> wp.getDimension().equals(dimension) && wp.getSequence() == targetSeq - 1)
            .findFirst()
            .orElse(null);
        if (above == null) return;

        target.setSequence(targetSeq - 1);
        above.setSequence(targetSeq);
        setDirty();
    }

    public void moveDown(Identifier dimension, BlockPos pos) {
        WaypointData target = getWaypoint(dimension, pos);
        if (target == null) return;

        int targetSeq = target.getSequence();
        WaypointData below = waypoints.values().stream()
            .filter(wp -> wp.getDimension().equals(dimension) && wp.getSequence() == targetSeq + 1)
            .findFirst()
            .orElse(null);
        if (below == null) return;

        target.setSequence(targetSeq + 1);
        below.setSequence(targetSeq);
        setDirty();
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
        return tag;
    }

    private static WaypointManager fromTag(CompoundTag tag) {
        WaypointManager manager = new WaypointManager();
        ListTag list = tag.getListOrEmpty("Waypoints");
        for (Tag entry : list) {
            entry.asCompound().map(WaypointData::load).ifPresent(waypoint -> manager.waypoints.put(key(waypoint.getDimension(), waypoint.getPos()), waypoint));
        }
        // Renumber all dimensions to ensure per-dimension sequence consistency (migrates old global sequences)
        for (Identifier dim : manager.getActiveDimensions()) {
            manager.renumberDimension(dim);
        }
        return manager;
    }

    private static String key(Identifier dimension, BlockPos pos) {
        return dimension + "|" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
