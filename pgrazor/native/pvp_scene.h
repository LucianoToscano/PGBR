#pragma once
#include <cstdint>
#include <cstring>

// Conservative transition filter for the 0.437.0 PvP GUI sequence.
// A generic VNext battle is NEVER classified as PvP without the Combat Hub
// followed by the preparation view; NPC battles and resolve cancel the lease.
struct PgrPvpScene {
    enum Stage { IDLE=0, HUB=1, PREPARATION=2, COMBAT=3 };
    Stage stage=IDLE;
    uint64_t expires_ms=0;

    void reset(){stage=IDLE;expires_ms=0;}
    bool live(uint64_t now) {
        if(!now||!expires_ms||now>=expires_ms){reset();return false;}
        return stage!=IDLE;
    }
    void gui(const char*ns,const char*name,uint64_t now) {
        if(!ns||!name||!now)return;
        live(now);
        if(std::strcmp(name,"NpcTrainerInterstitialGui")==0 ||
           std::strcmp(name,"CombatResolveGuiController")==0 ||
           std::strcmp(name,"IncidentCombatResolveGuiController")==0){
            reset();return;
        }
        if(std::strcmp(ns,"Niantic.Holoholo.Combat.HubState")==0 &&
           std::strcmp(name,"CombatHubGuiController")==0){
            stage=HUB;expires_ms=now+600000;return;
        }
        if(std::strcmp(ns,"Niantic.Holoholo.Combat.Preparation")==0 &&
           std::strcmp(name,"CombatPreparationRootGuiController")==0 &&
           stage==HUB && live(now)){
            stage=PREPARATION;expires_ms=now+180000;
        }
    }
    bool vnext(uint64_t now) {
        if(!live(now))return false;
        if(stage==PREPARATION){stage=COMBAT;expires_ms=now+300000;}
        return stage==COMBAT;
    }
};
