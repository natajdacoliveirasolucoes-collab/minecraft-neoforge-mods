package com.tandy.playerenginefork;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Reproducible, narrowly scoped derivative of the exact deployed binary, not a downgrade to upstream. */
public final class BuildFork {
    private static final String EXPECTED_BASE = "19a9910bb5be4cca1d32aa3d5f83a4b9bdd0a8429395a1679414095cbbf8b0a6";
    private static final String AUTH = "com/player2/playerengine/player2api/auth/AuthenticationManager.class";
    private static final String BRIDGE = "com/player2/playerengine/player2api/auth/AuthNoticeBridge";
    private static final Set<String> KEYS = Set.of("message.playerengine.auth.authorize_prompt",
            "message.playerengine.auth.dedicated_hint");

    public static void main(String[] args) throws Exception {
        Path base = Path.of(args[0]), output = Path.of(args[1]);
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(base)));
        if (!EXPECTED_BASE.equals(digest)) throw new IllegalStateException("Base JAR differs from the reviewed version");
        Files.createDirectories(output.getParent());
        int[] changedCalls = {0};
        try (ZipFile original = new ZipFile(base.toFile()); ZipOutputStream result = new ZipOutputStream(Files.newOutputStream(output))) {
            var entries = original.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                byte[] bytes = original.getInputStream(entry).readAllBytes();
                if (entry.getName().equals(AUTH)) {
                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(reader, 0);
                    reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                        @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                                private boolean notice;
                                @Override public void visitLdcInsn(Object value) {
                                    if (value instanceof String key && KEYS.contains(key)) notice = true;
                                    super.visitLdcInsn(value);
                                }
                                @Override public void visitMethodInsn(int opcode, String owner, String method, String desc, boolean itf) {
                                    if (notice && owner.equals("net/minecraft/world/entity/player/Player")
                                            && method.equals("sendSystemMessage") && desc.equals("(Lnet/minecraft/network/chat/Component;)V")) {
                                        super.visitMethodInsn(Opcodes.INVOKESTATIC, BRIDGE, "sendNotice",
                                                "(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/network/chat/Component;)V", false);
                                        notice = false; changedCalls[0]++;
                                    } else super.visitMethodInsn(opcode, owner, method, desc, itf);
                                }
                            };
                        }
                    }, 0);
                    bytes = writer.toByteArray();
                } else if (entry.getName().equals("META-INF/neoforge.mods.toml")) {
                    String metadata = new String(bytes, StandardCharsets.UTF_8);
                    if (!metadata.contains("version = \"1.21.1-1.4.0\"")) throw new IllegalStateException("Unexpected mod metadata");
                    bytes = metadata.replace("version = \"1.21.1-1.4.0\"", "version = \"1.21.1-1.4.0-authnotices.2\"").getBytes(StandardCharsets.UTF_8);
                }
                put(result, entry.getName(), bytes);
            }
            if (changedCalls[0] != 2) throw new IllegalStateException("Expected exactly two notice call sites; got " + changedCalls[0]);
            put(result, BRIDGE + ".class", Files.readAllBytes(Path.of(args[2])));
            put(result, "META-INF/auth-notices-fork.txt", ("PlayerEngine 1.4.0 auth-notices patch 2\nBase SHA-256: " + digest
                    + "\nOnly two notice call sites and the mod version metadata changed. All other original entries preserved.\n"
                    + "Configuration: playerengine/auth-notices.properties, showAuthNotices=false. Default false on client and server; true explicitly re-enables notices.\n"
                    + "Original LGPL-3.0 license and credits retained. Patch Java sources included below.\n").getBytes(StandardCharsets.UTF_8));
            try (var paths = Files.walk(Path.of(args[3]))) {
                for (Path source : paths.filter(p -> p.toString().endsWith(".java")).sorted().toList()) {
                    put(result, "META-INF/auth-notices-fork-source/" + Path.of(args[3]).relativize(source).toString().replace('\\', '/'), Files.readAllBytes(source));
                }
            }
        }
        System.out.println("Built exact-version PlayerEngine patch: " + changedCalls[0] + " guarded notice calls.");
    }

    private static void put(ZipOutputStream output, String name, byte[] bytes) throws Exception {
        ZipEntry entry = new ZipEntry(name); entry.setTime(0);
        output.putNextEntry(entry); output.write(bytes); output.closeEntry();
    }
}
