"""LT030 policy: sync Rocket/PvP switches without enabling unverified combat hooks."""
from pathlib import Path
root = Path(__file__).resolve().parents[2]
ui = (root/'pgrazor/src/dev/pgrazor/SettingsPanel.java').read_text(encoding='utf-8')
control = (root/'pgrazor/src/dev/pgrazor/Control.java').read_text(encoding='utf-8')
mode = (root/'pgrazor/src/dev/pgrazor/PvpBattleMode.java').read_text(encoding='utf-8')
assert 'pvpBattleChoices[i].setEnabled(true);' in ui
assert 'pvpBattleChoices[i].setEnabled(Control.pvpBattleReady())' not in ui
# One global Smart attacking switch replaces the duplicate PvP-only preference.
assert 'smartAttackSwitch=switchRow(owner,"Smart attacking");' in ui
assert 'pvpPreferSuper' not in ui
assert 'public static boolean smartAttack(){return smartAttack;}' in control
assert 'pvpBattleOptions=PvpBattleMode.update(pvpBattleOptions,PvpBattleMode.PREFER_SUPER,on);' in control
assert 'refreshPvpBattles();' in ui and 'refreshRocket();' in ui
assert 'Control.setRocketOptions(' in ui and 'Control.setPvpBattleOptions(' in ui
assert 'Opções Rocket/PvP sincronizadas' in ui
assert 'rocketOptions=SharedBattleOptions.restoreActions(' in control
assert 'pvpBattleOptions=SharedBattleOptions.pvpMask(rocketOptions,legacyPvP);' in control
assert 'pvpBattleOptions=SharedBattleOptions.pvpMask(value,pvpBattleOptions);' in control
assert 'rocketOptions=SharedBattleOptions.actionMask(options);' in control or 'int shared=SharedBattleOptions.actionMask(options);' in control
assert 'pvpBattleReady=false;' in control
assert 'PvpBattleMode.allowed(pvpBattleOptions,pvpBattleReady,quickSummaryReady(),hostLive())' in control
assert 'return (sanitize(options)&ACTIONS)!=0&&runtimeReady&&summaryReady&&hostLive;' in mode
# Configuration, readiness and observed status must be presented separately.
refresh = ui[ui.index('private void refreshPvpBattles()'):ui.index('private void refreshItems()')]
assert 'String status=Control.pvpBattleStatus();' in refresh
assert 'Controlador indisponível ou aguardando reconhecimento da sessão.' in refresh
assert 'dev.pgrazor.PtBr.tr(status)' in refresh
assert 'Funcionamento no aparelho não confirmado.' in refresh
assert 'treinamento/NPC reconhecido' not in refresh
print('LT030 battle settings sync and PvP fail-closed policy passed')
