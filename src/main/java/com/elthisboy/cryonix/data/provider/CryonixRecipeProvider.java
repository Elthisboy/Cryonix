package com.elthisboy.cryonix.data.provider;

import com.elthisboy.cryonix.init.ItemInit;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

public class CryonixRecipeProvider extends FabricRecipeProvider {
    public CryonixRecipeProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    public void generate(RecipeExporter recipeExporter) {

        // Lv1:
        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ItemInit.LASER_CHARGE_LV1, 1)
                .pattern("RR")
                .pattern("QQ")
                .input('R', Items.REDSTONE)
                .input('Q', Items.QUARTZ)
                .criterion("has_redstone", conditionsFromItem(Items.REDSTONE))
                .offerTo(recipeExporter);

        // Lv2:
        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ItemInit.LASER_CHARGE_LV2, 1)
                .pattern("C C")
                .pattern(" B ")
                .pattern("C C")
                .input('C', ItemInit.LASER_CHARGE_LV1)
                .input('B', Items.REDSTONE_BLOCK)
                .criterion("has_lv1", conditionsFromItem(ItemInit.LASER_CHARGE_LV1))
                .offerTo(recipeExporter);

        // Lv3:
        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ItemInit.LASER_CHARGE_LV3, 1)
                .pattern("C C")
                .pattern(" B ")
                .pattern(" D ")
                .input('C', ItemInit.LASER_CHARGE_LV2)
                .input('B', Items.REDSTONE_BLOCK)
                .input('D', Items.DIAMOND)
                .criterion("has_lv2", conditionsFromItem(ItemInit.LASER_CHARGE_LV2))
                .offerTo(recipeExporter);

        // SCANNER-GUN:
        ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, ItemInit.SCANNER_GUN, 1)
                .pattern("GBC")
                .pattern("DCW")
                .pattern("BB ")
                .input('C', ItemInit.LASER_CHARGE_LV2)
                .input('W', Items.GLOWSTONE_DUST)
                .input('G', Items.GLASS_PANE)
                .input('B', Items.REDSTONE_BLOCK)
                .input('D', Items.DIAMOND)
                .criterion("has_diamond_pickaxe", conditionsFromItem(Items.DIAMOND_PICKAXE))
                .offerTo(recipeExporter);


    }
}
