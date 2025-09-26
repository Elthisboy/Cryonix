package com.elthisboy.cryonix.client.fx;


import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayer.MultiPhaseParameters;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.RenderPhase.LineWidth;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

import java.util.OptionalDouble;


public class CryonixRenderLayers {

    public static final RenderLayer XRAY_LINES;

    static {
        MultiPhaseParameters params = MultiPhaseParameters.builder()
                .program(RenderPhase.LINES_PROGRAM)                 // <<< shader necesario para DrawMode.LINES
                .lineWidth(new LineWidth(OptionalDouble.of(1.0D)))
                .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)       // opcional: separa visualmente
                .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)           // GL_ALWAYS -> ignora z-buffer
                .writeMaskState(RenderPhase.COLOR_MASK)             // no escribe profundidad
                .cull(RenderPhase.DISABLE_CULLING)
                .build(false);

        XRAY_LINES = RenderLayer.of(
                "cryonix_xray_lines",
                VertexFormats.POSITION_COLOR,
                VertexFormat.DrawMode.LINES,
                256,
                false,
                false,
                params
        );
    }

    /** Forzar la inicialización estática (opcional). Llama en tu init cliente si quieres. */
    public static void init() {
        RenderSystem.assertOnRenderThread();
    }
}