# PGBR — ARQUIVO MESTRE ÚNICO
> Identificador: PGBR-MASTER-001  
> **Fonte oficial da verdade documental:** [`main/PGBR_ARQUIVO_MESTRE_UNICO.md`](https://github.com/LucianoToscano/PGBR/blob/main/PGBR_ARQUIVO_MESTRE_UNICO.md)  
> Criado em: **21/11/2026** · Última auditoria factual deste registro: **21/11/2026**  
> Estado: **documento vivo e obrigatório; não é um selo de homologação do aplicativo.**

## Como navegar pelo Arquivo Mestre

> **A consulta começa pela leitura integral, não apenas por este índice.** Esta página é a referência documental única; snapshots não substituem verificações atuais no GitHub.

- **Governança:** [leitura obrigatória](#mestre-leitura), [identidade e escopo](#mestre-escopo), [níveis de evidência](#mestre-evidencias).
- **Produto:** [versões e estado](#mestre-versoes), [branches](#mestre-branches), [arquitetura/build](#mestre-arquitetura), [registro de recursos](#mestre-recursos), [UX e requisitos](#mestre-ux).
- **Operação:** [privacidade/terceiros](#mestre-seguranca), [backups](#mestre-backups), [prioridades](#mestre-prioridades), [protocolo de execução](#mestre-protocolo), [fontes](#mestre-fontes).
- **Auditoria:** [histórico de alterações](#mestre-historico), [inventário conservador](#mestre-auditoria), [mapa de armazenamento e limpeza](#mestre-arquivos).

**Estado resumido auditado em 21/11/2026:** última release pública identificada **LT023**; candidato **LT032** compilado em CI (Root, sem Root e módulo), **não homologado em aparelho**; `main` ainda usa código histórico. A manutenção [PR #43](https://github.com/LucianoToscano/PGBR/pull/43), já integrada à `main`, foi documental e conservadora: **nenhum recurso ou arquivo rastreado foi removido**.

<a id="mestre-leitura"></a>
## 0. REGRA MÁXIMA — LEITURA OBRIGATÓRIA
**ANTES de analisar, alterar, compilar, corrigir, integrar, criar APK, responder sobre status ou publicar qualquer versão do PGBR:**
1. **Ler este Arquivo Mestre integralmente**, depois `AGENTS.md` e os documentos de arquitetura, build, Root e release relevantes.
2. Conferir no GitHub o estado real da branch, HEAD, Actions, artifacts, releases e PRs: **este documento registra um snapshot que pode envelhecer**. Data de um commit não equivale ao número da versão.
3. Consultar código, evidências de execução e backups antes de afirmar que um recurso funciona; não confundir existência de classe, teste JVM, compilação, instalação, teste em dispositivo e publicação.
4. Nunca modificar o PokeSearch como efeito colateral; nunca apagar versões, fontes, imagens, dados, logs e backups sem inventário, comparação e confirmação de segurança.
5. Ao concluir **mudança material validada**, atualizar este mesmo arquivo com referência de branch/commit, resultados, pendências e data. **Não criar outros documentos chamados “Arquivo Mestre PGBR”** nem manter status concorrentes.
6. Se houver conflito entre este resumo e o código/CI atual, reportar a divergência, verificar evidências e corrigir este arquivo. **Nunca adaptar evidências a uma afirmação antiga.**

Este protocolo é espelhado nas instruções `AGENTS.md`, `.github/copilot-instructions.md` e no teste `pgrazor/test/test_master_governance.py`. Estes pontos de entrada orientam agentes e CI que os consultam; não garantem leitura por qualquer ferramenta externa ou novo chat sem acesso ao repositório.

<a id="mestre-escopo"></a>
## 1. Identidade, limites e missão do produto
- Nome: **PGBR** — edição brasileira independente para Android, derivada do código aberto [PGRazor](https://github.com/jpitty03/PGRazor) com atribuição MIT e avisos de terceiros preservados.
- Repositório central: [LucianoToscano/PGBR](https://github.com/LucianoToscano/PGBR).
- Objetivo: **um aplicativo central, completo e em PT-BR**, preservando joystick, mapa/teleporte, caminhada/rotas, controles de captura, gestão de inventário, automações opcionais, VGP+, buscas, eventos e configurações. **Não empobrecer, não transformar em preview com funções ausentes.**
- Produzir variantes do **mesmo projeto**: APK completo sem Root, instalador Root e módulo Zygisk quando disponíveis e verificados. Não separar recursos do aplicativo principal indevidamente.
- **PokeSearch é projeto separado:** não alterar código, repositório, assinatura, APIs, releases ou dados do PokeSearch sem tarefa independente e autorização explícita. Integração eventual exige limites e contratos claros.
- Marca visível PGBR; conservar identificadores internos necessários à compatibilidade (por exemplo, `dev.pgrazor`, IDs de módulo, propriedades e caminhos), a menos que exista migração comprovada.
- Português brasileiro como idioma prioritário; preservar funcionalidade antes de decisões estéticas.
- Projeto experimental independente; não se apresentar como afiliado à Niantic, Pokémon Company, PGSharp, Pokémod, iPogo ou outros fornecedores. Créditos e licenças são obrigatórios.

<a id="mestre-evidencias"></a>
## 2. Hierarquia de evidências e estados
Use os termos com precisão:
1. **Planejado** — requisito/decisão, sem código integrado.
2. **Implementado em fonte** — alterações presentes em um commit identificado.
3. **Teste de política/fixture/JVM aprovado** — comprova apenas a lógica coberta em ambiente de teste.
4. **APK compilado e assinado** — artefato verificado, mas não necessariamente instalável ou funcional em aparelho.
5. **Instalado e testado em dispositivo** — evidência controlada de abertura e função específica; diferenciar ARM64 físico de emulador.
6. **Release publicada** — tag + assets confirmados, diferente de um artifact temporário.
7. **Homologado** — roteiro funcional e regressões aprovados em aparelho(s) definidos, com versão, evidência e limitações documentadas.

**Regra de produto:** sucesso do GitHub Actions não comprova VGP capturando, PvP atacando, privacidade ocultando nome na Unity, scanner recebendo dados nem ausência de crash. Não divulgar uma função como corrigida quando apenas seu botão ou teste foi ajustado.

<a id="mestre-versoes"></a>
## 3. Linha do tempo e estado de versões — fotografia de 21/11/2026
| Assunto | Situação confirmada | Evidência |
| --- | --- | --- |
| Última release pública do PGBR identificada | **`v0.00.23-LT`**, com seis assets; não confundir com compilação LT032 | [Release LT023](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.23-LT) |
| Candidato de desenvolvimento mais avançado | **`v0.00.32-LT`**, integrando LT031 e correções posteriores LT027; **não está publicado como release nem homologado em celular** | [PR #40 (draft)](https://github.com/LucianoToscano/PGBR/pull/40) |
| APK LT032 sem Root | **CI completo concluído com sucesso**, com teste PvP, `tools/check.py`, compilação e empacotamento; artifact temporário de 30 dias | [Actions 44958257380](https://github.com/LucianoToscano/PGBR/actions/runs/44958257380) |
| APK LT032 Root, módulo Zygisk e sem Root | **CI completo concluído com sucesso**, três produtos verificados; ainda sem homologação de instalação real | [Actions 44958259611](https://github.com/LucianoToscano/PGBR/actions/runs/44958259611), [PR #41 (draft)](https://github.com/LucianoToscano/PGBR/pull/41) |
| Relatório local de diagnóstico VGP resgatado de LT017 | **Teste de Java/CI aprovado; em PR separado, ainda não integrado aos APKs LT032 acima** | [Actions 44958652466](https://github.com/LucianoToscano/PGBR/actions/runs/44958652466), [PR #42 (draft)](https://github.com/LucianoToscano/PGBR/pull/42) |
| `main` | Base pública **desatualizada** para execução: README/VERSION da linhagem LT001. Não interpretar `main` como último binário funcional. É agora o **local canônico deste documento**; a promoção de código será decidida em PR próprio. | [main](https://github.com/LucianoToscano/PGBR/tree/main) |

**SHA do commit do candidato sem Root:** `a55b2907c611888cefdb78c4c1167188689fb02b`.  
**SHA do commit Root QA:** `90c699fbbf8b8ba126d009cb7d695886492d202c`.  
**SHA do commit VGP diagnóstico QA:** `cae406e90b8721f1078b5b2cd6060ccffa9194d0`.  
**Artifacts GitHub registrados:** sem Root ID `14728995791` (expira 21/12/2026); Root QA ID `14728653721` (expira 21/12/2026). É obrigatório verificar nova expiração e integridade antes de divulgar links.  
**Registro de transição:** LT031 integrou PvP experimental; LT027 recebeu correções posteriores de recuperação/CI. LT032 foi criada com commit de dois pais para **não perder o histórico de nenhuma ponta**. O merge de código para `main` não foi realizado.

<a id="mestre-branches"></a>
## 4. Branches e política de integração
- `main`: documentação mestre canônica, código histórico; **não forçar reset, merge ou release sem revisar**.
- `integracao/lt032-recuperacao-20261121`: núcleo consolidado LT032 + separação de visibilidade/início VGP; PR #40.
- `integracao/lt032-root-qa-20261121`: acrescenta workflow para três pacotes LT032; PR #41.
- `integracao/lt032-diagnosticos-recuperados-20261121`: resgata relatório VGP read-only e testes; PR #42.
- `fix/lt031-pvp-vnext-executor-20261121`: histórico principal dos experimentos PvP LT031, preservado.
- `feat/lt027-pokemon-selector-20261121`: histórico do Selector Pokémon e consertos de CI, preservado.
- Branches antigas são **fontes de recuperação e comparação**, não automaticamente versões mais recentes ou seguras. Auditar arquivo a arquivo: pode haver commits exclusivos e recursos não promovidos.

**Diretriz:** desenvolvimento em branch/PR próprio; revisar diffs e conflitos, manter código/artefatos antigos, executar testes e empacotar antes de considerar merge. Não publicar release automaticamente só porque o workflow deu verde.

<a id="mestre-arquitetura"></a>
## 5. Arquitetura e requisitos de compilação
Fontes complementares obrigatórias: [BUILDING](docs/BUILDING.md), [ARCHITECTURE](docs/ARCHITECTURE.md), [ROOTED](docs/ROOTED.md), [GAME_UPDATES](docs/GAME_UPDATES.md), [THIRD_PARTY](THIRD_PARTY.md).
- `pgrazor/src/dev/pgrazor/Control.java`: regras, estado e bridge pública estável. `SettingsPanel.java` e `Pad.java`: UI/overlays; preservar APIs, keys persistidas, lifecycle e posições.
- `pgrazor/native/`: núcleo compilado C++, hosts de combate, VGP, localização e ações; contratos e metadata são específicos de versão. As pontes Java/ART e transporte tipado são componentes distintos do motor de jogo.
- `LiveFeedClient.java`, `LiveFeedData.java`, `LiveFeedFilters.java`, `PokemonSelectorDialog.java` e conectores autorizados: fontes, expiração de spawns, filtros e confirmação de teleporte.
- `pgrazor/nonroot.py` e `pgrazor/release_apk.py`: variante sem Root; `pgrazor/rooted.py`: módulo e instalador Root.
- Entrada fixada nas versões auditadas: Pokémon GO **0.437.0 ARM64**, version code **2026110902**; SHA-256 do original fixado em BUILDING: `5be1ff372d8a3aa1c9ce9f4014b3825119086feaf71aae6b0538636acafebdd3`. **Não armazenar APKM original ou segredo no Git.**
- Compilação/release via GitHub Actions e Secrets existentes; não expor keystore, tokens, credenciais, URLs assinadas, dados de sessão nem payloads de terceiros. Verificar NDK/SDK/toolchain fixados antes de trocar a versão do jogo.
- Não remover suporte Root ou sem Root. Testar assinaturas, ABI, upgrades, `build-info.json`, `root-build-info.json`, `SHA256SUMS.txt` e hash dos binários, sem presumir compatibilidade com versões novas do jogo.

<a id="mestre-recursos"></a>
## 6. Registro de recursos por situação
| Recurso | Fonte/política registrada | O que NÃO está comprovado |
| --- | --- | --- |
| VGP+ | Preservado mecanismo nativo e dois designs de ícone; LT032 distingue **Exibir botão VGP+** de **Iniciar/Parar VGP+** e mantém confirmação contra toque acidental. Sem pausa imposta por cooldown estimado. | Registro de captura/giro real em aparelho após início confirmado |
| PvP (GBL/desafio) | Controlador PvP experimental, identificação restrita da sequência VNext, testes de escopo, ações rápidas/carregadas/escudo e configurações compartilhadas com Rocket | Ataques/escudos funcionando continuamente em partida física; **segundo carregado supereficaz ainda não conectado ao executor nativo** |
| Busca Pokémon e reides | Dois filtros PGBR, seleção, resultados da fonte declarada, sprites/expiração e confirmação antes de teleporte; Selector LT027 | Resultados reais de servidores privados PGSharp/Pokémod/iPogo/PGTools; autenticidade não é inferível de UI semelhante |
| Fontes de mapa/feed | LivePokeMap, iFlowGo/RocketMap e outros conectores devem ser avaliados separadamente com logs e contratos | Disponibilidade irrestrita, taxa, cobertura, endpoint privado e permissões de terceiros |
| Eternatus | Artes para Pokébola e joystick, rotas/GPX, retomada, opções e margens de cooldown; **nunca contar doces por parada sem confirmação** | Fluxo end-to-end autônomo de pesquisa/captura/recompensa em aparelho |
| Itens e ícones | Biblioteca de `item-icons/item-ID.png` exatos e marcador neutro por ID para sprite desconhecida; preservar os designs | Correspondência completa de todos os itens, sprites e efeitos no dispositivo |
| Ocultar nome em lives | Requisito visual local `*****` em mapa e tela “Eu”, restaurável por toggle; manter `PlayerService`/RPC/login intactos | Hook exato de apresentação no jogo e cobertura integral de todas as telas |
| Localização, conta, backup e HUD | Código e políticas no repositório; isolamento e persistência por conta, overlays e testes de layout | Regressões ausentes em Samsung/Android físico e todas as escalas/rotações |
| Tema/idioma | Português brasileiro, Eternatus por padrão conforme decisão de design; botões neutros/brancos mantendo ícones originais | Tradução integral, espaçamento, cores, duplicidades e visuais finais revisados |

A disponibilidade de serviço externo é variável. Falhas `401/403`, `429 Retry-After`, filtros sem resultados e spawns expirados devem aparecer como **indisponibilidade legítima**, sem inventar coordenadas nem contornar controles de acesso.

<a id="mestre-ux"></a>
## 7. Requisitos imutáveis de UX e recursos
- Manter todos os recursos existentes: **enriquecer, nunca empobrecer**. Não sacrificar automações, autorização, opções ou menu ao redesenhar a UI.
- Botão e joystick **Eternatus** como preferência de apresentação inicial; efeito visual opcional, evitando poluição. Não alterar identidade de jogo nem cobrir áreas críticas sem opção do usuário.
- VGP: **dois ícones realmente distintos**, transparência e tamanho configuráveis, opção de imagem própria, arrasto até bordas e quatro cantos, sem duplicação visual e com proteção de início manual.
- Ícones de itens: usar sprite correto para cada ID verificado; nunca desenhar poção, reviver, ovo ou item falsamente para ID desconhecido. Preservar nome e fonte da biblioteca.
- Visual: ícones ilustrados mantidos e fundos de botões verdes convertidos para branco/neutro quando fizer parte de revisão testada; não fazer regressão de legibilidade.
- Interface em PT-BR natural, agrupamento claro de recursos, estados de execução e erro visíveis. Testar em Android real, escalas de fontes, orientações e sobreposição nas bordas.
- O app deve funcionar sem computador local para receber uma versão: CI prepara artefatos completos, **mas somente depois de verificar**.

<a id="mestre-seguranca"></a>
## 8. Privacidade, segurança e integração de terceiros
- Proteção de streamer: **somente apresentação local**. Ocultar visualmente o nome do próprio treinador como `*****` onde verificado; restaurar ao desligar. Não modificar ID, conta, login, `PlayerService.get_PlayerName`, dados de servidor, pacotes, RPC nem nomes de outros jogadores.
- Se não houver metadata/hook de tela validado, dizer **“não homologado para lives”**.
- Serviços PGSharp, Pokémod, iPogo, PGTools, PokeList e similares: usar apenas APIs autorizadas/documentadas e arquivos sob direito de uso. **Compra de APK ou existência de descompilação não provam acesso legal/técnico ao feed privado nem fornecem credenciais.** Não copiar cliente fechado ou violar autenticação, nem usar bypass de rate-limit/anti-bot.
- Preservar créditos/licenças de software e direitos de imagens, fontes e assets.
- Não anexar logs brutos públicos com nick, e-mail, tokens, latitude/longitude privada, encounter ID ou credenciais. Registrar estado técnico mínimo sanitizado.

<a id="mestre-backups"></a>
## 9. Backups, retenção e recuperação
- Bibliotecas pessoais e conversas têm APKs ZIP LT027/LT031 recuperados, na Biblioteca ChatGPT: `/PGBR/Backups-2026-11-21/`. A existência na Biblioteca **não prova cópia no Google Drive**.
- Registro do backup: Google Drive `Projetos/PokeGoBR/03_Relatorios/PGBR_BACKUP_VERIFICADO_2026-11-21.md`; é manifesto, **não backup de 180 MB**.
- Artefatos LT032 disponíveis em GitHub Actions (IDs e expiração informados na seção 3); baixar e preservar integralmente antes do vencimento. Retenção pode ser alterada; reconfirmar.
- Releases anteriores não devem ser deletadas, nem ZIP original de entrada ou backup sobrescrito. Manter opção de retorno previamente instalada e chave de assinatura correspondente.
- O arquivo antigo `POKEGOBR_ARQUIVO_MESTRE_COMPLETO_v1.0_2026-11-01.zip` em `Projetos/PokeGoBR/00_Arquivo_Mestre` pertence ao contexto histórico **PokeGoBR**, não substitui este mestre do PGBR nem deve ser misturado automaticamente.

<a id="mestre-prioridades"></a>
## 10. Fila de correções — prioridade real
**P0 — integridade e funcionalidade**
1. Preservar LT023 (release de retorno) e LT027/LT031/LT032, com checksums, certificados e evidência.
2. VGP em dispositivo: depois do comando **Iniciar**, verificar `vgpEnabled`, `vgpAllowed`, backend, foreground, opções, localização e o resultado real de captura/giro. Falha em ON não prova defeito nativo se a automação nunca iniciou.
3. PvP em dispositivo: verificar identificação GBL/desafio versus Rocket/NPC, eventos `pvpExecutorReady`, comandos de ágil/carregado/escudo e resultado; evitar ciclos sem ações. **Segundo ataque supereficaz está pendente** e exige contrato real dos botões e da energia.
4. Testar instalação, atualização, abertura, configuração e reversão tanto no APK completo quanto no módulo Root (incluindo Samsung), sem apagar dados.

**P1 — produto e apresentação**
5. Validar feeds reais e teleporte: fonte declarada, autorização, filtros Pokémon/reides, tratamento 401/403/429, expiração e origem do resultado. Não confundir testes/mock com dados de produção.
6. Privacidade de streamer visual em mapa/perfil Eu: verificar `*****` e restaurar toggle sem tocar identidade.
7. Revisar sprites corretos dos itens e ausências por ID; limpar duplicidades dos botões VGP e demais overlays; uniformizar botões e tema Eternatus.
8. Resgatar/adicionar o relatório somente leitura do PR #42 após revisar merge e validar com a compilação principal.
9. Validar percurso Eternatus, GPX longo, resgates comprovados, limites, cooldown, prioridade e pausa.

**P2 — evolução controlada**
10. Consolidar tradução PT-BR remanescente, acessibilidade, limpeza de arquivos descartáveis com inventário e verificação de referências; documentação, testes, logs e experiência de usuário.

<a id="mestre-protocolo"></a>
## 11. Protocolo obrigatório para qualquer tarefa futura
1. Abrir **este documento inteiro**; depois `AGENTS.md`, `README.md`, `docs/ARCHITECTURE.md`, `docs/BUILDING.md` e `docs/ROOTED.md`. Consultar release/arquivos relacionados à tarefa.
2. Identificar a branch real, a versão do jogo e o commit; comparar com PRs #40/#41/#42 e o último CI. Não presumir que `main` contém LT032.
3. Fazer inventário/diff antes de modificar ou “limpar”. Resgatar requisitos da história, mas só migrar código após análise de compatibilidade e direitos.
4. Estabelecer hipótese, testes de regressão e rollback. Preservar versões Root/sem Root, funções, configs, salvamento por conta e PokeSearch.
5. Implementar **mudanças mínimas verificáveis**, com nomes e IDs corretos, logs de diagnóstico que não exponham dados, sem afirmativas especulativas.
6. Rodar testes nativos/JVM/UI relevantes e `tools/check.py` quando disponível. Testes aprovados não bastam; compilar, validar hashes/certificados e instalar em aparelho antes de declarar homologação.
7. Produzir links reais de APKs/artefatos apenas quando existem e foram verificados. Nunca criar URL de arquivo presumido nem usar número de release futuro fictício.
8. Atualizar **somente este arquivo** como fonte de status geral (data, evidência, versão e pendências) em PR; notas técnicas detalhadas podem existir, mas não podem concorrer como mestres.
9. Se a tarefa não puder ser completada, registrar **exatamente** o que foi feito, o erro observado e o que depende de aparelho, acesso, contrato ou ferramenta.

<a id="mestre-fontes"></a>
## 12. Índice de evidências e documentos
- [GitHub Actions](https://github.com/LucianoToscano/PGBR/actions), [Releases](https://github.com/LucianoToscano/PGBR/releases), [Pull requests](https://github.com/LucianoToscano/PGBR/pulls).
- [LT032 — release notes de desenvolvimento](https://github.com/LucianoToscano/PGBR/blob/integracao/lt032-recuperacao-20261121/docs/releases/v0.00.32-LT.md).
- [LT032 — checklist Android real](https://github.com/LucianoToscano/PGBR/blob/integracao/lt032-recuperacao-20261121/docs/validation/PGBR_LT032_CHECKLIST_DISPOSITIVO.md).
- [LT032 — resgate de branches](https://github.com/LucianoToscano/PGBR/blob/integracao/lt032-diagnosticos-recuperados-20261121/docs/validation/PGBR_LT032_RESCUE_BRANCHES.md).
- [Diagnóstico PvP](https://github.com/LucianoToscano/PGBR/blob/integracao/lt032-recuperacao-20261121/docs/PVP_BATTLE_ASSISTANT_LT028.md).
- [Privacidade do streamer](https://github.com/LucianoToscano/PGBR/blob/integracao/lt032-recuperacao-20261121/docs/PRIVACIDADE_STREAMER_MOD_APURACAO.md).
- [Árvore histórica do PGBR](https://github.com/LucianoToscano/PGBR/tree/main).
- [Biblioteca Pokémon GO no Drive](https://drive.google.com/drive/folders/1TPgVIGAWkY2Ez1VS3bHAitv9Y_NpczbK) — patrimônio de pesquisa e sprites; **não é prova de licença nem código-fonte oficial**.

<a id="mestre-historico"></a>
## 13. Registro de atualizações deste mestre
| Data | Evento | Estado |
| --- | --- | --- |
| 21/11/2026 | Criado um **Arquivo Mestre Único** e definida hierarquia de evidências, estado LT032, backups, riscos e prioridades. | Fonte canônica no `main`; outras branches podem conter espelhos idênticos necessários à leitura local, sempre subordinados à versão da `main`. |
| 21/11/2026 | Verificado sucesso de CI LT032 sem Root, Root+Zygisk e teste isolado VGP. | **CI verde; dispositivo não homologado; PRs #40/#41/#42 ainda drafts.** |
| 21/11/2026 | Inventário conservador de `main` e três variantes LT032; proposta de higiene documental no [PR #43](https://github.com/LucianoToscano/PGBR/pull/43), sem excluir arquivos nem promover código de LT032. | **1.966 arquivos no snapshot da `main`; 2.176 no candidato LT032; 7 grupos idênticos preservados. CI documental aprovado e PR #43 integrado à `main` (commit `51860388ac9e297c9a860413f25dcb0b562f2214`).** |
| 21/11/2026 | Organização navegável do Mestre, reparo de referências LT013, inclusão de guia de PR e CI documental read-only. | [CI #44972179155](https://github.com/LucianoToscano/PGBR/actions/runs/44972179155) e [CI #44972174481](https://github.com/LucianoToscano/PGBR/actions/runs/44972174481) aprovados; nenhuma exclusão, APK ou homologação nesta auditoria. |
| 21/11/2026 | Integração da governança e limpeza documental com zero exclusões; [PR #43](https://github.com/LucianoToscano/PGBR/pull/43) fundido à `main`. | [CI pós-merge #44972328792](https://github.com/LucianoToscano/PGBR/actions/runs/44972328792) aprovado; commit `51860388ac9e297c9a860413f25dcb0b562f2214`. Sem builds nem teste de aparelho novos. |

**Regra de atualização:** acrescente uma linha com data, novo estado e evidência para cada conclusão relevante. Preserve histórico em Git; nunca apague status antigo sem explicar a substituição.

<a id="mestre-auditoria"></a>
## 14. Auditoria conservadora e organização — PR #43 integrado (21/11/2026)

**Referência detalhada (não é mestre concorrente):** [docs/maintenance/PGBR_AUDITORIA_CONSERVADORA_20261121.md](docs/maintenance/PGBR_AUDITORIA_CONSERVADORA_20261121.md). Este documento é a fonte canônica na `main`; o relatório técnico vinculado é auxiliar, e o PR #43 foi revisado por critérios objetivos, aprovado no CI documental e integrado.

- **Inventário GitHub completo (árvore não truncada):** `main` no commit `284e5a583e6de9fcb256ae5b213bb4b352a09819` possui **1.966 arquivos rastreados**, com 34.782.810 bytes de blobs. O candidato LT032 no commit `15252c491d0c823ceb924516ae950abd685e931a` possui **2.176 arquivos**, com 38.012.286 bytes. Diferença de caminhos: 210 adições, 123 modificações e **nenhuma remoção** do inventário da `main`.
- **Outras árvores examinadas:** PR #41, commit `fb42bbf37783cd458772dd9bf3896ee020d4854c` (2.177 arquivos); PR #42, commit `a24aa693aefb858ff0f350e1e13257bc5b01de22` (2.181 arquivos). As branches não foram fundidas entre si nem promovidas à `main`.
- **Duplicidades byte a byte:** um grupo em `main` (logo SVG) e sete grupos no LT032 (cópias de logos e cinco sprites de bagas para pontos de consumo diferentes). **Preservados integralmente**: mesma hash não prova que links, pacotes Android ou caminhos sejam intercambiáveis.
- **Higiene não destrutiva proposta:** corrigir a referência quebrada do arquivo de recuperação em `PGBR_IMPORTACAO.md`, manter `AGENTS.md` rastreável removendo sua entrada do `.gitignore`, acrescentar apenas ignores de resíduos gerados, tornar o README mais preciso sobre LT001/LT023/LT032 e fornecer links diretos dos assets oficiais LT023 já confirmados.
- **Controle repetível proposto:** `tools/repo_inventory.py` realiza **somente leitura** do checkout (`git ls-files`, SHA-256, classificação e alertas); `pgrazor/test/test_repo_inventory.py` exercita preservação de arquivos. O script não apaga, move nem reescreve nada. A execução do CI documental foi verificada: [PR #43 — Actions 44972179155](https://github.com/LucianoToscano/PGBR/actions/runs/44972179155) e [push 44972174481](https://github.com/LucianoToscano/PGBR/actions/runs/44972174481) terminaram com sucesso. Este CI não compila APK, não testa runtime e não faz validação física.
- **Situação da mudança:** [PR #43](https://github.com/LucianoToscano/PGBR/pull/43) **integrado à `main`**, após CI documental aprovado. A [auditoria pós-merge](https://github.com/LucianoToscano/PGBR/actions/runs/44972328792) também passou. Não foi criada release/APK nesta manutenção. Compilação, instalação física, VGP, PvP e homologação da LT032 permanecem com o estado descrito nas seções anteriores, sem nova alegação de funcionamento.

**Critério permanente de limpeza:** primeiro consultar este mestre, depois inventariar, provar uso/ausência de uso por referências, comparar hashes, preservar licenças e rollback, e testar Root/sem Root. Somente excluir em mudança específica revisada, com preservação verificada. **Tamanho, nome antigo ou conteúdo duplicado não autorizam exclusão.**

<a id="mestre-arquivos"></a>
## 15. Organização de armazenamento e limpeza segura

**Regra de separação:** o código controlado por Git, os binários distribuídos, o patrimônio histórico e o acervo de referência são categorias diferentes. **Não mover nem apagar** somente porque dois arquivos têm o mesmo hash, um nome antigo ou tamanho grande.

| Camada | Local de referência | O que manter separado |
| --- | --- | --- |
| Arquivo Mestre oficial | [GitHub `main/PGBR_ARQUIVO_MESTRE_UNICO.md`](https://github.com/LucianoToscano/PGBR/blob/main/PGBR_ARQUIVO_MESTRE_UNICO.md) | Governança e estado único; o Drive mantém apenas apontamentos para esta cópia |
| Ponto de entrada do Drive | [PGBR/00_Arquivo_Mestre](https://drive.google.com/drive/folders/1aAXywzA-wjYhsk9ya4O_4-rgDeLT-DU8) | `LEIA_PRIMEIRO_PGBR.txt` aponta para o mestre; pacotes históricos PokeGoBR não mudam a identidade deste projeto |
| APKs e QA | [PGBR/01_APKs_QA](https://drive.google.com/drive/folders/1MT7O1yRzRbIMHxGpWI_HfW351I77EwGD) | Não equivale a release publicada ou homologação |
| Fontes | [PGBR/02_Codigo_Fonte](https://drive.google.com/drive/folders/1KfEIB-dNU2MIqIo_inSWepe8L-MrxVFb) | GitHub define revisão de fonte; cópias no Drive são auxiliares |
| Relatórios | [PGBR/03_Relatorios](https://drive.google.com/drive/folders/1gTfmZKCcJbnsYnKBt6KXfAmgIGYQYKkk) | Evidências e manifestos, não senhas nem dados pessoais |
| Builds/módulos | [PGBR/04_Builds_e_Modulos_PGBR](https://drive.google.com/drive/folders/1YfJOjth-WEb453gg4pyvtjQNRk1ReOXB) | Identificar versão, assinatura, checksums, origem e prazo de retenção |
| Área restrita | Pasta privada separada no Drive (não expor caminho/credenciais em repositório público) | Assinatura, inputs privados e dados sensíveis; preservar acesso restrito |
| Backups históricos | Biblioteca ChatGPT `/PGBR/Backups-2026-11-21/` | Cópias LT027/LT031; não afirmar que existem no Drive sem transferência verificada |

**Resultado da inspeção do Drive nesta revisão:** a pasta do mestre também abriga documentos de referência intitulados **PokeGoBR**. Foram preservados sem renomear, apagar ou mover, para não quebrar vínculos; devem ser tratados como contexto histórico, **não como um segundo mestre do PGBR**. A verificação da pasta não é inventário exaustivo de toda a Biblioteca Pokémon GO nem dos segredos privados.

**Higiene técnica proposta:** a verificação de documentação e inventário usa somente código-fonte de repositório com permissões `contents: read`, sem executar scripts de exclusão, sem downloads de input privado e sem publicações. As alterações documentais foram integradas pelo [PR #43](https://github.com/LucianoToscano/PGBR/pull/43) após CI aprovado; o [CI pós-merge 44972328792](https://github.com/LucianoToscano/PGBR/actions/runs/44972328792) também passou. A inspeção do GitHub identificou **zero arquivos rastreados com extensões típicas de chaves, APKs privados ou logs gerados**; isso **não equivale a auditoria de segredos no conteúdo**, nem autoriza exclusões.

**Governança de cada limpeza posterior:** preservar a versão atual, mapear consumidores e direitos, demonstrar que o item não é usado no Android Root/sem Root ou em automações, propor lista exata em PR separado, validar testes e rollback, e somente então considerar apagar. Não usar tamanho ou igualdade byte a byte como critério único.

---
## 22/11/2026 — Adendo técnico: scanner, sniper, feeds e PokeSearch

**Natureza deste registro:** consolidação documental solicitada em 22/11/2026 dos estudos deste chat; **não** é implementação, build, APK, correção funcional, autorização de fornecedor ou nova versão do PGBR. Registro incremental e reversível, mantendo intactos recursos, código e linhas LT023/LT032. O projeto PokeSearch é autônomo e não é alterado por este adendo.

### 1. Arquiteturas estudadas e origem dos dados

- **Mapeamento ativo histórico:** workers, pontos de spawn, tempo de desaparecimento, geometria e consultas de mapa. Exemplos de estudo: [spawnScan](https://github.com/TBTerra/spawnScan), [Monocle](https://github.com/ZeChrales/Monocle), [Monocle fork](https://github.com/Hydro74000/Monocle), [RocketMap](https://github.com/RocketMap/RocketMap), [RealDeviceMap](https://github.com/RealDeviceMap/RealDeviceMap) e [MAD](https://github.com/Map-A-Droid/MAD). Os códigos legados **não** comprovam acesso atual ou autorizado aos servidores de Pokémon GO.
- **Agregação/feeds:** coleta de observações de fontes documentadas, normalização, validação, deduplicação, cache, filtros e lista/mapa/alertas. [PKMSpawn](https://github.com/ashokvarmamatta/pkmspawn) usa WebView/mapas externos e contém lista de provedores comentada; o arquivo com mais de dez sites **não** prova integrações vivas. [ScanGO](https://github.com/pompobit/ScanGO) é referência de interface. [Pogo-Scanner-Android](https://github.com/azlan194/Pogo-Scanner-Android) revela dependência de serviço externo com token/Protobuf, **não** demonstra permissão ou disponibilidade de API pública.
- **Feeder:** encaminhamento normalizado de observações para consumidores via JSON/HTTP e duração limitada; [PSSnipeFeeder](https://github.com/walaoaaa1234/PSSnipeFeeder) e [PogoLocationFeeder](https://github.com/5andr0/PogoLocationFeeder) servem como referências históricas, não como fontes comprovadas.
- **Processamento moderno separado da coleta:** [Golbat](https://github.com/UnownHash/Golbat) é um processador experimental de dados recebidos; [ReactMap](https://github.com/WatWowMap/ReactMap) apresenta mapa e filtros (release **v1.59.0 de 19/11/2026** verificada nesta pesquisa); [PoracleNG](https://github.com/jfberry/PoracleNG) recebe webhooks e gera alertas; [Koji](https://github.com/TurtIeSocks/Koji) auxilia no tratamento geográfico. Esses projetos **não** constituem, isoladamente, um feed público de spawns.
- **Dados estáticos, formatos e testes:** [PokéAPI](https://pokeapi.co/docs/v2) fornece catálogo, **não** coordenadas ao vivo; PokeMiners e masterfiles/sprites exigem verificação de atualização e direitos; [POGOProtos](https://github.com/AeonLucid/POGOProtos) é histórico; [Appium UiAutomator2](https://github.com/appium/appium-uiautomator2-driver) serve a testes autorizados de apps próprios.

**Resultado da busca de 22/11/2026:** não foi identificada API pública documentada, autorizada e comprovadamente operacional para aparições atuais de Pokémon GO. Feeds privados e produtos proprietários (PGSharp, Pokemod, iPogo, PGTools etc.) não devem ser tratados como endpoints públicos nem copiados sem licença, contrato e verificação. A ausência de API encontrada **não** prova inexistência universal de fontes privadas. Dados de ocorrência requerem fonte, hora de observação, validade e origem; espécie/sprite não comprova encontro.

### 2. Contrato de observações e filtros a considerar em futura integração

Esquema neutro proposto no kit Kotlin: `id`, `speciesId`, `name`, latitude/longitude finitas nos intervalos válidos, `observedAt` obrigatório, `despawnsAt` opcional, `cp`, `ivPercent`, `form`, `source`, `kind`. Tipos `LIVE`, `HISTORICAL`, `ESTIMATED`, `SIMULATED`; estados de exibição `SOURCE_REPORTS_ACTIVE`, `RECENT_UNCONFIRMED`, `STALE`, `EXPIRED`, `HISTORICAL`, `ESTIMATED`, `SIMULATED`, `FUTURE_TIMESTAMP`. Uma fonte marcar LIVE não constitui prova independente de presença: `SourcePolicy` local, timestamps e autorização precisam ser verificados.

**Dois filtros planejados no PGBR (sem implementação neste PR):**
1. **Pesquisa global por espécie:** nome/ID, forma, IV, CP, tipo de observação, proveniência, expiração real quando fornecida.
2. **Radar por área:** ponto/região, raio Haversine, distância, ordenação, tempo de coleta, estado e origem; mapa/lista com dados vazios ou indisponíveis tratados explicitamente.

Os dois filtros devem consumir o **mesmo repositório de observações**, preservando interfaces e filtros já existentes, inclusive as PRs LT025/LT026/LT027 ainda não integradas na `main` quando auditadas. O toque em resultado e qualquer mecanismo de localização/teleporte são **integrações separadas, pendentes de avaliação e validação**; este estudo não implementa teleporte, captura automática, cliente alterado, consulta à API privada do jogo ou bypass.

Regras técnicas: não inferir despawn ou IV/CP ausentes, não transformar histórico/estimativa em atual, deduplicar usando origem+ID, não aceitar atualização mais antiga sobre a recente, limitar idade de observação sem horário de despawn, preservar proveniência no GPX/GeoJSON, tratar timezone UTC e estado de fonte fora do ar. Android futuro: adaptadores documentados/permitidos, Room, Flow, Retrofit/OkHttp, Compose e WorkManager com limites de background; testar sem alterar a linha oficial.

### 3. Artefatos e níveis de evidência

Conversa forneceu `SpawnFeedKit.kt`, pesquisa HTML, `pesquisa-pokemon-go-android-arquitetura.zip`, `pesquisa-pokemon-go-android-validada.zip`, `pesquisa_pokemon_go_scanners.zip` (apenas Markdown) e o pacote `PokeSearch_Observacoes_v2_2026-11-22.zip` com `SpawnObservationsV2.kt`, `MapNormalizer.kt`, fixtures, auditoria e testes. Arquivos anexados à conversa **não estão integrados ou versionados no PGBR por esta atualização**.

No kit v2, **37 verificações Kotlin/JVM** foram executadas e aprovadas no ambiente da conversa em 22/11/2026 (`bash testar.sh`); isso verifica somente o núcleo neutro, **não** Room, Compose, interface, backend, fonte live, Gradle, APK, instalação física nem compatibilidade com PGSharp. O kit anterior tinha fluxo simulado e foi corrigido quanto à validação de coordenadas, XML GPX, duplicatas e distinção temporal. `despawnsAt=null` significa desconhecido; `isFresh` não pode considerar um registro eternamente ativo por esse motivo.

**Pendências antes de qualquer alteração funcional:** inventariar branch candidata LT032 e integrações de feed existentes; verificar contratos, permissões e direitos de fontes; escolher fonte realmente disponível; implementar adaptador em componente separado; validar filtros em testes repetíveis e no Android; inspecionar colisões com PRs abertas; abrir PR própria sem apagar nem redefinir versões. **Nenhum APK foi gerado nesta ação documental.**


<!-- REGISTRO-DRIVE-20261122 -->
## 22/11/2026 — Registro transversal da organização do Google Drive e biblioteca Pokémon GO

> **Natureza:** histórico documental das operações realizadas na conversa "Banco de Dados Pokemon Go", verificado por listagens e metadados do Google Drive. **Não é alteração de código, integração de recursos, teste de APK ou homologação.** Mantêm-se integralmente os registros anteriores; divergências entre snapshots devem ser resolvidas consultando GitHub/Drive atuais, não sobrescrevendo o histórico.

### Estrutura central e migração de pastas

Pasta-pai: [Pokemon Go, em Projetos](https://drive.google.com/drive/folders/180wu9mqDYttMwYFP3XxXEUmOShLyvTJv). Foram realocadas as seis pastas de origem dentro dessa pasta-pai; depois a raiz foi consolidada e verificada com **exatamente cinco projetos**: [PGBR](https://drive.google.com/drive/folders/1RJ2NuBv-tnEDf9Pcxgn1I7-9CTdPGAHX), [PokeSearch](https://drive.google.com/drive/folders/1pHnwNFYlDxxt9g5PtdYWWFAylBkvVyRU), [PvPPokeGo](https://drive.google.com/drive/folders/1ybogVRRnaOe0KtWjlovxS56bJOi_JewI), [PGSharp](https://drive.google.com/drive/folders/1WAEmEpZmvUw9dDiapNcygtmZQq6XIB_j) e [Banco de Dados Pokemon Go](https://drive.google.com/drive/folders/1TPgVIGAWkY2Ez1VS3bHAitv9Y_NpczbK).

As pastas legadas **PGSharpLOT**, **PgSharp Publico**, **PokeGoBR** e **PokeLOT** deixaram de existir como áreas paralelas; os conteúdos identificados foram redistribuídos antes de excluir os contêineres vazios. Material legado de PokeGoBR foi incorporado à área PGBR; PvPPokeGo e PGSharp foram retirados de PokeLOT; builds PGBRLOT que estavam no PokeSearch foram para PGBR. Arquivos de terceiros, dossiês, referências e arquivos soltos identificados foram movidos às áreas correspondentes. Essas mudanças no Drive não significam merge entre repositórios GitHub nem validam novas versões.

### Biblioteca única de imagens, dados, referências e APKs

O [Banco de Dados Pokemon Go](https://drive.google.com/drive/folders/1TPgVIGAWkY2Ez1VS3bHAitv9Y_NpczbK) permaneceu a referência compartilhada, organizada em catálogos; sprites/imagens/sons/modelos; dados, rankings e inventários; mapas/radar/eventos; APKs de referência; links/guias/ferramentas; arquivo ZIP original; backups e históricos; e quarentena de duplicatas.

[Acervo visual](https://drive.google.com/drive/folders/1MnVVAsDOvam1laU6Ahv-whIPpBF0gz68): 01 Pokémon completos (normal, Shiny, formas e trajes); 02 recortes/partes/sprite sheets; 03 ícones e símbolos; 04 itens/acessórios/roupas; 05 efeitos/animações; 06 layouts/menus/interface; 07 fundos/cenários/telas; 08 modelos 3D; 09 sons/áudio; 10 eventos/banners/figurinhas; 11 avatares/personagens; 90 acervos de origem e materiais técnicos. **Shiny e formas** pertencem ao acervo de Pokémon, e os recortes de olhos/peças, ícones de interface e itens permanecem separados por finalidade. Acervos de origens distintas são preservados sem mesclar arbitrariamente arquivos de mesmo nome.

Os [catálogos](https://drive.google.com/drive/folders/1rH8aNhTP6HFy4VevrXlkGBPT75w5qXdl) foram divididos em manifestos/catálogos, inventários/índices, auditorias/validações e guias de aproveitamento. Há [quarentena separada para duplicatas](https://drive.google.com/drive/folders/1Z-DQuYIhsu-kgBg8nEXPJKEk8jumPUuM), inclusive itens apenas suspeitos de repetição: **não apagar por igualdade de nomes nem por aparência**. O ZIP original foi preservado. Pacotes ZIP não foram todos extraídos/classificados individualmente.

No [acervo de APKs](https://drive.google.com/drive/folders/1ghacTFCc7LpRnvP9GqmcjBpfN9joAobt), a pasta de ferramentas externas foi verificada com **74 itens** distribuídos em 64 instaladores/pacotes, 6 arquivos descompilados, 2 projetos externos e 2 bancos de configuração; o acervo PokeSearch de releases tem área separada. São **referências**, não código oficialmente licenciado ou APK funcional verificado apenas por estar armazenado.

### Privacidade, integridade e tarefas pendentes

**Permissões constatadas durante a organização:** a biblioteca Banco de Dados Pokemon Go permite leitura por link; a pasta PokeSearch tinha compartilhamento com permissão de edição por link (situação que exige revisão); PGBR, PvPPokeGo e PGSharp são pastas privadas. **Não transportar chaves, PGSData/PGSStats pessoais, configurações de conta, capturas privadas ou APKs internos à biblioteca pública sem rever permissões e classificar cada item.** Não expor links de material privado ou secretos em repositórios públicos. O backup marcado como privado do PokeSearch permaneceu separado.

Movimentos de arquivos do Drive **não atualizam automaticamente** caminhos em builds, GitHub Actions, scripts, Gradle, referências em documentos ou permissões herdadas. Verificar os consumidores antes de corrigir caminhos. Não declarar conclusão total da consolidação privada, equivalência de conteúdo por hash, compatibilidade dos APKs, autorização de uso de código de terceiros ou funcionamento de automação sem auditoria técnica. Os arquivos originais de código, histórico, versões, backups e licenças devem ser preservados.

### Protocolo global — desenvolvimento paralelo sem perdas

1. Ler integralmente o mestre canônico e sua documentação vinculada, depois conferir HEAD, PRs e estado atualizado do repositório oficial.
2. Nunca editar a partir de cópia antiga nem sobrescrever mudanças realizadas em outros chats.
3. Usar **branch independente por tarefa**, registrar commit-base/arquivos afetados/mudanças/testes e preparar Pull Request, **sem publicar diretamente na `main`**.
4. Preservar funcionalidades, arquivos de referência, assets, backups, histórico e direitos de terceiros; enriquecer o conteúdo, nunca reduzi-lo.
5. Distinguir **pedido, documentação, arquivo presente, implementação, teste local, CI, teste em aparelho e release publicada**. Documentar limitações e pendências sem promover intenção a fato.

### Registro específico — PGBR

Estrutura Drive verificada: `00_Arquivo_Mestre/`, `01_APKs_QA/`, `02_Codigo_Fonte/`, `03_Relatorios/`, `04_Builds_e_Modulos_PGBR/` e `99_Privado_Restrito/`. O arquivo mestre e pacotes de fontes do antigo PokeGoBR foram preservados; builds e módulos PGBR que estavam indevidamente em PokeSearch foram realocados. Materiais de assinatura/inputs privados ficaram segregados. Nenhuma chave ou credencial é reproduzida neste documento.

A biblioteca geral contém recursos gráficos e APKs **de referência**, mas os arquivos de produção do PGBR não foram massivamente transferidos para local de leitura pública. Este registro **não prova** revisão de código, restauração da VGP, validação de teleporte, escudos/PvP, funcionamento de scanner, nem build Android. Continuam obrigatórios o próprio Arquivo Mestre Único, o inventário de manutenção, a verificação da branch candidata e das PRs paralelas antes de qualquer modificação funcional.

