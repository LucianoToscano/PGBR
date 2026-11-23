# PGBR PR136 + Rocket QA6 — candidato de validação

Esta compilação experimental reúne as alterações do PR #136 e a investigação do Ataque Inteligente da PR #128, em uma branch separada ([PR #140](https://github.com/LucianoToscano/PGBR/pull/140)).

**Problema corrigido na integração:** o primeiro APK PR136 omitia as alterações Rocket QA3–QA6 da PR #128. Foram incorporadas políticas e diagnósticos do motor compartilhado Rocket/VNext/treinamento/PvP com provas de classificação de golpe, estado do adversário, botão e tentativa de execução. As demais funcionalidades do PR136 foram preservadas.

**Limitação:** a QA5 tinha `actorIdentity=unknown` em treinamento e a QA6 apenas instrumenta a causa (`actorProbe`), sem garantir que a identificação funcionará no jogo. Com Ataque Inteligente ON e dados inválidos, ataques carregados automáticos podem ficar suspensos. Os testes nativos são simulados, não homologação em Android. Raid/Gym/Max ainda não utilizam o mesmo seletor.

O pacote, quando compilado, será apenas para testes comparativos. Não substituir a instalação estável sem backup.
