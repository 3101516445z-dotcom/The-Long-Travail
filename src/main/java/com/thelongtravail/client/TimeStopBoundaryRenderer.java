package com.thelongtravail.client;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
// 缓存单位球网格以绘制双面球面或线框边界，显示档位不影响玩法判定。
@Mod.EventBusSubscriber(modid="the_long_travail",value=Dist.CLIENT)
public final class TimeStopBoundaryRenderer extends RenderType {
    private TimeStopBoundaryRenderer(){super("unused",DefaultVertexFormat.POSITION_COLOR_NORMAL,VertexFormat.Mode.LINES,256,false,false,()->{},()->{});}
    private static final int EXEMPT_COLOR=0x36E6C5,DANGER_COLOR=0xFF5A24;
    private static final RenderType BOUNDARY=create("travail_time_boundary",DefaultVertexFormat.POSITION_COLOR_NORMAL,VertexFormat.Mode.LINES,65536,false,false,
            CompositeState.builder().setShaderState(RENDERTYPE_LINES_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).setDepthTestState(LEQUAL_DEPTH_TEST).setLineState(new LineStateShard(OptionalDouble.of(1.5))).createCompositeState(false));
    private static final RenderType GLASS=create("travail_time_glass",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,262144,false,true,
            CompositeState.builder().setShaderState(new ShaderStateShard(GameRenderer::getPositionColorShader)).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(false));
    private static final Vec3[] SPHERE=sphere();
    private record MeshKey(long id,Vec3 center,double radius) {}
    private static final Map<MeshKey,Vec3[]> WORLD_MESHES=new HashMap<>();
    private static Vec3[] sphere(){
        // 共用相邻经纬顶点，不留缝隙；两极使用退化四边形闭合。
        int rows=32,columns=64;Vec3[] vertices=new Vec3[rows*columns*4];int n=0;
        for(int row=0;row<rows;row++)for(int col=0;col<columns;col++){
            double a=Math.PI*row/rows,b=Math.PI*(row+1)/rows;
            double c=2*Math.PI*col/columns,d=2*Math.PI*((col+1)%columns)/columns;
            vertices[n++]=point(a,c);vertices[n++]=point(b,c);vertices[n++]=point(b,d);vertices[n++]=point(a,d);
        }
        return vertices;
    }
    private static Vec3[] worldMesh(MeshKey key){
        return WORLD_MESHES.computeIfAbsent(key,k->{Vec3[] mesh=new Vec3[SPHERE.length];
            for(int i=0;i<mesh.length;i++)mesh[i]=k.center.add(SPHERE[i].scale(k.radius));return mesh;});
    }
    private static final List<Vec3[]> SEGMENTS=new ArrayList<>();
    static {int n=96;for(int latitude=1;latitude<12;latitude++){double a=Math.PI*latitude/12;for(int i=0;i<n;i++)SEGMENTS.add(new Vec3[]{point(a,2*Math.PI*i/n),point(a,2*Math.PI*(i+1)/n)});}for(int longitude=0;longitude<12;longitude++)for(int i=0;i<n;i++)SEGMENTS.add(new Vec3[]{point(Math.PI*i/n,2*Math.PI*longitude/12),point(Math.PI*(i+1)/n,2*Math.PI*longitude/12)});}
    private static Vec3 point(double a,double b){return new Vec3(Math.sin(a)*Math.cos(b),Math.cos(a),Math.sin(a)*Math.sin(b));}
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        var mc=Minecraft.getInstance();if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        if(mc.player==null||!TimeStopClient.valid(mc.level)){WORLD_MESHES.clear();return;}
        WORLD_MESHES.keySet().removeIf(k->TimeStopClient.fields.stream().noneMatch(f->f.id()==k.id&&f.center().equals(k.center)&&f.radius()==k.radius));
        var buffers=mc.renderBuffers().bufferSource();var stack=e.getPoseStack();stack.pushPose();Vec3 camera=e.getCamera().getPosition();stack.translate(-camera.x,-camera.y,-camera.z);
        var pose=stack.last();int level=com.thelongtravail.config.TravailClientConfig.TIME_STOP_BOUNDARY_LEVEL.get();
        var type=level==2?BOUNDARY:GLASS;var out=buffers.getBuffer(type);
        // 友方区域先画、危险区域后画；相交位置按危险色绘制，不写入深度缓冲。
        var sorted=new ArrayList<>(TimeStopClient.fields);sorted.sort(Comparator.comparing(f->!f.allies().contains(mc.player.getUUID())));
        for(var f:sorted){if(!e.getFrustum().isVisible(new AABB(f.center().x-f.radius(),f.center().y-f.radius(),f.center().z-f.radius(),f.center().x+f.radius(),f.center().y+f.radius(),f.center().z+f.radius())))continue;
            if(level==1){glass(out,pose,f,camera,mc.player.getUUID());continue;}
            for(Vec3[] s:SEGMENTS){Vec3 a=f.center().add(s[0].scale(f.radius())),b=f.center().add(s[1].scale(f.radius()));boolean danger=!f.allies().contains(mc.player.getUUID())||TimeStopClient.boundaryDanger(a.add(b).scale(.5));int color=danger?DANGER_COLOR:EXEMPT_COLOR;Vec3 normal=b.subtract(a).normalize();vertex(out,pose,a,normal,color);vertex(out,pose,b,normal,color);}
        }buffers.endBatch(type);stack.popPose();
    }
    private static void glass(VertexConsumer out,PoseStack.Pose pose,com.thelongtravail.network.TimeStopPacket.View field,Vec3 camera,UUID viewer){
        Vec3[] mesh=worldMesh(new MeshKey(field.id(),field.center(),field.radius()));
        boolean hostile=!field.allies().contains(viewer);
        for(int i=0;i<mesh.length;i++){
            Vec3 p=mesh[i],normal=SPHERE[i];boolean danger=hostile||TimeStopClient.boundaryDanger(p);
            int color=danger?DANGER_COLOR:EXEMPT_COLOR;
            double dx=camera.x-p.x,dy=camera.y-p.y,dz=camera.z-p.z;
            double distance=Math.sqrt(dx*dx+dy*dy+dz*dz);
            double facing=distance<1e-8?1:Math.min(1,Math.abs((dx*normal.x+dy*normal.y+dz*normal.z)/distance));
            // 正面通透、掠射轮廓更明显；使用共享顶点法线使表面连续，球内也能看见。
            double rim=Math.pow(1-facing,2);
            int alpha=(int)((danger?25:15)+rim*(danger?185:175));
            double highlight=Math.pow(Math.max(0,normal.x*.3+normal.y*.8+normal.z*.52),24)*.12;
            flat(out,pose,p,tint(color,highlight),alpha);
        }
    }
    private static int tint(int color,double white){int r=(color>>16)&255,g=(color>>8)&255,b=color&255;return ((int)(r+(255-r)*white)<<16)|((int)(g+(255-g)*white)<<8)|(int)(b+(255-b)*white);}
    private static void flat(VertexConsumer out,PoseStack.Pose pose,Vec3 p,int color,int alpha){out.vertex(pose.pose(),(float)p.x,(float)p.y,(float)p.z).color((color>>16)&255,(color>>8)&255,color&255,alpha).endVertex();}
    private static void vertex(VertexConsumer out,PoseStack.Pose pose,Vec3 p,Vec3 n,int color){out.vertex(pose.pose(),(float)p.x,(float)p.y,(float)p.z).color((color>>16)&255,(color>>8)&255,color&255,190).normal(pose.normal(),(float)n.x,(float)n.y,(float)n.z).endVertex();}
}
