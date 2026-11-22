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

---

## 22/11/2026 — Auditoria de atualização PGRazor v0.3.4 → v0.3.6 e portabilidade para PGBR

**Registro de rastreabilidade, não homologação.** A fonte upstream [jpitty03/PGRazor](https://github.com/jpitty03/PGRazor) publicou a [v0.3.5](https://github.com/jpitty03/PGRazor/releases/tag/v0.3.5) em 18/11/2026 e a [v0.3.6](https://github.com/jpitty03/PGRazor/releases/tag/v0.3.6) em 21/11/2026 UTC (20/11, horário de Brasília). O diff oficial entre as tags `v0.3.4...v0.3.6` contém **32 commits e 90 caminhos modificados/adicionados**, sendo 35 arquivos do motor nativo, 22 arquivos Java, 10 de testes, 2 de notas de release e 21 outros. Fonte: [compare entre tags](https://github.com/jpitty03/PGRazor/compare/v0.3.4...v0.3.6).

**Ponto de partida verificado:** o candidato PGBR LT032 `integracao/lt032-diagnosticos-recuperados-20261121`, commit `84f1b5f63c668ed7b0e61b5959e53dd4c6abd274`, não é a `main` histórica. Inspeção da árvore LT032 confirma ausência dos arquivos novos `SettingsSearch.java`, `SessionStats.java`, `session_stats.cpp`, `route_gift.cpp` e `max_catch_flag.h`. A ausência de um arquivo **não** prova por si só ausência de toda função análoga em outros módulos; a presença também não prova execução funcional.

| Fonte/funcionalidade | Encaminhamento PGBR | Evidência / pendências |
| --- | --- | --- |
| v0.3.6 — Hotspots: data/hora no fuso local, mudança sazonal UTC, 12/24 h | **Implementado em fonte em PR #51 (draft)** sobre a LT032. Preserva 14 coordenadas, visual PT-BR e campo `Spot.utc` de compatibilidade. | [PR #51](https://github.com/LucianoToscano/PGBR/pull/51), branch `feat/pgrazor-v036-hotspots-20261122`; testes JVM de regressão ampliados; **CI específico de Hotspots e auditoria aprovado em [Actions 44986590988](https://github.com/LucianoToscano/PGBR/actions/runs/44986590988)** (Java 17/JVM + inventário); **sem APK ou homologação Android**. |
| v0.3.5 — busca universal de configurações e interface agrupada Play/Automation/App | **Pendente de portabilidade controlada** para UI PGBR. | Novo `SettingsSearch.java` upstream; preservar PT-BR, menu/ícones Eternatus, VGP, entradas existentes, estado e dimensões. |
| v0.3.5 — troca de presentes com Mateo nas Rotas | **Pendente de portabilidade/validação nativa**. | Upstream `route_gift.cpp/.h`, `route_gift_policy.h`; verificar contratos reais e habilitação opcional antes de integrar. |
| v0.3.5 — joystick não recentralizar, Rotas manuais, auto-raid com passe grátis, captura no mapa seguindo regras de transferência, tablets e barras temáticas | **Pendente de diff seletivo e regressão**. | Áreas já alteradas por LT032 e pela identidade PGBR; não sobrescrever `Pad.java`, navegação, automação ou customizações. |
| v0.3.6 — nível mínimo de IV para preservar ao transferir (0–100%) | **Pendente de integração Java+C++ e migração de preferências**. | Verificar `transfer_policy.*`, `evolve_transfer.cpp`, `Control.java` e UI; padrão conservador para contas existentes. |
| v0.3.6 — busca de missões por doces e espécie (PoGoCoordinates) | **Pendente de adaptação aos feeds/filtros PGBR**. | Testar sem credenciais ou endpoints privados presumidos, incluindo retorno vazio e limites. |
| v0.3.6 — estatísticas por sessão 1h/24h | **Pendente de integração completa**. | Novos `SessionStats.java`, `SessionStatsCard.java`, `SessionStatsHistory.java` e `session_stats.*`; isolar contas e preservar privacidade. |
| v0.3.6 — VGP+ com spawns de iscas, PokéStop, Rotas, encontros diários e limpeza de entidades capturadas | **Prioridade alta; pendente de portabilidade e validação em aparelho**. | Estudar `vgp_actions.cpp`, `vgp_candidates.cpp`, `vgp_controller.cpp`, `vgp_incense.h`, `encounter_host.cpp`; preservar controles manuais LT032. |
| v0.3.6 — overlay opcional de ginásios desligado por padrão em instalação nova | **Implementado em fonte no [PR #53](https://github.com/LucianoToscano/PGBR/pull/53)**; escolhas salvas continuam valendo. | Teste JVM de preferências e auditoria **aprovados no [CI 44986715112](https://github.com/LucianoToscano/PGBR/actions/runs/44986715112)**; sem APK, merge ou homologação de aparelho. |
| v0.3.6 — ginásios: frutas/defensores, erros explícitos, menos leitura do servidor em reservas | **Pendente de teste e merge seletivo**. | `gym_runtime.cpp`, preferências, runtime e limites de solicitações. |
| v0.3.6 — pesquisa automática com teto de 80 operações/10 min e pausa/retomada | **Pendente de contrato e integração**. | `research.cpp` e configuração; manter controles de segurança e não confundir aviso com contagem real. |
| v0.3.6 — correção BSOA/tela azul e encerramento normal de resumos raid/Max/Rocket | **Pendente de auditoria de callbacks e testes físicos**. | Não substituir o fluxo de Quick Catch ou afirmar correção de travamento sem testes em Android. |

**Protocolo de atualização:** não realizar merge completo upstream→PGBR, reset, substituição massiva ou atualização de `VERSION` com a versão upstream. Cada grupo deve partir do HEAD atual LT032, inspecionar contratos `0.437.0`, checar branches concorrentes, preservar atribuição MIT, criar PR isolado com teste JVM/nativo, passar CI Root e sem Root, e somente depois testar abertura, execução e rollback em aparelho físico. A numeração PGBR continua própria; `PokeSearch` e outros projetos ficam intocados.

**Status no encerramento desta auditoria:** port de Hotspots presente em fonte no PR #51 (**CI JVM verificado**) e padrão conservador de overlay de ginásios em fonte no PR #53 (**CI JVM verificado**). Ambos ainda **não integrados à branch-base, não homologados e sem nova release ou APK**. As demais funcionalidades listadas são *candidatas à integração* — não anunciá-las como implementadas.

## 22/11/2026 — Integração incremental PGRazor 0.3.5/0.3.6 na LT032 (PR #54)

**Atualização posterior à tabela de planejamento acima:** as situações “pendente de portabilidade” daquela tabela retratam o instante anterior ao desenvolvimento do [PR #54](https://github.com/LucianoToscano/PGBR/pull/54). São preservadas como histórico, mas as alterações abaixo já estão **implementadas em fonte** na branch experimental `integracao/lt032-pgrazor-035-036-20261122`. Nada foi integrado à `main` ou à branch-base LT032; não houve homologação em dispositivo e nenhuma nova release foi publicada.

**Proveniência e proteção:** PR #54 é *draft*, base `integracao/lt032-diagnosticos-recuperados-20261121` @ `84f1b5f63c668ed7b0e61b5959e53dd4c6abd274`. O port não é um merge em massa; preserva PGBR, PT-BR, controle VGP de início manual, PvP, HUD Eternatus, interfaces/feeds próprios, referências MIT e código de Root/sem Root. O PokeSearch não foi modificado.

| Grupo | Resultado atual em fonte | Validação / limites |
| --- | --- | --- |
| Hotspots e ginásios por padrão | PRs [#51](https://github.com/LucianoToscano/PGBR/pull/51) e [#53](https://github.com/LucianoToscano/PGBR/pull/53) portados para a branch do PR #54 | CI Java de cada PR aprovado, preferências existentes preservadas |
| Pesquisa de configurações | `SettingsSearch.java` + regressão JVM; resultados navegáveis nas categorias reais do PGBR em PT-BR | Depende de verificação visual de UI, menus e acessibilidade em Android |
| Pesquisa Candy | Catalogação do tipo `Candy`, filtro por espécie, construção do JSON; evita buscas globais sem espécie/tarefa/região; `CandyQuestPolicy` e teste JVM | Nenhuma disponibilidade de dados externos assumida |
| Transferências por IV | Intervalo 0–100%, slider e campo numérico; preservado modo perigoso legado 101 sob confirmação; o protocolo de preferências **PGBR** usa bits altos de `transferProtect` em vez de substituir a versão do transporte. Política nativa compartilhada com Quick Catch, transferências externas e evolução | Cenários de C++ host e JVM adicionados; **transferências são irreversíveis** e exigem ensaio controlado em dispositivo antes da ativação de QA |
| Estatísticas de sessão | Histórico isolado por conta 1h/24h, XP, poeira, capturas, encontros, PokéStops, distância, ovos; nativo somente leitura e painel PT-BR | Sem dados reais antes da leitura e sem prova de exactidão em dispositivo; leitura de counters pode incluir outra sessão da conta |
| VGP+ e mapa | Port seletivo do commit upstream `eefdcc3fb2...`: isca, PokéStop, Rota, encontro diário e remoção de marcadores de capturas; port seletivo de captura no mapa para política de transferência | Compatibilidade `0.437.0` e funcionamento exigem CI Android e testes físicos; nenhum feed privado envolvido |
| Quick Catch | Port do commit `72770ef6...`: encerrar resumos de raid/Max/Rocket pelo fluxo OK nativo, sem ocultar saída do jogo prematuramente | Tela azul / travamento **não declarado corrigido** até teste real |
| Ginásios nativos | Port dos commits `75f8d627...`, `22aab3db...`: `PromiseKeeper` da versão 0.437.0, mensagens de recusa, respeito às reservas de frutas e redução de consultas | A LT032 já possui `find_open_gym`/`deploy_first` para priorizar ginásios com vaga antes de alimentar; preservar esta lógica própria e verificar regressões de runtime |
| Pesquisas, joystick, rotas, raid | Port de limite 80 operações/10 minutos, exclusões quando reclamação aguarda, tolerância de câmera 1,5s, checagem de rota manual e passe de raid recém-obtido | Regressões Android e de runtime pendentes |
| Mateo | `route_gift.cpp`, política, teste nativo, transporte de opção (frame 22) e toggle em PT-BR; padrão opt-in | Nunca operar troca de Pokémon; homologação física de UI, botão e detecção do NPC pendente |

**Execuções confirmadas:** [CI de QA somente leitura #44987616337](https://github.com/LucianoToscano/PGBR/actions/runs/44987616337), [CI pós-correções de ginásio #44987682066](https://github.com/LucianoToscano/PGBR/actions/runs/44987682066) e [CI pós-captura de mapa #44987734613](https://github.com/LucianoToscano/PGBR/actions/runs/44987734613) aprovados na sequência; provas são de JVM/C++ local e governança, não de APK instalado.

**Compilação Android:** a branch ativou o workflow existente `pgbr-lt032-consolidado-apk.yml` com artefato de QA de nome distinto `PGBR-LT032-PGR036-APK-completo-QA`; [execução #44987764532](https://github.com/LucianoToscano/PGBR/actions/runs/44987764532) iniciada, conclusão ainda não confirmada no instante deste registro. Não afirmar que APK existe até verificar conclusão e asset. O workflow testa apenas a variante sem Root. O candidato de integração permanece separado da versão pública LT023 e do histórico da LT032.

**Pendências prioritárias:** revisar eventuais falhas no build, fixture PvP/VGP, ABI e versão de transporte, cruzar diferenças remanescentes (UI nativa de tema, priorização de defensor de ginásio), compilar variantes Root/sem Root e testar instalação, VGP capturando/girando, Quick Catch, Mateo, missões, transferência reversível em conta de QA, PvP e autenticação. Registrar hashes, certificados e rollback antes de promover. Não usar status de código em PR como selo de funcionamento real.
---

## 22/11/2026 — Portabilidades PGRazor PR #10 (joystick) e PR #11 (ART Root)

**Fonte externa ainda em revisão (upstream, 22/11/2026):**
- https://github.com/jpitty03/PGRazor/pull/10 — `feat(joystick): add directional lock badge and continuous steering`, PR aberta no upstream, commit `5ca36cbec9f13817ecc332ac0bf911d80b1fea6c`.
- https://github.com/jpitty03/PGRazor/pull/11 — `Fix ARM64 ART hook backups overwritten by Vector`, PR aberta no upstream, commit `b9d0d2ce445024b143894484f69ccbc2aaefb6c9`.

**Base PGBR:** branch candidata `integracao/lt032-pgrazor-035-036-20261122`, commit `f9047481f53252eb3a299c181f351e063678511c`, [PR #54](https://github.com/LucianoToscano/PGBR/pull/54), sem merge. Regras de branches separadas, sem exclusão de funcionalidades e sem alteração da `main` aplicadas.

### PR #55 — trava de joystick

[PR PGBR #55](https://github.com/LucianoToscano/PGBR/pull/55), branch `feat/lt032-joystick-lock-pr10-20261122`, commit de integração inicial `98e1b554a9bb0ec302b0447fc801217f3435850a`, draft. Adaptado o `JoystickLock.java` do upstream com limiares `POP_DISTANCE=0.45`, `RESET_DISTANCE=0.35` e atraso `POP_DELAY_MS=250`; acrescentada a trava de movimento contínuo a `Pad.java`. **Diferença PGBR intencional:** os desenhos/efeitos dos cinco estilos, inclusive o Eternatus, não foram substituídos pelo joystick monocromático upstream. Desenhado cadeado em ambos os caminhos do renderizador; soltura, cancelamento, desanexação, mudança de conta/atividade e perda das permissões removem o estado travado. O loop `Control.stepMotion` reconhece `stick.isLocked` como comando manual para impedir que automações de caminhada assumam a direção. O modo relativo à câmera reobtém a orientação quando necessário.

**Testes incluídos:** `pgrazor/test/JoystickLockTest.java`, `test_joystick_lock_java.py`, `test_joystick_lock_integration.py`; CI de auditoria por PR. Android e experiência visual dos cinco estilos ainda precisam de homologação. Não significa APK publicado.

### PR #56 — encadeamento privado dos hooks ARM64 ART

[PR PGBR #56](https://github.com/LucianoToscano/PGBR/pull/56), branch `fix/lt032-art-hook-chain-pr11-20261122`, commit de integração inicial `d0c38b6bf3aa2f5a261ed2aeb9d11b4c406c6d7e`, draft. Port seletivo **exato** de `pgrazor/art/CMakeLists.txt` para realocar pelo menos 16 bytes do trampolim ARM64 da Dobby **privada do ART** antes do retorno; bloqueio CMake caso a assinatura do vendor pinado mude. Arquivo de origem do vendor e motor de hooks do gameplay permanecem intactos. Inclui `pgrazor/art/tests/hook_chain_test.cpp` com fixture opcional somente em build de diagnóstico ARM64, fora de APK e módulo, documentação Root e teste de fonte `test_art_hook_chain_source.py`.

**Validação upstream relatada:** OnePlus 15 Android 17, KernelSU/Zygisk Next/Vector 2.2 e Pokémon GO `0.437.0` ao iniciar. **Isso não é homologação PGBR.** Teste de compilação ART em ARM64, fixture em aparelho separado e instalação real do módulo Root ainda pendentes. Não declarar compatibilidade universal entre módulos.

### Estado da compilação PGBR e registros de falha

A execução [`44987764532`](https://github.com/LucianoToscano/PGBR/actions/runs/44987764532) de APK completo sem Root da base PR #54 **falhou** em `tools/check.py` antes da etapa de compilar o aplicativo, embora a preparação SDK/NDK/DEX e testes PvP tenham passado. O log mostra duas falhas específicas:
1. `test_control_java.py` compila `Control.java` sem stub ou inclusão da classe recém-acrescentada `SessionStats` (símbolo `SessionStats.publish` ausente no fixture).
2. `test_master_governance.py` exige que `.github/PULL_REQUEST_TEMPLATE.md` mencione `PGBR_ARQUIVO_MESTRE_UNICO.md`, mas ainda contém o template upstream original.

Esses problemas preexistem aos PRs #55/#56. Exigem correção de fixtures/documentação por branch própria antes da próxima tentativa de APK; falha na suíte **não prova falha de execução do módulo Root ou do joystick**. Nenhuma build do PR #55 ou PR #56 deve ser anunciada antes de CI e teste físico; a `main` e a LT032 anterior devem ficar preservadas.

**Validações seguintes, confirmadas:** o CI de auditoria do [PR #55, execução 44988965557](https://github.com/LucianoToscano/PGBR/actions/runs/44988965557) aprovou testes Java de limiares e invariantes visuais PGBR; o [PR #56, execução 44988970925](https://github.com/LucianoToscano/PGBR/actions/runs/44988970925) aprovou o teste estático da proteção ARM64 e a auditoria. Ainda não representam teste em Android/compilação Root. [PR PGBR #57](https://github.com/LucianoToscano/PGBR/pull/57), branch `fix/lt032-control-sessionstats-fixture-20261122`, corrigiu em fonte o stub `SessionStats` faltante de `test_control_java.py` e adicionou regressão estática. O modelo de PR da base consultada já faz referência ao Mestre, apesar da falha histórica desse template no log da compilação anterior. O PR #57 permanece em revisão e não foi fundido à LT032.

**Pendências:** conferir CI específico dos PRs #55/#56, corrigir fixture e template em manutenção isolada, compilar pacote completo PGBR e ART Root, testar joystick em 5 estilos e em modo câmera, iniciar Pokémon GO com/sem Vector, comparar comportamento, preservar artefatos de rollback.

---

## 22/11/2026 — Correções de CI, portabilidade e QA de integração (PRs #58–#60)

**Auditoria read-only recebida:** havia 16 classes de melhorias — ausência do gate real `tools/check.py` em PRs para `main`, ampla cobertura C++ fora do CI, geração PT-BR em automação sem validação efetiva, falhas de `TemporaryDirectory(dir=build)` e `LOCALAPPDATA`, colisão Windows de modelos de PR, inglês de acessibilidade/Toast, teste Frida Java bridge órfão, Actions flutuantes, manifest preview depurável, documentação desatualizada, preflight inadequado, Funding herdado, releases upstream desprotegidas, fonte LivePokeMap sem operador/autorização documentados e pequenos problemas DX/website.

### PR #58 — infraestrutura de verificação e hardening sem redução de funções

[PR PGBR #58](https://github.com/LucianoToscano/PGBR/pull/58) — branch `maintenance/quality-gates-crossplatform-20261122`, iniciada sobre `main` @ `072bd0b38a105612114057b2f946528582ae2d61`, draft. Inclui workflow `.github/workflows/pgbr-codigo-ci.yml` com testes de host C++ em `tools/native_host_tests.py`, compilação de fixtures com NDK 27.3 ARM64, `tools/check.py`, Java e validação do instalador Root em CI de PRs e pushes main. **7** testes de política C++ puros foram alistados (incluindo Auto-Raid de 296 asserts); demais fixtures ainda dependem de triagem e NDK/manual. Teste antes órfão `pgrazor/vendor/test_bridge_patch.cjs` agora é executado por `tools/check.py`; preflight explícito JDK 17/Node22/SDK plataforma+fontes. Corrigidos testes Python que exigiam `pgrazor/build` ignorado e `LOCALAPPDATA` Windows; skip correto quando SDK JSON não existe.

Corrigida colisão `.github/PULL_REQUEST_TEMPLATE.md`/minúscula **removendo somente a versão inglesa redundante**, preservando o template PT-BR do PGBR, com regressão sobre a **árvore Git** (e não `Path.exists()`, que é case-insensitive no Windows). Instrumentador PT-BR cobre `setContentDescription` sem ponto, `Toast.makeText` de string literal no segundo argumento e `setError`, com testes de idempotência; **não significa que todos os textos existentes já estão traduzidos**. Workflows principais com SHA pins, gerador PT-BR com teste de código antes de auto-commit, cópia de workflows upstream com restrição explícita ao repositório de origem.

Preview `pgrazor/AndroidManifest.xml` com `android:debuggable=false`, mantendo `versionCode=1` apenas para o preview não-distribuível; `docs/BUILDING.md` avisa que `pgrazor/VERSION=0.00.01-LT` é legado de desenvolvimento, não LT032. `.github/FUNDING.yml` identifica Ko-fi do upstream, `.gitignore` deixa de ignorar qualquer `*.txt` indiscriminadamente, fonte de website renomeada `upstreamSource`, `CHANGELOG.md` agora tem seção Unreleased; `docs/HUNDO_FEED_AUTORIZACAO.md` explicita que LivePokeMap/hundo-feed **não deve ser implantado sem permissão, contrato e responsável identificados**.

[CI primeira execução #44989885666](https://github.com/LucianoToscano/PGBR/actions/runs/44989885666): C++ host passou; Windows falhou em regexp `ptbr_wire.py` e teste case-insensitive de duplicação do template; ambos **corrigidos posteriormente no PR**. [CI com NDK #44990120865](https://github.com/LucianoToscano/PGBR/actions/runs/44990120865): NDK/host passaram; Windows cancelado ao atualizar branch. Não converter CI parcial em sucesso geral antes da execução corrente concluir.

### PRs #59 e #60 — integração incremental LT032 com reversão preservada

[PR #59](https://github.com/LucianoToscano/PGBR/pull/59) — branch `integracao/lt032-qa-consolidada-20261122`, commit `5c0f306f86cf65c938dc6183e7ba879fc571af96`. Aplica **somente** os códigos validados das PRs #55 (joystick), #56 (ART Root ARM64/Vector) e #57 (fixture SessionStats) sobre o PR #54, preservando toda personalização do PGBR, versões Root/sem Root, materiais, controles, VGP, PvP e resultados anteriores.

[PR #60](https://github.com/LucianoToscano/PGBR/pull/60) — branch `integracao/lt032-qa-quality-stage-20261122`, base PR #59, adiciona os ajustes de CI/portabilidade/segurança do PR #58 sem sobrescrever os códigos LT032 específicos; `test_control_java.py` retém stub `SessionStats` e recebe apenas correção do caminho SDK.

**Causa raiz de falha específica reproduzida:** [APK QA #44989966297](https://github.com/LucianoToscano/PGBR/actions/runs/44989966297) passou a fixture de escopo PvP e **250 dos 251 testes Python/Java**, mas falhou no teste de governança pela colisão de nomes na **cópia física Windows**. A correção do PR #60 remove a entrada minúscula no Git e retém o checklist PGBR, não desativa teste. Novo workflow de compilação foi explicitamente habilitado para branch #60. **Ainda não anunciar APK:** esperar CI completar, validar artefato, assinatura, hashes, Root e instalação física antes de subir release.

### Pendências que não podem ser apagadas apenas por CI

- **Runtime de jogo no aparelho:** PvP GBL/desafio com ágil, ambos carregados e escudos (priorização super eficaz depende de dados/energia/botão confirmados); VGP captura/giro, mapa/teleporte, treino Rocket, Quick Catch, Rotas/Mateo/Eternatus e privacidade em mapa/perfil.
- **Root:** abertura real com Vector/Zygisk e diferenças ARM64/Android em aparelho; fixture do PR #56 é teste de regressão independente, não prova essa compatibilidade.
- **Feeds de terceiros:** PGSharp/Pokémod/iPogo não oferecem neste projeto endpoints documentados/credenciais comprovadas. Não copiar APIs fechadas sem direito e autenticação; feeds autorizados requerem operador, consentimento, origem, TTL, limites e tratamento de erros.
- **Qualidade total:** 7 de 134 fixtures C++ puras agora entram nos gates host+NDK, a lista inteira exige classificação por dependências e execução adequada; PT-BR precisa revisar/gerar alterações concretas em Java e tradução de textos, além da instrumentação; testar sprites/itens, botões, acessibilidade, layout e as duas variantes em telefone.
- **Gestão de versões:** PRs #40–#60 permanecem em branches independentes até comparação de diff, tests, revisão, PR sem publicação direta na `main`. Artefatos antigos e hashes precisam de cópias reais e política de rollback; não inventar download.

### PR #61 — QA Root e sem Root na mesma assinatura

[PR #61](https://github.com/LucianoToscano/PGBR/pull/61) — branch `integracao/lt032-root-nonroot-qa-20261122`, base PR #60 @ `4c18fadb308791c327525890fd770c975c9e3069`, commit inicial `ab2af57e43da57f5a90f851fd21d835daf4480e0`. Amplia o workflow de QA da LT032 para **gerar e verificar** após a compilação não Root o instalador Root e módulo Zygisk no mesmo contexto e mesmo `PGRAZOR_VERSION=v0.00.32-LT`. Chama `pgrazor/rooted.py ... --skip-build --download --output dist/pgbr-lt032`, depois `tools/verify_release.py --tag v0.00.32-LT` que exige correspondência entre 3 pacotes, hashes SHA256, certificados alvos e recibos. Retém os 6 arquivos finais como um artefato de QA nomeado separadamente. Não publica release nem altera `main`. **CI iniciado:** [execução #44990744862](https://github.com/LucianoToscano/PGBR/actions/runs/44990744862). Resultado final e instalação física ainda não verificados ao escrever este trecho.

**Confirmação de qualidade de código:** [CI do PR #58 #44990523543](https://github.com/LucianoToscano/PGBR/actions/runs/44990523543) completou com **sucesso em quatro jobs**: Windows (`tools/check.py`, compilação Java e Root-installer check), C++ host, NDK ARM64 e website com `npm ci && npm run build`. Esse sucesso **não** comprova aprovação das branches QA #60/#61, pois o código de gameplay e a matriz de execução diferem.

---

## 22/11/2026 — APK LT032 QA verificado e textos PT-BR aplicados

[Execução LT032 #44990411941](https://github.com/LucianoToscano/PGBR/actions/runs/44990411941) **aprovada integralmente**: job `native-pvp-fixture` e `apk-test` concluídos com sucesso. O GitHub publicou o artefato `PGBR-LT032-PGR036-APK-completo-QA`, ID **14742195492**, com APK único, build-info e SHA256SUMS (retenção até 22/12/2026, sujeito a controles do GitHub). O arquivo foi baixado do artifact e validado por leitura de ZIP, hash de arquivo e conteúdo interno. **APK não Root:** nome interno `PGBR-v0.00.32-LT.apk`, 188.761.411 bytes, SHA-256 `ffe4f1e5946389fe3406a9df17a2352591faaae0461d1bef6ab82412abdcc1b0`. `build-info.json`: jogo `0.437.0`, versão code `2026110902`, ARM64, source commit `4c18fadb308791c327525890fd770c975c9e3069`, `device_tested=false`. `mode.json`: compilado nativo (sem Frida Gadget), módulos `compiledGameplay`, `compiledNativeTransport`, `compiledLocationGetters`, `compiledJavaBridges`. Essa verificação permite oferecer APK de **QA**, mas não afirmar abertura/execução no Android ou equivalência com pacote Root.

**PT-BR em código de tela:** [PR #62](https://github.com/LucianoToscano/PGBR/pull/62), branch `feat/lt032-ptbr-acessibilidade-20261122`, base PR #60; **16 arquivos Java de interface** atualizados para passar mensagens de `Toast.makeText`, `setError` e `setContentDescription` de string literal por `PtBr.tr`; preservados mecanismos, textos dinâmicos, IDs, chaves de conta e protobuf. Instrumentação foi seletiva para strings literais, não prova cobertura de todos os textos; teste de compilação Java/CI explícito acionado depois, resultado não assumido. Esta PR foi iniciada **depois** da geração do APK com hash acima; o APK validado **não** contém o código do PR #62.

**Build simultâneo Root/sem Root:** a execução [#44990744862](https://github.com/LucianoToscano/PGBR/actions/runs/44990744862) do PR #61 ainda estava em andamento no instante deste registro. Deve publicar duas APKs e um módulo ZIP verificados por `verify_release.py` apenas se concluir com sucesso. Não confundir com o APK único que já passou.

**Homologação de produto:** testes de instalação/upgrade/rollback físico, PvP automático em batalha real, VGP+ com giro/captura, feeds autorizados com localização em tempo real, privacidade de streaming em ambas as telas, joystick nos cinco visuais e Root+Vector ainda não realizados. Esses itens são explicitamente **pendências que só podem ser concluídas após evidência em aparelho e/ou credenciais/permissões legítimas**, apesar de código/CI verdes.

---

## 22/11/2026 — Artefatos Root/sem Root QA aprovados e integração PT-BR final

O [workflow #44990744862](https://github.com/LucianoToscano/PGBR/actions/runs/44990744862) do [PR #61](https://github.com/LucianoToscano/PGBR/pull/61) finalizou com **success** nos jobs PvP de host e APK Windows. O artefato **`PGBR-LT032-PGR036-Root-e-SemRoot-QA`**, id GitHub **14740934383**, foi gerado e baixado; o ZIP foi inspecionado e os hashes das **três** variantes conferidos de forma independente com SHA256SUMS.txt, `build-info.json` e `root-build-info.json`. Também foi verificado que o único módulo ZIP embutido no APK instalador Root tem SHA idêntico ao módulo distribuído e contém `zygisk/arm64-v8a.so` e `zygisk/x86_64.so`; correspondência de versão `0.437.0` e destino `com.nianticlabs.pokemongo`. Todos **device_tested=false**, sem instalação física.

| Artefato | Bytes | SHA-256 verificado |
| --- | ---: | --- |
| APK completo sem Root `PGBR-v0.00.32-LT.apk` | 188761411 | `f7d14f26a8d83315e696fed264715996db8780e45a789300d09bccb29eca8902` |
| Instalador Root `PGBR-v0.00.32-LT-Root.apk` | 8286926 | `a55098a140745a6d7ad57b67bb9fc6be378c55c4e6af29cff95ce0a9431bab4d` |
| Módulo Zygisk `PGBR-v0.00.32-LT-Root-module.zip` | 8204282 | `dfa4dcfa21e02fae192f0e4c6fe25589413a93fb77e699ab9c6abefbfa186404` |

A versão prévia do APK único, cujo SHA era `ffe4f1e...c1b0`, continua rastreável via #44990411941, mas **não é byte-a-byte a mesma assinatura/saída** da nova execução. Não misturar checksums entre os workflows.

O [PR #62](https://github.com/LucianoToscano/PGBR/pull/62) mudou 16 arquivos Java de textos e acessibilidade. O CI inicial [#44991114603](https://github.com/LucianoToscano/PGBR/actions/runs/44991114603) identificou **um erro de fixture Java isolado:** `test_one_shot_automation_java.py` não tinha stub `PtBr` depois que um Toast literal passou a ser traduzido. O fixture foi corrigido por uma classe inerte sem alterar `PtBr` do aplicativo, e o novo teste foi iniciado [#44991466213](https://github.com/LucianoToscano/PGBR/actions/runs/44991466213). Resultados ainda não consolidados.

[PR #63](https://github.com/LucianoToscano/PGBR/pull/63) — branch `integracao/lt032-root-ptbr-final-qa-20261122`, sobre PR #61 — reúne os 16 arquivos Java traduzidos e o fixture do PR #62 com todos os módulos Root e sem Root anteriormente testados. A execução [#44991558398](https://github.com/LucianoToscano/PGBR/actions/runs/44991558398) foi iniciada para compilar **novamente** todos os artefatos já com as traduções. **Ainda não confundir as APKs do #61 com resultados do #63** até inspecionar novos hashes e recibos.

**Não encerrar homologação de produto** sem instalação real, PvP GBL, escudos/ataques, VGP com capturas/giro, acessórios/sprites, joystick no jogo, privacidade streaming, comparação do Root+Vector e disponibilização autorizada de feeds de observações.

---

## 22/11/2026 — LT032 QA final com PT-BR aprovado e hashes conferidos

**Conclusão verificável de build (não de homologação no aparelho).** A execução [GitHub Actions #44991558398](https://github.com/LucianoToscano/PGBR/actions/runs/44991558398) do [PR #63](https://github.com/LucianoToscano/PGBR/pull/63) e commit de origem `987910c43e020ee03bff273d7d6f2e1a45b88e62` terminou `success` nos dois jobs: fixtures nativas PvP e `apk-test`. Os três pacotes LT032 com as alterações PT-BR do PR #62 foram **efetivamente** gerados. O artefato baixado foi `PGBR-LT032-PGR036-Root-e-SemRoot-QA` ID **14741698780**, contendo 3 binários e recibos. A execução independente do [PR #62, #44991466213](https://github.com/LucianoToscano/PGBR/actions/runs/44991466213) passou três jobs (Java/Windows, C++ host e NDK ARM64) após reparo do fixture `PtBr`.

Verificação adicional independente da interface do GitHub: ZIP íntegro (`ZipFile.testzip() == None`), leitura de `SHA256SUMS.txt`, `build-info.json`, `root-build-info.json`, versão `0.437.0`, código `2026110902`, arquitetura `arm64-v8a`, módulos compilados, integridade do ZIP embutido no instalador Root, e presença de `zygisk/arm64-v8a.so` + `zygisk/x86_64.so`. **Valores finais (não reutilizar hashes dos builds anteriores):**

| Pacote, compilação com PT-BR | Bytes exatos | SHA-256 |
| --- | ---: | --- |
| `PGBR-v0.00.32-LT.apk` não Root | 188761411 | `dfdc61998058f6767e39d2e6507ad78f55040b9330b56a6ed1a85c773e04ee9e` |
| `PGBR-v0.00.32-LT-Root.apk` instalador Root | 8286926 | `fdc219fb0148bd4c2d564ccb3aeabb7ab16afbd60c08cce18482edc3e09b242a` |
| `PGBR-v0.00.32-LT-Root-module.zip` módulo Zygisk | 8204368 | `8c723bb17a41571cb42861971288aa2ab7721174095e4012d5bdd9f9c90164f0` |

**Backup no Google Drive:** dentro da pasta existente `PGBR/04_Builds_e_Modulos_PGBR`, foram colocados e conferidos por metadados: [instalador Root](https://drive.google.com/file/d/18LmSk5x4sSdwTVoDTaR7uPyHKHFM3-a8/view), [módulo Zygisk](https://drive.google.com/file/d/1Rt6L0ZnEcq3Swa5VzeCXtSJ6d_3_b8-O/view) e [SHA256SUMS](https://drive.google.com/file/d/1C7nUTJgb2I4iix3L1OD6md-SXhv8uLEh/view). A cópia integral ZIP de ~194,6 MB não pôde ser inserida por limite de upload da integração Drive (100 MB); o APK não Root de 188,8 MB também não foi enviado ao Drive. Sua fonte preservada é o artifact GitHub #14741698780 e o arquivo baixado nesta conversa. **Não confundir ausência de cópia no Drive com ausência do APK.**

**Status de publicação:** compilação, hashes e correspondência das três variantes **aprovados**; nenhum instalador foi fisicamente executado nesta sessão; `device_tested=false` consta no recibo. Preservar a versão instalada atual e rollback. Não marcar como corrigidos sem observação: inicialização e convivência Root+Vector, automação de PvP (ataque carregado/ágil/escudo, leitura de eficácia, estado/energia), VGP/auto spin e auto catch, buscas e feeds de spawns em tempo real com licença confirmada, ícones e sprites de itens, menu, joystick no aparelho, ocultação visual do nome do treinador no mapa/perfil e compatibilidade de assinatura/pacote clone. **Não promover os PRs draft #58–#63 para `main` sem revisão de diffs e validação Android.**

**Governança:** o progresso em arquivos, CI e commits está concluído para esta etapa. As ações de dispositivo e feeds externos continuam registradas como validações de produto necessárias, não como sucesso presumido.

---

## 22/11/2026 — Glossário PT-BR complementado e auditoria dos ícones empacotados

[PR #64](https://github.com/LucianoToscano/PGBR/pull/64) — branch isolada `fix/lt032-ptbr-textos-interface-20261122`, base PR #63 @ `987910c43e020ee03bff273d7d6f2e1a45b88e62`, mudanças concentradas em glossário PT-BR + `PtBr.java` + `RootPtBr.java` + teste do glossário + disparadores de CI. **14 entradas de interface adicionadas, total 1.383**, para mensagens como `Catch it`, `Dismiss`, coordenadas, prévia de controles, exportação de logs, reides automáticas, ave de Galar e `Max`. A [execução de código #44999399043](https://github.com/LucianoToscano/PGBR/actions/runs/44999399043) passou nos três jobs (Windows Java, C++ host e NDK ARM64). O [workflow de compilação #44999399025](https://github.com/LucianoToscano/PGBR/actions/runs/44999399025) foi acionado, mas não tinha conclusão verificada neste registro. APKs anteriores **não** contêm essas 14 traduções, não renomear como se contivessem.

**Auditoria estática de assets no APK PR #63:** 3.406 entradas ZIP, 1.126 assets PGBR, 1.114 imagens catalogadas; 1.025 ícones de feed, 51 ícones PNG/WebP de mapa, 37 ícones de itens, 1 imagem branding. Duplas intencionais entre `map-icons/berry-<id>.png` e `item-icons/item-<id>.png` para IDs 701/703/705/706/707 representam compatibilidade e não têm de ser apagadas automaticamente.

**Achado verificável de integridade:** `assets/pgrazor/map-icons/vgp-device-off.png` tem estrutura PNG truncada: chunk IDAT declara 694 bytes, mas o arquivo inteiro tem 717 bytes; PIL registra `Truncated File Read`. Os outros 1.113 PNG/WebP auditados puderam ser decodificados. O código `Pad.VgpToggle` usa prioritariamente o atlas original íntegro `vgp-hd-states.webp` (192×576, três estados), então o arquivo defeituoso é fallback e **não** demonstra que a VGP em jogo esteja quebrada. Necessário reparar esse fallback **a partir do atlas original**, sem criar desenho diferente nem alterar a chave dos assets; a versão de QA acima ainda inclui o fallback truncado. Não marcar recursos visuais como 100% homologados até corrigir e rever em aparelho.

**Limitação temporal e operacional:** execução de código e pacote instalável por CI não comprovam PvP, VGP, scanner ao vivo, Root+Vector e imagens em jogo. Estas tarefas requerem prova real ou direitos de acesso a feeds externos, conforme protocolo global.

---

## 22/11/2026 — Reparação fiel do PNG VGP OFF e proteção do CI (PR #65)

**Confronto com o código-fonte atual do PGBR:** a imagem `pgrazor/assets/map-icons/vgp-device-off.png` original de **717 bytes** estava truncada e não podia ser considerada válida como fallback. O renderizador em `Pad.VgpToggle` usa primeiro o atlas `vgp-hd-states.webp` (192×576; OFF/ERROR/ON); quando não disponível, lê a imagem PNG. A presença do fallback quebrado não provava que o VGP estava inoperante, mas representava falha de empacotamento corrigível.

[PR #65](https://github.com/LucianoToscano/PGBR/pull/65) — branch `fix/lt032-vgp-fallback-icon-20261122`, derivada do [PR #64](https://github.com/LucianoToscano/PGBR/pull/64) com as 14 novas traduções. O workflow exclusivo [#45000246867](https://github.com/LucianoToscano/PGBR/actions/runs/45000246867) foi **aprovado**: converteu **exatamente a primeira célula OFF** do atlas original em PNG de 192×192 RGBA, comparou pixel a pixel com o atlas, testou a estrutura PNG, CRC, IDAT/zlib e IEND de todos os ícones PNG do mapa. Commit binário `646876a3c06f87173c40604c88f444a0f3850575`, blob `4c813234c45862579736f77718f145dafff23868`. **Não foi gerada arte nova nem substituído o atlas original.**

Após o reparo, o workflow foi tornado **somente leitura** (`permissions.contents: read`, `persist-credentials: false`) pelo commit `954e4c0c53e32cf853b261cdceb5da83f94a8bf9`, para não conservar uma automação de escrita em PR. A [verificação read-only #45000567430](https://github.com/LucianoToscano/PGBR/actions/runs/45000567430) passou. Testes de PNG agora também participam de `python -B tools/check.py` e da compilação de QA.

**Upstream conferido:** PGRazor continua na release **v0.3.6 de 21/11/2026**, com PR #10 (joystick) e PR #11 (hooks ART) ainda abertos, ambos já portados em PRs independentes no PGBR. Nenhum PR upstream posterior a #11 foi encontrado na listagem publicada nesta verificação.

**Compilação:** [PR #64 / run #44999399025](https://github.com/LucianoToscano/PGBR/actions/runs/44999399025) concluiu **success** nos dois jobs, produzindo os pacotes com 14 traduções, **sem** o reparo do fallback. A compilação nova, incluindo traduções + PNG reparado, foi iniciada em [#45000567415](https://github.com/LucianoToscano/PGBR/actions/runs/45000567415), e a suite de CI completa em [#45000567442](https://github.com/LucianoToscano/PGBR/actions/runs/45000567442). As conclusões e os novos hashes dessas duas execuções **não** estavam confirmados no instante deste registro. Nunca reutilizar os hashes da execução #44991558398 ou #44999399025 para artefatos dessa branch.

**Pendências externas não removíveis por código:** instalar e reproduzir VGP+, PvP em batalha real, bloquear/desbloquear joystick em cinco estilos no aparelho, Root + Vector e verificar feeds de localização com credenciais/autorizações legítimas. Sem log Android, conta de QA e comprovação de autorização, marcar como não homologado; não anunciar como teste físico passado.

---

## 22/11/2026 — Regressões C++ host ampliadas para 12 suítes

Para enfrentar a cobertura insuficiente dos 134 fixtures nativos, a branch de manutenção do [PR #58](https://github.com/LucianoToscano/PGBR/pull/58) recebeu o commit `ba49e7a1699e700291caf6c5289b2f5136e0614e`, que ampliou de 7 para **12 testes C++ executáveis em host Linux**, mantendo a execução NDK ARM64 como gate separado. Foram incluídos:
- `battle_counter_test.cpp` e `smart_battle_party_test.cpp`: escolha de contragolpes e equipe antes da batalha;
- `pvp_rank_test.cpp`: classificação e limites de ligas PvP;
- `rocket_combat_test.cpp` ligado a `rocket_combat.cpp`/`rocket_actions.cpp` com `PGR_RC_DIRECT_TEST`: **27 cenários** de ataques rápidos/carregados, escudos, turno, reentrância e liberação de objetos;
- `battles_test.cpp` ligado a `battles.cpp` com `PGR_BATTLE_DIRECT_TEST`: reides/ginásios/Max, troca, desvio, recuperação de estado e orbes Max.

O [job Linux da execução #45000885337](https://github.com/LucianoToscano/PGBR/actions/runs/45000885337) passou os 12 testes; os logs registraram explicitamente **296/296 checks Auto-Raid, 53/53 verificações Shiny e 27 casos Rocket**, além dos outros testes de cenário. O website e a compilação de políticas NDK da mesma execução também passaram, mas o gate Java/Windows ainda estava em andamento no momento deste registro. **Não afirmar 134/134 executados**: os 122 fixtures restantes requerem avaliação de dependências e uma matriz de testes adicional. Nenhum desses resultados comprova ataque ou escudo funcionando no jogo real sem Android.

---

## 22/11/2026 — Matriz de aceite de dispositivo e cobertura CI

A matriz de homologação funcional foi registrada em [`docs/QA_LT032_ANDROID_FINAL.md`](docs/QA_LT032_ANDROID_FINAL.md), commit `0e771ede5e725d21ad458c16521c6917e868ff10`. Inclui critérios mensuráveis de instalação/rollback, correlação SHA-256, VGP OFF/ON/ERRO, auto catch/spin, joystick nos cinco estilos e trava 250ms, deslocamento relativo à câmera, PvP ataques/escudos e supereficácia, Rocket/Ginásio/Reide/Max, privacidade visual `*****`, consultas autorizadas, stats, Mateo, Rotas e transferências irreversíveis. **Não é uma declaração de testes em telefone**: campos PASS/FAIL foram deixados em branco para execução real pelo operador.

A ampliação do gate C++ no PR #58 passou 12 fixtures host Linux [job #45000885337](https://github.com/LucianoToscano/PGBR/actions/runs/45000885337), incluindo 27 cenários Rocket e 296 checks Auto-Raid. O build de website e o NDK ARM64 desse mesmo run também passaram; conferir status global de Windows antes de declarar esse run integralmente aprovado.

Requisitos de conclusão inequívoca: o GitHub aprovar o build final de código e de três variantes, os hashes coincidirem com os recibos, o PNG VGP íntegro estar presente **no APK**, e a matriz em aparelho real ter resultados registrados. Fechar o projeto com “sem pendências” enquanto essas evidências estiverem ausentes seria incorreto.
