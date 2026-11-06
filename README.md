# Minecraft — mods e patches NeoForge 1.21.1

Fontes e historico das alteracoes mantidas por **Natã Oliveira (nata)**.
Cada projeto informa o artefato, a versao original, a versao alterada e o motivo do patch.
Este repositorio organiza desenvolvimento; nao e o modpack completo para jogadores.

| Projeto | Tipo | Base → versao local | Uso |
|---|---|---|---|
| [TrophyHeads](mods/TrophyHeads) | original | 1.0.2 | Servidor; cliente para renderizar snapshots PNG |
| [TitleSystem](mods/TitleSystem) | original | 1.0.1 | Servidor; cliente visual opcional |
| [LiveWhitelist](mods/LiveWhitelist) | original | 1.0.0 | Servidor |
| [ServerCountdown](mods/ServerCountdown) | original | 1.0.1 | Servidor |
| [PackRules](mods/PackRules) | original | 1.0.1 | Servidor |
| [QuietPlayer2](archive/QuietPlayer2) | deprecated | 1.0.0 | Servidor |
| [GateKeepRuntimeFork](patches/GateKeepRuntimeFork) | patch | 1.0.0 → 1.0.1-nata | Servidor |
| [PlayerEngineAuthFork](patches/PlayerEngineAuthFork) | patch | 1.21.1-1.4.0 → 1.4.0-authnotices.2 | Cliente e servidor |
| [SmallShipsConfigFork](patches/SmallShipsConfigFork) | patch | 2.0.2 → 2.0.3-nata | Cliente e servidor |
| [PalasItemClear](patches/PalasItemClear) | patch | v0.1.2 → 0.1.2-local.2 | Servidor |

## Fontes e compilados

Os fontes ficam em `mods/` e `patches/`. O projeto substituido fica em `archive/`.
Os JARs dos projetos proprios e do fork MIT de Pala ficam nas Releases, identificados por SHA-256.
JARs originais de terceiros, dependencias, mundos, contas e configuracoes de operadores nao sao versionados.
Small Ships e Gate Keep declaram All Rights Reserved: publicamos os builders dos nossos patches e os links oficiais, sem os binarios de terceiros.
PlayerEngine fica somente como patch-fonte nesta publicacao inicial; seus metadados de licenca precisam ser conciliados com a pagina oficial antes de publicar o binario.
As licencas dos originais continuam valendo; veja [THIRD_PARTY.md](THIRD_PARTY.md).

## Compilar

Requer JDK 21. Cada projeto tem seu Gradle Wrapper.
Para os projetos com dependencias de terceiros, execute `python tools/fetch_dependencies.py` na raiz.
O script baixa somente os inputs registrados em `dependencies.json` e verifica SHA-256.
Depois entre no projeto e execute `./gradlew build --console=plain` ou `gradlew.bat build --console=plain` no Windows.
Pala usa o wrapper de `patches/PalasItemClear/versions/1.21.1-neoforge`.
No Windows, configure JAVA_HOME para seu JDK 21 antes de compilar.

## Validacao

Os patches foram testados em desenvolvimento antes deste registro. Resumo em [VALIDATION.md](VALIDATION.md).
O status de testes graficos e de integracao e especificado separadamente; compilar nao comprova todos os fluxos em jogo.
As versoes de mods de terceiros sem alteracoes devem ser obtidas nos projetos oficiais.
