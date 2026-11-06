# SmallShipsConfigFork 2.0.3-nata

Evita acesso a configuracao descarregada durante ModConfigEvent.Unloading; preserva Loading e Reloading.

Minecraft 1.21.1 / NeoForge 21.1.265 / Java 21.

Lado: Cliente e servidor.

Base: 2.0.2 — https://modrinth.com/mod/small-ships

Artefato: `smallships-neoforge-1.21.1-2.0.3-nata.jar`.

Este diretorio contem o codigo do patch. O JAR original nao faz parte do Git.
Execute `python tools/fetch_dependencies.py` na raiz para obter e conferir os inputs antes do build.
O builder rejeita uma base com SHA-256 diferente. Nao ha JAR modificado de terceiros publicado na release inicial.

Build: entre neste diretorio e execute `./gradlew build --console=plain` (Windows: `gradlew.bat build --console=plain`).
