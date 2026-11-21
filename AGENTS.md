# REGRA ZERO — LEITURA OBRIGATÓRIA DO ARQUIVO MESTRE

**Antes de qualquer tarefa do PGBR, LEIA INTEGRALMENTE [PGBR_ARQUIVO_MESTRE_UNICO.md](PGBR_ARQUIVO_MESTRE_UNICO.md); confira a versão canônica da [main](https://github.com/LucianoToscano/PGBR/blob/main/PGBR_ARQUIVO_MESTRE_UNICO.md).** Não responder como se tivesse lido sem abrir. Se não estiver acessível, declarar limitação. Não criar outros mestres concorrentes. Todas as alterações verificadas de status devem atualizar o mestre canônico. Em seguida cumprir também as regras abaixo.

# PGBR — Instruções para desenvolvimento e auditoria

Leia `README.md`, `docs/BUILDING.md`, `docs/ARCHITECTURE.md`, `docs/ROOTED.md`, `docs/releases/v0.00.13-LT.md` e `AUDITORIA_IA_COMECE_AQUI_LT013.md`.

**Enriquecer, nunca empobrecer.** Preserve funções e permissões, sem alterar o PokeSearch. Não invente hooks Unity, IDs de itens, resultados de pesquisas ou testes. Separe implementação, testes em stub/JVM, compilação Android, homologação física e release publicada.

Qualquer mudança de privacidade deve afetar apenas a apresentação local do treinador; nunca `PlayerService.get_PlayerName`, login, conta, servidor ou RPC. Sem `global-metadata.dat` validado, a ocultação no mapa/perfil continua pendente.

O coordenador Eternatus só avança por sucesso de resgate da instância exata. A opção manual de pular parada não concede progresso. Preservar cooldown e não iniciar ações automáticas por toque acidental.

Nunca expor chaves de assinatura, tokens, APKM privado, dados pessoais nem credenciais. Builds oficiais usam os secrets existentes no GitHub Actions, e as releases antigas não são apagadas.
