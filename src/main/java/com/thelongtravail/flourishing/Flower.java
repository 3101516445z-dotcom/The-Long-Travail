package com.thelongtravail.flourishing;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public enum Flower {
    DANDELION("dandelion", .20), POPPY("poppy", .08), BLUE_ORCHID("blue_orchid", .25),
    ALLIUM("allium", .75), AZURE_BLUET("azure_bluet", .50), RED_TULIP("red_tulip", .12),
    ORANGE_TULIP("orange_tulip", .10), WHITE_TULIP("white_tulip", .12), PINK_TULIP("pink_tulip", .20),
    OXEYE_DAISY("oxeye_daisy", 1), CORNFLOWER("cornflower", .12), LILY_OF_THE_VALLEY("lily_of_the_valley", 1),
    WITHER_ROSE("wither_rose", 1), TORCHFLOWER("torchflower", .40), SUNFLOWER("sunflower", .10),
    LILAC("lilac", .25), ROSE_BUSH("rose_bush", .10), PEONY("peony", .10),
    PITCHER_PLANT("pitcher_plant", .10), PINK_PETALS("pink_petals", .10);
    public final String id;
    public final double initial;
    public static final net.minecraft.tags.TagKey<Item> TAG=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,ResourceLocation.fromNamespaceAndPath("the_long_travail","spring_flowers"));
    Flower(String id, double initial) { this.id = id; this.initial = initial; }
    public Item item() { return ForgeRegistries.ITEMS.getValue(new ResourceLocation("minecraft", id)); }
    public boolean craftable(){return new ItemStack(item()).is(TAG);}
    public static Flower of(ItemStack stack) {
        for (Flower f : values()) if (stack.is(f.item())) return f;
        return null;
    }
    public static Flower parse(String id) {
        for (Flower f : values()) if (f.id.equals(id) || ("minecraft:" + f.id).equals(id)) return f;
        return null;
    }
}
