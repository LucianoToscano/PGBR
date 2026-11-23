#pragma once
#include <cstdint>

// Pure, fail-closed precondition for future session-scoped Max PvE policy.
// No native/Java transport is connected to this policy yet.
// Values are from battles.h (raid=0, gym=1, max=2).
struct PgrMaxSessionGate {
  static constexpr int32_t MAX_MODE=2;
  static constexpr uint32_t ALLOWED_ACTIONS=0xFu; // quick, charged, max, dodge (logical bits)
  static uint32_t effective(uint32_t requested, bool consent_live,
                            bool host_ready, bool scope_active, int32_t mode,
                            bool native_failed, uint64_t scope_serial,
                            uint64_t authorized_scope_serial,
                            uint64_t now_ms, uint64_t lease_until_ms) {
    if (!consent_live || !host_ready || !scope_active ||
        mode != MAX_MODE || native_failed ||
        scope_serial == 0 || scope_serial != authorized_scope_serial ||
        now_ms == 0 || lease_until_ms <= now_ms ||
        lease_until_ms - now_ms > 2500 ||
        requested == 0 || (requested & ~ALLOWED_ACTIONS) != 0)
      return 0;
    return requested;
  }
};
