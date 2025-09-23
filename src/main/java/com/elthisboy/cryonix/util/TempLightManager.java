package com.elthisboy.cryonix.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Coloca Blocks.LIGHT con un TTL para iluminar temporalmente posiciones. */
public final class TempLightManager {
    private static final class LightEntry {
        final BlockPos pos;
        int ticksLeft;
        LightEntry(BlockPos pos, int ticksLeft) { this.pos = pos; this.ticksLeft = ticksLeft; }
    }

    // Por mundo (dimension) guardamos un map de posiciones iluminadas
    private static final Map<ServerWorld, Map<BlockPos, LightEntry>> LIGHTS = new ConcurrentHashMap<>();
    private static boolean registered = false;

    private TempLightManager() {}

    /** Registra el tick handler una sola vez. Llamar desde tu main mod init. */
    public static void register() {
        if (registered) return;
        registered = true;
        ServerTickEvents.END_WORLD_TICK.register(TempLightManager::onWorldTick);
    }

    private static void onWorldTick(ServerWorld world) {
        Map<BlockPos, LightEntry> map = LIGHTS.get(world);
        if (map == null || map.isEmpty()) return;

        Iterator<Map.Entry<BlockPos, LightEntry>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, LightEntry> e = it.next();
            LightEntry le = e.getValue();
            if (--le.ticksLeft <= 0) {
                // si el bloque aún es LIGHT, lo limpiamos
                BlockState st = world.getBlockState(le.pos);
                if (st.isOf(Blocks.LIGHT)) {
                    world.setBlockState(le.pos, Blocks.AIR.getDefaultState(), 3);
                }
                it.remove();
            }
        }
    }

    /** Intenta colocar una luz temporal (solo en aire). Devuelve true si colocó. */
    public static boolean placeTemporaryLight(ServerWorld world, BlockPos at, int level, int lifetimeTicks) {
        if (!world.getBlockState(at).isAir()) return false;

        // LIGHT tiene property "level" 0-15
        var state = Blocks.LIGHT.getDefaultState().with(net.minecraft.block.LightBlock.LEVEL_15, Math.max(0, Math.min(15, level)));
        boolean ok = world.setBlockState(at, state, 3);
        if (!ok) return false;

        LIGHTS.computeIfAbsent(world, w -> new ConcurrentHashMap<>())
                .put(at.toImmutable(), new LightEntry(at.toImmutable(), Math.max(1, lifetimeTicks)));
        return true;
    }
}
