# PGBR — índice permanente de execução e continuidade

STATUS: EM_EXECUCAO
RELEASE_TAG: v0.00.47-LT

## Fonte canônica e regras

O histórico completo permanece em [PGBR_ARQUIVO_MESTRE_UNICO.md](PGBR_ARQUIVO_MESTRE_UNICO.md). Este índice complementa, não substitui nem reduz o histórico. Consulte também [README](README.md), [CHANGELOG](CHANGELOG.md), [catálogo de recursos](docs/catalogo/PGBR_MAPA_RECURSOS_E_EVOLUCAO.md), issues, PRs e CI atuais. O acervo do Drive (pasta Arquivo Mestre, PROMPT-MESTRE-PGBR.txt e autonomo-pgbr.sh) foi localizado em 24/11/2026; consultar somente por acesso autorizado, sem publicar localizadores privados ou credenciais. Não confundir inventário inicial com auditoria integral concluída.

Ciclo: recuperar HEAD e checkpoint; inventariar; priorizar P0 (build/crash/perda), P1 regressões, P2 requisitos aprovados, P3 UI/PT-BR/assets, P4 qualidade/desempenho, P5 entrega/docs; corrigir no escopo autorizado; testar; registrar evidências, arquivo, causa, solução, SHA, PR, versão e próximo passo. Sem force push, sem desativar testes, sem modificar branches de terceiros. Sem atribuir integração ou teste físico a mocks. Respeitar licenças e interfaces autorizadas. A publicação de nova versão, merge e tag estão autorizados pelo proprietário após validação. Não substituir versão publicada. O executor agendado existente foi atualizado para frequência horária sem limite programado de ciclos; não criar executor concorrente. O script CLI é alternativa local, não execução simultânea.

## Checkpoint de 24/11/2026

- [x] PGBR-UI-001 / P1 — layout compacto, Locais unificado e filtros recuperados em código. Causa: divergência da branch experimental. Arquivos: WorldMapDialog.java e test_route_trace_java.py. PR #201, merge 8c644bc33d06d8772b939a0f07b94d4de5f8a542. CI 45479212037 (5/5) e 45479212042 aprovados. Aceite físico separado abaixo.
- [x] PGBR-DOC-001 / P5 — reconciliação documental #197/#198, catálogo e histórico preservados por #202. Main observada 2915b5b15374aff20a141009039d1362e6a106f6. Não implica conclusão das funcionalidades listadas.
- [ ] PGBR-BUILD-047 / P0 — concluir identificação, build QA, checks e entrega LT047. PR #203, HEAD observado 4092c29b7d4759bce8864224527e2a0bad72b1f5, main base 2915b5b15374aff20a141009039d1362e6a106f6, sem atraso. CI código 45482026547 e documentação 45482026278 aprovados; QA 45482026778 em execução, fixture nativo aprovado, etapa APK em testes. Próximo: consultar HEAD atual e resultado exato, verificar artefatos, assinaturas, versão e SHA256, merge somente com checks aprovados; publicação pelo fluxo autorizado, sem inventar links.
- [ ] PGBR-REG-200 / P1 — issue #200: validar ausência de regressões no pacote consolidado e identidade do artefato. Evidência de interface antiga nas capturas, correção de código #201. Próximo: verificar pacote exato; não confundir QA Arena com LT047.
- [ ] PGBR-AUD-001 / P1 — inventário e triagem de todas as issues, PRs, branches e registros históricos, com critério de aceite e deduplicação; recuperar requisitos antigos sem tratá-los como implementados. Próximo: revisar histórico e catálogo em unidades verificáveis.
- [ ] PGBR-ROUTE-199 / P2 — PR #199 isolada, protótipo experimental sem homologação; não sobrescrever branch Arena, não atribuir funcionamento comprovado. Próximo: revisar evidências e escopo autorizado; preservar demais correções.
- [ ] PGBR-RELEASE-047 / P5 — entrega real com versão reservada, tag imutável, artefatos e hashes confirmados no commit aprovado. Não marcar concluída sem evidência.

## Aceite em dispositivo (separado)

device_tested=false; provider_connected=false até evidência correspondente. Tela branca Rocket, Supereficaz, interface e traçado visual não são homologados fisicamente pela aprovação de CI. Não alterar estados por conveniência.

## Próximo ciclo

Verificar PR #203 e run 45482026778, registrar logs/resultado real, preservar o SHA do build em andamento e avançar as tarefas independentes. Antes de declarar STATUS: CONCLUIDO, revisar todos os registros canônicos e obter evidência de entrega real; a mera ausência de checkboxes não basta. A ID de auditoria de um eventual supervisor CLI só pode ser registrada pelo executor real desse supervisor.
