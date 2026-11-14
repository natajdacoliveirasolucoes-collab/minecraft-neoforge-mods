import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
public class BuildFork {
 public static void main(String[] a)throws Exception {
  Path input=Path.of(a[0]),classes=Path.of(a[1]),output=Path.of(a[2]),source=Path.of(a[3]);
  String sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(input)));
  if(!sha.equals("fa3a09f15265a4065a566960a8deab315f6c601a5f9a542840b362d455dd5470"))throw new IllegalArgumentException("Unexpected base JAR");
  int[] patches={0,0};Files.createDirectories(output.getParent());
  try(ZipFile z=new ZipFile(input.toFile());ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(output))){
   var entries=z.entries();
   while(entries.hasMoreElements()){
    var e=entries.nextElement();byte[] bytes=z.getInputStream(e).readAllBytes();String n=e.getName();
    boolean tick=n.equals("com/tacz/guns/event/ServerTickEvent.class"),consume=n.equals("com/tacz/guns/entity/shooter/LivingEntityShoot.class");
    if(tick||consume){
     ClassReader cr=new ClassReader(bytes);ClassWriter cw=new ClassWriter(cr,ClassWriter.COMPUTE_MAXS);
     cr.accept(new ClassVisitor(Opcodes.ASM9,cw){
      public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex){
       MethodVisitor mv=super.visitMethod(access,name,desc,sig,ex);
       boolean patch=tick&&name.equals("onServerTick")&&desc.equals("(Lnet/neoforged/neoforge/event/tick/ServerTickEvent$Pre;)V") || consume&&name.equals("shoot")&&desc.equals("(Ljava/util/function/Supplier;Ljava/util/function/Supplier;JFZ)Lcom/tacz/guns/api/entity/ShootResult;");
       if(!patch)return mv;patches[tick?0:1]++;
       return new MethodVisitor(Opcodes.ASM9,mv){
        public void visitInsn(int op){
         if((tick&&op==Opcodes.RETURN)||(consume&&op==Opcodes.ARETURN)){
          super.visitVarInsn(Opcodes.ALOAD,0);
          if(tick)super.visitMethodInsn(Opcodes.INVOKESTATIC,"com/tacz/guns/nata/InventoryAmmoSync","onServerTick","(Lnet/neoforged/neoforge/event/tick/ServerTickEvent$Pre;)V",false);
          else{
           super.visitFieldInsn(Opcodes.GETFIELD,"com/tacz/guns/entity/shooter/LivingEntityShoot","shooter","Lnet/minecraft/world/entity/LivingEntity;");
           super.visitInsn(Opcodes.DUP);
           super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"net/minecraft/world/entity/LivingEntity","getMainHandItem","()Lnet/minecraft/world/item/ItemStack;",false);
           super.visitMethodInsn(Opcodes.INVOKESTATIC,"com/tacz/guns/nata/InventoryAmmoSync","synchronize","(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V",false);
          }
         }
         super.visitInsn(op);
        }
       };
      }
     },0);bytes=cw.toByteArray();
    }
    if(n.equals("META-INF/neoforge.mods.toml"))bytes=new String(bytes,StandardCharsets.UTF_8).replace("version = \"1.1.8-hotfix-r7\"","version = \"1.1.8-hotfix-r8-server\"").getBytes(StandardCharsets.UTF_8);
    put(out,n,bytes);
   }
   if(patches[0]!=1||patches[1]!=1)throw new IllegalStateException("Unexpected patch targets "+Arrays.toString(patches));
   put(out,"com/tacz/guns/nata/InventoryAmmoSync.class",Files.readAllBytes(classes.resolve("com/tacz/guns/nata/InventoryAmmoSync.class")));
   put(out,"META-INF/nata-inventory-ammo-sync.txt",("Patch local por Natã Oliveira (nata).\nBased on TaCZ 1.1.8-hotfix-r7 SHA-256 "+sha+"\nServer-only inventory-ammo metadata synchronization before player ticks and after the shared shot handler (Lua and native paths).\nWeapon data, client classes, ammo extraction, fire-rate/network validation, protocols and upstream credits preserved.\n").getBytes(StandardCharsets.UTF_8));
   put(out,"META-INF/nata-inventory-ammo-sync-source/InventoryAmmoSync.java",Files.readAllBytes(source));
   put(out,"META-INF/nata-inventory-ammo-sync-source/BuildFork.java",Files.readAllBytes(Path.of(a[4])));
  }
  System.out.println("Patched two server methods; added server helper; client classes and gun resources unchanged.");
 }
 static void put(ZipOutputStream out,String name,byte[] bytes)throws Exception{ZipEntry e=new ZipEntry(name);e.setTime(0);out.putNextEntry(e);out.write(bytes);out.closeEntry();}
}
