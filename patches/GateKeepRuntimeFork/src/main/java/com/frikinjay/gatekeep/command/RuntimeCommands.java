package com.frikinjay.gatekeep.command;

import com.frikinjay.gatekeep.GateKeep;
import com.frikinjay.gatekeep.config.GateKeepConfig;
import com.frikinjay.gatekeep.dimension.DimensionUnlockManager;
import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Server administration for the existing GateKeep config and portal checks. */
public final class RuntimeCommands {
    private static final Path FILE = Path.of("config", "gatekeep.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static JsonObject lastApplied;
    private static final List<ResourceLocation> DIMENSIONS = List.of(
            ResourceLocation.parse("minecraft:the_nether"), ResourceLocation.parse("minecraft:the_end"));
    private RuntimeCommands() {}

    public static void register() {
        try { lastApplied = read(); }
        catch (Exception error) { GateKeep.LOGGER.error("GateKeep runtime commands could not validate initial config", error); }
        NeoForge.EVENT_BUS.addListener(RuntimeCommands::commands);
    }

    private static void commands(RegisterCommandsEvent event) {
        var root = Commands.literal("gatekeep").requires(source -> source.hasPermission(3))
                .executes(ctx -> status(ctx.getSource()))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                .then(Commands.literal("reload").executes(ctx -> reload(ctx.getSource())));
        for (String operation : List.of("lock", "unlock")) {
            boolean locked = operation.equals("lock");
            root.then(Commands.literal(operation).then(Commands.argument("dimension", ResourceLocationArgument.id())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(DIMENSIONS, builder))
                    .executes(ctx -> update(ctx.getSource(), ResourceLocationArgument.getId(ctx, "dimension"), locked))));
        }
        event.getDispatcher().register(root);
    }

    private static int status(CommandSourceStack source) {
        for (ResourceLocation id : DIMENSIONS) {
            String name = id.getPath().equals("the_nether") ? "Nether" : "End";
            boolean available = GateKeepConfig.isDimensionAvailable(id.toString());
            String reason = GateKeepConfig.isDimensionDisabled(id.toString()) ? " (manual)"
                    : GateKeepConfig.isDimensionTimeRestricted(id.toString()) ? " (horario)" : "";
            source.sendSuccess(() -> Component.literal(name + ": " + (available ? "LIBERADO" : "BLOQUEADO") + reason), false);
        }
        return 1;
    }

    private static int update(CommandSourceStack source, ResourceLocation id, boolean locked) {
        try {
            if (!DIMENSIONS.contains(id)) throw new IllegalArgumentException("Use minecraft:the_nether ou minecraft:the_end.");
            JsonObject before = read();
            if (lastApplied != null && !before.equals(lastApplied))
                throw new IllegalStateException("Config foi editado fora do mod. Execute /gatekeep reload antes.");
            byte[] original = Files.readAllBytes(FILE);
            JsonObject next = before.deepCopy();
            JsonArray disabled = new JsonArray();
            if (next.has("disabledDimensions")) for (JsonElement entry : next.getAsJsonArray("disabledDimensions"))
                if (!entry.getAsString().equals(id.toString())) disabled.add(entry.deepCopy());
            if (locked) disabled.add(id.toString());
            next.add("disabledDimensions", disabled);
            // A manual choice overrides an existing timer for this dimension only.
            if (next.has("timeRestrictions")) {
                JsonObject times = next.getAsJsonObject("timeRestrictions");
                if (times.has(id.toString())) times.getAsJsonObject(id.toString()).addProperty("enabled", false);
            }
            validate(next);
            try {
                write((GSON.toJson(next) + "\n").getBytes(StandardCharsets.UTF_8));
                apply(source.getServer(), next);
                if (GateKeepConfig.isDimensionAvailable(id.toString()) == locked)
                    throw new IllegalStateException("O estado em memoria nao corresponde a alteracao.");
            } catch (Exception error) {
                try { write(original); apply(source.getServer(), before); }
                catch (Exception rollback) { error.addSuppressed(rollback); }
                throw error;
            }
            source.sendSuccess(() -> Component.literal(id + (locked ? " bloqueado" : " liberado")
                    + ". Alteracao aplicada e salva, sem reiniciar."), false);
            return 1;
        } catch (Exception error) { return failure(source, error); }
    }

    private static int reload(CommandSourceStack source) {
        try {
            JsonObject next = read(); // Fully validate before touching the current in-memory policy.
            apply(source.getServer(), next);
            source.sendSuccess(() -> Component.literal("GateKeep recarregado. Nenhum reinicio necessario."), false);
            return status(source);
        } catch (Exception error) { return failure(source, error); }
    }

