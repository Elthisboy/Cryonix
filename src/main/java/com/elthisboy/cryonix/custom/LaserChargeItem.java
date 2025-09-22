package com.elthisboy.cryonix.custom;

import net.minecraft.item.Item;

public class LaserChargeItem extends Item {
    private final int energyAmount;

    public LaserChargeItem(Item.Settings settings, int energyAmount) {
        super(settings);
        this.energyAmount = energyAmount;
    }

    public int getEnergyAmount() {
        return energyAmount;
    }
}