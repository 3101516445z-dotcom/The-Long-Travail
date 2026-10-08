package com.thelongtravail.underworld;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.*;
import java.util.*;

// 按脚部坐标的欧氏距离搜索；每格用实际碰撞面的坐标划分连续空间，而非只检查方块中心。
public final class RevivalPosition {
    private record Cell(BlockPos pos,double distance) {}
    private final ServerLevel level;
    private final Vec3 origin;
    private final double half,height;
    private final PriorityQueue<Cell> queue=new PriorityQueue<>(Comparator.comparingDouble(Cell::distance)
            .thenComparingLong(c->c.pos.asLong()));
    private final Set<BlockPos> seen=new HashSet<>();
    private Vec3 best;
    private BlockPos active;
    private double[] cx,cy,cz;
    private int ix,iy,iz;
    public RevivalPosition(ServerLevel level,Vec3 origin,double width,double height) {
        this.level=level;this.origin=origin;this.half=width/2;this.height=height;
        if(clear(origin))best=origin;
        else offer(BlockPos.containing(origin));
    }
    private AABB body(Vec3 p) {return new AABB(p.x-half,p.y,p.z-half,p.x+half,p.y+height,p.z+half).deflate(1E-7);}
    private void load(AABB box) {
        for(int x=net.minecraft.util.Mth.floor(box.minX)>>4;x<=net.minecraft.util.Mth.floor(box.maxX)>>4;x++)
            for(int z=net.minecraft.util.Mth.floor(box.minZ)>>4;z<=net.minecraft.util.Mth.floor(box.maxZ)>>4;z++)level.getChunk(x,z);
    }
    public boolean clear(Vec3 p) {
        if(p.y<level.getMinBuildHeight())return false;
        AABB box=body(p);load(box);return !level.getBlockCollisions(null,box).iterator().hasNext();
    }
    private static double clamp(double v,double lo,double hi){return Math.max(lo,Math.min(hi,v));}
    private void offer(BlockPos p) {
        if(p.getY()+1<level.getMinBuildHeight()||!seen.add(p))return;
        double x=clamp(origin.x,p.getX(),p.getX()+1),y=clamp(origin.y,p.getY(),p.getY()+1),z=clamp(origin.z,p.getZ(),p.getZ()+1);
        queue.add(new Cell(p,origin.distanceToSqr(x,y,z)));
    }
    // 北、东、南、西、上、下；斜向先按其包含的最高优先方向，再按坐标稳定排序。
    public static int direction(Vec3 v) {return v.z<0?0:v.x>0?1:v.z>0?2:v.x<0?3:v.y>0?4:5;}
    public int compare(Vec3 a,Vec3 b) {
        double delta=origin.distanceToSqr(a)-origin.distanceToSqr(b);
        if(Math.abs(delta)>1E-9)return delta<0?-1:1;
        int c=Integer.compare(direction(a.subtract(origin)),direction(b.subtract(origin)));
        if(c!=0)return c;
        c=Double.compare(a.z,b.z);if(c!=0)return c;
        c=Double.compare(b.x,a.x);return c!=0?c:Double.compare(b.y,a.y);
    }
    public boolean step(int budget) {
        if(best!=null&&best.equals(origin))return true;
        while(budget>0&&(active!=null||!queue.isEmpty())) {
            if(active!=null) {
                Vec3 v=new Vec3(cx[ix],cy[iy],cz[iz]);budget--;
                if((best==null||compare(v,best)<0)&&clear(v))best=v;
                if(++iz==cz.length){iz=0;if(++iy==cy.length){iy=0;if(++ix==cx.length){
                    BlockPos p=active;active=null;offer(p.north());offer(p.east());offer(p.south());offer(p.west());offer(p.above());offer(p.below());
                }}}
                continue;
            }
            if(best!=null&&queue.peek().distance>origin.distanceToSqr(best)+1E-9)return true;
            Cell c=queue.remove();BlockPos p=c.pos;
            // 分批加载必要区块，不把未加载区块误认为空气。
            level.getChunk(p.getX()>>4,p.getZ()>>4);
            TreeSet<Double> xs=new TreeSet<>(),ys=new TreeSet<>(),zs=new TreeSet<>();
            add(xs,origin.x,p.getX());add(ys,origin.y,p.getY());add(zs,origin.z,p.getZ());
            AABB region=new AABB(p.getX()-half,p.getY(),p.getZ()-half,p.getX()+1+half,p.getY()+1+height,p.getZ()+1+half);
            load(region);
            for(var shape:level.getBlockCollisions(null,region))for(AABB box:shape.toAabbs()) {
                planes(xs,box.minX-half,box.maxX+half,p.getX());
                planes(ys,box.minY-height,box.maxY,p.getY());
                planes(zs,box.minZ-half,box.maxZ+half,p.getZ());
            }
            active=p;cx=xs.stream().mapToDouble(Double::doubleValue).toArray();cy=ys.stream().mapToDouble(Double::doubleValue).toArray();cz=zs.stream().mapToDouble(Double::doubleValue).toArray();ix=iy=iz=0;
        }
        return active==null&&(queue.isEmpty()||best!=null&&queue.peek().distance>origin.distanceToSqr(best)+1E-9);
    }
    private static void add(Set<Double>s,double v,int lo){s.add((double)lo);s.add(lo+1D);s.add(clamp(v,lo,lo+1));}
    private static void planes(Set<Double>s,double a,double b,int lo){if(a>=lo&&a<=lo+1)s.add(a);if(b>=lo&&b<=lo+1)s.add(b);}
    public Vec3 result(){return best;}
}
