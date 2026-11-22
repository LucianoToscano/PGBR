# Referência histórica PGSharp / Ekoparty 2021 — GPS no PGBR

**Data:** 22/11/2026  
**Branch isolada:** `feature/ekoparty-gps-validacao-20261122`  
**Commit-base da main:** `5087e7486e90f84167017b500cc8c3319e050825`  
**Origem:** Romain Thomas, *PGSharp: Analysis of a Cheat Engine on Android* (novembro de 2021) — https://github.com/romainthomas/pgsharp  
**Documento analisado:** `21-10-ekoparty-mobile-hacking-space-pgsharp.pdf` (48 páginas). O repositório de referência declara explicitamente PGSharp 1.33.0 e Pokémon GO 0.221.0 (2021).

## O que o PDF demonstra (não equivale a APIs públicas nem a código portável)

- Páginas 26–30: a demonstração de *GPS spoofing* identifica `NianticLocationManager.nativeAddLocationProviders`, `nativeGpsStatusUpdate` e `nativeLocationUpdate`, e descreve a troca de coordenadas entregues às rotinas nativas.
- Páginas 31–36: `JNIEnv` e interceptação de `RegisterNatives` como estratégia de pesquisa para alcançar funções obfuscadas. Os endereços exibidos são **específicos do binário histórico**.
- Páginas 23–25: assinatura do APK e ilustração de bypass. Não integrar esse mecanismo em produção a partir do artigo.
- Página 48: exemplo de interceptação de `UnityEngine.Application.OpenURL`, sem relação direta com segurança ou precisão do GPS.
- Páginas 8–19: VM Lua alterada, ofuscação e decodificação de strings para análise de 2021; **não** são módulos plug-and-play.

## Arquitetura corrente do PGBR

O PGBR já entrega coordenadas simuladas pelo adaptador compilado `pgrazor/src/dev/pgrazor/GameLocationDelivery.java`. Ele localiza a assinatura de `NianticLocationManager.nativeLocationUpdate` por reflexão e invoca o método original usando contexto e trava próprios do gerenciador. `NativeLocationGetters.java` e `pgrazor/art/` cobrem outros pontos de integração; `LocationState.java` valida o estado salvo. O runtime de produção não incorpora o Lua, o APK embarcado ou um novo `JNIEnv` proxifier do PGSharp de 2021.

**Conclusão:** não é correto afirmar que o PDF contém uma funcionalidade única ausente do PGBR, nem reutilizar sem validação os offsets de 2021 para o perfil atual de jogo 0.437.0.

## Incremento de código desta branch — integridade do GPS

O adaptador atual já tratava inicialização, restauração de listeners, callbacks reais e falsos, pausa, troca de gerenciadores e status. Esta branch adiciona validação **no último ponto anterior à chamada nativa** para impedir a entrega de valores vivos inválidos:

- Latitude e longitude devem ser finitas e estar dentro de seus intervalos geográficos;
- velocidade deve ser finita e não negativa;
- direção deve ser finita e estar no intervalo de 0 inclusive a 360 exclusivo;
- se a localização sintética for inválida, o adaptador **não chama** `nativeLocationUpdate` e deixa o callback real seguir normalmente;
- a falha registra um evento limitado, sem registrar coordenadas pessoais;
- o próximo valor válido volta a funcionar normalmente, sem reiniciar, alterar configurações ou introduzir atraso nos ataques.

Alteração deliberadamente limitada ao adaptador de GPS. **Não** há novo recurso de menu, desbloqueio de verificação de assinatura, falsificação de atestação, integração a endpoints do PGSharp, nem substituição dos hooks atuais. Nenhum binário do PGSharp foi incluído.

## Verificações e limites

**Fixture ampliada:** `pgrazor/test/GameLocationDeliveryTest.java` por `pgrazor/test/test_game_location_delivery_java.py` cobre NaN, infinito, valores fora do intervalo, velocidade inválida, direção inválida, encaminhamento do callback real e recuperação posterior. Validar a execução pelo CI do PR. Um teste em stubs/JVM **não** comprova compatibilidade com uma instalação real de Pokémon GO.

**Antes de considerar funcionalidade homologada no aplicativo:**

1. CI do PR verde, análise de diferenças e compatibilidade com a documentação e assinatura;
2. build Android QA das variantes necessárias, com hashes do artefato;
3. teste físico com joystick/Teleporte/rotas, ativar/desativar spoofing, pausar/retomar jogo e fechar/abrir, além do cenário de GPS real;
4. confirmar que a instalação Root e sem Root não regrediram. Manter `device_tested=false` até ensaio real;
5. somente então planejar integração/release, observando o protocolo de PR sem merge automático.

**Fora do escopo:** portar `JNIEnv` proxifier, bypass de assinatura e endereços de 2021. Qualquer proposta adicional exige escopo funcional e validação do binário atual; pesquisa histórica sozinha não comprova compatibilidade.
