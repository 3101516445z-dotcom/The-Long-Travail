package travail.diarypreview;
import com.thelongtravail.client.TravelDiaryScreen;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
@Mod("travail_smoke")
public class DiaryEffectsPreview {
 int phase,ticks,wait,saved;boolean done;TravelDiaryScreen page;
 public DiaryEffectsPreview(){MinecraftForge.EVENT_BUS.addListener(this::tick);}
 void capture(String name){var mc=Minecraft.getInstance();Screenshot.grab(mc.gameDirectory,name+".png",mc.getMainRenderTarget(),m->saved++);}
 void tick(TickEvent.ClientTickEvent e){if(done||e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();try{
 if(++ticks>6000)throw new AssertionError("timeout");
 if(phase==0&&mc.getOverlay()==null&&mc.screen!=null&&mc.screen.getClass().getSimpleName().matches("AccessibilityOnboardingScreen|StartupAdviceScreen"))mc.setScreen(new TitleScreen());
 if(phase==0&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){phase=1;mc.createWorldOpenFlows().loadLevel(new TitleScreen(),"preview");}
 else if(phase==1&&mc.player!=null&&++wait>80){var book=new ItemStack(ModRegistry.LONG_TRAVAIL.get());page=new TravelDiaryScreen(()->book,()->mc.setScreen(null));var f=TravelDiaryScreen.class.getDeclaredField("selected");f.setAccessible(true);f.set(page,TravailAspect.DEEP_VALLEY);var intro=TravelDiaryScreen.class.getDeclaredField("preface");intro.setAccessible(true);intro.set(page,false);mc.setScreen(page);phase=2;wait=0;}
 else if(phase==2&&++wait>40){capture("diary-valley-effects");phase=3;wait=0;}
 else if(phase==3&&saved>=1&&++wait>15){Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"),"PASS diary screenshot");done=true;mc.stop();}
 }catch(Throwable ex){ex.printStackTrace();done=true;mc.stop();}}
}
