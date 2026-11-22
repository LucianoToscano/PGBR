#include "battle_target_policy.h"
#include <cassert>
#include <cstdio>

int main() {
    using T=PgrAutoBattleTarget;
    static_assert(pgr_rocket_assist_target(T::ROCKET), "Rocket must remain enabled");
    static_assert(pgr_rocket_assist_target(T::PVP), "typed PVP includes friend challenges");
    static_assert(pgr_rocket_assist_target(T::TRAINER_NPC), "confirmed NPC training");
    static_assert(!pgr_rocket_assist_target(T::RAID), "Raid uses a separate engine");
    static_assert(!pgr_rocket_assist_target(T::GYM), "Gym uses a separate engine");
    static_assert(!pgr_rocket_assist_target(T::MAX), "Max uses a separate engine");
    static_assert(!pgr_rocket_assist_target(T::WILD), "wild encounter never arms Rocket");
    static_assert(!pgr_rocket_assist_target(T::UNKNOWN), "unknown must fail closed");
    assert(!pgr_allow_shared_battle_actions(T::PVP,0u,true,true));
    assert(!pgr_allow_shared_battle_actions(T::PVP,7u,false,true));
    assert(!pgr_allow_shared_battle_actions(T::PVP,7u,true,false));
    assert(!pgr_allow_shared_battle_actions(T::RAID,7u,true,true));
    assert(!pgr_allow_shared_battle_actions(T::MAX,7u,true,true));
    assert(!pgr_allow_shared_battle_actions(T::GYM,7u,true,true));
    assert(pgr_allow_shared_battle_actions(T::PVP,1u,true,true));
    assert(pgr_allow_shared_battle_actions(T::TRAINER_NPC,7u,true,true));
    std::puts("PgrBattleTargetPolicyTest passed");
}
