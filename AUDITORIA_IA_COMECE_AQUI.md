# PGBR — PACOTE MESTRE PARA AUDITORIA POR OUTRA IA
Data: 20/11/2026 · Idioma: PT-BR · Alvo: PGBR v0.00.09-LT
Código-base desta auditoria: `68657567e24e251844bc9d28abc3e9cc3edb37cf`.
Repositório: https://github.com/LucianoToscano/PGBR
Branch de distribuição: `feature/lt-hud-historico-locais-20261120`.
Branch de auditoria: `audit/pgbr-lt009-20261120`.

## 0. LEIA ISTO PRIMEIRO
Você é a IA auditora de um aplicativo Android derivado de PGRazor, chamado **PGBR**, com edição PT-BR e recursos de interface/captura/automação. Examine **todo o código-fonte fornecido neste ZIP**, não apenas o README, identificando bugs demonstráveis, problemas de arquitetura e lacunas de integração. Trabalhe com evidências (arquivo, linha, reprodução, efeito, teste e correção). Evite alegar funcionamento real sem executar e validar. Preserve todos os recursos existentes (**enriquecer, nunca empobrecer**). **Não altere o PokeSearch**, nem substitua a versão compilável por preview. Se faltarem binários/insumos privados, documente a limitação; não invente chaves, credenciais ou hashes.

O arquivo ZIP automático de código do GitHub inclui **todos os arquivos rastreados nesta branch**, inclusive Java, C/C++, Python, JS, testes, workflows, recursos gráficos, documentação, esquemas de versão e `upstream-history/PGRazor-upstream-full.bundle`. Não inclui `.git`, build outputs, segredos do GitHub Actions, a entrada original Pokémon GO APKM mantida em draft privado, dados pessoais do celular ou a biblioteca bruta de múltiplos GB do Google Drive. Esses elementos **não são necessários para uma auditoria estática**; compilação final dependerá de insumos legítimos na CI.

## 1. QUESTÃO PRINCIPAL QUE PRECISA SER SOLUCIONADA

**Como finalizar e homologar o PGBR v0.00.09-LT preservando todos os recursos, com proteções contra toques acidentais, ícones reais e completos de itens, privacidade visual verdadeira do nome do treinador, UI legível e evento Eternatus confiável, e publicar os três pacotes Android apenas após os testes?**

**Critério de resposta da outra IA**: entregar tabela ou relato por problema com `STATUS`, `CAUSA RAIZ`, `ARQUIVOS E LINHAS`, `CORREÇÃO EXATA`, `TESTES`, `LIMITAÇÕES`, `RISCO DE REGRESSÃO` e `RESULTADO DA COMPILAÇÃO`. Distinguir: implementado; testado só via mock/JVM; compilado Android; homologado num aparelho real; publicado. Nunca confundir essas categorias.

## 2. PRIORIDADES E PISTAS CONCRETAS

### P0 — Integridade dos recursos e segurança da distribuição
- Nunca alterar a identidade real da conta, autenticação, dados de jogador, ou o código do aplicativo PokeSearch.
- Não excluir recursos antigos da base, perfis, GPX, configurações, modos Root e sem Root, filtros PvP, backups ou documentação. Evitar perda de dados dos usuários. Respeitar licenças de material de terceiros.
- Checar `AGENTS.md`, `docs/BUILDING.md`, `docs/ARCHITECTURE.md`, `docs/ROOTED.md`, `tools/check.py`, `tools/verify_release.py`, e os workflows `.github/workflows/pgbr-build-full-ptbr-candidate.yml` e `pgbr-eternatus-incremental.yml`.
- Usar a **mesma** chave de assinatura configurada no GitHub Actions; nunca criar ou divulgar uma nova.

