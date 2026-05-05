package com.obsidiantears.neoforge.network;

import com.obsidiantears.neoforge.ObsidianTears;
import com.obsidiantears.neoforge.screen.WaypointNamingScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record NamingPacket(Identifier dimension, BlockPos pos, int sequence) implements CustomPacketPayload {
    public static final Type<NamingPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ObsidianTears.MODID, "naming"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NamingPacket> CODEC = CustomPacketPayload.codec(NamingPacket::write, NamingPacket::read);

    private static void write(NamingPacket packet, FriendlyByteBuf buf) {
        buf.writeIdentifier(packet.dimension);
        buf.writeBlockPos(packet.pos);
        buf.writeInt(packet.sequence);
    }

    private static NamingPacket read(FriendlyByteBuf buf) {
        return new NamingPacket(buf.readIdentifier(), buf.readBlockPos(), buf.readInt());
    }

    @Override
    public Type<NamingPacket> type() {
        return TYPE;
    }

    public static void handle(NamingPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new WaypointNamingScreen(packet.dimension, packet.pos, packet.sequence)));
    }
}
