# PGBR — ARQUIVO MESTRE ÚNICO
> Identificador: PGBR-MASTER-001  
> **Fonte oficial da verdade documental:** [`main/PGBR_ARQUIVO_MESTRE_UNICO.md`](https://github.com/LucianoToscano/PGBR/blob/main/PGBR_ARQUIVO_MESTRE_UNICO.md)  
> Criado em: **21/11/2026** · Última auditoria factual deste registro: **21/11/2026**  
> Estado: **documento vivo e obrigatório; não é um selo de homologação do aplicativo.**

## 0. REGRA MÁXIMA — LEITURA OBRIGATÓRIA
**ANTES de analisar, alterar, compilar, corrigir, integrar, criar APK, responder sobre status ou publicar qualquer versão do PGBR:**
1. **Ler este Arquivo Mestre integralmente**, depois `AGENTS.md` e os documentos de arquitetura, build, Root e release relevantes.
2. Conferir no GitHub o estado real da branch, HEAD, Actions, artifacts, releases e PRs: **este documento registra um snapshot que pode envelhecer**. Data de um commit não equivale ao número da versão.
3. Consultar código, evidências de execução e backups antes de afirmar que um recurso funciona; não confundir existência de classe, teste JVM, compilação, instalação, teste em dispositivo e publicação.
4. Nunca modificar o PokeSearch como efeito colateral; nunca apagar versões, fontes, imagens, dados, logs e backups sem inventário, comparação e confirmação de segurança.
5. Ao concluir **mudança material validada**, atualizar este mesmo arquivo com referência de branch/commit, resultados, pendências e data. **Não criar outros documentos chamados “Arquivo Mestre PGBR”** nem manter status concorrentes.
6. Se houver conflito entre este resumo e o código/CI atual, reportar a divergência, verificar evidências e corrigir este arquivo. **Nunca adaptar evidências a uma afirmação antiga.**

Este protocolo é espelhado nas instruções `AGENTS.md`, `.github/copilot-instructions.md` e no teste `pgrazor/test/test_master_governance.py`. Estes pontos de entrada orientam agentes e CI que os consultam; não garantem leitura por qualquer ferramenta externa ou novo chat sem acesso ao repositório.

## 1. Identidade, limites e missão do produto
- Nome: **PGBR** — edição brasileira independente para Android, derivada do código aberto [PGRazor](https://github.com/jpitty03/PGRazor) com atribuição MIT e avisos de terceiros preservados.
- Repositório central: [LucianoToscano/PGBR](https://github.com/LucianoToscano/PGBR).
- Objetivo: **um aplicativo central, completo e em PT-BR**, preservando joystick, mapa/teleporte, caminhada/rotas, controles de captura, gestão de inventário, automações opcionais, VGP+, buscas, eventos e configurações. **Não empobrecer, não transformar em preview com funções ausentes.**
- Produzir variantes do **mesmo projeto**: APK completo sem Root, instalador Root e módulo Zygisk quando disponíveis e verificados. Não separar recursos do aplicativo principal indevidamente.
- **PokeSearch é projeto separado:** não alterar código, repositório, assinatura, APIs, releases ou dados do PokeSearch sem tarefa independente e autorização explícita. Integração eventual exige limites e contratos claros.
- Marca visível PGBR; conservar identificadores internos necessários à compatibilidade (por exemplo, `dev.pgrazor`, IDs de módulo, propriedades e caminhos), a menos que exista migração comprovada.
- Português brasileiro como idioma prioritário; preservar funcionalidade antes de decisões estéticas.
- Projeto experimental independente; não se apresentar como afiliado à Niantic, Pokémon Company, PGSharp, Pokémod, iPogo ou outros fornecedores. Créditos e licenças são obrigatórios.

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

## 4. Branches e política de integração
- `main`: documentação mestre canônica, código histórico; **não forçar reset, merge ou release sem revisar**.
- `integracao/lt032-recuperacao-20261121`: núcleo consolidado LT032 + separação de visibilidade/início VGP; PR #40.
- `integracao/lt032-root-qa-20261121`: acrescenta workflow para três pacotes LT032; PR #41.
- `integracao/lt032-diagnosticos-recuperados-20261121`: resgata relatório VGP read-only e testes; PR #42.
- `fix/lt031-pvp-vnext-executor-20261121`: histórico principal dos experimentos PvP LT031, preservado.
- `feat/lt027-pokemon-selector-20261121`: histórico do Selector Pokémon e consertos de CI, preservado.
- Branches antigas são **fontes de recuperação e comparação**, não automaticamente versões mais recentes ou seguras. Auditar arquivo a arquivo: pode haver commits exclusivos e recursos não promovidos.

**Diretriz:** desenvolvimento em branch/PR próprio; revisar diffs e conflitos, manter código/artefatos antigos, executar testes e empacotar antes de considerar merge. Não publicar release automaticamente só porque o workflow deu verde.

## 5. Arquitetura e requisitos de compilação
Fontes complementares obrigatórias: [BUILDING](docs/BUILDING.md), [ARCHITECTURE](docs/ARCHITECTURE.md), [ROOTED](docs/ROOTED.md), [GAME_UPDATES](docs/GAME_UPDATES.md), [THIRD_PARTY](THIRD_PARTY.md).
- `pgrazor/src/dev/pgrazor/Control.java`: regras, estado e bridge pública estável. `SettingsPanel.java` e `Pad.java`: UI/overlays; preservar APIs, keys persistidas, lifecycle e posições.
- `pgrazor/native/`: núcleo compilado C++, hosts de combate, VGP, localização e ações; contratos e metadata são específicos de versão. As pontes Java/ART e transporte tipado são componentes distintos do motor de jogo.
- `LiveFeedClient.java`, `LiveFeedData.java`, `LiveFeedFilters.java`, `PokemonSelectorDialog.java` e conectores autorizados: fontes, expiração de spawns, filtros e confirmação de teleporte.
- `pgrazor/nonroot.py` e `pgrazor/release_apk.py`: variante sem Root; `pgrazor/rooted.py`: módulo e instalador Root.
- Entrada fixada nas versões auditadas: Pokémon GO **0.437.0 ARM64**, version code **2026110902**; SHA-256 do original fixado em BUILDING: `5be1ff372d8a3aa1c9ce9f4014b3825119086feaf71aae6b0538636acafebdd3`. **Não armazenar APKM original ou segredo no Git.**
- Compilação/release via GitHub Actions e Secrets existentes; não expor keystore, tokens, credenciais, URLs assinadas, dados de sessão nem payloads de terceiros. Verificar NDK/SDK/toolchain fixados antes de trocar a versão do jogo.
- Não remover suporte Root ou sem Root. Testar assinaturas, ABI, upgrades, `build-info.json`, `root-build-info.json`, `SHA256SUMS.txt` e hash dos binários, sem presumir compatibilidade com versões novas do jogo.

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

## 7. Requisitos imutáveis de UX e recursos
- Manter todos os recursos existentes: **enriquecer, nunca empobrecer**. Não sacrificar automações, autorização, opções ou menu ao redesenhar a UI.
- Botão e joystick **Eternatus** como preferência de apresentação inicial; efeito visual opcional, evitando poluição. Não alterar identidade de jogo nem cobrir áreas críticas sem opção do usuário.
- VGP: **dois ícones realmente distintos**, transparência e tamanho configuráveis, opção de imagem própria, arrasto até bordas e quatro cantos, sem duplicação visual e com proteção de início manual.
- Ícones de itens: usar sprite correto para cada ID verificado; nunca desenhar poção, reviver, ovo ou item falsamente para ID desconhecido. Preservar nome e fonte da biblioteca.
- Visual: ícones ilustrados mantidos e fundos de botões verdes convertidos para branco/neutro quando fizer parte de revisão testada; não fazer regressão de legibilidade.
- Interface em PT-BR natural, agrupamento claro de recursos, estados de execução e erro visíveis. Testar em Android real, escalas de fontes, orientações e sobreposição nas bordas.
- O app deve funcionar sem computador local para receber uma versão: CI prepara artefatos completos, **mas somente depois de verificar**.

## 8. Privacidade, segurança e integração de terceiros
- Proteção de streamer: **somente apresentação local**. Ocultar visualmente o nome do próprio treinador como `*****` onde verificado; restaurar ao desligar. Não modificar ID, conta, login, `PlayerService.get_PlayerName`, dados de servidor, pacotes, RPC nem nomes de outros jogadores.
- Se não houver metadata/hook de tela validado, dizer **“não homologado para lives”**.
- Serviços PGSharp, Pokémod, iPogo, PGTools, PokeList e similares: usar apenas APIs autorizadas/documentadas e arquivos sob direito de uso. **Compra de APK ou existência de descompilação não provam acesso legal/técnico ao feed privado nem fornecem credenciais.** Não copiar cliente fechado ou violar autenticação, nem usar bypass de rate-limit/anti-bot.
- Preservar créditos/licenças de software e direitos de imagens, fontes e assets.
- Não anexar logs brutos públicos com nick, e-mail, tokens, latitude/longitude privada, encounter ID ou credenciais. Registrar estado técnico mínimo sanitizado.

## 9. Backups, retenção e recuperação
- Bibliotecas pessoais e conversas têm APKs ZIP LT027/LT031 recuperados, na Biblioteca ChatGPT: `/PGBR/Backups-2026-11-21/`. A existência na Biblioteca **não prova cópia no Google Drive**.
- Registro do backup: Google Drive `Projetos/PokeGoBR/03_Relatorios/PGBR_BACKUP_VERIFICADO_2026-11-21.md`; é manifesto, **não backup de 180 MB**.
- Artefatos LT032 disponíveis em GitHub Actions (IDs e expiração informados na seção 3); baixar e preservar integralmente antes do vencimento. Retenção pode ser alterada; reconfirmar.
- Releases anteriores não devem ser deletadas, nem ZIP original de entrada ou backup sobrescrito. Manter opção de retorno previamente instalada e chave de assinatura correspondente.
- O arquivo antigo `POKEGOBR_ARQUIVO_MESTRE_COMPLETO_v1.0_2026-11-01.zip` em `Projetos/PokeGoBR/00_Arquivo_Mestre` pertence ao contexto histórico **PokeGoBR**, não substitui este mestre do PGBR nem deve ser misturado automaticamente.

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

## 12. Índice de evidências e documentos
- [GitHub Actions](https://github.com/LucianoToscano/PGBR/actions), [Releases](https://github.com/LucianoToscano/PGBR/releases), [Pull requests](https://github.com/LucianoToscano/PGBR/pulls).
- [LT032 — release notes de desenvolvimento](https://github.com/LucianoToscano/PGBR/blob/integracao/lt032-recuperacao-20261121/docs/releases/v0.00.32-LT.md).
- [LT032 — checklist Android real](https://github.com/LucianoToscano/PGBR/blob/integracao/lt032-recuperacao-20261121/docs/validation/PGBR_LT032_CHECKLIST_DISPOSITIVO.md).
- [LT032 — resgate de branches](https://github.com/LucianoToscano/PGBR/blob/integracao/lt032-diagnosticos-recuperados-20261121/docs/validation/PGBR_LT032_RESCUE_BRANCHES.md).
- [Diagnóstico PvP](https://github.com/LucianoToscano/PGBR/blob/integracao/lt032-recuperacao-20261121/docs/PVP_BATTLE_ASSISTANT_LT028.md).
- [Privacidade do streamer](https://github.com/LucianoToscano/PGBR/blob/integracao/lt032-recuperacao-20261121/docs/PRIVACIDADE_STREAMER_MOD_APURACAO.md).
- [Árvore histórica do PGBR](https://github.com/LucianoToscano/PGBR/tree/main).
- [Biblioteca Pokémon GO no Drive](https://drive.google.com/drive/folders/1TPgVIGAWkY2Ez1VS3bHAitv9Y_NpczbK) — patrimônio de pesquisa e sprites; **não é prova de licença nem código-fonte oficial**.

## 13. Registro de atualizações deste mestre
| Data | Evento | Estado |
| --- | --- | --- |
| 21/11/2026 | Criado um **Arquivo Mestre Único** e definida hierarquia de evidências, estado LT032, backups, riscos e prioridades. | Fonte canônica no `main`; outras branches podem conter espelhos idênticos necessários à leitura local, sempre subordinados à versão da `main`. |
| 21/11/2026 | Verificado sucesso de CI LT032 sem Root, Root+Zygisk e teste isolado VGP. | **CI verde; dispositivo não homologado; PRs #40/#41/#42 ainda drafts.** |

| 21/11/2026 | Inventário conservador de `main` e três variantes LT032; proposta de higiene documental no [PR #43](https://github.com/LucianoToscano/PGBR/pull/43), sem excluir arquivos nem promover código de LT032. | **1.966 arquivos na `main`; 2.176 no candidato LT032; 7 grupos idênticos em LT032 preservados. Testes/CI do PR e merge ainda não comprovados.** |

**Regra de atualização:** acrescente uma linha com data, novo estado e evidência para cada conclusão relevante. Preserve histórico em Git; nunca apague status antigo sem explicar a substituição.

## 14. Auditoria conservadora e organização — proposta PR #43 (21/11/2026)

**Referência detalhada (não é mestre concorrente):** [docs/maintenance/PGBR_AUDITORIA_CONSERVADORA_20261121.md](docs/maintenance/PGBR_AUDITORIA_CONSERVADORA_20261121.md). Este arquivo registra evidência e só passa a ser a referência canônica quando o PR correspondente for revisado e integrado à `main`.

- **Inventário GitHub completo (árvore não truncada):** `main` no commit `284e5a583e6de9fcb256ae5b213bb4b352a09819` possui **1.966 arquivos rastreados**, com 34.782.810 bytes de blobs. O candidato LT032 no commit `15252c491d0c823ceb924516ae950abd685e931a` possui **2.176 arquivos**, com 38.012.286 bytes. Diferença de caminhos: 210 adições, 123 modificações e **nenhuma remoção** do inventário da `main`.
- **Outras árvores examinadas:** PR #41, commit `fb42bbf37783cd458772dd9bf3896ee020d4854c` (2.177 arquivos); PR #42, commit `a24aa693aefb858ff0f350e1e13257bc5b01de22` (2.181 arquivos). As branches não foram fundidas entre si nem promovidas à `main`.
- **Duplicidades byte a byte:** um grupo em `main` (logo SVG) e sete grupos no LT032 (cópias de logos e cinco sprites de bagas para pontos de consumo diferentes). **Preservados integralmente**: mesma hash não prova que links, pacotes Android ou caminhos sejam intercambiáveis.
- **Higiene não destrutiva proposta:** corrigir a referência quebrada do arquivo de recuperação em `PGBR_IMPORTACAO.md`, manter `AGENTS.md` rastreável removendo sua entrada do `.gitignore`, acrescentar apenas ignores de resíduos gerados, tornar o README mais preciso sobre LT001/LT023/LT032 e fornecer links diretos dos assets oficiais LT023 já confirmados.
- **Controle repetível proposto:** `tools/repo_inventory.py` realiza **somente leitura** do checkout (`git ls-files`, SHA-256, classificação e alertas); `pgrazor/test/test_repo_inventory.py` exercita preservação de arquivos. O script não apaga, move nem reescreve nada. A execução real em CI ainda precisa ser verificada.
- **Situação da mudança:** [PR #43](https://github.com/LucianoToscano/PGBR/pull/43) **rascunho** para revisão. Não foi criada release/APK nesta manutenção. Compilação, instalação física, VGP, PvP e homologação da LT032 permanecem com o estado descrito nas seções anteriores, sem nova alegação de funcionamento.

**Critério permanente de limpeza:** primeiro consultar este mestre, depois inventariar, provar uso/ausência de uso por referências, comparar hashes, preservar licenças e rollback, e testar Root/sem Root. Somente excluir em mudança específica revisada, com preservação verificada. **Tamanho, nome antigo ou conteúdo duplicado não autorizam exclusão.**
