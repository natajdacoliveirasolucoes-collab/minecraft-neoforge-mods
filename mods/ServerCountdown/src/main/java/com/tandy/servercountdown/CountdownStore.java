package com.tandy.servercountdown;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** One atomic file contains the schedule and per-UUID preferences. Never stores login data. */
final class CountdownStore {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create();
    private final Path file;
    CountdownStore(Path file) { this.file = file.toAbsolutePath(); }

    CountdownState load() throws IOException {
        if (!Files.exists(file)) {
            CountdownState state = CountdownState.defaults();
            save(state);
            return state;
        }
        try {
            JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if (json.get("schemaVersion").getAsInt() != 1) throw new IllegalArgumentException("Versão de config desconhecida.");
            Set<UUID> hidden = new HashSet<>();
            for (JsonElement value : json.getAsJsonArray("hiddenPlayers")) hidden.add(UUID.fromString(value.getAsString()));
            return new CountdownState(json.get("enabled").getAsBoolean(), json.get("title").getAsString(),
                instant(json.get("startedAt")), instant(json.get("deadline")),
                ZoneId.of(json.get("timezone").getAsString()), hidden);
        } catch (RuntimeException e) {
            throw new IOException("Config inválida; arquivo preservado. " + e.getMessage(), e);
        }
    }

    private static Instant instant(JsonElement value) {
        if (value == null) throw new IllegalArgumentException("Campo de data ausente.");
        return value.isJsonNull() ? null : Instant.parse(value.getAsString());
    }

    void save(CountdownState state) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("schemaVersion", 1);
        json.addProperty("enabled", state.enabled());
        json.addProperty("title", state.title());
        json.add("startedAt", state.startedAt() == null ? JsonNull.INSTANCE : new JsonPrimitive(state.startedAt().toString()));
        json.add("deadline", state.deadline() == null ? JsonNull.INSTANCE : new JsonPrimitive(state.deadline().toString()));
        json.addProperty("timezone", state.timezone().getId());
        JsonArray hidden = new JsonArray();
        state.hiddenPlayers().stream().map(UUID::toString).sorted().forEach(hidden::add);
        json.add("hiddenPlayers", hidden);
        Files.createDirectories(file.getParent());
        Path tmp = Files.createTempFile(file.getParent(), "servercountdown-", ".tmp");
        try {
            Files.writeString(tmp, JSON.toJson(json) + "\n");
            try { Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(tmp); }
    }
}
