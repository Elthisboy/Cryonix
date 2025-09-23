package com.elthisboy.cryonix.client.hud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.Window;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class ScanHudOverlay implements HudRenderCallback {
    private static final long PANEL_LIFETIME_MS = 5000; // cuánto tiempo se muestra tras el último escaneo
    private static final int MAX_ROWS = 6;              // líneas máximas visibles

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        long age = System.currentTimeMillis() - ScanHudData.getLastUpdateMs();
        if (age > PANEL_LIFETIME_MS) return;

        // Datos a mostrar
        var blocks = ScanHudData.getBlocks();
        var mobs   = ScanHudData.getMobs();

        // Cálculo de tamaño del panel (alto dinámico según filas)
        int rows = Math.min(MAX_ROWS, blocks.size() + mobs.size());
        int titleH = 18;
        int padding = 10;
        int rowH = 10;
        int panelW = 180; // un poco más ancho para nombres largos
        int panelH = padding + titleH + rows * rowH + padding;

        Window win = mc.getWindow();
        int sw = win.getScaledWidth();
        int sh = win.getScaledHeight();

        int x = sw - panelW - 8;             // margen derecho
        int y = sh / 3;                       // un poco bajo del centro

        // Fondo y título
        context.fill(x, y, x + panelW, y + panelH, 0x880A0E14);
        context.drawText(mc.textRenderer, Text.literal("Cryonix Scan"),
                x + 8, y + 6, 0xA0D0FF, false);

        // Listas (sin energía)
        int listX = x + 8;
        int listY = y + padding + titleH - 2;
        int drawn = 0;

        // Bloques primero
        for (int i = 0; i < blocks.size() && drawn < MAX_ROWS; i++) {
            var b = blocks.get(i);
            context.drawText(mc.textRenderer,
                    "• " + b.name + "  [" + String.format("%.1f", b.dist) + "m]",
                    listX, listY + drawn * rowH, 0xFFD2F2FF, false);
            drawn++;
        }
        // Luego mobs
        for (int i = 0; i < mobs.size() && drawn < MAX_ROWS; i++) {
            var m = mobs.get(i);
            context.drawText(mc.textRenderer,
                    "⚠ " + m.name + "  [" + String.format("%.1f", m.dist) + "m]",
                    listX, listY + drawn * rowH, 0xFFFFA8A8, false);
            drawn++;
        }
    }
}