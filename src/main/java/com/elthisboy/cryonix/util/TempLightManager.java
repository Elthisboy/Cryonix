package com.elthisboy.cryonix.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLongArray;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.datafixer.DataFixTypes;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TempLightManager {
    private TempLightManager() {}

    /* ---------------------- PersistentState ---------------------- */

    public static final class TempLightPS extends PersistentState {
        private final Map<Long, Long> entries = new HashMap<>();

        public TempLightPS() {}

        public static TempLightPS fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
            TempLightPS ps = new TempLightPS();
            if (nbt.contains("lights")) {
                NbtCompound lights = nbt.getCompound("lights");
                long[] posArr = lights.getLongArray("p");
                long[] expArr = lights.getLongArray("e");
                int len = Math.min(posArr.length, expArr.length);
                for (int i = 0; i < len; i++) {
                    ps.entries.put(posArr[i], expArr[i]);
                }
            }
            return ps;
        }

        @Override
        public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
            long[] posArr = new long[entries.size()];
            long[] expArr = new long[entries.size()];
            int idx = 0;
            for (Map.Entry<Long, Long> e : entries.entrySet()) {
                posArr[idx] = e.getKey();
                expArr[idx] = e.getValue();
                idx++;
            }
            NbtCompound lights = new NbtCompound();
            lights.put("p", new NbtLongArray(posArr));
            lights.put("e", new NbtLongArray(expArr));
            nbt.put("lights", lights);
            return nbt;
        }

        public void put(BlockPos pos, long expiresAt) {
            entries.put(pos.asLong(), expiresAt);
            markDirty();
        }
        public void remove(BlockPos pos) {
            if (entries.remove(pos.asLong()) != null) markDirty();
        }
        public void remove(long packedPos) {
            if (entries.remove(packedPos) != null) markDirty();
        }
        public boolean isEmpty() { return entries.isEmpty(); }
        public Map<Long, Long> snapshot() { return new HashMap<>(entries); }
    }

    // Tipo requerido por PersistentStateManager en 1.21
    private static final PersistentState.Type<TempLightPS> TEMP_LIGHT_PS_TYPE =
            new PersistentState.Type<>(TempLightPS::new, TempLightPS::fromNbt, null);

    private static TempLightPS getPS(ServerWorld world) {
        PersistentStateManager mgr = world.getPersistentStateManager();
        return mgr.getOrCreate(TEMP_LIGHT_PS_TYPE, "cryonix_temp_lights");
    }

    /* ---------------------- Runtime cache ---------------------- */

    private static final Map<ServerWorld, Map<BlockPos, Integer>> LIVE = new ConcurrentHashMap<>();
    private static boolean registered = false;

    public static void register() {
        if (registered) return;
        registered = true;

        // Init/cleanup por mundo
        ServerWorldEvents.LOAD.register((server, world) -> {
            LIVE.computeIfAbsent(world, w -> new ConcurrentHashMap<>());
        });
        ServerWorldEvents.UNLOAD.register((server, world) -> {
            LIVE.remove(world);
        });

        // Tick por mundo
        ServerTickEvents.END_WORLD_TICK.register(TempLightManager::onWorldTick);
    }

    private static void onWorldTick(ServerWorld world) {
        TempLightPS ps = getPS(world);
        long now = world.getTime();

        // 1) Expiración runtime (luces puestas en esta sesión)
        Map<BlockPos, Integer> map = LIVE.get(world);
        if (map != null && !map.isEmpty()) {
            Iterator<Map.Entry<BlockPos, Integer>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<BlockPos, Integer> e = it.next();
                BlockPos pos = e.getKey();
                int left = e.getValue() - 1;
                if (left <= 0) {
                    if (world.getBlockState(pos).isOf(Blocks.LIGHT)) {
                        world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
                        world.getLightingProvider().checkBlock(pos);
                    }
                    it.remove();
                    ps.remove(pos);
                } else {
                    e.setValue(left);
                }
            }
        }

        // 2) Expiración persistida (sirve tras reconectar)
        if (!ps.isEmpty()) {
            int budget = 512; // evita lag
            for (Map.Entry<Long, Long> entry : ps.snapshot().entrySet()) {
                if (budget-- <= 0) break;
                long packed = entry.getKey();
                long expiresAt = entry.getValue();
                BlockPos pos = BlockPos.fromLong(packed);

                boolean isLight = world.getBlockState(pos).isOf(Blocks.LIGHT);
                if (expiresAt <= now || !isLight) {
                    if (isLight) {
                        world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
                        world.getLightingProvider().checkBlock(pos);
                    }
                    ps.remove(packed);
                }
            }
        }
    }

    /* ---------------------- API pública ---------------------- */

    /**
     * Coloca una luz temporal y la registra tanto en runtime como en persistencia.
     * @param world Mundo
     * @param at Posición
     * @param level 0..15
     * @param lifetimeTicks duración en ticks (≥1)
     */
    public static boolean placeTemporaryLight(ServerWorld world, BlockPos at, int level, int lifetimeTicks) {
        if (!world.getBlockState(at).isAir()) return false;

        int lvl = Math.max(0, Math.min(15, level));
        BlockState state = Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, lvl);
        boolean ok = world.setBlockState(at, state, 3);
        if (!ok) return false;

        world.getLightingProvider().checkBlock(at);

        LIVE.computeIfAbsent(world, w -> new ConcurrentHashMap<>())
                .put(at.toImmutable(), Math.max(1, lifetimeTicks));

        long expiresAt = world.getTime() + Math.max(1, lifetimeTicks);
        getPS(world).put(at.toImmutable(), expiresAt);
        return true;
    }

    public static void sprinkleOmni(ServerWorld world, BlockPos center, int radius, int maxLevel, int lifetimeTicks) {
        int r = Math.max(1, radius);
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos p = center.add(dx, dy, dz);
                    if (!world.getBlockState(p).isAir()) continue;

                    int d = Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz)); // Chebyshev
                    int level = Math.max(0, maxLevel - d);
                    if (level <= 0) continue;

                    placeTemporaryLight(world, p, level, lifetimeTicks);
                }
            }
        }
    }

    public static void raycastSixDirs(ServerWorld world, BlockPos center, int maxSteps, int startLevel, int lifetimeTicks) {
        Direction[] dirs = new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (Direction dir : dirs) {
            BlockPos.Mutable cursor = center.mutableCopy();
            for (int step = 1; step <= maxSteps; step++) {
                cursor.move(dir);
                if (!isChunkLoaded(world, cursor)) break;
                if (!world.getBlockState(cursor).isAir()) continue;

                int level = Math.max(0, startLevel - step);
                if (level <= 0) break;

                placeTemporaryLight(world, cursor, lifetimeTicks, startLevel); // orden: world,pos,level,lifetime
                // OJO: si tu sign es (world,pos,level,lifetime) deja como en el primer método
            }
        }
    }

    public static void illuminateOmni(ServerWorld world, BlockPos center, int coreLevel, int rayRadius, int sprinkleRadius, int lifetimeTicks) {
        placeTemporaryLight(world, center, coreLevel, lifetimeTicks);
        raycastSixDirs(world, center, Math.max(1, rayRadius), coreLevel, lifetimeTicks);
        sprinkleOmni(world, center, Math.max(1, sprinkleRadius), coreLevel, lifetimeTicks);
    }

    /* ---------------------- util ---------------------- */

    private static boolean isChunkLoaded(ServerWorld world, BlockPos pos) {
        return world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4);
    }
}