### P1 — VGP+ ativando sem querer (correção da v0.00.09-LT, verifique)
**Pedido do usuário:** opção liga/desliga `Evitar toque acidental no VGP+` em `Go Plus, PokéParadas e Itens → Virtual Go Plus`, **ativada por padrão**. Quando o ícone flutuante estiver desligado, tocar abre confirmação antes de começar a capturar Pokémon e girar PokéParadas. Cancelar não faz nada. Quando ligada, **desligar tem que ser imediato** com um toque. Quando a proteção está desligada, deve voltar a alternar direto. A automação Eternatus pode ligar a VGP durante as paradas sem aparecer o diálogo a cada parada.
**Onde verificar:** `pgrazor/src/dev/pgrazor/{Pad.java,Control.java,VgpController.java,SettingsPanel.java,OverlayDrag.java,SettingsBackupRules.java}`; `pgrazor/test/{ControlSettingsTest.java,ControlSettingsReloadTest.java,SettingsBackupRulesTest.java}`.
**Verificações:** toques/dobrados, arrasto não tratado como toque, dialog duplicado, mudança de conta/reabertura, estado runtime sempre OFF em reinício, cancelamento, overlay oculto, acessibilidade, diálogos em rotação/tela fechada, proibição de capturas/giros antes da confirmação, execução do coordenador automático. Os **fallbacks** ON/OFF/ERROR de `vgp-device-*.png` foram corrigidos; confirmar estados renderizados em aparelho.
**Status observado:** código integrado e testes incrementais passaram no PR #7, mas ainda exige homologação no Android real.

### P1 — Ícones de itens ausentes embora já existam na biblioteca
**Sintoma:** muitas entradas de itens aparecem com desenhos genéricos ou sem os sprites reais.
**Diagnóstico inicial:** `ItemBadge.java` busca **`assets/pgrazor/item-icons/item-<ID>.png`**, com fallback específico para algumas frutas. O repositório rastreia **37 sprites com ID** em `pgrazor/assets/item-icons/` (e o teste `pgrazor/test/test_item_icon_assets.py` exige exatamente esses 37). A biblioteca Google Drive tem mais itens, variantes e nomes que exigem mapeamento. Uma pasta da biblioteca de origem (`01 - Sprites, imagens, sons e modelos / 04 - Itens, acessórios e roupas / Items`) tinha **164 arquivos diretos**, incluindo 33 `Item_NNNN.png` e muitos `Bag_*.png`, `Egg*.png`, `luckyegg.png`, `Incense*.png`, `Pokeball_sprite.png`, etc. **Presença na biblioteca não significa empacotamento ou mapeamento correto por ID.**
**Referências de biblioteca autorizadas pelo proprietário:**
- https://drive.google.com/drive/folders/1TPgVIGAWkY2Ez1VS3bHAitv9Y_NpczbK
- https://drive.google.com/drive/folders/1EfE-GUyKPkZa_b-f92CRXQghwy9byvpl
**Solicitação:** auditar todos os IDs realmente vindos do catálogo de itens, construir manifesto `ID ↔ sprite de origem ↔ nome canônico ↔ licença ↔ asset final`; incluir variantes quando necessário e fallback consistente. Verificar dimensões, transparência, nitidez, amostragem e cache. Atualizar `build.py`, `nonroot.py`, `rooted.py` e testes para empacotar **todos os recursos pertinentes**, sem incorporar material sem permissão e sem aumentar desnecessariamente o APK. Separar falha de leitura de catálogo da falha de renderização. NÃO inventar IDs.

