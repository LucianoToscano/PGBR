# Auditoria de identidade — PGBR-v0.00.01-LT

Data: 20/11/2026.

## Causa corrigida
A página inicial do GitHub exibia o `README.md` da branch `main`, ainda pertencente à versão anterior do código. As alterações iniciais tinham sido feitas apenas na branch `feature/pt-br-traducao`.

## Renomeação visível
- README principal em português brasileiro, sem a captura de tela antiga e com links de download atuais do PGBR.
- Marca SVG/monograma, textos do site, metadados, links e descrição do aplicativo passam a PGBR.
- Arquivos da versão de teste seguem `PGBR-v0.00.01-LT.apk`, `PGBR-v0.00.01-LT-Root.apk` e `PGBR-v0.00.01-LT-Root-module.zip`.
- Menus Android, mensagens e textos de documentação revisados; glossário PT-BR mantido.

## Identificadores mantidos por compatibilidade
Não renomear apenas por substituição textual `dev.pgrazor`, `pgrazor/` (caminhos de asset), `PGRAZOR_*` (variáveis e Secrets), símbolos nativos nem `MODULE_ID='pgrazor'` (preserva estado e seleção do módulo Root). Migrá-los requer um plano próprio com testes de reinstalação, assinatura, migração de dados, fallback e builds end-to-end.

## Atribuição ao projeto de origem
O código deriva de [PGRazor](https://github.com/jpitty03/PGRazor), sob licença MIT. A atribuição e as referências históricas ao autor e à origem devem permanecer verdadeiras; renomear a marca do fork não altera a autoria original.

## Limitações
Textos fixos e branding podem ser verificados em CI. Os APKs e o site precisam de teste em dispositivos e navegadores reais; capturas antigas do projeto original não devem ser apresentadas como telas atuais do PGBR.
