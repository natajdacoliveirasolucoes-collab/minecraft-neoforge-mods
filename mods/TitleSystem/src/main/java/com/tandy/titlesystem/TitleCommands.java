package com.tandy.titlesystem;

import java.text.Normalizer;
import java.util.Locale;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = TitleSystem.MODID)
public final class TitleCommands {
    private TitleCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        var root = Commands.literal("titulos")
                .executes(context -> list(context.getSource()))
                .then(Commands.literal("preview").requires(source -> source.hasPermission(3))
                        .then(Commands.literal("off")
                                .then(Commands.argument("jogador", EntityArgument.player())
                                        .executes(context -> previewControl(context, false))))
                        .then(Commands.literal("status")
                                .then(Commands.argument("jogador", EntityArgument.player())
                                        .executes(context -> previewControl(context, true))))
                        .then(Commands.argument("jogador", EntityArgument.player())
                                .then(Commands.argument("titulo", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            for (Title title : Titles.ALL) builder.suggest(title.id());
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> preview(context, 60))
                                        .then(Commands.argument("segundos", IntegerArgumentType.integer(1, 600))
                                                .executes(context -> preview(context,
                                                        IntegerArgumentType.getInteger(context, "segundos")))))))
                .then(Commands.literal("set")
                        .then(Commands.argument("titulo", StringArgumentType.greedyString())
                                .suggests((context, builder) -> {
                                    for (Title title : Titles.ALL) {
                                        builder.suggest(title.id());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> select(context.getSource(),
                                        StringArgumentType.getString(context, "titulo")))))
                .then(Commands.literal("all").executes(context -> all(context.getSource())))
                .then(Commands.literal("off").executes(context -> off(context.getSource())))
                .then(Commands.literal("on").executes(context -> on(context.getSource())))
                .then(Commands.literal("stats").executes(context -> stats(context.getSource())))
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            try {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                for (Title title : Titles.unlocked(TitleData.kills(player), TitleData.capturedAll(player))) {
                                    builder.suggest(title.id());
                                }
                            } catch (Exception ignored) {
                                // not a player, no suggestions
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> select(context.getSource(), StringArgumentType.getString(context, "id"))));

        var node = dispatcher.register(root);
        dispatcher.register(Commands.literal("titles").redirect(node));
    }

