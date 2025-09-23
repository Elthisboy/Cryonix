package com.elthisboy.cryonix;

import com.elthisboy.cryonix.init.*;
import com.elthisboy.cryonix.networking.CryonixNetworking;
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
		LOGGER.info("Load!");
	}

	public static Identifier id(String path){
		return Identifier.of(MOD_ID,path);
	}
}

//falta las traducciones para el hud
//tratar de agregar los iconos de los mobs(cabezas) e icono del ore o del jugador
//cuando no scanea nada que en el hud lo mencione
//buscar la manera de cuando hay 3 menas juntas o más solo mencione menas del ore y muestre un x3
//buscar la manera de al tirar el raycast al chocar ilumine la zona al chocar (No es necesario que sea dinamica la iluminacion)
//que ilumine y genere particulas donde estén los ores
//que hayan particulas donde este todo el radio de escaneo
