package com.elthisboy.cryonix;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class CryonixDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {

        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

        pack.addProvider(CryonixBlockTableProvider::new);
        pack.addProvider(CryonixLanguageProvider::new);
        pack.addProvider(CryonixBlockTagProvider::new);
        pack.addProvider(CryonixItemTagProvider::new);
        pack.addProvider(CryonixProvider::new);
        pack.addProvider(CryonixRecipeProvider::new);
    }
}