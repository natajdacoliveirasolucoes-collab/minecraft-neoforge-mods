package com.tandy.trophyheads;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;

/** Self-contained head UVs, carried by the existing vanilla profile component. */
public final class SkinSnapshot {
    public static final String MARKER = "trophyheads:snapshot";
    public static final String PNG_PREFIX = "trophyheads:png";
    public static final int MAX_SIZE = 512;
    public static final int CHUNK_SIZE = 32000;
    public static final int MAX_CHUNKS = 12;
    private SkinSnapshot() {}

    public static GameProfile capture(GameProfile live, byte[] source) throws Exception {
        GameProfile copy = copy(live);
        if (source == null) return copy;
        if (source.length == 0 || source.length > 16 * 1024 * 1024)
            throw new IllegalArgumentException("Skin PNG exceeds upload bounds");
        // Read dimensions before allocation; decode only the head/hat UV strip.
        BufferedImage strip;
        int width;
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(source))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("Invalid skin PNG");
            var reader = readers.next();
            try {
                reader.setInput(input);
                width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (!reader.getFormatName().equalsIgnoreCase("png") || width < 64 || width > 4096
                        || width % 64 != 0 || (height != width && height != width / 2))
                    throw new IllegalArgumentException("Unsupported skin dimensions");
                var parameters = reader.getDefaultReadParam();
                parameters.setSourceRegion(new Rectangle(0, 0, width, width / 4));
                strip = reader.read(0, parameters);
            } finally { reader.dispose(); }
        }
        int size = Math.min(MAX_SIZE, width);
        while (true) {
            BufferedImage head = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            var graphics = head.createGraphics();
            try {
                graphics.setComposite(java.awt.AlphaComposite.Src);
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                graphics.drawImage(strip, 0, 0, size, size / 4, null);
            } finally { graphics.dispose(); }
            var output = new ByteArrayOutputStream();
            ImageIO.write(head, "png", output);
            byte[] png = output.toByteArray();
            String encoded = Base64.getEncoder().encodeToString(png);
            int chunks = (encoded.length() + CHUNK_SIZE - 1) / CHUNK_SIZE;
            // Vanilla network codec allows sixteen properties of <=32767 characters.
            if (chunks <= MAX_CHUNKS && copy.getProperties().size() + chunks + 1 <= 16) {
                copy.getProperties().put(MARKER, new Property(MARKER, "1:" + digest(png) + ":" + chunks));
                for (int index = 0; index < chunks; index++) {
                    String key = PNG_PREFIX + index;
                    copy.getProperties().put(key, new Property(key,
                            encoded.substring(index * CHUNK_SIZE, Math.min(encoded.length(), (index + 1) * CHUNK_SIZE))));
                }
                return copy;
            }
            if (size <= 64) throw new IllegalArgumentException("Insufficient profile property capacity for snapshot");
            size = Math.max(64, Integer.highestOneBit(size - 1));
        }
    }

    public static GameProfile copy(GameProfile live) {
        GameProfile copy = new GameProfile(live.getId(), live.getName());
        // Never carry a stale snapshot if another plugin puts one on the live player profile.
        live.getProperties().entries().stream().filter(entry -> !entry.getKey().equals(MARKER)
                && !entry.getKey().startsWith(PNG_PREFIX)).forEach(entry -> copy.getProperties().put(entry.getKey(), entry.getValue()));
        return copy;
    }

    public static String key(GameProfile profile) {
        var markers = profile.getProperties().get(MARKER);
        if (markers.size() != 1) return null;
        String[] fields = markers.iterator().next().value().split(":", -1);
        if (fields.length != 3 || !fields[0].equals("1") || !fields[1].matches("[0-9a-f]{64}")) return null;
        try {
            int count = Integer.parseInt(fields[2]);
            return count > 0 && count <= MAX_CHUNKS ? fields[1] : null;
        } catch (NumberFormatException invalid) { return null; }
    }

    public static byte[] png(GameProfile profile) {
        String key = key(profile);
        if (key == null) return null;
        try {
            String marker = profile.getProperties().get(MARKER).iterator().next().value();
            int count = Integer.parseInt(marker.substring(marker.lastIndexOf(':') + 1));
            StringBuilder encoded = new StringBuilder();
            for (int index = 0; index < count; index++) {
                var values = profile.getProperties().get(PNG_PREFIX + index);
                if (values.size() != 1) return null;
                String part = values.iterator().next().value();
                if (part.length() > CHUNK_SIZE) return null;
                encoded.append(part);
            }
            byte[] png = Base64.getDecoder().decode(encoded.toString());
            if (png.length < 24 || !key.equals(digest(png))) return null;
            var header = java.nio.ByteBuffer.wrap(png);
            if (header.getLong() != 0x89504e470d0a1a0aL) return null;
            int width = header.getInt(16), height = header.getInt(20);
            if (width < 64 || width > MAX_SIZE || width % 64 != 0 || height != width) return null;
            return png;
        } catch (Exception invalid) { return null; }
    }

    public static String digest(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
