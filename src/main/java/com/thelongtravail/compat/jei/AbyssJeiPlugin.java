package com.thelongtravail.compat.jei;

import com.thelongtravail.registry.ModRegistry;

import mezz.jei.api.*;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

import java.util.List;

@JeiPlugin
public final class AbyssJeiPlugin implements IModPlugin {
    private record Example(boolean stackable) {}

    private static final RecipeType<Example> TYPE =
            RecipeType.create("the_long_travail", "perfect_fluid_examples", Example.class);

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath("the_long_travail", "abyss_items");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration r) {
        r.addRecipeCategories(new Category(r.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration r) {
        r.addRecipes(TYPE, List.of(new Example(true), new Example(false)));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration r) {
        r.addRecipeCatalyst(new ItemStack(ModRegistry.PERFECT_FLUID.get()), TYPE);
    }

    private static final class Category implements IRecipeCategory<Example> {
        private final IGuiHelper gui;

        Category(IGuiHelper gui) {
            this.gui = gui;
        }

        @Override
        public RecipeType<Example> getRecipeType() {
            return TYPE;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("jei.the_long_travail.perfect_fluid.title");
        }

        @Override
        public IDrawable getIcon() {
            return gui.createDrawableItemStack(new ItemStack(ModRegistry.PERFECT_FLUID.get()));
        }

        @Override
        public int getWidth() {
            return 166;
        }

        @Override
        public int getHeight() {
            return 112;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder b, Example example, IFocusGroup focus) {
            var target = new ItemStack(example.stackable ? Items.DIAMOND : Items.DIAMOND_SWORD);
            b.addInputSlot(4, 3)
                    .addItemStack(new ItemStack(ModRegistry.PERFECT_FLUID.get()))
                    .setStandardSlotBackground();
            b.addInputSlot(26, 3).addItemStack(target).setStandardSlotBackground();
            var output = target.copy();
            output.setCount(example.stackable ? 2 : 1);
            b.addOutputSlot(106, 3).addItemStack(output).setOutputSlotBackground();
            if (!example.stackable)
                b.addSlot(RecipeIngredientRole.RENDER_ONLY, 140, 3)
                        .addItemStack(target.copy())
                        .setStandardSlotBackground();
        }

        @Override
        public void draw(
                Example example, IRecipeSlotsView slots, GuiGraphics graphics, double x, double y) {
            gui.getRecipeArrow().draw(graphics, 65, 3);
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder b, Example example, IFocusGroup focus) {
            b.addText(
                            Component.translatable(
                                    "jei.the_long_travail.perfect_fluid."
                                            + (example.stackable ? "stackable" : "unstackable")),
                            166,
                            84)
                    .setPosition(0, 29);
        }
    }
}
