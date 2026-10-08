package travail.smoke;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;
import java.util.*;

public final class TravailCommandsSmoke {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void run(ServerLevel level) throws Exception {
        List<String> messages = new ArrayList<>();
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "CommandSmoke")) {
            @Override public void sendSystemMessage(Component text) {
                if (text.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t) messages.add(t.getKey());
            }
        };
        var dispatcher = level.getServer().getCommands().getDispatcher();
        var source = player.createCommandSourceStack().withPermission(2);
        check(dispatcher.execute("the_long_travail reverse 0", source) == 0, "zero rejected without mutation");
        check(messages.get(messages.size() - 1).endsWith("reverse.zero"), "zero feedback");
        check(dispatcher.execute("the_long_travail reverse 1", source) == 0, "missing diary rejected");
        check(messages.get(messages.size() - 1).endsWith("reverse.not_equipped"), "missing diary feedback");
        var inv = CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
        var slots = new CurioStacksHandler(inv, "travel_diary", 1, true, false, true, ICurio.DropRule.DEFAULT);
        inv.setCurios(new HashMap<>(Map.of("travel_diary", slots)));
        var diary = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        check(LongTravailData.tryInitialize(diary, player), "initialize"); slots.getStacks().setStackInSlot(0, diary);
        for (int value = 1; value <= 6; value++) {
            check(dispatcher.execute("the_long_travail reverse " + value, source) == 1, "set witness");
            check(LongTravailData.hasWitness(diary, TravailAspect.values()[value - 1]), "correct aspect witness");
            check(dispatcher.execute("the_long_travail reverse -" + value, source) == 1, "set malice");
            check(!LongTravailData.hasWitness(diary, TravailAspect.values()[value - 1]), "correct aspect malice");
            check(dispatcher.execute("the_long_travail reverse clear " + value, source) == 1, "clear aspect override");
        }
        check(dispatcher.execute("the_long_travail reverse clear", source) == 1, "clear all");
        check(dispatcher.execute("the_long_travail reverse clear 0", source) == 1, "explicit clear all");
        for (var command : List.of("reverse 7", "reverse -7", "reverse clear -1", "reverse clear 7")) {
            try { dispatcher.execute("the_long_travail " + command, source); throw new AssertionError("invalid range accepted"); }
            catch (CommandSyntaxException expected) { }
        }
        try { dispatcher.execute("the_long_travail reverse 1", source.withPermission(1)); throw new AssertionError("permission bypass"); }
        catch (CommandSyntaxException expected) { }
        System.out.println("TRAVAIL_COMMANDS_PASS: registered dispatcher, permission, zero/missing diary, all six positive/negative states, clear and range validation");
    }
}
