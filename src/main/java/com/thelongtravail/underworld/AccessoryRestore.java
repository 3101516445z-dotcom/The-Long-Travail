package com.thelongtravail.underworld;

import net.minecraft.nbt.*;
import net.minecraft.world.entity.LivingEntity;
import top.theillusivec4.curios.api.SlotContext;

// Curios 在提交新槽位表前校验物品，此时须根据待恢复的完整数据判断前置条件。
public final class AccessoryRestore implements AutoCloseable {
    private static final ThreadLocal<AccessoryRestore> CURRENT=new ThreadLocal<>();
    private final AccessoryRestore previous;
    private final LivingEntity wearer;
    private boolean diary;
    private int ring=Integer.MAX_VALUE,book=Integer.MAX_VALUE;
    public AccessoryRestore(LivingEntity wearer,Tag raw) {
        this.wearer=wearer;previous=CURRENT.get();
        if(raw instanceof CompoundTag root)for(Tag entry:root.getList("Curios",Tag.TAG_COMPOUND)) {
            CompoundTag c=(CompoundTag)entry;String slot=c.getString("Identifier");
            for(Tag item:c.getCompound("StacksHandler").getCompound("Stacks").getList("Items",Tag.TAG_COMPOUND)) {
                CompoundTag stack=(CompoundTag)item;if(stack.getByte("Count")<=0)continue;
                String id=stack.getString("id");
                if(slot.equals("travel_diary")&&id.equals("the_long_travail:the_long_travail"))diary=true;
                if(slot.equals("ring")&&id.equals("the_long_travail:a_thousand_years_later"))ring=Math.min(ring,stack.getInt("Slot"));
                if(slot.equals("charm")&&id.equals("the_long_travail:book_of_the_dead"))book=Math.min(book,stack.getInt("Slot"));
            }
        }
        CURRENT.set(this);
    }
    public static Boolean allowed(SlotContext c,boolean ring) {
        var s=CURRENT.get();if(s==null||s.wearer!=c.entity())return null;
        return s.diary&&c.index()==(ring?s.ring:s.book);
    }
    @Override public void close(){if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
}
