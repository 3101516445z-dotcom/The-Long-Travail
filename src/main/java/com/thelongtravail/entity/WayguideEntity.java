package com.thelongtravail.entity;

import com.thelongtravail.data.WayguideGeometry;
import com.thelongtravail.data.WayguideReturns;
import com.thelongtravail.data.WayguideSearch;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import java.util.UUID;

// 搜索时跟随头顶；成功后飞向目标，失败则返还唯一一份托管物品。
public final class WayguideEntity extends Entity implements ItemSupplier {
    private static final EntityDataAccessor<ItemStack> ITEM = SynchedEntityData.defineId(WayguideEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> SEARCHING = SynchedEntityData.defineId(WayguideEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(WayguideEntity.class, EntityDataSerializers.INT);
    private Vec3 destination = Vec3.ZERO, lerpTarget = Vec3.ZERO;
    private int age, lerpSteps;
    private boolean returnItem, recoverSearch, escrowed;
    private UUID owner;
    private ServerPlayer searchOwner;

    public WayguideEntity(EntityType<? extends WayguideEntity> type, Level level) { super(type, level); noPhysics = true; }
    public void beginSearch(ServerPlayer player, ItemStack item) {
        searchOwner = player; owner = player.getUUID(); age = 0;
        ItemStack carried = item.copy(); carried.setCount(1); entityData.set(ITEM, carried);
        entityData.set(OWNER, player.getId()); entityData.set(SEARCHING, true);
        setPos(player.getX(), player.getY() + 3, player.getZ());
        // addFreshEntity 成功且手持物品扣除后，由调用方确认托管。
        returnItem = false;
    }
    public void reserve(boolean survival) {
        returnItem = survival;
        if (survival && level() instanceof ServerLevel serverLevel) {
            WayguideReturns.get(serverLevel.getServer()).hold(getUUID(), owner, getItem()); escrowed = true;
        }
    }
    public boolean searching() { return entityData.get(SEARCHING); }
    public void launch(ServerPlayer player, BlockPos target) {
        if (escrowed) { WayguideReturns.get(player.server).release(getUUID()); escrowed = false; }
        entityData.set(SEARCHING, false); age = 0; searchOwner = null;
        destination = WayguideGeometry.destination(player.position(), target);
    }
    public void guide(ServerPlayer player, BlockPos target) { guide(player, target, new ItemStack(ModRegistry.WAYGUIDE.get())); }
    public void guide(ServerPlayer player, BlockPos target, ItemStack item) {
        beginSearch(player, item); reserve(!player.getAbilities().instabuild); launch(player, target);
    }
    public void refund(ServerPlayer player) {
        if (returnItem && owner != null && level() instanceof ServerLevel serverLevel) {
            var returns = WayguideReturns.get(serverLevel.getServer());
            if (escrowed) returns.refund(getUUID()); else returns.enqueue(getUUID(), owner, getItem());
            returnItem = false; escrowed = false;
            try { if (player != null) returns.deliver(player); }
            catch (RuntimeException error) { com.thelongtravail.TheLongTravail.LOGGER.error("Wayguide return deferred after delivery failure", error); }
            finally { discard(); }
            return;
        }
        discard();
    }
    @Override protected void defineSynchedData() {
        entityData.define(ITEM, new ItemStack(ModRegistry.WAYGUIDE.get()));
        entityData.define(SEARCHING, false); entityData.define(OWNER, -1);
    }
    @Override public ItemStack getItem() { return entityData.get(ITEM); }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps, boolean teleport) {
        lerpTarget = new Vec3(x, y, z); lerpSteps = Math.max(1, steps);
    }
    @Override public void tick() {
        baseTick();
        if (level().isClientSide) {
            Entity player = searching() ? level().getEntity(entityData.get(OWNER)) : null;
            if (player != null) {
                // 起伏在客户端计算，服务器只同步跟随位置。
                setPos(player.getX(), player.getY() + 3 + Math.sin(tickCount * 0.22) * 0.22, player.getZ());
            } else if (lerpSteps > 0) { setPos(position().lerp(lerpTarget, 1.0 / lerpSteps)); lerpSteps--; }
            if (tickCount % 2 == 0) level().addParticle(ParticleTypes.ENCHANT, getX(), getY(), getZ(), 0, 0.05, 0);
            return;
        }
        if (searching()) {
            if (recoverSearch || (returnItem && !WayguideSearch.ownsSearch(owner, getUUID()))) {
                refund(searchOwner != null ? searchOwner : ((ServerLevel) level()).getServer().getPlayerList().getPlayer(owner)); return;
            }
            if (searchOwner != null) setPos(searchOwner.getX(), searchOwner.getY() + 3, searchOwner.getZ());
            return;
        }
        age++;
        Vec3 motion = destination.subtract(position()).scale(0.16);
        setDeltaMovement(motion); setPos(position().add(motion));
        int durationTicks = com.thelongtravail.config.TravailConfig.WAYGUIDE_GUIDE_DURATION_SECONDS.get() * 20;
        if (age >= durationTicks && (age - durationTicks) % 20 == 0) {
            if (!returnItem) { discard(); return; }
            var drop = new ItemEntity(level(), getX(), getY(), getZ(), getItem().copy());
            drop.setDefaultPickUpDelay();
            if (level().addFreshEntity(drop)) { returnItem = false; discard(); }
        }
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("GuideX", destination.x); tag.putDouble("GuideY", destination.y); tag.putDouble("GuideZ", destination.z);
        tag.putInt("GuideAge", age); tag.putBoolean("ReturnItem", returnItem); tag.putBoolean("Searching", searching());
        tag.putBoolean("Escrowed", escrowed);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.put("Item", getItem().save(new CompoundTag()));
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        destination = new Vec3(tag.getDouble("GuideX"), tag.getDouble("GuideY"), tag.getDouble("GuideZ"));
        age = tag.getInt("GuideAge"); returnItem = tag.getBoolean("ReturnItem");
        escrowed = tag.getBoolean("Escrowed");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        entityData.set(SEARCHING, tag.getBoolean("Searching")); recoverSearch = searching();
        ItemStack item = ItemStack.of(tag.getCompound("Item"));
        if (item.is(ModRegistry.WAYGUIDE.get())) { item.setCount(1); entityData.set(ITEM, item); }
    }
}
