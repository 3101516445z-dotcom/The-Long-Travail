package travail.talespreview;
import mezz.jei.api.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
@JeiPlugin
public class PreviewJei implements IModPlugin {
 public static IJeiRuntime runtime;
 public ResourceLocation getPluginUid(){return ResourceLocation.fromNamespaceAndPath("travail_smoke", "tales_preview");}
 public void onRuntimeAvailable(IJeiRuntime value){runtime=value;}
}
