package travail.glossarypreview;
import com.thelongtravail.client.catalog.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.*;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.nio.file.*;
@Mod("travail_smoke")
public class GlossaryPreview {
 int phase,ticks,wait,index,saved;boolean done; double pxKeep,pyKeep;
 String[] items={"sun_chaser","azrael","kagotsurube","lorentz_fantasy","demon_king_of_the_sixth_heaven"};
 String[] terms={"direct_strike","execution","insight","critical","overlord"};
 public GlossaryPreview(){MinecraftForge.EVENT_BUS.addListener(this::tick); MinecraftForge.EVENT_BUS.addListener(this::render);}
 Object field(Object o,String name)throws Exception {var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
 void set(Object o,String name,Object val)throws Exception {var f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,val);}
 void render(net.minecraftforge.client.event.ScreenEvent.Render.Post e){if(phase==3&&wait==5)System.out.println("HOVER_EVENT "+e.getMouseX()+","+e.getMouseY()+" expectedphysical="+pxKeep+","+pyKeep);}
 void checkAnnotations(){
  var list=CatalogGlossary.annotate(List.of(Component.literal("直击伤害、直击、见切、见切。"),Component.empty(),Component.literal("直击伤害，霸主，霸主，必杀，处决。")));
  Map<String,Integer> counts=new HashMap<>();
  for(var c:list)c.getVisualOrderText().accept((i,s,cp)->{String id=CatalogGlossary.term(s);if(id!=null){if(s.isUnderlined())throw new AssertionError("Native underline must be disabled");counts.merge(id,1,Integer::sum);}return true;});
  if(!counts.equals(Map.of("direct_strike",4,"insight",2,"overlord",2,"critical",2,"execution",2)))throw new AssertionError(counts);
  if(!list.get(0).getString().equals("直击伤害、直击、见切、见切。"))throw new AssertionError("Text changed");
  if(CatalogGlossary.term(CatalogGlossary.annotate(List.of(Component.literal("见切"))).get(0).getSiblings().get(0).getStyle())==null)throw new AssertionError("Page reset failed");
  System.out.println("GLOSSARY_ASSERT first-only aliases paragraphs page-reset PASS");
 }
 void open(){var mc=Minecraft.getInstance();var owner=new InventoryScreen(mc.player);mc.setScreen(owner);var entry=CatalogEntries.byId(items[index]);CatalogSession.openDetail(owner,entry,entry.stack());}
 void hover()throws Exception {
  var mc=Minecraft.getInstance();var page=CatalogSession.active().page();
  float scale=(float)field(page,"bodyTextScale");
  int top=(int)field(page,"bodyTop"),bottom=(int)field(page,"bodyBottom"),left=(int)field(page,"left");
  int height=(int)field(page,"contentHeight");
  for(Object row:(List<?>)field(page,"rows")){
   var text=(FormattedCharSequence)field(row,"text");int rowY=(int)field(row,"y");float[] x={0,-1};
   text.accept((i,s,cp)->{if(terms[index].equals(CatalogGlossary.term(s))&&x[1]<0)x[1]=x[0];x[0]+=mc.font.width(FormattedCharSequence.forward(new String(Character.toChars(cp)),s));return true;});
   if(x[1]<0)continue;
   double scroll=Math.max(0,Math.min(((CatalogDetailScreen)page).maxScroll(),rowY-20));set(page,"scroll",scroll);
   double mx=left+34+(x[1]+2)*scale;
   double my=top+Math.max(0,(bottom-top-height)/2)+rowY-(int)scroll+3*scale;
   double px=mx*mc.getWindow().getScreenWidth()/page.width, py=my*mc.getWindow().getScreenHeight()/page.height;
   GLFW.glfwSetCursorPos(mc.getWindow().getWindow(),px,py);
   pxKeep=px;pyKeep=py;set(mc.mouseHandler,"xpos",px); set(mc.mouseHandler,"ypos",py);
   System.out.println("GLOSSARY_HOVER "+terms[index]+" at "+mx+","+my+" scroll="+scroll);
   return;
  }
  throw new AssertionError("Term not found "+terms[index]);
 }
 void tick(TickEvent.ClientTickEvent e){if(done||e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();try{
  if(phase==3){set(mc.mouseHandler,"xpos",pxKeep);set(mc.mouseHandler,"ypos",pyKeep);}
  if(++ticks>6000)throw new AssertionError("timeout phase="+phase);
  if(phase==0&&mc.getOverlay()==null&&mc.screen!=null&&mc.screen.getClass().getSimpleName().matches("AccessibilityOnboardingScreen|StartupAdviceScreen"))mc.setScreen(new TitleScreen());
  if(phase==0&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){checkAnnotations();phase=1;mc.createWorldOpenFlows().loadLevel(new TitleScreen(),"preview");}
  else if(phase==1&&mc.player!=null&&++wait>100){open();phase=2;wait=0;}
  else if(phase==2&&++wait>30){hover();phase=3;wait=0;}
  else if(phase==3&&++wait>30){Screenshot.grab(mc.gameDirectory,"glossary-"+terms[index]+".png",mc.getMainRenderTarget(),m->saved++);phase=4;wait=0;}
  else if(phase==4&&++wait>15){CatalogSession.active().close();if(++index<items.length){open();phase=2;wait=0;}else phase=5;}
  else if(phase==5&&saved==5){Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"),"PASS glossary first-only/aliases/paragraphs/page-reset; screenshots="+saved);done=true;mc.stop();}
 }catch(Throwable ex){ex.printStackTrace();try{Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"),"FAIL "+ex);}catch(Exception ignored){}done=true;mc.stop();}}
}
