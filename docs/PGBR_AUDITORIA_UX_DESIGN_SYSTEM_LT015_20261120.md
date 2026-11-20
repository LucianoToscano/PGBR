# PGBR LT015 — Auditoria de UI/UX, design system e arquitetura

**Data:** 20/11/2026. **Regra:** SEMPRE ENRIQUECER, NUNCA EMPOBRECER. **Estado:** diagnóstico técnico, aplicação incremental parcial e plano de execução. Não é certificação visual ou de jogabilidade no Android.

## 1. Método e escopo

A análise combina design centrado no humano, heurísticas de Nielsen, leis de Hick/Fitts/Jakob, consistência com estética Pokémon GO, acessibilidade e arquitetura Android real — sem atribuir esta avaliação pessoalmente a terceiros. Foram lidos os módulos Java do PGBR e o guia do catálogo PokemonGo Geral no Drive. **Não foi feita execução de APK de concorrente, profiling de FPS nem inspeção integral de cada sprite**. As notas são heurísticas, não resultado de teste com usuários.

### Pontuação provisória (0–10)

| Área | Nota | Evidência e prioridade |
| --- | ---: | --- |
| Alcance funcional | 8,0 | Amplos recursos de mapa, rotas, busca, captura, VGP, batalhas, automações |
| Navegação e localização de opções | 6,5 | Há páginas e busca global; grupos longos não distinguem sempre ação de configuração |
| Coerência de tema claro/escuro | 6,0 | Paleta de chrome copiada em quatro classes; tokens agora compartilhados na LT015 |
| Ergonomia dos overlays | 6,0 | Muitos elementos podem competir com mapa/captura; requer teste em diferentes telas |
| Busca e filtros de Pokémon | 6,5 antes da LT015 | Dex e variantes preservados, mas consulta sensível a acentos/ordem; ampliada nesta branch |
| Acessibilidade | 5,5 | Ações de menu sem descrição contextual, contraste e TalkBack não auditados integralmente |
| Desempenho e robustez | Sem nota | Exige dados de FPS, GC, memória, bateria e compatibilidade do jogo |

### Achados e gravidade

| Gravidade | Problema/risco | Tratamento |
| --- | --- | --- |
| Crítico na reprodução | Automação de evento aparentemente para após completar uma missão | Registrar estado, ação, cooldown, posição n/N, checkpoint, recuperação e condição segura de avanço; testar fluxo real, sem alterar state machine nesta PR de UI |
| Alto | Nome do treinador ainda visível em superfícies do jogo, segundo relatos | Mascaramento apenas visual nas superfícies confirmadas; nunca alterar ID, sessão, RPC, servidor ou login |
| Alto | Toques acidentais em VGP | Verificar trava e gesto explícito; preservar controle e testes existentes |
| Alto | Quatro definições duplicadas do mesmo tema neutro | Delegar a FeedTheme.chrome* sem alterar cores antigas; **feito na LT015** |
| Alto | Falha de busca humana com acentos e palavras reordenadas | Normalizar nomes e aceitar busca composta por tokens; **feito na LT015** |
| Polimento | Cards, botões, recuos e fontes divergentes | Migrar gradualmente para tokens FeedTheme, verificar escala e contraste |
| Polimento | Ícones de itens ausentes/errados em telas | Mapear item ID real → asset único, cache e fallback; integrar Drive após auditoria de licenças |
| Polimento | Ações do Quick Menu sem descrição acessível específica | ContentDescription com texto local e valor atual da velocidade; **feito na LT015** |

## 2. Nova navegação (alvo, não implementada integralmente)

    Launcher do PGBR
    ├─ Menu rápido (personalizável; Configurações sempre acessível)
    │  ├─ Mover: Mapa, Velocidade, Caminhada, Rotas, Teletransportar
    │  ├─ Encontrar: Busca Pokémon, Favoritos, Pontos de interesse
    │  ├─ Executar: Eventos, Automação, Pesquisas
    │  └─ Sistema: Contas, Ocultar overlays, Configurações
    └─ Configurações (busca transversal, situação atual)
       ├─ Localização e mapa
       ├─ Hora, cooldown e legendas
       ├─ Captura: rápida, bolas, frutas, Smart Catch, avançado
       ├─ Pokémon: rastreio, IV/PvP, transferência, proteção, evolução
       ├─ VGP, Poképaradas e itens
       ├─ Batalhas: PvP, Rocket, ginásios, Raids, Max
       ├─ Companheiro, ovos e amigos
       ├─ Automação: Farm, Hunt, presets, ajustes técnicos
       ├─ Eventos: missão, progresso, retomar
       ├─ Aparência e desempenho
       └─ Contas e backup

Preservar cada linha de ação, controle, filtro e configuração original e apenas reparentear visualmente com testes. Não mudar bits de QuickMenu.CUSTOMIZABLE nem serialização Control. Grupos avançados expansíveis, sem ocultação inacessível. Presets só como conjuntos explicitamente revisáveis, com botão de restaurar configuração anterior.

