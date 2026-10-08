package com.thelongtravail.valley;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.registries.*;
import net.minecraftforge.eventbus.api.IEventBus;
import java.util.*;

// 旧版真实光源的兼容清理器；不再创建或续期。已加载旧光源在服务端下一刻移除。
public final class LanternLight {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,"the_long_travail");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,"the_long_travail");
    public static final RegistryObject<Block> BLOCK=BLOCKS.register("lantern_light",Light::new);
    public static final RegistryObject<BlockEntityType<Data>> TYPE=ENTITIES.register("lantern_light",()->BlockEntityType.Builder.of(Data::new,BLOCK.get()).build(null));
    private static final Set<Data> LOADED=Collections.newSetFromMap(new IdentityHashMap<>());
    private record Change(Level level,BlockPos pos,BlockState before,BlockState after){}
    private static final ThreadLocal<Change> CHANGE=new ThreadLocal<>();
    public static void register(IEventBus bus){BLOCKS.register(bus);ENTITIES.register(bus);}
    public static boolean permitted(Level level,BlockPos pos,BlockState after){
        Change c=CHANGE.get();return c!=null&&c.level==level&&c.pos.equals(pos)&&c.after==after&&level.getBlockState(pos)==c.before;
    }
    private static boolean change(ServerLevel level,BlockPos pos,BlockState after){
        BlockState before=level.getBlockState(pos);
        if(!(before.is(BLOCK.get())&&after.isAir()))return false;
        Change old=CHANGE.get();CHANGE.set(new Change(level,pos.immutable(),before,after));
        try{return level.setBlock(pos,after,Block.UPDATE_ALL);}finally{if(old==null)CHANGE.remove();else CHANGE.set(old);}
    }
    public static final class Light extends Block implements EntityBlock {
        Light(){super(BlockBehaviour.Properties.of().noCollission().noOcclusion().replaceable().noLootTable().lightLevel(s->15).pushReaction(PushReaction.DESTROY));}
        @Override public RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
        @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.empty();}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Data(p,s);}
        @Override public boolean propagatesSkylightDown(BlockState s,BlockGetter l,BlockPos p){return true;}
    }
    public static final class Data extends BlockEntity {
        long expires;
        public Data(BlockPos p,BlockState s){super(TYPE.get(),p,s);}
        @Override public void onLoad(){super.onLoad();if(level instanceof ServerLevel)LOADED.add(this);}
        @Override public void setRemoved(){LOADED.remove(this);super.setRemoved();}
        @Override public void onChunkUnloaded(){LOADED.remove(this);super.onChunkUnloaded();}
        @Override public void load(CompoundTag n){super.load(n);expires=n.getLong("Expires");}
        @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);n.putLong("Expires",expires);}
    }
    private static void remove(Data d){
        if(d.getLevel() instanceof ServerLevel level&&level.hasChunkAt(d.getBlockPos())&&level.getBlockEntity(d.getBlockPos())==d)
            change(level,d.getBlockPos(),Blocks.AIR.defaultBlockState());
    }
    public static void tick(){
        for(Data d:List.copyOf(LOADED)) {
            if(d.isRemoved())LOADED.remove(d);else remove(d);
        }
    }
    public static void stop(){for(Data d:List.copyOf(LOADED))remove(d);LOADED.clear();}
    private LanternLight(){}
}
