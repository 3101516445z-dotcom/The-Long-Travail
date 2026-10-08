package com.thelongtravail.boundless;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ChunkPos;
import java.util.*;
public final class TimeStopGeometry {
    public static Set<Long> chunks(Vec3 c,double r){
        Set<Long> out=new HashSet<>();
        int x0=(int)Math.floor((c.x-r)/16),x1=(int)Math.floor((c.x+r)/16),z0=(int)Math.floor((c.z-r)/16),z1=(int)Math.floor((c.z+r)/16);
        for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++){
            double dx=Math.max(Math.max(x*16.0-c.x,c.x-(x+1)*16.0),0),dz=Math.max(Math.max(z*16.0-c.z,c.z-(z+1)*16.0),0);
            if(dx*dx+dz*dz<=r*r)out.add(ChunkPos.asLong(x,z));
        }return out;
    }
    public static boolean overlaps(Vec3 a,double ar,Vec3 b,double br){return a.distanceToSqr(b)<=(ar+br)*(ar+br);}
    // 最早进入球面的线段参数；未进入返回1。
    public static double entry(Vec3 from,Vec3 delta,Vec3 c,double r){
        double x=from.x-c.x,y=from.y-c.y,z=from.z-c.z,a=delta.lengthSqr();
        if(x*x+y*y+z*z<=r*r)return 0;if(a<1e-12)return 1;
        // |q×delta|² 形式避免远距离下 b² 与 4ac 两个巨大近等数相减。
        double cx=y*delta.z-z*delta.y,cy=z*delta.x-x*delta.z,cz=x*delta.y-y*delta.x;
        double disc=a*r*r-(cx*cx+cy*cy+cz*cz);
        if(disc<0)return 1;
        double t=(-(x*delta.x+y*delta.y+z*delta.z)-Math.sqrt(disc))/a;
        return t>=0&&t<=1?t:1;
    }
    // 进入补偿按世界距离限幅，且不越过最近点；切线接触不会被推到球外。
    public static double entryInside(Vec3 from,Vec3 delta,Vec3 center,double radius){
        double entry=entry(from,delta,center,radius);
        if(entry<=0||entry>=1)return entry;
        double lengthSquared=delta.lengthSqr();
        double closest=-((from.x-center.x)*delta.x+(from.y-center.y)*delta.y+(from.z-center.z)*delta.z)/lengthSquared;
        double inset=1e-7/Math.max(1,Math.sqrt(lengthSquared));
        return Math.min(1,Math.min(entry+inset,Math.max(entry,closest)));
    }
    private TimeStopGeometry(){}
}
