package com.thelongtravail.flourishing;

import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.item.ItemStack;
import java.util.*;

// 只追踪曾被绑定的实体；离线或放在箱中的饰品取出后也能识别已确认的死亡。
public final class AffectionDeaths extends SavedData {
    private final Set<UUID> watched=new HashSet<>(),dead=new HashSet<>();
    public static AffectionDeaths get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(AffectionDeaths::load,AffectionDeaths::new,"the_long_travail_affection");}
    private static AffectionDeaths load(CompoundTag tag) {
        AffectionDeaths data=new AffectionDeaths();read(tag,"Watched",data.watched);read(tag,"Dead",data.dead);return data;
    }
    private static void read(CompoundTag tag,String key,Set<UUID> out){for(Tag entry:tag.getList(key,Tag.TAG_INT_ARRAY))try{out.add(NbtUtils.loadUUID(entry));}catch(IllegalArgumentException ignored){}}
    @Override public CompoundTag save(CompoundTag tag){write(tag,"Watched",watched);write(tag,"Dead",dead);return tag;}
    private static void write(CompoundTag tag,String key,Set<UUID> values){ListTag list=new ListTag();values.forEach(id->list.add(NbtUtils.createUUID(id)));tag.put(key,list);}
    public void watch(UUID id){boolean changed=watched.add(id);changed|=dead.remove(id);if(changed)setDirty();}
    public boolean died(UUID id){if(!watched.remove(id))return false;dead.add(id);setDirty();return true;}
    public void clean(ItemStack stack){if(stack.hasTag()&&stack.getTag().hasUUID(Affection.TARGET)&&dead.contains(stack.getTag().getUUID(Affection.TARGET)))Affection.clear(stack);}
}
