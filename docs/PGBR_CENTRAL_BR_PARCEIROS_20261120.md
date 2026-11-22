# PGBR Central Brasil — integração de ferramentas e parceiros

**Projeto-alvo:** `LucianoToscano/PGBR` (aplicativo completo baseado no PGRazor), com PokeSearch/PvPPokeGo como fontes de componentes e bibliotecas, **sem substituir o núcleo atual nem modificar seus aplicativos/repositórios originais**.
**Ponto de partida:** LT015, build Root, APK sem Root, módulo Root e testes publicados em 20–21/11/2026. Não anunciar que as propostas abaixo já estão integradas.

## 1. Tratamento dos materiais adquiridos e das autorizações

O responsável informa aquisição, autorização de desenvolvimento e parceria para uso das ferramentas listadas. A documentação localizada no Drive inclui:
- Inventário mestre total, identificado como autorizado para projetos/licença de desenvolvedor;
- Arquivo Mestre Pokémon — Inventário Drive;
- PokeGoBR — Master fontes, APIs, bancos, imagens, sons e APKs;
- Especificação PoGoCoordinates indicada como autorizada.

Esses documentos comprovam a existência do inventário e das referências de autorização declaradas pelo responsável; alguns próprios inventários esclarecem que **não são, por si, contratos de redistribuição de cada fornecedor**. Não apagar nem contrariar o histórico do usuário: vincular cada concessão concreta ao componente, à versão e às condições correspondentes antes de publicar seu código ou binário. Arquivos de licença, aquisição, recibos, keys e credenciais ficam no Drive privado — **nunca no GitHub, APK, testes ou logs**. O registro técnico mantém somente ID não sensível do comprovante, escopo e estado de validação.

**Não confundir direitos com compatibilidade:** mesmo quando a licença permite importar código/SDK/lib `.so`, um APK fechado não é automaticamente um SDK carregável. É obrigatório verificar ABI, versão 0.437.0, dependências, assinaturas, símbolos, IDs de pacote, arquitetura e efeitos de dois loaders concorrentes.

## 2. Papéis: um aplicativo PGBR, sem fragmentação

- **PGBR:** shell Android, identidade visual, configuração única, mapa, joystick, overlay, ferramentas de captura, VGP, rotas, cooldown, segurança de ações e distribuição Root/sem Root. Preservar PGRazor completo.
- **PokeSearch (código próprio):** radar, filtros, alertas, leitura visual, catálogo, biblioteca de sprites/sons, fontes remotas, mapeamento e pesquisa avançada. Importar bibliotecas/rotinas aprovadas ao PGBR ou integração explícita, sem editar/renomear o PokeSearch instalado.
- **PvPPokeGo (código próprio):** cálculo de IV, rankings, ataques, energia, análise de tipos, simulação, observação e assistência de batalhas. Criar interface Kotlin/Java bem definida e testes de cobertura sem mexer no aplicativo original.
- **PokeGoBR / biblioteca do Drive:** índice canônico de dados, multimídia, proveniência, esquemas e website/comunidade, sem criar outro app substituto do PGBR.

## 3. Matriz de tecnologia por ferramenta

