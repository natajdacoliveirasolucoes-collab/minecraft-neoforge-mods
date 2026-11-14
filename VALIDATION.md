# Registro de validacao

| Projeto | Evidencia previa |
|---|---|
| TrophyHeads 1.0.2 | Reproducao vermelha com a versao anterior; snapshots PNG, persistencia, regressao de texturas vanilla e compatibilidade de cabecas antigas; verificacao grafica isolada. |
| TitleSystem 1.0.1 | Aba de conquistas e previa temporaria administrativa validadas em desenvolvimento. |
| LiveWhitelist 1.0.0 | Identidade offline e whitelist ao vivo verificadas; nenhum banco de senhas alterado. |
| ServerCountdown 1.0.1 | Sete testes unitarios; comandos, persistencia e preferencias individuais testados. |
| PackRules 1.0.1 | Bloqueio de entidades e demonstracao real de campeoes na barra de acao testados. |
| Small Ships 2.0.3-nata | Encerramento limpo e reinicio com o mesmo barco/passageiro; outras classes preservadas. |
| Gate Keep 1.0.1-nata | 21 verificacoes no conjunto completo: permissoes, lock/unlock, reload, JSON invalido, preservacao e transporte real entre dimensoes. |
| PlayerEngine authnotices.2 | Flag ausente/false/true/invalida e mensagens comuns; rebuild e alteracao binaria delimitada. Nao foi teste de OAuth real. |
| Pala local.2 | 56 testes originais e seis cenarios de runtime, incluindo limpeza de zero itens. |

Este resumo registra validacoes anteriores; logs brutos, mundos e identidades de jogadores nao sao publicados.

## Build da publicacao inicial — 06/11/2026

Os nove projetos ativos compilaram nesta copia. As classes executaveis geradas sao identicas as dos artefatos ja validados. Os testes unitarios executados somam 63 casos, sem falhas. Detalhes em compilation-results.json.

## Atualizacao — 14/11/2026

TrophyHeads 1.0.3, TitleSystem 1.0.2, PackRules 1.0.5, MekaTaCZCompat 1.0.0 e TaCZInventorySyncFork recompilaram a partir da estrutura publica do repositorio. Detalhes em update-validation.json.

- TrophyHeads/TitleSystem: validacao local anterior incluiu entrega ao assassino, protecao contra quatro integracoes de imas, cabecas antigas e persistencia apos renascer/reiniciar. Foram 82 verificacoes automatizadas. Corpse nao estava instalado; nao afirmar validacao especifica desse mod.
- TaCZ: 41 verificacoes do fluxo de municao; o jogador comparou o servidor local com e sem o patch, reproduzindo a falha apenas sem a correcao. O builder confirma que somente duas classes de servidor e a versao existente mudaram.
- MekaTaCZCompat: startup isolado confirmou carregamento e injecao; resultado visual aceito pelo jogador. Nao ha cobertura visual automatizada de cada arma.
- PackRules: inclui precificacao e auditoria EMC. Esta atualizacao de fontes nao altera arquivos ou configuracoes de servidores em execucao.

Compilar estes projetos nao repete os testes historicos nem comprova ausencia de malware.
