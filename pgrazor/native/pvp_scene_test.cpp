#include "pvp_scene.h"
#include <cassert>
#include <cstdio>
int main(){
    PgrPvpScene s;
    assert(!s.vnext(100));
    s.gui("Niantic.Holoholo.Combat.Preparation","CombatPreparationRootGuiController",100);
    assert(!s.vnext(101)); // preparation without PvP hub is not proof
    s.gui("Niantic.Holoholo.Combat.HubState","CombatHubGuiController",100);
    assert(!s.vnext(101)); // hub alone is not proof
    s.gui("Niantic.Holoholo.Combat.Preparation","CombatPreparationRootGuiController",102);
    assert(s.vnext(103));
    assert(s.vnext(104));
    s.gui("Niantic.Holoholo.Combat.Resolution","CombatResolveGuiController",105);
    assert(!s.vnext(106)); // stop after results
    s.gui("Niantic.Holoholo.Combat.HubState","CombatHubGuiController",200);
    s.gui("Niantic.Holoholo.Combat.Preparation","CombatPreparationRootGuiController",201);
    s.gui("Niantic.Holoholo.Combat.NpcTrainers","NpcTrainerInterstitialGui",202);
    assert(!s.vnext(203)); // NPC overrides prior Hub
    s.gui("Niantic.Holoholo.Combat.HubState","CombatHubGuiController",500);
    s.gui("Niantic.Holoholo.Combat.Preparation","CombatPreparationRootGuiController",501);
    assert(!s.vnext(181000)); // expired prep never attacks
    s.gui("Niantic.Holoholo.Combat.HubState","CombatHubGuiController",900000);
    s.gui("Niantic.Holoholo.Combat.Preparation","CombatPreparationRootGuiController",900001);
    assert(s.vnext(900002));
    assert(!s.vnext(1200003)); // finite combat lease
    std::puts("PgrPvpSceneTest passed");
}
