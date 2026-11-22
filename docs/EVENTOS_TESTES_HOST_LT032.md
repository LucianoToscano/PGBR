# Regressões Halloween / GO Wild / Safari / VGP / Mateo em host

A branch de QA `test/lt032-eventos-vgp-safari-host-20261122` executa código nativo real em Linux, sem jogos, contas, rede nem frameworks Android. As cinco fixtures são `event_collector_test.cpp`, `route_gift_policy_test.cpp`, `safari_always_hit_test.cpp`, `safari_throws_test.cpp` e `safari_vgp_actions_test.cpp`. Cada teste contém o controlador original ou está diretamente ligado à sua implementação; um simulador apenas fornece objetos de entrada.

`tools/event_host_tests.py` compila e executa cada teste C++17 num diretório temporário; também são executados `test_quick_menu_events_java.py` e `test_event_settings_java.py` com JVM. Não altera gameplay nem configurações e tem permissão GitHub apenas de leitura.

**Não equiparar aprovação a runtime:** Eventos depende de caixas realmente presentes, hooks Unity/IL2CPP validados e elegibilidade sazonal; GO Wild depende de Safari Ball e Mighty presentes; VGP+ precisa de capturas/giros reais; Mateo depende de Rotas. São experimentos controlados sem Android, não homologação. O estado final dos testes estará no próprio Actions da branch.
