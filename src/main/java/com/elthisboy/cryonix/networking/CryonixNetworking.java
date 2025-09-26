package com.elthisboy.cryonix.networking;


import com.elthisboy.cryonix.Cryonix;
import com.elthisboy.cryonix.networking.payload.ScanResultsPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Registro de tipos de payload y utilidades de networking (lado común/servidor).
 */
public final class CryonixNetworking {

    private CryonixNetworking() {}

    // --- Guardia para evitar doble registro ---
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    /** Llamar una sola vez: ya maneja idempotencia. */
    public static void registerPayloadTypesOnce() {
        if (!REGISTERED.compareAndSet(false, true)) {
            // Ya estaba registrado; no hacer nada.
            return;
        }
        PayloadTypeRegistry.playS2C().register(ScanResultsPayload.ID, ScanResultsPayload.CODEC);
        Cryonix.LOGGER.info("[Networking] Payloads registrados (S2C): {}", ScanResultsPayload.ID.id());
    }

    public static void sendScanResults(ServerPlayerEntity player,
                                       int energy, int max,
                                       List<String> blocksN, List<Double> blocksD, List<String> blocksId,
                                       List<String> mobsN,   List<Double> mobsD,   List<String> mobsId,
                                       BlockPos scanCenter, int scanRadius) {          // <— NUEVO radius

        int radius = Math.max(1, scanRadius);
        int limit  = 48;

        List<BlockPos> foundOres = findOresMatchingIds(
                player.getWorld(),
                (scanCenter == null ? player.getBlockPos() : scanCenter),
                radius, blocksId, limit);

        List<Long> orePacked = new ArrayList<>(foundOres.size());
        for (BlockPos pos : foundOres) if (pos != null) orePacked.add(pos.asLong());

        ScanResultsPayload payload = new ScanResultsPayload(
                energy, max,
                (scanCenter == null ? player.getBlockPos() : scanCenter).asLong(), // <—
                radius,                                                             // <—
                blocksN, blocksD, blocksId,
                orePacked,
                mobsN, mobsD, mobsId
        );

        ServerPlayNetworking.send(player, payload);

        Cryonix.LOGGER.info("[SCAN:SEND] -> {} blocks={} mobs={} orePos={} (center={}, r={}, cap={})",
                player.getName().getString(),
                blocksN == null ? 0 : blocksN.size(),
                mobsN   == null ? 0 : mobsN.size(),
                orePacked.size(),
                (scanCenter == null ? "playerPos" : scanCenter.toShortString()),
                radius, limit);
    }

    // Mantén tus overloads; ahora delegan con radius por defecto
    public static void sendScanResults(ServerPlayerEntity player,
                                       int energy, int max,
                                       List<String> blocksN, List<Double> blocksD, List<String> blocksId,
                                       List<String> mobsN,   List<Double> mobsD,   List<String> mobsId,
                                       BlockPos scanCenter) {
        sendScanResults(player, energy, max, blocksN, blocksD, blocksId, mobsN, mobsD, mobsId, scanCenter, 16);
    }
    public static void sendScanResults(ServerPlayerEntity player,
                                       int energy, int max,
                                       List<String> blocksN, List<Double> blocksD, List<String> blocksId,
                                       List<String> mobsN,   List<Double> mobsD,   List<String> mobsId) {
        sendScanResults(player, energy, max, blocksN, blocksD, blocksId, mobsN, mobsD, mobsId, player.getBlockPos(), 16);
    }
    public static void sendScanResults(ServerPlayerEntity player,
                                       List<String> blocksN, List<Double> blocksD, List<String> blocksId,
                                       List<String> mobsN,   List<Double> mobsD,   List<String> mobsId,
                                       int energy, int max) {
        sendScanResults(player, energy, max, blocksN, blocksD, blocksId, mobsN, mobsD, mobsId, player.getBlockPos(), 16);
    }



    // ===== Helper =====
    private static List<BlockPos> findOresMatchingIds(World world, BlockPos center, int radius,
                                                      List<String> blocksId, int limit) {
        List<BlockPos> out = new ArrayList<>();
        if (world == null || center == null || blocksId == null || blocksId.isEmpty()) return out;

        Set<String> wanted = new HashSet<>(blocksId);
        int r = Math.max(1, radius);
        int cap = Math.max(1, limit);

        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos p = center.add(dx, dy, dz);
                    var id = Registries.BLOCK.getId(world.getBlockState(p).getBlock());
                    if (id != null && wanted.contains(id.toString())) {
                        out.add(p.toImmutable());
                        if (out.size() >= cap) return out;
                    }
                }
            }
        }
        return out;
    }
}