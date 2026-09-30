package travail.smoke;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.api.TravailStateApi;
import com.thelongtravail.api.TravailStateApi.AspectState;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayer;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;
import top.theillusivec4.curios.api.type.capability.ICurio;
import java.util.*;

public final class StateApiSmoke {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void run(ServerLevel level) throws Exception {
        var player = new FakePlayer(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "StateApi"));
        for (var aspect : TravailAspect.values()) {
            check(TravailStateApi.state(null, aspect) == AspectState.INACTIVE, "null player inactive");
            check(!TravailStateApi.hasMalice(player, aspect) && !TravailStateApi.hasWitness(player, aspect), "unequipped is neither");
        }
        check(!TravailStateApi.inspectDiary(null).diaryPresent(), "null stack");
        check(!TravailStateApi.inspectDiary(ItemStack.EMPTY).diaryPresent(), "empty stack");
        var diary = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        player.getInventory().setItem(0, diary);
        check(!TravailStateApi.snapshot(player).diaryPresent(), "inventory diary inactive");
        var inventory = CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
        var slots = new CurioStacksHandler(inventory, "travel_diary", 2, true, false, true, ICurio.DropRule.DEFAULT);
        inventory.setCurios(new HashMap<>(Map.of("travel_diary", slots)));
        slots.getStacks().setStackInSlot(0, diary);
        var initial = TravailStateApi.snapshot(player);
        check(initial.diaryPresent() && !initial.initialized(), "uninitialized equipped diary reported");
        for (var aspect : TravailAspect.values()) check(initial.hasMalice(aspect), "uninitialized follows malice rules");
        check(!diary.hasTag(), "queries never initialize or create NBT");
        check(LongTravailData.tryInitialize(diary, player), "fixture initialized");
        var root = diary.getTag().getCompound("LongTravail");
        String[] names = {"Flourishing", "Abyss", "FarReach", "DeepValley", "Underworld", "Boundless"};
        for (int mask=0; mask<64; mask++) {
            root.putInt("Witnesses", mask);
            var before = diary.getTag().copy();
            var snapshot = TravailStateApi.snapshot(player);
            for (var aspect : TravailAspect.values()) {
                boolean expected = (mask & aspect.mask()) != 0;
                check(TravailStateApi.hasWitness(player, aspect) == expected, "generic witness");
                check(TravailStateApi.hasMalice(player, aspect) != expected, "generic malice");
                check(snapshot.hasWitness(aspect) == expected && snapshot.hasMalice(aspect) != expected, "snapshot parity");
                check((boolean) TravailStateApi.class.getMethod("has" + names[aspect.ordinal()] + "Witness", Player.class).invoke(null, player) == expected, "named witness");
                check((boolean) TravailStateApi.class.getMethod("has" + names[aspect.ordinal()] + "Malice", Player.class).invoke(null, player) != expected, "named malice");
            }
            check(before.equals(diary.getTag()), "all queries read-only");
        }
        var captured = TravailStateApi.snapshot(player);
        for (var aspect : TravailAspect.values()) {
            LongTravailData.setWitness(diary, aspect, false);
            check(TravailStateApi.hasMalice(player, aspect), "forced malice overrides natural witness");
        }
        for (var aspect : TravailAspect.values()) check(captured.hasWitness(aspect), "snapshot stays immutable");
        root.putInt("Witnesses", 0);
        for (var aspect : TravailAspect.values()) {
            LongTravailData.setWitness(diary, aspect, true);
            check(TravailStateApi.hasWitness(player, aspect), "forced witness overrides natural malice");
        }
        root.putInt("ForcedMalices", 63);
        for (var aspect : TravailAspect.values()) check(TravailStateApi.hasMalice(player, aspect), "conflicting forced flags prefer malice");
        LongTravailData.clearForcedWitnessState(diary, null);
        var second = diary.copy(); second.getTag().getCompound("LongTravail").putInt("Witnesses", 63);
        slots.getStacks().setStackInSlot(1, second);
        check(TravailStateApi.hasAbyssMalice(player), "multiple diaries follow first equipped, no union");
        slots.getStacks().setStackInSlot(0, ItemStack.EMPTY);
        check(TravailStateApi.hasAbyssWitness(player), "next equipped diary selected after removal");
        var impostor = new ItemStack(Items.STONE); impostor.setTag(second.getTag().copy());
        check(!TravailStateApi.inspectDiary(impostor).diaryPresent(), "foreign item with same NBT rejected");
        slots.getStacks().setStackInSlot(1, ItemStack.EMPTY);
        check(!TravailStateApi.snapshot(player).diaryPresent() && TravailStateApi.inspectDiary(second).hasWitness(TravailAspect.ABYSS), "item inspection distinct from equipped state");
        System.out.println("TRAVAIL_STATE_API_PASS: all 64 masks x six aspects, 12 named methods, absent/inventory/uninitialized, forced priority, immutable snapshots, first equipped, read-only NBT");
    }
}
