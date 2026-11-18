# PGBR — réplica INTEGRAL do aplicativo PGRazor

Decisão do proprietário em 18/11/2026: manter o **mesmo aplicativo PGRazor completo** dentro de `LucianoToscano/PGBR`, sem decompor o app em aplicativos separados ou substituir seu núcleo por módulos reescritos.

## Estado confirmado
- A árvore `main` do PGBR contém o snapshot integral dos arquivos versionados do PGRazor, incluindo `pgrazor/`, código Java/C++, runtime nativo, bridge, interface, recursos, scripts, ferramentas, documentação e website.
- Origem fixada para a importação: `jpitty03/PGRazor`, `main` commit `f758746f4e1479f4b3105209bdd1d80958605f37`.
- Os workflows originais foram copiados para `upstream-workflows/` e não são executados automaticamente.
- O histórico completo, tags e refs da origem estão preservados no arquivo restaurável `upstream-history/PGRazor-upstream-full.bundle`, e também anexados à release de arquivo.
- O espelho Git direto (commits e branches originais publicados como refs do PGBR) **ainda não foi realizado**; um `git push --mirror` anterior foi bloqueado pela falta de permissão `workflows` do GitHub App. Não classificar o snapshot + bundle como mirror Git direto.
- Backup do PGBR inicial: `backup/main-antes-replica-pgrazor-20261118`.
- Os 28 Releases e 95 assets da origem constam no catálogo `docs/PGBR_RELEASES_UPSTREAM_CATALOGO_20261118.json` com links, tamanhos e SHA256.
- Um arquivo de backup separado no PGBR guarda releases ROOT antigas e metadados. **É somente backup histórico**, NÃO representa app novo, arquitetura separada ou a base do projeto.
- A versão sem root `PGRazor.apk` da origem pode incorporar o jogo Pokémon GO modificado e não deve ser reempacotada, renomeada ou republicada como PGBR sem autorização dos respectivos detentores. Sua localização e checksum estão no catálogo; os binários originais permanecem disponíveis por link de origem.
- A licença MIT do código original PGRazor não cobre Pokémon GO, suas imagens, nem todos os componentes terceiros. Preservar `LICENSE` e `THIRD_PARTY.md`.

## Regras permanentes de desenvolvimento
1. Manter a identidade arquitetural, as funcionalidades e o fluxo de build do PGRazor completo como base do PGBR. Não dividir módulos em outros aplicativos.
2. Alterar de forma incremental, após auditoria e testes; preservar todas as funcionalidades e os dois modos de distribuição originais (não-root e root) enquanto estiverem no escopo do código-fonte.
3. O código original permanece rastreável: manter referência para upstream, commits e arquivos de licença.
4. Evitar publicar executáveis, clientes do jogo modificados ou artefatos de terceiros sem direitos e autorização; não inferir que copiar código-fonte significa que um APK pronto já foi compilado.
5. Não modificar `LucianoToscano/PokeSearch`, suas releases, configurações, assinatura, inventários ou fonte.
6. O PGBR não se torna PokeSearch: é um laboratório **independente** para desenvolvimento futuro, mas a base preserva o aplicativo PGRazor até que o proprietário autorize mudanças.
