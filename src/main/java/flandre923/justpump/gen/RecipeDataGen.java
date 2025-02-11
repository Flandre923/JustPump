package flandre923.justpump.gen;

import flandre923.justpump.JustPump;
import flandre923.justpump.reg.ItemRegister;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.concurrent.CompletableFuture;

public class RecipeDataGen extends RecipeProvider{
    public RecipeDataGen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        super.buildRecipes(recipeOutput);
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ItemRegister.PUMP_ITEM.get())
                .pattern(" C ")
                .pattern(" K ")
                .pattern(" B ")
                .define('C', Blocks.COPPER_BLOCK)
                .define('K', Blocks.DRIED_KELP_BLOCK)
                .define('B', Items.BAMBOO)
                .unlockedBy("has_copper", RecipeProvider.has(Blocks.COPPER_BLOCK))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(JustPump.MODID, "pump_crafting"));
    }
}
