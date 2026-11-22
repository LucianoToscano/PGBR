#pragma once
#include <cstdint>

// This is an allowlist of opponent *battle modes*, not a spoofed Rocket NPC.
// Only the existing Rocket action executor is reused by typed combat scopes.
// PvE modes remain exclusively owned by feature 19 (battles.cpp).
enum class PgrAutoBattleTarget : uint8_t {
    UNKNOWN=0, ROCKET=1, PVP=2, TRAINER_NPC=3,
    RAID=4, GYM=5, MAX=6, WILD=7
};

constexpr bool pgr_rocket_assist_target(PgrAutoBattleTarget kind) {
    return kind==PgrAutoBattleTarget::ROCKET ||
           kind==PgrAutoBattleTarget::PVP ||
           kind==PgrAutoBattleTarget::TRAINER_NPC;
}

// A saved bitmask does not authorize any game action by itself. Runtime hooks
// must still prove both an exact type and an actual open combat scene.
constexpr bool pgr_allow_shared_battle_actions(PgrAutoBattleTarget kind,
                                                uint32_t action_mask,
                                                bool typed_scope,
                                                bool valid_lease) {
    return pgr_rocket_assist_target(kind) &&
           (action_mask & 7u)!=0u && typed_scope && valid_lease;
}
