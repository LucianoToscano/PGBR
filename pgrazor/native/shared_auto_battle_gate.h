#pragma once
#include <cstdint>

// Use one persisted Rocket/PvP fast/charged/shield mask, but arm it only when
// the *native executor* has published an exact, validated battle scope.
// Rocket InvasionCombatState and PvP/Training VNext enter that same engine;
// Raid/Gym/Max are never entered into this scope by combat_host.cpp.
// Only atomic sample fields are used: refresh_leaf_gates can be called from
// non-owner threads and must never read raw PgrPvpScene/GC handles.
struct PgrSharedAutoBattleGate {
  static uint32_t effective(uint32_t user_mask, bool allowed, bool lease_live,
                            bool native_scope_active, bool native_failed) {
    if(!allowed || !lease_live || !native_scope_active || native_failed ||
       user_mask==0 || (user_mask & ~7u)!=0u) return 0;
    return user_mask;
  }
};
