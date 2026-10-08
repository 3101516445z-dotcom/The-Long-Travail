package com.thelongtravail.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

// 沿贴图的透明度轮廓生成薄片模型，沿用原版关节和飞行动画。
public final class IcarusWingModel<T extends LivingEntity> extends ElytraModel<T> {
    private final ModelPart left, right;
    private final boolean[][] solid = new boolean[20][10];
    public IcarusWingModel(ResourceLocation texture) { this(ElytraModel.createLayer().bakeRoot(), texture); }
    private IcarusWingModel(ModelPart root, ResourceLocation texture) {
        super(root);left=root.getChild("left_wing");right=root.getChild("right_wing");
        try (var stream=Minecraft.getInstance().getResourceManager().getResourceOrThrow(texture).open();
             var pixels=NativeImage.read(stream)) {
            for(int y=0;y<20;y++)for(int x=0;x<10;x++)solid[y][x]=((pixels.getPixelRGBA(36+x,2+y)>>>24)&255)>0;
        } catch(java.io.IOException e) {throw new IllegalStateException("Cannot load Icarus wing outline",e);}
    }
    private boolean filled(int x,int y){return x>=0&&x<10&&y>=0&&y<20&&solid[y][x];}
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer out,int light,int overlay,float r,float g,float b,float a){
        wing(pose,out,left,false,light,overlay,r,g,b,a);wing(pose,out,right,true,light,overlay,r,g,b,a);
    }
    private void wing(PoseStack pose,VertexConsumer out,ModelPart joint,boolean mirror,int light,int overlay,float r,float g,float b,float a){
        pose.pushPose();joint.translateAndRotate(pose);
        for(int y=0;y<20;y++)for(int x=0;x<10;x++)if(solid[y][x]){
            float x0=x-10,x1=x0+1,y0=y,y1=y+1,z0=0,z1=.5F;
            float u=(36+x+.5F)/64F,v=(2+y+.5F)/32F;
            quad(pose,out,mirror,light,overlay,r,g,b,a,u,v,0,0,-1,x0,y0,z0,x0,y1,z0,x1,y1,z0,x1,y0,z0);
            quad(pose,out,mirror,light,overlay,r,g,b,a,u,v,0,0,1,x1,y0,z1,x1,y1,z1,x0,y1,z1,x0,y0,z1);
            if(!filled(x-1,y))quad(pose,out,mirror,light,overlay,r,g,b,a,u,v,-1,0,0,x0,y0,z1,x0,y1,z1,x0,y1,z0,x0,y0,z0);
            if(!filled(x+1,y))quad(pose,out,mirror,light,overlay,r,g,b,a,u,v,1,0,0,x1,y0,z0,x1,y1,z0,x1,y1,z1,x1,y0,z1);
            if(!filled(x,y-1))quad(pose,out,mirror,light,overlay,r,g,b,a,u,v,0,-1,0,x0,y0,z1,x0,y0,z0,x1,y0,z0,x1,y0,z1);
            if(!filled(x,y+1))quad(pose,out,mirror,light,overlay,r,g,b,a,u,v,0,1,0,x0,y1,z0,x0,y1,z1,x1,y1,z1,x1,y1,z0);
        }
        pose.popPose();
    }
    private static void quad(PoseStack pose,VertexConsumer out,boolean mirror,int light,int overlay,float r,float g,float b,float a,float u,float v,float nx,float ny,float nz,float... xyz){
        var p=pose.last();
        for(int i=0;i<4;i++){int j=(mirror?3-i:i)*3;
            out.vertex(p.pose(),(mirror?-xyz[j]:xyz[j])/16F,xyz[j+1]/16F,xyz[j+2]/16F)
                .color(r,g,b,a).uv(u,v).overlayCoords(overlay).uv2(light)
                .normal(p.normal(),mirror?-nx:nx,ny,nz).endVertex();
        }
    }
}
