package travail.smoke;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.data.*;
import com.thelongtravail.helper.TravailCurios;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.nbt.*;
import java.util.*;
public final class RenewalSmoke {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void run(net.minecraft.server.level.ServerLevel level) {
        var dropped = new ArrayList<ItemStack>();
        var player = new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "RenewalSmoke")) {
            @Override public net.minecraft.world.entity.item.ItemEntity drop(ItemStack stack, boolean random) {
                dropped.add(stack.copy()); return null;
            }
        };
        var cues = new ArrayList<com.thelongtravail.network.ItemSoundCue>();
        java.util.function.Consumer<net.minecraft.network.protocol.Packet<?>> capture = packet -> {
            if (packet instanceof net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket payload
                    && payload.getIdentifier().toString().equals("the_long_travail:main")) {
                var data = payload.getData(); int offset = data.readerIndex();
                if (data.readableBytes() == 2 && data.getUnsignedByte(offset) == 3)
                    cues.add(com.thelongtravail.network.ItemSoundCue.values()[data.getUnsignedByte(offset + 1)]);
            }
        };
        var wire = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) { capture.accept(packet); }
        };
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(), wire, player) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) { capture.accept(packet); }
        };
        var item = ModRegistry.RENEWAL.get();
        check(item.getMaxStackSize() == 1 && ModRegistry.HOMECOMING.get().getMaxStackSize() == 1, "single stack limits");
        var tooltip = new ArrayList<net.minecraft.network.chat.Component>();
        item.appendHoverText(new ItemStack(item), level, tooltip, TooltipFlag.NORMAL);
        check(tooltip.size() == 3 && item.getName(new ItemStack(item)).getStyle().getInsertion().equals("the_long_travail:name"), "tooltip and name material");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        check(!item.use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction() && player.getMainHandItem().getCount() == 1, "unequipped no consumption");
        check(cues.isEmpty(),"failed use has no sound packet");
        var inventory = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
        var diarySlots = new top.theillusivec4.curios.common.inventory.CurioStacksHandler(inventory, "travel_diary", 1, true, false, true, top.theillusivec4.curios.api.type.capability.ICurio.DropRule.DEFAULT);
        var extraSlots = new top.theillusivec4.curios.common.inventory.CurioStacksHandler(inventory, "curio", 1, true, false, true, top.theillusivec4.curios.api.type.capability.ICurio.DropRule.DEFAULT);
        inventory.setCurios(new HashMap<>(Map.of("travel_diary", diarySlots, "curio", extraSlots)));
        ItemStack diary = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        LongTravailData.initialize(diary, player);
        var root = diary.getTag().getCompound("LongTravail");
        UUID oldId = root.getUUID("JourneyId");
        root.putInt("Witnesses", 63); root.putInt("ForcedWitnesses",63); root.putInt("ForcedMalices",1);
        root.putLong("AttackCleanseCooldown",12345); root.putLong("ValleyWitnessCooldown",54321);
        for (var aspect : TravailAspect.values()) {
            var completed = new ListTag(); completed.add(StringTag.valueOf("minecraft:plains"));
            root.getCompound("Requirements").getCompound(aspect.id()).put("CompletedBiomes",completed);
        }
        diary.setHoverName(net.minecraft.network.chat.Component.literal("Kept name"));
        diary.enchant(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, 1);
        diary.getTag().putString("ExternalData","keep");
        PlayerJourneyData.reveal(player,TravailAspect.FLOURISHING);
        PlayerJourneyData.discoverBiome(player,new net.minecraft.resources.ResourceLocation("minecraft","plains"));
        diarySlots.getStacks().setStackInSlot(0,diary);
        TravailCurios.syncExtraSlots(player,1); extraSlots.update();
        check(extraSlots.getStacks().getSlots()==2,"fixture gained slot");
        extraSlots.getStacks().setStackInSlot(1,new ItemStack(Items.DIAMOND));
        check(item.use(level,player,InteractionHand.MAIN_HAND).getResult().consumesAction(),"equipped reset succeeds");
        check(cues.equals(List.of(com.thelongtravail.network.ItemSoundCue.RENEWAL)),"exactly one renewal sound cue");
        var fresh=diarySlots.getStacks().getStackInSlot(0); var reset=fresh.getTag().getCompound("LongTravail");
        check(!reset.getUUID("JourneyId").equals(oldId) && reset.getInt("Witnesses")==0,"new journey and no witnesses");
        check(!reset.contains("ForcedWitnesses") && !reset.contains("ForcedMalices") && !reset.contains("AttackCleanseCooldown") && !reset.contains("ValleyWitnessCooldown"),"old states removed");
        for(var aspect:TravailAspect.values()) check(reset.getCompound("Requirements").getCompound(aspect.id()).getList("CompletedBiomes",Tag.TAG_STRING).isEmpty(),"completed conditions cleared");
        check(fresh.getHoverName().getString().equals("Kept name") && fresh.isEnchanted() && fresh.getTag().getString("ExternalData").equals("keep"),"external metadata preserved");
        check(PlayerJourneyData.revealedMask(player)==1 && PlayerJourneyData.discoveredBiomeCount(player)==1,"permanent records kept");
        check(player.getInventory().countItem(item)==0 && player.getInventory().countItem(Items.DIAMOND)==1 && dropped.isEmpty(),"consume before returning accessory to hand");
        extraSlots.update(); check(extraSlots.getStacks().getSlots()==1,"bonus slot revoked");
        var restored=ItemStack.of(fresh.save(new CompoundTag()));
        check(restored.getTag().equals(fresh.getTag()),"save reload roundtrip");
        UUID newId=reset.getUUID("JourneyId"); player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));
        item.use(level,player,InteractionHand.MAIN_HAND);
        check(player.getMainHandItem().getCount()==1 && diarySlots.getStacks().getStackInSlot(0).getTag().getCompound("LongTravail").getUUID("JourneyId").equals(newId),"cooldown prevents double reset");
        check(cues.size()==1,"cooldown has no sound cue");
        player.getCooldowns().removeCooldown(item); player.getAbilities().instabuild=true;
        // 满背包加上创造模式保留使用物品的行为，强制触发饰品溢出。
        for(int i=0;i<player.getInventory().items.size();i++) player.getInventory().items.set(i,new ItemStack(Items.COBBLESTONE,64));
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));
        LongTravailData.setWitness(diarySlots.getStacks().getStackInSlot(0),TravailAspect.BOUNDLESS,true);
        TravailCurios.syncExtraSlots(player,1); extraSlots.update();
        extraSlots.getStacks().setStackInSlot(1,new ItemStack(Items.EMERALD));
        item.use(level,player,InteractionHand.MAIN_HAND);
        check(player.getMainHandItem().is(item) && dropped.size()==1 && dropped.get(0).is(Items.EMERALD),"creative no consumption and full inventory drops accessory");
        player.getCooldowns().removeCooldown(item); player.getAbilities().instabuild=false;
        player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(item));
        LongTravailData.setWitness(diarySlots.getStacks().getStackInSlot(0),TravailAspect.BOUNDLESS,true);
        TravailCurios.syncExtraSlots(player,1); extraSlots.update();
        extraSlots.getStacks().setStackInSlot(1,new ItemStack(Items.GOLD_INGOT));
        item.use(level,player,InteractionHand.OFF_HAND);
        check(player.getOffhandItem().isEmpty() && dropped.size()==2 && dropped.get(1).is(Items.GOLD_INGOT),"survival offhand use with full backpack drops gear");
        player.getAbilities().instabuild=true;
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModRegistry.HOMECOMING.get()));
        ModRegistry.HOMECOMING.get().use(level,player,InteractionHand.MAIN_HAND);
        check(diarySlots.getStacks().getStackInSlot(0).isEmpty() && dropped.size()==3 && dropped.get(2).is(ModRegistry.LONG_TRAVAIL.get()),"homecoming never voids diary in full creative inventory");
        check(cues.size()==4 && cues.get(3)==com.thelongtravail.network.ItemSoundCue.HOMECOMING,"one success cue per item action");
        var persisted=player.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
        persisted.getCompound("the_long_travail").putInt("revealed_aspects",0);
        for(var aspect:TravailAspect.values()) {
            var stone=ModRegistry.REVELATION_STONES.get(aspect.ordinal()).get();
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(stone));
            int before=cues.size();
            stone.use(level,player,InteractionHand.MAIN_HAND);
            check(cues.size()==before+1 && cues.get(before)==com.thelongtravail.network.ItemSoundCue.stone(aspect),"stone emits correct cue");
            stone.use(level,player,InteractionHand.MAIN_HAND);
            check(cues.size()==before+1,"already unlocked stone is silent");
        }
        System.out.println("TRAVAIL_SOUND_PACKETS_PASS: successful item actions once, failures/cooldown silent, six stones and repeat unlock");
        var crafting=new net.minecraft.world.inventory.TransientCraftingContainer(player.inventoryMenu,3,3);
        Item[] ingredients={Items.DRAGON_BREATH,Items.NETHER_STAR,Items.DRAGON_BREATH,Items.NETHER_STAR,Items.FEATHER,Items.NETHER_STAR,Items.DRAGON_BREATH,Items.NETHER_STAR,Items.DRAGON_BREATH};
        for(int i=0;i<9;i++) crafting.setItem(i,new ItemStack(ingredients[i]));
        var recipe=level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,crafting,level).orElseThrow();
        check(recipe.assemble(crafting,level.registryAccess()).is(item),"recipe result");
        check(recipe.getRemainingItems(crafting).stream().filter(s->s.is(Items.GLASS_BOTTLE)).count()==4,"dragon breath returns four bottles");
        System.out.println("TRAVAIL_RENEWAL_PASS: reset, metadata, permanent records, consume, cooldown, slots, overflow, creative, recipe/bottles, serialization");
    }
}
