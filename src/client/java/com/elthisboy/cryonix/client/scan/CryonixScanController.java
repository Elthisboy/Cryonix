package com.elthisboy.cryonix.client.scan;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class CryonixScanController {
    private static final AtomicLong COUNTER = new AtomicLong(1);
    private static long currentSession = 0;
    private static boolean active = false;

    static final Deque<BlockPos> work = new ArrayDeque<>();
    private static Set<Identifier> recognized = Set.of();

    //variables por sesión:
    public static int POSITIONS_PER_TICK = 600; // valor por defecto, se recalcula al iniciar
    private static long SESSION_TTL_MS = 4000;   // se recalcula al iniciar

    public static long session(){ return currentSession; }
    public static boolean isActive(){ return active; }
    public static long ttlMs(){ return SESSION_TTL_MS; } // usado por XrayScanner
    public static void cancel(){ active=false; work.clear(); recognized = Set.of(); }



    public static void startScan(MinecraftClient mc,
                                 BlockPos center,
                                 int range,
                                 Set<Identifier> recognizedSet,
                                 int durationTicks) {
        if (mc==null || mc.world==null) return;
        currentSession = COUNTER.getAndIncrement();
        active = true;

        if (recognizedSet == null || recognizedSet.isEmpty()) {
            recognized = com.elthisboy.cryonix.client.state.XrayState.targetIds();
        } else {
            recognized = Set.copyOf(recognizedSet);
        }

        XrayCache.invalidateAll();
        work.clear();

        for (int d = 0; d <= range; d++) {
            for (int dx = -d; dx <= d; dx++) {
                for (int dy = -d; dy <= d; dy++) {
                    int dzAbs = d - Math.abs(dx) - Math.abs(dy);
                    if (dzAbs < 0) continue;
                    for (int dz : new int[]{-dzAbs, dzAbs}) {
                        if (dx==0 && dy==0 && dz==0 && d>0) continue;
                        var p = center.add(dx, dy, dz);
                        if (p.getSquaredDistance(center) > (long)range * (long)range) continue;
                        work.addLast(p);
                    }
                }
            }
        }

        int ticks = Math.max(1, durationTicks);          // nunca 0
        SESSION_TTL_MS = ticks * 50L;                    // TTL visible = duración del escaneo
        int size = Math.max(1, work.size());
        POSITIONS_PER_TICK = Math.max(200, (int)Math.ceil(size / (double)ticks));
    }

    static boolean accepts(Identifier id){ return recognized.contains(id); }
    static void doneIfEmpty(){
        if (work.isEmpty()) {
            active = false;
        }
    }
}