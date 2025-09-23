package com.elthisboy.cryonix.data.provider;

import com.elthisboy.cryonix.Cryonix;
import com.elthisboy.cryonix.init.ItemGroupInit;
import com.elthisboy.cryonix.init.ItemInit;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class CryonixLanguageProvider extends FabricLanguageProvider {
    public CryonixLanguageProvider(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(dataOutput, "en_us", registryLookup);
    }

    private static void addText(@NotNull TranslationBuilder builder, @NotNull Text text, @NotNull String value){
        if(text.getContent() instanceof TranslatableTextContent translatableTextContent){
            builder.add(translatableTextContent.getKey(), value);
        }else{
            Cryonix.LOGGER.warn("Failded to add translation for text {}", text.getString());
        }
    }

    @Override
    public void generateTranslations(RegistryWrapper.WrapperLookup wrapperLookup, TranslationBuilder translationBuilder) {
        addText(translationBuilder, ItemGroupInit.CRYONIX_NIX_TEXT, "Cryonix: Scanner-gun Mod");


        // Items
        translationBuilder.add(ItemInit.SCANNER_GUN, "Scanner-Gun");
        translationBuilder.add(ItemInit.LASER_CHARGE_LV1, "Laser Charge Lv.1");
        translationBuilder.add(ItemInit.LASER_CHARGE_LV2, "Laser Charge Lv.2");
        translationBuilder.add(ItemInit.LASER_CHARGE_LV3, "Laser Charge Lv.3");

        translationBuilder.add("message.cryonix.reload.success", "Recharged +%s (%s/%s)");
        translationBuilder.add("message.cryonix.reload.none", "No Laser Charges or energy is full");
        translationBuilder.add("message.cryonix.cooldown", "Cooling down...");
        translationBuilder.add("message.cryonix.low_battery", "Low battery");
        translationBuilder.add("message.cryonix.scan.none", "Nothing detected");
        translationBuilder.add("message.cryonix.scan.block", "Block: %s — %s m");
        translationBuilder.add("message.cryonix.scan.entity", "Entity: %s — %s m");
        translationBuilder.add("message.cryonix.scan.aoe", "Around: %s ores, %s mobs");


    }


}

