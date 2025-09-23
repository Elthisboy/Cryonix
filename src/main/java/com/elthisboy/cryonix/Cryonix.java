package com.elthisboy.cryonix;

import com.elthisboy.cryonix.init.*;
import com.elthisboy.cryonix.networking.CryonixNetworking;
import com.elthisboy.cryonix.util.TempLightManager;
import net.fabricmc.api.ModInitializer;

import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cryonix implements ModInitializer {
	public static final String MOD_ID = "cryonix";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Loading...");
		ItemInit.load();
		BlockInit.load();
		ItemGroupInit.load();
		TempLightManager.register();
		CryonixNetworking.registerPayloadTypes();
		LOGGER.info("Load!");

	}

	public static Identifier id(String path){
		return Identifier.of(MOD_ID,path);
	}
}


