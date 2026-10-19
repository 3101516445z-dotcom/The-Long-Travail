package com.thelongtravail.compat.jei;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.registry.ModRegistry;
import java.util.List;
import java.util.function.Supplier;
import java.math.BigDecimal;
import com.thelongtravail.config.BoundlessItemsConfig;
import com.thelongtravail.network.TooltipConfigSync;
import net.minecraft.world.item.Item;
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

@JeiPlugin
public final class EssenceJeiPlugin implements IModPlugin {
    private record BlockDrop(Item block, Supplier<Item> result, String description, String chanceKey) {
        Component text() {
            if (chanceKey == null) return Component.translatable(description);
            double chance =
                    TooltipConfigSync.decimal("boundlessItem." + chanceKey, BoundlessItemsConfig.get(chanceKey));
            String percent =
                    BigDecimal.valueOf(chance).multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString();
            return Component.translatable(description, percent);
        }
    }

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
        registration.addRecipes(
                TYPE,
                List.of(
                        new BlockDrop(
                                Items.BUDDING_AMETHYST,
                                () -> ModRegistry.ESSENCES.get(TravailAspect.DEEP_VALLEY.ordinal()).get(),
                                "jei.the_long_travail.block_drop.description",
                                null),
                        new BlockDrop(
                                Items.POPPY,
                                ModRegistry.LITTLE_PRINCE::get,
                                "jei.the_long_travail.block_drop.prince_poppy",
                                "prince.poppyChance"),
                        new BlockDrop(
                                Items.ROSE_BUSH,
                                ModRegistry.LITTLE_PRINCE::get,
                                "jei.the_long_travail.block_drop.prince_rose",
                                "prince.roseChance")));
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
            builder.addInputSlot(20, 2).addItemStack(recipe.block().getDefaultInstance()).setStandardSlotBackground();
            builder.addOutputSlot(106, 2)
                    .addItemStack(recipe.result().get().getDefaultInstance())
                    .setOutputSlotBackground();
        }

        @Override
        public void draw(BlockDrop recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            gui.getRecipeArrow().draw(graphics, 65, 2);
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, BlockDrop recipe, IFocusGroup focuses) {
            builder.addText(recipe.text(), 150, 60).setPosition(0, 29);
        }
    }
}
