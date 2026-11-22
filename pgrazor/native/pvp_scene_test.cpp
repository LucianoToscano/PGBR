#include "pvp_scene.h"
#include <cassert>
#include <cstdio>
#include <limits>
static void hub(PgrPvpScene &s,uint64_t t){s.gui("Niantic.Holoholo.Combat.HubState","CombatHubGuiController",t);}
static void prep(PgrPvpScene &s,uint64_t t){s.gui("Niantic.Holoholo.Combat.Preparation","CombatPreparationRootGuiController",t);}
int main(){
    PgrPvpScene s;
    assert(!s.vnext(100));
    prep(s,100);assert(!s.vnext(101)); // preparation without hub
    hub(s,100);assert(!s.vnext(101)); // hub without preparation
    prep(s,102);assert(s.vnext(103));assert(s.vnext(104));
    s.gui("Niantic.Holoholo.Combat.Resolution","CombatResolveGuiController",105);
    assert(!s.vnext(106));
    hub(s,200);prep(s,201);
    s.gui("Niantic.Holoholo.Combat.NpcTrainers","NpcTrainerInterstitialGui",202);
    assert(!s.vnext(203));
    hub(s,500);prep(s,501);assert(!s.vnext(181000)); // prep expiry
    hub(s,900000);prep(s,900001);assert(s.vnext(900002));
    assert(!s.vnext(1200003)); // no heartbeat: expires
    hub(s,2000000);prep(s,2000001);assert(s.vnext(2000002));
    assert(s.vnext(2200002));
    assert(s.vnext(2400002)); // past old 5-minute wall clock at next update
    assert(s.vnext(2600002));
    assert(s.vnext(2800002));
    assert(s.vnext(3000002));
    assert(s.vnext(3150002));
    assert(!s.vnext(3200002)); // 20-minute absolute cap
    hub(s,4000000);prep(s,4000001);assert(s.vnext(4000002));
    assert(!s.vnext(4000001)); // clock regression
    hub(s,5000000);prep(s,5000001);assert(s.vnext(5000002));
    hub(s,5000100);assert(!s.vnext(5000101)); // later hub disarms
    prep(s,5000102);assert(s.vnext(5000103)); // separate encounter
    hub(s,std::numeric_limits<uint64_t>::max()-100);
    assert(!s.vnext(std::numeric_limits<uint64_t>::max()));
    std::puts("PgrPvpSceneTest passed");
}
