package net.palasitemclear.config;

public record MessagesConfig(String warning, String cleared, String emptyCleared) {
    public MessagesConfig {
        // Existing JSON has only warning/cleared. Keep its language and styling for zero too.
        if (emptyCleared == null || emptyCleared.isBlank()) emptyCleared = cleared;
    }

    public MessagesConfig(String warning, String cleared) {
        this(warning, cleared, cleared);
    }
    public static MessagesConfig defaults() {
        return new MessagesConfig(
                "<yellow>Items on the ground will be cleared in <gold>{seconds}</gold> seconds.",
                "<gray>Cleared <white>{count}</white> dropped items."
        );
    }
}
