# LT036 — diagnóstico da instalação e auditoria de treinamento

## Evidência apresentada
A primeira captura mostra PGBR-v0.00.35-LT.apk (cerca de 189 MB) selecionado no gerenciador de arquivos e diálogo Android "App não instalado" exibindo Pokémon GO. A segunda captura mostra treinamento com líder de equipe, Lickilicky CP 1500 versus Sneasel CP 1465, com botões ágil/carregado; não mostra o PGBR em funcionamento. **Não há prova de que o PGBR LT035 esteja rodando nessa batalha.**

## Bloqueio de assinatura, confirmado no gerador
O não Root mantém pacote `com.nianticlabs.pokemongo` (`pgrazor/nonroot.py`) e o re-assina com a chave do projeto. Se já estiver instalado Pokémon GO/PGSharp com outra assinatura, o Android recusa a atualização. Isso **é uma causa provável**, não diagnóstico confirmado pelo diálogo, pois o erro exato não aparece na captura.

Para descobrir o erro de instalação sem apagar dados, usar ADB autorizado no computador: `adb install -r PGBR-v0.00.36-LT.apk` e registrar somente o código final. Se houver `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, são certificados diferentes; se `INSTALL_FAILED_VERSION_DOWNGRADE`, é versãoCode; outras falhas podem envolver ABI, armazenamento ou política do aparelho. **Não desinstalar o jogo original automaticamente nem apagar dados da conta.**

Com root/Zygisk e alvo compatível, `PGBR-v0.00.36-LT-Root.apk` possui pacote próprio `dev.pgrazor.root`; o módulo trabalha sobre o jogo oficial assinado, evitando substituição do pacote Niantic. Isso não implica compatibilidade com todo dispositivo.

## Por que o treinamento não era reconhecido
O classificador anterior `pvp_scene.h` sempre descartava a GUI `NpcTrainerInterstitialGui`. Essa GUI também é usada por diálogos de NPC (incluindo Mateo), então não é seguro habilitar o motor só por vê-la.

A LT036 candidata trata o interstitial de `Niantic.Holoholo.Combat.NpcTrainers` como **candidato de 45 s**, não batalha ativa. Só depois de `CombatPreparationRootGuiController` e callback real `CombatVNextGuiController` se abre o executor compartilhado ágil/carregado/escudo (mesmos bits Rocket e PvP). O tipo de evento passa a mostrar `mode=training`. A entrada Rocket validada por `InvasionCombatState` revoga candidatos pendentes; finais/resolução também desarmam. Diálogo Mateo sozinho continua incapaz de enviar ataques.

## Teste de aceitação em Android
Instalar de maneira compatível e verificar versão de Pokémon GO 0.437.0, hooks e módulo ativo; habilitar Ágil no assistente, depois testar Carregado e Escudo separadamente. Durante batalha de treinamento, capturar registro sem dados pessoais e verificar `pvpExecutorReady=true`, `mode=training`, `pvpAction=quick`, dano real, escudo usado e fim da sessão sem novos gestos. Se não houver evento, o caminho real de classes da tela precisa ser observado antes de permitir outro hook.

**Importante:** logs de invocação de método não confirmam ataque aceito pelo jogo; os testes de C++/Python/CI não substituem teste físico. Segundo carregado supereficaz, quaisquer batalhas além dos escopos comprovados e treinamento em variantes não verificadas permanecem pendentes. `device_tested=false`.