| Material adquirido / fonte | Componentes candidatos | Integração no PGBR | Gate técnico |
|---|---|---|---|
| PGSharp 1.277.2 / jogo 0.435.2 | Busca/feed, filtros, `hideTrainerName`, apelidos/IV, VGP, cooldown | Conector de filtros e pesquisa; máscara **somente visual**; importar código/biblioteca apenas dentro do escopo autorizado | Versão diferente de 0.437.0; confirmar símbolos/hook real, identidade intocada, fallback |
| iPogo 27.3 / jogo 0.437.0 | Metadados do jogo, buscas, scanner, overlays, Incognito | Matriz IL2CPP versão 0.437.0; UI/Scanner via interface de capacidade | Metadata v39 ≠ API invocável; verificar tipos/assinaturas e dispositivos |
| iPogo Launcher 27.3 | Pré-flight Root, attach, serviço e diagnóstico | Melhorar inicialização e tela de saúde Root sem duplicar launcher | Dependências `su`, injector, pacote, SELinux e versão |
| Pokemod 14.2.1 | Recursos Root, overlay, inventário, capturas | Conector Android/Root isolado e testes de configuração | Compatibilidade/consentimento/permissões mínimas |
| Pokemod Atlas 25101101 | Mapeamento, serviço, configuração do worker | Monitor de tarefas e estado por fonte, sem converter backend RDM em backend PGBR | Contrato de serviço e autenticação própria; não reutilizar tokens |
| Pokemod Aegis 26073101 | Mapeamento, múltiplos workers, estado e reinicialização | Limites de concorrência, stop seguro, diagnóstico | Nunca iniciar workers externos sem comando do usuário |
| PGTools 6.1.1 (estável + teste) | Regras de itens, bolas, transferência, missões, rotinas/eventos, histórico administrativo | Editores de política, exceções de missões, limites de estoque, sessão de testes | `web.zip` minificado; distinguir configuração de execução; diff da variante teste |
| Regibot 2.5.12 | Private mode (nome visual), 5 esquemas JSON/419 campos, Pokédex/PvP, Stamp Rally, cooldown/histórico, alertas, watchdog Root e controle remoto | Diagnóstico + editor de buscas/presets, check-point de eventos e painel por conta, sem carregar o injector em produção | JNI/Frida ARM64 próprio; jogo-base suportado não comprovado, 141 falhas de decompilação, backend e cobertura de privacidade pendentes |\n| PokeList 1.3.18 | IV A/D/PS, gênero/forma, Rocket, Auto Hunt e alertas | Filtros compostos, listas de espécies, alertas com cooldown e revisão de Rocket | Catálogo com 1.008 espécies não substitui o canônico PGBR (até 1.025) |
| Shungo 1.7.2 | Startup, serviço, busca especializada e fontes de spawns | Conector de resultados apenas se protocolo/autorização da fonte estiver documentado | Binário não oferece automaticamente API dos servidores |
| Velocity 8.8.2 | Seletor de aplicativos, WebView e ferramentas de inicialização | Ajuda contextual, diagnóstico, acesso a documentação/instalação | Evitar WebView para controles críticos e abrir URLs inseguras |
| PoGoCoordinates / LivePokeMap | Pokémon, raids, quests, Rocket, weather, Power Spots, feeds, IV e PvP | **Reaproveitar** `PogoFeed*`/`LiveFeed*` que já existem | Respeitar autenticação, entitlement, TTL, backoff 401/403/429 |
| Game Master / PvPoke / sprites | Espécies, formas, ataques, ranks, eventos, itens, assets | Catálogo canônico + resolver de assets com versão/SHA/proveniência | Não confundir jogo principal com Pokémon GO; manter licenças e créditos |

Os ZIPs fornecidos pelo usuário servem como **artefatos de origem privados**. Para módulos fechados licenciados: preferir SDK/export oficial com documentação; quando somente houver APK/ELF, documentar ABI e bridge testada por versão em laboratório, separando implementação interna de distribuição pública. Não publicar logs de contas nem material de terceiros fora do escopo autorizado.

## 4. Componentes centrais necessários

### A. Registro de parceiros e compatibilidade (`PartnerRegistry`)
Um registro único por fornecedor, componente e licença: identificador, nome, pacote, versão, ABI, suporte jogo 0.437.0, modos Root/sem Root, permissão de redistribuição, documentação técnica vinculada, hash de fonte, estado (`inventariado`, `licenca-vinculada`, `compatibilidade-validada`, `qa-aparelho`, `distribuicao-aprovada`), status de atualização e motivos do bloqueio. Nunca incluir secrets. O registro não transforma autorização declarada em parceria publicamente atestada.

### B. Conector universal de dados (`ProviderAdapter`)
`query(FilterSpec) -> ProviderResult` com modelo canônico (espécie+forma+gênero+costume, IVs A/D/PS opcionais, CP, nível, PvP, geografia, fonte, hora da leitura, expiração, nível de confiança, restrição da conta). Separar `desconhecido` de zero e de falso. O PGBR mantém provedores independentes e não mistura identidades/sessões. Deduplicação somente com chave de spawn confiável.

