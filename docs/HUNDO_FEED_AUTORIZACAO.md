# Política de fontes externas: LivePokeMap / hundo-feed

**Situação em 22/11/2026:** o arquivo `tools/hundo-feed/hundo_feed.py` é código herdado do PGRazor, documentado como um serviço opcional de polling; sua presença no repositório não comprova que o PGBR o executa, controla o servidor, possui conta autorizada ou tem licença de redistribuição.

## Requisitos antes de qualquer ativação

1. Confirmar por escrito o operador responsável, a conta e os direitos explícitos de uso da API e de publicação dos dados. Uma assinatura pessoal não autoriza automaticamente a revenda ou republicação de respostas.
2. Registrar contrato e limites de taxa, endpoints documentados e origem; não contornar autenticação, proibições de redistribuição ou bloqueios 401/403/429.
3. Manter `LPM_USERNAME`/`LPM_PASSWORD` e sessões exclusivamente nos secrets do operador, nunca no GitHub, APK, JSON público ou logs. Inventariar exclusão e rotação de credenciais, retenção de dados e prazo de expiração das observações.
4. Mostrar indisponibilidade real no aplicativo em vez de inventar coordenadas, IV ou spawns; expor proveniência e timestamps.
5. Separar o serviço experimental da build PGBR e impedir o deploy automático até que autorização, privacidade e manutenção estejam documentadas.

**Estado:** autorização e operador **não verificados**; portanto nenhum deploy ou ativação é autorizado por esta documentação. Revisão de fontes externas e testes de integração permanecem independentes do CI de código.
