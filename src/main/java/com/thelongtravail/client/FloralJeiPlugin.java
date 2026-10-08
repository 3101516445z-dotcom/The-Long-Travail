package com.thelongtravail.client;

import com.thelongtravail.flourishing.*;
import com.thelongtravail.flourishing.FloralRecipeDisplays.*;
import com.thelongtravail.config.FlourishingItemsConfig;
import com.thelongtravail.network.TooltipConfigSync;
import com.thelongtravail.registry.ModRegistry;
import mezz.jei.api.*;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.*;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.core.NonNullList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import java.util.*;

// 只合并客户端展示；服务器仍使用真实配方。每行输入、材料和输出按索引联动。
@JeiPlugin
public final class FloralJeiPlugin implements IModPlugin {
    public static final RecipeType<ChangeDisplay> TYPE=RecipeType.create("the_long_travail","flower_arrangement",ChangeDisplay.class);
    private final Map<Integer,Snapshot> snapshots=new HashMap<>();
    private IJeiRuntime runtime;
    private Snapshot active;
    private long revision=Long.MIN_VALUE;
    private boolean listening;
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("the_long_travail",path);}
    @Override public ResourceLocation getPluginUid(){return id("floral");}
    @Override public void registerCategories(IRecipeCategoryRegistration r){r.addRecipeCategories(new Category(r.getJeiHelpers().getGuiHelper()));}
    @Override public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration r){r.getCraftingCategory().addCategoryExtension(CrownDisplay.class,CrownExtension::new);}
    @Override public void registerRecipes(IRecipeRegistration r) {
        snapshots.clear();active=null;
        r.addItemStackInfo(new ItemStack(ModRegistry.SPRING_GAME.get()),FloralText.text("jei.the_long_travail.flowers"));
    }
    @Override public void onRuntimeAvailable(IJeiRuntime runtime){this.runtime=runtime;revision=Long.MIN_VALUE;if(!listening){MinecraftForge.EVENT_BUS.addListener(this::tick);MinecraftForge.EVENT_BUS.addListener(this::tagsUpdated);MinecraftForge.EVENT_BUS.addListener(this::recipesUpdated);listening=true;}refresh();}
    @Override public void onRuntimeUnavailable(){runtime=null;active=null;snapshots.clear();revision=Long.MIN_VALUE;}
    private void tagsUpdated(net.minecraftforge.event.TagsUpdatedEvent e){revision=Long.MIN_VALUE;}
    private void recipesUpdated(net.minecraftforge.client.event.RecipesUpdatedEvent e){revision=Long.MIN_VALUE;}
    private void tick(TickEvent.ClientTickEvent e){if(e.phase==TickEvent.Phase.END)refresh();}
    private void refresh() {
        if(runtime==null||Minecraft.getInstance().level==null||revision==TooltipConfigSync.revision())return;
        revision=TooltipConfigSync.revision();var manager=runtime.getRecipeManager();
        var originals=manager.createRecipeLookup(RecipeTypes.CRAFTING).includeHidden().get().filter(r->r instanceof SpringRecipes.Crown).toList();
        manager.hideRecipes(RecipeTypes.CRAFTING,originals);
        int mask=0;for(Flower f:Flower.values())if(f.craftable()&&FlourishingItemsConfig.shownEnabled(f))mask|=1<<f.ordinal();
        if(active!=null){if(active.crown()!=null)manager.hideRecipes(RecipeTypes.CRAFTING,List.of(active.crown()));manager.hideRecipes(TYPE,active.changes());}
        Snapshot next=snapshots.get(mask);
        if(next==null){next=FloralRecipeDisplays.build(mask);snapshots.put(mask,next);if(next.crown()!=null)manager.addRecipes(RecipeTypes.CRAFTING,List.of(next.crown()));manager.addRecipes(TYPE,next.changes());}
        if(next.crown()!=null)manager.unhideRecipes(RecipeTypes.CRAFTING,List.of(next.crown()));manager.unhideRecipes(TYPE,next.changes());active=next;
    }
    private record CrownExtension(CrownDisplay recipe) implements ICraftingCategoryExtension {
        @Override public void setRecipe(IRecipeLayoutBuilder b,ICraftingGridHelper grid,IFocusGroup focus){
            var inputs=new ArrayList<>(recipe.getIngredients().stream().map(i->Arrays.asList(i.getItems())).toList());
            inputs.set(4,recipe.flowers.stream().map(f->new ItemStack(f.item())).toList());
            var slots=grid.createAndSetInputs(b,inputs,3,3);
            var output=grid.createAndSetOutputs(b,recipe.flowers.stream().map(SpringRecipes::crown).toList());
            b.createFocusLink(slots.get(4),output);
        }
        @Override public ResourceLocation getRegistryName(){return recipe.getId();}
        @Override public int getWidth(){return 3;}
        @Override public int getHeight(){return 3;}
    }
    private static final class Category implements IRecipeCategory<ChangeDisplay> {
        private final IGuiHelper gui;
        Category(IGuiHelper gui){this.gui=gui;}
        @Override public RecipeType<ChangeDisplay> getRecipeType(){return TYPE;}
        @Override public Component getTitle(){return FloralText.gold(Component.translatable("jei.the_long_travail.arrangement"));}
        @Override public IDrawable getIcon(){return gui.createDrawableItemStack(new ItemStack(ModRegistry.SPRING_GAME.get()));}
        @Override public int getWidth(){return 150;}
        @Override public int getHeight(){return 52;}
        @Override public void setRecipe(IRecipeLayoutBuilder b,ChangeDisplay d,IFocusGroup focus){
            var input=b.addInputSlot(1,1).addItemStacks(d.inputs()).setStandardSlotBackground();
            var material=b.addInputSlot(36,1).addItemStacks(d.materials()).setStandardSlotBackground();
            var output=b.addOutputSlot(96,1).addItemStacks(d.outputs()).setOutputSlotBackground();
            b.createFocusLink(input,material,output);
            if(d.operation().equals("wash"))b.addOutputSlot(126,1).addItemStack(new ItemStack(Items.BUCKET)).setStandardSlotBackground();
            b.setShapeless();
        }
        @Override public void draw(ChangeDisplay d,IRecipeSlotsView slots,GuiGraphics g,double x,double y){
            gui.getRecipePlusSign().draw(g,22,4);gui.getRecipeArrow().draw(g,62,1);

        }
        @Override public void createRecipeExtras(mezz.jei.api.gui.widgets.IRecipeExtrasBuilder builder,ChangeDisplay d,IFocusGroup focus){
            builder.addText(Component.translatable("jei.the_long_travail."+d.operation()),150,10).setPosition(0,29);
            if(d.operation().equals("add"))builder.addText(Component.translatable("jei.the_long_travail.limits"),150,10).setPosition(0,41);
        }
        // 每个配置快照使用独立索引，旧快照隐藏后不会因查询旧材料而重新出现。
        @Override public ResourceLocation getRegistryName(ChangeDisplay d){return null;}
    }
}
