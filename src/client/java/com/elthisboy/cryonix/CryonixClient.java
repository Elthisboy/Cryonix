package com.elthisboy.cryonix;

import com.elthisboy.cryonix.client.fx.ClientMobXray;
import com.elthisboy.cryonix.client.fx.CryonixRenderLayers;
import com.elthisboy.cryonix.client.fx.XrayOverlayRenderer;
import com.elthisboy.cryonix.client.scan.XrayScanner;
import com.elthisboy.cryonix.client.state.XrayState;
import com.elthisboy.cryonix.networking.CryonixClientNetworking;
import net.fabricmc.api.ClientModInitializer;
import com.elthisboy.cryonix.client.hud.ScanHudOverlay;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;


public class CryonixClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Inicializa la red del cliente UNA vez
		CryonixClientNetworking.initClient();

		// Cargar o generar el config al iniciar el cliente
		com.elthisboy.cryonix.client.state.XrayState.loadTargetsFromConfig();
		com.elthisboy.cryonix.client.state.MobXrayState.loadTargetsFromConfig();

		// Registra el overlay del HUD UNA vez
		HudRenderCallback.EVENT.register(new ScanHudOverlay());

		ClientMobXray.init();

		CryonixRenderLayers.init();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.world == null) return;
			XrayScanner.tick(client);
		});

		WorldRenderEvents.LAST.register(ctx -> {
			if (!XrayState.enabled()) return;
			XrayOverlayRenderer.render(ctx);
		});
	}
}