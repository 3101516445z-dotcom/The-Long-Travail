package com.thelongtravail.event;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.data.*;
import com.thelongtravail.helper.TravailCurios;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;


final class TravailCommands {

    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("the_long_travail")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("diagnose_effects").executes(context -> EffectChanges.diagnose(context.getSource().getPlayerOrException())))
                .then(Commands.literal("stiff").then(Commands.literal("clear").executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    StiffState.clear(player);
                    player.sendSystemMessage(Component.translatable("command.the_long_travail.stiff.cleared"));
                    return 1;
                })))
                .then(Commands.literal("visual_deprivation")
                        .then(Commands.literal("clear")
                                .executes(context -> clearVisualDeprivation(context.getSource().getPlayerOrException()))))
                .then(Commands.literal("reverse")
                        .then(Commands.literal("clear")
                                .executes(context -> clearWitnessTesting(context.getSource().getPlayerOrException(), 0))
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 6))
                                        .executes(context -> clearWitnessTesting(
                                                context.getSource().getPlayerOrException(),
                                                IntegerArgumentType.getInteger(context, "value")))))
                        .then(Commands.argument("value", IntegerArgumentType.integer(-6, 6))
                                .executes(context -> setWitnessForTesting(
                                        context.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(context, "value"))))));
    }

    static int clearVisualDeprivation(ServerPlayer player) {
        VisualDeprivation.clear(player, VisualDeprivation.ClearReason.COMMAND);
        player.sendSystemMessage(Component.translatable("command.the_long_travail.visual_deprivation.cleared"));
        return 1;
    }

    static int setWitnessForTesting(ServerPlayer player, int value) {
        if (value == 0) {
            player.sendSystemMessage(Component.translatable("command.the_long_travail.reverse.zero"));
            return 0;
        }
        ItemStack travail = TravailCurios.stack(player);
        if (travail.isEmpty()) {
            player.sendSystemMessage(Component.translatable("command.the_long_travail.reverse.not_equipped"));
            return 0;
        }
        TravailAspect aspect = TravailAspect.values()[Math.abs(value) - 1];
        boolean witness = value > 0;
        LongTravailData.setWitness(travail, aspect, witness);
        String state = witness ? "witness" : "malice";
        String key = "tooltip.the_long_travail." + aspect.id() + "." + state + ".title";
        player.sendSystemMessage(Component.translatable("command.the_long_travail.reverse.success", Component.translatable(key)));
        return 1;
    }

    static int clearWitnessTesting(ServerPlayer player, int value) {
        ItemStack travail = TravailCurios.stack(player);
        if (travail.isEmpty()) {
            player.sendSystemMessage(Component.translatable("command.the_long_travail.reverse.not_equipped"));
            return 0;
        }
        if (value == 0) {
            LongTravailData.clearForcedWitnessState(travail, null);
            player.sendSystemMessage(Component.translatable("command.the_long_travail.reverse.clear_all"));
            return 1;
        }
        TravailAspect aspect = TravailAspect.values()[value - 1];
        LongTravailData.clearForcedWitnessState(travail, aspect);
        player.sendSystemMessage(Component.translatable("command.the_long_travail.reverse.clear_one", value));
        return 1;
    }

    private TravailCommands() {}
}
