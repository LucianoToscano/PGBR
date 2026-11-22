# PGBR LT032 — Matriz de paridade PGRazor 0.3.5/0.3.6, Eventos e testes pendentes

**Auditoria:** 22/11/2026. Upstream `jpitty03/PGRazor` último release `v0.3.6` (21/11/2026); 32 commits / 90 caminhos alterados na comparação `v0.3.4...v0.3.6`. São **arquivos**, não 90 funcionalidades. Base de QA do PGBR: `fix/lt032-vgp-fallback-icon-20261122` commit `954e4c0c53e32cf853b261cdceb5da83f94a8bf9`. Este documento pertence ao PR #66, derivado dessa HEAD; não é uma confirmação de teste físico.

## Diagnóstico da aba Eventos

O botão **Eventos** está efetivamente criado em `QuickMenu.java`, bit estável **512**, e chama `dev.pgrazor.events.EventPanel.showSelector`. A página `SettingsPanel.buildEventsPage` oferece **Halloween 2026**, **Go Wild November 2026** e **Evento Rota Doces Eternatus** (a terceira opção é extensão específica do PGBR). O formulário de personalização em Exibição e desempenho usa `QuickMenu.CUSTOMIZABLE` na mesma ordem; item Eventos é índice **9**.

Antes do PR #66 o usuário podia ocultar o atalho sem caminho explícito dentro de Eventos para restaurá-lo. O próprio controle do menu rejeitava o bit **8192**, usado por **Selector Pokémon**, e a carga de preferências aceitava apenas máscara **8191**. O PR #66 oferece botão de acesso direto ao seletor de eventos, opção explícita para restaurar Eventos no menu e política de visibilidade **16383** validada em JVM. Não ativa coleta, automação ou golpes por alterar visibilidade.

### Eventos e seus contratos

| Evento | Código encontrado | O que faz de fato | Ainda não demonstrado |
| --- | --- | --- | --- |
| **Halloween 2026** | `EventPanel.java`, `EventSettings.java`, `native/events/event_collector.cpp`; arquivos nativos idênticos ao PGRazor upstream | Abre caixas `TAPPABLE_TYPE_BOX` visíveis e ignora encontros apenas sob sua propriedade; opção começa desligada e requer capacidade publicada pelo runtime; **não captura** Pokémon | Abertura de caixas no Android, responsividade, disponibilidade da classe/gancho no jogo atual; evento sazonal pode não oferecer caixas |
| **GO Wild — novembro de 2026** | `EventSettings.java` idêntico ao upstream, integração `Control.java` | Preferência independente VGP+/Auto Ball para priorizar Safari Balls em Mighty, com fallback à regra normal quando indisponível | Inventário real, validade da bola para encontro real, alternância independente em aparelho |
| **Rota Doces Eternatus** (custom PGBR) | `EventPanel.showEternatus`, `EternatusRouteRunner`, `ResearchSettings` | Importa GPX, escolhe pesquisa observada, espera cooldown, permite retomar em coordenada e avançar sem contar doces não confirmados | Reconhecimento e resgate verdadeiro de recompensa, deslocamento/cooldown no aparelho, 25 doces em missão real |

**Como acessar no APK candidato:** Menu Pokébola → **Eventos**; ou Configurações → **Eventos** → “Abrir seletor de Eventos”. Caso o atalho não apareça, acionar “Mostrar Eventos no Menu Pokébola” na página Eventos ou reabilitar na personalização do menu. Reiniciar e reconferir. A tela não garante que o recurso sazonal esteja ativo nos servidores.

## Paridade oficial — novidades PGRazor v0.3.5 e v0.3.6

| Novidade do release oficial | Situação no código PGBR LT032 | Verificação pendente |
| --- | --- | --- |
| Mateo: troca de presentes na Rota | Portado, `route_gift.cpp/h` idênticos à HEAD upstream | RPC/rota/movimento/limites físicos |
| Busca nas configurações por nome, descrição e termo | Portada em `SettingsSearch` + `SettingsPanel`; implementação PGBR distinta em pontos de apresentação | Pesquisa, rolagem e controles ocultos em Android |
| Visual redesenhado e agrupamento Play/Automation/App | **Adaptado** com tema/painéis próprios; `SettingsUi`, `FeedTheme`, `MapTheme`, `QuickMenu` não idênticos; preserva identidade PGBR | Auditoria visual de paridade e acessibilidade |
| Auto-Raid com passe gratuito novo | Portado em controlador nativo | Concessão e consumo de passe reais |
| Rotas manuais não bloqueadas por erro de posição | Portado em `routes_host.cpp` | Rotas físicas |
| Joystick não volta ao centro durante toque / trava PR #10 | Ajustes portados, além da trava com badge de PR upstream ainda **aberto** | Gesto, 5 estilos, interferência de câmera, falha de encerramento |
| Captura do mapa usa regras de transferência | Portado | Captura/transferência reais; não testar com Pokémon valioso |
| Limite de IV de conservação 0–100% | Portado com transporte e modo legado 101 próprio PGBR | Limites, proteção e confirmação sem perda irreversível |
| Filtro de pesquisas que recompensam doces / Pokémon | Portado nos modos `PogoFeed` | Fonte autorizada e respostas reais |
| Estatísticas 1h/24h da sessão | `SessionStats` / `SessionStatsHistory` e nativo idênticos; o `SessionStatsCard.java` separado do PGRazor é **substituído** pela exibição PGBR em `SettingsPanel` | Coleta de XP, poeira, capturas, encontros, km, paradas e ovos em runtime |
| Quick Catch: resumo tela branca após reide/Max/Rocket | Portado `max_catch_flag.h` + correções de host | Fechar resultados em encontro real |
| VGP+: lure, PokéStop, Rota, diário, incenso e remoção do mapa | Portado em `vgp_controller.cpp`, idêntico ao upstream | Aparições reais, remoção, giro/captura e cooldown |
| Hotspots com hora local/offset real | Portado, adaptado em `Hotspots`/diálogo | Horário IANA, interface em local real |
| Ginásio: alimentação, defensor, motivo de recusa, overlay opt-in | `gym_runtime.cpp` idêntico; preferências/mapa adaptados à interface PGBR | Ginásio cheio, berries, deploy e status real |
| Pesquisa: 80 ações em 10 min, pausa e retomada | Portado no controlador `research.cpp` com adaptações próprias | Limite real, fila e regressões |
| Suporte ART + Vector upstream PR #11 | Adaptado no Root PGBR em PR #56, apesar de PR upstream **ainda aberto** | Boot real Android ARM64 com Vector, crash SIGILL e retorno |
| Capturas/screenshots novos do site e `design.md` | Não portados como arquivos do website; **não são funcionalidades do aplicativo** | Apenas eventual atualização de documentação visual |

