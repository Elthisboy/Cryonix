package com.elthisboy.cryonix.client.hud;

import com.elthisboy.cryonix.custom.ScannerGunItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ScanHudData {
    public static class EntryB {
        public final String name; public final double dist;
        public EntryB(String n, double d) { name = n; dist = d; }
    }
    public static class EntryM {
        public final String name; public final double dist;
        public EntryM(String n, double d) { name = n; dist = d; }
    }

    private static volatile List<EntryB> lastBlocks = new ArrayList<>();
    private static volatile List<EntryM> lastMobs   = new ArrayList<>();
    private static volatile long lastUpdateMs = 0L;

    // Energía actual del scanner (para la barra del HUD)
    private static volatile int lastEnergy = 0;
    private static volatile int lastMax    = 300;

    /** Llamado por el receptor de red (cliente). */
    public static void updateFromNet(List<EntryB> blocks, List<EntryM> mobs, int energy, int max) {
        lastBlocks = blocks;
        lastMobs   = mobs;
        lastEnergy = energy;
        lastMax    = max;
        lastUpdateMs = System.currentTimeMillis();
    }

    public static List<EntryB> getBlocks() { return lastBlocks; }
    public static List<EntryM> getMobs()   { return lastMobs; }
    public static long getLastUpdateMs()   { return lastUpdateMs; }
    public static int getEnergy()          { return lastEnergy; }
    public static int getMaxEnergy()       { return lastMax; }
}