### Wireframe — configuração por intenção

    ┌───────────────────────────────┐
    │ ‹  CAPTURA           ?       │
    │ Busca nesta tela…            │
    │ ───────────────────────────── │
    │ Captura rápida          [●]  │
    │ Smart Catch          [Editar]│
    │ Bolas e frutas       [Editar] │
    │                               │
    │ ▸ Regras avançadas            │
    │ ▸ Exceções e segurança        │
    │                               │
    │ Estado: preparado / indisponível│
    └───────────────────────────────┘

### Wireframe — evento ou rota longa

    ┌───────────────────────────────┐
    │ EVENTO · DOCES                │
    │ Etapa 128 / 550               │
    │ [████████░░░░░░░░░░]          │
    │ Estado: cooldown 00:12        │
    │ Última ação: Poképarada       │
    │ [Pausar]  [Retomar na etapa]  │
    │ ▸ Registro e falhas           │
    └───────────────────────────────┘

Esta UI precisa refletir progresso real e não inventar recebimento de recompensa ou sucesso de captura; 00:00 em verde deve significar tempo de espera estimado concluído, não garantia do servidor. Prosseguir somente respeitando readiness/leases e incerteza das ações.

## 3. Design system — FeedTheme

Não há Compose: os widgets Android são construídos em Java. FeedTheme já contém tamanho, fontes, tokens e drawables. Nesta onda foram introduzidos tokens de **chrome** de cores idênticas às anteriormente usadas, delegados por SettingsUi/MapTheme/QuickMenu.

| Token | Claro | Escuro | Uso |
| --- | --- | --- | --- |
| chromeInk | #19232F | #EBF1F6 | títulos e texto principal no mapa/painéis |
| chromeMuted | #586979 | #9AABBB | descrições e indicadores secundários |
| chromeSurface | #F9FBFD | #141B25 | cards e overlays neutros |
| chromeAccent | #00745C | #45E1BA | seleção e ações principais |
| danger | FeedTheme.danger() | FeedTheme.danger() | risco, erro, transferência e exclusão |
| warn | FeedTheme.warn() | FeedTheme.warn() | cooldown ativo e cautela |

Padrões propostos para próxima onda: título 20–24sp, seção 14–16sp, linha 14–16sp, apoio 12–13sp; raio 10–14dp; espaçamento de 4/8/12/16dp; alvo de toque preferencial de 48dp; elementos de status entre 24–36dp quando não forem interativos. Testar uiScalePercent sem cortar conteúdo, contraste AA quando pertinente e orientação/recortes/IME.

**Overlays:** independentes, arrastáveis/recolhíveis e sem bloquear os gestos de arremesso; relógio com/sem ícone; cooldown compacto/discreto/destaque, amarelo enquanto aguarda, verde quando estimativa chega a 00:00; fundos e opacidades por overlay; contador visual mínimo com rótulo acessível; posição persistida e limites de viewport.

## 4. Assets do Drive: origem mapeada

Raiz: https://drive.google.com/drive/folders/1TPgVIGAWkY2Ez1VS3bHAitv9Y_NpczbK

| Módulo | Origem verificada no catálogo | Regra |
| --- | --- | --- |
| Pokédex e busca | 01 / Pokémon completos (Normal, Shiny, formas e trajes) | Chave por Dex + forma + traje + gênero + shiny; fallback sem troca de identidade |
| Tags e tipos | 01 / 03 - Ícones e símbolos / PokeMiners - Ícones de tipos | Ex.: POKEMON_TYPE_WATER_BORDERED.png; manter texto de tipo além da cor |
| Quick Menu | 01 / 03 - Ícones e símbolos / Menu Icons | Mapear por função estável; preferir MenuIcon atual até correspondência verificada |
| Captura e VGP | 01 / 04 - Itens, acessórios e roupas / Items | Ex.: ultraball_sprite.png, wildball_sprite.png; reconhecer IDs antes de substituir |
| Auto Farm/Hunt e feeds | 01 / Pokémon completos e 02 / Dados, rankings e leitura de inventário | Miniaturas com cache LRU e lazy loading, fonte e data explícitas |
| Rocket | 01 / 04 / Items | Ex.: TR_hideout_icon_leader.png, TR_hideout_icon_giovanni.png |
| Ginásios e Raids | 01 / 06 - Layouts, menus e interface / Raids and Gyms | Prévia estilística, evitar copiar layout ou redistribuir asset sem direito |
| Overlays/joystick Eternatus | 01 / 05 - Efeitos e animações | Prévia de baixa carga; nunca animar sem controle de intensidade e bateria |
| Mapa | 03 / Mapas, radar e fontes de eventos | Mostrar origem, atualização e limites da fonte; sem falso status em tempo real |

**Pendência**: localizar arquivo exato, hash, dimensões, suporte de transparência e termos de uso antes de inserir em APK. O Drive contém muitas versões históricas; escolher a adequada por prova visual e metadados, não por nomes aproximados.

## 5. Benchmark (hipóteses a validar em APKs/prints autorizados)

