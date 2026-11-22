# PGBR — correção PT-BR das capturas do Google Drive (22/11/2026)

## Referência e isolamento
- Pasta de evidências: https://drive.google.com/drive/folders/138ds5DP0RN2GOmHnCB9qSWI4c-4DWWnG
- Commit-base de `main`: `be01d9b59a9e1f31f292b8c562744e5801993d08`.
- Branch independente: `fix/ptbr-drive-capturas-20261122`.
- Não altera tempos, motor de ataques, Escudo Inteligente ou Troca Inteligente; não mesclar diretamente em `main`.

## Falhas identificadas nas capturas
- Reides automáticas: estados `Not running`, `Any tier`, seletor de distância e níveis de reide.
- Localização, mapa e rotas: nomes de Ginásios, quantidades de paradas, progresso dinâmico da caminhada.
- Fontes de dados, filtros e categorias: condições climáticas, grupos de Pokémon e títulos de filtros.
- Scanner de Brilhantes: estado dinâmico `All N nearby checked · N checked, N shinies · N/M in 24 h`.
- Autenticação em fonte ao vivo: erro `Your account doesn't have access to this feature.`.
- Histórico do VGP: registro `VGP ball throw`.

## Mudanças
- Adicionadas **65 traduções ausentes**; as 12 chaves já presentes no conjunto analisado foram preservadas, evitando apagar a tradução aprovada.
- Total do glossário passou de **1.384 para 1.449** entradas.
- Mantidas exatamente iguais as chaves de apresentação em `pgrazor/i18n/pt-BR.json`, `PtBr.java` e `RootPtBr.java`.
- Reconhecimento estritamente visual de três formas numéricas do Scanner de Brilhantes no tradutor do app e no Root. Sem modificar os códigos nativos que emitem o status.
- Correção da montagem dos contadores dinâmicos de `AutoWalkDialog.java` por segmentos traduzidos, mantendo as variáveis numéricas e a lógica de automação.
- Testes adicionados a `PtBrDynamicTest.java` e `test_ptbr_glossary.py`.

## Limites, QA e integração
- Revisão inicial de capturas e verificação estática de sincronização efetuadas; validação completa do CI e ensaio Android ainda precisam de confirmação independente.
- Algumas capturas podem ser de um APK anterior ao commit-base; identificar eventual cache/versão instalada antes de concluir que uma tradução está ausente na versão compilada.
- Identificadores de tarefas de pesquisa, códigos de API, IDs, chaves de filtros e nomes de provedores são preservados.
- Não foi criado APK nem modificado o aplicativo instalado.
- A integração exige testes de tradução, compilação e revisão de conflitos com outras branches; usar PR sem merge automático.

## Resultado de QA confirmado em 22/11/2026
- PR de revisão em rascunho: https://github.com/LucianoToscano/PGBR/pull/101.
- [Workflow isolado #45044210002](https://github.com/LucianoToscano/PGBR/actions/runs/45044210002): **success**, job `translate` concluído; compila os dois tradutores Java e executa `PtBrDynamicTest` e `test_ptbr_glossary`.
- Verificação local de correspondência: 1.449 chaves no JSON e 1.449 entradas em cada tradutor; 66 cenários de tradução simulados sem divergência.
- Suíte ampliada de código [#45044210035](https://github.com/LucianoToscano/PGBR/actions/runs/45044210035) estava em execução na verificação. Sem homologação em aparelho ou geração de APK nesta branch.
