# PGBR — Compatibilidade entre dados de webhook PokeSearch e fontes de mapa (22/11/2026)

**Situação:** contrato de integração/documentação técnica. Nenhum novo transporte HTTP ou feed automático foi adicionado ao APK PGBR nesta branch. Código existente preservado.

## Antes de editar
- Mestre único: PGBR_ARQUIVO_MESTRE_UNICO.md. Base consultada: main 5087e7486e90f84167017b500cc8c3319e050825.
- Fonte atual: pgrazor/src/dev/pgrazor/RocketMapFeedData.java e RocketMapFeedClient.java. Já existe parse independente de /raw_data com pokemons[] e gyms/raid; seleção de fonte manual no LiveFeedDialog.
- iFlowGo já oferece conector de nearby e busca global LIMITADA; não chamar de PGSharp ou API oficial Niantic.
- PGSharp Free/Standard tem UI e cofre opcional de key; a leitura da rede privada e validação do serviço não foram comprovadas.

## Origem do contrato novo
PokeSearch: https://github.com/LucianoToscano/PokeSearch/tree/feat/authorized-spawn-webhook-normalizer-20261122
Módulo: functions/spawn-observation-normalizer.js
Contrato: docs/SPAWN_WEBHOOK_AUTHORIZED_NORMALIZATION_20261122.md

O normalizador aceita eventos OBSERVADOS de webhook com ID de encontro, espécies, coordenadas e desaparecimento verificados. Não extrai diretamente dados do jogo. A transformação offline toRocketMapRawData() gera JSON equivalente ao subconjunto RocketMap:

    {"pokemons":[{"pokemon_id":25,"encounter_id":"teste_123","latitude":-6.15,
      "longitude":-35.21,"disappear_time":1795363800000,
      "individual_attack":15,"individual_defense":15,"individual_stamina":15}]}

Dados fictícios. O timestamp disappear_time é Unix MILISSEGUNDOS, como espera RocketMapFeedData.unixMillis(). O resultado somente aparecerá no PGBR se houver um servidor que hospede um endpoint autorizado com o contrato de acesso correto, scanner ativo e seleção manual da URL. Essa hospedagem não foi implementada por este PR.

## Restrições técnicas
1. Nunca inferir IV100 do campo shinyPossible, nem inferir Shiny de uma aparição global. Componentes IV ausentes são desconhecidos, mesmo que outro serviço anuncie “100%”.
2. Não mostrar como ‘ao vivo’ uma entrada sem hora de expiração confiável ou com registro expirado. Tratar HTTP 401/403, 429 e rede fora do ar sem contornar bloqueio; respeitar quotas e termos da origem.
3. Interconexão obrigatória com proveniência: sourceId e receivedAtMs são metadados de ingestão, não prova de captura individual nem origem Niantic.
4. Não transportar chaves PGSharp, cookies, JWT ou credenciais privadas entre projetos nem inserir em logs.
5. Não alterar padrão de teleporte: somente ação manual com confirmação do usuário nas telas existentes.
6. Testar primeiro com fixture fictícia, depois evento autorizado real, em seguida regressões Java/CI e homologação física Root/sem Root. Sem Android físico, status device_tested=false.

## O que já está funcionando em outro lugar
- PGBR possui LiveFeedData, RocketMapFeedData, PokeListFeedData, IFlowGoFeedData, PogoFeedDialog, HundoFeedDialog e FeedChooser. Não substituir esse trabalho.
- Escudo Inteligente e Troca Inteligente constam do Mestre e foram trabalhados em branches/releases próprias; esta integração de coordenadas não interfere em combate.
- O PDF de Ekoparty de 2021 por Romain Thomas documenta Lua/JNI/Unity/GPS históricas, NÃO endpoints atuais globais: https://drive.google.com/file/d/1nEf72fUU4hXS2V8alnuc4pv3unUdINV3/view

## Pendências
- Fonte com permissão verificável e cobertura desejada;
- Receiver autenticado, publicação controlada do JSON e endpoint configurável;
- Integração UI/alertas com provedor e QoS;
- Validação no aparelho/CI e eventual APK de QA, em tarefa própria.

Catálogo completo de 85 registros numerados e links adicionais:
https://docs.google.com/document/d/12-I-rUfnQR0YL_mDExTJfx2zXgfxgB8-Bkp2tyuVPfI/edit
