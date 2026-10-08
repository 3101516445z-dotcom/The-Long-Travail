package com.thelongtravail.underworld;

import com.thelongtravail.TravailAspect;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

// 世界级记录仅保存身份与死亡资格，不托管、不复制死亡物品。
public final class UnderworldLedger extends SavedData {
    public static final int MAX_NBT_DEPTH = 64;
    public static final String BOOK_ID="TravailBookId";
    private final Set<UUID> spent=new HashSet<>(), witnesses=new HashSet<>();
    public final Map<UUID,CompoundTag> deaths=new HashMap<>();
    public final Set<String> graves=new HashSet<>();
    public static UnderworldLedger get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(UnderworldLedger::load,UnderworldLedger::new,"the_long_travail_underworld");
    }
    public static UUID identity(ItemStack s) {
        CompoundTag n=s.getOrCreateTag();
        if(!n.hasUUID(BOOK_ID)) n.putUUID(BOOK_ID,UUID.randomUUID());
        return n.getUUID(BOOK_ID);
    }
    public boolean consumed(ItemStack s) { return s.hasTag() && s.getTag().hasUUID(BOOK_ID) && spent.contains(s.getTag().getUUID(BOOK_ID)); }
    public boolean spent(UUID id) { return spent.contains(id); }
    public boolean hasChanges() { return !spent.isEmpty()||!witnesses.isEmpty(); }
    public void consume(UUID id) { spent.add(id); setDirty(); }
    public void witness(UUID id) { witnesses.add(id); setDirty(); }
    public void reconcile(ItemStack s) {
        if(s.isEmpty()) return;
        if(consumed(s)) { s.setCount(0); return; }
        if(s.hasTag()) reconcileTag(s.getTag(),0);
    }
    // 同时覆盖储物水晶和提前序列化的保物品数据；仅识别本模组身份字段。
    public void reconcileTag(Tag tag,int depth) {
        if(depth>MAX_NBT_DEPTH||tag==null||!hasChanges())return;
        // 立即完成的事务仍同步校正，无需复制每个复合标签的键集合。
        if(depth==0){var cursor=new NbtReconcileCursor(()->tag);while(cursor.step(this)){}return;}
        reconcileNode(tag);
        if(tag instanceof CompoundTag c)for(String key:c.getAllKeys())reconcileTag(c.get(key),depth+1);
        else if(tag instanceof ListTag list)for(Tag child:list)reconcileTag(child,depth+1);
    }
    public void reconcileNode(Tag tag) {
        if(tag instanceof CompoundTag c) {
            if(c.getString("id").equals("the_long_travail:book_of_the_dead")) {
                CompoundTag n=c.getCompound("tag");
                if(n.hasUUID(BOOK_ID)&&spent.contains(n.getUUID(BOOK_ID))) c.putByte("Count",(byte)0);
            }
            if(c.contains("LongTravail",Tag.TAG_COMPOUND)) {
                CompoundTag root=c.getCompound("LongTravail");
                if(root.hasUUID("JourneyId")&&witnesses.contains(root.getUUID("JourneyId"))) {
                    if(!root.getBoolean("UnderworldBookWitness"))root.putLong("RequirementsRevision",root.getLong("RequirementsRevision")+1);
                    root.putInt("Witnesses",root.getInt("Witnesses")|TravailAspect.UNDERWORLD.mask());
                    root.putBoolean("UnderworldBookWitness",true);
                }
            }
        }
    }
    public void recordSize() {
        com.thelongtravail.data.HotPathMetrics.Counter.STORAGE_SPENT.set(spent.size());
        com.thelongtravail.data.HotPathMetrics.Counter.STORAGE_WITNESSES.set(witnesses.size());
        com.thelongtravail.data.HotPathMetrics.Counter.STORAGE_GRAVES.set(graves.size());
    }
    public static UnderworldLedger load(CompoundTag n) {
        UnderworldLedger d=new UnderworldLedger();
        for(Tag t:n.getList("Spent",Tag.TAG_STRING)) d.spent.add(UUID.fromString(t.getAsString()));
        for(Tag t:n.getList("Witnesses",Tag.TAG_STRING)) d.witnesses.add(UUID.fromString(t.getAsString()));
        for(Tag t:n.getList("Graves",Tag.TAG_STRING)) d.graves.add(t.getAsString());
        for(Tag t:n.getList("Deaths",Tag.TAG_COMPOUND)) { CompoundTag c=(CompoundTag)t; d.deaths.put(c.getUUID("Player"),c); }
        return d;
    }
    @Override public CompoundTag save(CompoundTag n) {
        ListTag a=new ListTag(),b=new ListTag(),c=new ListTag(),g=new ListTag();
        spent.forEach(id->a.add(StringTag.valueOf(id.toString()))); witnesses.forEach(id->b.add(StringTag.valueOf(id.toString())));
        deaths.values().forEach(v->c.add(v.copy())); graves.forEach(v->g.add(StringTag.valueOf(v)));
        n.put("Spent",a);n.put("Witnesses",b);n.put("Deaths",c);n.put("Graves",g);return n;
    }
}
