#pragma once
#include <cstdint>
#include <cstring>
#include <limits>
#include "battle_target_policy.h"

// Only verified 0.437.0 GUI sequences can arm a vNext combat session.
// NPC interstitial is also used by Mateo gifts: it alone NEVER attacks.
struct PgrPvpScene {
    enum Stage { IDLE=0, HUB=1, PREPARATION=2, COMBAT=3, TRAINING_ENTRY=4, VERIFIED_PVP_ENTRY=5 };
    enum Mode { NONE=0, PVP=1, TRAINING=2 };
    static constexpr uint64_t HUB_MS=600000, PREP_MS=180000;
    static constexpr uint64_t COMBAT_IDLE_MS=300000, MAX_COMBAT_MS=1200000;
    static constexpr uint64_t TRAINING_ENTRY_MS=45000;
    static constexpr uint64_t VERIFIED_PVP_ENTRY_MS=15000;
    Stage stage=IDLE;
    Mode mode=NONE;
    uint64_t expires_ms=0, started_ms=0, last_vnext_ms=0;
    static uint64_t until(uint64_t now,uint64_t span) {
        const uint64_t max=std::numeric_limits<uint64_t>::max();
        return now>max-span?max:now+span;
    }
    void reset(){stage=IDLE;mode=NONE;expires_ms=started_ms=last_vnext_ms=0;}
    bool training() const {return stage==COMBAT && mode==TRAINING;}
    bool pvp() const {return stage==COMBAT && mode==PVP;}
    // Called only after the 0.437.0 CombatTypeExtensions.IsPvPCombat
    // validated method returned true for a live CombatDirectorV2. This
    // covers direct friend challenges and an already-observed PvP hub whose
    // preparation GUI was omitted/reordered; a genuine VNext controller is
    // still required before opening any action scope.
    void verified_pvp_type(uint64_t now) {
        if(!now)return;
        live(now);
        // A validated game combat type may upgrade a pending PvP hub/preparation
        // to a short-lived typed candidate. Previously, the HUB/PREPARATION
        // stages rejected this proof, so a missing GUI event permanently
        // prevented a live PvP/duel from arming.
        // Do not override any NPC/training scope or an ongoing combat session.
        if(mode==TRAINING || stage==COMBAT ||
           (stage!=IDLE && stage!=HUB && stage!=PREPARATION))return;
        if(mode!=NONE && mode!=PVP)return;
        stage=VERIFIED_PVP_ENTRY;mode=PVP;
        expires_ms=until(now,VERIFIED_PVP_ENTRY_MS);
    }
    bool allowlisted() const {
        const PgrAutoBattleTarget kind =
            mode==PVP?PgrAutoBattleTarget::PVP:
            mode==TRAINING?PgrAutoBattleTarget::TRAINER_NPC:
            PgrAutoBattleTarget::UNKNOWN;
        return stage==COMBAT && pgr_rocket_assist_target(kind);
    }
    bool live(uint64_t now) {
        if(!now||!expires_ms||now>=expires_ms){reset();return false;}
        return stage!=IDLE;
    }
    void gui(const char*ns,const char*name,uint64_t now) {
        if(!ns||!name||!now)return;
        live(now);
        if(std::strcmp(name,"CombatResolveGuiController")==0 ||
           std::strcmp(name,"IncidentCombatResolveGuiController")==0){
            reset();return;
        }
        if(std::strcmp(name,"NpcTrainerInterstitialGui")==0){
            // Candidate only: preparation + genuine VNext are also required.
            reset();
            if(std::strcmp(ns,"Niantic.Holoholo.Combat.NpcTrainers")==0){
                stage=TRAINING_ENTRY;mode=TRAINING;
                expires_ms=until(now,TRAINING_ENTRY_MS);
            }
            return;
        }
        if(std::strcmp(ns,"Niantic.Holoholo.Combat.HubState")==0 &&
           std::strcmp(name,"CombatHubGuiController")==0){
            reset();stage=HUB;mode=PVP;expires_ms=until(now,HUB_MS);return;
        }
        if(std::strcmp(ns,"Niantic.Holoholo.Combat.Preparation")==0 &&
           std::strcmp(name,"CombatPreparationRootGuiController")==0 &&
           (stage==HUB || stage==TRAINING_ENTRY) && live(now)){
            stage=PREPARATION;expires_ms=until(now,PREP_MS);
        }
    }
    bool vnext(uint64_t now) {
        if(!live(now))return false;
        if(stage==PREPARATION || (stage==VERIFIED_PVP_ENTRY && mode==PVP)){
            stage=COMBAT;started_ms=last_vnext_ms=now;
            expires_ms=until(now,COMBAT_IDLE_MS);
        }else if(stage==COMBAT){
            if(!started_ms||now<last_vnext_ms||now<started_ms||
               now-started_ms>=MAX_COMBAT_MS){reset();return false;}
            last_vnext_ms=now;
            const uint64_t cap=until(started_ms,MAX_COMBAT_MS);
            const uint64_t renewed=until(now,COMBAT_IDLE_MS);
            expires_ms=renewed<cap?renewed:cap;
        }
        return stage==COMBAT;
    }
};
