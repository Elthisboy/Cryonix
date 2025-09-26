package com.elthisboy.cryonix.client.hud;

import com.elthisboy.cryonix.Cryonix;
import com.elthisboy.cryonix.custom.ScannerGunItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class ScanHudData {

    private ScanHudData() {}

    // ===== Tipos de entrada que usa el HUD =====
    public static final class BlockEntry {
        public final String name;
        public final double dist;
        public final ItemStack icon;

        public BlockEntry(String name, double dist, ItemStack icon) {
            this.name = name;
            this.dist = dist;
            this.icon = icon == null ? ItemStack.EMPTY : icon;
        }
    }

    public static final class MobEntry {
        public final String name;
        public final double dist;
        public final ItemStack icon;
        public final int color; // color del texto

        public MobEntry(String name, double dist, ItemStack icon, int color) {
            this.name = name;
            this.dist = dist;
            this.icon = icon == null ? ItemStack.EMPTY : icon;
            this.color = color;
        }
    }

    // ===== Estado (cliente) =====
    private static final List<BlockEntry> BLOCKS = new ArrayList<>();
    private static final List<MobEntry> MOBS = new ArrayList<>();
    private static long lastUpdateMs = 0L;
    private static boolean lastScanWasEmpty = false;

    // ===== API =====
    public static synchronized void update(List<BlockEntry> blocks, List<MobEntry> mobs, boolean nothingFound) {
        BLOCKS.clear();
        MOBS.clear();

        if (blocks != null) BLOCKS.addAll(blocks);
        if (mobs != null)   MOBS.addAll(mobs);
        lastUpdateMs = System.currentTimeMillis();
        lastScanWasEmpty = nothingFound;

        Cryonix.LOGGER.info(
                "[SCAN:HUD] update blocks={}, mobs={}, empty={}",
                blocks != null ? blocks.size() : 0,
                mobs != null ? mobs.size() : 0,
                nothingFound
        );
    }

    public static synchronized void clear() {
        BLOCKS.clear();
        MOBS.clear();
        lastScanWasEmpty = false;
        lastUpdateMs = System.currentTimeMillis();
    }

    public static synchronized List<BlockEntry> getBlocks() {
        return Collections.unmodifiableList(new ArrayList<>(BLOCKS));
    }

    public static synchronized List<MobEntry> getMobs() {
        return Collections.unmodifiableList(new ArrayList<>(MOBS));
    }

    public static long getLastUpdateMs() {
        return lastUpdateMs;
    }

    /** Verdadero si el último escaneo no encontró nada (sirve para pintar el mensaje “No detections”). */
    public static boolean isEmptyScan() {
        return lastScanWasEmpty;
    }


}