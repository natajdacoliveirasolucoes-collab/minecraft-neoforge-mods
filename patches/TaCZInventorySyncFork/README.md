# TaCZ inventory-ammo synchronization — local server trial

Patch local por Natã Oliveira (nata).

Based on the exact installed `tacz-neoforge-1.21.1-1.1.8-hotfix-r7.jar`, SHA-256 `fa3a09f15265a4065a566960a8deab315f6c601a5f9a542840b362d455dd5470`.

The original minigun client scheduler caps each scheduled shot using the stored magazine count. Its HUD refresh temporarily writes the inventory total into that field. The dedicated server originally retains zero in that field for inventory-fed guns. Server inventory updates can therefore replace the client HUD cache with zero, suppressing scheduling even when rounds are available.

This server-only mitigation synchronizes the held inventory-fed gun's stored count with real matching ammunition, including ammo boxes. It runs before server ticks and after the shared shot handler returns (covering Lua-scripted and native firing). It never extracts extra ammunition or changes server shot acceptance checks. Magazine-fed weapons are excluded. Empty supplies remain empty.

`InventoryAmmoSync.java` is compiled against NeoForge 21.1.265 and the original TaCZ JAR. Run `gradlew.bat build`. The packager verifies the upstream hash and changes only `ServerTickEvent.class`, `LivingEntityShoot.class`, and version metadata, adds the helper and patch sources, and leaves all client classes, protocol classes, gun definitions and assets byte-identical.

Output: `build/fork/tacz-neoforge-1.21.1-1.1.8-hotfix-r8-server.jar`.

No player client JAR has been replaced. This patch is intended for dedicated servers; clients keep the original TaCZ jar. The player also performed an original/patched comparison on a local dedicated server.

## Regression testing

The first trial caught a failure in the post-consumption hook: modern guns consume ammunition through the Lua script API rather than the native consumeAmmoFromPlayer method. The final hook runs when the shared LivingEntityShoot.shoot implementation returns, covering both paths. Original shot results remain on the stack unchanged.

The dedicated-server test suite exercises real ammo extraction and the unchanged native client scheduling calculation with loose rounds, boxed rounds, mixed supply, empty supply, incompatible rounds, burst mode with two rounds, burst mode with at least six rounds, and a magazine-fed M95 control. No connected-client timing claim is made from this test alone.

Final local dedicated-server regression result: **41 checks passed**. Full connected-client spray validation remains pending. The player subsequently compared original and patched builds on a local dedicated server: the patched minigun worked and the original reproduced the fault.
