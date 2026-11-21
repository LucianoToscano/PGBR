"""LT029 UI policy: settings may be configured without asserting PvP automation readiness."""
from pathlib import Path

root = Path(__file__).resolve().parents[2]
ui = (root / 'pgrazor/src/dev/pgrazor/SettingsPanel.java').read_text(encoding='utf-8')
control = (root / 'pgrazor/src/dev/pgrazor/Control.java').read_text(encoding='utf-8')
mode = (root / 'pgrazor/src/dev/pgrazor/PvpBattleMode.java').read_text(encoding='utf-8')

assert 'pvpBattleChoices[i].setEnabled(true);' in ui, 'PvP preferences must be editable'
assert 'pvpBattleChoices[i].setEnabled(Control.pvpBattleReady())' not in ui, 'Do not gate preference edits on runtime'
assert 'pvpPreferSuper.setEnabled((Control.pvpBattleOptions()&PvpBattleMode.CHARGED)!=0);' in ui, 'Super preference requires charged move'
assert ui.count('refreshPvpBattles();') >= 3, 'Refresh preferences after edits'
assert 'nenhum toque automático será executado' in ui, 'Not-yet-operational status must be explicit'
assert 'pvpBattleReady=false;' in control, 'Never enable unverified gameplay hooks on settings reload'
assert 'PvpBattleMode.allowed(pvpBattleOptions,pvpBattleReady,quickSummaryReady(),hostLive())' in control, 'Gameplay guard must remain intact'
assert 'return (sanitize(options)&ACTIONS)!=0&&runtimeReady&&summaryReady&&hostLive;' in mode, 'Action readiness must be fail-closed'
print('LT029 PvP settings persistence/readiness policy passed')
