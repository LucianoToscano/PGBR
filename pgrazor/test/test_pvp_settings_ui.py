"""LT030 policy: sync Rocket/PvP switches without enabling unverified combat hooks."""
from pathlib import Path
root = Path(__file__).resolve().parents[2]
ui = (root/'pgrazor/src/dev/pgrazor/SettingsPanel.java').read_text(encoding='utf-8')
control = (root/'pgrazor/src/dev/pgrazor/Control.java').read_text(encoding='utf-8')
mode = (root/'pgrazor/src/dev/pgrazor/PvpBattleMode.java').read_text(encoding='utf-8')
assert 'pvpBattleChoices[i].setEnabled(true);' in ui
assert 'pvpBattleChoices[i].setEnabled(Control.pvpBattleReady())' not in ui
assert 'pvpPreferSuper.setEnabled((Control.pvpBattleOptions()&PvpBattleMode.CHARGED)!=0);' in ui
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
print('LT030 battle settings sync and PvP fail-closed policy passed')
