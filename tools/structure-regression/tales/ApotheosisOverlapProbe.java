package travail.tales;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.journey.LongTravailData;
import com.thelongtravail.flourishing.ImmortalBud;
import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.underworld.UnderworldItems;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;
import top.theillusivec4.curios.api.type.capability.ICurio.DropRule;

final class ApotheosisOverlapProbe {
    static Runnable attackProbe, defenseProbe;

    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    static void run(MinecraftServer server) throws Exception {
        var lines = new ArrayList<String>();
        var world = server.overworld();
        var player = FakePlayerFactory.getMinecraft(world);
        int items = 0;
        for (var entry : ForgeRegistries.ITEMS.getEntries()) {
            if (!entry.getKey().location().getNamespace().equals("the_long_travail")) continue;
            var stack = new ItemStack(entry.getValue());
            stack.getHoverName().getString();
            ItemStack.of(stack.save(new net.minecraft.nbt.CompoundTag()));
            stack.getEnchantmentValue();
            stack.getAllEnchantments();
            items++;
        }
        lines.add("PASS TLT item name/NBT/enchantment queries: " + items);
        var protectedSword = ImmortalBud.apply(new ItemStack(Items.DIAMOND_SWORD));
        for (int n = 0; n < 10000; n++) {
            if (protectedSword.hurt(100, world.random, player))
                throw new AssertionError("protected sword broken");
            if (protectedSword.getDamageValue() != 0)
                throw new AssertionError("protected sword damaged");
        }
        var drop = new ItemEntity(world, 0, 100, 0, protectedSword);
        for (int n = 0; n < 1000; n++)
            if (drop.hurt(world.damageSources().cactus(), 100))
                throw new AssertionError("protected cactus damage");
        lines.add("PASS durability 10000 and cactus 1000");
        var inv = CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
        Map<String, top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler> slots =
                new HashMap<>();
        for (String id : List.of("travel_diary", "ring", "belt"))
            slots.put(id, new CurioStacksHandler(inv, id, 2, true, false, true, DropRule.DEFAULT));
        inv.setCurios(slots);
        var diary = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        LongTravailData.initialize(diary, player);
        LongTravailData.setWitness(diary, TravailAspect.UNDERWORLD, true);
        inv.getStacksHandler("travel_diary").orElseThrow().getStacks().setStackInSlot(0, diary);
        inv.getStacksHandler("ring")
                .orElseThrow()
                .getStacks()
                .setStackInSlot(0, new ItemStack(ModRegistry.THOUSAND_YEARS.get()));
        if (!UnderworldItems.seesUndead(player)) throw new AssertionError("witness setup");
        var spider = EntityType.SPIDER.create(world);
        var weapon = new ItemStack(Items.DIAMOND_SWORD);
        weapon.enchant(Enchantments.BANE_OF_ARTHROPODS, 5);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, weapon);
        LongTravailData.setWitness(diary, TravailAspect.UNDERWORLD, false);
        net.minecraft.world.item.enchantment.EnchantmentHelper.doPostDamageEffects(player, spider);
        require(spider.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "baseline arthropod callback");
        spider.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        LongTravailData.setWitness(diary, TravailAspect.UNDERWORLD, true);
        for (int n = 0; n < 10000; n++) {
            net.minecraft.world.item.enchantment.EnchantmentHelper.doPostDamageEffects(
                    player, spider);
            require(!spider.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "undead callback");
            require(
                    spider.getMobType() == net.minecraft.world.entity.MobType.ARTHROPOD,
                    "callback cleanup");
        }
        var cow = EntityType.COW.create(world);
        for (int n = 0; n < 10000; n++) {
            try (var outer =
                    com.thelongtravail.underworld.AttackMobTypeContext.open(player, spider)) {
                require(
                        spider.getMobType() == net.minecraft.world.entity.MobType.UNDEAD,
                        "outer target");
                require(
                        cow.getMobType() == net.minecraft.world.entity.MobType.UNDEFINED,
                        "unrelated target");
                try (var inner =
                        com.thelongtravail.underworld.AttackMobTypeContext.open(player, cow)) {
                    require(
                            cow.getMobType() == net.minecraft.world.entity.MobType.UNDEAD,
                            "inner target");
                    require(
                            spider.getMobType() == net.minecraft.world.entity.MobType.ARTHROPOD,
                            "outer masked");
                }
                try (var inner =
                        com.thelongtravail.underworld.AttackMobTypeContext.open(cow, spider)) {
                    require(
                            spider.getMobType() == net.minecraft.world.entity.MobType.ARTHROPOD,
                            "ineligible nested attacker");
                }
                require(
                        spider.getMobType() == net.minecraft.world.entity.MobType.UNDEAD,
                        "outer restored");
                throw new IllegalStateException("fixture");
            } catch (IllegalStateException expected) {
                require("fixture".equals(expected.getMessage()), "expected exception");
            }
            require(
                    spider.getMobType() == net.minecraft.world.entity.MobType.ARTHROPOD,
                    "exception cleanup");
        }
        var callbackWeapon = new ItemStack(Items.DIAMOND_SWORD);
        callbackWeapon.enchant(
                ForgeRegistries.ENCHANTMENTS.getValue(
                        new net.minecraft.resources.ResourceLocation("travail_smoke", "broken")),
                1);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, callbackWeapon);
        defenseProbe =
                () ->
                        require(
                                spider.getMobType() == net.minecraft.world.entity.MobType.ARTHROPOD,
                                "defense callback sees actual type");
        attackProbe =
                () -> {
                    require(
                            spider.getMobType() == net.minecraft.world.entity.MobType.UNDEAD,
                            "custom callback sees undead");
                    net.minecraft.world.item.enchantment.EnchantmentHelper.doPostHurtEffects(
                            player, spider);
                    require(
                            spider.getMobType() == net.minecraft.world.entity.MobType.UNDEAD,
                            "defense callback restores outer");
                    throw new IllegalStateException("callback fixture");
                };
        try {
            for (int n = 0; n < 1000; n++) {
                try {
                    net.minecraft.world.item.enchantment.EnchantmentHelper.doPostDamageEffects(
                            player, spider);
                    throw new AssertionError("callback must throw");
                } catch (IllegalStateException expected) {
                    require(
                            "callback fixture".equals(expected.getMessage()),
                            "callback exception propagated");
                }
                require(
                        spider.getMobType() == net.minecraft.world.entity.MobType.ARTHROPOD,
                        "helper exception cleanup");
            }
        } finally {
            attackProbe = defenseProbe = null;
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, weapon);
        }
        lines.add("PASS custom callback/defense masking/exception cleanup 1000");
        // 经过真实 Mob.doHurtTarget 的附魔增伤计算，仅截获最终 hurt 输入以排除外部减伤随机性。
        class AttackTarget extends net.minecraft.world.entity.monster.Spider {
            float input;
            boolean fail;
            net.minecraft.world.entity.MobType observed;

            AttackTarget() {
                super(EntityType.SPIDER, world);
            }

            @Override
            public boolean hurt(
                    net.minecraft.world.damagesource.DamageSource source, float amount) {
                input = amount;
                observed = getMobType();
                if (fail) throw new IllegalStateException("nested mob fixture");
                return false;
            }
        }
        var nestedTarget = new AttackTarget();
        var nestedAttacker = EntityType.ZOMBIE.create(world);
        var smiteWeapon = new ItemStack(Items.DIAMOND_SWORD);
        smiteWeapon.enchant(Enchantments.SMITE, 5);
        nestedAttacker.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, smiteWeapon);
        nestedAttacker.doHurtTarget(nestedTarget);
        float baselineInput = nestedTarget.input;
        require(baselineInput > 0, "nested mob control damage");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, callbackWeapon);
        attackProbe =
                () -> {
                    require(
                            nestedTarget.getMobType() == net.minecraft.world.entity.MobType.UNDEAD,
                            "nested mob outer view");
                    for (boolean fail : new boolean[] {false, true}) {
                        nestedTarget.fail = fail;
                        try {
                            nestedAttacker.doHurtTarget(nestedTarget);
                            require(!fail, "nested mob exception expected");
                        } catch (IllegalStateException expected) {
                            require(
                                    fail && "nested mob fixture".equals(expected.getMessage()),
                                    "nested mob exception propagated");
                        }
                        require(
                                Math.abs(nestedTarget.input - baselineInput) < 1e-6,
                                "nested mob must not inherit undead damage bonus: "
                                        + nestedTarget.input
                                        + " versus "
                                        + baselineInput);
                        require(
                                nestedTarget.observed
                                        == net.minecraft.world.entity.MobType.ARTHROPOD,
                                "nested mob sees actual type");
                        require(
                                nestedTarget.getMobType()
                                        == net.minecraft.world.entity.MobType.UNDEAD,
                                "nested mob restores outer view");
                    }
                };
        try {
            for (int n = 0; n < 1000; n++) {
                net.minecraft.world.item.enchantment.EnchantmentHelper.doPostDamageEffects(
                        player, nestedTarget);
                require(
                        nestedTarget.getMobType() == net.minecraft.world.entity.MobType.ARTHROPOD,
                        "nested mob outer cleanup");
            }
        } finally {
            attackProbe = null;
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, weapon);
        }
        lines.add(
                "PASS real nested Mob.doHurtTarget normal/exception 1000; baselineInput="
                        + baselineInput);

        inv.getStacksHandler("belt")
                .orElseThrow()
                .getStacks()
                .setStackInSlot(0, new ItemStack(ModRegistry.SWORD_AND_LANTERN.get()));
        spider.setPos(player.getX() + 1, player.getY(), player.getZ());
        LongTravailData.setWitness(diary, TravailAspect.UNDERWORLD, false);
        double normal = com.thelongtravail.valley.lantern.SwordLanternState.bonus(player, spider);
        LongTravailData.setWitness(diary, TravailAspect.UNDERWORLD, true);
        double undead = com.thelongtravail.valley.lantern.SwordLanternState.bonus(player, spider);
        require(
                Math.abs(
                                undead
                                        - normal
                                        - com.thelongtravail.config.SwordLanternConfig.UNDEAD_BONUS
                                                .get())
                        < 1e-8,
                "SwordLantern undead bonus");
        lines.add(
                "PASS callback 10000, nested/exception scope 10000, SwordLantern bonus difference="
                        + (undead - normal));
        lines.add("Bane implementation=" + Enchantments.BANE_OF_ARTHROPODS.getClass().getName());
        SuccessfulAttackProbe.run(server, player, callbackWeapon, lines);
        Files.write(Path.of("apotheosis-overlap-result.txt"), lines);
        lines.forEach(System.out::println);
    }
}
