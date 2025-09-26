package com.elthisboy.cryonix.client.scan;

import com.elthisboy.cryonix.client.state.XrayState;
import com.elthisboy.cryonix.client.util.RGBA;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class XrayScanner {

    private XrayScanner() {}

    public static void tick(MinecraftClient mc) {
        if (!XrayState.enabled() || mc == null || mc.world == null) return;
        if (!CryonixScanController.isActive()) return;

        int budget = CryonixScanController.POSITIONS_PER_TICK;
        long session = CryonixScanController.session();

        while (budget-- > 0 && !CryonixScanController.work.isEmpty()) {
            BlockPos p = CryonixScanController.work.pollFirst();
            if (p == null) break;

            // Si esto te filtra demasiado, coméntalo para pruebas:
            if (!mc.world.isChunkLoaded(p)) continue;

            BlockState state = mc.world.getBlockState(p);
            if (state.isAir()) continue;

            Identifier id = Registries.BLOCK.getId(state.getBlock());
            if (!CryonixScanController.accepts(id)) continue;

            // Color desde config o fallback
            RGBA color = XrayState.colorFor(id, new RGBA(255, 255, 0, 200));
            XrayCache.put(new BlockMark(p, color, CryonixScanController.ttlMs(), session));
        }

        // Si ya no quedan posiciones, la sesión deja de estar activa (las marcas persisten hasta su TTL)
        CryonixScanController.doneIfEmpty();
    }

    public static void invalidateAll() {
        CryonixScanController.cancel();
        XrayCache.invalidateAll();
    }
}