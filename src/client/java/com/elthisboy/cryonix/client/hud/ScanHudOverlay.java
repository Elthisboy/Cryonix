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

    private static final long PANEL_LIFETIME_MS = 5000;
    private static final int  MAX_ROWS          = 7;

    private static final int  ROW_H      = 18;
    private static final int  PAD        = 10;   // margen interno
    private static final int  ICON_W     = 16;  // tamaño icono
    private static final int  ICON_GAP   = 6;   // separación icono-texto
    private static final int  SECTION_GAP= 6;   // gap header -> lista

    private static final int  MIN_W      = 160; // límites para evitar “bailes”
    private static final int  MAX_W      = 340;

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        var mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        long age = System.currentTimeMillis() - ScanHudData.getLastUpdateMs();
        if (age > PANEL_LIFETIME_MS) return;

        Window win = mc.getWindow();
        int sw = win.getScaledWidth();
        int sh = win.getScaledHeight();

        // ===== Preparar líneas y medir =====
        final var tr = mc.textRenderer;
        String title = "Cryonix Scan";

        // número de filas reales
        int rowsBlocks = Math.min(ScanHudData.getBlocks().size(), MAX_ROWS);
        int rowsLeft = MAX_ROWS - rowsBlocks;
        int rowsMobs = Math.min(ScanHudData.getMobs().size(), Math.max(0, rowsLeft));
        boolean showNothing = (rowsBlocks + rowsMobs) == 0 && ScanHudData.isEmptyScan();

        // calcular ancho según texto más largo
        int textStartXOffset = ICON_W + ICON_GAP;
        int maxTextW = tr.getWidth(title);
        for (int i = 0; i < rowsBlocks; i++) {
            var b = ScanHudData.getBlocks().get(i);
            String line = "• " + b.name + "  [" + String.format("%.1f", b.dist) + "m]";
            maxTextW = Math.max(maxTextW, tr.getWidth(line) + textStartXOffset);
        }
        for (int i = 0; i < rowsMobs; i++) {
            var m = ScanHudData.getMobs().get(i);
            String line = m.name + "  [" + String.format("%.1f", m.dist) + "m]";
            maxTextW = Math.max(maxTextW, tr.getWidth(line) + textStartXOffset);
        }
        if (showNothing) {
            maxTextW = Math.max(maxTextW, tr.getWidth("Nothing found"));
        }

        int headerH = 18;
        int panelW = Math.max(MIN_W, Math.min(MAX_W, maxTextW + PAD * 2));
        int rowsNeeded = (rowsBlocks + rowsMobs > 0) ? (rowsBlocks + rowsMobs)
                : (showNothing ? 1 : 0);
        int panelH = headerH + SECTION_GAP + rowsNeeded * ROW_H + PAD;

        // colocar panel pegado al borde derecho, centrado verticalmente
        int x = sw - panelW - 8;
        int y = sh / 2 - panelH / 2;

        // ===== Fondo =====
        context.fill(x, y, x + panelW, y + panelH, 0x880A0E14);

        // ===== Título centrado =====
        int titleW = tr.getWidth(title);
        int titleX = x + (panelW - titleW) / 2;
        int titleY = y + 6;
        context.drawText(tr, Text.literal(title), titleX, titleY, 0xA0D0FF, false);

        // ===== Listas =====
        int listX = x + PAD;
        int listY = y + headerH + SECTION_GAP;
        int rows = 0;

        // BLOQUES / ORES
        for (int i = 0; i < rowsBlocks && rows < MAX_ROWS; i++) {
            var b = ScanHudData.getBlocks().get(i);

            // icono
            if (b.icon != null && b.icon != ItemStack.EMPTY) {
                context.drawItem(b.icon, listX, listY + rows * ROW_H);
            }

            // texto
            String line = "• " + b.name + "  [" + String.format("%.1f", b.dist) + "m]";
            int textX = listX + textStartXOffset;
            int textY = listY + rows * ROW_H + 5;
            context.drawText(tr, line, textX, textY, 0xD2F2FF, false);

            rows++;
        }

        // MOBS
        for (int i = 0; i < rowsMobs && rows < MAX_ROWS; i++) {
            var m = ScanHudData.getMobs().get(i);

            if (m.icon != null && m.icon != ItemStack.EMPTY) {
                context.drawItem(m.icon, listX, listY + rows * ROW_H);
            }

            String line = "• " + m.name + "  [" + String.format("%.1f", m.dist) + "m]";
            int textX = listX + textStartXOffset;
            int textY = listY + rows * ROW_H + 5;
            context.drawText(tr, line, textX, textY, m.color, false);

            rows++;
        }

        // Nada encontrado
        if (showNothing) {
            context.drawText(
                    mc.textRenderer,
                    net.minecraft.text.Text.translatable("hud.cryonix.nothing.found"),
                    listX,
                    listY + 2,
                    0x9EC3FF,
                    true
            );        }
    }
}