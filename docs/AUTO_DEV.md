# PGBR AUTO DEV — modo gratuito obrigatório

Objetivo do proprietário: nenhum gasto adicional. Não contratar APIs, créditos, assinaturas, runners ou armazenamento adicionais. O código não contém rotas de API OpenAI/Arena; esses agentes continuam pela automação ChatGPT existente e pelo chat Arena, com GitHub #320 e Drive compartilhado. Não controlar chats externos sem integração autorizada.

## Gemini (opcional, desativado por padrão)

Depois de CI e merge, criar um projeto exclusivo no Google AI Studio sem Cloud Billing vinculado, sem pré-pagamento e sem upgrade. Confirmar na conta que a camada Free está disponível para o modelo escolhido, e que os limites de uso se aplicam. Não reutilizar chave de projeto pago. Se a camada gratuita não estiver disponível, não habilitar a API.

1. https://aistudio.google.com/apikey — criar chave apenas no projeto gratuito.
2. Se o Secret existente `GEMINI_API_KEY` estiver cadastrado e vinculado ao projeto gratuito confirmado, reutilize-o sem copiar a chave: variável `PGBR_GEMINI_KEY_SOURCE=existing`. Caso contrário, mantenha `dedicated` (padrão), com `PGBR_GEMINI_FREE_KEY`. Não há fallback automático entre chaves. Nunca colocar chave no chat, issue ou commit.
3. Em https://github.com/LucianoToscano/PGBR/settings/variables/actions, configurar PGBR_GEMINI_FREE_MODEL com um modelo explicitamente gratuito e disponível na conta. Conferir https://ai.google.dev/gemini-api/docs/pricing e limites em AI Studio; não há modelo padrão nem fallback.
4. Configurar orçamento de gastos adicionais zero para Actions e armazenamento, com bloqueio ao atingir limite (se disponível): https://github.com/settings/billing/budgets. Não habilitar jobs novos enquanto essa proteção não estiver confirmada. Se o GitHub não oferecer bloqueio efetivo, manter job extra desligado; monitorar também o scheduler existente e builds.
5. Somente após confirmar os dois controles, definir PGBR_AUTO_DEV_MODE=free, PGBR_FREE_BILLING_CONFIRMED=true, PGBR_GITHUB_FREE_BUDGET_CONFIRMED=true e PGBR_AUTO_DEV_ENABLED=true. As confirmações são declarações do proprietário, não verificações automáticas da conta de faturamento. Se mudar a cobrança, desabilitar imediatamente o job.
6. Conferir recibos da issue #320 e artifacts de propostas com retenção de um dia. Não enviar código privado, APK, dados pessoais ou segredos à camada gratuita; consultar os termos do provedor, que podem permitir uso dos dados para melhorar produtos.

## Proteções

- Uma chamada Gemini no máximo por ciclo, 4096 tokens de saída solicitados, timeout de 90 segundos, resposta limitada a 256 KiB. Fila finita; cada ID reservado antes da chamada. Sem retries, fallback pago ou troca automática de modelo. Erros 401/403/429 registram uma pausa durável na issue #320, impedindo chamadas de todos os lotes Gemini subsequentes. Depois de confirmar que a cota/acesso voltou e o faturamento permanece desativado, o proprietário pode definir `PGBR_GEMINI_RESUME_AFTER` com o ID exato do último lote pausado. O lote anterior continua reservado; só novos IDs podem ser despachados. Sem confirmação, não retomar. Não burlar cotas.
- Não ler credenciais OpenAI ou Arena, ainda que existam. Não habilitar APIs pagas nem comprar créditos. O job não roda em PRs. O limite de tokens e chamadas não prova custo zero: o projeto deve permanecer sem faturamento e o GitHub deve bloquear excedentes.
- O agendador de dez minutos já existente é reaproveitado. A automação ChatGPT horária continua independente de API paga. GitHub Actions pode consumir franquia de minutos e armazenamento, inclusive no agendador histórico; verificar painel de uso e impedir excedentes. Não criar outro executor.
- ChatGPT e Arena permanecem na fila compartilhada; não são acionados via API. Arena precisa de acionamento pelo chat, caso não possua execução automática gratuita autorizada.
- As respostas Gemini são propostas para auditoria, não código executado. Não transferir arquivos privados aos provedores. Sem execução autônoma de patches ou merge de respostas. V5, perfil e releases preservados.

## Testes e operação

python -B -m unittest discover -s pgrazor/test -p test_auto_dev_orchestrator.py

python -B tools/auto_dev_orchestrator.py --queue tools/auto_dev_queue.json --output /tmp/pgbr-auto-dev

Dry-run sem rede. CI obrigatório antes de merge. Nenhum teste real de API até confirmação de gratuidade. Desligar: PGBR_AUTO_DEV_ENABLED=false. Não remover reservas históricas.

Referências: https://ai.google.dev/gemini-api/docs/pricing ; https://docs.github.com/en/billing/how-tos/set-up-budgets ; https://docs.github.com/en/actions/concepts/billing-and-usage .
