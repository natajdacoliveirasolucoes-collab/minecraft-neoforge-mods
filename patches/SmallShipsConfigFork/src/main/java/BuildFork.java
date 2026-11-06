import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.objectweb.asm.*;

/** Applies one event-lifecycle guard to the exact user-supplied NeoForge 2.0.2 binary. */
public final class BuildFork {
    private static final String BASE_SHA = "aed11c65cd44a62309f52528727f83d05f74cced5cd93951692972273c340796";
    private static final String TARGET = "com/talhanation/smallships/neoforge/common/CommonModBus.class";
    public static void main(String[] args) throws Exception {
        Path base=Path.of(args[0]), output=Path.of(args[1]), source=Path.of(args[2]);
        String sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(base)));
        if(!BASE_SHA.equals(sha)) throw new IllegalArgumentException("Unexpected upstream binary");
        Files.createDirectories(output.getParent());
        int[] changes={0};
        try(var zip=new ZipFile(base.toFile());var out=new ZipOutputStream(Files.newOutputStream(output))) {
            var entries=zip.entries();
            while(entries.hasMoreElements()) {
                var entry=entries.nextElement();byte[] bytes=zip.getInputStream(entry).readAllBytes();
                if(entry.getName().equals(TARGET)) {
                    var reader=new ClassReader(bytes);
                    var writer=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);
                    reader.accept(new ClassVisitor(Opcodes.ASM9,writer) {
                        @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions) {
                            var original=super.visitMethod(access,name,descriptor,signature,exceptions);
                            if(!name.equals("initRegisterConfigs")||!descriptor.equals("(Lnet/neoforged/fml/event/config/ModConfigEvent;)V")) return original;
                            if((access&Opcodes.ACC_STATIC)==0) throw new IllegalStateException("Expected static config handler");
                            changes[0]++;
                            return new MethodVisitor(Opcodes.ASM9,original) {
                                @Override public void visitCode() {
                                    super.visitCode();
                                    Label loading=new Label();
                                    super.visitVarInsn(Opcodes.ALOAD,0);
                                    super.visitTypeInsn(Opcodes.INSTANCEOF,"net/neoforged/fml/event/config/ModConfigEvent$Unloading");
                                    super.visitJumpInsn(Opcodes.IFEQ,loading);
                                    super.visitInsn(Opcodes.RETURN);
                                    super.visitLabel(loading);
                                    super.visitFrame(Opcodes.F_SAME,0,null,0,null);
                                }
                            };
                        }
                    },0);
                    bytes=writer.toByteArray();
                } else if(entry.getName().equals("META-INF/neoforge.mods.toml")) {
                    String text=new String(bytes,StandardCharsets.UTF_8);
                    if(!text.contains("version = \"2.0.2\"")) throw new IllegalArgumentException("Unexpected version metadata");
                    if(!text.contains("authors = \"talhanation, metroite\"")) throw new IllegalArgumentException("Unexpected upstream authors");
                    text=text.replace("version = \"2.0.2\"","version = \"2.0.3-nata\"");
                    text=text.replace("authors = \"talhanation, metroite\"",
                        "authors = \"talhanation, metroite\"\ncredits = \"Patch local por Natã Oliveira (nata). Base upstream: Small Ships 2.0.2.\"");
                    bytes=text.getBytes(StandardCharsets.UTF_8);
                }
                put(out,entry.getName(),bytes);
            }
            if(changes[0]!=1) throw new IllegalStateException("Expected exactly one patched handler, got "+changes[0]);
            put(out,"META-INF/config-lifecycle-fork.txt",("Small Ships 2.0.3-nata local fork, based on upstream Small Ships 2.0.2\n"
                +"Patch local por Natã Oliveira (nata). Text attribution; original authors preserved.\n"
                +"Upstream SHA-256: "+sha+"\n"
                +"initRegisterConfigs returns immediately on ModConfigEvent.Unloading. Loading and Reloading remain unchanged.\n"
                +"Original authors, license, mod ID, content, networking, saves and configuration preserved.\n").getBytes(StandardCharsets.UTF_8));
            put(out,"META-INF/config-lifecycle-fork-source/BuildFork.java",Files.readAllBytes(source));
        }
        System.out.println("Patched exactly one config handler; retained every other upstream class and resource.");
    }
    private static void put(ZipOutputStream out,String name,byte[] bytes) throws Exception {
        var entry=new ZipEntry(name);entry.setTime(0);out.putNextEntry(entry);out.write(bytes);out.closeEntry();
    }
}
