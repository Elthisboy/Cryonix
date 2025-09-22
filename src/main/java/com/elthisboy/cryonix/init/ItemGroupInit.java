package com.elthisboy.cryonix.init;

import com.elthisboy.cryonix.Cryonix;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ItemGroupInit {

    public static final Text CRYONIX_NIX_TEXT = Text.translatable("itemGroup."+ Cryonix.MOD_ID+ ".titleGroup");

    public static final ItemGroup NEO_NIX_GROUP = Registry.register(
            Registries.ITEM_GROUP,
            Identifier.of(Cryonix.MOD_ID, "neo_nix_group"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(ItemInit.SCANNER_GUN)) // icono de la pestaña
                    .displayName(CRYONIX_NIX_TEXT)
                    .entries((context, entries) -> {
                        // === Agregamos todos los ítems aquí ===
                        entries.add(ItemInit.SCANNER_GUN);
                        entries.add(ItemInit.LASER_CHARGE_LV1);
                        entries.add(ItemInit.LASER_CHARGE_LV2);
                        entries.add(ItemInit.LASER_CHARGE_LV3);

                    })
                    .build()
    );

    public static void load() {
    }
}
