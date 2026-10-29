package travail.talespreview;
import com.thelongtravail.client.catalog.*;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import mezz.jei.api.constants.*;
import mezz.jei.api.recipe.*;
import java.util.*;
import java.nio.file.*;
@Mod("travail_smoke")
public class TalesPreview {
 int phase,ticks,wait,saved; boolean done; volatile boolean ready;
 public TalesPreview(){MinecraftForge.EVENT_BUS.addListener(this::tick);}
 static class Hover extends InventoryScreen {
  Hover(){super(Minecraft.getInstance().player);}
  public void render(GuiGraphics g,int x,int y,float t){super.render(g,leftPos+16,topPos+92,t);}
 }
 void shot(String name){var mc=Minecraft.getInstance();Screenshot.grab(mc.gameDirectory,name+".png",mc.getMainRenderTarget(),m->saved++);System.out.println("TALES_SCREENSHOT "+name);}
 <T> void jei(RecipeType<T> type){
  var rt=PreviewJei.runtime; var focus=rt.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT,VanillaTypes.ITEM_STACK,new ItemStack(ModRegistry.ANCIENT_STRANGE_TALES.get()));
  var recipes=rt.getRecipeManager().createRecipeLookup(type).limitFocus(List.of(focus)).get().toList();
  if(recipes.isEmpty())throw new AssertionError("Missing recipes "+type);
  System.out.println("TALES_JEI "+type+" count="+recipes.size());
  rt.getRecipesGui().showRecipes(rt.getRecipeManager().getRecipeCategory(type),recipes,List.of(focus));
 }
 void tick(TickEvent.ClientTickEvent e){
  if(done||e.phase!=TickEvent.Phase.END)return; var mc=Minecraft.getInstance();
  try{
   if(++ticks>6000)throw new AssertionError("Timeout phase="+phase);
   if(ticks%200==0)System.out.println("TALES_PREVIEW phase="+phase);
   if(phase==0&&mc.getOverlay()==null&&mc.screen!=null&&mc.screen.getClass().getSimpleName().matches("AccessibilityOnboardingScreen|StartupAdviceScreen"))mc.setScreen(new TitleScreen());
   if(phase==0&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){phase=1;mc.createWorldOpenFlows().loadLevel(new TitleScreen(),"preview");}
   else if(phase==1&&mc.player!=null&&PreviewJei.runtime!=null){phase=2;mc.getSingleplayerServer().execute(()->{var pl=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());pl.setInvulnerable(true);pl.getInventory().clearContent();pl.getInventory().setItem(9,new ItemStack(ModRegistry.ANCIENT_STRANGE_TALES.get()));pl.inventoryMenu.broadcastChanges();ready=true;});}
   else if(phase==2&&ready&&++wait>80){mc.getToasts().clear();mc.setScreen(new Hover());phase=3;wait=0;}
   else if(phase==3&&++wait>40){shot("01-tooltip");phase=4;wait=0;}
   else if(phase==4&&++wait>15){var owner=new InventoryScreen(mc.player);mc.setScreen(owner);var entry=CatalogEntries.byId("ancient_strange_tales");CatalogSession.openDetail(owner,entry,entry.stack());phase=5;wait=0;}
   else if(phase==5&&++wait>40){shot("02-catalog");phase=6;wait=0;}
   else if(phase==6&&++wait>15){CatalogSession.active().close();mc.setScreen(new InventoryScreen(mc.player));jei(RecipeTypes.CRAFTING);phase=7;wait=0;}
   else if(phase==7&&++wait>40){shot("03-jei-crafting");phase=8;wait=0;}
   else if(phase==8&&++wait>15){jei(RecipeTypes.INFORMATION);phase=9;wait=0;}
   else if(phase==9&&++wait>40){shot("04-jei-information");phase=10;wait=0;}
   else if(phase==10&&saved>=4&&++wait>30){Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"),"PASS screenshots="+saved);done=true;mc.stop();}
  }catch(Throwable ex){ex.printStackTrace();try{Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"),"FAIL "+ex);}catch(Exception ignored){}done=true;mc.stop();}
 }
}
