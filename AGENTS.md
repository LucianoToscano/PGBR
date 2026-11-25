# REGRA ZERO — LEITURA OBRIGATÓRIA DO ARQUIVO MESTRE

**Antes de qualquer tarefa do PGBR, LEIA INTEGRALMENTE [PGBR_ARQUIVO_MESTRE_UNICO.md](PGBR_ARQUIVO_MESTRE_UNICO.md); confira a versão canônica da [main](https://github.com/LucianoToscano/PGBR/blob/main/PGBR_ARQUIVO_MESTRE_UNICO.md).** Não responder como se tivesse lido sem abrir. Se não estiver acessível, declarar limitação. Não criar outros mestres concorrentes. Todas as alterações verificadas de status devem atualizar o mestre canônico. Em seguida cumprir também as regras abaixo.

## REGRA PERMANENTE — ATUALIZAR O ARQUIVO MESTRE EM TODAS AS ENTREGAS

**Obrigatório, sem exceções:** toda correção, recurso, alteração de interface, teste, atualização de upstream, PR, merge, mudança de versão, build, upload ou release deve deixar registro factual no **mesmo** `PGBR_ARQUIVO_MESTRE_UNICO.md`. Preparar o registro no mesmo PR, com branch, HEAD, arquivos relevantes, evidência real, testes, links, limitações e pendências. **Após** o merge/build/release, atualizar novamente o Mestre na `main` com commit final, tag publicada, SHA-256 e URLs que existirem; não classificar resultados pendentes como aprovados. Não criar um segundo mestre, não substituir histórico nem apagar evidências anteriores.

O trabalho só é encerrado depois de conferir a HEAD e os links de destino, preservar os outros chats/branches, confirmar os resultados e deixar o Mestre sincronizado. Builds não homologados em Android permanecem `QA / device_tested=false` mesmo quando são publicados como pré-release.

# PGBR — Instruções para desenvolvimento e auditoria

Leia `README.md`, `docs/BUILDING.md`, `docs/ARCHITECTURE.md`, `docs/ROOTED.md`, `docs/GAME_UPDATES.md` e `THIRD_PARTY.md`. Documentos históricos LT013 não existem na `main`: consultar [release notes LT013](https://github.com/LucianoToscano/PGBR/blob/integracao/lt032-recuperacao-20261121/docs/releases/v0.00.13-LT.md) e [auditoria LT013](https://github.com/LucianoToscano/PGBR/blob/integracao/lt032-recuperacao-20261121/AUDITORIA_IA_COMECE_AQUI_LT013.md) apenas na branch indicada.

**Enriquecer, nunca empobrecer.** Preserve funções e permissões, sem alterar o PokeSearch. Não invente hooks Unity, IDs de itens, resultados de pesquisas ou testes. Separe implementação, testes em stub/JVM, compilação Android, homologação física e release publicada.

Qualquer mudança de privacidade deve afetar apenas a apresentação local do treinador; nunca `PlayerService.get_PlayerName`, login, conta, servidor ou RPC. Sem `global-metadata.dat` validado, a ocultação no mapa/perfil continua pendente.

O coordenador Eternatus só avança por sucesso de resgate da instância exata. A opção manual de pular parada não concede progresso. Preservar cooldown e não iniciar ações automáticas por toque acidental.

Nunca expor chaves de assinatura, tokens, APKM privado, dados pessoais nem credenciais. Builds oficiais usam os secrets existentes no GitHub Actions, e as releases antigas não são apagadas.

## REGRA PRIORITÁRIA — CLARÃO NO ATAQUE CARREGADO
Para investigar a tela branca no instante do ataque carregado, iniciar pela Batalha Rocket e pelo fluxo visual compartilhado, antes de avaliar PvP/Treinamento. Consultar o registro prioritário no Mestre e https://github.com/LucianoToscano/PGBR/issues/192. Preservar funcionalidades e regras de combate; corrigir somente a apresentação local identificada, sem encobrir a tela. Não presumir causalidade nem marcar como homologado sem validação física. Preservar outros trabalhos paralelos.


## REGRA PERMANENTE — ENRIQUECER, NUNCA EMPOBRECER (ANTI-REGRESSÃO)
- Toda contribuição, inclusive de IA externa, deve partir da main atual em branch isolada e preservar funcionalidades, preferências, interfaces, assets, contratos e requisitos existentes. Não remover, simplificar, desabilitar ou mascarar funcionalidades para passar testes; correções justificadas de dados incorretos não são perda de funcionalidade.
- Antes de integrar: revisar diff completo contra main, conferir fontes independentes para dados de domínio, preservar testes anteriores e acrescentar teste que falha com o defeito e passa com a correção. Concordância entre cópias da mesma tabela não comprova correção; fixtures não podem ser alterados apenas para concordar com o código. Registrar motivo, referência e casos de fronteira.
- Não mesclar se houver regressão conhecida, perda de requisito, conflito não resolvido ou CI obrigatório não aprovado na HEAD exata. Não aprovar resultado por mera reprodução do defeito. Reverter ou corrigir somente a alteração defeituosa, preservando melhorias independentes; homologação física separada de CI.
- Gemini e outras IAs consultivas não devem escrever na main. Arena permanece no escopo designado; integração pelo coordenador após revisão. Esta regra não substitui proteções efetivas de branch nem garante ausência de todo defeito.
