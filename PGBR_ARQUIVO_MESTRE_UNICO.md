# PGBR — ARQUIVO MESTRE ÚNICO
> Identificador: PGBR-MASTER-001  
> **Fonte oficial da verdade documental:** [`main/PGBR_ARQUIVO_MESTRE_UNICO.md`](https://github.com/LucianoToscano/PGBR/blob/main/PGBR_ARQUIVO_MESTRE_UNICO.md)  
> Criado em: **21/11/2026** · Última auditoria factual deste registro: **22/11/2026**  
> Estado: **documento vivo e obrigatório; não é um selo de homologação do aplicativo.**

## Como navegar pelo Arquivo Mestre

> **A consulta começa pela leitura integral, não apenas por este índice.** Esta página é a referência documental única; snapshots não substituem verificações atuais no GitHub.

- **Governança:** [leitura obrigatória](#mestre-leitura), [identidade e escopo](#mestre-escopo), [níveis de evidência](#mestre-evidencias).
- **Produto:** [versões e estado](#mestre-versoes), [branches](#mestre-branches), [arquitetura/build](#mestre-arquitetura), [registro de recursos](#mestre-recursos), [UX e requisitos](#mestre-ux).
- **Operação:** [privacidade/terceiros](#mestre-seguranca), [backups](#mestre-backups), [prioridades](#mestre-prioridades), [protocolo de execução](#mestre-protocolo), [fontes](#mestre-fontes).
- **Auditoria:** [histórico de alterações](#mestre-historico), [inventário conservador](#mestre-auditoria), [mapa de armazenamento e limpeza](#mestre-arquivos).

> **Atualização cronológica posterior:** a pré-release **LT035 (v0.00.35-LT)** foi publicada após LT034 e LT033; ver recibo de LT035 no fim do documento. O resumo inicial abaixo é um snapshot histórico, não a release mais recente.

**Estado resumido auditado em 22/11/2026:** a `main` já incorporou a linhagem LT032, os recursos de Eventos e a versão **v0.00.33-LT** pelo [merge PR #68](https://github.com/LucianoToscano/PGBR/pull/68) no commit `28a365fec1277033a1089cb394aecd6e9cc01115`. A [pré-release LT033](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.33-LT) foi publicada com os três pacotes assinados e hashes verificados; **ainda não houve homologação no Android físico**. LT023 e os pacotes LT032 permanecem preservados; LT033 é a publicação mais recente, marcada **QA**. A manutenção [PR #43](https://github.com/LucianoToscano/PGBR/pull/43) preservou o inventário histórico.

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

## REGRA PERMANENTE DO USUÁRIO — REGISTRAR CADA ETAPA NO ARQUIVO MESTRE

**Regra vinculante (22/11/2026):** todo recurso, correção, teste, atualização upstream, mudança de interface, merge, PR, build, upload e release do PGBR deve ser documentado neste **único** arquivo, na revisão correspondente. Registrar estado antes/depois, branch e commit-base/commit final, arquivos, testes com resultados reais, URLs comprovados, hashes e pendências. No momento do merge, reconferir a HEAD da `main`. **Depois da publicação**, registrar tag, commit de release e três hashes reais, com link direto ao APK que exista. Nunca declarar sucesso antes do CI, nunca inventar número de versão/artefato e nunca retirar o histórico em nome de limpeza.

O release `v0.00.33-LT` é uma **nova identificação de build**, não uma mudança cosmética no nome dos APKs `v0.00.32-LT`. A versão 0.437.0 e seu versionCode são do Pokémon GO-base; o número `0.00.33-LT` identifica o PGBR e o installer Root. Publicar primeiramente como **pré-release QA**, com `device_tested=false`, sem anunciar homologação de PvP, VGP, Root+Vector, feed ou privacidade.

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
| Release pública mais recente (pré-release de QA) | **`v0.00.33-LT`**; três pacotes assinados e checksum publicado; **sem teste físico homologado** | [Pré-release LT033](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.33-LT) |
| Última versão anterior preservada | **`v0.00.23-LT`**, release histórica, e pacotes QA LT032 preservados separadamente | [Release LT023](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.23-LT) |
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

A ampliação do gate C++ no PR #58 passou 12 fixtures host Linux [job #45000885337](https://github.com/LucianoToscano/PGBR/actions/runs/45000885337), incluindo 27 cenários Rocket e 296 checks Auto-Raid. O build de website, o NDK ARM64 e a suíte Windows/Java/Python/Node/Root desse mesmo run também terminaram **success**: o CI completo [#45000885337](https://github.com/LucianoToscano/PGBR/actions/runs/45000885337) foi aprovado nos **quatro jobs**, inclusive os 12 testes C++ host.

Requisitos de conclusão inequívoca: o GitHub aprovar o build final de código e de três variantes, os hashes coincidirem com os recibos, o PNG VGP íntegro estar presente **no APK**, e a matriz em aparelho real ter resultados registrados. Fechar o projeto com “sem pendências” enquanto essas evidências estiverem ausentes seria incorreto.

---

## 22/11/2026 — QA final PR #65: APK com VGP corrigido e backup verificado

**Código-fonte, CI e APK concluídos para esta revisão, não homologação no jogo.** [PR #65](https://github.com/LucianoToscano/PGBR/pull/65), código base PGBR LT032 + PR64 glossário PT-BR + fallback VGP do atlas original, commit de build `954e4c0c53e32cf853b261cdceb5da83f94a8bf9`. O [workflow completo #45000567415](https://github.com/LucianoToscano/PGBR/actions/runs/45000567415) passou nos dois jobs, incluindo fixtures PvP e compilação/assinatura/empacotamento Root e sem Root. [CI completo #45000567442](https://github.com/LucianoToscano/PGBR/actions/runs/45000567442) terminou `success` em Linux C++/NDK ARM64 e Windows Java/Python/Node/Root; a suíte Python/Java registrou **261 testes, OK (skipped=1)**. [Verificação pixel-exata #45000567430](https://github.com/LucianoToscano/PGBR/actions/runs/45000567430) também passou com `contents: read`. C++ de manutenção ampliado em PR58 [#45000885337](https://github.com/LucianoToscano/PGBR/actions/runs/45000885337): quatro jobs aprovados e **12 suítes host**. O PR65 não efetuou merge em `main`, respeitando integração paralela sem perdas.

**Artefato Github:** id **14745906741**, nome `PGBR-LT032-PGR036-Root-e-SemRoot-QA`, download ZIP da execução #45000567415. ZIP extraído e conferido independentemente por CRC, SHA256SUMS, JSON de build e de Root, versão 0.437.0 código 2026110902, ABIs, módulo embarcado no instalador e modos compilados.

| Pacote, build com PT-BR e VGP OFF corrigido | Bytes exatos | SHA-256 confirmado |
| --- | ---: | --- |
| `PGBR-v0.00.32-LT.apk` — jogo não Root | 188810563 | `bad3febe5988f627633704be31099fdba430f37e1e823b5d390a4149fa1d4e5a` |
| `PGBR-v0.00.32-LT-Root.apk` — instalador Root | 8336078 | `286cbbc3d04d1ee8c7cb167e94947885787f2779405862ec2d3ae3df9bf05cd1` |
| `PGBR-v0.00.32-LT-Root-module.zip` — módulo Zygisk | 8251277 | `b4887fb7fba58e251aeaf5589a649080f1574c98d35b24f74c1581ceba600093` |

**Novo teste direto do PNG dentro do APK:** `assets/pgrazor/map-icons/vgp-device-off.png` tem 46937 bytes e é PNG RGBA **192×192**, decodifica integralmente, com todos os pixels idênticos à célula OFF da imagem original `vgp-hd-states.webp` 192×576. O ZIP não Root possui modo `compiledGameplay=true` e o instalador Root embute um módulo com o mesmo SHA da distribuição externa. O bloco de assinatura Android foi detectado nos dois APKs, sem afirmar auditoria independente criptográfica de certificado. O recibo de CI diz **`device_tested=false`**.

**Backup Google Drive efetivamente confirmado**, na pasta `PGBR/04_Builds_e_Modulos_PGBR`, preservando os backups antigos:
- [Instalador Root 8.336.078 bytes](https://drive.google.com/file/d/1DFcqUyYxZQwELJjG78d4ibOiTwXZnu1w/view)
- [Módulo Zygisk 8.251.277 bytes](https://drive.google.com/file/d/1C_oFdwWxEsyz1bELOoSnSceghYn8fJE4/view)
- [SHA256SUMS da mesma execução](https://drive.google.com/file/d/13EDOPrBH9QXx6wc4NFaQlCXEOSOxRwHr/view)

O APK não Root completo de 188,8 MB **não** entrou no Drive: a integração rejeitou o upload como `file_too_large` (limite não contornável por nova tentativa). A cópia binária do APK íntegro foi baixada do GitHub na conversa e o artefato GitHub id 14745906741 permanece como fonte de backup; não confundir com os arquivos de build anterior existentes no Drive.

**Critérios ainda externos ao código:** instalar em Android real e provar PvP carregado/ágil/escudos, VGP giro/captura, joystick, Root+Vector, privacidade do nome, pesquisa ao vivo e validade das credenciais/termos dos feeds. A matriz para preencher os testes físicos está em [`docs/QA_LT032_ANDROID_FINAL.md`](docs/QA_LT032_ANDROID_FINAL.md). Sem essas evidências não é legítimo declarar "sem pendências" ou promover para `main`.

---

## 22/11/2026 — PR #66: botão Eventos, visibilidade do seletor e paridade PGRazor

**Origem e branch de QA:** a pedido do usuário, foi consultada a documentação mestra antes de alterar a branch atual `fix/lt032-vgp-fallback-icon-20261122` @ `954e4c0c53e32cf853b261cdceb5da83f94a8bf9`, preservando a linha principal `main` @ `072bd0b38a105612114057b2f946528582ae2d61`. [PR #66](https://github.com/LucianoToscano/PGBR/pull/66) foi aberto em `fix/lt032-eventos-menu-visibilidade-20261122` com base no PR #65; commits `7c13c13c46917e51162f78d392a86b10ee09fbe8` (código e testes), `5f9544a9b66c26e77c3f581b28979096f571bac1` (CI) e `adf3b5c4b0dbe1eb7736669256b1cc5d85d622a8` (matriz de paridade).

### Correções aplicadas ao código (sem publicação automática)

- O botão **Events/“Eventos”** já existia no menu rápido e usa bit **512**, com ação `EventPanel.showSelector`. O menu das configurações também inclui Halloween, GO Wild e Rota Doces Eternatus. O problema era de **visibilidade/personalização**, não ausência de código.
- A página **Configurações → Eventos** ganhou opção `Mostrar Eventos no Menu Pokébola`, sincronizada com o controle da página de personalização, e botão `Abrir seletor de Eventos` para acesso direto.
- **Defeito concreto corrigido:** Selector Pokémon usa bit 8192 no menu e na lista de 14 itens, mas `Control.setQuickMenuVisible` aceitava apenas 1–4096 e `loadSettings` limitava máscara a 8191. A nova `QuickMenuVisibilityPolicy` usa **16383** e mantém os bits 512 e 8192 independentes, sem alterar preferências do gameplay.
- Novos testes `QuickMenuVisibilityPolicyTest.java` + `test_quick_menu_events_java.py`, rodados em JVM e verificações de roteamento no código da interface. O runner Control JVM agora compila a política real.
- Registrei a matriz funcional recurso a recurso de PGRazor 0.3.5/0.3.6 em `docs/PGR036_PARIDADE_EVENTOS_LT032.md` (na branch do PR #66), distinguindo ports nativos idênticos, adaptações de interface PGBR, recursos não portados de site e testes pendentes no jogo.

### Constatação importante sobre Eventos e PvP

O PGRazor upstream `v0.3.6` de 21/11/2026 continua como último release observado; os PRs upstream #10 (joystick) e #11 (ART/Vector) permanecem abertos, mas com portabilidade isolada em PGBR #55/#56. A área de Eventos **não é novidade exclusiva do v0.3.6**: Halloween e GO Wild já constavam da linha anterior do upstream; PGBR acrescentou a opção de missão Rota Doces Eternatus. `EventSettings.java`, `native/events/event_collector.cpp/h` e os testes correspondentes têm SHA idêntico ao upstream. `EventPanel.java` é diferente devido ao terceiro evento personalizado.

**A preferência PvP “priorizar ataque carregado supereficaz” ainda não tem executor nativo correspondente**. O código `rocket_combat.cpp` seleciona o primeiro carregado pronto e o UI já traz nota de recurso experimental. Não declarar corrigido ou testado até disponibilidade de tipo/energia do oponente, identidade dos dois golpes, regras e cenário Android comprovados. As fontes de scanner/feeds privados, a máscara visual de nome na Unity, o VGP e o Root com Vector continuam pendências de validação real.

### Testes e gates deste PR

- [Suíte completa de código #45003323966](https://github.com/LucianoToscano/PGBR/actions/runs/45003323966) — CI host/NDK ARM64/Windows Java+Python+Node+Root, incluindo novos casos JVM do menu.
- [Build Root + sem Root + Zygisk #45003323939](https://github.com/LucianoToscano/PGBR/actions/runs/45003323939) — assinar, empacotar e verificar hashes cruzados dos três artefatos.
- [Governança #45003328261](https://github.com/LucianoToscano/PGBR/actions/runs/45003328261) — garantir inventário e Mestre sem perdas.

**Estado dos dois primeiros gates ao escrever:** execução em andamento, ainda sem atestado de sucesso nem novo link de APK verificado. A PR permanece draft e `main` intocada. Testes de Android físico e links de feed autorizados seguem bloqueados por ambiente/credenciais/consentimento.

---

## 22/11/2026 — PR #67: cinco regressões nativas sazonais aprovadas

[PR #67](https://github.com/LucianoToscano/PGBR/pull/67), branch `test/lt032-eventos-vgp-safari-host-20261122`, base PR #66 @ `adf3b5c4b0dbe1eb7736669256b1cc5d85d622a8`, criado para validar código de Eventos sem Android, com permissão GitHub somente leitura, sem modificar artefatos de gameplay.

**CI confirmado:** [run #45003782779](https://github.com/LucianoToscano/PGBR/actions/runs/45003782779) `success`. O log registra **5 fixtures C++ host**, executando implementações nativas reais: `events/event_collector_test.cpp` (Halloween, atribuição de callback, encontros OFF e ações manuais), `route_gift_policy_test.cpp` (Mateo), `safari_always_hit_test.cpp`, `safari_throws_test.cpp` e `safari_vgp_actions_test.cpp`. As **3 verificações JVM em duas suítes** `test_quick_menu_events_java.py` e `test_event_settings_java.py` também passaram (2+1 testes).

**Falhas de CI diagnosticadas e solucionadas, sem alteração de runtime:** primeira tentativa não tinha `dobby.h` no include de testes; segunda encontrou que o stub compilatório pré-existente não definia o tipo `DobbyRegisterContext` e que fixtures Safari não incluíam `<initializer_list>`. Foi criado um *stub exclusivo de host* `pgrazor/test/seasonal-stubs/dobby.h` de registro x86_64 (não usado pelo build do jogo), e foram adicionadas apenas as inclusões C++ faltantes aos arquivos de teste. A terceira execução concluiu toda a matriz.

**Distinção essencial:** a aprovação não equivale a capturar caixas do Halloween 2026 em jogo real, nem comprova estoque Safari/Mighty ou giro/captura VGP+. Exigir testes no Android e período de evento ativo antes da release. A PR de código/menu #66 passou integralmente o [CI #45003323966](https://github.com/LucianoToscano/PGBR/actions/runs/45003323966), com três jobs verdes. O empacotamento [#45003323939](https://github.com/LucianoToscano/PGBR/actions/runs/45003323939) estava gerando o instalador Root ao escrever esta atualização, sem resultado de artifact conferido ainda.

---

## 22/11/2026 — Artefato LT032 com Eventos restaurados (PR #66) verificado

**CI de código:** [run #45003323966](https://github.com/LucianoToscano/PGBR/actions/runs/45003323966) `success` nos três jobs — Windows `tools/check.py` + JVM + Root, Linux C++ host, NDK ARM64. Os novos testes de visibilidade de Eventos (bit 512), Selector Pokémon (8192) e persistência da máscara (16383) compilaram e passaram. A [governança #45003328261](https://github.com/LucianoToscano/PGBR/actions/runs/45003328261) também passou.

**Compilação e artefatos:** [run #45003323939](https://github.com/LucianoToscano/PGBR/actions/runs/45003323939) `success` em `native-pvp-fixture` e `apk-test`. ZIP de QA `PGBR-LT032-PGR036-Root-e-SemRoot-QA`, artifact GitHub **14746594462**, compilado de `adf3b5c4b0dbe1eb7736669256b1cc5d85d622a8` (PR #66, após teste/doc e ativação de workflow). ZIP testado por CRC; arquivos extraídos e cada SHA-256 comparado à linha respectiva de `SHA256SUMS.txt`, `build-info.json` e `root-build-info.json`.

| Pacote do PR #66 com Menu Eventos | Bytes | SHA-256 exato |
| --- | ---: | --- |
| `PGBR-v0.00.32-LT.apk` (não Root) | 188810563 | `8eb1c6b7e8802b12116a74c2c5dae4ad586d8efc636d217c7a67e4417930df9f` |
| `PGBR-v0.00.32-LT-Root.apk` (instalador) | 8336078 | `ad0c55a725c8123a83e1dda06f25ca3c17a08bd96fafc25874cee3c0fb6648dd` |
| `PGBR-v0.00.32-LT-Root-module.zip` (Zygisk) | 8251835 | `aa4146e2b4b34bc17eb62debd7c82faa42b842868ed00fa87b3dd0a6e30bada5` |

Conferidos dentro dos binários: versão `v0.00.32-LT`, jogo `0.437.0` code `2026110902`, `compiledGameplay=true`, `QuickMenuVisibilityPolicy` presente em DEX e strings “Mostrar Eventos no Menu Pokébola” e “Abrir seletor de Eventos” presentes; ícone VGP OFF recuperado `192×192` idêntico pixel a pixel ao atlas WebP `192×576` **dentro do APK**. Instalador Root embute um módulo ZIP cujo SHA-256 coincide com o externo; módulo tem ABIs `arm64-v8a` e `x86_64`. Detectado Android APK Signing Block em ambos APKs (checagem estrutural, não substitui `apksigner verify`/instalação). `device_tested=false`. A revisão PR #66 é draft e não foi mesclada na `main`.

**Backups confirmados no Google Drive** `PGBR/04_Builds_e_Modulos_PGBR`, sem sobrescrever pacotes anteriores:
- [Root APK 8336078 bytes](https://drive.google.com/file/d/1IeEK19k7FatCQsw4k4l-GFPPbrihjyD6/view)
- [Zygisk módulo 8251835 bytes](https://drive.google.com/file/d/10aDPsjYrrahR2MbiUKP24QgvrAJuMA_N/view)
- [SHA256SUMS da mesma execução](https://drive.google.com/file/d/1HZYnR7cX_ycQxT-tFXmvhb18-ozdmROr/view)

O APK não Root de 188,8 MB permanece acessível no artifact GitHub e pelo download da conversa; a integração Drive já teve limite de tamanho para esse arquivo e não se declarou upload concluído.

**Regressões sazonais separadas:** o [PR #67](https://github.com/LucianoToscano/PGBR/pull/67) em [run #45003782779](https://github.com/LucianoToscano/PGBR/actions/runs/45003782779) passou cinco cenários native host reais (coletor Halloween, Mateo, Safari Always Hit, Safari throw, Safari VGP) e 3 testes JVM adicionais. Essas fixtures não alteram o APK PR #66; podem ser revisadas/mescladas depois sem tocar no código de jogo.

**Restante não homologado:** não há sessão com dispositivo Android físico, prova de Halloween ativo, GO Wild com Safari/Mighty, GPX Eternatus com resgate real, PvP (ataques/escudos e escolha de segundo golpe super eficaz), giro/captura VGP+, Root/Vector, feeds externos autorizados, joystick, ícones completos e privacidade visual garantida em mapa/perfil “Eu”. As 12 prioridades permaneceram documentadas no [relatório PR #66](https://github.com/LucianoToscano/PGBR/blob/fix/lt032-eventos-menu-visibilidade-20261122/docs/PGR036_PARIDADE_EVENTOS_LT032.md) e no protocolo `docs/QA_LT032_ANDROID_FINAL.md`, que ainda depende do operador do telefone. **Não classificar como sem pendências nem publicar release definitiva até completar esses ensaios.**

---

## 22/11/2026 — Consolidação e pré-release PGBR v0.00.33-LT (execução controlada)

**Solicitação explícita:** integrar via merge os recursos LT032 + menu Eventos + fixtures de Halloween, GO Wild/Safari e Mateo, consolidar governança e 12 cenários C++ host, incrementar o identificador do PGBR a `v0.00.33-LT`, disponibilizar três variantes em uma **pré-release QA**, e tornar obrigatória a atualização deste Mestre **em todos os PRs, merges e releases futuros**.

**Base e preservação:** `main` no início da execução `072bd0b38a105612114057b2f946528582ae2d61`; branch de código QA `test/lt032-eventos-vgp-safari-host-20261122` @ `04f0203e89a54bb128158d9f831a6dc97adcc88e`, CI eventos `#45003782779` aprovado; branch de infraestrutura `maintenance/quality-gates-crossplatform-20261122` @ `ba49e7a1699e700291caf6c5289b2f5136e0614e`, CI quatro jobs `#45000885337` aprovado; arquivo mestre completo `docs/pgrazor-v036-auditoria-20261122` @ `1e5a166b52170b091eecfb228c8f5a31b5f8f22e`.

**Integração em branch própria:** `release/pgbr-v0.00.33-lt-20261122`, com commit octopus inicial `aa038fbef56a017e6ade44c43d2a0fcc1a4bc65f`: preserva **todos os ancestrais** QA, manutenção, docs e `main` em quatro pais, sem sobrescrever branch de outro chat. Conflitos selecionados conscientemente: interface/traduções da árvore QA; arquivo `PGBR_ARQUIVO_MESTRE_UNICO.md` + matriz QA da branch docs; runner nativo com 12 fixtures e workflow de quatro jobs (incluindo website) da manutenção. Alterações PT-BR e componentes de interface permanecem da árvore QA; os ajustes de tradução e Runtime não são redefinidos pela versão upstream.

**Política de release:** `pgrazor/VERSION = 0.00.33-LT`; novo workflow `.github/workflows/pgbr-lt033-release.yml` (mantém workflow LT032 histórico intacto) compila e executa `tools/check.py`, testes PvP host, Rust/C++/NDK, gera APK sem Root, instalador Root e módulo Zygisk assinados com os segredos oficiais do repo e verifica `tools/verify_release.py --tag v0.00.33-LT`. O ato de publicar é restrito ao push da `main` e ocorre **somente após verificação de integridade**; cria tag `v0.00.33-LT` e pré-release GitHub, sem alterar uma tag preexistente. Os três pacotes devem ter hashes próprios dessa execução, jamais reutilizar os de LT032.

**Status deste registro antes de CI/merge:** integração preparada, sem proclamar PR merged, publicação concluída ou APK LT033 disponível até retorno do GitHub. Após resultado, anexar comprovantes de CI, PR de integração/merge commit, release, hashes e URLs diretos. Sem dispositivo Android, `device_tested=false`; a prioridade de recursos experimentais PvP/carregado supereficaz, VGP+ real, joystick, Root/Vector, privacidade e feeds autenticados permanece pendente e visível.


### Diagnóstico de CI do candidato LT033 e correção sem renomear APK

A primeira compilação em [Actions #45005115642](https://github.com/LucianoToscano/PGBR/actions/runs/45005115642) e o primeiro CI de código em [#45005143789](https://github.com/LucianoToscano/PGBR/actions/runs/45005143789) identificaram **uma única falha de assertiva legada**, não um crash ou erro dos binários: `test_lt032_consolidation.py::test_latest_selector_and_ci_requirements` exigia `pgrazor/VERSION == 0.00.32-LT` mesmo após o incremento efetivo para LT033. O workflow histórico LT032 segue intacto. A verificação passa a aceitar explicitamente `0.00.32-LT` ou `0.00.33-LT`, com comprovação adicional de que a LT033 usa seu próprio workflow, etiqueta e gate de integridade. O gate Linux de políticas C++, NDK ARM64 e build do website passaram em #45005143789; a etapa Windows falhou **somente** pela assertiva, impedindo a publicação, como esperado. Novo CI e novos hashes são obrigatórios depois da correção. A confirmação posterior do merge/release permanece pendente.


---

## 22/11/2026 — Ajuste idempotente do arquivo upstream após merge LT033

Depois do merge PR #68 em `main` @ `28a365fec1277033a1089cb394aecd6e9cc01115`, a execução de arquivo histórico upstream [#45006532786](https://github.com/LucianoToscano/PGBR/actions/runs/45006532786) falhou porque a release `archive-pgrazor-source-root-20261118` **já existia**. Isso não foi falha dos APKs LT033 e não autoriza substituir esse arquivo histórico. A branch `fix/lt033-archive-idempotency-20261122`, baseada exatamente nessa HEAD da main, acrescenta uma verificação de existência com retorno idempotente e leitura da quantidade de assets, preservando a release original. O workflow continuará limitado a alterações no próprio YAML (`on.push.paths`), sem disparar em atualizações comuns da documentação. **CI/merge desta correção pendentes até verificar o novo PR.**


### Ajuste complementar no PR #69 (sem cancelar build da release)
O workflow `.github/workflows/pgbr-lt033-release.yml` também passa a ter filtro `push.paths` restrito à versão, à própria definição da build e às notas versionadas. O motivo é evitar que a documentação/merge corretivo do arquivo histórico dispare de novo o pipeline de 75 minutos para a **mesma tag imutável**. A execução manual `workflow_dispatch` permanece disponível. O build oficial já iniciado em `main` @ `28a365fec1277033a1089cb394aecd6e9cc01115` não sofre alteração retroativa.


---

## 22/11/2026 — FECHAMENTO factual do merge e publicação GitHub PGBR v0.00.33-LT

**Identificação e integridade:** versão real PGBR `0.00.33-LT`, jogo base Pokémon GO `0.437.0` / code `2026110902`; `pgrazor/VERSION` e `PGRAZOR_VERSION=v0.00.33-LT` confirmados. Tag `v0.00.33-LT` publicada em **22/11/2026 09:26:15 UTC** como **pré-release** GitHub ID `454333149`, `draft=false`, direcionada ao commit de merge da `main` `28a365fec1277033a1089cb394aecd6e9cc01115` ([PR #68, merged](https://github.com/LucianoToscano/PGBR/pull/68)). O arquivo `build-info.json` da execução oficial comprova `source_commit=28a365fec1277033a1089cb394aecd6e9cc01115` e **`device_tested=false`**; `root-build-info.json` declara `version=v0.00.33-LT` e targets `arm64-v8a` / `x86_64`.

**Evidência CI antes do merge:** [código PR #68 #45005415348](https://github.com/LucianoToscano/PGBR/actions/runs/45005415348) **success 4/4** (Windows Java/Python/Node/Root, 12 C++ host, NDK ARM64, website); [governança #45005415414](https://github.com/LucianoToscano/PGBR/actions/runs/45005415414) **success**; [triplet PR #68 #45005410258](https://github.com/LucianoToscano/PGBR/actions/runs/45005410258) **success**; [cinco fixtures sazonais #45003782779](https://github.com/LucianoToscano/PGBR/actions/runs/45003782779) **success**.

**Evidência CI depois do merge:** [código `main` #45006532866](https://github.com/LucianoToscano/PGBR/actions/runs/45006532866) **success 4/4**, [governança #45006532740](https://github.com/LucianoToscano/PGBR/actions/runs/45006532740) **success**, [branding #45006532753](https://github.com/LucianoToscano/PGBR/actions/runs/45006532753) **success**, e [build/release oficial da `main` #45006532739](https://github.com/LucianoToscano/PGBR/actions/runs/45006532739) **success**. Artefato GitHub Actions `PGBR-LT033-PGR036-Root-e-SemRoot-QA` ID `14748332530`; ZIP do artefato foi baixado, validado pelo CRC e os três SHA-256 conferidos pela leitura completa de cada arquivo contra `SHA256SUMS.txt` e `build-info.json`/ `root-build-info.json`.

| Artefato oficial LT033 (real, não renomeado) | Bytes | SHA-256 SHA256SUMS.txt |
| --- | ---: | --- |
| [APK completo sem Root](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.33-LT/PGBR-v0.00.33-LT.apk) | 188810563 | `484ae6ff6eee4f7d72b1bc8bec9dae8d9c4baa22d154561776baebd0c8ac4b09` |
| [Instalador Root](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.33-LT/PGBR-v0.00.33-LT-Root.apk) | 8336078 | `6116a4f482d23c3e0b0cae8fcac3152bed84da14cc58e844672d788221a835d6` |
| [Módulo Zygisk](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.33-LT/PGBR-v0.00.33-LT-Root-module.zip) | 8251868 | `66a5523c3589286af42116bcc674278596f3bc2de72b20bc0794f3a70137ed62` |

[SHA256SUMS oficial](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.33-LT/SHA256SUMS.txt), [build-info.json](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.33-LT/build-info.json), [root-build-info.json](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.33-LT/root-build-info.json), [página da pré-release](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.33-LT). A release pública tem **exatamente seis assets**, incluindo os três instaláveis, sem substituir a `v0.00.23-LT` ou quaisquer pacotes LT032 anteriores.

**O que está publicado vs. comprovado:** a `main` contém as correções para Eventos no Menu Pokébola (bit 512), Seletor Pokémon (bit 8192), testes VGP/Safari/Halloween e ports do PGRazor 0.3.5/0.3.6. Build e testes automatizados estão comprovados. **Não são homologados em dispositivo**: abertura do jogo, PvP real, escolha de segundo carregado supereficaz (ainda não implementado), uso de escudos, VGP+ capturando/girando, joystick, Root/Vector ARM64, ocultação do nome nas duas telas, scanner/feeds externos e Halloween/GO Wild/Eternatus reais. Preservar `device_tested=false`, não anunciar estabilidade nem segurança de contas.

**Pendência de limpeza de CI independente da release:** `#45006532786` falhou apenas porque o workflow `pgbr-archive-root-assets.yml` tentou recriar um tag de arquivo upstream existente. [PR #69](https://github.com/LucianoToscano/PGBR/pull/69) corrige a idempotência e impede que merges só documentais disparem novamente o workflow da tag LT033. A cadeia de CI de #69 deve ser confirmada, integrada à `main` e ter merge SHA final registrado neste mesmo Mestre.

**REGRA PERMANENTE REAFIRMADA:** para cada tarefa e merge, registrar neste único Mestre fonte, HEAD, alterações, testes, APK, tag, hashes, URLs e pendências; atualizar novamente após merge quando necessário. Nenhum ChatGPT, branch ou workflow substitui essa obrigação.

---

## 22/11/2026 — Pós-merge PR #69: arquivamento idempotente, Mestre sincronizado e LT033 preservada

**Consulta realizada antes da alteração:** `main` @ `ef87f76daaf20a68c44f05d196f5d31ffd18038a`, [release v0.00.33-LT](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.33-LT) publicada como **pré-release QA** com seis assets, tag `refs/tags/v0.00.33-LT` mantida no commit de aplicação `28a365fec1277033a1089cb394aecd6e9cc01115`. A nova HEAD avançou **por correção de CI/arquivo**, não por novo build do aplicativo. Portanto é **incorreto** anunciar APK novo ou renomear a versão: os três assets publicados continuam os da LT033 e mantêm os hashes da tabela “FECHAMENTO factual”.

**Merge efetivamente concluído:** [PR #69](https://github.com/LucianoToscano/PGBR/pull/69) `fix/lt033-archive-idempotency-20261122` @ `506d03c09af926b3f6d9d5932bbcbd132f4ad4ed` foi **merged** na `main`, merge commit `ef87f76daaf20a68c44f05d196f5d31ffd18038a`. [CI completo da branch #45007763455](https://github.com/LucianoToscano/PGBR/actions/runs/45007763455) **success**, e [governança #45007763457](https://github.com/LucianoToscano/PGBR/actions/runs/45007763457) **success** antes do merge. Mudança: arquivamento de releases anteriores não tenta recriar tag/arquivo existente, é idempotente e conserva todos os assets; workflow de LT033 passa a respeitar `push.paths` para não reconstruir a mesma tag após merge apenas documental; teste adicional `pgrazor/test/test_archiver_idempotency_policy.py`. Arquivos modificados no PR: `.github/workflows/pgbr-archive-root-assets.yml`, `.github/workflows/pgbr-lt033-release.yml`, este Mestre e o teste.

**PRs históricos sem perda de código:** [#66](https://github.com/LucianoToscano/PGBR/pull/66) e [#67](https://github.com/LucianoToscano/PGBR/pull/67) ainda podem aparecer abertos/draft no GitHub, mas seus commits são **ancestrais da `main`** via PR #68 (comparação `main...branches` mostra `ahead=0` para as duas). Não integrá-los novamente; nenhum arquivo dessas branches foi excluído. [PR #70](https://github.com/LucianoToscano/PGBR/pull/70), criado em paralelo para registrar o release, foi encerrado **sem merge** como documentalmente redundante após #69; a branch e seus commits permanecem preservados.

**Evidência da release que continua válida:** [workflow de publicação #45006532739](https://github.com/LucianoToscano/PGBR/actions/runs/45006532739) **success**, [CI `main` da versão #45006532866](https://github.com/LucianoToscano/PGBR/actions/runs/45006532866) **success**, tag e release `v0.00.33-LT` id 454333149 com links diretos ao APK, Root e ZIP no quadro “FECHAMENTO factual”. Nenhuma tag foi sobrescrita, nem foi feito novo upload binário nesta tarefa.

**Falta comprovar antes de homologação:** instalação em aparelho Android físico; ataques carregados/escudos e priorização supereficaz no PvP; VGP+ capturando/girando; Root+Vector; joystick; privacidade visual no mapa/perfil; eventos sazonais reais; feeds autorizados e comparação visual. Status permanece **pré-release QA / `device_tested=false`**. A regra permanente exige atualizar este mesmo Mestre após cada mudança relevante; esta anotação documenta o merge #69 de forma retroativa e verificável, sem inventar resultados físicos.

---
## 22/11/2026 — Implementação do painel manual Sniper 100% IV (branch de recurso)

**Ramo e base conferidos:** `feat/sniper100iv-manual-feed-20261122`, criado a partir da `main` em `129108d57497f6af9f997997294b5e17c1dc0556`. **Natureza:** adição funcional em código-fonte, revisão/CI pendentes; **não** é release LT034, teste físico, conexão de feed aprovada ou APK assinado.

### Pedido e referência de comportamento
O responsável reiterou sua autorização previamente declarada e documentada no Guia Mestre PokeSearch (LT364/LT365), sem exigir repetição da solicitação. Os vídeos PGSharp 11, 12 e 21 e a captura de configurações mostram Radar Próximos, Sniper I/II independentes e listagem 100% IV no plano gratuito do PGSharp, com limitação de filtros. Essa UI não concede ao PGBR acesso automático à API proprietária do PGSharp. O trabalho aqui reutiliza o código e a URL de feed compacto **já existentes no próprio PGBR**, não tenta reproduzir autenticação de outro cliente.

### Mudanças realizadas na branch
- `pgrazor/src/dev/pgrazor/HundoFeedData.java`: decodificador independente `v=1` de `hundos.json` (id, espécie, forma, lat/lon, expiração, CP e nível opcionais). Rejeita snapshot ausente, antigo ou com data futura, coordenadas inválidas, IDs/espécies fora de domínio, alvos expirados e duplicados; impõe limites de linhas. As entradas “100% IV” são **afirmações do feed**, não IVs conferidos por leitura do jogo. Sem preencher estatísticas ausentes.
- `pgrazor/src/dev/pgrazor/HundoFeedDialog.java`: painel Android com sprites por espécie via `LiveFeedIcons`, pesquisa local por #Dex, uma consulta inicial e atualização **somente manual**, limite mínimo de 30 s entre requisições, sem autorretry; recusa HTTP 401/403/429 e respostas acima de 8 MB; status explícito de erro/indisponibilidade. Atualiza contagem e expiração visual a cada 10 s **sem consumir rede**. Toque permite copiar coordenadas ou encaminhar teleporte via `TeleportDialog.confirm` e `Control.canEnable()`; nunca captura automaticamente.
- `pgrazor/src/dev/pgrazor/FeedChooser.java`: adiciona uma quinta opção “Sniper 100% IV · PGBR” **sem mudar os índices 0–3** das buscas Pokémon/reides, Radar LivePokeMap e PoGoCoordinates. Não modifica os dois Snipers já existentes nem sua persistência.
- `pgrazor/test/HundoFeedDataTest.java`: fixtures sintéticas de expiração, idade, formato, deduplicação, atributos desconhecidos e lista vazia.
- `pgrazor/test/test_live_feed_java.py`: conecta esse teste ao runner JVM existente com `org.json` Android.
- `pgrazor/test/test_hundo_manual_ui_contract.py`: guardas estáticas para preservar menu, impedir automação acidental e exigir confirmação/estado de prontidão.
- `.github/workflows/pgbr-hundo100-qa.yml`: pipeline PR para as verificações acima.
- `docs/HUNDO_FEED_AUTORIZACAO.md`: preserva a declaração prévia do usuário e documenta condições concretas da leitura manual, sem confundir licença comercial e viabilidade de backend.

### Evidência e limites
**Executado:** leitura do Mestre, inspeção do código da `main`, gravação dos arquivos na branch, conferência das alterações em GitHub. **Pendente:** executar/testar CI e compilação Android, consultar uma resposta do feed em produção, validar botão/resultados/cooldown/teleporte em Samsung físico, auditar comportamento de contas e artefatos. A URL pública referenciada pelo código não pôde ser comprovada operacional na tentativa de verificação web desta conversa; o painel falha explicitamente fechado em caso de erro, sem criar spawns. Não foram alterados servidor, credenciais, automação Shundo existente, PvP, VGP+, joystick, APK original PGSharp ou projeto PokeSearch.

**Backlog paralelo preservado:** controles de Sniper I/II horizontal/vertical inspirados nas mídias, ícones/vetores de menu e efeitos de toque do PGSharp como referência (não importar sem direito de distribuição), PvP de ataques/esquivas/escudos e compatibilidade de joystick. Nenhum desses itens foi homologado ou anunciado como corrigido por esta PR.

**Estado:** FONTE ALTERADA, QA PENDENTE, `device_tested=false`. Integrar somente com PR revisada e comparar de novo com a HEAD vigente antes do merge. Sem criar nova release automaticamente.


### Atualização de QA da PR #72 — 22/11/2026
O módulo Sniper 100% IV foi implementado em fonte e submetido como [PR #72](https://github.com/LucianoToscano/PGBR/pull/72) em draft. No commit `fbdd819966608e9a9b148e75aaa8bdf7282e06b3` (última alteração de código após corrigir a exceção checked do fixture), foram verificados:
- **CI específico Sniper 100% IV**: [Actions #45009944092](https://github.com/LucianoToscano/PGBR/actions/runs/45009944092), status **SUCCESS**: testes estruturais de UI e parser Java/suite de feed com `org.json` Android.
- **CI geral de código**: [Actions #45009944093](https://github.com/LucianoToscano/PGBR/actions/runs/45009944093), status **SUCCESS**: testes C++ host, ARM64 NDK, frontend, suíte `tools/check.py`, compilação do código Java do aplicativo e validação de compilação do instalador Root.
- **Auditoria conservadora do Mestre**: [Actions #45009944127](https://github.com/LucianoToscano/PGBR/actions/runs/45009944127), status **SUCCESS**, sem limpeza/descarte.
- O CI inicial [#45009872068](https://github.com/LucianoToscano/PGBR/actions/runs/45009872068) falhou na declaração da exceção de um **teste Java**, foi corrigido na branch e o novo CI passou. **Não ocultar o histórico da correção.**

**Status atualizado:** IMPLEMENTADO EM FONTE + QA/COMPILAÇÃO DE CÓDIGO APROVADOS; APK de jogo completo **não gerado**, assinatura de instalador **não testada**, nenhuma instalação física, nenhum acesso ao feed ao vivo confirmado, nenhum servidor implantado. Estes testes não homologam o Sniper 100% IV em dispositivo. Modificações posteriores somente documentais devem ser verificadas em um diff antes da integração na `main`. **Preservar a branch e a PR para revisão.**

---

## 22/11/2026 — Provedor PGSharp Free e armazenamento opcional de key (PR #73)

**Pedido:** manter o Sniper 100% IV atual do PGBR e acrescentar **fonte distinta PGSharp**, com feed de 100% IV gratuito para qualquer usuário e possibilidade futura de filtros extras mediante login com a key Standard, sem repetir solicitação de autorização já documentada no Guia Mestre do PokeSearch LT364/LT365.

**Produtos oficialmente declarados pelo PGSharp (consultados em 22/11/2026):** versão Free inclui *100 IV Feed*; Standard inclui *All Coordinates Feed*. Fontes: https://www.pgsharp.com/ e https://www.pgsharp.com/features/. Do APK plugin 1.278.0 foram identificadas strings e pontes `https://f.pgsharp.com/peanuts`, `q4.d(k5/b)`, `q4.e(k5/b)`, `FASTSNIPE/FASTSNIPE2` e `post.auth.lic`; **não** foi comprovado contrato completo para integração HTTP externa, login de key ou equivalência de dados. Não confundir gratuidade do recurso no aplicativo PGSharp com API já operacional no PGBR.

**Governança:** branch `feat/pgsharp-free-key-tier-20261122`, criada do commit `c3430532a363ca0704e713c6a4a2c7e492c301d6` (PR #72), PR dependente **[#73](https://github.com/LucianoToscano/PGBR/pull/73)**, draft, base `feat/sniper100iv-manual-feed-20261122`. Não fazer merge direto em `main`, nem sobrescrever trabalhos paralelos. A PR #72 permanece base para a consulta manual do feed PGBR.

**Código novo nesta PR:**
- `PgSharpFeedPolicy.java`: política Free/Standard, conector não verificado por padrão, key guardada localmente não libera funções pagas. Com conector verificado, o Free não exige key, mas o Standard exige verificação de licença pelo provedor.
- `PgSharpFeedDialog.java`: nova opção PGSharp no menu de buscas (índice 5, preservando 0–4), com informações honestas de disponibilidade e botão para abrir o Sniper PGBR atual, sem atribuir-lhe origem PGSharp.
- `PgSharpLicenseVault.java`: armazenamento local opcional de key, AES-256/GCM com AndroidKeyStore, arquivo exclusivo sob `getNoBackupFilesDir()`, escrita atômica, sem logs de chave, entrada mascarada com `FLAG_SECURE`, botão de exclusão. Não usar o cofre de sessões de Pokémon GO.
- `PgSharpFeedPolicyTest.java`, `test_pgsharp_policy_java.py`, `test_pgsharp_provider_contract.py`, `.github/workflows/pgbr-pgsharp-provider-qa.yml`, `docs/PGSHARP_FREE_FEED_KEY_DESIGN_2026-11-22.md`: testes e contrato de integração.

**Limites preservados:** nenhum request HTTP ao PGSharp, nenhuma key transmitida/verificada, nenhum feed proprietário consumido, nenhuma captura ou teleporte PGSharp validado; configuração de key **não** é login. API, autenticação, lat/lng/IV/despawn e permissões do serviço deverão ser confirmados em nova etapa. Não gerar keygen, não copiar credenciais de Pokémon GO, não inventar resultados, não usar automaticamente backend alternativo sob o nome PGSharp.

**Estado desta revisão:** IMPLEMENTADO EM FONTE (UI, política, cofre); QA automático sendo verificado em [workflow da PR #73](https://github.com/LucianoToscano/PGBR/pull/73/checks); compilação e teste Android físico não equivalentes; nenhum APK novo/publicação/merge. Atualizar este registro com resultados efetivos após os jobs terminarem. **A linha LT033 publicada permanece imutável, `device_tested=false`.**


### CI validado — ponte PGSharp Free/Standard — PR #73
Última alteração de código do conector opcional: commit `2366ccc0948b79b7469e0598d54dab7c4817b213`, que protege as tarefas de key contra encerramento da tela. A anotação do Mestre foi acrescentada no commit `1d00a173bbf782916d72b0be62e3cdf3f7fd0bfd`.

- [QA PGSharp Free/Key #45011614066](https://github.com/LucianoToscano/PGBR/actions/runs/45011614066): **SUCCESS**; verificações Java de níveis Free/Standard, proteção de chave/isolamento de origem e compilação de todas as classes Java Android. **Não são testes de acesso à API proprietária nem teste físico**.
- [Auditoria Mestre e inventário #45011614067](https://github.com/LucianoToscano/PGBR/actions/runs/45011614067): **SUCCESS**, sem exclusão.
- Permanecem em aberto, **não implementados nesta PR**: chamada efetiva a `f.pgsharp.com/peanuts`, respostas reais 100 IV do PGSharp, login/validação da key Standard no backend, quotas e escopos de filtros pagos, uso de coordenadas PGSharp em teleporte, APK assinado/testado fisicamente. Não declarar fonte operacional até demonstrar essas etapas.

**Estado definitivo deste registro:** interface/arquitetura e cofre de key implementados, QA de compilação/contratos aprovado; rede PGSharp intencionalmente inativa por falta do contrato completo. [PR #73 em draft](https://github.com/LucianoToscano/PGBR/pull/73), depende da PR #72; nenhuma mesclada na `main`.

---

## 22/11/2026 — Preparação da LT034: Sniper 100% IV e provedor PGSharp separado

**Pedido explícito do proprietário:** integrar o código das PRs #72 e #73 à `main`, gerar novo APK completo/Root com o mesmo certificado da LT033, publicar atualização e registrar todos os artefatos, sem apagar releases anteriores.

**Base/merges verificados no GitHub:**
- `main` antes do merge: `129108d57497f6af9f997997294b5e17c1dc0556`.
- [PR #72](https://github.com/LucianoToscano/PGBR/pull/72): Sniper 100% IV manual, merge `dd70a3256fc17b25b176476a60131457ba632749`, testes de feed #45010221816 e CI completo #45010221821 aprovados.
- [PR #73](https://github.com/LucianoToscano/PGBR/pull/73): origem PGSharp Free/Standard e cofre opcional de key, merge `bf1c93e43fe23a4aa6fd9934529303e22b8642c8`, teste de política + compilação Java #45011698758 aprovado.
- Nova branch `release/pgbr-v0.00.34-lt-sniper-20261122`, inicialmente no commit da `main` `bf1c93e43fe23a4aa6fd9934529303e22b8642c8`. Não trabalhar sobre cópia anterior.

**Escopo entregue pelas PRs:** o quinto item do menu é o Sniper 100 IV do PGBR, com leitura manual do JSON da fonte `ShundoSniper.FEED_URL` (FaustusFoundry), validação de frescor e coordenadas e ação de copiar/teleporte com confirmação. O sexto item PGSharp Free/Standard é uma origem distinta, com conector **inativo** e key opcional guardada em `AndroidKeyStore`, nunca transmitida nem validada nesta etapa. A edição gratuita do PGSharp inclui 100 IV Feed, mas o PGBR ainda não consulta o endpoint proprietário: não publicar alegação de “mesmos Pokémon 100% IV do PGSharp”. Nada foi alterado no PokeSearch ou no cliente comercial PGSharp.

**Plano LT034:** criar workflow específico `.github/workflows/pgbr-lt034-release.yml` (preservando os workflows LT032/LT033), incrementar `pgrazor/VERSION` para `0.00.34-LT`, atualizar as verificações de regressão versionadas, criar `docs/releases/v0.00.34-LT.md`, compilar com ferramentas fixadas/entrada original revisada e segredos de assinatura existentes, executar `tools/check.py`, gerar os três pacotes, verificar `tools/verify_release.py --tag v0.00.34-LT`, `SHA256SUMS.txt` e metadados de Root, e publicar exclusivamente como **pré-release QA** da `main`. A tag LT033 e seus seis assets não podem ser substituídos.

**Neste registro preliminar:** merges concluídos, preparação de nova compilação em andamento. Não há APK LT034, hash, URL de download verificada, teste físico nem disponibilidade de feed PGSharp comprovada. A publicação só será considerada concluída após presença de cada asset no release e validação de checksum; status no dispositivo permanece `device_tested=false`. Após o workflow terminar, anexar número de run, commit de merge, hashes e links efetivos neste mesmo Mestre. Preservar todas as pendências anteriores de PvP, VGP, joystick, privacidade, Root e feeds.

### Diagnóstico e reexecução CI LT034 — 22/11/2026

- [Primeiro CI PR #74 #45012569041](https://github.com/LucianoToscano/PGBR/actions/runs/45012569041): políticas C++ host, NDK ARM64 e website passaram; etapa Windows `tools/check.py` encontrou **uma falha de contrato textual**, `test_branding.PGBRBrandingTests.test_main_readme`, que exigia a presença da string literal `PGBR-v0.00.34-LT.apk` no README. Resultado exato: `Ran 279 tests; FAILED (failures=1, skipped=1)`. Não foi crash do Sniper/PGSharp nem erro de compilação do aplicativo.
- README corrigido no commit `8d311c9fb1f3c0db5fd41c9d2d021a3e69901de9`, preservando LT033 e explicitando que link LT034 só funciona após publicação.
- Workflow isolado LT034 reexecutado após correção no commit `bf4a659bfd7c18547935c4d6720d05204813c58d`, [run #45012893288](https://github.com/LucianoToscano/PGBR/actions/runs/45012893288). Status e resultados **pendentes de leitura efetiva**; não reutilizar artefatos do commit anterior nem anunciar link/hashes novos antes do sucesso.
- O primeiro run da build #45012564986 pertence à revisão anterior à correção do README; seu resultado não pode substituir a validação da HEAD atual.


---

## 22/11/2026 — PUBLICAÇÃO VERIFICADA PGBR v0.00.34-LT

**Pedido concluído nesta etapa:** executar merge das implementações de Sniper 100% IV + opção PGSharp Free/Standard, incrementar PGBR, compilar três variantes assinadas e publicar pré-release QA, mantendo LT033 e atualizando registro documental.

**Merges e proveniência (confirmados no GitHub):**
- [PR #72](https://github.com/LucianoToscano/PGBR/pull/72) merge `dd70a3256fc17b25b176476a60131457ba632749` — painel manual 100% IV com a fonte PGBR existente.
- [PR #73](https://github.com/LucianoToscano/PGBR/pull/73) merge `bf1c93e43fe23a4aa6fd9934529303e22b8642c8` — opção PGSharp e cofre opt-in de key Standard.
- [PR #74](https://github.com/LucianoToscano/PGBR/pull/74) merge `156a34d72f3f969a5b5bda878dcb548a7fed52e4` — aumento para `0.00.34-LT`, workflow exclusivo e notas de release. Esta é a fonte da tag.
- Nenhum reset de branches e nenhum arquivo da LT033 sobrescrito; workflows históricos mantidos.

**QA e compilação confirmados:**
- Primeira revisão do CI LT034 [#45012569041](https://github.com/LucianoToscano/PGBR/actions/runs/45012569041) detectou 1 falha de contrato de nome de APK no README; corrigida em `8d311c9fb1f3c0db5fd41c9d2d021a3e69901de9`. O histórico foi preservado.
- CI de código da PR corrigida [#45012928973](https://github.com/LucianoToscano/PGBR/actions/runs/45012928973) e auditoria [#45012929052](https://github.com/LucianoToscano/PGBR/actions/runs/45012929052): **SUCCESS**. Build preliminar da branch [#45012893288](https://github.com/LucianoToscano/PGBR/actions/runs/45012893288): **SUCCESS**, artefato GitHub ID `14751241845`.
- CI da `main` [#45013312319](https://github.com/LucianoToscano/PGBR/actions/runs/45013312319): **SUCCESS 4/4** (C++ host, ARM64 NDK, Java/Root e website); governança [#45013312436](https://github.com/LucianoToscano/PGBR/actions/runs/45013312436) **SUCCESS**; branding [#45013312335](https://github.com/LucianoToscano/PGBR/actions/runs/45013312335) **SUCCESS**.
- Build/release oficial `main`: [Actions #45013312358](https://github.com/LucianoToscano/PGBR/actions/runs/45013312358), **SUCCESS 2/2**, com módulo PvP host e APK/Root/integração de hashes; artifact `PGBR-LT034-PGR036-Root-e-SemRoot-QA` ID `14750583041` (194734364 bytes), separado do ZIP de homologação.
- [Release oficial v0.00.34-LT](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.34-LT): ID `454377337`, publishedAt `2026-11-22T10:49:19Z`, `prerelease=true`, tag direcionada ao merge `156a34d72f3f969a5b5bda878dcb548a7fed52e4`. A API GitHub lista **seis assets reais** e seus digests SHA-256.

**Arquivos instaláveis publicados (digest informado diretamente na API Releases):**

| Artefato | Bytes reais | SHA-256 |
| --- | ---: | --- |
| [APK sem Root](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.34-LT/PGBR-v0.00.34-LT.apk) | 188822851 | `6870ee9068acb1e0c9db67bb7d14e82bb66b57dae90555f2f8d28b162d207308` |
| [Instalador Root APK](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.34-LT/PGBR-v0.00.34-LT-Root.apk) | 8348366 | `5528f7b390859de917eec127cbd086e9d9a20fd1bd26ccfb46f334f5fe39d8f7` |
| [Módulo Zygisk ZIP](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.34-LT/PGBR-v0.00.34-LT-Root-module.zip) | 8263546 | `f39c885c5cff6a114d711b7b06907576f23fa317982fbee5eb001823c030f476` |

**Demais comprovantes do mesmo release:** [SHA256SUMS.txt](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.34-LT/SHA256SUMS.txt), [build-info.json](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.34-LT/build-info.json) e [root-build-info.json](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.34-LT/root-build-info.json). A API GitHub confirma bytes e SHA de cada um; verificação cruzada de release foi aprovada pelo workflow, sem inventar hash. A tag e os instaláveis `v0.00.33-LT` permanecem publicados inalterados.

**Escopo realmente entregue:** painel manual do feed 100% IV existente, origem PGSharp identificada separadamente e armazenamento de key opt-in local. **Não foi implementado** acesso ao servidor de resultados 100% do PGSharp nem validação de key Standard. Guardar uma key não ativa licença; resultados do feed FaustusFoundry não podem ser rotulados como PGSharp. Ausência de conexão/erro retorna estado explícito, sem fabricar spawns. Sem captura automática decorrente de tocar resultado. Teleporte depende de confirmação.

**Estado físico:** `device_tested=false`; nenhum teste de instalação/upgrade/rollback, servidor de feeds ao vivo, Root+Vector, VGP+ girando/capturando, uso real de ataque carregado/escudo PvP, joystick/cooldown ou ocultação visual no mapa/perfil foi homologado neste trabalho. Não afirmar conclusão funcional dessas pendências a partir de CI. Preservar backup do aparelho antes de testar.


### Fechamento pós-publicação — CI da documentação LT034 integralmente verde

A documentação final foi incorporada na [PR #75](https://github.com/LucianoToscano/PGBR/pull/75), merge `1e68fcd3a18e37b26a93a801b2eadd60b5166512`. A revisão de integridade posterior confirmou que a HEAD da `main` permaneceu nesse SHA, enquanto a tag `v0.00.34-LT` continua apontando para o commit do build `156a34d72f3f969a5b5bda878dcb548a7fed52e4`.

**Resultados finais verificados dos três workflows pós-merge documental:**
- [#45014540350](https://github.com/LucianoToscano/PGBR/actions/runs/45014540350): **SUCCESS, 4/4 jobs**, incluindo `tools/check.py`, compilação Java/Root, C++ host, NDK ARM64 e website.
- [#45014540417](https://github.com/LucianoToscano/PGBR/actions/runs/45014540417): **SUCCESS**, validação de branding/README.
- [#45014540433](https://github.com/LucianoToscano/PGBR/actions/runs/45014540433): **SUCCESS**, governança, integridade do Mestre e auditoria conservadora.

A API Releases segue retornando `prerelease=true` e seis assets com os **mesmos bytes e digests SHA-256** já registrados na tabela de publicação acima. A nota é apenas documental: **nenhuma nova compilação, alteração de assinatura, reconstrução ou substituição do APK LT034 ocorreu**. Os links de download direto e os três hashes da publicação anterior continuam canônicos. O status `device_tested=false` permanece; o feed diretamente originado no PGSharp e o login Standard seguem não implementados, conforme registro anterior.


## 22/11/2026 — PGBR: correção isolada do observador PvP (PGSharp como referência)
**Pedido:** portar as funções de Auto PvP do PGSharp para PGBR, com autorização declarada pelo responsável. **Base:** `main` @ `1e68fcd3a18e37b26a93a801b2eadd60b5166512`; branch isolada `fix/pvp-pgsharp-screen-observer-20261122`. Fontes: APKs PGSharp 1.33.0, 1.50.3, 1.278.0 e vídeos 13/14 previamente inventariados no Drive.

**Mudança real em fonte:** `pgrazor/native/cutscene_skip.cpp` não torna `tried` permanente antes do bootstrap IL2CPP e aceita o observador `GuiController.Open` validado quando nenhum hook opcional de cutscene está disponível. Novo teste de regressão `pgrazor/test/test_pvp_trace_bootstrap.py`; detalhamento em `docs/PGSHARP_PVP_BRIDGE_AUDIT_20261122.md`. Sem alteração de ABI, recursos Rocket, ataques/escudos, UI, PokéSearch, APKs nem automação em servidores.

**Evidência e limites:** não foi possível inspecionar os executáveis dos APKs nem reproduzir os MP4 no runtime nesta sessão; não foi importado código fechado do PGSharp. Nenhuma função de Auto PvP foi homologada; testes locais e CI ainda pendentes, `device_tested=false`; segundo carregado supereficaz continua não integrado. Revisão pendente por PR, sem merge/publicação automática.


## 22/11/2026 — LT035: Auto PvP, observador e ciclo de vida; candidato a QA
**Branch isolada:** `fix/pvp-pgsharp-screen-observer-20261122`, PR #76. **Base:** main `1e68fcd3a18e37b26a93a801b2eadd60b5166512`; primeiro commit `5f130d0f7bfbb2d075891ca13c17b8a16e82258e` com 5/5 verificações CI `success` no GitHub. O próximo commit também corrige `pvp_scene.h`/`combat_host.cpp`: sessão reconhecida Hub→Preparação→VNext, renovação somente por eventos VNext válidos, limite absoluto 20 minutos, bloqueio de NPC/resolução e expiração sem observação. Novo teste de regressão `pvp_scene_test.cpp`. Preserva Rocket, reides, VGP, joystick, PokeSearch e a ABI existente.

**Release:** versão 0.00.35-LT, notas `docs/releases/v0.00.35-LT.md` e novo workflow `.github/workflows/pgbr-lt035-release.yml` para compilar/assinar/publicar apenas como pré-release QA, mantendo v0.00.34 intacta. A compilação e os links da LT035 precisam ser verificados pela execução, **não presumidos**. A fonte da mudança é código próprio PGBR; **não houve extração/cópia literal** de funções dos APKs PGSharp 1.33/1.50/1.278 na sessão por indisponibilidade de runtime binário, embora o responsável declare autorização. As duas mídias PvP têm auditoria prévia, sem reprodução nova. **device_tested=false** e segundo carregado supereficaz não conectado ao nativo; pendência de teste real GBL, escudos e golpes permanece.

### Continuidade de desenvolvimento paralelo — novo ramo sobre a main atual
No momento desta integração, a main já havia avançado até `19a42fd564e77e2f1303714a1734dc00cf9cd692` por outros chats alterando exclusivamente o Mestre; esses registros foram preservados integralmente. Branch de integração limpa `release/pgbr-lt035-pvp-rebased-20261122` sobre essa base, com os mesmos arquivos de código da revisão PvP LT035 e adição documental sem sobrescrever histórico. A versão permanece pré-release QA e `device_tested=false`.


### LT035 — contrato de versões e branding (preparação QA)
O versionamento 0.00.35-LT exigiu atualizar os contratos de testes anteriores (LT032, LT033, LT034) para **aceitar a nova versão sem remover a validação histórica**. Adicionado `test_lt035_release_contract.py` que verifica workflow/nomes, preservação de workflows 33/34, estado `device_tested=false` e guardas de escopo PvP. README e CHANGELOG foram atualizados para mostrar LT035 como **candidato ainda não publicado**, mantendo o download LT034 existente como versão verificadamente publicada. CI e nova release não são presumidos.


### LT035 — evitar reexecução das releases históricas
O bump do `pgrazor/VERSION` acionaria acidentalmente os antigos workflows LT033 e LT034, que usam nomes e tags imutáveis próprios. Foram removidos **somente os filtros de push sobre `pgrazor/VERSION`** dos dois workflows antigos. Seus steps, tags, documentação, gatilhos manuais `workflow_dispatch` e os gatilhos para mudanças em seus próprios arquivos/notas continuam preservados. O workflow LT035 é o único acionado automaticamente por incremento do VERSION atual. Teste novo protege essa regra. Nenhuma release antiga é alterada.

**Ajuste de acionamento do mesmo candidato LT035:** também removidos os gatilhos automáticos por alteração dos próprios YAMLs das releases antigas 033/034, para impedir que o mero ajuste de trigger desse PR provoque seus builds históricos. Permanecem os gatilhos `workflow_dispatch` manuais e os de notas de release, sem exclusão de steps antigos.


**Correção de CI LT035 após execução real:** [run 45018365260](https://github.com/LucianoToscano/PGBR/actions/runs/45018365260) teve C++ host, NDK ARM64, website e governança com `success`, mas `tools/check.py` falhou em 1 de 281 testes Python: `test_archiver_idempotency_policy.py` ainda exigia o gatilho `pgrazor/VERSION` na release antiga LT033. O teste foi atualizado para exigir **o comportamento correto de imutabilidade da LT033**, com `workflow_dispatch` e notas preservados e sem rebuild automático por incremento LT035. CI do novo commit ainda pendente, sem alegar aprovação antecipada.


### LT035 — integração efetuada (22/11/2026, registro pós-merge)
A [PR #79](https://github.com/LucianoToscano/PGBR/pull/79) foi **integrada à main**, merge commit `e91b881f000fe7da9f57aed2cafabb1b4a9a1899`, após **5/5 verificações CI `success`** no commit de cabeça `547f4b1922a67fdde0db39a06fff2791030941f6` ([GitHub Actions CI #45018666239](https://github.com/LucianoToscano/PGBR/actions/runs/45018666239); [governança #45018666221](https://github.com/LucianoToscano/PGBR/actions/runs/45018666221)). A PR #79 reuniu e substituiu as PRs #76 e #78, encerradas sem merge para evitar duplicação. Código PGBR de observação e ciclo de vida PvP integrado; **não houve extração/cópia literal das funções PGSharp**, nem validação GBL real. `device_tested=false`.

O merge da main iniciou o **workflow de release LT035** [#45018980405](https://github.com/LucianoToscano/PGBR/actions/runs/45018980405), em execução quando este registro foi escrito. A fixture nativa PvP havia concluído com `success`; job de APK/Root ainda estava em andamento. **Tag LT035, URLs de download, hashes e publicação não verificados neste momento; jamais reutilizar os hashes da LT034.** Após o resultado do workflow, atualizar este mesmo Mestre com status real, URLs e digests, conservando os outros projetos e o histórico da main.


## 22/11/2026 — LT035 publicada: recibo pós-build e sincronização final
**Status confirmado diretamente no GitHub:** [pré-release v0.00.35-LT](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.35-LT), ID **454415624**, `prerelease=true`, publicada em `2026-11-22T11:58:51Z`. Merge funcional [PR #79](https://github.com/LucianoToscano/PGBR/pull/79), commit `e91b881f000fe7da9f57aed2cafabb1b4a9a1899`; registro pós-merge [PR #80](https://github.com/LucianoToscano/PGBR/pull/80), commit `13a7c91d329fc179bbedbe4e514b303958a7d389`. A execução [LT035 #45018980405](https://github.com/LucianoToscano/PGBR/actions/runs/45018980405) concluiu `success`, incluindo fixture nativa PvP, compilação APK, Root/Zygisk, verificação cruzada e upload de assets. Todas as releases anteriores, incluindo LT034, preservadas.

**Arquivos exatos publicados e SHA-256 declarados pela API de Releases (não reutilizados de versões anteriores):**

| Artefato | Bytes | SHA-256 |
| --- | ---: | --- |
| [PGBR-v0.00.35-LT.apk](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.35-LT/PGBR-v0.00.35-LT.apk) | 188822851 | `31f361c2b3284a329a6a82e1bf194d6d2dd8a9dd03e6485bccfe94ce6db5d3aa` |
| [PGBR-v0.00.35-LT-Root.apk](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.35-LT/PGBR-v0.00.35-LT-Root.apk) | 8348366 | `031e4dd2113027520df37fe97f172f7838c6defbf524d2b15b511cdf11645597` |
| [PGBR-v0.00.35-LT-Root-module.zip](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.35-LT/PGBR-v0.00.35-LT-Root-module.zip) | 8264186 | `0d972a46dc128b93c8dfab9b00b80676c3cb1adeb59d789a6d4c222a1918ff6e` |

Outros comprovantes publicados: [SHA256SUMS.txt](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.35-LT/SHA256SUMS.txt), [build-info.json](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.35-LT/build-info.json), [root-build-info.json](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.35-LT/root-build-info.json). Os três pacotes estão disponíveis nos links diretos registrados acima, sob o mesmo release.

**Escopo entregue em código:** observação de UI PvP após bootstrap tardio, aceitação do observador mesmo sem hooks de animação, expiração renovável exclusivamente por atualização VNext de sessão reconhecida, limite máximo de 20 minutos e proteção contra NPCs/resultados. **Limites inalterados:** não se extraiu nem se copiou rotinas literais PvP dos binários PGSharp 1.33.0, 1.50.3 e 1.278.0; a existência de UI nos vídeos não é prova de implementação compatível. `device_tested=false`; golpes rápidos/carregados, escudos e segundo golpe supereficaz ainda carecem de comprovação funcional em GBL e desafio no aparelho. A publicação é QA, não homologação de gameplay.


---

<!-- CHAT_PGSHARP_PVP_CROSSPROJECT_20261122 -->
## 22/11/2026 — Consolidação do chat: PGSharp 1.33/1.50/1.278, Auto PvP PGBR LT035 e projetos irmãos

**Fonte:** histórico desta conversa com a pesquisa, solicitações de copiar funcionalidades PvP, implantação própria PGBR, QA, merge, release e atualizações aos quatro mestres. **Escopo do registro:** somente documentação; não altera executores ou versões. Protocolo Global: conferir Mestre e GitHub HEAD, preservar trabalhos paralelos, branch/PR por tarefa, rastrear fonte e testes, jamais excluir histórico.

### 1. APKs e vídeos identificados

| Versão/material | Referência verificável | Limite de conclusão |
| --- | --- | --- |
| PGSharp 1.33.0 (2021) | [pgs1.33.0.apk no Drive](https://drive.google.com/file/d/1qvz4iYBRGfZZycazYagpxLaXMQqx2d48/view); [análise independente romainthomas/pgsharp](https://github.com/romainthomas/pgsharp) | Lua, símbolos e hooks históricos incluindo CombatDirector__Initialize, mas **nenhuma prova de Auto PvP** em 1.33. Repositório acadêmico ≠ fonte oficial completa do PGSharp. |
| PGSharp 1.50.3 (~2022) | [pgsharp_v1.50.3.apk](https://drive.google.com/file/d/1GuH3JkL77DYC7FfZujcEFxfy4rrafM32/view) | Linha 1.50 lançou **Raid & Gym Battle Assistant** para PvE; não se comprovou executor de GBL. |
| PGSharp 1.278.0, jogo 0.437.0 (2026) | [pgs1.278.0_0.437.0_GDvJI(arm64).apk](https://drive.google.com/file/d/13wvFdgVpnVHXsRUftJYjWiyQCzmJDsiU/view) | Strings e recursos de batalha catalogados anteriormente, sem diff binário completo validado neste chat. |
| Vídeo 13 | [Assistente Batalha PvP](https://drive.google.com/file/d/1O6OyTOvJcFcKps5Qn9tHgJoqsfU3tTXM/view), ~44,2 s na auditoria anterior | HUD de ataque ágil, carregado e escudo sobre GBL. |
| Vídeo 14 | [Assistente Batalha PvP](https://drive.google.com/file/d/1CuMTP0q8X2NSV_8wHmogbeGwTIMimz7n/view), ~76,6 s na auditoria anterior | HUD durante golpes, trocas e animações de escudo. |

**Ressalva técnica:** os vídeos foram analisados por relatório visual anterior, **não reproduzidos novamente** na sessão da implementação. O runtime não permitiu extração/diff real dos três APKs. Nenhuma função binária do PGSharp foi literalmente copiada ou atribuída a offsets verificados. A manifestação do responsável sobre autorização é **declaração do usuário**, não uma auditoria de direitos.

### 2. Alvos de inspeção e separação de escopos

Inventário forense pré-existente para 1.278 inclui `libmain.so`, `classes.dex` do plugin, `res/layout/hl_settings_object_pvpinfo.xml`, `res/layout/hl_context_panel.xml`, e strings `BATTLEASSIST`, `BREAD_BATTLEASSIST`, `rocketFlowIncident`, `battleAssistant`, `breadBattleFast`, `breadBattleCharge1`, `CombatVNextBattleStateShim`, `RaidBattleState`, `GymBattleState`, `pvpCombatStats`, `PVPIV`, `rocket.shield`. Presença de identificadores não comprova execução automática contra jogadores. `PVPIV` descreve rank/estatística; reides/ginásios, Rocket, Max e GBL possuem estados e políticas diferentes. No repositório histórico 1.33, buscas por `pvp` e `autobattle` não localizaram implementação; **ausência em pesquisa textual não significa prova de inexistência no binário**.

### 3. Código PGBR realmente implementado e liberado

- `pgrazor/native/cutscene_skip.cpp`: observa abertura de `GuiController.Open` em modo validado; bootstrap IL2CPP indisponível no primeiro tick agora pode ser reavaliado sem consumir permanentemente a tentativa; observador permanece útil quando nenhum hook opcional de pular cutscenes funciona.
- `pgrazor/native/pvp_scene.h` e `combat_host.cpp`: escopo PvP reconhecido por Hub → Preparação → CombatVNext; NPC/resolução cancelam; renovação somente sob chamadas válidas VNext com prazo de inatividade e máximo absoluto de 20 minutos; sair da fase COMBAT desarma a sessão.
- `pgrazor/native/pvp_scene_test.cpp` e `pgrazor/test/test_pvp_trace_bootstrap.py`: regressões para ordem de telas, NPC, timeout, relógio, bootstrap, observador sem hooks de animação.
- Políticas legadas, contratos LT032/33/34, `test_archiver_idempotency_policy.py`, `test_lt035_release_contract.py`, workflows antigos, README/CHANGELOG e Mestre foram ajustados para permitir LT035 **sem reiniciar releases históricas**.
- Permanecem limites identificados: flags e bitmask compartilhados Rocket/PvP; segundo golpe carregado mais eficaz `PREFER_SUPER` não validado no executor nativo; execução invocada não comprova toque aceito; escudo, troca e minigame ainda exigem instrumentação/ensaio físico. Não declarar essas pendências como resolvidas.

### 4. PRs, testes, merge, build e release

- [PR #76](https://github.com/LucianoToscano/PGBR/pull/76): correção inicial; CI 5/5 em `5f130d0f7bfbb2d075891ca13c17b8a16e82258e`; encerrada **sem merge** após substituição.
- [PR #78](https://github.com/LucianoToscano/PGBR/pull/78): tentativa intermediária; encerrada **sem merge**, por conflito com documentação paralela.
- [PR #79](https://github.com/LucianoToscano/PGBR/pull/79): integração sem perdas do código e QA LT035; CI 5/5; **merged** em `e91b881f000fe7da9f57aed2cafabb1b4a9a1899`.
- [PR #80](https://github.com/LucianoToscano/PGBR/pull/80): registro no Mestre pós-merge, **merged** em `13a7c91d329fc179bbedbe4e514b303958a7d389`.
- [PR #81](https://github.com/LucianoToscano/PGBR/pull/81): documentação de release verificada, README e SHA-256, CI 5/5; **merged** em `01111cad8f0ddf0e6f3cd16a63752334956777da`.
- [GitHub Actions LT035 #45018980405](https://github.com/LucianoToscano/PGBR/actions/runs/45018980405): **completed/success**, fixture PvP, build APK não Root, Root/Zygisk, assinatura/integração, checagem cruzada e publicação de arquivos.
- [Release v0.00.35-LT](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.35-LT): **pré-release QA** publicada em 22/11/2026 11:58:51 UTC, preservando LT034 e anteriores.
  - [PGBR sem Root](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.35-LT/PGBR-v0.00.35-LT.apk): 188.822.851 bytes; SHA-256 `31f361c2b3284a329a6a82e1bf194d6d2dd8a9dd03e6485bccfe94ce6db5d3aa`.
  - [Instalador Root](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.35-LT/PGBR-v0.00.35-LT-Root.apk): 8.348.366 bytes; SHA-256 `031e4dd2113027520df37fe97f172f7838c6defbf524d2b15b511cdf11645597`.
  - [Módulo Zygisk](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.35-LT/PGBR-v0.00.35-LT-Root-module.zip): 8.264.186 bytes; SHA-256 `0d972a46dc128b93c8dfab9b00b80676c3cb1adeb59d789a6d4c222a1918ff6e`.
  - [SHA256SUMS.txt](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.35-LT/SHA256SUMS.txt). Hashes provenientes da API de release e já registrados no Mestre; **não** homologação física.

### 5. Matriz de responsabilidades para projetos irmãos

| Projeto | Uso desta pesquisa | Não confundir |
| --- | --- | --- |
| **PGBR** | Observador PvP, executor nativo experimental, `PvpBattleController.java`, `PvpChargedPreference.java`, `rocket_combat.cpp`, `rocket_actions.cpp` | Políticas Java e APIs nativas são camadas diferentes. Segundo carregado, escudo e ações aceitas não foram validados em jogo real. |
| **PvPPokeGo** | Core desacoplado de OCR, energia, eficácia, preferências e sugestões/intenções; potencial contrato de integração com PGBR | Lógica de preferência/carregado 1/2 não equivale a executor já ligado ao PGBR ou ao jogo. |
| **PokeSearch** | Catálogos de espécies, PvP rank, tipos, sprites, referência de dados e biblioteca compartilhável | Nenhuma alteração em P1/P2, login, feeds, Auto Hunt ou release oficial LT409 foi realizada. |
| **PGSharp (repositório do usuário)** | Biblioteca de evidências, estudo comparativo e construtor próprio experimental | Não é o código oficial do fornecedor nem prova de código Auto PvP extraído; direitos de terceiros não são inferidos do nome do repositório. |

**Estado final verificável:** correções próprias PGBR, testes/CI, merge, três APKs e registro documental da LT035 **concluídos**. **Pendências explícitas:** diff binário real das três versões PGSharp; recuperação de funções exatas; reprodução nova dos dois MP4; comprovação de Auto PvP em GBL, seleção de segundo carregado, escudos, trocas, tolerância a mudanças de UI; **device_tested=false**. Esta consolidação documental não gera outra release nem resolve pendências de runtime.


---

## 22/11/2026 — Consolidação transversal deste chat: PGSharp, Sniper, assets e releases LT034/LT035

**Abrangência:** este complemento registra as descobertas sobre o PGSharp e sua biblioteca de referências, a implementação solicitada do Sniper 100 IV no PGBR, a opção Free/Standard com key opcional e o fechamento de merges/builds. Preserva integralmente o histórico anterior; é **registro técnico**, não nova release nem prova de execução no Samsung. Ler também as seções cronológicas de PRs #72–#80, que contêm o detalhamento e os SHA-256 já verificados.

### Fontes originais e auditoria visual
- [Pasta PGSharp no Drive](https://drive.google.com/drive/folders/1WAEmEpZmvUw9dDiapNcygtmZQq6XIB_j), [dossiê de 16 imagens e 20 vídeos](https://drive.google.com/file/d/11_OSo_6hPaXeRB5GW-HLXbVSr5HyugyO/view), [inventário CSV com IDs](https://drive.google.com/file/d/1pvXnzt41spOD82q1u1_i5ta4Xnm0a2SO/view), [biblioteca de recursos APK extraídos](https://drive.google.com/drive/folders/1HHNySaQo9w6NU3XuoQX0KBbTUy6ds46O), [auditoria PNG/joystick/efeitos](https://drive.google.com/file/d/1PjXJr87jK2F801_lz7A7x9OK0_Bhmyy9/view), [auditoria de menus/código/licenças](https://drive.google.com/file/d/1aKp7Is0q6yFenLdBsiG2D582T6D_ha5u/view).
- As **16 imagens** foram inspecionadas; os **20 vídeos** foram examinados por amostragem de quadros, sem auditoria exaustiva de todos os frames nem instrumentação nativa. Em vídeo 12, **Quick Sniper I e II** são distintos, verticais/horizontais com filtros de espécie, IV, nível, gênero, forma, distância, prioridade, notificação e guias Pokémon/Raid/Quest; vídeo 11: **Radar Próximos separado**. Vídeos 13–14: HUD externo PGSharp com **Ataques ágeis / Ataques carregados / Shield**, sem prova de autoescudo ou seleção automática de golpe supereficaz. Vídeos 9/10/16–18: joystick/arraste e toque no mapa; vídeo 15: ocultação visual do nome. Diferenciar UI visível de funcionalidade comprovada.
- A auditoria dos **14 APKs/ZIPs** reporta PGSharp completo **1.278.0** de 211.194.913 bytes, plugin interno com **3.777 entradas e 1.973 PNGs** (1.577 de Pokémon, 64 de itens, 332 Android). Catálogo dos menus contém **237 layouts, 369 `res/drawable`, 32 `res/anim` e 25 `res/animator`**; o ZIP anterior apenas de PNG **não continha os vetores/layouts binários XML**. A versão histórica 1.33.0, 1.277.2 e 1.278.0 têm os cinco PNGs do joystick idênticos: três contêm arte útil (`ui_pic_joystick_arrow.png` 588×588, `ui_pic_joystick_right_pad.png` 300×300, `ui_pic_joystick_control_ball_mini.png` 50×50) e **dois são placeholders 1×1** (`ui_pic_joystick_control_ball.png`, `ui_pic_joystick_left_pad.png`). O pulso ciano de toque dos vídeos **não foi provado** como PNG original isolado. Recursos Unity do jogo e reconstruções derivadas dos vídeos **não são automaticamente sprites originais do overlay PGSharp**. Não redistribuir arquivos sem avaliar os direitos envolvidos.
- Para código reusável: [PGRazor MIT](https://github.com/jpitty03/PGRazor) fornece estrutura aberta de UI/overlay, mas seus sprites de terceiros não estão incluídos na licença; [repositório próprio PGSharp](https://github.com/LucianoToscano/PGSharp) contém `CustomShortcutsModel`, `NativeMenuCoordinator`, `LiveFavoritesOps` e `FavoritesOverlay` como componentes próprios separados do cliente comercial. Não declarar biblioteca extraída ou `.plg.apk` embutido como instalador independente.

### Feed 100 IV e licença — implementação real e fronteiras
- O proprietário **já declarou autorização** de APKs/APIs PokeList, Data Hub, PGTools, Pokemod e PGSharp; o Guia Mestre PokeSearch (LT364/LT365) registra que não se deve solicitar repetição. Verificar apenas **protocolo, credencial, escopo e disponibilidade técnica** concretos de cada integração; não assumir endpoint documentado apenas porque o produto oferece uma função gratuita.
- PGSharp descreve oficialmente **100 IV Feed como Free** e **All Coordinates Feed como Standard**. A análise estática da 1.278.0 reportou `f.pgsharp.com/peanuts`, `q4.d(...)`, `q4.e(...)`, `FASTSNIPE`, `FASTSNIPE2`, `PL.NRL/NSMTC/PUL` e `post.auth.lic`. A origem/headers/schema da chamada gratuita e a verificação de key **não foram validadas**. Qualquer cliente PGSharp depende de contrato/protocolo verificável; não inventar sessão ou parâmetros.
- [PR #72](https://github.com/LucianoToscano/PGBR/pull/72), merge `dd70a3256fc17b25b176476a60131457ba632749`: `HundoFeedData.java` e `HundoFeedDialog.java`, parser do `hundos.json` de **FaustusFoundry/PGBR** já existente, filtros locais, sprite, TTL, descarte de duplicatas/inválidos, copiar coordenadas e teleporte **manual confirmado**, sem captura automática. **Não é a mesma fonte do PGSharp.**
- [PR #73](https://github.com/LucianoToscano/PGBR/pull/73), merge `bf1c93e43fe23a4aa6fd9934529303e22b8642c8`: sexta opção PGSharp Free/Standard no menu, `PgSharpFeedPolicy.java`, `PgSharpFeedDialog.java` e `PgSharpLicenseVault.java` (key opcional criptografada AES-GCM/AndroidKeyStore, no-backup, sem transmissão; armazenada ≠ verificada). **Não existe login PGSharp remoto nem feed PGSharp integrado na LT034/LT035**.
- [PR #74](https://github.com/LucianoToscano/PGBR/pull/74): **pré-release v0.00.34-LT** com APK sem Root, instalador Root e módulo Zygisk, [Actions #45013312358](https://github.com/LucianoToscano/PGBR/actions/runs/45013312358) verde; tag aponta `156a34d72f3f969a5b5bda878dcb548a7fed52e4`. A falha inicial de contrato de README foi corrigida e retestada. [PR #75](https://github.com/LucianoToscano/PGBR/pull/75) e [PR #77](https://github.com/LucianoToscano/PGBR/pull/77) encerraram o registro documental. URLs, bytes e hashes LT034 constam na tabela imediatamente anterior deste Mestre.
- **Estado posterior e mais recente verificado:** [LT035 pré-release](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.35-LT), ID `454415624`, publicada **22/11/2026 11:58:51 UTC**, após [PR #79](https://github.com/LucianoToscano/PGBR/pull/79) (observação/ciclo de vida PvP). LT035 **não homologa** autoescudos, ataques rápidos/carregados, segundo ataque supereficaz, login PGSharp ou feed privado; `device_tested=false`. Os três downloads e hashes únicos da LT035 estão no registro cronológico desta mesma página. **Não retroceder para LT034 ao dizer “versão mais recente”.**

### Diretrizes de continuidade sem perdas
Manter Quick Snipers I/II, Radar e fonte PGBR como conceitos separados; preservar telas/ativos de menu, joystick, radar, cooldown, privacidade somente visual, permissões e preferências pré-existentes. Em avanços futuros, anotar versão/ABI/hash do binário, chamada de rede documentada, provenance, TTL/despawn, método de resultado e teste Android; distinguir **observado**, **estático/descompilado**, **implementado**, **CI passou**, **release publicada** e **funcionou fisicamente**. Não colocar keys/tokens no GitHub ou Drive nem anunciar APK funcional além das evidências. Este complemento modifica apenas a documentação canônica.


## 22/11/2026 — LT036: relatos do usuário, auditoria e patch experimental de treinamento

**Capturas anexadas nesta conversa:** o sistema Android exibiu `App não instalado` ao tentar abrir `PGBR-v0.00.35-LT.apk` (189 MB), rotulado pelo Android como `Pokémon GO`; outra captura mostra batalha de treinamento com Lickilicky (CP 1500) contra Sneasel (CP 1465), sem ataques automáticos observáveis. As fotos não comprovam que PGBR estava instalado durante o treino — a primeira mostra a falha de instalação.

**Causa de instalação comprovada em fonte:** `pgrazor/nonroot.py` mantém `com.nianticlabs.pokemongo` e re-assina com a chave do PGBR. Outra instalação do mesmo pacote com certificado diferente pode provocar `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, mas o código preciso ainda exige ADB. A variante Root usa instalador de pacote `dev.pgrazor.root` e módulo Zygisk, exige ambiente e versão de jogo compatíveis; não substituir o app oficial nem apagar dados do telefone automaticamente. Guia: [QA_LT036_TREINAMENTO_INSTALACAO.md](docs/QA_LT036_TREINAMENTO_INSTALACAO.md).

**Problema de fonte no treinamento:** `pvp_scene.h` descartava `NpcTrainerInterstitialGui`; a mesma classe aparece em diálogos de Mateo e não prova uma batalha. No patch da branch `fix/lt036-training-shared-battle-20261122` (base `f8ad55bb186ad6321f687cef50a037964d051d96`), a GUI NPC exata só vira um candidato de **45 segundos** e precisa de `CombatPreparationRootGuiController` e callback VNext real para autorizar o motor nativo compartilhado ágil/carregado/escudo. Rocket mantém precedência e cancela até candidatos sem UI capturada; `pvpExecutorReady` indica `mode=training` na sessão apropriada. Não há ativação cega em diálogos de NPC ou em raid/gym/Max.

**Código e testes preparados:** `pgrazor/native/pvp_scene.h`, `pgrazor/native/pvp_scene_test.cpp`, `pgrazor/native/combat_host.cpp`, `pgrazor/src/dev/pgrazor/Control.java`, `pgrazor/src/dev/pgrazor/SettingsPanel.java`, `pgrazor/test/test_lt036_training_scope.py`. Preservados os três bits de preferências Rocket/PvP, anti-reentrância, leases, segurança em Rocket e as releases históricas. CI, merge, APK e assinatura **ainda não haviam sido verificados** no momento deste registro inicial.

**Escopo e evidência:** esta correção não demonstra ataque aceito em Pokémon GO nem resolve o impedimento de assinatura para instalar sobre clientes existentes; precisa de testes físicos separados de ágil/carregado/escudo no treinador, desafios/GBL e diagnóstico de ADB. Sem cópia literal de funções PGSharp e sem second-charged supereficaz homologado. `device_tested=false`.


### 22/11/2026 — Preparação da pré-release LT036 e preservação dos históricos

**Origem:** fotos de “App não instalado” e do combate Lickilicky/Sneasel no treinamento; motivo documentado em [QA_LT036_TREINAMENTO_INSTALACAO.md](docs/QA_LT036_TREINAMENTO_INSTALACAO.md). A nova `0.00.36-LT` contém candidato explícito de treinamento validado por NPC → preparação → VNext, usando a bitmask fast/charged/shield já compartilhada com Rocket e PvP. A mudança **não** garante ativação de toda espécie de batalha nem corrige o conflito de certificados do mesmo packageId; telas isoladas de NPC/Mateo nunca devem atacar.

**Documentação e QA:** `docs/releases/v0.00.36-LT.md`, `pgrazor/VERSION`, `.github/workflows/pgbr-lt036-release.yml`, `test_lt036_release_contract.py` e `test_lt036_training_scope.py`. Contratos de LT032/33/34/35 passam a tolerar LT036 sem apagar verificações históricas. O workflow LT035 publicado deixa de ter gatilhos automáticos por alteração de VERSION e de seu próprio YAML, conservando o despacho manual e **todos os assets antigos**. README/CHANGELOG atualizados de forma aditiva.

**Evidência/pendências:** alterações em código ainda aguardam verificação CI, branch isolada `fix/lt036-training-shared-battle-20261122`, sem merge automático na main antes da conclusão. A futura tag LT036, hashes e APKs exigem prova real após publicação; **não informar links hipotéticos como downloads válidos**. Status `device_tested=false`, validação de ágil/carregado/escudo e diagnóstico ADB pendentes. Sem cópia de executáveis PGSharp, credenciais ou alterações de outros projetos.


**Validação CI #45023799721, primeiro teste:** quatro dos cinco checks passaram (host C++, NDK ARM64, website e governança). A suíte Windows falhou em **2 de 284 testes**, ambos documentais: `test_branding.test_main_readme` exigia o nome literal `PGBR-v0.00.36-LT.apk` no README; `test_lt036_release_contract.test_release_version_and_assets` interpretava UTF-8 das notas com encoding implícito Windows, causando falso negativo de “App não instalado”. O patch seguinte explicitou o nome **não publicado** no README e fixou `encoding='utf-8'` na leitura do contrato. **Novo CI e release precisam ser verificados; nenhum problema nativo foi apontado nos quatro jobs aprovados.**


## 22/11/2026 — LT036 publicada: confirmação após CI e hashes reais

Após [workflow #45024489012](https://github.com/LucianoToscano/PGBR/actions/runs/45024489012) concluído com `completed/success`, a [pré-release LT036](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.36-LT) foi confirmada na API GitHub:
- [APK sem Root LT036](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.36-LT/PGBR-v0.00.36-LT.apk): **188.822.851 bytes**, SHA-256 `453c5e47a00b022750150f3b9bcf6dc2c06032b285976de5b05d2ebc9c3d1ea3`.
- [APK instalador Root LT036](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.36-LT/PGBR-v0.00.36-LT-Root.apk): **8.348.366 bytes**, SHA-256 `a7ca62a91b4050accc8893fdaad22ec9e499a4405bdce98b1ada491d2d1a4e8a`.
- [Módulo Zygisk LT036](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.36-LT/PGBR-v0.00.36-LT-Root-module.zip): **8.264.501 bytes**, SHA-256 `a7568c523aee06f45a3059dfa230fe2d41ae6b60180c100df74887f6b0449d3f`.
- [SHA256SUMS LT036](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.36-LT/SHA256SUMS.txt), hash do arquivo `cddb0e3080276f135ec728f17420f121da080ec75d9da354b425450de9d26ffe`.

Os hashes são os `digest` verificados dos anexos na API de Releases, e **não** validam instalação, funcionamento ou aceitação dos ataques. A LT036 permanece preservada.

## 22/11/2026 — Especificação refinada LT037: Rocket como único motor em batalhas permitidas

**Requisito exato do proprietário:** ativar e desativar automaticamente as três ações já disponíveis no motor Rocket (Ágil, Carregado, Shield) ao identificar **Recrutas/Arlo/Cliff/Sierra/Giovanni**, PvP contra jogadores, desafios privados entre amigos, treinadores NPC e treinamento. Não alterar comportamento de Max, reides ou encontros sem treinador; ginásios só se usassem o mesmo executor, caso contrário manter o seu motor. **Lista permitida, e modo desconhecido = desarmado.** Evitar alternâncias manuais por batalha. As preferências individuais 1/2/4 persistentes continuam sob controle do usuário; a autorização de execução é transitória por escopo.

**Auditoria de código:** `pgrazor/native/rocket_combat.cpp` (motor Rocket, feature 21) executa ações Ágil, Carregado, Escudo, já reutilizado em PvP no hook `PGR_RC_OBS_PVP_SCOPE_ENTER`. `pgrazor/native/battles.cpp` (feature 19) tem executor **separado** para Reide/Ginásio/Max e esquivas; **recomendação: ginásios NÃO entram na allowlist Rocket**, porque seu motor próprio já existe e a batalha pode ser apenas contra Pokémon defensor, sem treinador. O modo Rocket original permanece sob `InvasionCombatState`; não é permitido falsificar tipo de oponente no jogo nem abrir hooks gerais.

**Alteração própria LT037 em branch `feat/lt037-rocket-assist-allowlist-20261122`**, derivada da `main` após merge da LT036 (base `8715c5c584a048f60a606cc47885c08f75c06e78`):
- Novo `pgrazor/native/battle_target_policy.h`: classificação explícita Rocket/PvP/Treinador NPC = permitido; Raid/Gym/Max/Wild/Unknown = negado. Teste independente `battle_target_policy_test.cpp` prova oito categorias, preferências desligadas, escopo inválido e lease vencida.
- `pvp_scene.h` mantém Hub→Preparação→VNext para PvP e NPC interstitial→Preparação→VNext para treinamento, mas agora suporta também identificação **tipada** de PvP via `CombatDirectorV2`/`CombatTypeExtensions.IsPvPCombat` (`pvp_probe.inc`) para desafios diretos; candidato dura **15 s** e exige VNext real. Se essa classe/método não estiver validado na versão, não há liberação alternativa.
- `combat_host.cpp` reutiliza o **mesmo** `g_rocket_combat`, sem modificar o motor Rocket original, usando máscara de ações e lease de **≤2.500 ms** vigentes; nega ou desarma imediatamente ao surgir escopo PvE tipado de `g_battles`, em resultado, fechamento de VNext (inclusive sem executor já armado), Rocket priorizado e parada. Sem classificador independente de PvE, a rota não Rocket também nega. Não há alteração da ABI nativa.
- Telemetria `pvpExecutorReady`, `pvpAction` e eventos de scope recebem `mode=pvp` ou `mode=training`; os valores são **diagnóstico de chamada**, não confirmação de aceitação pelo servidor.
- `Control.java` apresenta status distinto dos modos. Workflow e contratos dedicados `v0.00.37-LT`, preservando os anteriores; documentação [notas LT037](docs/releases/v0.00.37-LT.md), README e CHANGELOG aditivos.

**Riscos/limites transparentes:** a função `IsPvPCombat` foi previamente validada estaticamente no código da versão alvo, mas não foi testemunhada no aparelho do usuário para todos os desafios entre amigos. O interstitial NPC é compartilhado com diálogos; sem preparação e VNext nunca ativar. Se alguma rota de NPC usa classes distintas, ficará desligada até evidência. Execução nativa `invoked=true` não prova que o jogo aceitou golpes, carregado, escudo ou causou dano. O segundo ataque por superefetividade permanece pendente. `device_tested=false`; produção do APK LT037, merge e QA somente após CI, e hashes reais apenas após release publicada. Não tocar no PokeSearch, PvPPokeGo ou em releases anteriores.


<!-- AUTO_BATTLE_TRAINING_LT037_20261122_RECEIPT -->
## 22/11/2026 — LT037 publicada: Auto Batalha Rocket compartilhado em PvP, amigos e Treinamento

**Pedido e evidência da captura:** o usuário mostrou batalha contra treinador (Lickilicky PC 1500 contra Sneasel PC 1465), relatando que o assistente não atacava, mesmo com botões ágil/carregado/escudo; a imagem, sozinha, **não prova qual APK/módulo estava ativo**. Preservar a falha como relato real, não declarar que a função foi homologada pela captura.

**Requisito:** manter um único executor existente de Rocket para Recrutas, Arlo, Cliff, Sierra, Giovanni; ativar/desativar automaticamente (conforme três opções que o usuário salvou) também em PvP GBL, desafios privados e treinamento/NPC classificados; nunca acionar para raids, Max, selvagens ou tipo desconhecido. Ginásios: **não adicionar à allowlist Rocket**, pois têm executor próprio em `pgrazor/native/battles.cpp` para reide/ginásio/Max. Nada de falsificar identidade do adversário no servidor; classificação apenas no app e encerramento automático por escopo.

**Código realmente integrado em `main`:** [PR #87](https://github.com/LucianoToscano/PGBR/pull/87), merge `7d723b9ad7e483bdb436571054095848cebcf64a`, sobre LT036 já preservada. `pgrazor/native/battle_target_policy.h` nega RAID/GYM/MAX/WILD/UNKNOWN; `rocket_combat.cpp` permanece motor de ágil/carregado/escudo reutilizado. `combat_host.cpp` aceita observação explícita `PGR_RC_OBS_PVP_SCOPE_ENTER` sob máscara 1/2/4 e lease de até 2500 ms, sem alterar ABI; `pvp_scene.h` exige Hub→Preparação→VNext ou `CombatTypeExtensions.IsPvPCombat` tipado + VNext para PvP privado; NPC `NpcTrainerInterstitialGui` (45 s)→Preparação→VNext para treinamento. Diálogo Mateo sozinho não ativa combate. Rocket tipado tem precedência. Escopo PvE, fechamento, resultado, interrupção ou lease inválida desarmam o executor.

**Auditoria específica da captura:** o primeiro carregado presente na UI não significa que está pronto; o `rocket_combat.cpp` usa `vm_charged`, `vm_visible` e `m_interactable` para elegibilidade e `vf_gesture` para Ágil em VNext. O código possui sinais `pvpExecutorReady` / `pvpAction` com `mode=training|pvp`, porém `invoked=true` demonstra tentativa local, **não** dano nem aceitação pelo jogo. Sem log de evento e sem teste físico não é possível afirmar se a falha original era classificação de UI, máscara/lease, instalação, versão de metadados ou gesto nativo rejeitado. Não inventar causa.
`Control.java` sincroniza `rocketOptions` com `pvpBattleOptions` (bits 1,2,4), respeitando as preferências do usuário. `pvp_scene_test.cpp`, `battle_target_policy_test.cpp` e demais contratos validam ordem, modos permitidos/excluídos, TTL/lease, prioridade Rocket e desarme. A pré-release NÃO inclui escolha do segundo golpe mais supereficaz comprovada em batalha real.

**CI e release confirmados:** [Actions #45028477340](https://github.com/LucianoToscano/PGBR/actions/runs/45028477340) `completed/success` (fixture nativa, testes, aplicação completa e três variantes Android). [Pré-release QA v0.00.37-LT](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.37-LT), `published_at=2026-11-22T13:45:20Z`, `device_tested=false`:

- [PGBR-v0.00.37-LT.apk](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.37-LT/PGBR-v0.00.37-LT.apk) — 188822851 bytes; SHA-256: `1df76ebdfaa9c4d72d203c61f4f317f5b08af5970bd6271c9d3d9b425e4d4a88`.
- [PGBR-v0.00.37-LT-Root.apk](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.37-LT/PGBR-v0.00.37-LT-Root.apk) — 8348366 bytes; SHA-256: `e2b594a21738a81eb4bc99858a6ad8f6ed094539db8dee69ae2cfb43b9d678c9`.
- [PGBR-v0.00.37-LT-Root-module.zip](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.37-LT/PGBR-v0.00.37-LT-Root-module.zip) — 8266003 bytes; SHA-256: `33d9611fcdd46c31272ddd488d4e04a24f11ef7857b8f741a2613da28f1b15dd`.
- [SHA256SUMS.txt](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.37-LT/SHA256SUMS.txt) — 281 bytes; SHA-256: `c8bec1cb23f71828a52c36dee871e338a1550852ad548a8bb7b40348bf2c7021`.

**Instalação:** o APK não Root continua com o packageId original do Pokémon GO re-assinado pela chave do PGBR; assinatura diferente no aparelho pode causar `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. Só logs ADB autorizados podem confirmar o código exato; não desinstalar/apagar dados automaticamente. Instalador Root usa pacote próprio e exige Zygisk/versão compatível.

**Estado e próximos testes necessários (não concluídos):** instalar/ativar de forma verificável a LT037, identificar `pvpExecutorReady=true`, `mode=training`, `pvpAction=quick|charged|shield` e observar dano e consumo de escudo no treino, GBL e desafio privado; checar trocas, prompt, timeout e que raid/Max/ginásio jamais adotam motor Rocket. Se o caminho de treino real não emitir as três telas tipadas esperadas, manter OFF e coletar trace de nomes de classes, não liberar VNext genérico. Separar `PREFER_SUPER` para carregado 1/2 somente após validação dos movimentos e energia. **Nenhum teste físico foi executado nesta conversa: `device_tested=false`.**

**Referências cruzadas:** atualizar os mestres PokeSearch, PvPPokeGo e PGSharp com o resultado comprovado, sem alterar os três apps, direitos de terceiros, APKs ou dados. Preservar LT035/LT036 e trabalhos paralelos; esta PR modifica somente documentação.


### 22/11/2026 — Trava pós-publicação da release imutável LT037
Após merge da [PR #88](https://github.com/LucianoToscano/PGBR/pull/88) (recibo de release e README), a alteração em `docs/releases/v0.00.37-LT.md` acionou desnecessariamente outra execução do workflow histórico LT037 ([Actions #45029579609](https://github.com/LucianoToscano/PGBR/actions/runs/45029579609)), embora o código de publicação já use `gh release view` para não sobrescrever anexos. **Correção de governança:** a versão 0.00.37-LT publicada só pode ser invocada por `workflow_dispatch` manual para reprodução; removido o gatilho `on.push` dessa tag imutável, sem excluir nenhum passo, recurso ou arquivo. O teste `test_lt037_release_contract.py` verifica a regra. O segundo workflow existente continua sendo um evento histórico e pode terminar com sucesso ou falha sem alterar os hashes oficiais; não declarar sua conclusão antes de consultar o GitHub. Esta mudança **não gera novo APK nem altera a release existente**.


## 22/11/2026 — Preferência carregada: super eficaz > neutro > pouco eficaz

**Requisito confirmado após o usuário comprovar na LT037 que Ágil, Carregado e Escudo funcionam no seu aparelho:** escolher o golpe carregado pela eficácia e não pela ordem. Super eficaz (>1) precede neutro (=1), que precede pouco eficaz (<1), inclusive quando o segundo botão é neutro e o primeiro é resistido. Empates super eficazes usam valor de dano/energia; entre dois resistidos prefere o menos resistido; neutro/neutro preserva o primeiro pronto. Espera por golpe melhor apenas em condições seguras, até o limite de 5,5s do controlador.

**Implementado nesta branch apenas na camada de política Java:** `PvpChargedPreference.java` e seus testes `PvpChargedPreferenceTest.java`, com casos de botão 2 neutro, ordens invertidas, resistência dupla, espera limitada e HP baixo. O relato do usuário é comprovação física **dos três comandos da LT037 no cenário testado**, mas **não** é comprovação desta priorização nova nem de todas as modalidades.

**Pendência decisiva:** o executor nativo atual `rocket_combat.cpp` chama o primeiro botão carregado pronto em vNext e em legacy, e a camada `PvpBattleController.java` é política pura sem adaptador de observação e comando ligado a essa função. `smart_battle.cpp` conhece Pokémon/tipos e Game Master em seus contextos, mas não entrega tipos, movimentos, energia e correspondência de botões verificadas a esse executor em PvP/Rocket/Treino/Raid/Ginásio/Max. **Esta alteração Java NÃO muda o golpe efetivamente disparado no APK**; requer ponte nativa comprovada e QA físico antes de nova release funcional. Não modificar agora a LT037 que o usuário confirmou funcionar, nem declarar APK novo ou integração cross-mode como concluídos.


### 22/11/2026 — Comprovante pós-merge da preferência eficaz (PR #91)
[PR #91](https://github.com/LucianoToscano/PGBR/pull/91) **merged** após **cinco checks CI `success`**, incluindo `check.py`, testes Java (controle e preferência), políticas C++ nativas, NDK ARM64, site e governança. Merge `2d0036140e2aa6e4d5843677c738e4641c1c72cc`, commits de feature `d65d4a650d0a820b3e4b62bd925a353e1c8eb84e` e testes adicionais `6e46efaad9ad84bb453da9d6501391125beea493`.

**Entregue:** alteração da política de decisão Java pura e testes determinísticos para priorizar super eficaz > neutro > pouco eficaz, inclusive ataque 2 neutro contra ataque 1 pouco eficaz, resistência dupla e desistência segura da espera. **Não entregue:** leitura tipada de movimentos e adversário em todas as modalidades, mapeamento confiável de slots de UI, passagem dessa decisão ao motor nativo Rocket/VNext e ao motor independente Raid/Gym/Max, confirmação de toques em jogo, APK da nova regra. A release publicada continua **v0.00.37-LT**, cuja eficácia dos três comandos (Ágil, Carregado, Escudo) foi relatada pelo usuário em seu teste, mas **a seleção automática de golpes por eficácia ainda não foi testada nem habilitada no app**. Não incrementar `pgrazor/VERSION` ou acionar release até a ponte nativa e seu QA. Os dados de tipos e golpes existentes em `smart_battle.cpp` são uma fonte potencial; devem ter proveniência, geração de batalha e relação 1:1 com o botão carregado comprovadas antes de serem consumidos em produção.


## 22/11/2026 — Preferência global Ataque Inteligente e menu de Batalhas

Usuário pediu ordem de switches: **Batalha Inteligente**, **Ataque Inteligente**, **Cura Inteligente**. Uma única chave Ataque Inteligente (ON/OFF), para priorizar carregados Super eficaz > Neutro > Pouco eficaz futuramente em Rocket, PvP, amigos, Treinamento/NPC, Ginásios, Raides e Max, em vez de selecionar o primeiro pronto. **Implementado nesta PR:** toggle persistido `smartAttack`, migração do antigo bit `PREFER_SUPER` de PvP (sem alterar bits de Ágil/Carregado/Escudo), remoção de checkbox duplicado PvP, posição na seção 'Todas as batalhas' e traduções PT-BR. Status do menu deixa explícita a limitação. Batalha Inteligente já seleciona equipe Ginásios/Raides/Rocket e troca Ginásios/Rocket; não se afirmou suporte a todas modalidades. Cura Inteligente usa itens após batalha ou no mapa, nunca durante combate.

**Não implementado ainda:** detecção confiável do texto de eficácia que o próprio jogo exibe sob golpes, vínculo de tipos/movimentos com os dois botões da UI e decisão no executor nativo Rocket/GBL/treino/raid/gym/Max. As strings visuais têm diferentes contextos/idiomas e não podem ser confundidas com telemetria tipada confirmada. A nova chave **salva intenção**, não altera por si só o disparo nativo; `device_tested=false` para Ataque Inteligente universal. Preservar LT037 comprovada pelo usuário no Ágil, Carregado e Shield. O ataque carregado deve permanecer instantâneo. Outras necessidades registradas: prazo manual maior antes de trocar Pokémon/usar escudo e investigar tela branca durante minijogo (hipótese: escala do colisor do `rocket_minigame.cpp`, sem prova). Nenhuma release preparada neste registro.


### 22/11/2026 — Recibo pós-merge: menu Ataque Inteligente e tempo manual
**Integrações reais:** [PR #94](https://github.com/LucianoToscano/PGBR/pull/94) **merged** em `9e0721bfcebeadfdc45385206d2d4ac75315bf82`; [PR #93](https://github.com/LucianoToscano/PGBR/pull/93) **merged** em `c4645f0dc8ab39521300e7c4a514b84c66678041`. Ambas passaram por 5/5 verificações GitHub Actions. Não houve merge forçado nem perda das linhas paralelas; os arquivos alterados eram disjuntos.

**Entregas em código:** Menu `Batalha Inteligente → Ataque Inteligente → Cura Inteligente` com **uma única** preferência `smartAttack` persistida e migrada do `PREFER_SUPER` PvP. Tradução sincronizada para ambos aplicativos Android, não reintroduzido checkbox PvP duplicado. O motor nativo de troca `rocket_swap.cpp` aguarda **2300 ms** após o prompt, dando preferência à escolha humana; `rocket_combat.cpp` aguarda **1700 ms** no prompt de escudo, cancela ao fechar ou usar manualmente. `rocket_combat_test.cpp` ampliado para 33 cenários. O disparo ágil e carregado **não foi atrasado** nem recodificado.

**Correções em CI:** o primeiro teste de UI falhou por assert legado que exigia o checkbox já removido; o glossário Root precisava da mesma atualização que o não Root. A PR #93 atualizou `test_pvp_settings_ui.py` para o novo contrato e o gerador `RootPtBr.java` com a nova chave; segunda execução terminou 5/5 verde. A PR #94 terminou 5/5 verde. CI demonstra política e compilação, **não comportamento em aparelho** dos novos timers.

**Escopo e limitações críticos:** `smartAttack` **é uma preferência configurável**, e ainda não seleciona o botão de golpe de melhor eficácia no runtime. A rotina `PvpChargedPreference.java` sabe classificar super eficaz / neutro / pouco eficaz, mas `rocket_combat.cpp` e `battles.cpp` ainda não recebem os dois movimentos do Pokémon ativo vinculados aos botões Unity e o tipo do oponente com geração/timestamp confiáveis. O texto exibido pelo jogo sob cada golpe é um possível indicador, **não um getter/telemetria validada disponível no PGBR**. O suporte de Batalha Inteligente para troca/equipe é comprovado pelo código para modos limitados (Ginásios/Rocket e seleção antes de Raides); não se prometeu habilitação universal de PvP, Treino e Max. Cura Inteligente só gasta poções/revives após batalha ou no mapa, não durante luta. O usuário confirmou fisicamente os três comandos (Ágil, Carregado, Escudo) da LT037 **antes desta mudança**, mas ainda não homologou os novos atrasos nem a seleção por eficácia em todas modalidades.

**Tela branca:** a hipótese de origem está em `rocket_minigame.cpp` que amplia escala de colisor 1000x, mas sem comparação com o jogo/colisor e captura não foi possível confirmar causalidade; nenhuma mudança de brilho, shader, renderer ou efeito foi feita para não destruir o minijogo de Excelente. Proposta futura: audit visual controlada com e sem coletor e indicador discreto não bloqueante.

**Status de publicação:** este conjunto foi mesclado em código, **sem nova release ou APK publicados**. Última pré-release de APK confirmada `v0.00.37-LT` permanece intacta (a tag utilitária `build-inputs` não é um APK instalável). `device_tested=false` para o trabalho atual. Qualquer promessa de Ataque Inteligente universal exige integração Unity tipada, associação estável de ataque→botão, testes por modo e QA físico antes de tag nova.


### 22/11/2026 — Recibo final: trocar manualmente antes da Batalha Inteligente
[PR #96](https://github.com/LucianoToscano/PGBR/pull/96) **merged** em `04797c6d8f861a4012026b02c8a9649922747831`, após **5/5 checks CI aprovados**; commits de implementação `641812e202b28b704d29dafc688ca619ca2de3bd`, refinamento/manual picker `83e5a77b2633b51d0c966baf8fb484cc3c834a1f` e correção do teste `86d74705c56f5c11e571f3cca41a49384c32d671`.

**Motivo:** a primeira correção `rocket_swap.cpp` passou a esperar 2300ms para selecionar o próximo Pokémon, mas `smart_battle.cpp` ainda podia abrir uma troca antes após só 1200ms quando Batalha Inteligente estava ON. **Conserto:** o temporizador `kSettleMs` do Smart Battle foi alinhado a 2300ms (Gym/Rocket) e agora a automação **recusa sobrescrever o seletor já aberto manualmente** por `get_IsOpen`. O tempo do escudo continua 1700ms e **nenhum ataque carregado foi atrasado**. `test_manual_battle_choice_grace.py` registra contrato de constantes, precedência humana e ausência de mudança no caminho de clique carregado. O CI inicial detectou apenas assert com variável escrita `now_shield`, corrigido para `shield_now`; após nova execução os cinco checks passaram.

**Estado real:** código, testes e merges concluídos, `device_tested=false` para novos atrasos, troca manual e UI. A opção Ataque Inteligente continua *apenas preferência armazenada*, sem identificação tipada dos textos “Super eficaz”/“Pouco eficaz” e sem escolha nativa implementada nos seis modos. A tela branca do coletor carregado permanece sob investigação (sem mudança em colisor/renderização). Nenhum novo APK foi publicado por este conjunto de alterações. A última pré-release APK para instalar e comparar continua [PGBR LT037](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.37-LT), que **não contém** o novo menu e atrasos. Não confundir build aprovado com homologação de gameplay nem enviar o APK antigo como atualizado.


## 22/11/2026 — Candidato de build LT038: recuperação do chat anterior sobre Ataque Inteligente

**Commit-base verificado:** `1dfd83d04f04b4ed894d44e823a2a0586dc548c6`. As PRs [#93](https://github.com/LucianoToscano/PGBR/pull/93), [#94](https://github.com/LucianoToscano/PGBR/pull/94), [#95](https://github.com/LucianoToscano/PGBR/pull/95), [#96](https://github.com/LucianoToscano/PGBR/pull/96) e [#97](https://github.com/LucianoToscano/PGBR/pull/97) já estavam **merged na main** quando esta revisão começou; não replicar branches antigas. A última release publicada era [LT037](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.37-LT). Pede-se uma única opção Ataque Inteligente sob Batalha Inteligente, com Cura Inteligente em seguida, aplicável futuramente a PvP, Rocket, treino, amigos, ginásios, raides e Max.

**Código que já existe:** `SettingsPanel.java` exibe os três interruptores na ordem; `Control.java` persiste `smartAttack` e migra `PREFER_SUPER` legado; `rocket_swap.cpp` e `smart_battle.cpp` concedem 2300 ms de escolha humana; `rocket_combat.cpp` concede 1700 ms de escolha humana de escudo; o ataque carregado permanece imediato. `healConfig`/cura só age no mapa ou pós-batalha. Suíte e testes fonte das PRs anteriores aprovados.

**PENDÊNCIAS técnicas explícitas, NÃO chamar de prontas:** a chave `smartAttack` não altera a seleção do carregado no código nativo, que ainda escolhe o primeiro disponível. `PvpChargedPreference.java` contém tier Super eficaz > Neutro > Pouco eficaz, mas depende de observar movimentos, tipos do adversário e seu vínculo com os botões em cada modo, com proveniência e expiração. `smart_battle.cpp` possui dados de oponente/espécie para seleção de Pokémon, não um mapeamento de UI carregada. Modos Raide/Ginásio/Max têm executor separado `battles.cpp` e regras independentes; não falsificar escopo Rocket para eles. Texto visual de eficácia sob cada botão não foi obtido de API IL2CPP verificada. A tela branca pode ser efeito colateral da escala 1000x do colisor do `rocket_minigame.cpp`, hipótese não comprovada; não reduzir escala às cegas e arriscar Excelente.

**Candidato de entrega:** versão `0.00.38-LT`, novo workflow `.github/workflows/pgbr-lt038-release.yml`, notas `docs/releases/v0.00.38-LT.md`, contratos históricos e novo teste `test_lt038_release_contract.py`. Empacotar apenas menu e tempos funcionais em **QA** sem alegar Ataque Inteligente universal; marcar `device_tested=false`. Nenhuma publicação ou hash afirmado antes do resultado real de CI. Após release, registrar URL/hash verificados e preservar os assets de LT037.

### 22/11/2026 — LT038 publicada: recibo pós-merge, hashes e imutabilidade
A [PR #98](https://github.com/LucianoToscano/PGBR/pull/98) foi **merged**, commit `4ca4c48d417cc4218915db7d763fd360f2c0fc85`, após **5/5 checks CI aprovados** no commit `28c4719554ba4f2ac461b1c225fee464e10ce10a`. O primeiro CI da PR encontrou apenas um erro de contrato em README por ausência do nome do artefato exato `PGBR-v0.00.38-LT.apk`; corrigido sem declarar publicação antecipada. Release QA [v0.00.38-LT](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.38-LT) publicada em `2026-11-22T15:46:48Z`, workflow [Actions #45039119340](https://github.com/LucianoToscano/PGBR/actions/runs/45039119340) **completed/success**, com fixture PvP, testes, APK sem Root, instalador Root, Zygisk, verificação cruzada e upload dos seis assets.

**Anexos, tamanhos e hashes reais da API GitHub Releases:**

| Asset | Bytes | SHA-256 |
|---|---:|---|
| [PGBR-v0.00.38-LT.apk](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.38-LT/PGBR-v0.00.38-LT.apk) | 188822851 | `cdea0e27a9a1adf198bc25163b0313ed7dd1381f3c8cccc07470823dcc4f01ea` |
| [PGBR-v0.00.38-LT-Root.apk](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.38-LT/PGBR-v0.00.38-LT-Root.apk) | 8348366 | `36cd54df01036a4e9e81defd3ddd1cecfa824034e693a4971ac1c9f012026597` |
| [PGBR-v0.00.38-LT-Root-module.zip](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.38-LT/PGBR-v0.00.38-LT-Root-module.zip) | 8266609 | `ee9eea160001e9b9495df91ff9d77b21b4fd720722c057b06ec33b5a8dc5c9a1` |
| [SHA256SUMS.txt](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.38-LT/SHA256SUMS.txt) | 281 | `004142a6a7687c3df98d30958189082185a789f900034d8249ae06dffd3c7872` |

O novo menu (Batalha Inteligente / Ataque Inteligente / Cura Inteligente), espera de 2300 ms para troca manual, 1700 ms para escudo e carregados instantâneos constam no código dessa versão. **Esta release não implementa a ponte nativa entre `smartAttack` e escolha do golpe Super eficaz > Neutro > Pouco eficaz, nem corrige a tela branca no minijogo.** Cura com itens segue apenas no mapa/entre rodadas validáveis. O usuário tinha confirmado Ágil/Carregado/Escudo funcionando em LT037; nenhum novo teste físico da LT038 foi executado: `device_tested=false`. A tag LT038 publicada fica congelada sem `on.push` (apenas `workflow_dispatch`) para evitar recompilar a versão antiga na próxima mudança de `pgrazor/VERSION`; LT037/LT036/LT035 e outras releases estão preservadas.


## 22/11/2026 — Desenvolvimento isolado: Escudo Inteligente e Troca Inteligente configuráveis

**Origem e preservação:** revisão iniciada na `main` commit `be01d9b59a9e1f31f292b8c562744e5801993d08`, após LT038 (`4ca4c48d417cc4218915db7d763fd360f2c0fc85`). Branch própria `feat/smart-shield-swap-timings-20261122`; a tag QA `v0.00.38-LT`, seus APKs/hashes e PR #98 permanecem intocados. Esta seção documenta trabalho de desenvolvimento, **não uma release nem homologação**. PR e CI serão anexados quando comprovados.

**Alterações submetidas na branch:**
- `Control.java` armazena `smartShieldTiming` e `smartFaintedSwapTiming` (índices 0–3, padrão 1). Restabelecimento em instalação anterior assume 1 para ambos, entradas inválidas são rejeitadas e persistência usa o mesmo arquivo de preferências. O getter `smartBattleConfig` usa `F2|enabled|shieldChoice|faintedChoice` no slot 21 do transporte já existente (sem aumentar o quadro ou alterar a ABI).
- `SettingsPanel.java` mantém os três controles globais Batalha Inteligente, Ataque Inteligente e Cura Inteligente. Adiciona, dentro de Ataque Inteligente, duas seleções exclusivas em PT-BR: **Escudo** 0 ms, 1.500 ms (padrão), 3.000 ms, desativado; **Troca após Nocaute** 400 ms VNext, 2.000 ms (padrão), 6.000 ms, desativado.
- `battle_timing_policy.h` valida rigorosamente preferências e dois snapshots adjacentes com geração/seq idênticas; falha sem snapshot válido. `rocket_combat.cpp` usa somente a preferência antes de acionar o escudo, preservando cancelamento por ação manual, fecho e sessão. `rocket_swap.cpp` limita tentativas a 1.000 ms / 8.000 ms, observa fechamento do seletor antes de agir e mantém a janela manual, inclusive quando desativado. `smart_battle.cpp` não executa sua troca estratégica sobre o seletor de nocaute; o tempo `kSettleMs=2300` de troca estratégica é preservado por ser mudança posterior comprovada na PR #96.
- `rocket_actions.cpp` **não foi modificado**: pós-escudo 250 ms, ágil 150 ms, carregado Rocket Legacy 500 ms e VNext 600 ms. `battles.cpp` e esquivas permanecem intactos; sem espera intencional no disparo carregado. Preferência `smartAttack` de escolha de eficácia ainda **não** tem a ponte nativa de seleção do golpe. Não se ampliou artificialmente o escopo de batalhas ou suporte a Legacy/Max.
- Testes de política host `battle_timing_policy_test.cpp` (todas as opções e snapshots inválidos/rasgados), atualização dos contratos Python, `NativeRuntimeTransportTest.java`, `ControlSettingsTest.java` (persistência com reinício), registro no `tools/native_host_tests.py` e `CHANGELOG.md`.

**Evidência e limites:** alterações de fonte na branch, aguardando revisão PR e evidência CI. Testes da política nativa em host não comprovam a execução real de Shield, nocaute, PvP, treinador, amigos, Root ou sem Root em dispositivo. Necessário inspecionar a PR, executar a suíte/NDK e fazer validação física por opção. `device_tested=false`. **Não gerar/fornecer APK novo sem arquivo e link reais; não fazer merge automático.**


## 22/11/2026 — PR #100 integrada; preparação de lançamento separado LT039

**Fatos confirmados no GitHub:** [PR #100](https://github.com/LucianoToscano/PGBR/pull/100) `merged=true` na `main` pelo commit `848127b8582ce665a3ce9c4c41f00dadd3324e1c`, vindo da branch `feat/smart-shield-swap-timings-20261122`, último HEAD de recurso `f35aa2a614463d135145b812cdb9d8cf5efff62d`. [Execução experimental #45043935988](https://github.com/LucianoToscano/PGBR/actions/runs/45043935988) `completed/success`; 7 checks aprovados no HEAD de recurso, incluindo `check.py`, compilação Java, testes Root, website, política C++ host, NDK ARM64, governança, testes nativos PvP e assinatura das variantes experimentais. Artefato experimental GitHub `PGBR-TimingsQA-Root-e-SemRoot`, ID `14762654832`, expirável (não é Release; não substituir por links LT038 nem chamar de homologado).

**Novo ramo de lançamento:** `release/lt039-smart-battle-timings-20261122`, criado a partir de `848127b8582ce665a3ce9c4c41f00dadd3324e1c`. Atualiza `pgrazor/VERSION` para `0.00.39-LT`, prepara `docs/releases/v0.00.39-LT.md`, `.github/workflows/pgbr-lt039-release.yml`, `README.md`, `CHANGELOG.md` e atualiza contratos históricos de versões para aceitarem uma versão posterior sem reexecutar releases antigas. Publicação proposta é uma **nova pré-release QA LT039**, e não substituir os seis anexos LT038. Arquivos do repositório e testes devem ser validados na PR específica antes de novo merge. O workflow separado é acionado pela entrada do seu próprio arquivo na `main`, compila a versão 0.00.39-LT, valida coerência das três variantes, assina com Secrets do Actions, envia os seis anexos e usa proteção de release existente; sem merge ou APK LT039 **não alegar publicação**.

**Funcionalidade incluída na PR #100:** temporizadores Escudo 0/1.500/3.000 ms/desativado, Nocaute VNext 400/2.000/6.000 ms/desativado, prioridades de escolha humana, persistência e transporte tipado F2. Intervalo pós-escudo 250 ms, ágil 150 ms, carregados Legacy 500 ms e VNext 600 ms **inalterados**. Troca estratégica mantém 2.300 ms da PR #96; tentativas KO 1.000 ms e teto 8.000 ms preservados. Não prometer comportamento universal de Ataque Inteligente nem correção da tela branca; `device_tested=false`. **Pendentes:** evidências de CI do novo ramo, merge da versão, build LT039, URLs reais/SHA-256 e testes de aparelho. Atualizar este mesmo Mestre após publicação com commit final e recibos reais.


### 22/11/2026 — LT039 publicada: recibo de build, merges e anexos verificados

**Integração real:** a [PR #100](https://github.com/LucianoToscano/PGBR/pull/100) foi integrada em `848127b8582ce665a3ce9c4c41f00dadd3324e1c` após 7 verificações de código/build experimental `success`. A [PR #102](https://github.com/LucianoToscano/PGBR/pull/102) preparou o lançamento com versão `0.00.39-LT` e foi integrada na `main` pelo commit `6e12bcfe826cbd4648a509b204c8019ab09c4559` após **5/5 checks `success`** (NDK ARM64, C++ host, Java + Root, website, governança). Histórico LT038 preservado, sem alteração nos anexos antigos.

**Publicação oficial confirmada:** [release QA v0.00.39-LT](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.39-LT) publicada em `2026-11-22T17:01:31Z`; [Actions #45045923119](https://github.com/LucianoToscano/PGBR/actions/runs/45045923119) `completed/success`, jobs `native-pvp-fixture` e `apk-test` completos. Executados testes PvP/Rocket, preferência de escudo/troca, `tools/check.py`, build de APK completo, Root, Zygisk, verificação cruzada de integridade por `verify_release.py`, hashes e upload de seis assets. Assinatura usa apenas GitHub Secrets; nenhuma credencial publicada.

| Pacote publicado | Bytes | SHA-256 exato (API GitHub Releases) |
|---|---:|---|
| [PGBR-v0.00.39-LT.apk](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.39-LT/PGBR-v0.00.39-LT.apk) | 188826947 | `5447b1532ce1e9a520b48352004bf0227318a3d65cbb839f514bbbb87058efb9` |
| [PGBR-v0.00.39-LT-Root.apk](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.39-LT/PGBR-v0.00.39-LT-Root.apk) | 8352462 | `a1461d5369c8829d3438a8bd2aada1c9577f88de505b711e10ffd25e64ba4cf2` |
| [PGBR-v0.00.39-LT-Root-module.zip](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.39-LT/PGBR-v0.00.39-LT-Root-module.zip) | 8269149 | `78ba9cf3704176f278bb3791018a056ff3397358272ab561015cf2699bb40782` |
| [SHA256SUMS.txt](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.39-LT/SHA256SUMS.txt) | 281 | `4f9a0eb556e849b3d63295e479957c5daad7c0d56d9fe4cb80e15d11a268a82e` |

**Estado funcional e limitações mantidas:** escudo 0/1.500/3.000 ms/OFF e troca após nocaute VNext 400/2.000/6.000 ms/OFF; prioridade manual, persistência F2, 250 ms pós-escudo, 150 ms ágil, 500 ms Legacy / 600 ms VNext carregado, 1.000 ms retry KO, 8.000 ms timeout. A seleção estratégica de 2.300 ms continua preservada. `smartAttack` ainda é preferência sem associação nativa universal dos botões de golpes; tela branca do minijogo continua sem causa confirmada. **Aprovação de CI e build NÃO equivale à homologação física: `device_tested=false`**. Testar no Android os oito modos de configuração, cancelamento manual, PvP/treino com identificadores confirmados, Root/sem Root; preservar backup e releases anteriores.

**Proteção contra recompilação histórica:** após publicar, a configuração da LT039 torna-se de manutenção manual `workflow_dispatch` apenas. Esta proteção é registrada em PR de recibo independente, sem reescrever a tag ou anexos da release.


## 22/11/2026 — Fix isolado LT039 para evidência PvP tipada após Hub/Preparação (issue #105)

**Relato de Android:** usuário confirma que o Assistente de Batalha opera no **Treinamento NPC**, mas não nas batalhas PvP contra jogadores nem em **duelos com amigos**. Vídeos privados de 22/11/2026 documentados separadamente no [PR documental #106](https://github.com/LucianoToscano/PGBR/pull/106). O novo relato não autoriza assumir que a causa está confirmada; LT039 ainda não foi retestada com logs de escopo em aparelho.

**Base e isolamento:** branch `fix/pvp-typed-hub-recovery-lt039-20261122`, baseada na `main` SHA `5087e7486e90f84167017b500cc8c3319e050825`, sem edição de `main`, sem merge automático e sem alterar trabalho paralelo. Comparação de tags `v0.00.37-LT...v0.00.39-LT` mostra `pvp_scene.h`, `pvp_probe.inc`, `combat_host.cpp` e `cutscene_skip.cpp` sem mudanças diretas nesses arquivos, embora outras configurações e temporizações tenham sido modificadas.

**Defeito específico identificável no código:** `PgrPvpScene::verified_pvp_type()` aceitava a prova do próprio jogo `CombatTypeExtensions.IsPvPCombat` somente com `stage==IDLE`. Se `CombatHubGuiController` já tivesse aparecido (HUB) e a preparação não fosse observada, a evidência tipada era descartada. Quando PREPARATION já é válida, seu prazo original de 180 s continua suficiente e **não deve ser reduzido** pelo fallback de 15 s. A prova validada só ocorre se `observe_pvp_legacy_ui` conseguir classificar um CombatDirectorV2; VNext ainda é obrigatório antes de ações.

**Alteração proposta em `pgrazor/native/pvp_scene.h`:** adicionar `typed_expires_ms` como **prova temporária separada** de `stage` e de `expires_ms`, mantendo a sessão HUB válida por seus **600 s** originais. Quando o tipo PvP é confirmado de modo validado durante HUB, o VNext pode entrar somente nos **15 s** seguintes à prova; sem VNext, a expiração dos 15 s **não destrói HUB**, e a preparação que surgir posteriormente ainda obtém seus **180 s** originais. A preparação já validada nunca é reduzida pela prova tipada. A entrada direta com prova tipada sem Hub continua com janela de 15 s. VNext real, escopo permitido, lease, máscara, exclusão PvE e motor Rocket são exigidos como antes. Não modificar TRAINING, combate já ativo, alvos desconhecidos, Raid/Ginásio/Max ou seleção de golpes `smartAttack` (issue #90). Sem hook novo não validado.

**Testes alterados em `pgrazor/native/pvp_scene_test.cpp`:** HUB + prova → VNext em 15 s, HUB + PREPARATION + prova sem encurtar 180 s, HUB + prova expirada ainda apto a PREPARATION tardia, expiração de prova em 15 s sem limpar HUB, NPC que não pode virar PvP e combate TRAINING não substituído. O fixture `pvp_scene_test.cpp` foi adicionado à allowlist `tools/native_host_tests.py`, para executar como teste C++ portátil durante CI, sem Android; resultados da CI ainda precisam ser confirmados. `device_tested=false`.

**Limitação importante:** a LT039 pode falhar também porque o `observe_pvp_legacy_ui()` só é chamado por hook da UI legado (`hook_rk_ui_update`); se o duelo for VNext-only sem Hub nem CombatUI, o detector ainda não comprova o tipo. A correção deste PR é **parcial e condicional** à prova de combate validada aparecer. Investigar logs de `pvpObserved`, `pvpExecutorReady`, `pvpAction` e motivos de saída antes de declarar problema resolvido em GBL/amigos. Preservar comportamento seguro fail-closed, artefatos antigos e códigos paralelos. Nenhum APK novo nem release nesta etapa.


## 22/11/2026 — Solicitação consolidada do usuário: recuperar Auto Ataque LT037, tempos LT039 e Ataque Inteligente

**Relato determinante e base para QA:** o usuário afirma que o auto-ataque funcionava na **LT037** e deixou de executar de modo confiável na LT039 em GBL/PvP, duelos privados e, em parte dos testes, treinamento NPC. Dois vídeos foram enviados no chat em 22/11/2026. A segunda gravação mostra uma luta de Treinamento contra NPC; animações não provam automação, logs de execução devem ser confrontados. Não presumir que o vídeo prova a causa. A PR [#107](https://github.com/LucianoToscano/PGBR/pull/107) é correção **candidata QA**, e a [#105](https://github.com/LucianoToscano/PGBR/issues/105) registra o defeito; a [#90](https://github.com/LucianoToscano/PGBR/issues/90) acompanha o Ataque Inteligente por eficácia.

**Auditoria de versionamento:** comparação de tags `v0.00.37-LT...v0.00.39-LT` confirmou que `combat_host.cpp`, `pvp_scene.h`, `pvp_probe.inc` e `cutscene_skip.cpp` não sofreram alteração direta nesse intervalo, enquanto `rocket_combat.cpp`, `rocket_swap.cpp`, `Control.java`, `SettingsPanel.java` e a nova política `battle_timing_policy.h` receberam modificações. Os intervalos rápidos (150 ms) e carregados VNext (600 ms) ou Legacy (500 ms) não foram deliberadamente alterados. Os temporizadores novos afetam escudo e escolha após nocaute; o índice 'Desativado' 3 foi admitido na LT039 e pode explicar ausência dessas ações, **não prova causa da falta de ataques ágeis**.

**Controles solicitados (branch PR #107):** retirar a quarta opção `Desativado` dos tempos Escudo e Troca após Nocaute. Permanecem Escudo `0/1500/3000 ms` e troca de nocaute VNext `400/2000/6000 ms`. A preferência antiga `3` é migrada para o índice padrão `1` pelo `Control.java`, e por compatibilidade o leitor nativo F2 trata um quadro antigo com `3` como espera padrão (sem acionar comandos quando o snapshot é inválido). A desativação efetiva das ações continua sendo feita nas caixas Ágil/Carregado/Escudo. Não alterar o intervalo técnico pós-escudo 250 ms, carregados sem espera intencional, retries 1000 ms, limite 8000 ms, ou a troca estratégica separada 2300 ms.

**Ataque Inteligente — evidência:** o interruptor `smartAttack` atualmente modifica apenas a preferência persistida e o bit `PvpBattleMode.PREFER_SUPER` (8). O motor `rocket_combat.cpp` usa exclusivamente bits `1/2/4`; portanto, alternar `smartAttack` não desativa os ataques. Testes JVM/source foram adicionados para garantir que ligar/desligar a estratégia **preserve os três bits compartilhados**. Entretanto, o algoritmo `PvpChargedPreference`/`PvpBattleController` **não está ligado a um adaptador real** que forneça IDs de golpes, tipos e adversário e selecione botões no native; **SUPER > NEUTRO > POUCO não está operacional** e não pode ser anunciado como corrigido. Sem metadados validados da versão de jogo, preservar a execução padrão do primeiro carregado pronto, sem tentar inferir ataques ou eficácia.

**Reativação por sessão e segurança:** recuperar prova temporária de tipo em HUB (`typed_expires_ms` 15 s) sem encurtar a espera original 600 s de HUB/180 s de Preparação. Passar a instalar passivamente o observador validado `GuiController.Open` após bootstrap, independentemente de o usuário ligar a opção antes/depois de entrar no lobby: antes o instalador esperava algum bit `1/2/4` e podia perder eventos de Hub/Preparação. Observador **não despacha ações**. O módulo Rocket continua com `PGR_RC_OBS_SCOPE_ENTER` para `InvasionCombatState` exata; PvP/duelo/treinamento usam `PGR_RC_OBS_PVP_SCOPE_ENTER` somente após confirmação da máquina de estados, VNext real, PVE exclusion, máscara/lease, sem falsificar identidade ou RPC.

**Máscara efetiva compartilhada:** portar com proteção de thread a ideia proposta na PR histórica [#86](https://github.com/LucianoToscano/PGBR/pull/86) (ainda aberta, não fazer merge integral de branch antiga). Novos `shared_auto_battle_gate.h` e teste host; `refresh_leaf_gates()` primeiro lê amostra atômica `g_rocket_combat->sample()`, depois entrega **máscara 0 fora do escopo nativo real**, com lease expirada, motor com falha, app desativado ou preferências todas desmarcadas; entrega exatamente os bits pedidos `1/2/4` no escopo nativo validado. Não acessa `PgrPvpScene` e ponteiros Unity em atualizações que possam ocorrer em outra thread. Raid/Ginásio/Max conservam executor `battles.cpp` separado; monstros selvagens/estados desconhecidos nunca são autorizados. Ginásios já têm opções ágil/carregado próprias e **não devem ser remapeados ao executor Rocket**.

**Diagnóstico adicionado:** `combat_host.cpp` publica eventos sanitizados `pvpTypeProbeReady`, `pvpScene` (hub, preparação, candidato de treinamento) e `pvpCandidateDenied` por mudança de razão, além de `pvpExecutorReady/pvpAction` existentes. `Control.event` reflete mensagens de bloqueio nas configurações, sem expor dados pessoais. Permite distinguir ausência de metadados, máscara desligada, lease indisponível, escopo inválido e colisão com outro modo. Falta validar os registros no Android; 'invoked' não garante comando aceito.

**Arquivos e verificações no PR:** `pvp_scene.h`, `pvp_scene_test.cpp`, `cutscene_skip.cpp`, `combat_host.cpp`, `Control.java`, `SettingsPanel.java`, `battle_timing_policy.h`, `battle_timing_policy_test.cpp`, `shared_auto_battle_gate.h` e teste, `tools/native_host_tests.py`, `test_pvp_trace_bootstrap.py`, `test_lt037_battle_allowlist.py`, `test_battle_timing_controls.py`, `SharedBattleOptionsTest.java`, `PvpBattleModeTest.java`, `ControlSettingsTest.java` e este Mestre. O fixture do classificador e o fixture do gate são executados na CI C++ portátil. Houve falha transitória de sintaxe em comentário `//` dentro de Python que **foi corrigida** no commit `30cd18ab8537e891caa14b6c6dbd22312178ffd6`. Cada novo commit cancela/verifica a execução anterior conforme política CI; usar apenas checks do último HEAD para declarar resultado.

**Estado de entrega:** PR em revisão, sem merge, sem nova release/APK confirmado, testes físicos `device_tested=false`. A geração do APK e o fechamento da falha dependem de CI atual mais validação ao menos em Rocket, Treinamento, GBL, duelo de amigos, Raid/Ginásio/Max sem regressão. É incorreto anunciar 'Ataque Inteligente por eficácia funcional' ou 'PvP corrigido definitivamente' antes de um adaptador de dados validado e teste Android.


## 22/11/2026 — Integração #107 confirmada e lançamento LT040 preparado (pendente de release)

**Integração real de código:** [PR PGBR #107](https://github.com/LucianoToscano/PGBR/pull/107) integrada na main no commit **`f873c8652bac5dc4cded154e49fbab82bf778445`** após aprovação dos **cinco checks** no HEAD `e17ae9019dae201b5a9330ff28e124e093724f29`: Java/Root, C++ host (15 fixtures incluindo cena PvP e gate do assistente), NDK ARM64, website e governança. [CI da PR](https://github.com/LucianoToscano/PGBR/actions/runs/45053315564). Isso comprova código e compilações parciais, **não** homologação física.

**Branch de release:** `release/lt040-pvp-scope-20261122`, iniciada no commit já integrado `f873c865...`, preservando LT037/LT038/LT039. Propõe `pgrazor/VERSION=0.00.40-LT`, novas notas `docs/releases/v0.00.40-LT.md`, workflow separado `.github/workflows/pgbr-lt040-release.yml`, novo contrato `test_lt040_release_contract.py`, README/CHANGELOG e atualização dos contratos de versão históricos para permitir a nova versão sem reabrir tags passadas. Nenhum segredo, APK binário ou fonte de fornecedor inserido no GitHub.

**Política de lançamento:** apenas ao integrar a branch de release na main, workflow `push` restrito à nova versão/novo workflow/notas inicia build de QA para sem Root, Root e Zygisk; verifica hashes e integridade cruzada e publica somente uma tag nova `v0.00.40-LT` caso ela ainda não exista. Nunca atualizar tag LT039 nem confundir um workflow com um APK publicado. Se a versão LT040 for publicada, remover gatilhos de push na definição LT040 em um PR posterior (manual only), preservando auditoria e evitar disparos históricos. Associar URLs e SHA-256 reais no Mestre após recibos API. `device_tested=false` até teste Android de GBL/duelo/NPC/Rocket/PVE. Permanecem pendentes eficácia nativa de `smartAttack` (#90), falhas possíveis de detecção VNext-only e tela branca do minijogo.


## 22/11/2026 — Recibo factual de merge do lançamento LT040 e registros cruzados

**Main e código funcional:** PR [#107](https://github.com/LucianoToscano/PGBR/pull/107) integrada em `f873c8652bac5dc4cded154e49fbab82bf778445`, após 5 checks concluídos com êxito no HEAD `e17ae9019dae201b5a9330ff28e124e093724f29`. Recuperação de cenas PvP, observer passivo, gate compartilhado somente em sessão validada, diagnóstico e seleções de tempo (sem `Desativado`). `device_tested=false`; a integração não prova a aceitação real de ataques no Android.

**Nova versão:** PR de lançamento [#110](https://github.com/LucianoToscano/PGBR/pull/110) integrada na `main` em `57f6789bb887ef372ba84c922f9c27c00ce3f1bf` (HEAD de release `a0563c36ad91efafb4dc159aff07145047bd8d1f`). Foram 5 checks `success` da PR, incluindo 292 testes, código Java/Root, C++ host, NDK ARM64, website e governança; a falha prévia de nome de APK no README foi corrigida antes do merge. Workflow de lançamento dedicado [Actions #45054751331](https://github.com/LucianoToscano/PGBR/actions/runs/45054751331) foi acionado por push na main; `native-pvp-fixture` passou, e `apk-test` iniciou. **No momento deste recibo, publicação de `v0.00.40-LT` e links/hash de APK ainda não estavam confirmados; nunca considerá-los existentes só pela configuração.** A LT039 e todas as versões anteriores permanecem imutáveis. `device_tested=false`.

**Outros arquivos mestres atualizados sem editar aplicativos:** auditoria do mesmo chat foi integrada em [PokeSearch PR #558](https://github.com/LucianoToscano/PokeSearch/pull/558), commit `5149baae8117294bf7b31eb98ef620ddf2610d17`, arquivo `docs/GUIA_MESTRE.md`; [PvPPokeGo PR #56](https://github.com/LucianoToscano/PvPPokeGo/pull/56), commit `79fc9b032842cd87bcce75b38fcb9fd0b8935ff9`, arquivo `docs/CONSOLIDATED_PROJECT_CONTEXT_2026-09-26.md`; e [PGSharp PR #15](https://github.com/LucianoToscano/PGSharp/pull/15), commit `b9f8dbc67d60f3b3d50e64f928fc919babc5c70e`, `README.md` canônico de referência. Todos foram conferidos na `main` dos respectivos repositórios; sem mudança de código, APK, release, login, API, HUD ou integração de terceiros nesses três projetos. Não alegar cópia de implementação binária do PGSharp.

**Acompanhamento:** usuário solicitou link direto quando o APK novo existir. Foi criada a automação de verificação da release LT040 com ID `6b03334eb52881919bb678ab8493b2fb`, somente para comunicar links reais após confirmação de assets/hashes ou falha definitiva; a presença de monitor não equivale a build aprovada. PGBR [issue #105](https://github.com/LucianoToscano/PGBR/issues/105) continua aberta para reprodução física do assistente e da detecção PvP VNext-only, [#90](https://github.com/LucianoToscano/PGBR/issues/90) continua aberta para ligação real do algoritmo Super eficaz > Neutro > Pouco eficaz; esbranquiçamento do minijogo continua pendente.

**Próxima atualização factual obrigatória:** após o término do workflow, verificar estado `completed/success`, lista real de seis assets, tamanho/digest, tag, links diretos; registrar o recibo neste **mesmo** Mestre e congelar o gatilho `push` da LT040 para manutenção `workflow_dispatch` apenas. Não reescrever histórico anterior.


## 22/11/2026 — LT040 publicada: recibos GitHub e congelamento do lançamento

**Merge/release confirmado:** a [PR #110](https://github.com/LucianoToscano/PGBR/pull/110) foi integrada na main em `57f6789bb887ef372ba84c922f9c27c00ce3f1bf`, após 5 checks `success`. [Workflow de build #45054751331](https://github.com/LucianoToscano/PGBR/actions/runs/45054751331) terminou **`completed/success`**, com `native-pvp-fixture` e `apk-test` aprovados, e a [pré-release v0.00.40-LT](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.40-LT) foi publicada em **2026-11-22T18:32:42Z**, `draft=false`, `prerelease=true`. API GitHub confirmou seis anexos, incluindo os três pacotes, recibos e `SHA256SUMS.txt`. Assinatura via Secrets; sem chave publicada.

| Asset verificado pela API GitHub | Bytes | SHA-256 |
|---|---:|---|
| [PGBR-v0.00.40-LT.apk](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.40-LT/PGBR-v0.00.40-LT.apk) | 188831043 | `1326f2223c454de50a99900e1b5e8d1ccca9759fbca1e134ffcc678a9cfa1a56` |
| [PGBR-v0.00.40-LT-Root.apk](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.40-LT/PGBR-v0.00.40-LT-Root.apk) | 8356558 | `204b6e8529693c1985488405de8cb5dd430d384e9feac1a818fbc6035963f4a8` |
| [PGBR-v0.00.40-LT-Root-module.zip](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.40-LT/PGBR-v0.00.40-LT-Root-module.zip) | 8271707 | `9817fe4062657cb5241119de6c9182d0853b7b7a44aacf176734ccf588e5951c` |
| [SHA256SUMS.txt](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.40-LT/SHA256SUMS.txt) | 281 | `0cf3768167964a577dd8c9f8159230ccbb284d01e6c37eb7f53d64a8bf88c9ad` |

Os digests são **dados da API GitHub Releases**, não hashes recalculados baixando o APK. Artefatos LT037, LT038 e LT039 mantidos imutáveis. A `main` recebeu recibos documentais das integrações do PokeSearch #558, PvPPokeGo #56 e PGSharp #15 via PGBR [PR #112](https://github.com/LucianoToscano/PGBR/pull/112), merge `27a39ced365c4978df216f89fad4b5aa9f57a0c0`. A presente alteração de manutenção remove gatilho `push` de `.github/workflows/pgbr-lt040-release.yml`, deixando `workflow_dispatch` para reproduções controladas, e ajusta teste de contrato; nenhum rebuild de uma tag antiga deve disparar.

**Sem homologação física:** `device_tested=false`. A escolha de golpe por eficácia permanece não implementada nativamente [#90](https://github.com/LucianoToscano/PGBR/issues/90), duelos sem Hub/CombatUI podem não passar no classificador [#105](https://github.com/LucianoToscano/PGBR/issues/105), e a tela branca de carregado não foi corrigida. A pré-release permite avaliação no Android, mas os checks **não** provam dano/escudo efetivamente aceitos. O monitor provisório da release, ID `6b03334eb52881919bb678ab8493b2fb`, foi **desativado** após os links diretos serem confirmados, para evitar avisos redundantes.

---

## 22/11/2026 — Auditoria e motor independente de recomendação de ataque

**Base e isolamento:** branch `arena/be6b0894-pgbr` a partir de `main` `dc3f928e7bb4d8da9363ee45db3105bb8411e90c`; main e PRs abertas conferidas, incluindo #86, #106, #114 e #115. Sem reset, merge de branches paralelas ou nova release. Auditoria detalhada e contrato: [docs/AUDITORIA_MOTOR_ATAQUE_2026-11-22.md](docs/AUDITORIA_MOTOR_ATAQUE_2026-11-22.md).

**Código:** `pgrazor/native/smart_attack_decision.h` introduz recomendação C++ pura com tipos duplos, STAB configurável, energia, disponibilidade, até dois carregados, ágil como alternativa, resultado e motivo explícitos, recálculo sem cache e falha fechada para dados inválidos. Reutiliza tabela local `battle_counter.h`, sem hooks/offsets. É um modelo heurístico por potência/energia, não cálculo de dano real nem controle do cliente. **Não está ligado** ao interruptor, observação do jogo ou execução nativa; #90 permanece pendente para integração real.

**Correção verificada em fonte:** `SettingsBackupRules.java` omitía `smartAttack`, `smartShieldTiming` e `smartFaintedSwapTiming` da exportação/importação; agora inclui e valida as três, sem alterar os tempos aprovados ou a compatibilidade de backups antigos. `ControlSettingsTest.java` e `SettingsBackupRulesTest.java` receberam regressões de ON/OFF, independência, reinício e validação; requerem CI JVM para execução. Contrato de UI já declara que a seleção nativa está pendente.

**Testes locais:** `tools/native_host_tests.py` executou com sucesso **16 fixtures C++ host**, incluindo motor novo, tempos, PvP, 33 cenários Rocket e Raid/Gym/Max. Contratos fonte de tempos, UI e bootstrap passaram; allowlist PvP passou 2 testes. `tools/check.py` foi tentado e parou no preflight: faltam JDK (`javac`/`java`) e plataforma/fontes Android SDK 36. Testes Java/SDK, NDK, APK e homologação física não executados nesta sessão; CI da branch ainda deve ser consultado. A release LT040 não foi alterada e `device_tested=false` para este trabalho.

**Dependências externas para integração:** catálogo de golpes/tipos por versão e modalidade; snapshot confiável de ativo/adversário/energia/prontidão; identificação estável de ambos botões; invalidar dados em troca/saída e validar ações por modalidade, inclusive executor separado Raid/Gym/Max; ensaio em Android autorizado. A recomendação independente não comprova ação aceita, correção definitiva do PvP (#105), correção de tela branca nem funcionamento no jogo.

**Rastreabilidade da entrega:** [PR #116](https://github.com/LucianoToscano/PGBR/pull/116), draft, código inicial `b596f3f` na branch acima. Checks GitHub desta revisão ainda pendentes de confirmação; consultar a PR antes de integrar. Sem merge, build de release ou artefatos novos.

**CI confirmado no HEAD de código `1ea803ec509fa5bce8f0096358dfdcb6a084de68`:** [código #45063215622](https://github.com/LucianoToscano/PGBR/actions/runs/45063215622) `success` nos quatro jobs (16 fixtures host, NDK selecionado ARM64, website e `tools/check.py`/compilação Java/instalador Root); [governança #45063215581](https://github.com/LucianoToscano/PGBR/actions/runs/45063215581) `success`. O primeiro run da PR, #45063206098, foi cancelado pela concorrência ao enviar a atualização documental, não é resultado da revisão final de código. O NDK compila a seleção configurada pelo workflow, não implica que o motor novo foi executado em Android. Esta anotação é posterior aos runs e dispara nova verificação; não presumir resultado do novo HEAD. Sem APK de jogo ou teste físico.

## 22/11/2026 — Revisão da PR #116: estratégias e backup integral

Na mesma branch `arena/be6b0894-pgbr`, o comparador experimental deixou de priorizar efetividade absoluta: PvP usa potência ajustada por STAB/tipo dividida pelo custo; PvE usa indicador de potência por tempo de ciclo ágil/carregado com durações fornecidas pelo catálogo de cada modalidade. São heurísticas diferentes, com pressupostos e limitações explícitos em `docs/AUDITORIA_MOTOR_ATAQUE_2026-11-22.md`, **não dano real nem garantia de melhor jogada**. Snapshot PvE incompleto é rejeitado. Não há conexão ao executor nem novos hooks ou offsets. Testes cobrem super eficaz fraco versus neutro forte, custo, STAB, prontidão, empate, modos e dados ausentes.

O fluxo real de backup foi extraído sem alteração funcional da UI para `SettingsBackupStore` e exercitado em JVM: JSON exportado e reaberto, validação, restore, três preferências, grupo de preferências, reload, legado/parcial e arquivo inválido. A prova do fluxo revelou o limite desatualizado de `quickMenuHidden`; validação agora acompanha a máscara existente, preservando o bit do seletor. Tag LT039 confirma que as três chaves não eram exportadas; índice local legado 3 é convertido ao padrão 1 pelo comportamento LT040 preexistente, sem alterar tempos aprovados. Migração de `PREFER_SUPER` e precedência de `smartAttack` explícito são testadas separadamente de backups que não traziam o bit.

Passaram 16 fixtures C++ host e suites JVM de Control/backup/feed com runtime e compilador temporários e fontes `org.json` AOSP de espelho; não é SDK oficial nem validação Android. Consultar checks da HEAD corrente da PR antes de solicitar revisão. Nenhuma release ou validação em dispositivo nesta revisão. `device_tested=false`.

## 22/11/2026 — PR #116: revisão final e rollback de backup

**Base:** `main` `dc3f928e7bb4d8da9363ee45db3105bb8411e90c`, PR #116 na branch `arena/be6b0894-pgbr`, HEAD anterior `b30d0236b9267d1d7544166fbfeb219fbf9528f7`. Seis checks anteriores passaram: [código](https://github.com/LucianoToscano/PGBR/actions/runs/45064453181), [governança](https://github.com/LucianoToscano/PGBR/actions/runs/45064453168), [contrato adicional](https://github.com/LucianoToscano/PGBR/actions/runs/45064453159). PRs paralelas consultadas e preservadas; não alterar tempos de batalha nem integrar outras branches.

**Problema corrigido:** falha depois de aplicar parte de `Control.importSettings` antes revertia apenas `SharedPreferences`. `Control.java`, `SettingsBackupStore.java`, `SettingsBackup.java` e `PtBr.java` agora tentam recuperar propriedades completas, memória, velocidade e grupos tentados; recarga estrita sinaliza falhas, e reversão incompleta aparece ao usuário. Rollback é best-effort para exceções; não há atomicidade entre arquivos perante morte do processo/falha persistente, nem restauração de automações/rotas suspensas por segurança. Backup externo permanece recomendado.

**Validação executada:** 16 fixtures C++ host; contrato de tempos/UI/bootstrap PvP e allowlist LT037; `test_control_java.py` e `test_live_feed_java.py` em runtime Java temporário, ECJ e `org.json` AOSP espelhado. `SettingsBackupRoundTripTest.java` injeta falha parcial em Control, checkpoint de velocidade, commits de grupos e falha durante rollback, além dos testes de arquivo antigo/inválido; `smart_attack_decision_test.cpp` reforça comparação PvE e ausência de ações. O gate oficial JDK/SDK/NDK/governança precisa passar no novo HEAD antes do merge; nenhum desses testes garante durabilidade Android, ausência de regressão em todos os aparelhos ou gameplay.

**Próximo passo:** contrato separado A motor/B provedor autorizado/C apresentação informativa no relatório `docs/AUDITORIA_MOTOR_ATAQUE_2026-11-22.md`; sem fonte live, tela ou executor ligados. `device_tested=false`. Esta anotação antecede o commit de revisão e os seus checks; consultar [PR #116](https://github.com/LucianoToscano/PGBR/pull/116) para o HEAD final e resultado da integração. Não publicar release.


## Registro factual — 22/11/2026: auditoria de regressões e Ataque Inteligente simples (branch independente)

- **Base verificada:** `b355def7f6693b4d7f88c802b6ea40df018f0490`; branch `fix/ataque-simples-sem-espera-20261122`. Registro técnico: [AUDITORIA_PGBR_ATAQUE_SIMPLES_2026-11-22.md](docs/AUDITORIA_PGBR_ATAQUE_SIMPLES_2026-11-22.md).
- **Histórico/downgrade:** a tag QA `v0.00.40-LT` é ancestral da `main` auditada (16 commits à frente, 0 atrás); o diff de commits não contém arquivos removidos. **Nenhum downgrade estrutural de branch/versão foi identificado nesse intervalo**; isso não exclui regressão em gameplay. `v0.00.39-LT` também é ancestral da `v0.00.40-LT` (57 commits à frente, 0 atrás). Manter a investigação das issues #90 (seleção do carregado) e #105 (PvP/VNext).
- **Defeito corrigido em política pura Java:** `PvpChargedPreference.select()` podia segurar um carregado pronto para aguardar o mais eficaz; agora a política simplificada escolhe **super eficaz > neutro > resistido** entre os *prontos*, sem espera adicional; categorias iguais usam a primeira opção. Com um único golpe conhecido e pronto, usa o primeiro. O `PvpBattleController` deixa de manter o timeout antigo de espera para farm. Mantidos os controles/assinaturas da política; são decisões simuláveis, **não controle real** do cliente.
- **Arquivos relevantes:** `pgrazor/src/dev/pgrazor/PvpChargedPreference.java`, `pgrazor/src/dev/pgrazor/PvpBattleController.java`, `pgrazor/test/PvpChargedPreferenceTest.java`, documentação de auditoria e este Mestre. Cobertura acrescentada para matriz 3×3 de categorias, empate, ataque imediatamente pronto, golpe único e prevenção de duplicatas no controlador.
- **PR #117 preservada:** contém transporte F3 e regra nativa C++ pura em rascunho, **não** consumida pelos executores; não houve merge ou publicação dela nesta auditoria. Não foi feita alteração nos temporizadores, Cura Inteligente, Rocket Legacy/VNext, Raid/Gym/Max ou PokeSearch. As outras PRs continuam separadas.
- **Validação:** CI da branch e qualquer teste físico devem ser consultados na PR antes de serem anunciados como aprovados. Nesta sessão, análise GitHub e alterações de código/documentação foram possíveis, mas **não houve geração de APK/NDK completo, assinatura, instalação nem teste de gameplay**. A release existente `v0.00.40-LT` fica imutável; não inventar v0.00.41, SHA ou download.
- **Pendência principal:** associar eficácia de cada carregado ao botão e à sessão atual com leitura verificável de energia/prontidão, revalidar estado antes do comando, testar por modalidade e dispositivo. As capturas visuais do usuário mostram classificações nos botões, mas não comprovam contrato runtime. **Não declarar Ataque Inteligente funcional nem fechar #90.**


### Recibo pós-merge da auditoria de Ataque Inteligente simples — 22/11/2026

- **[PR #120](https://github.com/LucianoToscano/PGBR/pull/120) integrada à `main`** pelo commit de merge [`239901f4fdbfaca2fe345899805de79b846401af`](https://github.com/LucianoToscano/PGBR/commit/239901f4fdbfaca2fe345899805de79b846401af), com HEAD de código revisada `c0dc84b2fc052f5a6c3129b2a945e3a5ad35aff0`. A branch independente manteve a PR #117 em rascunho, sem alterar seus commits.
- **CI pré-merge:** cinco checks `completed/success`, incluindo Java/Root/`tools/check.py`, NDK ARM64 selecionado, políticas C++ host, website e governança. [Run de código #45083232310](https://github.com/LucianoToscano/PGBR/actions/runs/45083232310); [governança #45083232296](https://github.com/LucianoToscano/PGBR/actions/runs/45083232296). Verificar novamente CI pós-merge; estes runs pertencem à HEAD da PR.
- **Correção realmente integrada:** política PvP Java classifica apenas super eficaz > neutro > pouco eficaz, usa carregado pronto sem espera estratégica, aceita único carregado conhecido e mantém desempate pelo primeiro. O `PvpBattleController` deixou de armazenar timeout de farm de 5,5 s para a política simplificada. Testes ampliados para nove combinações e prontidão. **O executor nativo não foi alterado e ainda não recebe essa seleção.**
- **Sem publicação nova:** não foi gerado ou assinado APK neste trabalho, não houve validação em Android e nenhuma tag/release/hashes novos foram criados. A release pública vigente continua [v0.00.40-LT](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.40-LT), `device_tested=false`. Não atribuir à LT040 a correção de código posterior a sua tag.
- **Pendência obrigatória:** issue #90 aberta; dependências de eficácia por botão, identidade/sessão, energia, integração nativa e homologação física continuam sem comprovação. [Auditoria desta revisão](docs/AUDITORIA_PGBR_ATAQUE_SIMPLES_2026-11-22.md). Não anunciar "Ataque Inteligente funcionando no jogo" com base na política Java ou no CI.



## 22/11/2026 — Auditoria de regressões pós-LT040 e correção de escopo iFlowGo

**Origem e proteção de paralelismo:** `main` `b355def7f6693b4d7f88c802b6ea40df018f0490`; branch `audit/20261122-no-downgrade-hardening`, sem alterações em `main`, nos artefatos imutáveis da LT040 ou nas PRs em andamento. A comparação GitHub `v0.00.40-LT...main` mostrou 16 commits à frente e zero atrás; alterações são principalmente motor de recomendação/backup da PR #116 e documentação. Não houve deleção de arquivo no comparador deste intervalo, **mas isso não prova ausência de downgrade de comportamento no aparelho**. Checks da `main` #45072268952/#45072268865 passaram no commit base, `device_tested=false`.

**Problemas de código concretos corrigidos na branch:** `IFlowGoFeedClient.fetch` permitia raio `NaN`; a busca global ignorava #Dex inválido e alargava consulta sem aviso; o modal de Reides oferecia e selecionava Global no iFlowGo enquanto o conector limitava a busca a 25 km de uma área. A política pura `IFlowGoRequestPolicy` bloqueia parâmetros inválidos; `LiveFeedDialog` usa raio local padrão nos Reides, avisa que Global de Reides não é suportado e barra #Dex numérico inválido. Não altera demais fontes ou tempos de batalha. Também foram atualizadas as instruções desatualizadas LT034 em `docs/BUILDING.md` e `docs/ROOTED.md`; LT040 preservada. Testes novos: `IFlowGoRequestPolicyTest.java` e `test_iflowgo_request_policy_java.py`.

**Limites e coordenação:** Ataque Inteligente com escolha efetiva dos botões ainda pendente (#90); relato de desarme PvP após janela sem resolução física (#105). PRs #101, #108/#114, #109, #115, #117 e #118 consultadas, **não mescladas**; #117 e #118 ainda rascunhos, #118 depende de #115, e #114 tinha conflito. Garantir que não haja substituição destrutiva de trabalhos concorrentes antes da integração. Auditoria técnica: [docs/AUDITORIA_SEM_DOWNGRADE_2026-11-22.md](docs/AUDITORIA_SEM_DOWNGRADE_2026-11-22.md). Branch/proposta ainda depende de CI e revisão; **nenhuma nova APK, tag ou release foi produzida por esta auditoria**. Nunca anunciar LT041 antes de build/verify/publish reais.

## 22/11/2026 — Preparação de nova QA LT041 (não publicada até confirmação do GitHub)

**Antecedente:** PR de auditoria #122 na branch `audit/20261122-no-downgrade-hardening`, derivada de `main` `b355def7f6693b4d7f88c802b6ea40df018f0490`. Esta preparação em `release/lt041-audit-qa-20261122` contém apenas alterações adicionais de versão, workflow e notas necessárias à publicação de um **novo** pacote QA, preservando a tag imutável v0.00.40-LT e branches paralelas.

**Implementações preparadas:** `pgrazor/VERSION` agora `0.00.41-LT` na branch da candidata; workflow dedicado `.github/workflows/pgbr-lt041-release.yml` para compilar, assinar e verificar três variantes no Windows com ferramentas/entradas fixadas, disparando na `main` apenas pelo novo workflow/arquivo de versão; `tools/verify_release.py` e publicação condicional sem substituir a tag existente. `docs/releases/v0.00.41-LT.md`, README e CHANGELOG identificam a candidata como **ainda não publicada**. Suítes históricas de releases ganham `0.00.41-LT` na allowlist sem alterar o teste da tag antiga. Novo `test_lt041_release_contract.py`; teste Java dos limites iFlowGo explícito no workflow.

**Critério imutável:** não mesclar/lançar enquanto CI falhar ou conflitos pendentes; atualizar este registro factual **após** efetiva release com SHA-256, CI e links dos downloads. `device_tested=false` até ensaio em Android físico. Sem integrações de PRs #115/#117/#118 ou alegação de Ataque Inteligente funcionando (#90), e problema PvP #105 permanece em investigação.

## 22/11/2026 — Auditoria parcial de batalha e proteção de cura no mapa

Base `main` `b355def7f6693b4d7f88c802b6ea40df018f0490`, PR #116 integrada. Branch `arena/8b0f505f-pgbr`; PRs paralelas consultadas e preservadas. [Relatório de evidências e pendências](docs/AUDITORIA_BATALHA_CURA_2026-11-22.md). O motor de recomendação continua **sem integração** ao executor, inclusive com a preferência ligada; catálogo de equipe/troca não prova estado por sessão, dados PvP e associação de ambos os botões. Não reivindicar ação inteligente, correção de tela branca, homologação ou release.

`heal.cpp` revalida configuração e mapa após consultas gerenciadas antes de usar item, fechando uma janela de estado obsoleto no fluxo normal; entre rodadas de ginásio preserva callback de autorização e opções. Não é uma transação atômica com o cliente. Fixture do runtime real com objetos/relógio simulados passa a integrar `tools/native_host_tests.py`; nenhum tempo de ataque/escudo/troca foi alterado. A UI deixa explícito que a seleção nativa permanece pendente e remove a promessa simplista de prioridade por tipo. Não há prova em dispositivo; `device_tested=false`. Consultar a PR desta branch para checks finais; não incrementar versão ou publicar artefatos enquanto a integração e homologação exigidas não existirem.

## 22/11/2026 — Ataque Inteligente: transporte da preferência e limite de integração

Na branch de sessão `arena/8b0f505f-pgbr`, baseada em `main` `b355def7f6693b4d7f88c802b6ea40df018f0490`, o slot existente de preferências de batalha passa a emitir F3 com bit independente de Ataque Inteligente; o leitor nativo continua aceitando F2 com ataque desligado. Snapshot inválido, antigo ou alterado durante a leitura é recusado. Os identificadores de instância do motor passam a 64 bits, compatíveis com `ActivePokemonId`; não houve mudança na fórmula, nas cadências nem nos executores. Testes de parsing, controles independentes, backup e identidade foram atualizados. Consultar os checks do HEAD para o resultado.

**Ainda não há seleção inteligente executada:** falta comprovar energia e ligação de cada golpe ao display/ação, além do catálogo por modalidade e confirmação em Android. [Evidências e fontes](docs/AUDITORIA_BATALHA_CURA_2026-11-22.md). A branch é a mesma da [PR #117](https://github.com/LucianoToscano/PGBR/pull/117), imposta pela sessão; código anterior de cura foi preservado, sem novas alterações de cura. Não afirmar integração, teste de gameplay, merge ou release; `device_tested=false`.

## 22/11/2026 — Ataque Inteligente: inventário Drive e metadados do original

Na branch `arena/8b0f505f-pgbr`, [PR #117](https://github.com/LucianoToscano/PGBR/pull/117), a revisão anterior deixou implícito que seria necessário obter o APK original. **Correção factual:** ele já está na biblioteca Drive fornecida. Foram baixadas duas partes e reconstruído fora do Git o APKM 0.437.0 / 2026110902; SHA-256 calculado `5be1ff372d8a3aa1c9ce9f4014b3825119086feaf71aae6b0538636acafebdd3`, igual ao perfil fixado. `tools/game_update.py verify-input` retornou `identity_verified=true`, `gameplay_verified=false`; não foram verificadas assinaturas Android. A extração contém base e split ARM64, e dentro deles estão `global-metadata.dat` e `libil2cpp.so` reais. A proveniência e os catálogos da extração também foram consultados. Não se adicionou conteúdo binário privado ao Git.

Inspeção estrutural exploratória do metadata identificou nomes candidatos de estado, energia e golpes em `BattlePokemonState` e observação/ação em `ChargeAttackDisplayVNext`; nomes e detalhes de hashes/limites estão no [relatório](docs/AUDITORIA_BATALHA_CURA_2026-11-22.md). A inspeção **não** resolveu as assinaturas dos membros nem o fluxo entre MoveId e botão; `get_IsCharged` só não prova identidade. A presença dos binários não é estado de sessão, nem prova semântica da energia, forma ou modalidade. Não houve alteração de executor, hooks, tempos, cura ou escopo nesta etapa; preferência F3 e motor permanecem **desconectados**, com seleção inteligente não implementada. Faltam associação e revalidação por sessão/KO/troca, testes integrados de execução e homologação em Android para todos os modos. Sem `adb`, dispositivo ou KVM nesta sessão; `device_tested=false`.

**Testes desta etapa:** verificação de hash e perfil de entrada; 17 fixtures C++ host e contratos de timings/UI passaram (inclusive motor puro e executores tradicionais simulados; não são teste end-to-end do Ataque Inteligente). Cinco checks da PR no HEAD anterior `34f3adce1223f74b64da958eff91c03fbab63d68` passaram: [código](https://github.com/LucianoToscano/PGBR/actions/runs/45075398923) e [governança](https://github.com/LucianoToscano/PGBR/actions/runs/45075398934). Consultar checks da nova HEAD; sem merge, build de release ou novo APK. O bloqueio restante é prova da cadeia de runtime, não ausência de APK original.

## 22/11/2026 — Regra simples de eficácia e investigação do display

Na branch `arena/8b0f505f-pgbr`, [PR #117](https://github.com/LucianoToscano/PGBR/pull/117), a regra solicitada passa a ser super eficaz > normal > pouco eficaz, empate pelo primeiro botão disponível, sem estimativa de dano, energia ou espera. `smart_attack_priority.h` e `smart_attack_priority_test.cpp` implementam/testam essa comparação pura, reutilizando a tabela de tipos existente e rejeitando inconsistências de identidade e sessão. As nove combinações, prontidão, OFF, tipo duplo e cenários de vínculo/troca foram simulados; **não há ligação ao executor real**. O motor heurístico de #116 foi preservado, mas não é usado pela regra simples.

A investigação do APKM original 0.437.0, verificado por SHA-256, encontrou classificação interna e tipo de carregado no `ChargeAttackDisplayVNext`; análise estática também mostrou que `showEffectiveness=false` pode produzir normal e que a atualização depende de dados guardados. O hook atual encaminha o display antes da atualização original e não repassa o estado, de modo que classificação por objeto ainda precisa de prova de frescor/adversário e ordem dos botões. Detalhes em [auditoria](docs/AUDITORIA_BATALHA_CURA_2026-11-22.md). Nada foi alterado em executores, tempos, cura ou outras modalidades; não se inferiu associação por ordem de captura. **Não declarar correção integrada nem publicar:** faltam provedor validado e testes integrados nos executores, seguidos de QA Android. `device_tested=false`.

Foram executadas 18 fixtures C++ host, contratos de tempos/UI e verificação do input; são testes locais, não seleção no Android. Consultar checks do HEAD na PR; preservar alterações anteriores e demais branches. Sem merge ou release.


## 22/11/2026 — Integração inicial do Ataque Inteligente simples VNext (trabalho em revisão)

- **Base isolada:** branch `fix/ataque-vnext-pos-observacao-20261122` criada a partir da HEAD `b7306354a531fc8acf71868e4c87748a0af13d1b` da PR #117; PR #117 e suas alterações paralelas preservadas. `main` tem commits independentes da PR #122; mesclar/reconciliar apenas depois de conferir HEAD e testes para não perder trabalhos.
- **Foco único:** quando `smartAttack` F3 estiver ON e houver **dois** carregados VNext prontos, coletar indicação Super eficaz > Neutro > Pouco eficaz do próprio display e usar o enum `Charge`/`Charge2` para desempate por primeiro botão, sem alterar as cadências. A leitura só ocorre após `ObserveBattleState` original; o metadata precisa validar `ervu`, `erwh`, `showEffectiveness` e valores dos enums. Se inválidos/desatualizados, preservar a seleção tradicional. `showEffectiveness=false` **não** implica neutro.
- **Arquivos:** `pgrazor/native/combat_host.cpp`, `rocket_combat.cpp`, `rocket_combat_test.cpp`, `smart_attack_priority.h`, `smart_attack_priority_test.cpp`, `charge_effect_ui.h`, `charge_effect_ui_test.cpp`, `tools/native_host_tests.py`, `docs/ATAQUE_INTELIGENTE_SIMPLES_VNEXT_2026-11-22.md`, Mestre/CHANGELOG.
- **Limitação:** `BattleState` compartilhado oferece token de coesão, **não** certeza sobre identidade de adversário ou categoria já recalculada; as verificações de prompt não dão prova de gameplay. Apenas executor Rocket/PvP/Treinamento VNext (quando escopo corretamente reconhecido), não Legacy, Raid, Gym ou Max. Os testes host são com objetos simulados; compilações CI e homologação física devem ser relatadas pelos respectivos jobs antes de qualquer promessa de funcionamento. [Documento técnico](docs/ATAQUE_INTELIGENTE_SIMPLES_VNEXT_2026-11-22.md), [issue #90](https://github.com/LucianoToscano/PGBR/issues/90).
- **Publicação:** nenhum APK, assinatura, SHA ou release produzido nesta branch até aqui; v0.00.40-LT histórica não contém esta mudança. Manter `device_tested=false`, sem merge de PR de implementação enquanto requisitos de revisão/compatibilidade e CI não forem satisfeitos.


### Recibo de CI da PR #124 — seletor VNext experimental, 22/11/2026

- **PR em rascunho:** [#124](https://github.com/LucianoToscano/PGBR/pull/124), HEAD de código validada `80a3775c3d9d90db895dc252fe1a8fecbe9b17fd`. Uma reconciliação manual preservou a candidata LT041 da `main` (`1530a6a1897f8ec4cbbe3b6986fdc514229ffdda`) e o código da PR #117 sem sobrescrever outros trabalhos; commit de união `0e665cda1b43c220154b57d21ceda8009344f429`.
- **6/6 checks `completed/success`:** [CI de código #45086216380](https://github.com/LucianoToscano/PGBR/actions/runs/45086216380) aprovou testes C++ host incluindo cenários do **executor Rocket real com objetos simulados** (segundo carregado supereficaz, OFF, token inválido, primeiro único pronto, desempate de slot, sequência inversa), compilação seletiva NDK ARM64, compilação de **sintaxe** do `combat_host.cpp` e `rocket_combat.cpp` com Dobby fixado/verificado, `tools/check.py`, Java e instalador Root e website. [Governança #45086216385](https://github.com/LucianoToscano/PGBR/actions/runs/45086216385) também passou. **Não são build de APK, teste de enlace completo nem homologação física.**
- Nenhum merge desta PR, APK, tag, SHA de instalável ou release foi criado por este trabalho. A PR #117 segue independente e em rascunho. Faltam testes em Android com e sem Root, especialmente troca/KO e validade do sinal de eficácia em cada sessão; por isso a issue #90 permanece aberta.


## 22/11/2026 — PR #124 integrada e candidata LT042 separada

- **PR #124 merged:** [commit `828807c15672d31614e1236458cc448927301c73`](https://github.com/LucianoToscano/PGBR/commit/828807c15672d31614e1236458cc448927301c73) incorporou o executor VNext e todos os commits `b7306354a531fc8acf71868e4c87748a0af13d1b` da PR #117. Não duplicar integração da #117. LT041 já estava publicada e seus assets são imutáveis.
- **Branch exclusiva:** `release/lt042-ataque-inteligente-vnext-qa-20261122` baseada na main após #124. Versão `0.00.42-LT`, novo workflow `.github/workflows/pgbr-lt042-release.yml` sem substituir qualquer tag anterior, com APKM original fixado, três pacotes assinados (sem Root, Root, módulo Zygisk), `SHA256SUMS.txt`, verificações de versões iguais e publicação só em main. O workflow LT041 publicado fica sem gatilho push e não concorre com o novo.
- **Compatibilidade e regressões:** testes históricos LT032–LT041 aceitam a nova versão sem reduzir suas asserções de imutabilidade; teste LT042 novo confirma presença do build, os três nomes de pacotes, as políticas nativas, verificador, notas, QA e tags anteriores. CI de código anterior: [#45086216380](https://github.com/LucianoToscano/PGBR/actions/runs/45086216380) e [governança #45086216385](https://github.com/LucianoToscano/PGBR/actions/runs/45086216385). Esses checks verificam código e não são APK nem gameplay.
- **Escopo funcional real:** ON, dois carregados VNext prontos e indicadores classificados e atualizados -> super eficaz > normal > pouco eficaz; empate Charge. Qualquer dado ausente, somente um pronto ou OFF mantém legado imediato. Executor Rocket compartilhado PvP/Treinamento somente com escopo confirmado; outros modos têm executores separados e não adotam automaticamente a prioridade. Sem mudar tempos, escudos e trocas. Em runtime sem metadata reconhecida, permanece a escolha anterior. O token `BattleState` não certifica oponente.
- **Limitações:** `device_tested=false`, ainda faltam teste em Android, troca/KO e escopo correto de PvP. A tela branca não foi resolvida. Manter issues [#90](https://github.com/LucianoToscano/PGBR/issues/90) e [#105](https://github.com/LucianoToscano/PGBR/issues/105) abertas. Não afirmar APK publicado antes de verificar `v0.00.42-LT`, seis assets, hashes e workflow Windows final. Não modificar PRs Sniper/GPS/i18n.
- **Docs:** `docs/ATAQUE_INTELIGENTE_SIMPLES_VNEXT_2026-11-22.md`, `docs/releases/v0.00.42-LT.md`, CHANGELOG, README, esta entrada do Mestre.


### CI inicial LT042 — falha documental corrigida na branch, 22/11/2026

- [CI #45087636731](https://github.com/LucianoToscano/PGBR/actions/runs/45087636731): cinco checks aprovados (C++ host, ARM64 selecionado, sintaxe do executor, website e governança); o job Java/Root foi interrompido por quatro asserts de **documentação/contrato**: prefixo exato `PGBR-v0.00.42-LT` em README (dois testes), expectativa antiga de push `branches: [main]` na workflow LT041 congelada e grafia literal `issue #90` nas notas LT042. Corrigidos README, teste histórico e notas sem tocar em motores, temporizadores ou sistema de assinatura. **Reexecutar CI completo da HEAD corrigida antes de merge e release; a falha original não deve ser ocultada.**
