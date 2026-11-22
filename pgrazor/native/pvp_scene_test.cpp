#include "pvp_scene.h"
#include <cassert>
#include <cstdio>
#include <limits>
static void hub(PgrPvpScene &s,uint64_t t){s.gui("Niantic.Holoholo.Combat.HubState","CombatHubGuiController",t);}
static void prep(PgrPvpScene &s,uint64_t t){s.gui("Niantic.Holoholo.Combat.Preparation","CombatPreparationRootGuiController",t);}
static void npc(PgrPvpScene &s,uint64_t t){s.gui("Niantic.Holoholo.Combat.NpcTrainers","NpcTrainerInterstitialGui",t);}
int main(){
    PgrPvpScene s;
    assert(!s.vnext(100));
    s.verified_pvp_type(110);
    assert(!s.allowlisted()); // typed PvP proof does not press attack
    assert(s.vnext(111)&&s.pvp()&&s.allowlisted()); // actual VNext is required
    s.reset();
    s.verified_pvp_type(200);
    assert(!s.vnext(15200)); // stale typed proof is not actionable
    s.verified_pvp_type(40000);
    npc(s,40001);
    assert(!s.vnext(40002)); // NPC candidate supersedes typed proof
    s.reset();
    // Verified type is stronger proof than a partial PvP GUI sequence.
    // A missing/reordered preparation GUI must not permanently block PvP.
    hub(s,100);s.verified_pvp_type(101);
    assert(s.stage==PgrPvpScene::VERIFIED_PVP_ENTRY && !s.allowlisted());
    assert(s.vnext(102)&&s.pvp()&&s.allowlisted());
    s.reset();
    hub(s,110);prep(s,111);s.verified_pvp_type(112);
    assert(s.stage==PgrPvpScene::VERIFIED_PVP_ENTRY);
    assert(s.vnext(113)&&s.pvp()&&s.allowlisted());
    s.reset();
    hub(s,120);s.verified_pvp_type(121);
    assert(!s.vnext(15121)); // typed proof still expires after 15 seconds
    s.reset();
    npc(s,130);s.verified_pvp_type(131);
    assert(s.stage==PgrPvpScene::TRAINING_ENTRY); // never relabel NPC
    prep(s,132);s.verified_pvp_type(133);
    assert(s.vnext(134)&&s.training()&&s.allowlisted());
    s.verified_pvp_type(135);
    assert(s.training()); // never override active trainer battle
    s.reset();
    prep(s,100);assert(!s.vnext(101)); // preparation without hub
    hub(s,100);assert(!s.vnext(101)); // hub without preparation
    prep(s,102);assert(s.vnext(103));assert(s.vnext(104));
    s.gui("Niantic.Holoholo.Combat.Resolution","CombatResolveGuiController",105);
    assert(!s.vnext(106));
    hub(s,200);prep(s,201);
    npc(s,202);
    assert(!s.vnext(203)); // NPC only: never start a battle
    assert(s.stage==PgrPvpScene::TRAINING_ENTRY);
    prep(s,204);assert(s.vnext(205)&&s.training()); // verified team-leader path
    s.gui("Niantic.Holoholo.Combat.Resolution","CombatResolveGuiController",206);
    assert(!s.vnext(207)); // results disarm
    npc(s,300);assert(!s.vnext(301)); // Mateo dialogue must not attack
    prep(s,45301);assert(!s.vnext(45302)); // expired interstitial
    s.gui("Not.Combat","NpcTrainerInterstitialGui",400);
    prep(s,401);assert(!s.vnext(402)); // namespace matters
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
    hub(s,3500000);prep(s,3500001);
    assert(s.vnext(3500002)&&s.pvp());
    npc(s,3500003);assert(!s.vnext(3500004)); // NPC overrides PvP
    hub(s,3600000);prep(s,3600001);assert(s.vnext(3600002)&&s.pvp());
    hub(s,4000000);prep(s,4000001);assert(s.vnext(4000002));
    assert(!s.vnext(4000001)); // clock regression
    hub(s,5000000);prep(s,5000001);assert(s.vnext(5000002));
    hub(s,5000100);assert(!s.vnext(5000101)); // later hub disarms
    prep(s,5000102);assert(s.vnext(5000103)); // separate encounter
    hub(s,std::numeric_limits<uint64_t>::max()-100);
    assert(!s.vnext(std::numeric_limits<uint64_t>::max()));
    s.reset();
    s.verified_pvp_type(7000000);
    assert(s.vnext(7000001)); // private challenge typed + VNext
    s.reset(); assert(!s.vnext(7000002)); // VNext close tears down mode
    s.verified_pvp_type(8000000);
    s.gui("Niantic.Holoholo.Combat.Resolution","CombatResolveGuiController",8000001);
    assert(!s.vnext(8000002)); // results disarm before VNext
    s.verified_pvp_type(9000000);
    assert(s.vnext(9000001)&&s.pvp());
    s.reset();
    assert(!s.vnext(9000002)); // unknown battle type defaults off
    std::puts("PgrPvpSceneTest passed");
}
