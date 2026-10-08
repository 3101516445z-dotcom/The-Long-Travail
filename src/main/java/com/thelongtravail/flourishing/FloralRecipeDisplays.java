package com.thelongtravail.flourishing;

import com.thelongtravail.registry.ModRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import java.util.*;

// JEI 合并展示的数据模型，不注册到服务器配方管理器。
public final class FloralRecipeDisplays {
    public record ChangeDisplay(String operation,List<ItemStack> inputs,List<ItemStack> materials,List<ItemStack> outputs) {}
    public record Snapshot(CrownDisplay crown,List<ChangeDisplay> changes) {}
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("the_long_travail",path);}
    private static ItemStack stack(Flower...flowers){ItemStack s=new ItemStack(ModRegistry.SPRING_GAME.get());FlowerData.write(s,List.of(flowers));return s;}
    private static ChangeDisplay display(String op){return new ChangeDisplay(op,new ArrayList<>(),new ArrayList<>(),new ArrayList<>());}
    private static void row(ChangeDisplay d,ItemStack input,ItemStack material,ItemStack output){d.inputs.add(input);d.materials.add(material);d.outputs.add(output);}
    public static Snapshot build(int mask) {
        List<Flower> enabled=Arrays.stream(Flower.values()).filter(f->(mask&(1<<f.ordinal()))!=0).toList();
        var select=display("select");var add=display("add");var wash=display("wash");
        for(Flower flower:enabled)row(select,stack(),new ItemStack(flower.item()),stack(flower));
        // 覆盖所有主花与新增花；第三朵展示一个合法示例，避免枚举全部排列。
        for(Flower primary:Flower.values()) {
            for(Flower added:enabled)if(primary!=added) {
                row(add,stack(primary),new ItemStack(added.item()),stack(primary,added));
                Flower existing=Arrays.stream(Flower.values()).filter(f->f!=primary&&f!=added).findFirst().orElseThrow();
                row(add,stack(primary,existing),new ItemStack(added.item()),stack(primary,existing,added));
            }
            var secondary=Arrays.stream(Flower.values()).filter(f->f!=primary).limit(2).toList();
            row(wash,stack(primary,secondary.get(0)),new ItemStack(Items.WATER_BUCKET),stack(primary));
            row(wash,stack(primary,secondary.get(0),secondary.get(1)),new ItemStack(Items.WATER_BUCKET),stack(primary));
        }
        List<ChangeDisplay> changes=new ArrayList<>();
        for(var d:List.of(select,add,wash))if(!d.inputs.isEmpty())changes.add(d);
        return new Snapshot(enabled.isEmpty()?null:new CrownDisplay(enabled,mask),List.copyOf(changes));
    }
    public static final class CrownDisplay extends ShapedRecipe {
        public final List<Flower> flowers;
        CrownDisplay(List<Flower> flowers,int mask){super(id("jei/spring_game/"+mask),"",CraftingBookCategory.EQUIPMENT,3,3,ingredients(flowers),stack(flowers.get(0)));this.flowers=List.copyOf(flowers);}
        private static NonNullList<Ingredient> ingredients(List<Flower> flowers){
            return NonNullList.of(Ingredient.EMPTY,Ingredient.of(Items.WHEAT),Ingredient.of(Items.WHEAT),Ingredient.of(Items.WHEAT),Ingredient.of(Items.STRING),Ingredient.of(flowers.stream().map(f->new ItemStack(f.item()))),Ingredient.of(Items.STRING),Ingredient.of(Items.WHEAT),Ingredient.EMPTY,Ingredient.of(Items.WHEAT));
        }
    }
    private FloralRecipeDisplays() {}
}
