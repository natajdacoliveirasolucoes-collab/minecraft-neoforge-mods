package com.tandy.livewhitelist;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.server.players.UserWhiteListEntry;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Changes whitelist commands only. Never changes login profiles, passwords or player data. */
@Mod("livewhitelist")
public final class LiveWhitelist {
    private final Map<String, Long> revisions = new HashMap<>();
    private long sequence;
    public LiveWhitelist() {
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        // Merge with vanilla: keep on/off/list/reload, permissions, selectors and suggestions.
        event.getDispatcher().register(Commands.literal("whitelist")
            .requires(source -> source.hasPermission(3))
            .then(Commands.literal("add")
                .then(Commands.argument("targets", GameProfileArgument.gameProfile())
                    .executes(this::add)))
            .then(Commands.literal("remove")
                .then(Commands.argument("targets", GameProfileArgument.gameProfile())
                    .executes(this::remove))));
    }

    private static String target(CommandContext<CommandSourceStack> context) {
        return context.getNodes().stream()
            .filter(node -> node.getNode().getName().equals("targets"))
            .map(node -> node.getRange().get(context.getInput()).trim())
            .findFirst().orElseThrow();
    }

    private int add(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        String name = target(context);
        long revision = ++sequence;
        revisions.put(name.toLowerCase(Locale.ROOT), revision);
        if (server.usesAuthentication() || name.startsWith("@")) {
            return applyAdd(source, GameProfileArgument.getGameProfiles(context, "targets"));
        }
        if (!name.matches("[A-Za-z0-9_]{1,16}")) {
            source.sendFailure(Component.literal("Nick invalido. Use a grafia exata do login (1-16 letras, numeros ou _)."));
            return 0;
        }
        // Online profiles already contain the identity selected by the authentication mod.
        ServerPlayer player = server.getPlayerList().getPlayerByName(name);
        if (player != null) return applyAdd(source, List.of(player.getGameProfile()));
        if (!ModList.get().isLoaded("directauth")) {
            return applyAdd(source, List.of(UUIDUtil.createOfflineProfile(name)));
        }
        // Read through DirectAuth's public async API. Never open/write its database ourselves.
        source.sendSuccess(() -> Component.literal("Consultando a identidade existente de " + name + "..."), false);
        accountId(name).whenComplete((id, failure) -> server.execute(() -> {
            // A later remove/add for this player wins, even if this DB read finishes afterwards.
            if (!Long.valueOf(revision).equals(revisions.get(name.toLowerCase(Locale.ROOT)))) return;
            if (failure != null) {
                source.sendFailure(Component.literal("Nao foi possivel consultar o DirectAuth; whitelist preservada. "
                    + "Confira a compatibilidade com DirectAuth 1.3.1/1.3.2."));
                return;
            }
            // The player may have connected while the database lookup was pending.
            ServerPlayer current = server.getPlayerList().getPlayerByName(name);
            GameProfile profile = current != null ? current.getGameProfile()
                : id != null ? new GameProfile(id, name) : UUIDUtil.createOfflineProfile(name);
            applyAdd(source, List.of(profile));
        }));
        return 1;
    }

    static CompletableFuture<UUID> accountId(String name) {
        try {
            Class<?> auth = Class.forName("com.marcp.directauth.DirectAuth");
            Object database = auth.getMethod("getDatabase").invoke(null);
            if (database == null) throw new IllegalStateException("DirectAuth database is not ready");
            Object result = database.getClass().getMethod("getUserAsync", String.class).invoke(database, name);
            return ((CompletableFuture<?>) result).thenApply(user -> {
                if (user == null) return null;
                try {
                    String id = (String) user.getClass().getMethod("getAccountUUID").invoke(user);
                    return id == null ? null : UUID.fromString(id);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Unsupported DirectAuth account API", e);
                }
            });
        } catch (ReflectiveOperationException | RuntimeException e) {
            return CompletableFuture.failedFuture(e instanceof InvocationTargetException wrapped
                ? wrapped.getCause() : e);
        }
    }

    private static int applyAdd(CommandSourceStack source, Collection<GameProfile> profiles) {
        UserWhiteList list = source.getServer().getPlayerList().getWhiteList();
        if (!backup(source, list)) return 0;
        int changed = 0;
        for (GameProfile profile : profiles) {
            // Explicit add repairs stale entries for this name only; unrelated entries are untouched.
            List<UserWhiteListEntry> stale = list.getEntries().stream()
                .filter(entry -> entry.getUser().getName().equalsIgnoreCase(profile.getName())
                    && !entry.getUser().getId().equals(profile.getId())).toList();
            for (UserWhiteListEntry entry : stale) list.remove(entry);
            boolean exists = list.isWhiteListed(profile);
            if (!exists || !stale.isEmpty()) {
                list.add(new UserWhiteListEntry(profile));
                changed++;
            }
        }
        if (!persist(source, list)) return 0;
        for (GameProfile profile : profiles) {
            source.sendSuccess(() -> Component.literal(profile.getName() + " liberado: " + profile.getId()
                + " (aplicado agora; sem reiniciar)."), true);
        }
        return changed;
    }

    private int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        UserWhiteList list = source.getServer().getPlayerList().getWhiteList();
        String name = target(context);
        revisions.put(name.toLowerCase(Locale.ROOT), ++sequence);
        List<UserWhiteListEntry> matches;
        if (name.startsWith("@")) {
            Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(context, "targets");
            matches = list.getEntries().stream()
                .filter(entry -> profiles.stream().anyMatch(profile -> profile.getId().equals(entry.getUser().getId())))
                .toList();
        } else {
            // Remove the actual stored entries, even if the vanilla cache resolves a different UUID.
            matches = list.getEntries().stream()
                .filter(entry -> entry.getUser().getName().equalsIgnoreCase(name)).toList();
        }
        if (matches.isEmpty()) {
            source.sendFailure(Component.literal("Jogador nao consta na whitelist: " + name));
            return 0;
        }
        if (!backup(source, list)) return 0;
        for (UserWhiteListEntry entry : matches) list.remove(entry);
        if (!persist(source, list)) return 0;
        source.getServer().kickUnlistedPlayers(source);
        source.sendSuccess(() -> Component.literal(name + " removido da whitelist (aplicado agora)."), true);
        return matches.size();
    }

    private static boolean backup(CommandSourceStack source, UserWhiteList list) {
        try {
            Path file = list.getFile().toPath();
            if (Files.exists(file)) {
                Path directory = file.toAbsolutePath().getParent().resolve("whitelist-backups");
                Files.createDirectories(directory);
                Path copy = Files.createTempFile(directory, "whitelist-", ".json");
                Files.copy(file, copy, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (java.io.IOException e) {
            source.sendFailure(Component.literal("Falha ao criar backup; whitelist preservada: " + e.getMessage()));
            return false;
        }
    }

    private static boolean persist(CommandSourceStack source, UserWhiteList list) {
        try {
            list.save();
            return true;
        } catch (java.io.IOException e) {
            source.sendFailure(Component.literal("Whitelist alterada em memoria, mas falhou ao salvar: " + e.getMessage()));
            return false;
        }
    }
}
