# Site do PGBR

Página informativa do **PGBR**, edição independente em português brasileiro.

O código do site está em `website/`, construído com React e Vite. Não é necessário publicar o site para gerar os APKs: o [GitHub Actions do PGBR](https://github.com/LucianoToscano/PGBR/actions/workflows/pgbr-build-full-ptbr-candidate.yml) realiza a compilação na nuvem.

## Executar localmente

```bash
cd website
npm ci
npm run dev
```

A base do Vite é `/PGBR/`, correspondente ao nome do repositório. Confira a configuração de GitHub Pages antes de publicar.

## Testar

```bash
npm run build
npx playwright test
```

As imagens antigas do projeto de origem podem conter marcas anteriores e são apenas material de referência. As telas atuais do PGBR devem ser capturadas a partir de builds validadas antes de publicadas como demonstração.

## Créditos

Derivado do código aberto original [PGRazor](https://github.com/jpitty03/PGRazor), sob licença MIT. Atribuição e licenças de terceiros devem ser preservadas.
