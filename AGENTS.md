# PGBR — regra obrigatória para agentes, desenvolvimento e auditoria

## 1. PRIMEIRA LEITURA (NÃO PULAR)
**ANTES de qualquer análise, implementação, compilação, correção, limpeza, pergunta de versão, integração ou publicação, LEIA INTEGRALMENTE [PGBR_ARQUIVO_MESTRE_UNICO.md](PGBR_ARQUIVO_MESTRE_UNICO.md).** Ele é a única fonte canônica documental do PGBR, controlada na branch `main`:
https://github.com/LucianoToscano/PGBR/blob/main/PGBR_ARQUIVO_MESTRE_UNICO.md

Não responda como se o arquivo tivesse sido lido sem de fato abri-lo. Se estiver em outra branch, leia a cópia de trabalho e confira a revisão canônica da `main`; documente eventuais divergências. Se não conseguir acessá-lo, diga claramente que a leitura obrigatória não pôde ser confirmada. Não crie outro Arquivo Mestre.

## 2. FONTES COMPLEMENTARES
Em seguida, leia `README.md`, `docs/BUILDING.md`, `docs/ARCHITECTURE.md`, `docs/ROOTED.md` e a documentação correspondente à funcionalidade. Identifique branch, commit, data, versão do jogo, testes e status de release reais.

## 3. LIMITES INEGOCIÁVEIS
- **Enriquecer, nunca empobrecer**: preservar funções, configurações, suporte Root e sem Root, artes e opções.
- **PokeSearch é separado; não modificar** por tarefas do PGBR.
- Diferenciar requisito, código, testes JVM/native, APK compilado, instalado em aparelho, release e homologação. Não inventar hooks Unity, IDs de itens, fontes reais de busca, sucesso de batalhas ou validação.
- A ocultação do nome do treinador é apenas visual/local, `*****` quando validado; nunca alterar `PlayerService.get_PlayerName`, login, RPC, conta ou servidor.
- VGP só liga mediante ação manual/confirmada, sem pausa baseada em cooldown estimado; botões de exibição não comprovam automação funcionando. Eternatus só conta recompensa após confirmação da instância exata.
- Não expor keystore, tokens, APKM privado, coordenadas de usuário, dumps de conta ou credenciais. Preservar releases e backups. Não presumir acesso autorizado a servidores privados de terceiros.
- Não publicar ou fundir automaticamente alterações sem revisar CI, compatibilidade e plano de retorno.
- Ao registrar novo estado **verificado**, atualizar o mesmo Arquivo Mestre em `main` mediante mudança rastreável e informar links.

