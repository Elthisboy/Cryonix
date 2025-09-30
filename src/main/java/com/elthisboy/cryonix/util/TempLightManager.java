package com.elthisboy.cryonix.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    public static boolean placeTemporaryLight(ServerWorld world, BlockPos at, int level, int lifetimeTicks) {
        if (!world.getBlockState(at).isAir()) return false;

        int lvl = Math.max(0, Math.min(15, level));
        var state = Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, lvl);
        boolean ok = world.setBlockState(at, state, 3);
        if (!ok) return false;

        var key = at.toImmutable();
        LIGHTS.computeIfAbsent(world, w -> new ConcurrentHashMap<>())
                .put(key, new LightEntry(key, Math.max(1, lifetimeTicks)));
        return true;
    }

    /* ------------------------ helpers “omni” ------------------------ */


    public static void sprinkleOmni(ServerWorld world, BlockPos center, int radius, int maxLevel, int lifetimeTicks) {
        int r = Math.max(1, radius);
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos p = center.add(dx, dy, dz);
                    if (!world.getBlockState(p).isAir()) continue;

                    int d = Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz)); // Chebyshev
                    int level = Math.max(0, maxLevel - d); // caída lineal simple
                    if (level <= 0) continue;

                    placeTemporaryLight(world, p, level, lifetimeTicks);
                }
            }
        }
    }


    public static void raycastSixDirs(ServerWorld world, BlockPos center, int maxSteps, int startLevel, int lifetimeTicks) {
        Direction[] dirs = new Direction[]{
                Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
        };
        for (Direction dir : dirs) {
            BlockPos.Mutable cursor = center.mutableCopy();
            for (int step = 1; step <= maxSteps; step++) {
                cursor.move(dir);
                if (!world.isChunkLoaded(cursor)) break;
                if (!world.getBlockState(cursor).isAir()) continue;

                int level = Math.max(0, startLevel - step);
                if (level <= 0) break;

                // Coloca una luz aquí y sigue buscando más lejos para “tiras” continuas
                placeTemporaryLight(world, cursor, level, lifetimeTicks);
            }
        }
    }

    public static void illuminateOmni(ServerWorld world, BlockPos center, int coreLevel, int rayRadius, int sprinkleRadius, int lifetimeTicks) {
        // Centro (por si es cueva/cavidad)
        placeTemporaryLight(world, center, coreLevel, lifetimeTicks);

        // Rayos
        raycastSixDirs(world, center, Math.max(1, rayRadius), coreLevel, lifetimeTicks);

        // Rociado
        sprinkleOmni(world, center, Math.max(1, sprinkleRadius), coreLevel, lifetimeTicks);
    }
}