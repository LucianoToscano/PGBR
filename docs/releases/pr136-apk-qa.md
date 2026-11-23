# PGBR v0.00.43-LT-PR136 — Pré-release QA

Pacote de **teste** gerado da branch isolada `qa/apk-pr136-20261123` do [PR #139](https://github.com/LucianoToscano/PGBR/pull/139), correspondente às alterações integradas do [PR #136](https://github.com/LucianoToscano/PGBR/pull/136) no commit-base `c79500a1`.

- Inclui as melhorias em integração dos forks (gestos de mapa, indicador de emoção do companheiro, controle de visibilidade de ginásios, ordenação da sobreposição IV) e preparação de sessão Max.
- A sessão de Batalha Max **não executa novos ataques automáticos**: sua integração nativa está bloqueada até prova do escopo de batalha, conforme [issue #137](https://github.com/LucianoToscano/PGBR/issues/137).
- Versões sem Root, instalador Root e módulo Root produzidas do mesmo código e entrada Pokémon GO 0.437.0 ARM64 fixada. Artefatos compilados pelo [workflow QA #45255048891](https://github.com/LucianoToscano/PGBR/actions/runs/45255048891).
- Build e verificações automatizadas passaram, porém **não houve validação em aparelho Android** (`device_tested=false`). Use somente para QA, mantenha backup e preserve sua instalação estável.
- Esta pré-release não implica merge do PR #136 ou conclusão das funcionalidades ainda pendentes.
