package com.thelongtravail.flourishing;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public enum Flower {
    DANDELION("dandelion", .20),
    POPPY("poppy", .10),
    BLUE_ORCHID("blue_orchid", .25),
    ALLIUM("allium", .75),
    AZURE_BLUET("azure_bluet", .50),
    RED_TULIP("red_tulip", .20),
    ORANGE_TULIP("orange_tulip", .10),
    WHITE_TULIP("white_tulip", .15),
    PINK_TULIP("pink_tulip", .20),
    OXEYE_DAISY("oxeye_daisy", 1),
    CORNFLOWER("cornflower", .20),
    LILY_OF_THE_VALLEY("lily_of_the_valley", 1),
    WITHER_ROSE("wither_rose", 1),
    TORCHFLOWER("torchflower", .40),
    SUNFLOWER("sunflower", .15),
    LILAC("lilac", .25),
    ROSE_BUSH("rose_bush", .05),
    PEONY("peony", .10),
    PITCHER_PLANT("pitcher_plant", .10),
    PINK_PETALS("pink_petals", .10),
    CHERRY_LEAVES("cherry_leaves", .20),
    FLOWERING_AZALEA("flowering_azalea", .20),
    CHORUS_FLOWER("chorus_flower", 1),
    SPORE_BLOSSOM("spore_blossom", .20),
    FLOWERING_AZALEA_LEAVES("flowering_azalea_leaves", 2);
    public final String id;
    public final double initial;
    private final ResourceLocation itemId;
    private static final Flower[] ORDERED = values();
    private static final java.util.Map<String, Flower> BY_ID;

    static {
        var ids = new java.util.HashMap<String, Flower>();
        for (Flower flower : ORDERED) {
            ids.put(flower.id, flower);
            ids.put("minecraft:" + flower.id, flower);
        }
        BY_ID = java.util.Collections.unmodifiableMap(ids);
    }

    public static final net.minecraft.tags.TagKey<Item> TAG =
            net.minecraft.tags.TagKey.create(
                    net.minecraft.core.registries.Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath("the_long_travail", "spring_flowers"));

    Flower(String id, double initial) {
        this.id = id;
        this.initial = initial;
        this.itemId = ResourceLocation.fromNamespaceAndPath("minecraft", id);
    }

    public Item item() {
        return ForgeRegistries.ITEMS.getValue(itemId);
    }

    public boolean craftable() {
        return new ItemStack(item()).is(TAG);
    }

    public static Flower of(ItemStack stack) {
        for (Flower f : ORDERED) if (stack.is(f.item())) return f;
        return null;
    }

    public static Flower parse(String id) {
        return BY_ID.get(id);
    }
}
