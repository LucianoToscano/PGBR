"""Fail-safe timing contract for manual decisions in battle.

This is a source contract, not a hardware timing or QA claim.
"""
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2] / "pgrazor" / "native"
smart = (ROOT / "smart_battle.cpp").read_text(encoding="utf-8")
swap = (ROOT / "rocket_swap.cpp").read_text(encoding="utf-8")
combat = (ROOT / "rocket_combat.cpp").read_text(encoding="utf-8")
assert "kSettleMs=2300,kOpenMs=350" in smart
assert '!unbox<bool>(call(ui,"get_IsOpen"))' in smart  # player picker wins
assert "kRetryMs=1000,kGiveUpMs=8000" in swap
assert "fainted_wait_ms(prefs.fainted_choice)" in swap
assert "now-pending_since<wait" in swap
assert "pgr_rocket_swap_prompt_active()" in smart
assert "shield_choice_wait_ms()" in combat
assert "shield_now-g_shield_opened_ms<wait_ms" in combat
assert "invoke(g_cfg.vm_charge, target, nullptr, &err);" in combat
print("PASS: manual swap/shield grace with unchanged charged click path")
