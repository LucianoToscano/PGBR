"""Source-level contract: F2 controls and native manual-choice safety.

These checks do not certify Android gameplay. Native policy and JVM cold-start
fixtures exercise execution separately.
"""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
control=(ROOT/"pgrazor/src/dev/pgrazor/Control.java").read_text(encoding="utf-8")
panel=(ROOT/"pgrazor/src/dev/pgrazor/SettingsPanel.java").read_text(encoding="utf-8")
policy=(ROOT/"pgrazor/native/battle_timing_policy.h").read_text(encoding="utf-8")
swap=(ROOT/"pgrazor/native/rocket_swap.cpp").read_text(encoding="utf-8")
smart=(ROOT/"pgrazor/native/smart_battle.cpp").read_text(encoding="utf-8")
combat=(ROOT/"pgrazor/native/rocket_combat.cpp").read_text(encoding="utf-8")
actions=(ROOT/"pgrazor/native/rocket_actions.cpp").read_text(encoding="utf-8")

assert 'smartShieldTiming=1, smartFaintedSwapTiming=1' in control
assert '"smartShieldTiming",String.valueOf(smartShieldTiming)' in control
assert '"smartFaintedSwapTiming",String.valueOf(smartFaintedSwapTiming)' in control
assert 'smartShieldTiming=readBoundedVisual(p,"smartShieldTiming",1,0,2)' in control
assert 'smartFaintedSwapTiming=readBoundedVisual(p,"smartFaintedSwapTiming",1,0,2)' in control
assert '"F2|"+(smartBattle?1:0)+"|"+smartShieldTiming+"|"+smartFaintedSwapTiming' in control
assert '.setSingleChoiceItems(SHIELD_WAIT_LABELS,Control.smartShieldTiming()' in panel
assert '.setSingleChoiceItems(FAINTED_SWAP_LABELS,Control.smartFaintedSwapTiming()' in panel
assert '"Imediato — 0 ms"' in panel and '"1,5 segundo (padrão)"' in panel
assert '"3 segundos"' in panel and '"Original VNext — 400 ms"' in panel
assert '"2 segundos (padrão)"' in panel and '"6 segundos"' in panel
assert 'fainted_wait_ms(prefs.fainted_choice)' in swap
assert 'if(!unbox<bool>(invoke(prompt_is_open,widget))){release();return;}' in swap
assert 'kRetryMs=1000,kGiveUpMs=8000' in swap
assert 'pgr_rocket_swap_prompt_active()' in smart
assert 'if(pgr_rocket_swap_prompt_active()){press=Press{};rocket_watch=Watch{};return;}' in smart
assert 'kSettleMs=2300,kOpenMs=350' in smart  # strategic swap separately preserved
assert 'read(tr,cfg)' in smart
assert 'shield_now-g_shield_opened_ms<wait_ms' in combat
assert 'PGR_RC_OBS_SHIELD_MANUAL' in combat and 'PGR_RC_OBS_SHIELD_CLOSE' in combat
assert 'shield_wait_ms(p.shield_choice)' in combat
assert 'invoke(g_cfg.vm_charge, target, nullptr, &err);' in combat
assert 'action==PGR_ROCKET_QUICK?150' in actions
assert 'action==PGR_ROCKET_LEGACY_CHARGE?500' in actions
assert 'action==PGR_ROCKET_VNEXT_CHARGE?600:250' in actions
assert 'return option == 0 ? 0 : option == 2 ? 3000 : option == 1 || option == 3 ? 1500' in policy
assert 'return option == 0 ? 400 : option == 2 ? 6000 : option == 1 || option == 3 ? 2000' in policy
print("PASS: PT-BR timing UI, persistence, manual safety and unchanged attack cadence source contract")
