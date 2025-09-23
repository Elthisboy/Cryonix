package com.elthisboy.cryonix.networking;


import com.elthisboy.cryonix.Cryonix;
import com.elthisboy.cryonix.networking.payload.ScanResultsPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * Registro de tipos de payload y utilidades de networking (lado común/servidor).
 */
public final class CryonixNetworking {

    private static boolean PAYLOAD_TYPES_REGISTERED = false;

    private CryonixNetworking() {}

    /** Llamar una sola vez por ejecución. Reentrante/seguro. */
    public static synchronized void registerPayloadTypes() {
        if (PAYLOAD_TYPES_REGISTERED) return;
        PayloadTypeRegistry.playS2C().register(ScanResultsPayload.ID, ScanResultsPayload.CODEC);
        PAYLOAD_TYPES_REGISTERED = true;
    }

    /**
     * Envía al cliente los resultados del escaneo para el HUD.
     *
     * @param player    jugador destino
     * @param blocksN   nombres (agrupados) de bloques/menas
     * @param blocksD   distancias (mismo orden que blocksN)
     * @param blocksId  ids de bloque (para iconos), mismo orden que blocksN
     * @param mobsN     nombres (agrupados) de mobs
     * @param mobsD     distancias (mismo orden que mobsN)
     * @param mobsId    ids de entity type (para iconos), mismo orden que mobsN
     * @param energy    energía actual (aunque no la muestres en HUD)
     * @param max       energía máxima
     */
    public static void sendScanResults(
            ServerPlayerEntity player,
            List<String> blocksN, List<Double> blocksD, List<String> blocksId,
            List<String> mobsN,   List<Double> mobsD,   List<String> mobsId,
            int energy, int max
    ) {
        ScanResultsPayload payload = new ScanResultsPayload(
                energy, max,
                blocksN, blocksD, blocksId,
                mobsN, mobsD, mobsId
        );
        ServerPlayNetworking.send(player, payload);

        Cryonix.LOGGER.info(
                "[SCAN:SEND] -> {} blocks={}, mobs={}",
                player.getGameProfile().getName(),
                blocksN.size(),
                mobsN.size()
        );
    }
}

//falta editar esto, ta en el chat
//CryonixNetworking
//Cryonix
//CryonixClientNetworking