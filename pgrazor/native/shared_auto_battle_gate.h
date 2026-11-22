#pragma once
#include "pvp_scene.h"
#include <cstdint>

// The user's three saved choices remain intact. Runtime actions are enabled
// only while an exact Rocket encounter or an explicit PvP/training VNext
// session owns a live combat scope. Raid/Gym/Max use other engines.
struct PgrSharedAutoBattleGate {
    static uint32_t effective(uint32_t user_mask,bool allowed,bool lease_live,
                              bool scope_active,bool pvp_ui_owned,
                              const PgrPvpScene &scene) {
        if(!allowed || !lease_live || !scope_active ||
           user_mask==0u || (user_mask&~7u)!=0u)return 0u;
        if(pvp_ui_owned &&
           (scene.stage!=PgrPvpScene::COMBAT ||
            (scene.mode!=PgrPvpScene::PVP && scene.mode!=PgrPvpScene::TRAINING)))return 0u;
        return user_mask;
    }
};