### C. Busca inteligente única
- **Coleção:** ampliar `PokemonInventorySearchBuilder`, favoritos, histórico local, operadores seguros, idioma do jogo e exportação/importação. `IPokemonFilterService`/Unity só após validação na 0.437.0; copiar/colar continua fallback.
- **Mundo/feeds:** enriquecer `PogoFeedFilters` e `LiveFeedFilters` com IV exato A/D/PS, OR de perfis, gênero, forma, tamanho, espécie, distância e rank; manter payload remoto 21/8 chaves e aplicar filtros PGBR-only localmente. Priorizar limitação de memória e API rate limits.
- **Preferências:** filtros globais reutilizáveis em raids, Rocket, quests, Weather e Power Spots; critérios que não existem no provedor aparecem como `indisponível` em vez de inventar resultado.

### D. PvP e coleções
Integrar `PvPPokeGo/core` por um bridge bem definido para ranking de IV, melhores evoluções e ataques, com Game Master versionado. Evitar confundir ranking de espécie com ranking de IV individual. Diferenciar salvamento manual, snapshot/importação e leitura em tempo real. Proteger dados de conta.

### E. Orquestrador de automações e eventos
Máquina de estados única: `IDLE -> PRECHECK -> COOLDOWN -> ACTION -> CONFIRM -> NEXT -> COMPLETE` com `PAUSED`, `FAILED`, `UNVERIFIED`. Persistir checkpoint `N/total`, justificativa de parada, retomada sem repetir ações irreversíveis, cooldown visual verde quando liberado, confirmação por evidência quando disponível e fallback explícito sem inventar recompensas. Integrar ao `EternatusRouteRunner` existente; não refazer suas rotas. Para transferências/descartes, exigir política de proteção e confirmação.

### F. Segurança, privacidade e monitor
- Máscara `*****` / vazio / apelido **somente de exibição**, com restauração quando desliga. Verificar mapa e perfil `Eu`; não alterar autenticação, ID, PlayerService ou RPC. Identificar estado de cobertura antes de alegar `proteção para live`.
- Root Health: módulo instalado, versão, processo anexado, componente nativo habilitado, falhas e recuperação limitada, controles para parar, diagnósticos sem informações privadas.
- Compatibilidade de serviços: nunca carregar simultaneamente dois loaders que disputem o mesmo processo/hook ou dois VGP/capturadores; proprietário único para cada ação. Backoff de provedores externos.

### G. Biblioteca de assets
`assets/pokemon/{id}/{form}/{gender}/{shiny}/{costume}`, `items`, `effects`, `ui`, `icons`, `audio`, `research`; índice e hashes; fontes e permissões por asset, aliases e resolução de fallback. Não duplicar variantes idênticas, não misturar sprite de item com UI nem asset completo com cortes de olhos. Reciclagem/limpeza somente após comparação hash e backup.

### H. UI/UX Brasil
Tela inicial tipo painel de ação; navegação `Pokémon`, `Radar`, `PvP`, `Raids`, `Missões/Eventos`, `Automação`, `Mapa/Rotas`, `Biblioteca`, `Privacidade`, `Diagnósticos/Parceiros`. Menus com busca e atalhos, agrupamentos colapsáveis, legendas opcionais, recursos demonstráveis, modo discreto e acessibilidade. Preservar a estética do PGBR e o joystick Eternatus; não criar um clone de múltiplos menus sobrepostos.

## 5. Infraestrutura/engenharia

| Camada | Ferramentas necessárias | Saída |
|---|---|---|
| APK Android | JDK 17, Python build já existente, Android SDK 36, `dexlib2` | APK sem Root compatível e assinado |
| Nativo/Root | NDKs fixados, CMake, Ninja, Dobby/LSPlant conforme arquitetura existente, Magisk/KernelSU/APatch | módulos próprios, isolamento e validação de ABI |
| IL2CPP | metadata resolver, mapa de símbolos por versão, testes de assinatura e chamadas não destrutivas | compatibilidade 0.437.0 sem offsets da 0.435.x |
| Dados | catálogo Game Master versionado, JSON schema + modelos canônicos + cache TTL | consultas consistentes e auditáveis |
| Serviços | conectores de provedores autenticados, fila/backoff/quotas, chaves privadas fora APK | feeds confiáveis sem compartilhamento de contas |
| Multimídia | manifesto de sprites, SHA256, licença, atlas e fallback | biblioteca organizada e reutilizável |
| Teste | `tools/check.py`, JVM/unit, native C++, smoke no aparelho, teste Root/sem Root, roteiros e snapshots | sem regressão nas funções existentes |
| Entrega | GitHub Actions, artefatos assinados, SHA256, releases, changelog, rollback e export de preferências | releases identificáveis por versão |

