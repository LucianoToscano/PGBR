# PGBR LT032 — Protocolo de homologação Android (Root e sem Root)

## Pacote candidato mais recente (comprovado por hashes)

Revisão QA: PGBR `v0.00.32-LT`, Pokémon GO `0.437.0`, commit `954e4c0c53e32cf853b261cdceb5da83f94a8bf9`, GitHub Actions [#45000567415](https://github.com/LucianoToscano/PGBR/actions/runs/45000567415), artifact `14745906741`. SHA do APK não Root: `bad3febe5988f627633704be31099fdba430f37e1e823b5d390a4149fa1d4e5a`; instalador Root: `286cbbc3d04d1ee8c7cb167e94947885787f2779405862ec2d3ae3df9bf05cd1`; módulo: `b4887fb7fba58e251aeaf5589a649080f1574c98d35b24f74c1581ceba600093`. É QA: `device_tested=false`. A variante mais antiga não tem o fallback VGP original reconstruído.

> **Status:** lista de execução para dispositivo real. O CI passou nas etapas explicitamente marcadas no Mestre, mas **nenhum resultado abaixo é automaticamente um teste físico aprovado**. Registrar modelo, Android, arquitetura, versão do jogo, commit, SHA-256 e horário antes de preencher.

## Preparação e recuperação
- [ ] Salvar APK atualmente funcional, versão, certificado, preferências e caminho de retorno. Não desinstalar antes de confirmar que uma atualização assinada é possível.
- [ ] Conferir hash SHA-256 do download contra o `SHA256SUMS.txt` **da mesma execução** (não reutilizar recibos antigos).
- [ ] Em uma conta de QA e sem transferência automática de dados irreversíveis, iniciar variante **sem Root**; registrar log caso feche. Nunca testar primeiro com conta principal.
- [ ] Em aparelho de QA Root, instalar módulo separado, reiniciar e testar com **Vector desativado**. Depois repetir com **Vector ativado**, verificando eventual conflito ARM64 ART.
- [ ] Em qualquer falha de abertura, coletar log de crash e versão Android; desligar módulo e restaurar build anterior antes de novo ensaio.

## Interface, movimentos e VGP+
- [ ] Confirmar menu Pokébola PGBR, SPG e joystick, sem controles duplicados; testar transparência, cantos e limites da tela.
- [ ] Verificar 5 estilos de joystick, inclusive Eternatus; acionar cadeado após 250 ms, caminhar e soltar com toque; perder foco, desativar e reabrir sem movimento preso.
- [ ] Testar movimento relativo à câmera após mudança de heading e intervalo de 1,5 s.
- [ ] Verificar VGP desligado, ligado e erro, inclusive **fallback OFF** com atlas desabilitado em ambiente de QA; sem quadrado invisível ou PNG truncado.
- [ ] Ativar VGP manualmente; comprovar giro e captura por eventos reais e inventário, não apenas por um status “ativo”; desligar e confirmar parada.
- [ ] Testar Quick Catch em encontro normal, Rocket, Reide e Max sem mapa em branco; preservar ações de confirmação do jogo.

## Batalha (uso controlado)
- [ ] Em desafio PvP de teste, alternar ataques rápidos, carregados e escudos **separadamente**; garantir que “desativar” não bloqueie recursos errados.
- [ ] Observar pelo menos 3 trocas de oponente e 2 golpes carregados: comparar priorização supereficaz com energia real; não declarar o seletor concluído sem prova dos tipos.
- [ ] Testar estados “sem botão carregado”, “escudo oferecido” e “batalha encerrada”, sem ficar parado enquanto recebe dano.
- [ ] Testar Rocket / Ginásio / Reide / Max por separado. Documentar opção, ação, evento de saída e log de falha.

## Pesquisas, privacidade e dados
- [ ] Verificar busca local e global por espécies, Candy, itens e coordenadas: fonte identificável, timestamps, TTL e nenhuma posição inventada.
- [ ] Verificar teleporte apenas a partir de resultados legítimos e autorizados; respeitar cooldown e restrições do jogo.
- [ ] Verificar ocultação **somente visual** do nome por `*****` tanto no mapa quanto no perfil “Eu”; restaurar ao desligar sem modificar login, ID ou nome servidor.
- [ ] Verificar ícones de itens, berries, sprites, shiny e overlays, sem trocar imagens de IDs diferentes.
- [ ] Estatísticas de sessão 1h/24h e isolamento entre contas, usando dados observados e sem enviar informações privadas.
- [ ] Testar Mateo, Rotas e Auto-Raid como opções independentes; desligamento restaura comportamento manual.
- [ ] **Transferências IV:** testar somente em conta de QA com Pokémon descartáveis; confirmar lista protegida e limite 0–100%. O modo 101 exige confirmação. Transferência é irreversível.

## Critérios de aceite e bloqueios
Aceitar uma release apenas depois de registrar **PASS/FAIL real** para cada item aplicável, hash do artefato, ABI, versão do jogo e evidência de dispositivo. Qualquer falha em login, início Root, ações PvP, VGP, assinatura, travamento de movimento ou perda de dados bloqueia a promoção. Feeds PGSharp/Pokémod/iPogo precisam de API/conta/redistribuição autorizadas; a ausência desses direitos não pode ser “corrigida” contornando autenticação.

**Resultado por preencher pelo operador físico:** dispositivo: ____ · Android: ____ · modo: ____ · build SHA: ____ · pacote SHA-256: ____ · PASS/FAIL/LOG por cenário: ____.
