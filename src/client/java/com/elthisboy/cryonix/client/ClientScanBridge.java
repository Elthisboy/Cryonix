package com.elthisboy.cryonix.client;

import com.elthisboy.cryonix.client.scan.CryonixScanController;
import com.elthisboy.cryonix.client.state.XrayState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Set;

public final class ClientScanBridge {

    private ClientScanBridge() {}

    public static void startScanAt(BlockPos center, int range, Set<Identifier> recognized, int durationTicks) {
        var mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) return;

        XrayState.setEnabled(true); // <- importante
        XrayState.setRange(range);
        CryonixScanController.startScan(mc, center, range, recognized, durationTicks);

        try {
            com.elthisboy.cryonix.client.fx.ClientMobXray.start(center, range, 60);
        } catch (Throwable t) {
        }
    }


}