package com.thelongtravail.client;
import com.thelongtravail.boundless.*;
import com.thelongtravail.network.TimeStopPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.*;
public final class TimeStopClient {
    public static volatile List<TimeStopPacket.View> fields=List.of();private static volatile net.minecraft.resources.ResourceLocation dimension;
    public static void setup(){TimeStopPacket.receiver=p->{dimension=p.dimension();fields=p.fields();};TimeStopPacket.clientFrozen=TimeStopClient::frozen;TimeStopPacket.clientClip=TimeStopClient::clip;TimeStopPacket.clientBlock=(l,p)->valid(l)&&fields.stream().anyMatch(f->f.contains(Vec3.atCenterOf(p)));}
    public static boolean valid(Level l){return l!=null&&dimension!=null&&dimension.equals(l.dimension().location());}
    public static boolean frozen(Entity e){return valid(e.level())&&fields.stream().anyMatch(f->f.contains(e.position())&&!TimeStopExemptions.exempt(e,f.allies()));}
    private static Vec3 clip(Entity e,Vec3 delta){if(!valid(e.level()))return delta;double t=1;for(var f:fields)if(!TimeStopExemptions.exempt(e,f.allies()))t=Math.min(t,TimeStopGeometry.entryInside(e.position(),delta,f.center(),f.radius()));return t<1?delta.scale(t):delta;}
    public static void clear(){fields=List.of();dimension=null;}
    public static boolean boundaryDanger(Vec3 p){var player=Minecraft.getInstance().player;return player!=null&&fields.stream().anyMatch(f->!f.allies().contains(player.getUUID())&&f.contains(p));}
    private TimeStopClient(){}
}
