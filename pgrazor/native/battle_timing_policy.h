#pragma once
// PGBR battle timing preferences: one validated, coherent snapshot. No ABI changes.
// Slot 21 was already allocated to smartBattleConfig. F2 keeps the original flag
// and adds a shield-choice digit and a fainted-swap-choice digit.
// Reject stale/malformed settings instead of issuing unverified game actions.
#include "runtime_transport.h"
#include <cstdint>
#include <cstring>

namespace pgr_battle_timing {
struct Preferences {
  bool smart_battle = false;
  uint32_t shield_choice = 3; // disabled until a valid snapshot is read
  uint32_t fainted_choice = 3;
};

// 0 immediate, 1 default 1.5 s, 2 3 s, 3 disabled.
inline uint64_t shield_wait_ms(uint32_t option) {
  return option == 0 ? 0 : option == 1 ? 1500 : option == 2 ? 3000 : UINT64_MAX;
}
// 0 original VNext 400 ms, 1 default 2 s, 2 6 s, 3 disabled.
inline uint64_t fainted_wait_ms(uint32_t option) {
  return option == 0 ? 400 : option == 1 ? 2000 : option == 2 ? 6000 : UINT64_MAX;
}

inline bool parse(const char *s, uint32_t n, Preferences &out) {
  if (!s || n != 8 || std::memcmp(s, "F2|", 3) != 0 ||
      (s[3] != '0' && s[3] != '1') || s[4] != '|' ||
      s[5] < '0' || s[5] > '3' || s[6] != '|' ||
      s[7] < '0' || s[7] > '3') return false;
  out.smart_battle = s[3] == '1';
  out.shield_choice = static_cast<uint32_t>(s[5] - '0');
  out.fainted_choice = static_cast<uint32_t>(s[7] - '0');
  return true;
}

inline bool read(const PgrRuntimeTransportApi *transport, Preferences &out) {
  if (!transport || !transport->sample || !transport->get_string) return false;
  PgrRuntimeTransportSample before{}, after{};
  if (transport->sample(&before) || !before.snapshot_fresh || before.fault) return false;
  char value[16]{};
  uint32_t n = 0;
  if (transport->get_string(21, value, sizeof(value), &n)) return false;
  if (transport->sample(&after) || !after.snapshot_fresh || after.fault ||
      before.snapshot_seq != after.snapshot_seq ||
      before.generation != after.generation) return false;
  return parse(value, n, out);
}
} // namespace pgr_battle_timing
