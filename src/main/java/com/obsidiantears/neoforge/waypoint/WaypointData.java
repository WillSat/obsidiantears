package com.obsidiantears.neoforge.waypoint;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

public class WaypointData {
    private static final int OVERWORLD_COLOR = 0x76FF03;
    private static final int NETHER_COLOR = 0xFF1744;
    private static final int END_COLOR = 0xD500F9;
    private static final int[] SEQUENCE_COLORS = {0xFF1744, 0xD500F9, 0x651FFF, 0x00E5FF, 0x76FF03, 0xFFEA00};

    private final String playerName;
    private final Identifier dimension;
    private final BlockPos pos;
    private int sequence;
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

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public String getDimensionPrefix() {
        return dimensionPrefix(dimension);
    }

    public String getQualifiedSequence() {
        return dimensionPrefix(dimension) + "-" + sequence;
    }

    public static String dimensionPrefix(Identifier dimension) {
        return switch (dimension.toString()) {
            case "minecraft:overworld" -> "OVW";
            case "minecraft:the_nether" -> "NTH";
            case "minecraft:the_end" -> "END";
            default -> {
                String path = dimension.getPath();
                if (path.startsWith("the_")) {
                    path = path.substring(4);
                }
                yield path;
            }
        };
    }

    public static String qualifiedSequence(Identifier dimension, int sequence) {
        return dimensionPrefix(dimension) + "-" + sequence;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public static MutableComponent buildLabelComponent(WaypointData waypoint) {
        MutableComponent text = Component.literal(waypoint.getDisplayName())
            .withStyle(ChatFormatting.WHITE);
        text.append(Component.literal(" " + waypoint.getQualifiedSequence())
            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(sequenceColor(waypoint.getDimension(), waypoint.getSequence())))));
        return text;
    }

    public static int sequenceColor(Identifier dimension, int sequence) {
        return switch (dimension.toString()) {
            case "minecraft:overworld" -> OVERWORLD_COLOR;
            case "minecraft:the_nether" -> NETHER_COLOR;
            case "minecraft:the_end" -> END_COLOR;
            default -> SEQUENCE_COLORS[(sequence - 1) % SEQUENCE_COLORS.length];
        };
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
            tag.getStringOr("DisplayName", "Unnamed Monument"),
            tag.getLongOr("CreatedTime", System.currentTimeMillis())
        );
    }
}