    private static int list(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            int kills = TitleData.kills(player);
            boolean collector = TitleData.capturedAll(player);
            var captured = TitleData.captured(player);
            int known = Math.max(0, TitleData.knownPlayers(player.getServer()).size() - 1);

            source.sendSuccess(() -> Component.literal("[Títulos] Kills PvP: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.valueOf(kills)).withStyle(ChatFormatting.WHITE)), false);
            source.sendSuccess(() -> Component.literal("Cabeças: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(captured.getAllKeys().size() + "/" + known)
                            .withStyle(ChatFormatting.WHITE)), false);

            String equippedId = TitleData.equipped(player);
            Component equipped = Titles.byId(equippedId).map(Title::display)
                    .orElse(Component.literal("nenhum").withStyle(ChatFormatting.DARK_GRAY));
            source.sendSuccess(() -> Component.literal("Equipado: ").withStyle(ChatFormatting.GRAY)
                    .append(equipped), false);

            source.sendSuccess(() -> Component.literal("Desbloqueados:").withStyle(ChatFormatting.GRAY), false);
            for (Title title : Titles.unlocked(kills, collector)) {
                source.sendSuccess(() -> Component.literal(" - ")
                        .append(title.display())
                        .append(Component.literal(" (" + title.id() + ")").withStyle(ChatFormatting.DARK_GRAY)),
                        false);
            }
            source.sendSuccess(() -> Component.literal("Use /titulos set <nome>, /titulos off|on|stats")
                    .withStyle(ChatFormatting.DARK_GRAY), false);
            return 1;
        } catch (Exception exception) {
            source.sendFailure(Component.literal("Apenas jogadores podem usar esse comando."));
            return 0;
        }
    }

    private static int preview(CommandContext<CommandSourceStack> context, int seconds)
            throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "jogador");
        Title title = findTitle(StringArgumentType.getString(context, "titulo"));
        if (title == null) {
            context.getSource().sendFailure(Component.literal("Título desconhecido."));
            return 0;
        }
        TitlePreviews.start(player, title, seconds);
        context.getSource().sendSuccess(() -> Component.literal("Prévia: " + player.getGameProfile().getName()
                + " -> " + title.id() + " por " + seconds + "s. Progresso preservado."), false);
        return 1;
    }

    private static int previewControl(CommandContext<CommandSourceStack> context, boolean status)
            throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "jogador");
        if (!status) TitlePreviews.stop(player);
        Title current = TitlePreviews.current(player);
        context.getSource().sendSuccess(() -> Component.literal("Prévia de " + player.getGameProfile().getName()
                + ": " + (current == null ? "nenhuma" : current.id() + " ("
                + TitlePreviews.remainingSeconds(player) + "s)")), false);
        return current == null ? 0 : 1;
    }

    private static int select(CommandSourceStack source, String input) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            Title title = findTitle(input);
            if (title == null) {
                source.sendFailure(Component.literal("Título desconhecido: " + input));
                return 0;
            }
            int kills = TitleData.kills(player);
            boolean collector = TitleData.capturedAll(player);
            if (!Titles.isUnlocked(title, kills, collector)) {
                source.sendFailure(Component.literal("Você ainda não desbloqueou esse título."));
                return 0;
            }

            TitleData.setEquipped(player, title.id());
            TitleData.setHidden(player, false);
            KillTracker.refresh(player);
            source.sendSuccess(() -> Component.literal("Título equipado: ").withStyle(ChatFormatting.GRAY)
                    .append(title.display()), false);
            return 1;
        } catch (Exception exception) {
            source.sendFailure(Component.literal("Apenas jogadores podem escolher título."));
            return 0;
        }
    }

    private static int off(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            TitleData.setHidden(player, true);
            KillTracker.refresh(player);
            source.sendSuccess(() -> Component.literal("Seu título foi ocultado.").withStyle(ChatFormatting.GRAY), false);
            return 1;
        } catch (Exception exception) {
            source.sendFailure(Component.literal("Apenas jogadores podem usar esse comando."));
            return 0;
        }
    }

    private static int on(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            TitleData.setHidden(player, false);
            KillTracker.refresh(player);
            source.sendSuccess(() -> Component.literal("Seu título está visível.").withStyle(ChatFormatting.GRAY), false);
            return 1;
        } catch (Exception exception) {
            source.sendFailure(Component.literal("Apenas jogadores podem usar esse comando."));
            return 0;
        }
    }

    private static int all(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            int kills = TitleData.kills(player);
            boolean collector = TitleData.capturedAll(player);

            source.sendSuccess(() -> Component.literal("[Títulos] Todos os títulos:").withStyle(ChatFormatting.GRAY), false);
            for (Title title : Titles.ALL) {
                boolean unlocked = Titles.isUnlocked(title, kills, collector);
                Component requirement = title.collector()
                        ? Component.literal("todos os players").withStyle(ChatFormatting.DARK_GRAY)
                        : Component.literal(title.killsRequired() + " kills").withStyle(ChatFormatting.DARK_GRAY);
                source.sendSuccess(() -> Component.literal(unlocked ? "[x] " : "[ ] ")
                        .withStyle(unlocked ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY)
                        .append(title.display())
                        .append(Component.literal(" (" + title.id() + ") - ").withStyle(ChatFormatting.DARK_GRAY))
                        .append(requirement), false);
            }
            return 1;
        } catch (Exception exception) {
            source.sendFailure(Component.literal("Apenas jogadores podem usar esse comando."));
            return 0;
        }
    }

    private static int stats(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            int kills = TitleData.kills(player);
            boolean collector = TitleData.capturedAll(player);
            int captured = TitleData.captured(player).getAllKeys().size();
            int known = Math.max(0, TitleData.knownPlayers(player.getServer()).size() - 1);

            source.sendSuccess(() -> Component.literal("[Títulos] Kills: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.valueOf(kills)).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" | Cabeças: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(captured + "/" + known).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" | Colecionador: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(collector ? "completo" : "incompleto")
                            .withStyle(collector ? ChatFormatting.GREEN : ChatFormatting.RED)), false);
            return 1;
        } catch (Exception exception) {
            source.sendFailure(Component.literal("Apenas jogadores podem usar esse comando."));
            return 0;
        }
    }

    private static Title findTitle(String input) {
        String target = normalize(input);
        for (Title title : Titles.ALL) {
            if (normalize(title.id()).equals(target) || normalize(title.display().getString()).equals(target)) {
                return title;
            }
        }
        return null;
    }

    private static String normalize(String input) {
        String decomposed = Normalizer.normalize(input, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }
}
