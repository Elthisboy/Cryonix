package com.elthisboy.cryonix.client.scan;

import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class XrayCache {
    private static final Map<Long, BlockMark> MARKS = new ConcurrentHashMap<>();
    public static void put(BlockMark m){ MARKS.put(m.pos.asLong(), m); }
    public static void invalidateAll(){ MARKS.clear(); }

    public static List<BlockMark> visibleMarks(Vec3d cam, int max, long session){
        long now = System.currentTimeMillis();
        ArrayList<BlockMark> out = new ArrayList<>(Math.min(max, 2048));
        for (Iterator<BlockMark> it = MARKS.values().iterator(); it.hasNext();){
            BlockMark m = it.next();
            if (m.sessionId != session) continue;
            if (m.expiresAt < now){ it.remove(); continue; }
            if (cam.squaredDistanceTo(m.pos.getX()+0.5, m.pos.getY()+0.5, m.pos.getZ()+0.5) > 512*512) continue;
            out.add(m); if (out.size()>=max) break;
        }
        return out;
    }
}