**Conclusão precisa:** principais recursos funcionais v0.3.5/0.3.6 foram portados, porém **não** foram importados indiscriminadamente todos os 90 arquivos. Alterações próprias da UI/estatísticas mantêm comportamento potencialmente equivalente, **ainda não atestado em dispositivo**. Tampouco o PGRazor possui recurso de priorização supereficaz do segundo golpe PvP nesta linha: o PGBR apresenta a preferência experimental, mas o executor nativo utiliza o primeiro carregado pronto; esta parte **não está implementada**.

## Separação das pendências solicitadas

| Prioridade | Implementação atual | Confirmação necessária |
| --- | --- | --- |
| **P0:** eventos visíveis | PR #66 altera controle do menu, teste JVM e novo atalho | CI e abrir aplicativo |
| **P0:** PvP | Escopo/ataque rápido/carregado/escudo possuem casos simulados; preferência supereficaz não efetivada | Eventos de batalha/tipos/energia, decisão do segundo botão e logs em partida de teste |
| **P0:** Root+Vector | APK e módulo compilados, correção ART integrada | Inicialização física, teste com e sem Vector e rollback |
| **P0:** VGP+ | Hooks, estados e PNG OFF recuperado com teste de pixels | Captura/giro e variação dos indicadores em jogo |
| **P1:** feeds/scanner | Filtros e conectores PGBR estão em desenvolvimento paralelo | Origens/credenciais autorizadas, timestamps, TTL, cobertura e resultados reais |
| **P1:** joystick/itens/sprites | Interface própria PGBR + sprites VGP auditados | 5 estilos, swipe/badge, toque no mapa, catálogo por ID, Android |
| **P1:** privacidade do nome | Máscara `*****` e hook visual experimental; sem alteração de conta | Mapa e perfil “Eu”, restauração e evidência em live |
| **P2:** consolidação | PRs independentes; `main` não promovida | Revisão da cadeia/CI, logs e homologação de dispositivo |

### Execução de teste em Android (sem falsear resultado)

1. Instalar somente em dispositivo de QA e confirmar versão/assinatura; preservar APK e configurações anteriores.
2. Abrir Pokébola; buscar **Eventos**, acessar seletor e cada tela. Ocultar e reexibir pela página Eventos, reabrir e reiniciar para confirmar persistência. Repetir com **Selector Pokémon**, sem afetar a entrada Eventos.
3. Halloween com runtime ausente deve recusar habilitar e mostrar indisponibilidade. Com caixa sazonal real, verificar que não afeta toques manuais nem inicia capturas.
4. GO Wild: alternar as duas preferências separadamente e confirmar regras de elegibilidade na presença de Safari Ball/Mighty reais.
5. Eternatus: GPX, missões observadas, cooldown, retomada, opção sem VGP e nenhum doce contabilizado sem confirmação.
6. Em PvP, registrar status de cada ação e botões. **Não marcar golpe supereficaz como aprovado** até implementar a seleção baseada em tipos e energia.
7. Exportar logs sem identificadores de conta, preencher PASS/FAIL e reportar modelo, versão Android, branch, SHA do APK, Root/sem Root e Vector.

Fontes: [releases upstream v0.3.5](https://github.com/jpitty03/PGRazor/releases/tag/v0.3.5), [v0.3.6](https://github.com/jpitty03/PGRazor/releases/tag/v0.3.6), [PR upstream #10](https://github.com/jpitty03/PGRazor/pull/10), [#11](https://github.com/jpitty03/PGRazor/pull/11), [PGBR PR #65](https://github.com/LucianoToscano/PGBR/pull/65), [PR #66](https://github.com/LucianoToscano/PGBR/pull/66). Os testes e o último estado QA devem ser atualizados depois que o GitHub terminar as execuções #45003175809 e #45003175854.
