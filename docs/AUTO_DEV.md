# PGBR AUTO DEV — fase 1: despachante de propostas

Esta fase acrescenta um job ao agendador já existente (.github/workflows/pgbr-continuity-10min.yml). Não cria um segundo agendador. O job é desativado por padrão e não roda em PRs. Não é um executor de código, não controla os chats externos e não altera branches, APKs, releases, permissões ou dados do jogo.

## Configuração (pelo proprietário, depois de CI e merge)

1. Nos [Secrets do repositório](https://github.com/LucianoToscano/PGBR/settings/secrets/actions), criar somente as chaves dos provedores autorizados: OPENAI_API_KEY, GEMINI_API_KEY e ARENA_API_KEY. Não enviar chaves por chat, issue ou commit.
2. Em [Actions Variables](https://github.com/LucianoToscano/PGBR/settings/variables/actions), definir PGBR_OPENAI_MODEL, PGBR_GEMINI_MODEL e PGBR_ARENA_MODEL com IDs disponíveis nas contas correspondentes. Gemini usa apenas o ID sem prefixo models/. Arena usa modelo direto compatível com chat completions e max_tokens; conferir lista da conta. Não presumir modelos gratuitos, acesso ou compatibilidade.
3. Definir limites financeiros e alertas nos próprios provedores. Assinaturas de chat não incluem necessariamente créditos de API. A fase 1 limita chamadas e tokens de saída, não é um teto monetário garantido.
4. Revisar tools/auto_dev_queue.json: contém somente três lotes piloto de propostas (três tarefas cada). Cada item é texto explicitamente aprovado para envio aos provedores; não incluir código privado, dados pessoais, segredos ou APKs.
5. Só então criar a variável PGBR_AUTO_DEV_ENABLED com valor true. Ausência ou qualquer outro valor desabilita o job. Rodar manualmente o workflow de continuidade na main ou aguardar o agendamento. Sem credenciais/modelo o provedor não é acionado.
6. Conferir logs, artifacts pgbr-auto-dev-proposals-<run>-<attempt> e recibos na issue #320. As respostas são propostas não confiáveis, nunca prova de implementação/teste.

## Garantias e limites

- Reserva durável por ID na issue #320, reconhecendo somente o bot do Actions. Uma tentativa por ID, antes da chamada potencialmente paga; novo ID revisado é necessário para retentar uma falha/timeout. Não remover reservas. A concorrência do workflow existente serializa execuções main; chamadas de até três provedores em paralelo.
- Máximo 3 chamadas por ciclo (uma por agente), 4096 tokens de saída solicitados por chamada, 90 segundos de timeout de rede, 256 KiB de resposta e 15 tarefas por lote. Sem retries automáticos. Nenhum prompt é extraído de comentários ou arquivos não revisados.
- Secrets em headers de API; sem redirecionamentos; sem impressão de respostas de erro. As propostas ficam em artifacts com retenção de sete dias, e a issue recebe ID, resultado e hash, não o texto do modelo. Não executar uma proposta sem inspeção. Reservas não são locks sobre arquivos do produto: manter o controle de escopo pelo coordenador.
- Uma vez consumidos os lotes da fila, não há novas chamadas até o coordenador adicionar novos IDs em PR revisada. O supervisor ChatGPT existente pode publicar os próximos lotes; ainda não há gerador automático de lotes nem executor isolado de patches. Google Drive continua como canal humano/IA conectado via ferramentas autorizadas; o runner não recebe credencial Drive nem sincroniza automaticamente.
- Desligamento: PGBR_AUTO_DEV_ENABLED=false. Não apagar o histórico.
- Sem mudança de runtime, V5, perfil 0.437.0 ou artefatos LT049. Esta fase não comprova compatibilidade 0.439.0.

## Testes

Sem rede: python -B -m unittest discover -s pgrazor/test -p test_auto_dev_orchestrator.py
Dry-run: python -B tools/auto_dev_orchestrator.py --queue tools/auto_dev_queue.json --output /tmp/pgbr-auto-dev
O dry-run não usa credenciais nem reserva lotes. CI obrigatório antes de integrar. Teste real de APIs depende das credenciais e orçamento aprovados.

Referências oficiais:
- https://platform.openai.com/docs/api-reference/responses/create
- https://ai.google.dev/api/generate-content
- https://platform.arena.ai/docs/api
- https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/control-workflow-concurrency

Próxima fase: agente de código isolado com escopo de arquivos reservado, sandbox sem segredos de produção, exportação de patch, testes independentes e PR; integração só após CI/revisão e preservação de branches. Não ativar merge automático sem critérios.
