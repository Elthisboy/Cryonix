package com.elthisboy.cryonix;

import com.elthisboy.cryonix.networking.CryonixClientNetworking;
import com.elthisboy.cryonix.networking.CryonixNetworking;
import net.fabricmc.api.ClientModInitializer;
import com.elthisboy.cryonix.client.hud.ScanHudOverlay;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;


public class CryonixClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		com.elthisboy.cryonix.networking.CryonixClientNetworking.initClient();
		net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(
				new com.elthisboy.cryonix.client.hud.ScanHudOverlay()
		);
	}
}