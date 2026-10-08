package com.thelongtravail.flourishing;

import com.google.gson.JsonObject;
import com.thelongtravail.config.FlourishingItemsConfig;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.core.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.*;

public final class SpringRecipes {
    public static final class Crown extends ShapedRecipe {
        public final Flower flower;
        public Crown(ResourceLocation id,ShapedRecipe base,Flower flower) {
            super(id,base.getGroup(),base.category(),base.getWidth(),base.getHeight(),base.getIngredients(),crown(flower));this.flower=flower;
        }
        @Override public boolean matches(CraftingContainer c,Level level) {return enabled(flower,level)&&super.matches(c,level);}
        @Override public RecipeSerializer<?> getSerializer(){return ModRegistry.SPRING_CROWN_RECIPE.get();}
    }
    public static ItemStack crown(Flower f) {ItemStack s=new ItemStack(ModRegistry.SPRING_GAME.get());FlowerData.write(s,List.of(f));return s;}
    private static boolean enabled(Flower f,Level level) {return f.craftable()&&(level.isClientSide?FlourishingItemsConfig.shownEnabled(f):FlourishingItemsConfig.ENABLED.get(f).get());}
    public static final class CrownSerializer implements RecipeSerializer<Crown> {
        private final ShapedRecipe.Serializer delegate=new ShapedRecipe.Serializer();
        @Override public Crown fromJson(ResourceLocation id,JsonObject json) {
            Flower f=Flower.parse(json.get("flower").getAsString());if(f==null)throw new IllegalArgumentException("Unknown crown flower");
            return new Crown(id,delegate.fromJson(id,json),f);
        }
        @Override public Crown fromNetwork(ResourceLocation id,FriendlyByteBuf b){Flower f=b.readEnum(Flower.class);return new Crown(id,delegate.fromNetwork(id,b),f);}
        @Override public void toNetwork(FriendlyByteBuf b,Crown r){b.writeEnum(r.flower);delegate.toNetwork(b,r);}
    }
    public static final class Change extends CustomRecipe {
        public Change(ResourceLocation id,CraftingBookCategory category){super(id,category);}
        private record Input(ItemStack crown,Flower flower,boolean wash){}
        private Input input(CraftingContainer c) {
            ItemStack crown=ItemStack.EMPTY; Flower flower=null;boolean wash=false;int other=0;
            for(int i=0;i<c.getContainerSize();i++) {
                ItemStack s=c.getItem(i);if(s.isEmpty())continue;
                if(s.is(ModRegistry.SPRING_GAME.get())) {if(!crown.isEmpty())return null;crown=s;}
                else {other++;if(s.is(Items.WATER_BUCKET))wash=true;else {flower=Flower.of(s);if(flower==null)return null;}}
            }
            return crown.isEmpty()||other!=1?null:new Input(crown,flower,wash);
        }
        @Override public boolean matches(CraftingContainer c,Level level){
            Input in=input(c);if(in==null)return false;var flowers=FlowerData.read(in.crown);
            return in.wash?flowers.size()>1:enabled(in.flower,level)&&flowers.size()<3&&!flowers.contains(in.flower);
        }
        @Override public ItemStack assemble(CraftingContainer c,RegistryAccess access){
            Input in=input(c);if(in==null)return ItemStack.EMPTY;var flowers=FlowerData.read(in.crown);
            if(in.wash&&flowers.size()<=1)return ItemStack.EMPTY;
            if(in.wash)flowers=new ArrayList<>(flowers.subList(0,1));else {if(flowers.size()>=3||flowers.contains(in.flower))return ItemStack.EMPTY;flowers.add(in.flower);}
            ItemStack out=in.crown.copy();out.setCount(1);FlowerData.write(out,flowers);return out;
        }
        @Override public boolean canCraftInDimensions(int w,int h){return w*h>=2;}
        @Override public RecipeSerializer<?> getSerializer(){return ModRegistry.SPRING_CHANGE_RECIPE.get();}
        @Override public NonNullList<ItemStack> getRemainingItems(CraftingContainer c){
            NonNullList<ItemStack> result=NonNullList.withSize(c.getContainerSize(),ItemStack.EMPTY);
            for(int i=0;i<c.getContainerSize();i++)if(c.getItem(i).is(Items.WATER_BUCKET))result.set(i,new ItemStack(Items.BUCKET));return result;
        }
    }
}
