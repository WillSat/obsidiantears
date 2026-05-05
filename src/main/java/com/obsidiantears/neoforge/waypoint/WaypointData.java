package com.obsidiantears.neoforge.waypoint;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

public class WaypointData {
    private final String playerName;
    private final Identifier dimension;
    private final BlockPos pos;
    private final int sequence;
    private String displayName;
    private final long createdTime;

    public WaypointData(String playerName, Identifier dimension, BlockPos pos, int sequence, String displayName) {
        this(playerName, dimension, pos, sequence, displayName, System.currentTimeMillis());
    }

    public WaypointData(String playerName, Identifier dimension, BlockPos pos, int sequence, String displayName, long createdTime) {
        this.playerName = playerName;
        this.dimension = dimension;
        this.pos = pos;
        this.sequence = sequence;
        this.displayName = displayName;
        this.createdTime = createdTime;
    }

    public String getPlayerName() {
        return playerName;
    }

    public Identifier getDimension() {
        return dimension;
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getSequence() {
        return sequence;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("PlayerName", playerName);
        tag.putString("Dimension", dimension.toString());
        tag.putInt("X", pos.getX());
        tag.putInt("Y", pos.getY());
        tag.putInt("Z", pos.getZ());
        tag.putInt("Sequence", sequence);
        tag.putString("DisplayName", displayName);
        tag.putLong("CreatedTime", createdTime);
        return tag;
    }

    public static WaypointData load(CompoundTag tag) {
        int sequence = tag.getIntOr("Sequence", 0);
        Identifier dimension = Identifier.tryParse(tag.getStringOr("Dimension", Level.OVERWORLD.identifier().toString()));
        if (dimension == null) {
            dimension = Level.OVERWORLD.identifier();
        }
        return new WaypointData(
            tag.getStringOr("PlayerName", ""),
            dimension,
            new BlockPos(tag.getIntOr("X", 0), tag.getIntOr("Y", 0), tag.getIntOr("Z", 0)),
            sequence,
            tag.getStringOr("DisplayName", "Waypoint #" + sequence),
            tag.getLongOr("CreatedTime", System.currentTimeMillis())
        );
    }
}
