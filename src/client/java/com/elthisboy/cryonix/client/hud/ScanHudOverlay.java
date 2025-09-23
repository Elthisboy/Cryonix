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
    private static final long PANEL_LIFETIME_MS = 5000; // tiempo visible después del último scan
    private static final int  MAX_ROWS          = 8;

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        var mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        long age = System.currentTimeMillis() - ScanHudData.getLastUpdateMs();
        if (age > PANEL_LIFETIME_MS) return;

        Window win = mc.getWindow();
        int sw = win.getScaledWidth();
        int sh = win.getScaledHeight();

        int panelW = 160;
        int panelH = 160;
        int x = sw - panelW - 6;
        int y = sh / 2 - panelH / 2;

        // Fondo + título
        context.fill(x, y, x + panelW, y + panelH, 0x880A0E14);
        context.drawText(mc.textRenderer, Text.literal("Cryonix Scan"), x + 8, y + 6, 0xA0D0FF, false);

        // Listas
        var blocks = ScanHudData.getBlocks();
        var mobs   = ScanHudData.getMobs();

        int rowY = y + 24;
        int rows = 0;

        // Bloques/Ores primero
        for (int i = 0; i < blocks.size() && rows < MAX_ROWS; i++) {
            var b = blocks.get(i);
            if (!b.icon.isEmpty()) {
                context.drawItem(b.icon, x + 8, rowY - 2);
            }
            String line = b.name + "  [" + String.format("%.1f", b.dist) + "m]";
            context.drawText(mc.textRenderer, line, x + 8 + 20, rowY + 2, 0xFFD2F2FF, false);
            rowY += 18; rows++;
        }

        // Mobs
        for (int i = 0; i < mobs.size() && rows < MAX_ROWS; i++) {
            var m = mobs.get(i);
            if (!m.icon.isEmpty()) {
                context.drawItem(m.icon, x + 8, rowY - 2);
            }
            String line = m.name + "  [" + String.format("%.1f", m.dist) + "m]";
            context.drawText(mc.textRenderer, line, x + 8 + 20, rowY + 2, m.color, false);
            rowY += 18; rows++;
        }

        // Si no hay nada, muestra aviso
        if (rows == 0 && ScanHudData.isEmptyScan()) {
            context.drawText(mc.textRenderer, Text.literal("No detections"), x + 8, y + 28, 0x94B4C8, false);
        }
    }
}