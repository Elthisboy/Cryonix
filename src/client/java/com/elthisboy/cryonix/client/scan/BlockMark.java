package com.elthisboy.cryonix.client.scan;

import com.elthisboy.cryonix.client.util.RGBA;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public class BlockMark {
    public final BlockPos pos;
    public final Box aabb;
    public final RGBA color;
    public final long sessionId;
    public long createdAt;
    public long expiresAt; // ms

    public BlockMark(BlockPos pos, RGBA color, long ttlMs, long session){
        this.pos = pos;
        this.aabb = new Box(pos);
        this.color = color;
        this.sessionId = session;
        this.expiresAt = System.currentTimeMillis() + ttlMs;
        this.createdAt = System.currentTimeMillis();

    }
}