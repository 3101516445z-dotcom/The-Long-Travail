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
        event.getDispatcher().register(
            Commands.literal("tlt")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("timestop")
                    .executes(context -> timeStop(context.getSource(),
                        com.thelongtravail.config.BoundlessItemsConfig.ticks("star.witnessDuration")))
                    .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 15))
                        .executes(context -> timeStop(context.getSource(),
                            IntegerArgumentType.getInteger(context, "seconds") * 20))))
                .then(Commands.literal("execute")
                    .then(Commands.argument("targets", net.minecraft.commands.arguments.EntityArgument.entities())
                        .executes(context -> executeTargets(context.getSource(),
                            net.minecraft.commands.arguments.EntityArgument.getEntities(context, "targets")))))
                .then(Commands.literal("diagnose_effects")
                    .executes(context -> EffectChanges.diagnose(context.getSource().getPlayerOrException())))
                .then(Commands.literal("stiff")
                    .then(Commands.literal("clear")
                        .executes(context -> {
                            var player = context.getSource().getPlayerOrException();
                            StiffState.clear(player);
                            player.sendSystemMessage(
                                Component.translatable("command.the_long_travail.stiff.cleared"));
                            return 1;
                        })))
                .then(Commands.literal("visual_deprivation")
                    .then(Commands.literal("clear")
                        .executes(context -> clearVisualDeprivation(context.getSource().getPlayerOrException()))))
                .then(Commands.literal("reverse")
                    .executes(context -> setAllWitnessesForTesting(context.getSource().getPlayerOrException()))
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

    private static int timeStop(net.minecraft.commands.CommandSourceStack source, int ticks)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = source.getPlayerOrException();
        if (!player.isAlive()
                || player.isSpectator()
                || com.thelongtravail.boundless.TimeStopManager.frozen(player)) {
            source.sendFailure(
                    Component.translatable("command.the_long_travail.timestop.unavailable"));
            return 0;
        }
        boolean started =
                com.thelongtravail.boundless.TimeStopManager.get(player.serverLevel())
                        .start(
                                player,
                                true,
                                player.level().getGameTime(),
                                player.position(),
                                com.thelongtravail.config.BoundlessItemsConfig.get("star.radius"),
                                ticks,
                                com.thelongtravail.boundless.TimeStopExemptions.snapshot(player)
                                        .orElse(null));
        if (!started) {
            source.sendFailure(Component.translatable("command.the_long_travail.timestop.failed"));
            return 0;
        }
        source.sendSuccess(
                () -> Component.translatable("command.the_long_travail.timestop.started"), true);
        return 1;
    }

    private static int executeTargets(
            net.minecraft.commands.CommandSourceStack source,
            java.util.Collection<? extends net.minecraft.world.entity.Entity> targets) {
        var actor =
                source.getEntity() instanceof net.minecraft.world.entity.LivingEntity living
                        ? living
                        : null;
        int dead = 0, terminated = 0, survived = 0, skipped = 0, failed = 0;
        for (var target : targets) {
            if (!(target instanceof net.minecraft.world.entity.LivingEntity living)) {
                skipped++;
                continue;
            }
            try {
                switch (com.thelongtravail.valley.AzraelExecution.executeForCommand(
                        actor, living)) {
                    case DEAD -> {
                        if (com.thelongtravail.valley.FinalExecution.terminated(living))
                            terminated++;
                        else dead++;
                    }
                    case SURVIVED -> survived++;
                    case INVALID -> skipped++;
                }
            } catch (RuntimeException error) {
                failed++;
                com.thelongtravail.TheLongTravail.LOGGER.error(
                        "Execution command failed for {}",
                        target.getType() + " " + target.getUUID(),
                        error);
            }
        }
        var message =
                Component.translatable(
                        "command.the_long_travail.execute.result",
                        dead,
                        terminated,
                        survived,
                        skipped,
                        failed);
        source.sendSuccess(() -> message, true);
        return dead + terminated;
    }

    static int clearVisualDeprivation(ServerPlayer player) {
        VisualDeprivation.clear(player, VisualDeprivation.ClearReason.COMMAND);
        player.sendSystemMessage(
                Component.translatable("command.the_long_travail.visual_deprivation.cleared"));
        return 1;
    }

    static int setAllWitnessesForTesting(ServerPlayer player) {
        ItemStack travail = TravailCurios.stack(player);
        if (travail.isEmpty()) {
            player.sendSystemMessage(
                    Component.translatable("command.the_long_travail.reverse.not_equipped"));
            return 0;
        }
        if (!accepted(
                player,
                com.thelongtravail.api.TravailJourneyApi.setAllOverrides(
                        player, JourneyOperations.Override.WITNESS))) return 0;
        player.sendSystemMessage(Component.translatable("command.the_long_travail.reverse.all"));
        return 1;
    }

    static int setWitnessForTesting(ServerPlayer player, int value) {
        if (value == 0) {
            player.sendSystemMessage(
                    Component.translatable("command.the_long_travail.reverse.zero"));
            return 0;
        }
        ItemStack travail = TravailCurios.stack(player);
        if (travail.isEmpty()) {
            player.sendSystemMessage(
                    Component.translatable("command.the_long_travail.reverse.not_equipped"));
            return 0;
        }
        TravailAspect aspect = TravailAspect.values()[Math.abs(value) - 1];
        boolean witness = value > 0;
        if (!accepted(
                player,
                com.thelongtravail.api.TravailJourneyApi.setOverride(
                        player,
                        aspect,
                        witness
                                ? JourneyOperations.Override.WITNESS
                                : JourneyOperations.Override.MALICE))) return 0;
        String state = witness ? "witness" : "malice";
        String key = "tooltip.the_long_travail." + aspect.id() + "." + state + ".title";
        player.sendSystemMessage(
                Component.translatable(
                        "command.the_long_travail.reverse.success", Component.translatable(key)));
        return 1;
    }

    static int clearWitnessTesting(ServerPlayer player, int value) {
        ItemStack travail = TravailCurios.stack(player);
        if (travail.isEmpty()) {
            player.sendSystemMessage(
                    Component.translatable("command.the_long_travail.reverse.not_equipped"));
            return 0;
        }
        if (value == 0) {
            if (!accepted(
                    player, com.thelongtravail.api.TravailJourneyApi.clearAllOverrides(player)))
                return 0;
            player.sendSystemMessage(
                    Component.translatable("command.the_long_travail.reverse.clear_all"));
            return 1;
        }
        TravailAspect aspect = TravailAspect.values()[value - 1];
        if (!accepted(
                player, com.thelongtravail.api.TravailJourneyApi.clearOverride(player, aspect)))
            return 0;
        player.sendSystemMessage(
                Component.translatable("command.the_long_travail.reverse.clear_one", value));
        return 1;
    }

    private static boolean accepted(ServerPlayer player, JourneyService.OperationResult result) {
        if (result.accepted()) return true;
        player.sendSystemMessage(
                Component.translatable(
                        "message.the_long_travail.journey."
                                + result.status().name().toLowerCase(java.util.Locale.ROOT)));
        return false;
    }

    private TravailCommands() {}
}
