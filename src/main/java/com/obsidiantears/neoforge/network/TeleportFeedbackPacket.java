package com.obsidiantears.neoforge.network;

import com.obsidiantears.neoforge.ObsidianTears;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TeleportFeedbackPacket(String biomeKey, String waypointName, String qualifiedSequence, int seqColor, String timeText, String weatherText, boolean showInfo) implements CustomPacketPayload {
    public static final Type<TeleportFeedbackPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ObsidianTears.MODID, "teleport_feedback"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TeleportFeedbackPacket> CODEC = CustomPacketPayload.codec(TeleportFeedbackPacket::write, TeleportFeedbackPacket::read);

    private static void write(TeleportFeedbackPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.biomeKey);
        buf.writeUtf(packet.waypointName);
        buf.writeUtf(packet.qualifiedSequence);
        buf.writeInt(packet.seqColor);
        buf.writeUtf(packet.timeText);
        buf.writeUtf(packet.weatherText);
        buf.writeBoolean(packet.showInfo);
    }

    private static TeleportFeedbackPacket read(FriendlyByteBuf buf) {
        return new TeleportFeedbackPacket(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readInt(), buf.readUtf(), buf.readUtf(), buf.readBoolean());
    }

    @Override
    public Type<TeleportFeedbackPacket> type() {
        return TYPE;
    }

    public static void handle(TeleportFeedbackPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> com.obsidiantears.neoforge.client.TeleportFeedbackOverlay.show(
            packet.biomeKey, packet.waypointName, packet.qualifiedSequence, packet.seqColor, packet.timeText, packet.weatherText, packet.showInfo));
    }
}
