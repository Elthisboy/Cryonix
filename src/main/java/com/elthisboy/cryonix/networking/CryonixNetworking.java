package com.elthisboy.cryonix.networking;


import com.elthisboy.cryonix.networking.payload.ScanResultsPayload;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;

public final class CryonixNetworking {

    private CryonixNetworking() {}

    /** Envía al cliente los resultados del escaneo (HUD). */
    public static void sendScanResults(ServerPlayerEntity player,
                                       List<String> blocksN, List<Double> blocksD,
                                       List<String> mobsN,   List<Double> mobsD,
                                       int energy, int max) {

        ScanResultsPayload payload = new ScanResultsPayload(
                energy, max,
                blocksN, blocksD,
                mobsN,   mobsD
        );
        ServerPlayNetworking.send(player, payload);
    }
}