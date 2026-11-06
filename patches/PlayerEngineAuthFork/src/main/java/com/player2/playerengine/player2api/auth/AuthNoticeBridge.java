package com.player2.playerengine.player2api.auth;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** Called only by the two authorization-notice call sites patched in AuthenticationManager. */
public final class AuthNoticeBridge {
    private static final Path CONFIG = Path.of("playerengine", "auth-notices.properties");
    private AuthNoticeBridge() {}

    public static void sendNotice(Player player, Component message) {
        if (showAuthNotices()) player.sendSystemMessage(message);
    }

    public static boolean showAuthNotices() {
        try {
            if (!Files.exists(CONFIG)) {
                Files.createDirectories(CONFIG.getParent());
                try {
                    Files.writeString(CONFIG,
                            "# PlayerEngine authorization link and dedicated-server hint only.\nshowAuthNotices=false\n",
                            StandardOpenOption.CREATE_NEW);
                } catch (FileAlreadyExistsException ignored) {
                    // Another authentication thread created the default first.
                }
            }
            Properties values = new Properties();
            try (InputStream input = Files.newInputStream(CONFIG)) { values.load(input); }
            // Distribute a quiet default to every client; admins can explicitly opt in.
            return "true".equalsIgnoreCase(values.getProperty("showAuthNotices", "false").trim());
        } catch (IOException | IllegalArgumentException e) {
            System.getLogger(AuthNoticeBridge.class.getName()).log(System.Logger.Level.WARNING,
                    "Could not read auth-notices.properties; Player2 authorization notices remain hidden.");
            return false;
        }
    }
}
