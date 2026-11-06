package com.tandy.gatekeepfork;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.*;
import org.objectweb.asm.*;

public final class BuildFork {
    public static void main(String[] args) throws Exception {
        Path base=Path.of(args[0]),output=Path.of(args[1]),classes=Path.of(args[2]),sources=Path.of(args[3]);
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(base)));
        if(!hash.equals("aa6f16ccca66d6416e24f41d188b79d3fbdc9c93c6aa9582e4342a37c3c4c280")) throw new IllegalArgumentException("Unexpected upstream GateKeep");
        Files.createDirectories(output.getParent());int[] hooks={0};
        try(var original=new ZipFile(base.toFile());var zip=new ZipOutputStream(Files.newOutputStream(output))) {
            var entries=original.entries();
            while(entries.hasMoreElements()) {
                var entry=entries.nextElement();byte[] data=original.getInputStream(entry).readAllBytes();
                if(entry.getName().equals("com/frikinjay/gatekeep/neoforge/GateKeepNeoForge.class")) {
                    var reader=new ClassReader(data);var writer=new ClassWriter(reader,0);
                    reader.accept(new ClassVisitor(Opcodes.ASM9,writer) {
                        @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] exceptions) {
                            var method=super.visitMethod(access,name,desc,sig,exceptions);
                            if(!name.equals("<init>")||!desc.equals("()V"))return method;
                            return new MethodVisitor(Opcodes.ASM9,method) {
                                @Override public void visitInsn(int opcode) {
                                    if(opcode==Opcodes.RETURN) {
                                        super.visitMethodInsn(Opcodes.INVOKESTATIC,"com/frikinjay/gatekeep/command/RuntimeCommands","register","()V",false);hooks[0]++;
                                    }
                                    super.visitInsn(opcode);
                                }
                            };
                        }
                    },0);data=writer.toByteArray();
                } else if(entry.getName().equals("META-INF/neoforge.mods.toml")) {
                    String metadata=new String(data,StandardCharsets.UTF_8);
                    metadata=metadata.replace("version = \"1.0.0\"","version = \"1.0.1-nata\"")
                        .replace("authors = \"frikinjay\"","authors = \"frikinjay\"\ncredits = \"Comandos administrativos em tempo real por Natã Oliveira (nata).\"");
                    data=metadata.getBytes(StandardCharsets.UTF_8);
                }
                put(zip,entry.getName(),data);
            }
            if(hooks[0]!=1)throw new IllegalStateException("Expected exactly one constructor hook");
            try(var files=Files.walk(classes.resolve("com/frikinjay/gatekeep/command"))) {
                for(Path file:files.filter(f->f.toString().endsWith(".class")).sorted().toList())put(zip,classes.relativize(file).toString().replace('\\','/'),Files.readAllBytes(file));
            }
            try(var files=Files.walk(sources)) {
                for(Path file:files.filter(f->f.toString().endsWith(".java")).sorted().toList())put(zip,"META-INF/runtime-fork-source/"+sources.relativize(file).toString().replace('\\','/'),Files.readAllBytes(file));
            }
            put(zip,"META-INF/runtime-fork.txt",("GateKeep 1.0.1-nata; base SHA-256 "+hash+"\nOriginal author frikinjay retained. Patch local por Natã Oliveira (nata).\nCommands require permission level 3; existing portal logic/config preserved.\n").getBytes(StandardCharsets.UTF_8));
        }
        System.out.println("Built GateKeep runtime-command fork; one constructor hook, existing portal classes preserved.");
    }
    private static void put(ZipOutputStream zip,String name,byte[] bytes)throws Exception {
        var entry=new ZipEntry(name);entry.setTime(0);zip.putNextEntry(entry);zip.write(bytes);zip.closeEntry();
    }
}
