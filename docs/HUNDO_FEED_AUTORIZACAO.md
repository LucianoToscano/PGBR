# Política de fontes externas: LivePokeMap / hundo-feed

**Situação em 22/11/2026:** o arquivo `tools/hundo-feed/hundo_feed.py` é código herdado do PGRazor, documentado como um serviço opcional de polling; sua presença no repositório não comprova que o PGBR o executa, controla o servidor, possui conta autorizada ou tem licença de redistribuição.

## Requisitos antes de qualquer ativação

1. Confirmar por escrito o operador responsável, a conta e os direitos explícitos de uso da API e de publicação dos dados. Uma assinatura pessoal não autoriza automaticamente a revenda ou republicação de respostas.
2. Registrar contrato e limites de taxa, endpoints documentados e origem; não contornar autenticação, proibições de redistribuição ou bloqueios 401/403/429.
3. Manter `LPM_USERNAME`/`LPM_PASSWORD` e sessões exclusivamente nos secrets do operador, nunca no GitHub, APK, JSON público ou logs. Inventariar exclusão e rotação de credenciais, retenção de dados e prazo de expiração das observações.
4. Mostrar indisponibilidade real no aplicativo em vez de inventar coordenadas, IV ou spawns; expor proveniência e timestamps.
5. Separar o serviço experimental da build PGBR e impedir o deploy automático até que autorização, privacidade e manutenção estejam documentadas.

**Estado:** autorização e operador **não verificados**; portanto nenhum deploy ou ativação é autorizado por esta documentação. Revisão de fontes externas e testes de integração permanecem independentes do CI de código.

## Registro de autorização declarada e implementação manual — 22/11/2026

O responsável pelo projeto já declarou em conversas anteriores autorização para uso de
APKs/APIs de referência PGSharp, Pokemod, PGTools, PokeList e Data Hub; essa
declaração está registrada no [Guia Mestre do PokeSearch, LT364 e LT365]
(https://github.com/LucianoToscano/PokeSearch/blob/main/docs/GUIA_MESTRE.md).
**Não solicitar ao usuário que repita a mesma declaração de autorização.**
Isso não acrescenta credenciais, autorização contratual a novas origens nem
elimina a necessidade de validar a disponibilidade e o comportamento de cada
serviço antes de uma ativação de produção.

O recurso `HundoFeedDialog` nesta PR:
- não executa `hundo_feed.py` nem publica snapshots por conta própria;
- usa o endereço HTTPS de arquivo JSON **já constante no `ShundoSniper.FEED_URL`**
  em modo de consulta pontual, sem login, tokens ou segredos;
- valida versão do esquema, idade do snapshot, expiração, coordenadas e duplicatas,
  e não apresenta dados antigos como resultados ativos;
- exibe os alvos 100% IV como **declaração da origem**, não confirmação no jogo;
- oferece copiar coordenadas e teleporte somente após confirmação explícita do
  usuário e teste de prontidão do adaptador local; não ativa captura automática;
- respeita intervalos entre consultas, 401/403/429 e falta de rede.
  
**Evidência:** implementação em branch separada, com fixtures Java e contrato
estático; nenhum servidor foi implantado, nenhuma credencial foi acessada e
nenhum feed ou APK foi homologado fisicamente por esta alteração.
