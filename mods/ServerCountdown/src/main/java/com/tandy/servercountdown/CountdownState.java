package com.tandy.servercountdown;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.*;

/** Wall-clock deadline: neither low TPS nor restarting extends the countdown. */
record CountdownState(boolean enabled, String title, Instant startedAt, Instant deadline,
                      ZoneId timezone, Set<UUID> hiddenPlayers) {
    static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Manaus");
    static final String ZONE_LABEL = "(GMT-4)";
    private static final DateTimeFormatter INPUT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm")
        .withResolverStyle(ResolverStyle.STRICT);

    CountdownState {
        Objects.requireNonNull(title);
        Objects.requireNonNull(timezone);
        if (title.isBlank() || title.length() > 160 || title.codePoints().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Use um título de 1 a 160 caracteres, sem controles.");
        if ((startedAt == null) != (deadline == null) || (enabled && deadline == null))
            throw new IllegalArgumentException("A contagem precisa de início e horário final.");
        if (deadline != null && !deadline.isAfter(startedAt))
            throw new IllegalArgumentException("O horário final deve ser posterior ao início.");
        hiddenPlayers = Set.copyOf(hiddenPlayers);
    }

    static CountdownState defaults() {
        return new CountdownState(false, "Início do evento", null, null,
            DEFAULT_ZONE, Set.of());
    }

    CountdownState schedule(String input, Instant now) {
        String[] parts = input.strip().split("\\s+", 3);
        if (parts.length != 3) throw new IllegalArgumentException("Informe data, hora e título.");
        return start(parts[2], now, parseDeadline(parts[0], parts[1], DEFAULT_ZONE), DEFAULT_ZONE);
    }

    String bossbarTitle(Instant now) {
        return title + " - " + remaining(now) + " " + ZONE_LABEL;
    }

    static Instant parseDeadline(String date, String time, ZoneId zone) {
        LocalDateTime local = LocalDateTime.parse(date + " " + time, INPUT);
        var offsets = zone.getRules().getValidOffsets(local);
        if (offsets.size() != 1)
            throw new IllegalArgumentException("Horário inexistente ou ambíguo nesse fuso. Use UTC para desambiguar.");
        return local.toInstant(offsets.getFirst());
    }

    CountdownState start(String newTitle, Instant now, Instant target, ZoneId zone) {
        if (!target.isAfter(now)) throw new IllegalArgumentException("Escolha um horário no futuro.");
        return new CountdownState(true, newTitle, now, target, zone, hiddenPlayers);
    }

    CountdownState stop() {
        return new CountdownState(false, title, startedAt, deadline, timezone, hiddenPlayers);
    }

    CountdownState visibility(UUID player, boolean show) {
        Set<UUID> hidden = new HashSet<>(hiddenPlayers);
        if (show) hidden.remove(player); else hidden.add(player);
        return new CountdownState(enabled, title, startedAt, deadline, timezone, hidden);
    }

    boolean active(Instant now) { return enabled && deadline != null && now.isBefore(deadline); }

    String remaining(Instant now) {
        if (deadline == null || !now.isBefore(deadline)) return "00:00:00";
        Duration duration = Duration.between(now, deadline);
        long seconds = duration.getSeconds() + (duration.getNano() == 0 ? 0 : 1);
        long days = seconds / 86400;
        return (days == 0 ? "" : days + "d ") + String.format(Locale.ROOT, "%02d:%02d:%02d",
            seconds / 3600 % 24, seconds / 60 % 60, seconds % 60);
    }

    float progress(Instant now) {
        if (!active(now)) return 0;
        Duration totalDuration = Duration.between(startedAt, deadline);
        Duration remaining = Duration.between(now, deadline);
        double total = totalDuration.getSeconds() + totalDuration.getNano() / 1e9;
        return (float)Math.clamp((remaining.getSeconds() + remaining.getNano() / 1e9) / total, 0, 1);
    }
}
