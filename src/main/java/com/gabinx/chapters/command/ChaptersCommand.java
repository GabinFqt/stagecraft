package com.gabinx.chapters.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.gabinx.chapters.api.ChaptersAPI;
import com.gabinx.chapters.stage.StageManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.stream.Collectors;

public final class ChaptersCommand {
    private static final SuggestionProvider<CommandSourceStack> STAGE_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggestResource(
                    StageManager.get().stageIds(), builder
            );

    private ChaptersCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("chapters")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("add")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("stage", IdentifierArgument.id())
                                        .suggests(STAGE_SUGGESTIONS)
                                        .executes(ctx -> {
                                            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                            Identifier stage = IdentifierArgument.getId(ctx, "stage");
                                            validateStage(stage);
                                            ChaptersAPI.addStage(player, stage);
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.translatable("commands.chapters.add.success", stage.toString(), player.getGameProfile().name()),
                                                    true
                                            );
                                            return 1;
                                        }))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("stage", IdentifierArgument.id())
                                        .suggests(STAGE_SUGGESTIONS)
                                        .executes(ctx -> {
                                            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                            Identifier stage = IdentifierArgument.getId(ctx, "stage");
                                            validateStage(stage);
                                            ChaptersAPI.removeStage(player, stage);
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.translatable("commands.chapters.remove.success", stage.toString(), player.getGameProfile().name()),
                                                    true
                                            );
                                            return 1;
                                        }))))
                .then(Commands.literal("list")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                    Set<Identifier> values = ChaptersAPI.getStages(player);
                                    String joined = values.isEmpty()
                                            ? "-"
                                            : values.stream().map(Identifier::toString).collect(Collectors.joining(", "));
                                    ctx.getSource().sendSuccess(
                                            () -> Component.translatable("commands.chapters.list.header", player.getGameProfile().name(), joined),
                                            false
                                    );
                                    return values.size();
                                })))
                .then(Commands.literal("check")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("stage", IdentifierArgument.id())
                                        .suggests(STAGE_SUGGESTIONS)
                                        .executes(ctx -> {
                                            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                            Identifier stage = IdentifierArgument.getId(ctx, "stage");
                                            boolean has = ChaptersAPI.hasStage(player, stage);
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.translatable(
                                                            has ? "commands.chapters.check.true" : "commands.chapters.check.false",
                                                            player.getGameProfile().name(),
                                                            stage.toString()
                                                    ),
                                                    false
                                            );
                                            return has ? 1 : 0;
                                        }))))
                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            ctx.getSource().getServer().reloadResources(ctx.getSource().getServer().getPackRepository().getSelectedIds());
                            ctx.getSource().sendSuccess(
                                    () -> Component.translatable("commands.chapters.reload.success"),
                                    true
                            );
                            return 1;
                        })));
    }

    private static void validateStage(Identifier stage) throws CommandSyntaxException {
        if (!StageManager.get().stageIds().contains(stage)) {
            throw new SimpleCommandExceptionType(
                    Component.translatable("commands.chapters.error.unknown_stage", stage.toString())
            ).create();
        }
    }
}
