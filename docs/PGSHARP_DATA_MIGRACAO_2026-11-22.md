# Migração de dados PGSharp para PGBR — 22/11/2026

## Escopo e origem

Importação **local e explícita** a partir de um arquivo JSON convertido em ambiente confiável, não a partir da serialização Java original. O arquivo pessoal de origem permanece **fora do repositório público**.

- Fonte examinada: arquivo `.dat` do usuário, Java Serialization (`java.util.HashMap`), SHA-256 `ad81b56007acaf785f99f5adf5a178e7b4c7155a0fae057183c1bc1208355a6a`.
- Dados detectados: **80 registros de favoritos**, sendo **54 coordenadas de destino** e **26 legendas/notas em (0,0)**, **18 rotas** (474 pontos), **130 filtros de Pokémon**, **28 filtros de pesquisas**, **14 filtros de reides** e **9 opções legadas**.
- Format/version do conversor: `PGBR PGSharp migration` / `1`.
- O JSON pessoal não é versionado no Git e não é embutido no APK.

## Integração

`PgSharpDataImport.java` valida o JSON antes de persistir:

1. Usa `SavedPlaces` para unir os favoritos existentes com os importados, sem apagar os já salvos e sem repetir nomes; nomes diferentes podem referir-se à mesma coordenada. Registros em (0,0) ficam no arquivo pessoal, não na lista de destinos. O teto é ampliado de 50 para 200; nomes aceitam até 100 unidades de código, suficientes para os nomes analisados.
2. Converte pontos de rotas `[latitude, longitude, elevação]` para `GpxRoute` (elevação zero da origem não é relevante ao motor 2D) e salva via `GpxStore` por perfil, preservando a ordem. Rota já salva com o mesmo nome é ignorada.
3. Preserva os filtros originais e suas opções legadas em preferências privadas `pgrazor-pgsharp-data-v1/migration_json`, sem alterar filtros ativos, escudos, ataques, teleporte ou decisões de batalha.
4. O arquivo importado é incluído na exportação de backups da conta por `SettingsBackupStore`. A validação de backup permite as novas quantidades sem remover os formatos antigos.
5. Em erro de importação, tenta restaurar os favoritos da fotografia anterior, remover apenas as rotas inseridas e restaurar o arquivo legado anterior. Falha de rollback é sinalizada por exceções suprimidas.

## Como importar

**PGBR sem Root:** Configurações → Contas e backup → **Importar dados PGSharp (JSON convertido)** → selecionar `PGBR_PGSharp_Importacao.json`.

**PGBR Root:** copiar `PGBR_PGSharp_Importacao.json` para a pasta externa do aplicativo exibida na tela **Contas e backup** e selecionar **Importar dados PGSharp (JSON convertido)**. O fluxo Root usa o arquivo local, sem depender de retorno do seletor Android.

A tela **Ver dados PGSharp importados** mostra o total preservado por categoria. A opção de importação normal **Import settings** continua aceitando somente um backup PGBR, não o arquivo de migração.

## Limites e pendências

- Os **130/28/14 filtros originais** são preservados, **não executados**: PGSharp e PGBR possuem provedores, contratos e esquemas diferentes. Não confundir o acervo salvo com Sniper funcional ou feed global validado.
- O importador não abre `.dat` diretamente, não usa `ObjectInputStream` em entrada externa, não extrai credenciais, não chama serviços externos e não faz teleporte.
- O código foi criado em branch separada e aguarda CI e teste de importação em aparelho Android Root e sem Root. **Nenhum APK/teste físico está homologado por este registro**.
- Antes de mesclar, revisar o impacto das novas capacidades de `SavedPlaces` e `SettingsBackupRules` nas PRs paralelas. Não substituir um arquivo pessoal por uma cópia pública.

## Matriz de verificação proposta

| Caso | Resultado esperado |
|---|---|
| JSON original convertido, perfil vazio | +54 favoritos geográficos, +18 rotas, 26 legendas/notas preservadas no arquivo, filtros preservados |
| Segunda importação do mesmo arquivo | Sem duplicatas nem perda de configurações |
| Perfil com favoritos e rotas já existentes | Mescla sem substituir existentes, inclusive se dois nomes apontarem à mesma coordenada |
| JSON inválido / latitude fora dos limites / versão errada | Rejeita sem gravar |
| Excesso de capacidade de favoritos ou rotas | Rejeita antes de iniciar |
| Falha ao gravar rota ou perfil | Rollback das mudanças geradas pela importação, com falha sinalizada se rollback incompleto |
| Exportação PGBR após importação | Inclui arquivo de migração em escopo por conta |
| Filtros PGSharp importados | Preservados, não ativados e não anunciados como fonte de dados ao vivo |
