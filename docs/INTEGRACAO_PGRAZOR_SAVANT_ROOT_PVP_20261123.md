# PGBR — adoção conservadora de ideias do PGRazor-Savant (23/11/2026)

Origem consultada: [dojistarman/PGRazor-Savant](https://github.com/dojistarman/PGRazor-Savant), especialmente os commits [PvP b4b9c45](https://github.com/dojistarman/PGRazor-Savant/commit/b4b9c45ba2c0587601fd9229bdaa382876db61bc) e [coexistência Root e8a547f](https://github.com/dojistarman/PGRazor-Savant/commit/e8a547f877f040547258c8e6deb15380beb1c077). Código de origem MIT: manter atribuição e avisos de terceiros.

## Limites de escopo
- Base da integração: `main` `f78d0e2544cdb55a31bdf1dd5cf5b813e2376622`.
- Branch: `feat/savant-safe-root-pvp-20261123`; PR de revisão antes de qualquer merge. Não alterar `main` diretamente.
- Não modificar o motor de decisão de eficácia da LT042, cadências, Ataque Carregado, escudos, troca, Cura, Snipers, GPS, PokeSearch, variantes sem Root ou imagens.
- Não renomear `pgrazor` / `dev.pgrazor.root`: identidade de módulo/instalador legados exigem migração própria e testes de upgrade/rollback; a escolha segura é manter essa compatibilidade.

## Root: verificação fail-closed do módulo PGSavant
- `pgrazor/zygisk/peer_guard.h`: política pura sobre somente três caminhos fixos: a raiz de módulos, o módulo `pgsavant` e o marcador de desligamento `pgrazor/injection-off` dele. Apenas a ausência comprovada de módulo ou um marcador Off regular permite avançar.
- `pgrazor/zygisk/loader.cpp`: consulta o acompanhante Zygisk antes de ler DEX/payload; nenhum caminho/ação é fornecido pelo processo do jogo. IPC com espera limitada a 1,5 s e falha fechada (status desconhecido, falha do IPC, módulo ativo).
- O interruptor original `pgrazor/injection-off` segue prioritário e não muda seu significado.
- Limitação: módulos com IDs desconhecidos não são enumerados; este não é um mecanismo universal de exclusão entre todos os softwares Root. Não resolve por si só a colisão histórica do ID `pgrazor` entre PGBR e upstream.

## PvP: prova complementar, não mecanismo de ação
- `pvp_vsseeker_probe.inc` tenta validar metadados exatos e código da classe `VsSeekerCombatState.EnterState` somente como hook opcional; em erro, conserva motor anterior sem ativação extra.
- O sinal é observado **depois** de executar o método original. `PgrPvpScene::verified_vs_seeker` apenas cria uma prova temporária de até 15 s quando o Hub PvP já está presente. Não abre batalha partindo de IDLE, não converte treinamento, não altera máscara 1/2/4 ou política de eficácia.
- A abertura real ainda exige o controller VNext correto, ausência confirmada de escopo PvE, lease/configurações ativas e motor Rocket/PvP apto.
- O hook depende da assinatura real 0.437.0 e pode não se instalar; somente testes Android determinarão utilidade. O observador não corrige por si o problema de troca de janelas da issue #105.

## Cobertura de regressão
- `peer_guard_test.cpp`: matriz 64 estados de pai/módulo/marcador.
- `pvp_scene_test.cpp`: sem sinal sozinho, Hub + VNext, prova expirada, treinamento preservado e sequência normal de preparação.
- `test_savant_adoption.py`: testes host/estáticos, preservação da identidade Root e isolamento.
- `tools/native_host_tests.py`: executa a fixture Root no Linux.
- `pgbr-codigo-ci.yml`: verifica sintaxe do loader real via NDK ARM64, além do host de combate e suíte `tools/check.py`.

## Evidências ainda necessárias
1. CI de todos os jobs na HEAD exata do PR; não extrapolar compilação para compatibilidade real.
2. Android físico Root: jogo sozinho; PGSavant ausente; PGSavant Off; PGSavant On; estado desconhecido/falha de IPC; troca com jogo fechado.
3. Android físico PvP: Hub→VNext sem Preparação, challenges privados, tela alternada, treino, Rocket, e rejeição Raid/Ginásio/Max.
4. Validar versão/signature, relatório Root, atualização e rollback da LT042 sem perder preferências. `device_tested=false` até homologação.

Nenhum APK novo ou release são produzidos automaticamente por esta PR.
