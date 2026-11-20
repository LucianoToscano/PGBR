# Auditoria PT-BR do PGBR — v0.00.01-LT

Data: 20/11/2026

## Escopo
Tradução da interface do PGBR e do instalador Root, especialmente menus, filtros, descrições, automações, pesquisas, GPX, mapa, contas, backup, mensagens de status, diagnósticos e erros.

## Estado do glossário
- Base anterior: 405 entradas.
- Após a auditoria: 1141 entradas.
- Entradas acrescentadas nesta rodada: 736.
- Mesmo conjunto em `pgrazor/i18n/pt-BR.json`, `PtBr.java` e `RootPtBr.java`.
- Priorizadas mensagens inteiras e prefixos/sufixos que incluem dados variáveis; a resolução de prefixos/sufixos agora seleciona o trecho mais longo em vez de depender da ordem do HashMap.

## Segurança e compatibilidade
Não traduzir nem renomear chaves de preferências, nomes de pacotes Java (`dev.pgrazor`), identificador do módulo Root (`pgrazor`), assets, caminhos internos, comandos, valores de API ou formatos de dados. Essas referências mantêm compatibilidade. Os créditos e a licença do projeto original permanecem preservados.

## Limitações e verificações
A revisão é estática: não prova que todas as telas foram visitadas ou que 100% dos textos nativos/dinâmicos foram localizados. Testes automatizados e compilação completa devem passar; confirmação em aparelho Android continua necessária. Registrar qualquer inglês residual com uma captura de tela e a tela correspondente.
