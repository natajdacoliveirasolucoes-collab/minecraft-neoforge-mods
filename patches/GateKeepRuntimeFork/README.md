# GateKeepRuntimeFork 1.0.1-nata

Comandos lock/unlock/status/reload sem reinicio; persistencia atomica e permissao nivel 3.

Minecraft 1.21.1 / NeoForge 21.1.265 / Java 21.

Lado: Servidor.

Base: 1.0.0 — https://modrinth.com/mod/gate-keep

Artefato: `GateKeep-1.21.x-neoforge-1.0.1-nata.jar`.

Este diretorio contem o codigo do patch. O JAR original nao faz parte do Git.
Execute `python tools/fetch_dependencies.py` na raiz para obter e conferir os inputs antes do build.
O builder rejeita uma base com SHA-256 diferente. Nao ha JAR modificado de terceiros publicado na release inicial.

Build: entre neste diretorio e execute `./gradlew build --console=plain` (Windows: `gradlew.bat build --console=plain`).

Console ou OP 3+: `gatekeep status`, `gatekeep unlock minecraft:the_nether`, `gatekeep lock minecraft:the_nether`, `gatekeep reload`.
Tambem aceita `minecraft:the_end`. Comandos manuais persistem e substituem o timer da dimensao escolhida.