## 6. Ordem de execução

| Fase | Prioridade | Entregável e aceite |
|---|---|---|
| 0. Governança e compatibilidade | P0 | Registry de parceiros, vínculo de autorizações, versões e recursos sem dados privados; lista de contratos técnicos reais |
| 1. Busca única + fonte dos resultados | P0 | Consolidar busca da coleção/feeds, IVs compostos, ranking, filtros por espécie/forma/gênero e favoritos; testes de dados desconhecidos |
| 2. Privacidade real do nome | P0 | Somente após QA: renderização mapa + Eu, ligar/desligar e troca de conta, preservando o nome real |
| 3. Estabilidade de eventos/automação | P0 | checkpoint N/total, confirmação e fallback explícitos, cooldown e retomada sem duplicação |
| 4. PvP e segurança de inventário | P1 | bridge PvPPokeGo, ranking e sugestões, proteção de transferências e de itens, diagnóstico |
| 5. Conectores autorizados dos parceiros | P1 | SDK/source/ABI por fornecedor, contrato testado no próprio processo, versão mínima e fallback |
| 6. UI, assets, alertas e central de atualizações | P1 | Menu Brasil coeso, biblioteca indexada, acessibilidade, alerts opt-in, logs locais |
| 7. Homologação, builds e migração | Gate | CI verde, APK/Root/module verificados, teste Android, hashes, backup e rollback |

## 7. Portões de publicação

1. Nenhuma perda de funções PGRazor/PGBR já existentes ou de opções Root/sem Root.
2. Cada integração real tem um responsável por hooks/ações, com versão, ABI, contrato, licença e fallback documentados.
3. Toda mudança de versão preserva o mesmo pacote/identidade quando necessário; migrações explícitas e backup antes de alterar persistência.
4. Configurações de serviço e dados de contas ficam isolados por perfil; tokens e chaves fora de relatórios públicos.
5. Inventários e snapshots não são provas de disponibilidade do backend; resultados exibem fonte, horário e condição.
6. Privacidade só anunciada após teste real no aparelho; atos irreversíveis têm proteção.
7. Os ZIPs originais e licenças privadas não entram no repositório; apenas o que o contrato permitir, com registro de licença e hash.
8. Publicação de nova versão exige compilação, verificação e download real; não confundir PR mergeado com APK publicado.

## 8. Evidência consultada

- Auditoria adicional do Regibot 2.5.12: `docs/PGBR_AUDITORIA_REGIBOT_2_5_12_20261120.md` (12º registro no manifesto; não integrado).\n- Auditoria estática privada dos 10 ZIPs enviados (PokeList, Pokemod, Atlas, Aegis, iPogo, launcher, PGTools/PGTools teste, Shungo, Velocity), além dos ZIPs anteriores de PGSharp.
- PGBR: `docs/ARCHITECTURE.md`, `docs/PGBR_DIRETRIZ_REPLICA_INTEGRAL_20261118.md`, `THIRD_PARTY.md`, versão 0.00.15-LT.
- PokeSearch: `AuthorizedApiCollector.java`, `PgsFilterImport.java`, `PgsSmartSearchSettings.java`, `PgsAutoHuntPolicy.java`, `PgsMissionLedgerPolicy.java`.
- PvPPokeGo: `core/src/main/kotlin/pvppokego/core/battle/`, `core/data/PvpokeRepository.kt`, `core/vision/`.
- Referências públicas: https://www.pgsharp.com/features/, https://ipogo.app/features.php, https://pokemod.dev/pt, https://pgtools.net/wsoo.php, https://shungo.app/tools e https://pogocoordinates.com/.
