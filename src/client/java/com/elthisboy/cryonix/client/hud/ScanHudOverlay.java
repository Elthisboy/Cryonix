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
    private static final int MAX_ROWS = 6;

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        var mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        long age = System.currentTimeMillis() - ScanHudData.getLastUpdateMs();
        if (age > PANEL_LIFETIME_MS) return;

        Window win = mc.getWindow();
        int sw = win.getScaledWidth();
        int sh = win.getScaledHeight();

        int panelW = 120;
        int panelH = 120;
        int x = sw - panelW - 6;
        int y = sh / 2 - panelH / 2;

        // fondo
        context.fill(x, y, x + panelW, y + panelH, 0x880A0E14);
        context.drawText(mc.textRenderer, Text.literal("Cryonix Scan"), x + 8, y + 6, 0xA0D0FF, false);

        // barra de energía (lee del item NBT)
        ItemStack held = mc.player.getMainHandStack();
        if (held.isEmpty() || !isScanner(held)) {
            ItemStack off = mc.player.getOffHandStack();
            if (!off.isEmpty() && isScanner(off)) held = off;
        }
        int max = 300;
        int cur = getEnergyFromStack(held, max);
        float pct = (float) cur / (float) max;

        int barX = x + panelW - 14;
        int barY = y + 8;
        int barH = panelH - 16;
        context.fill(barX, barY, barX + 6, barY + barH, 0xFF1A2530); // marco
        int filled = Math.round(pct * (barH - 2));
        int fy1 = barY + barH - 1 - filled;
        context.fill(barX + 1, fy1, barX + 5, barY + barH - 1, 0xFF2EC4FF); // relleno
        String pctTxt = (int)(pct*100) + "%";
        context.drawText(mc.textRenderer, pctTxt, barX - mc.textRenderer.getWidth(pctTxt) - 4, barY + barH - 10, 0xFFAAD6FF, false);

        // listas
        var blocks = ScanHudData.getBlocks();
        var mobs   = ScanHudData.getMobs();
        int listX = x + 8;
        int listY = y + 22;
        int rows = 0;
        for (int i = 0; i < blocks.size() && rows < MAX_ROWS; i++) {
            var b = blocks.get(i);
            context.drawText(mc.textRenderer, "• " + b.name + "  [" + String.format("%.1f", b.dist) + "m]",
                    listX, listY + rows*10, 0xFFD2F2FF, false);
            rows++;
        }
        for (int i = 0; i < mobs.size() && rows < MAX_ROWS; i++) {
            var m = mobs.get(i);
            context.drawText(mc.textRenderer, "⚠ " + m.name + "  [" + String.format("%.1f", m.dist) + "m]",
                    listX, listY + rows*10, 0xFFFFA8A8, false);
            rows++;
        }
    }

    private boolean isScanner(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        var id = net.minecraft.registry.Registries.ITEM.getId(stack.getItem());
        return id != null && "cryonix:scanner_gun".equals(id.toString());
    }
    private int getEnergyFromStack(ItemStack stack, int max) {
        if (stack == null || stack.isEmpty()) return 0;
        var comp = stack.get(net.minecraft.component.DataComponentTypes.CUSTOM_DATA);
        if (comp == null) return max; // por defecto llena si no hay tag
        var tag = comp.copyNbt();
        if (!tag.contains("ScannerEnergy")) return max;
        int v = tag.getInt("ScannerEnergy");
        return Math.max(0, Math.min(max, v));
    }
}