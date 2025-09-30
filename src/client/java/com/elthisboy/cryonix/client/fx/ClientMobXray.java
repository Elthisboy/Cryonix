package com.elthisboy.cryonix.client.fx;

import com.elthisboy.cryonix.client.state.MobXrayState;
import com.elthisboy.cryonix.client.util.RGBA;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import net.minecraft.client.render.BufferBuilderStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Environment(EnvType.CLIENT)
public final class ClientMobXray {

    private static boolean registered = false;

    // Estado del “scan” de mobs
    private static BlockPos center = null;
    private static int range = 0;
    private static long untilTick = 0;

    // Caché simple de entidades
    private static final List<LivingEntity> CACHE = new ArrayList<>();
    private static long nextRefreshTick = 0; // refresco cada ~4 ticks

    private static final BufferBuilderStorage LOCAL_BUFFERS = new BufferBuilderStorage(256);

    private ClientMobXray() {}

    public static void init() {
        if (registered) return;
        registered = true;
        // Evento correcto (consumers() no es null)
        WorldRenderEvents.AFTER_ENTITIES.register(ClientMobXray::onWorldRender);
    }

    public static void start(BlockPos centerPos, int scanRange, int durationTicks) {
        init();
        MobXrayState.loadTargetsFromConfig();

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        center = centerPos.toImmutable();
        range = Math.max(8, scanRange > 0 ? scanRange : MobXrayState.range());
        int dur = Math.max(10, durationTicks); // evita 0
        untilTick = mc.world.getTime() + dur;

        nextRefreshTick = 0;
        System.out.println("[Cryonix] MobXray START center=" + center + " range=" + range + " dur=" + dur);
    }

    private static void onWorldRender(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        if (!MobXrayState.enabled()) { center = null; range = 0; CACHE.clear(); return; }

        long now = mc.world.getTime();
        if (center == null || now >= untilTick) { center = null; range = 0; CACHE.clear(); return; }

        if (now >= nextRefreshTick) { nextRefreshTick = now + 4; refreshCache(); }

        renderOutlines(ctx);
    }

    private static void refreshCache() {
        CACHE.clear();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || center == null) return;

        Box area = new Box(center).expand(range);
        List<LivingEntity> list = mc.world.getEntitiesByClass(
                LivingEntity.class, area,
                ent -> ent.isAlive() && !ent.isSpectator() && ent != mc.player
        );

        for (LivingEntity ent : list) {
            Identifier id = Registries.ENTITY_TYPE.getId(ent.getType());
            if (id == null) continue;

            // ✅ Si está en targets y ON -> true; si no está, decide con renderUnknown
            if (MobXrayState.shouldRender(id)) {
                CACHE.add(ent);
            }
        }
    }
    private static void renderOutlines(WorldRenderContext ctx) {
        if (CACHE.isEmpty()) return;

        MatrixStack matrices = ctx.matrixStack();
        Vec3d cam = ctx.camera().getPos();

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        VertexConsumerProvider.Immediate immediate = LOCAL_BUFFERS.getEntityVertexConsumers();
        VertexConsumer vc = immediate.getBuffer(CryonixRenderLayers.XRAY_LINES);

        for (LivingEntity e : CACHE) {
            if (e == null || !e.isAlive()) continue;

            Identifier id = Registries.ENTITY_TYPE.getId(e.getType());
            if (id == null) continue;

            // ✅ Un solo punto de verdad
            if (!MobXrayState.shouldRender(id)) continue;

            // ✅ Color: config o default (para mobs de mods no listados)
            RGBA c = MobXrayState.colorForOrDefault(id);
            float r = c.r / 255f, g = c.g / 255f, b = c.b / 255f, a = c.a / 255f;

            Box box = e.getBoundingBox();
            double x1 = box.minX - cam.x, y1 = box.minY - cam.y, z1 = box.minZ - cam.z;
            double x2 = box.maxX - cam.x, y2 = box.maxY - cam.y, z2 = box.maxZ - cam.z;

            // Relleno (opcional)
            WorldRenderer.renderFilledBox(matrices, vc, x1,y1,z1, x2,y2,z2, r,g,b, a * 0.25f);

            // Contorno
            WorldRenderer.drawBox(matrices, vc, x1,y1,z1, x2,y2,z2, r,g,b, a);
        }

        immediate.draw();

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}