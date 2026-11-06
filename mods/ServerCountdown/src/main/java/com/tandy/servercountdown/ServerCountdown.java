package com.tandy.servercountdown;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.*;
import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

@Mod("servercountdown")
public final class ServerCountdown {
    private static final Logger LOG = LogUtils.getLogger();
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm");
    private final CountdownStore store = new CountdownStore(FMLPaths.CONFIGDIR.get().resolve("servercountdown.json"));
    private final ServerBossEvent bar = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.YELLOW,
        BossEvent.BossBarOverlay.PROGRESS);
    private CountdownState state = CountdownState.defaults();
    private boolean loaded;
    private long lastSecond = Long.MIN_VALUE;

    public ServerCountdown() {
        NeoForge.EVENT_BUS.addListener(this::commands);
        NeoForge.EVENT_BUS.addListener(this::started);
        NeoForge.EVENT_BUS.addListener(this::stopped);
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::login);
        NeoForge.EVENT_BUS.addListener(this::logout);
        bar.setVisible(false);
    }

    private void started(ServerStartedEvent event) {
        try { state = store.load(); loaded = true; }
        catch (IOException e) { LOG.error("ServerCountdown: configuração preservada; corrija e use countdown recarregar", e); }
        refresh(event.getServer());
    }
    private void stopped(ServerStoppedEvent event) {
        bar.removeAllPlayers(); bar.setVisible(false);
        state = CountdownState.defaults(); loaded = false; lastSecond = Long.MIN_VALUE;
    }
    private void tick(ServerTickEvent.Post event) {
        long second = Instant.now().getEpochSecond();
        if (second != lastSecond) { lastSecond = second; refresh(event.getServer()); }
    }
    private void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) refresh(player.getServer());
    }
    private void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) bar.removePlayer(player);
    }
    private void refresh(MinecraftServer server) {
        Instant now = Instant.now();
        if (!loaded || !state.active(now)) { bar.removeAllPlayers(); bar.setVisible(false); return; }
        bar.setName(Component.literal(state.bossbarTitle(now)).withStyle(ChatFormatting.BOLD));
        bar.setProgress(state.progress(now));
        bar.setVisible(true);
        for (ServerPlayer player : List.copyOf(bar.getPlayers()))
            if (state.hiddenPlayers().contains(player.getUUID()) || !server.getPlayerList().getPlayers().contains(player))
                bar.removePlayer(player);
        for (ServerPlayer player : server.getPlayerList().getPlayers())
            if (!state.hiddenPlayers().contains(player.getUUID())) bar.addPlayer(player);
    }

    private void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("countdown")
            .executes(ctx -> status(ctx.getSource()))
            .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
            .then(Commands.literal("ocultar").executes(ctx -> visibility(ctx.getSource(), false)))
            .then(Commands.literal("mostrar").executes(ctx -> visibility(ctx.getSource(), true)))
            .then(Commands.literal("parar").requires(s -> s.hasPermission(3))
                .executes(ctx -> change(ctx.getSource(), state.stop(), "Contagem desativada.")))
            .then(Commands.literal("recarregar").requires(s -> s.hasPermission(3)).executes(ctx -> reload(ctx.getSource())))
            .then(Commands.literal("em").requires(s -> s.hasPermission(3))
                .then(Commands.argument("segundos", IntegerArgumentType.integer(1, 31536000))
                    .then(Commands.argument("titulo", StringArgumentType.greedyString()).executes(ctx -> relative(ctx)))))
            .then(Commands.literal("definir").requires(s -> s.hasPermission(3))
                .then(Commands.argument("agendamento", StringArgumentType.greedyString()).executes(this::define))));
    }

    private int status(CommandSourceStack source) {
        if (!loaded) { source.sendFailure(Component.literal("Config indisponível. Corrija config/servercountdown.json e recarregue.")); return 0; }
        String text = state.deadline() == null ? "Nenhuma contagem configurada."
            : state.title() + " | " + (state.active(Instant.now()) ? state.remaining(Instant.now()) : state.enabled() ? "Encerrada" : "Desativada")
                + " | " + displayDeadline(state.deadline());
        source.sendSuccess(() -> Component.literal(text), false);
        if (source.getEntity() instanceof ServerPlayer player) {
            boolean hidden = state.hiddenPlayers().contains(player.getUUID());
            source.sendSuccess(() -> Component.literal(hidden ? "[Mostrar contagem]" : "[Ocultar contagem]")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, hidden ? "/countdown mostrar" : "/countdown ocultar"))), false);
        }
        return 1;
    }
    private int visibility(CommandSourceStack source, boolean show) throws CommandSyntaxException {
        return change(source, state.visibility(source.getPlayerOrException().getUUID(), show),
            show ? "Contagem habilitada para você. Use /countdown ocultar para esconder."
                 : "Contagem oculta para você. Use /countdown mostrar para reexibir.");
    }
    private int define(CommandContext<CommandSourceStack> ctx) {
        try {
            // Only vanilla argument types are sent to clients. The timezone stays internal.
            CountdownState next = state.schedule(StringArgumentType.getString(ctx, "agendamento"), Instant.now());
            return change(ctx.getSource(), next,
                "Contagem definida: " + displayDeadline(next.deadline()) + ". Aplicada agora.");
        } catch (DateTimeException | IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.literal("Horário/título inválido: " + e.getMessage() + " Use: /countdown definir AAAA-MM-DD HH:mm Título"));
            return 0;
        }
    }
    private int relative(CommandContext<CommandSourceStack> ctx) {
        try {
            Instant now = Instant.now();
            return change(ctx.getSource(), state.start(StringArgumentType.getString(ctx, "titulo"), now,
                now.plusSeconds(IntegerArgumentType.getInteger(ctx, "segundos")), CountdownState.DEFAULT_ZONE), "Contagem iniciada. Aplicada agora.");
        } catch (IllegalArgumentException e) { ctx.getSource().sendFailure(Component.literal(e.getMessage())); return 0; }
    }
    private static String displayDeadline(Instant deadline) {
        return DISPLAY.format(deadline.atZone(CountdownState.DEFAULT_ZONE)) + " " + CountdownState.ZONE_LABEL;
    }
    private int change(CommandSourceStack source, CountdownState next, String message) {
        if (!loaded) { source.sendFailure(Component.literal("Corrija e recarregue a config antes de alterá-la.")); return 0; }
        try {
            store.save(next); state = next; refresh(source.getServer());
            source.sendSuccess(() -> Component.literal(message), false); return 1;
        } catch (IOException e) {
            LOG.error("ServerCountdown: falha ao salvar; estado anterior mantido", e);
            source.sendFailure(Component.literal("Não foi possível salvar. Estado anterior preservado.")); return 0;
        }
    }
    private int reload(CommandSourceStack source) {
        try {
            CountdownState next = store.load(); state = next; loaded = true; refresh(source.getServer());
            source.sendSuccess(() -> Component.literal("Contagem e preferências recarregadas sem reiniciar."), false); return 1;
        } catch (IOException e) {
            source.sendFailure(Component.literal("Config inválida; estado anterior preservado. " + e.getMessage())); return 0;
        }
    }
}
