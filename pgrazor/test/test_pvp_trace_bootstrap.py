"""Source regression for PvP GUI observer bootstrap; not a runtime/Android validation."""
from pathlib import Path

root=Path(__file__).resolve().parents[2]
source=(root/"pgrazor/native/cutscene_skip.cpp").read_text(encoding="utf-8")
setup=source.split("void install(",1)[1].split("void pgr_cutscene_skip_tick(",1)[0]
tick=source.split("void pgr_cutscene_skip_tick(",1)[1].split("void pgr_cutscene_skip_stop(",1)[0]
assert 'const bool pvp_trace_installed=gui_open_original.load(std::memory_order_acquire)!=nullptr;' in setup
assert 'require(pvp_trace_installed,"no cutscene or PvP screen observer could be hooked");' in setup
assert '"PvP screen trace installed; cutscene skip hooks unavailable"' in setup
assert 'require(count>0,"no cutscene could be hooked");' not in setup
assert 'pvp_requested=(mask&7)!=0;' in tick
assert tick.index("if(!api||api->snapshot(&boot)!=0)return;") < tick.index("tried=true;") < tick.index("install();")
assert 'require(api&&api->snapshot(&boot)==0,"Runtime unavailable")' not in tick
assert "pgrazor_pvp_gui_open(ns,name)" in source
assert "PGR_GAME_CUTSCENE_SKIP_CPP_GUI_OPEN_FINGERPRINT" in setup
print("PvP screen observer bootstrap regression: PASS")