    private static void apply(MinecraftServer server, JsonObject next) throws Exception {
        // Native GateKeep iterates its mutable timer map on a background thread.
        // Stop that reader during reload, then resume it with the same server.
        DimensionUnlockManager.shutdown();
        try {
            Map<String, GateKeepConfig.TimeRestriction> old = new HashMap<>(GateKeepConfig.getTimeRestrictions());
            var queueField = DimensionUnlockManager.class.getDeclaredField("pendingAnnouncements");
            queueField.setAccessible(true);
            ((Queue<?>)queueField.get(null)).clear();
            GateKeepConfig.loadConfig();
            Set<String> expected = new HashSet<>();
            if (next.has("disabledDimensions")) for (JsonElement value : next.getAsJsonArray("disabledDimensions")) expected.add(value.getAsString());
            var field = GateKeepConfig.class.getDeclaredField("DISABLED_DIMENSIONS");
            field.setAccessible(true);
            if (!expected.equals(field.get(null))) throw new IllegalStateException("Falha ao carregar a lista de dimensoes.");
            for (var entry : GateKeepConfig.getTimeRestrictions().entrySet()) {
                var previous = old.get(entry.getKey());
                if (previous != null && previous.hasBeenAnnounced() && lastApplied != null
                        && Objects.equals(timer(lastApplied, entry.getKey()), timer(next, entry.getKey()))) entry.getValue().setAnnounced();
            }
            lastApplied = next.deepCopy();
        } finally { DimensionUnlockManager.initialize(server); }
    }

    private static JsonElement timer(JsonObject config, String id) {
        return config.has("timeRestrictions") ? config.getAsJsonObject("timeRestrictions").get(id) : null;
    }

    private static JsonObject read() throws Exception {
        if (Files.size(FILE) > 1024 * 1024) throw new IllegalArgumentException("gatekeep.json excede 1 MiB.");
        JsonElement parsed = JsonParser.parseString(Files.readString(FILE));
        if (!parsed.isJsonObject()) throw new IllegalArgumentException("A configuracao deve ser um objeto JSON.");
        JsonObject object = parsed.getAsJsonObject(); validate(object); return object;
    }

    private static void validate(JsonObject object) {
        if (object.has("disabledDimensions")) {
            if (!object.get("disabledDimensions").isJsonArray()) throw new IllegalArgumentException("disabledDimensions deve ser uma lista.");
            for (JsonElement value : object.getAsJsonArray("disabledDimensions"))
                if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
                        || ResourceLocation.tryParse(value.getAsString()) == null) throw new IllegalArgumentException("Identificador de dimensao invalido.");
        }
        for (String flag : List.of("announce"))
            if (object.has(flag) && (!object.get(flag).isJsonPrimitive() || !object.getAsJsonPrimitive(flag).isBoolean()))
                throw new IllegalArgumentException(flag + " deve ser true ou false.");
        for (String key : List.of("restrictedDimensionMessage", "timedDimensionRestrictedMessage", "unrestrictedAnnouncementMessage"))
            if (object.has(key) && (!object.get(key).isJsonPrimitive() || !object.getAsJsonPrimitive(key).isString()))
                throw new IllegalArgumentException(key + " deve ser texto.");
        if (object.has("timeRestrictions")) {
            if (!object.get("timeRestrictions").isJsonObject()) throw new IllegalArgumentException("timeRestrictions deve ser um objeto.");
            for (var entry : object.getAsJsonObject("timeRestrictions").entrySet()) {
                if (ResourceLocation.tryParse(entry.getKey()) == null || !entry.getValue().isJsonObject())
                    throw new IllegalArgumentException("Restricao de horario invalida.");
                JsonObject value = entry.getValue().getAsJsonObject();
                if (value.has("enabled") && (!value.get("enabled").isJsonPrimitive() || !value.getAsJsonPrimitive("enabled").isBoolean()))
                    throw new IllegalArgumentException("enabled deve ser true ou false.");
                if (value.has("enabled") && value.get("enabled").getAsBoolean()) {
                    ZoneId.of(value.has("timeZone") ? value.get("timeZone").getAsString() : "UTC");
                    LocalDateTime.parse(value.has("unlockDateTime") ? value.get("unlockDateTime").getAsString() : "2023-01-01T00:00:00");
                }
            }
        }
    }

    private static void write(byte[] bytes) throws Exception {
        Path temporary = Files.createTempFile(FILE.toAbsolutePath().getParent(), "gatekeep-", ".tmp");
        try {
            Files.write(temporary, bytes);
            try { Files.move(temporary, FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException unsupported) { Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }

    private static int failure(CommandSourceStack source, Exception error) {
        GateKeep.LOGGER.error("GateKeep command failed; inspect config before retrying", error);
        source.sendFailure(Component.literal("GateKeep: " + error.getMessage())); return 0;
    }
}
