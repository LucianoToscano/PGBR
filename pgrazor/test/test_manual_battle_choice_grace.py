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
assert "kSettleMs=2300,kRetryMs=1000" in swap
assert "kShieldChoiceGraceMs = 1700" in combat
assert "shield_now-g_shield_opened_ms<kShieldChoiceGraceMs" in combat
assert "invoke(g_cfg.vm_charge, target, nullptr, &err);" in combat
print("PASS: manual swap/shield grace with unchanged charged click path")
