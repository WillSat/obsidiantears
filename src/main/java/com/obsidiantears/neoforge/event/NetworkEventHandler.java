package com.obsidiantears.neoforge.event;

import com.obsidiantears.neoforge.ObsidianTears;
import com.obsidiantears.neoforge.network.MoveWaypointPacket;
import com.obsidiantears.neoforge.network.NamingPacket;
import com.obsidiantears.neoforge.network.RequestWaypointsPacket;
import com.obsidiantears.neoforge.network.SyncWaypointsPacket;
import com.obsidiantears.neoforge.network.BiomeEntryPacket;
import com.obsidiantears.neoforge.network.TeleportFeedbackPacket;
import com.obsidiantears.neoforge.network.ReturnTeleportPacket;
import com.obsidiantears.neoforge.network.TeleportRequestPacket;
import com.obsidiantears.neoforge.network.WaypointNamingPacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public class NetworkEventHandler {
    @SubscribeEvent
    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");

        registrar.playToServer(RequestWaypointsPacket.TYPE, RequestWaypointsPacket.CODEC, RequestWaypointsPacket::handle);
        registrar.playToServer(TeleportRequestPacket.TYPE, TeleportRequestPacket.CODEC, TeleportRequestPacket::handle);
        registrar.playToServer(ReturnTeleportPacket.TYPE, ReturnTeleportPacket.CODEC, ReturnTeleportPacket::handle);
        registrar.playToServer(WaypointNamingPacket.TYPE, WaypointNamingPacket.CODEC, WaypointNamingPacket::handle);
        registrar.playToServer(MoveWaypointPacket.TYPE, MoveWaypointPacket.CODEC, MoveWaypointPacket::handle);
        registrar.playToClient(SyncWaypointsPacket.TYPE, SyncWaypointsPacket.CODEC, SyncWaypointsPacket::handle);
        registrar.playToClient(NamingPacket.TYPE, NamingPacket.CODEC, NamingPacket::handle);
        registrar.playToClient(TeleportFeedbackPacket.TYPE, TeleportFeedbackPacket.CODEC, TeleportFeedbackPacket::handle);
        registrar.playToClient(BiomeEntryPacket.TYPE, BiomeEntryPacket.CODEC, BiomeEntryPacket::handle);
    }
}
