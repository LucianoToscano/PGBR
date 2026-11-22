# Pesquisa PGSharp e correção de ponte Auto PvP — 22/11/2026

**PGBR base:** `1e68fcd3a18e37b26a93a801b2eadd60b5166512`. **Branch:** `fix/pvp-pgsharp-screen-observer-20261122`. **Estado:** revisão de fonte, sem merge, sem APK e sem homologação física.

## Referências do PGSharp e autorização declarada
O responsável declarou possuir autorização para aproveitamento de funções e código. A pesquisa foi direcionada aos arquivos [1.33.0](https://drive.google.com/file/d/1qvz4iYBRGfZZycazYagpxLaXMQqx2d48/view), [1.50.3](https://drive.google.com/file/d/1GuH3JkL77DYC7FfZujcEFxfy4rrafM32/view) e [1.278.0](https://drive.google.com/file/d/13wvFdgVpnVHXsRUftJYjWiyQCzmJDsiU/view), aos [vídeos 13](https://drive.google.com/file/d/1O6OyTOvJcFcKps5Qn9tHgJoqsfU3tTXM/view) e [14](https://drive.google.com/file/d/1CuMTP0q8X2NSV_8wHmogbeGwTIMimz7n/view), e ao relatório de auditoria anterior. O vídeo demonstra HUD de ataque ágil, ataque carregado e escudo no PvP, sem provar execução automática ou sua política.

**Limitação verificável:** o runtime para leitura binária não estava disponível nesta sessão. Não houve inspeção executável ou diff dos APKs 1.33/1.50.3/1.278 nem reprodução nova dos MP4; portanto **nenhuma rotina proprietária foi literalmente copiada**. O presente patch é uma correção original no PGBR, não uma extração atribuída ao PGSharp.

## Defeitos de fonte identificados e corrigidos
1. Em `pgrazor/native/cutscene_skip.cpp`, a tentativa única `tried=true` era consumida **antes** da confirmação de bootstrap IL2CPP. Se o runtime ainda não estivesse pronto, `faulted=true` impedia novas tentativas na mesma sessão. Agora o código aguarda o bootstrap e só depois consome a tentativa de instalar hooks.
2. O mesmo módulo exigia ao menos um hook de **pular cutscenes**, mesmo que `GuiController.Open` — observador que alimenta `pgrazor_pvp_gui_open()` — estivesse validado e instalado. Agora esse observador permanece elegível, e um aviso diferencia a prontidão do trace PvP da indisponibilidade dos hooks opcionais de cutscene.

**Preservado:** `combat_host.cpp`, `rocket_combat.cpp`, `rocket_actions.cpp`, opções Rocket/PvP atuais, ABI, opções de reide/gym, escudos e batalha, hooks verificados, créditos e arquivos históricos.

## Teste e limites
O novo teste `pgrazor/test/test_pvp_trace_bootstrap.py` protege a sequência bootstrap -> tentativa única -> instalação e o caso de observador PvP sem hooks de animação. **Teste local não executado nesta sessão; CI e aparelho ainda pendentes.** Isso não prova que o Auto PvP ataca, lança carregado, usa escudo nem funciona em GBL. O selector supereficaz de segundo golpe permanece sem executor nativo. Não copiar offsets de versões antigas ou fabricar JNI, servidor ou API.
