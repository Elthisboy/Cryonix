package com.elthisboy.cryonix.client.fx;


import net.minecraft.client.render.RenderLayer;

public class XrayOverlayRenderer {

    public static void render(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext ctx) {
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.world == null) return;

        var cam = ctx.camera().getPos();
        var matrices = ctx.matrixStack();

        long session = com.elthisboy.cryonix.client.scan.CryonixScanController.session();
        var marks = com.elthisboy.cryonix.client.scan.XrayCache.visibleMarks(cam, 8000, session);

        if (marks.isEmpty()) return;

        var buffers = mc.getBufferBuilders().getEntityVertexConsumers();

        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
        com.mojang.blaze3d.systems.RenderSystem.disableCull();

        long now = System.currentTimeMillis();
        long ttlMs = com.elthisboy.cryonix.client.scan.CryonixScanController.ttlMs();

        for (var m : marks) {
            double ox = m.aabb.minX - cam.x, oy = m.aabb.minY - cam.y, oz = m.aabb.minZ - cam.z;
            double ex = m.aabb.maxX - cam.x, ey = m.aabb.maxY - cam.y, ez = m.aabb.maxZ - cam.z;

            long ageMs = Math.max(0L, now - m.createdAt);
            float fadeIn = clamp01(ageMs / 200f);
            long leftMs = Math.max(0L, m.expiresAt - now);
            float fadeOut = ttlMs > 0 ? clamp01(leftMs / 150f) : 1f;
            float fade = Math.min(fadeIn, Math.max(0.2f, fadeOut));

            float aFill  = 0.18f * fade;
            float aLines = m.color.af() * (0.5f + 0.5f * fade);

            // Relleno translúcido
            var fill = buffers.getBuffer(RenderLayer.getDebugFilledBox());
            net.minecraft.client.render.WorldRenderer.drawBox(
                    matrices, fill, ox, oy, oz, ex, ey, ez,
                    m.color.rf(), m.color.gf(), m.color.bf(), aFill
            );

            // Contorno -> usando nuestro RenderLayer custom XRAY_LINES
            var lines = buffers.getBuffer(CryonixRenderLayers.XRAY_LINES);
            net.minecraft.client.render.WorldRenderer.drawBox(
                    matrices, lines, ox, oy, oz, ex, ey, ez,
                    m.color.rf(), m.color.gf(), m.color.bf(), aLines
            );
        }

        buffers.draw();

        com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.enableCull();
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (Math.min(v, 1f));
    }
}