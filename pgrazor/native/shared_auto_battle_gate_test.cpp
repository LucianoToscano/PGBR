#include "shared_auto_battle_gate.h"
#include <cassert>
#include <cstdio>

static void hub(PgrPvpScene &s,uint64_t n){
    s.gui("Niantic.Holoholo.Combat.HubState","CombatHubGuiController",n);
}
static void prep(PgrPvpScene &s,uint64_t n){
    s.gui("Niantic.Holoholo.Combat.Preparation","CombatPreparationRootGuiController",n);
}
int main(){
    PgrPvpScene scene;
    const auto active=[&](uint32_t mask,bool allowed,bool lease,bool scope,bool ui){
        return PgrSharedAutoBattleGate::effective(mask,allowed,lease,scope,ui,scene);
    };
    assert(active(7,true,true,false,false)==0); // map/overworld: off
    assert(active(7,true,true,true,false)==7);  // exact Rocket scope: on
    assert(active(5,true,true,true,false)==5);  // honor user toggles, never force charged
    assert(active(0,true,true,true,false)==0);  // all toggles off
    assert(active(7,false,true,true,false)==0);
    assert(active(7,true,false,true,false)==0); // expired lease
    assert(active(8,true,true,true,false)==0);  // invalid mask
    assert(active(7,true,true,true,true)==0);  // generic VNext/raid/max excluded
    prep(scene,100);assert(!scene.vnext(101));
    assert(active(7,true,true,true,true)==0);  // generic prep alone cannot attack
    scene.positive_pvp(102);
    assert(scene.vnext(103)&&scene.pvp());
    assert(active(7,true,true,true,true)==7);  // positive PvP + prep + VNext
    scene.gui("Niantic.Holoholo.Combat.Resolution","CombatResolveGuiController",104);
    assert(active(7,true,true,true,true)==0);  // end shuts the scope off
    scene.reset();
    scene.gui("Niantic.Holoholo.Combat.NpcTrainers","NpcTrainerInterstitialGui",200);
    assert(active(7,true,true,true,true)==0);  // Mateo/NPC dialogue: off
    prep(scene,201);assert(scene.vnext(202)&&scene.training());
    assert(active(3,true,true,true,true)==3);  // trainer: quick+charged
    assert(active(4,true,true,true,true)==4);  // trainer: shield-only selection
    assert(active(7,true,true,false,true)==0); // VNext UI alone isn't a proven scope
    scene.reset();
    hub(scene,300);prep(scene,301);assert(scene.vnext(302)&&scene.pvp());
    assert(active(7,true,true,true,true)==7);  // GBL
    scene.gui("Niantic.Holoholo.Combat.Resolution","CombatResolveGuiController",303);
    assert(active(7,true,true,true,true)==0);
    std::puts("PgrSharedAutoBattleGateTest passed");
}