| Referência | O que avaliar e adaptar como boa prática | Oportunidade no PGBR | Verificação |
| --- | --- | --- | --- |
| PGSharp | Acesso direto a mapa/teleporte e menu de uso frequente | Menu rápido personalizável já existe; tornar grupos mais óbvios | Pasta PGSharp localizada; APK comparativo não executado |
| iPogo | Redução de passos entre ação e ajuste | Presets e últimas ações, sem esconder configuração fina | Inspeção binária/prints pendente |
| Pokemod (Atlas/Aegis) | Feedbacks discretos e controle por contexto | Diferenciar o que é status, automação e comando manual | Inspeção pendente |
| PGTools | Visibilidade de lotes e operação em andamento | Progresso, checkpoint, pausa e razões de exclusão | Inspeção pendente |
| Shundos/live feeds | Filtros de espécie/IV/Shiny com contexto | Fonte, data, confiabilidade e motivo da recomendação | Inspeção pendente |
| PGBR atual | Flexibilidade, rotas, filtros, modos Root/sem Root, ponte Control | Preservar funções, preferências e verificações existentes | Confirmado em código; device pendente |

Não afirmar superioridade funcional ou equivalência técnica antes de reproduzir tarefas com as mesmas versões dos aplicativos.

## 6. Microcopy — antes / depois

| Antes (técnico ou extenso) | Depois (PT-BR curto) | Quando mostrar explicação |
| --- | --- | --- |
| Virtual Go Plus | VGP | "Gira Poképaradas e tenta capturas automaticamente" no detalhe |
| Autowalk | Caminhada automática | Mostrar velocidade ativa |
| In-game Routes | Rotas do jogo | Distinguir de GPX |
| Catch chance & ball choice | Bolas e chances | Detalhe dos critérios |
| Transfer on catch | Transferir ao capturar | Avisar sobre proteções |
| Always keep | Sempre proteger | Mostrar regras ativas |
| Keep for PvP | Proteger para PvP | Ligas e rank |
| Quick menu entries | Atalhos do menu | Ordem e visibilidade |
| Battery usage | Bateria e desempenho | Qualidade/FPS avançado |
| Hide overlay | Ocultar elementos | Instrução de reexibição |
| Coordinates & speed | Local e velocidade | Coordenada e km/h |
| Advanced | Opções avançadas | Recolhido quando apropriado |

Não renomear chaves técnicas/API nem traduções geradas à mão; aplicar textos via dicionário pgrazor/i18n/pt-BR.json e regeneração prevista pelo projeto.

## 7. Três ondas de implementação

1. **Vitórias rápidas (LT015):** paleta de chrome centralizada, descrições acessíveis no Quick Menu, busca de Pokémon tolerante a acentos e tokens com testes. Próximos: revisão de microcopy e contraste em telas reais.
2. **Estrutura:** seções por intenção, presets revisáveis, resumo dos filtros ativos, navegação 1–2 toques, estados de automação e controle de overlay. Guardar snapshot/migração de preferências e verificar cada função existente.
3. **Premium:** assets oficiais/verificados, placeholders e cache sob demanda, microinterações opcionais de baixo custo, benchmarks instrumentados e telemetria local não sensível. Sem novos downloads bloqueando a UI.

**Gates:** python -B tools/check.py; python -B pgrazor/build.py; regressão em Android Root e sem Root; screenshots tema claro/escuro e pequena/grande escala; TalkBack, orientação, overlay e toque; testes de auto-transfer com inventário simulado; comparar chaves públicas/persistidas antes/depois. Aplicações em jogo e servidor exigem evidência separada. Não publicar APK ou merge na main sem esses gates.

## 8. Checklist "nunca empobrecer"

- [x] Nenhum recurso removido por esta branch.
- [x] Nenhuma entrada do Quick Menu removida ou reordenada.
- [x] Bits de visibilidade do menu preservados.
- [x] Chaves de seleção de formas/variantes e configurações não alteradas.
- [x] Control/JNI, offsets, state machines e regras de transferência não modificados.
- [x] Paleta neutral anterior mantida com fonte central.
- [x] Pesquisa por nome, #Dex e categorias preservada e ampliada; testes criados.
- [ ] Compilação e suíte CI confirmadas na branch LT015.
- [ ] Teste de tela com tema claro/escuro, uiScalePercent, cutouts e TalkBack.
- [ ] Benchmark de APK/prints de concorrentes concluído.
- [ ] Hash/proveniência/dimensões de cada sprite escolhido validados.
- [ ] Manual Root e sem Root homologados.

**Arquivos relevantes:** pgrazor/src/dev/pgrazor/{FeedTheme,SettingsUi,MapTheme,QuickMenu,PokemonOptionFilter,SettingsPanel}.java e pgrazor/test/PokemonOptionFilterTest.java.

**Referências do Drive:** guia "Guia de aproveitamento - PGBR e PokeSearch", "Mapa_de_recursos.txt", catálogo por pastas. Condições de licença devem ser conferidas por arquivo.