### P1 — Ocultar realmente o nome do jogador no jogo
**Sintoma:** escolher "Ocultar" ou apelido no PGBR não esconde o nome apresentado pelo Pokémon GO no mapa/perfil.
**Causa já documentada:** `Control.visibleTrainerName` e `AccountsDialog` alteram apenas textos dos menus geridos pelo PGBR, **não** os widgets Unity do Pokémon GO. Não confundir com solução pronta.
**Leia:** `docs/PGBR_COMPARATIVO_PRIVACIDADE_NOME_APKS_LT006_20261120.md`, `pgrazor/src/dev/pgrazor/{Control.java,AccountsDialog.java,SettingsPanel.java,TrainerAliasPolicy.java}` e as rotinas de conta. Em outras ferramentas, PGSharp apresenta `Hide Trainer Name` e Pokemod `Incognito Mode`, mas NÃO presumir seus hooks sem validação.
**Resultado desejado:** 3 modos: original, ocultar na tela, substituição local por apelido fictício. Interceptar APENAS a renderização final de texto relevante (mapa e perfil `Eu`, depois outras superfícies verificadas), **nunca** `PlayerService.get_PlayerName`, login, ID, RPC ou dados salvos. Falha segura (não falsificar estado de privacidade), restauração ao desabilitar, teste de screenshots e vídeos para superfícies prometidas. Se hooks Unity verificados para a versão suportada não estiverem disponíveis, declarar a funcionalidade pendente.

### P1 — Evento Eternatus automático, sem avanços falsos ou travamentos
**Objetivo do proprietário:** rota GPX com pesquisas corretas que entreguem doces Eternatus; automatizar tanto quanto os mecanismos existentes permitirem, passar à próxima parada após confirmação nativa real do resgate, sem ficar em loop nem interromper a rota só por minimizar o painel visual. Controlar VGP+, spin e grade Excelente conforme o estado, sem burlar cooldown.
**Onde verificar:** `EternatusRouteRunner.java`, `EternatusMission.java`, `EternatusProgress.java`, `events/EventPanel.java`, `ResearchSettings.java`, `ResearchPreferences.java`, `Cooldown.java`, `Control.java`, `GpxRoute*.java` e o owner nativo de pesquisa. `pgrazor/test/test_eternatus_route_java.py`.
**Estado atual:** `EternatusRouteRunner.tick()` aguarda `Cooldown.remaining()` e assentamento de 3,5 s após teleporte; reativa controles disponíveis e fica aguardando `onClaim(exactTemplate, questId)` emitido pelo proprietário nativo da pesquisa. Avança somente com ID de resgate confirmado e diferente. **Confirmação de resgate não comprova a quantidade de doces**; tela de início requer verificação humana da recompensa. `EternatusProgress` conta resgates locais. Verificar a ligação real `native owner → EternatusMission.onResult(...) → EternatusRouteRunner.onClaim(...)`, o caso de quests já completas, paradas sem missão, sem PokéStop, limite de pesquisas, perda de conexão, retorno ao foreground, concorrência com outras automações, recuperação após erro e conclusão.
**Critério:** nunca avançar com base em legenda de missão ou contagem presumida; nunca repetir resgate da mesma instância; preservar estado e cancelar com justificativa; em caso de minimização **apenas da janela PGBR**, preservar rota se o jogo permanecer em foreground; se jogo sai realmente do primeiro plano, respeitar política de segurança existente.

### P2 — Joystick e Pokébola Eternatus, tempestade e transparência
**Pedido:** joystick transparente em área entre aro externo e centro móvel; usar vórtice no espaço transparente e tempestade com raios por dentro e por fora; controle de efeitos e desempenho. Pokébola com ícone correto, intensidade temática, sem obscurecer mapa nem confundir o centro. Revisar densidades DPI, bordas e renderização.
**Onde:** `Pad.java` (Stick e launcher), `EternatusStorm.java`, `SettingsPanel.java`, e `pgrazor/assets/map-icons/joystick-eternatus-{base,rim,thumb,vortex}.webp`, `eternatus-ball*.webp`. Diagnosticar sobreposição alpha e FPS em Android real. Não substituir imagens finais por desenhos regressivos.

