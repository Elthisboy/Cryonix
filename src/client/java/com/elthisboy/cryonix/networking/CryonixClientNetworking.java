package com.elthisboy.cryonix.networking;

import com.elthisboy.cryonix.client.hud.ScanHudData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.elthisboy.cryonix.networking.payload.ScanResultsPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Environment(EnvType.CLIENT)
public final class CryonixClientNetworking {
    private CryonixClientNetworking() {}

    // Flags para evitar dobles registros
    private static final AtomicBoolean TYPE_REGISTERED = new AtomicBoolean(false);
    private static final AtomicBoolean RECEIVER_REGISTERED = new AtomicBoolean(false);

    public static void initClient() {
        // 1) Registrar el TIPO S2C una sola vez
        if (TYPE_REGISTERED.compareAndSet(false, true)) {
            try {
                PayloadTypeRegistry.playS2C().register(ScanResultsPayload.ID, ScanResultsPayload.CODEC);
            } catch (IllegalArgumentException already) {
                // Ya estaba registrado: ignorar para no crashear
            }
        }

        // 2) Registrar el handler una sola vez
        if (RECEIVER_REGISTERED.get()) {
            return; // ya está registrado, no hagas nada
        }

        ClientPlayNetworking.registerGlobalReceiver(ScanResultsPayload.ID, (payload, ctx) -> {
            var blocks = new ArrayList<ScanHudData.EntryB>(payload.blocksN().size());
            for (int i = 0; i < payload.blocksN().size(); i++) {
                blocks.add(new ScanHudData.EntryB(payload.blocksN().get(i), payload.blocksD().get(i)));
            }
            var mobs = new ArrayList<ScanHudData.EntryM>(payload.mobsN().size());
            for (int i = 0; i < payload.mobsN().size(); i++) {
                mobs.add(new ScanHudData.EntryM(payload.mobsN().get(i), payload.mobsD().get(i)));
            }

            ctx.client().execute(() ->
                    ScanHudData.updateFromNet(blocks, mobs, payload.energy(), payload.max())
            );
        });

        RECEIVER_REGISTERED.set(true);
    }
}