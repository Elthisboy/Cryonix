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
		CryonixNetworking.registerPayloadTypesOnce();
		LOGGER.info("Load!");

	}

	public static Identifier id(String path){
		return Identifier.of(MOD_ID,path);
	}
}


//AGREGAR EN EL SCANER BLOQUES DE INTERES COMO SPAWNERS
//QUE LA LUZ SE VEA EN TODOS LOS SENTIDOS, NO SOLO ARRIBA!


//TRATAR de ver si se puede hacer la misma logica del xray con los ores, pero con los mobs... para evitar usar glow y todos lo vean