### P2 — HUD, cooldown, relógio e menu
- Hora: opções discretas de "Só hora", "Hora e data", e formatos com ícone/fuso/bandeira, não forçar ícones.
- Cooldown: número legível em modos **Discreto** e **Chamativo** (sem texto redundante), tamanho/fonte ajustáveis, fundo independente e opacidade sob controle; evitar poluição visual.
- Legendas: visibilidade, cor, opacidade e fundo independentes por indicador; cores forte verde, laranja, rosa choque e vermelho.
- Menus: reduzir duplicidade/níveis profundos, melhorar hierarquia/legibilidade/toques acessíveis, sem eliminar opções; preservar as categorias úteis.
- Coordenadas/histórico: ícones corretos de copiar, mapa, lixeira; confirmar ações destrutivas; favorito, pesquisa e histórico persistentes.
- Imagem da VGP deve encostar no limite direito permitido e ter qualidade/alpha adequadas. Redimensionamento da janela PGBR não pode interromper a rota do jogo.
**Onde:** `SettingsPanel.java`, `Pad.java`, `OverlayDrag.java`, `LocalTimeOverlay.java`, `Cooldown.java`, `MapTheme.java`, `SettingsUi.java`.

## 3. MATRIZ DE TESTES/ENTREGA MÍNIMA
1. Validar fontes: `python -B tools/check.py`; verificar o conteúdo específico dos scripts `pgrazor/test/test_*.py` e testes JVM/C++.
2. Executar os testes existentes, sem confundir mocks com teste end-to-end. Adicionar regressões para qualquer correção.
3. Verificar integridade do diff `git diff --check`, chaves e segredos não expostos, atribuições e licenças.
4. Compilar Android completo usando os insumos privados **somente via Secrets existentes** da CI, reutilizando a chave de assinatura original do PGBR. Gerar/validar `PGBR-v0.00.09-LT.apk`, `PGBR-v0.00.09-LT-Root.apk`, `PGBR-v0.00.09-LT-Root-module.zip`, `SHA256SUMS.txt`, `build-info.json` e `root-build-info.json`. Não disponibilizar `game.apkm` original nem chaves em artifacts.
5. Verificar a instalação real e uso em dispositivo Android (versão suportada, permissões, contas, perfis, VGP, spinner/captura, joystick, modos root/não root), documentando precisamente o que não pôde ser testado.
6. Publicar GitHub Release apenas após todo gate verde; preservar releases anteriores, backups e `main` até aprovação.

## 4. STATUS DE REFERÊNCIA
- Último pré-lançamento efetivamente confirmado em 20/11/2026 durante a investigação: **v0.00.08-LT**, com 6 assets, inclusive APK sem root de 188.240.406 bytes.
- A branch alvo contém código `v0.00.09-LT`, com proteção VGP integrada via PR #7; a execução incremental concluiu com sucesso em 20/11/2026: https://github.com/LucianoToscano/PGBR/actions/runs/44817127278 .
- Compilação completa LT009 iniciada em https://github.com/LucianoToscano/PGBR/actions/runs/44817127414 . Verificar conclusão e Release antes de anunciar a LT009 como publicada.
- Implementação de privacidade Unity de nome: **pendente**. Ícones de itens: existem 37 sprites com IDs empacotados, mas cobertura completa do catálogo da biblioteca: **pendente**. Automação Eternatus: presente, end-to-end no jogo **a validar**.

## 5. ENTREGA ESPERADA DA OUTRA IA
- `RELATORIO_AUDITORIA_PGBR_LT009.md` — achados priorizados por gravidade, com evidências arquivo:linha, reprodução, causa raiz, diffs sugeridos, testes e limites.
- `PLANO_CORRECOES_SEM_REGRESSOES.md` — fases independentes e quais arquivos tocar.
- Alterações no código com testes novos, somente após evidência/validação.
- `RELATORIO_COMPILACAO_PUBLICACAO.md` — SHA/branch, resultados CI, nomes e hashes dos pacotes, links reais e uma seção explícita "não testado no Android".
- Não modificar PokeSearch. Não expor dados particulares nem certificados/segredos de distribuição.

## 6. LINKS ÚTEIS
- Repositório: https://github.com/LucianoToscano/PGBR
- PR #7: https://github.com/LucianoToscano/PGBR/pull/7
- Release LT008 verificada: https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.08-LT
- Orientação de build/credenciais: `docs/BUILDING.md`.
