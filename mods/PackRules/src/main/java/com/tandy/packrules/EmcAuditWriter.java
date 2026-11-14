package com.tandy.packrules;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

/** Administrative text history. This class never accesses or changes player data. */
public final class EmcAuditWriter {
    private record Previous(BigInteger balance, Instant timestamp) { }
    private static final ZoneId ZONE = ZoneId.of("America/Manaus");
    private final Path directory;
    private final Map<UUID, Previous> previous = new HashMap<>();

    public EmcAuditWriter(Path directory) throws IOException {
        this.directory = directory;
        Path state = directory.resolve("last-balances.properties");
        if (Files.exists(state)) {
            Properties props = new Properties();
            try (var reader = Files.newBufferedReader(state, StandardCharsets.UTF_8)) { props.load(reader); }
            for (String key : props.stringPropertyNames()) {
                String[] fields = props.getProperty(key).split(";", 2);
                try {
                    previous.put(UUID.fromString(key), new Previous(new BigInteger(fields[0]), Instant.parse(fields[1])));
                } catch (RuntimeException ignored) {
                    // A damaged checkpoint does not prevent logging the other players.
                }
            }
        }
    }

    public synchronized String append(UUID uuid, String name, BigInteger balance, String event, Instant now) throws IOException {
        Previous last = previous.get(uuid);
        String delta = last == null ? "NA" : signed(balance.subtract(last.balance()));
        String line = now.atZone(ZONE) + " | evento=" + event
            + " | jogador=" + name.replaceAll("[\\r\\n|]", "_") + " | uuid=" + uuid
            + " | emc=" + balance + " | variacao=" + delta;
        Files.createDirectories(directory);
        Files.writeString(directory.resolve(now.atZone(ZONE).toLocalDate() + ".txt"), line + "\n",
            StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        previous.put(uuid, new Previous(balance, now));
        Properties props = new Properties();
        previous.forEach((id, sample) -> props.setProperty(id.toString(), sample.balance() + ";" + sample.timestamp()));
        Path temp = directory.resolve("last-balances.properties.tmp");
        try (var writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) { props.store(writer, "Last EMC audit sample per UUID"); }
        try {
            Files.move(temp, directory.resolve("last-balances.properties"), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(temp, directory.resolve("last-balances.properties"), StandardCopyOption.REPLACE_EXISTING);
        }
        return line;
    }

    private static String signed(BigInteger value) { return value.signum() > 0 ? "+" + value : value.toString(); }
}
