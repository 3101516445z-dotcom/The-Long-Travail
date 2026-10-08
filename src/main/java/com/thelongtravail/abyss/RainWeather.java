package com.thelongtravail.abyss;

import com.thelongtravail.config.RainConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.ServerLevelData;

// 每个主世界统一保存天气托管状态，玩家仅保存冷却，不持有天气快照。
public final class RainWeather extends SavedData {
    private int remaining, clearTime, rainTime, thunderTime;
    private boolean raining, thundering;
    private float rainLevel, thunderLevel;
    public static RainWeather get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(RainWeather::load, RainWeather::new, "travail_rain");
    }
    public static RainWeather load(CompoundTag tag) {
        RainWeather w = new RainWeather();
        w.remaining = Math.max(0, tag.getInt("Remaining"));
        w.clearTime = tag.getInt("Clear"); w.rainTime = tag.getInt("Rain"); w.thunderTime = tag.getInt("Thunder");
        w.raining = tag.getBoolean("Raining"); w.thundering = tag.getBoolean("Thundering");
        w.rainLevel = tag.getFloat("RainLevel"); w.thunderLevel = tag.getFloat("ThunderLevel");
        return w;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        tag.putInt("Remaining", remaining); tag.putInt("Clear", clearTime); tag.putInt("Rain", rainTime); tag.putInt("Thunder", thunderTime);
        tag.putBoolean("Raining", raining); tag.putBoolean("Thundering", thundering);
        tag.putFloat("RainLevel", rainLevel); tag.putFloat("ThunderLevel", thunderLevel); return tag;
    }
    public int remaining() { return remaining; }
    public boolean active() { return remaining > 0; }
    public boolean naturalRain(ServerLevel level) { return active() ? raining : level.getLevelData().isRaining(); }
    public void start(ServerLevel level, int ticks) {
        if (!level.dimension().equals(Level.OVERWORLD)) throw new IllegalArgumentException("Overworld weather only");
        ServerLevelData d = (ServerLevelData)level.getLevelData();
        if (!active()) {
            clearTime = d.getClearWeatherTime(); rainTime = d.getRainTime(); thunderTime = d.getThunderTime();
            raining = d.isRaining(); thundering = d.isThundering();
            rainLevel = level.getRainLevel(1); thunderLevel = rawThunderLevel(level);
        }
        remaining = Math.max(1, ticks); force(level); setDirty();
        RainEvents.syncWorld(level);
    }
    private void force(ServerLevel level) {
        ServerLevelData d = (ServerLevelData)level.getLevelData();
        d.setClearWeatherTime(0); d.setRaining(true); d.setThundering(false);
        d.setRainTime(remaining); d.setThunderTime(remaining);
    }
    // 仅暂停自然天气计时，天气强度仍由原版更新和同步。
    public boolean advance(ServerLevel level) {
        if (!active()) return false;
        if (!RainConfig.SKILL_ENABLED.get()) { restore(level); return true; }
        if (--remaining <= 0) restore(level); else { force(level); setDirty(); }
        return true;
    }
    public void restore(ServerLevel level) {
        ServerLevelData d = (ServerLevelData)level.getLevelData();
        remaining = 0;
        d.setClearWeatherTime(clearTime); d.setRainTime(rainTime); d.setThunderTime(thunderTime);
        d.setRaining(raining); d.setThundering(thundering);
        // 保留当前视觉强度，由原版按恢复后的天气标志逐渐过渡。
        setDirty(); RainEvents.syncWorld(level);
    }
    public void discard(ServerLevel level) {
        remaining = 0; setDirty(); RainEvents.syncWorld(level);
    }
    public static boolean active(Level level) {
        if (!level.dimension().equals(Level.OVERWORLD)) return false;
        return level instanceof ServerLevel server ? get(server).active() : RainState.clientWeather;
    }
    public static float rawThunderLevel(Level level) {
        return ((com.thelongtravail.mixin.RainLevelAccessor) level).travail$rawThunderLevel();
    }
    public static boolean dry(Level level) { return active(level) && (level.isClientSide ? RainState.clientDry : RainConfig.DRY_RAIN.get()); }
    public static boolean snow(Level level) { return active(level) && (level.isClientSide ? RainState.clientSnow : RainConfig.SNOW_RAIN.get()); }
}
