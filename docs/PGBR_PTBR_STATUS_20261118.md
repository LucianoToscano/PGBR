# PGBR — tradução PT-BR · status técnico de 18/11/2026

**Projeto de destino:** LucianoToscano/PGBR — réplica isolada do aplicativo completo PGRazor.
**Origem e licença:** jpitty03/PGRazor; MIT somente para o código original; avisos em LICENSE e THIRD_PARTY.md.
**Branch de tradução:** feature/pt-br-traducao. A main permanece como cópia original; o PokeSearch não foi alterado.

## Implementação validada

- Glossário PT-BR localizado em `pgrazor/i18n/pt-BR.json`: **405 expressões traduzidas**.
- `PtBr.java` para interface do aplicativo completo e `RootPtBr.java` para a interface do instalador Root, ambos no mesmo projeto PGBR, sem separação arquitetural.
- `tools/ptbr_wire.py`: instrumenta chamadas de exibição (`setText`, títulos, botões de diálogo, mensagens, dicas e descrições de acessibilidade) sem alterar os literais usados nas regras de negócio.
- Primeira execução: 45 arquivos Java, 734 chamadas de apresentação instrumentadas.
- Instalador Root: mais 2 arquivos Java, 23 chamadas de apresentação instrumentadas.
- Total auditável nesta etapa: **47 arquivos Java e 757 pontos de apresentação** com suporte ao tradutor PT-BR.
- Strings sem tradução cadastrada continuam em inglês; este estágio **não significa tradução integral de todas as telas**. A linguagem principal não modifica nomes de classes, identificadores de pacotes, autenticação, estrutura do APK, dados de espécies, contratos da API ou funções do jogo.
- A versão de referência do código é PGRazor v0.3.4. Não substituir a UI do jogo Pokémon GO: ela possui localização própria e direitos separados.

## Testes e compilação

- Verificação Linux de sintaxe Python, idempotência, integridade do glossário e compilação das duas classes Java tradutoras: success, workflow 44248866842.
- Suite de regressão Windows e compilação de controles Android + instalador Root já passaram na execução 44247965267 (primeira etapa).
- Após incluir as 23 chamadas no Root, a execução 44249185023 compilou e passou nos testes anteriores, mas o cheque final PowerShell marcou falso negativo porque tratou uma lista de linhas como único booleano. A verificação foi corrigida.
- Reexecutar/consultar o workflow Windows atualizado; homologação física do aplicativo completo ainda está pendente.

## APK completo PGBR

O pacote jogável **ainda não foi gerado**. O pipeline original exige um pacote original Niantic ARM64 Pokémon GO **0.437.0 / versionCode 2026110902**, cujo SHA-256 esperado está no `docs/BUILDING.md`, além das ferramentas nativas e da assinatura.

A branch possui `.github/workflows/pgbr-build-full-ptbr-candidate.yml`, que recompila o **mesmo PGRazor completo** com interface PT-BR, incluindo os métodos originais sem root e root. Somente execução manual `workflow_dispatch`, somente nessa branch, somente quando as cinco Actions secrets necessárias estiverem disponíveis. Este workflow **não publica Releases**, não anuncia em Discord e não modifica o PokeSearch.

Secrets requeridos, configuráveis diretamente no GitHub PGBR (nunca em código ou chat):
- `PGRAZOR_INPUT_URL` (localização autenticada do arquivo do jogo original)
- `PGRAZOR_KEYSTORE_BASE64`
- `PGRAZOR_KEY_ALIAS`
- `PGRAZOR_STORE_PASSWORD`
- `PGRAZOR_KEY_PASSWORD`

Não redistribuir um APK contendo o jogo de terceiros sem direitos de distribuição. O artefato privado de QA ainda depende de configurar as entradas. Não confundir `pgrazor-preview.apk` com APK completo jogável.

## Pendências da tradução

1. Extrair e revisar termos restantes das telas Android, dropdowns, estados dinâmicos e notificações.
2. Revisar mensagens do código nativo e caminhos de acessibilidade; preservar chaves internas.
3. Homologar visualmente textos longos, quebras de linha, unidades, filtros e formulários no Android físico.
4. Testar integração autenticada dos feeds sem expor credenciais.
5. Gerar APK completo somente com entrada original autorizada e chave assinadora própria, e instalar em aparelho de testes antes de qualquer publicação.

**Regra permanente:** PGBR preserva o aplicativo PGRazor completo e suas funcionalidades. Nenhuma modificação ou release é feita em LucianoToscano/PokeSearch.
