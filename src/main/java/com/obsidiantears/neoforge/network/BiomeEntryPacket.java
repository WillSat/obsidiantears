package com.obsidiantears.neoforge.network;

import com.obsidiantears.neoforge.ObsidianTears;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BiomeEntryPacket(String biomeKey, String timeText, String weatherText, boolean showInfo, int color) implements CustomPacketPayload {
    public static final Type<BiomeEntryPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ObsidianTears.MODID, "biome_entry"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BiomeEntryPacket> CODEC = CustomPacketPayload.codec(BiomeEntryPacket::write, BiomeEntryPacket::read);

    private static void write(BiomeEntryPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.biomeKey);
        buf.writeUtf(packet.timeText);
        buf.writeUtf(packet.weatherText);
        buf.writeBoolean(packet.showInfo);
        buf.writeInt(packet.color);
    }

    private static BiomeEntryPacket read(FriendlyByteBuf buf) {
        return new BiomeEntryPacket(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readBoolean(), buf.readInt());
    }

    @Override
    public Type<BiomeEntryPacket> type() {
        return TYPE;
    }

    public static void handle(BiomeEntryPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> com.obsidiantears.neoforge.client.TeleportFeedbackOverlay.showBiomeEntry(
            packet.biomeKey, packet.timeText, packet.weatherText, packet.showInfo, packet.color));
    }
}
