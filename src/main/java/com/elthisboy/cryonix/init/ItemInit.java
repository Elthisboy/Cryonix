package com.elthisboy.cryonix.init;

import com.elthisboy.cryonix.Cryonix;
import com.elthisboy.cryonix.custom.LaserChargeItem;
import com.elthisboy.cryonix.custom.ScannerGunItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ItemInit {

    // Helper
    private static Item.Settings withLore(Item.Settings s, Text... lines) {
        return s.component(DataComponentTypes.LORE, new LoreComponent(java.util.List.of(lines)));
    }

    private static Item.Settings withName(Item.Settings base, String translationKey, Formatting color) {
        return base.component(
                DataComponentTypes.CUSTOM_NAME,
                Text.translatable(translationKey).formatted(color).styled(s -> s.withItalic(false))
        );
    }




    public static final Item SCANNER_GUN = register("scanner_gun", new ScannerGunItem(new Item.Settings().maxCount(1)));


    // Cargas Láser
    public static final Item LASER_CHARGE_LV1 = register("laser_charge_lv1",
            new LaserChargeItem(new Item.Settings(), 50));
    public static final Item LASER_CHARGE_LV2 = register("laser_charge_lv2",
            new LaserChargeItem(new Item.Settings(), 100));
    public static final Item LASER_CHARGE_LV3 = register("laser_charge_lv3",
            new LaserChargeItem(new Item.Settings(), 200));


    public static <T extends Item> T register(String name, T item) {
        return Registry.register(Registries.ITEM, Cryonix.id(name), item);
    }


    public static void load() {

    }

}
