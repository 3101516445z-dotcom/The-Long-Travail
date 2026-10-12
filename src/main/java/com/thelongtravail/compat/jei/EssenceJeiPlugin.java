package com.thelongtravail.compat.jei;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.registry.ModRegistry;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

// 独立展示母岩破坏来源，不依赖 ALI 或其方块分类开关。
@JeiPlugin
public final class EssenceJeiPlugin implements IModPlugin {
    private record BlockDrop() {}

    private static final RecipeType<BlockDrop> TYPE =
            RecipeType.create("the_long_travail", "essence_block_drop", BlockDrop.class);

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath("the_long_travail", "essence_sources");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new Category(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(TYPE, List.of(new BlockDrop()));
    }

    private static final class Category implements IRecipeCategory<BlockDrop> {
        private final IGuiHelper gui;

        private Category(IGuiHelper gui) {
            this.gui = gui;
        }

        @Override
        public RecipeType<BlockDrop> getRecipeType() {
            return TYPE;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("jei.the_long_travail.block_drop");
        }

        @Override
        public IDrawable getIcon() {
            return gui.createDrawableItemStack(Items.BUDDING_AMETHYST.getDefaultInstance());
        }

        @Override
        public int getWidth() {
            return 150;
        }

        @Override
        public int getHeight() {
            return 92;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, BlockDrop recipe, IFocusGroup focuses) {
            builder.addInputSlot(20, 2)
                    .addItemStack(Items.BUDDING_AMETHYST.getDefaultInstance())
                    .setStandardSlotBackground();
            builder.addOutputSlot(106, 2)
                    .addItemStack(
                            ModRegistry.ESSENCES
                                    .get(TravailAspect.DEEP_VALLEY.ordinal())
                                    .get()
                                    .getDefaultInstance())
                    .setOutputSlotBackground();
        }

        @Override
        public void draw(
                BlockDrop recipe,
                IRecipeSlotsView slots,
                GuiGraphics graphics,
                double mouseX,
                double mouseY) {
            gui.getRecipeArrow().draw(graphics, 65, 2);
        }

        @Override
        public void createRecipeExtras(
                IRecipeExtrasBuilder builder, BlockDrop recipe, IFocusGroup focuses) {
            builder.addText(
                            Component.translatable("jei.the_long_travail.block_drop.description"),
                            150,
                            60)
                    .setPosition(0, 29);
        }
    }
}
