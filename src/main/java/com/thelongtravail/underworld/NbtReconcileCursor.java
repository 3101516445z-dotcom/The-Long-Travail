package com.thelongtravail.underworld;

import net.minecraft.nbt.*;
import java.util.*;
import java.util.function.Supplier;

// 在主线程分步遍历，每一步都核对祖先节点的引用，避免外部替换后继续修改已脱离物品的旧标签。
public final class NbtReconcileCursor {
    private final Supplier<Tag> root;
    private final Deque<Frame> stack = new ArrayDeque<>();
    private boolean started;
    private static final class Frame {
        final Tag tag;
        final Frame parent;
        final String key;
        final int index, depth;
        boolean visited;
        Iterator<String> keys;
        int nextIndex;
        Frame(Tag tag, Frame parent, String key, int index) {
            this.tag=tag;this.parent=parent;this.key=key;this.index=index;
            depth=parent==null?0:parent.depth+1;
        }
        boolean attached(Tag root) {
            if(parent==null)return tag==root;
            if(!parent.attached(root))return false;
            if(parent.tag instanceof CompoundTag c)return c.get(key)==tag;
            return parent.tag instanceof ListTag l&&index<l.size()&&l.get(index)==tag;
        }
    }
    public NbtReconcileCursor(Supplier<Tag> root){this.root=root;}
    // 返回是否仍有工作；一次调用最多访问或推进一个节点。
    public boolean step(UnderworldLedger ledger){
        com.thelongtravail.data.HotPathMetrics.Counter.NBT_STEPS.add(1);
        Tag current=root.get();
        if(!started){started=true;if(current!=null)stack.push(new Frame(current,null,null,0));}
        if(stack.isEmpty())return false;
        Frame f=stack.peek();
        if(!f.attached(current)){stack.clear();started=false;return true;}
        // 数字、字符串、数组及只含标量的同质列表不可能包含物品身份或日记节点。
        if(!(f.tag instanceof CompoundTag)&&(!(f.tag instanceof ListTag list)
                ||list.getElementType()!=Tag.TAG_COMPOUND&&list.getElementType()!=Tag.TAG_LIST)){
            stack.pop();return !stack.isEmpty();
        }
        if(!f.visited){
            ledger.reconcileNode(f.tag);f.visited=true;
            if(f.tag instanceof CompoundTag c)f.keys=c.getAllKeys().iterator();
            return true;
        }
        if(f.depth>=UnderworldLedger.MAX_NBT_DEPTH){stack.pop();return !stack.isEmpty();}
        try {
            if(f.tag instanceof CompoundTag c&&f.keys.hasNext()){
                String key=f.keys.next();Tag child=c.get(key);
                if(child instanceof CompoundTag||child instanceof ListTag)stack.push(new Frame(child,f,key,0));
            }else if(f.tag instanceof ListTag l&&f.nextIndex<l.size()){
                int index=f.nextIndex++;stack.push(new Frame(l.get(index),f,null,index));
            }else stack.pop();
        }catch(ConcurrentModificationException changed){stack.clear();started=false;}
        return !stack.isEmpty()||!started;
    }
}
