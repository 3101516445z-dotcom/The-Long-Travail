package com.thelongtravail.underworld;

import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import java.util.*;
import java.util.function.Supplier;

// 深层扫描共用处理额度，复活和反序列化等须立即完成的校正仍由 UnderworldStorage 同步执行。
public final class UnderworldMaintenance {
    private static final Map<UUID,Deque<NbtReconcileCursor>> SCANS=new LinkedHashMap<>();
    private static final Deque<UUID> ORDER=new ArrayDeque<>();
    public static void clear(){SCANS.clear();ORDER.clear();}
    public static void forget(UUID id){SCANS.remove(id);ORDER.remove(id);}
    private static void item(UnderworldLedger ledger,Deque<NbtReconcileCursor> work,Supplier<ItemStack> source){
        ItemStack stack=source.get();
        if(ledger.consumed(stack)){stack.setCount(0);return;}
        ledger.reconcileNode(stack.getTag());
        if(work!=null)work.add(new NbtReconcileCursor(()->source.get().getTag()));
    }
    public static void request(ServerPlayer player){
        var ledger=UnderworldLedger.get(player.server);
        if(!ledger.hasChanges())return;
        // 顶层资格每秒仍立即核对，深层遍历不会因下一次请求而被反复重置。
        var work=SCANS.containsKey(player.getUUID())?null:new ArrayDeque<NbtReconcileCursor>();
        for(int i=0;i<player.getInventory().getContainerSize();i++){
            int slot=i;item(ledger,work,()->player.getInventory().getItem(slot));
        }
        CuriosApi.getCuriosInventory(player).ifPresent(inv->inv.getCurios().forEach((id,handler)->{
            for(boolean cosmetic:new boolean[]{false,true}){
                var items=cosmetic?handler.getCosmeticStacks():handler.getStacks();
                for(int i=0;i<items.getSlots();i++){
                    int slot=i;
                    item(ledger,work,()->CuriosApi.getCuriosInventory(player).map(current->current.getStacksHandler(id).map(h->{
                        var stacks=cosmetic?h.getCosmeticStacks():h.getStacks();
                        return slot<stacks.getSlots()?stacks.getStackInSlot(slot):ItemStack.EMPTY;
                    }).orElse(ItemStack.EMPTY)).orElse(ItemStack.EMPTY));
                }
            }
        }));
        if(work==null)return;
        work.add(new NbtReconcileCursor(player::getPersistentData));
        SCANS.put(player.getUUID(),work);ORDER.addLast(player.getUUID());
    }
    public static void tick(MinecraftServer server){
        if(ORDER.isEmpty())return;
        int budget=2048;
        long deadline=System.nanoTime()+1_000_000L;
        var ledger=UnderworldLedger.get(server);
        while(budget>0&&!ORDER.isEmpty()&&System.nanoTime()<deadline){
            UUID id=ORDER.removeFirst();var work=SCANS.get(id);
            var player=server.getPlayerList().getPlayer(id);
            if(work==null||player==null||player.hasDisconnected()){SCANS.remove(id);continue;}
            for(int n=0;n<64&&budget>0&&!work.isEmpty()&&System.nanoTime()<deadline;n++,budget--){
                // 根节点也轮换：持续替换的物品不能阻塞其他槽位和玩家持久数据。
                var cursor=work.removeFirst();
                if(cursor.step(ledger))work.addLast(cursor);
            }
            if(work.isEmpty())SCANS.remove(id);else ORDER.addLast(id);
        }
    }
    private UnderworldMaintenance(){}
}
