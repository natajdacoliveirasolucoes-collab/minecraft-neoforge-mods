# Historico das alteracoes

## TrophyHeads
- 1.0.0: base do sistema de trofeus de cabeca.
- 1.0.1: copia independente do GameProfile e texturas, evitando compartilhar o perfil mutavel do jogador.
- 1.0.2: snapshot PNG de Quick Skin, renderer do trofeu e compatibilidade com cabecas existentes. ID persistente examplemod mantido.

## TitleSystem
- 1.0.0: titulos, progresso PvP e conquistas.
- 1.0.1: raiz da aba de conquistas acessivel e previa temporaria administrativa sem alterar o progresso permanente.

## LiveWhitelist
- 1.0.0: comando add respeitando a grafia no UUID offline e a identidade existente via DirectAuth. Alteracoes ao vivo e backup do JSON.

## ServerCountdown
- 1.0.0: contagem global por bossbar com preferencia individual de visibilidade.
- 1.0.1: agendamento sem parametro de fuso; America/Manaus interno e indicador GMT-4.

## PackRules
- 1.0.0: bloqueio configuravel de entidades.
- 1.0.1: demonstracoes de campeoes e aviso na barra de acao para validar a apresentacao em jogo.

## Pala's Item Clear
- Base: v0.1.2 de Palawizard, MIT.
- 0.1.2-local.1: mensagem mesmo quando zero itens sao removidos, emptyCleared opcional e placeholders do intervalo.
- 0.1.2-local.2: respostas operacionais e logs de aviso/resultado em PT-BR. O agendador original continua baseado em ticks.

## PlayerEngine
- Base: 1.21.1-1.4.0.
- authnotices.1: flag para suprimir os avisos especificos originados no servidor.
- authnotices.2: padrao silencioso tambem no cliente; true explicito reativa os avisos. Fluxo de autorizacao preservado.

## Small Ships
- Base: 2.0.2 NeoForge.
- 2.0.3-nata: ignora ModConfigEvent.Unloading no handler que acessava configuracao ja descarregada; autoria original preservada.

## Gate Keep
- Base: 1.0.0.
- 1.0.1-nata: comandos administrativos para liberar, bloquear, consultar e recarregar em tempo real, com salvamento e validacao.

## Experimento arquivado
- QuietPlayer2 1.0.0: tentativa inicial de filtrar avisos. Substituida pelo patch direto do PlayerEngine.
