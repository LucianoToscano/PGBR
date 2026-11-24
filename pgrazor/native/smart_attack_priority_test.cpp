#include "smart_attack_priority.h"
#include <cassert>
#include <cstdio>
using namespace pgr_smart_priority;

int main() {
  // These identities and associations are synthetic. Array order models a
  // provider that has actually verified visual slots, not capture order.
  constexpr uintptr_t first = 1001, second = 2002;
  constexpr uint64_t session = 40, opponent = 50;
  Button b[] = {{first, first, session, opponent, true, Effect::NEUTRAL},
                {second, second, session, opponent, true, Effect::SUPER}};
  auto picked = [&]() { return choose(true, session, opponent, first, second, b); };
  const Effect effects[] = {Effect::RESISTED, Effect::NEUTRAL, Effect::SUPER};
  // All nine combinations in both orders; tie always selects first slot.
  for (auto a : effects) for (auto c : effects) {
    b[0].effect = a; b[1].effect = c;
    auto r = picked();
    assert(r.kind == ResultKind::SELECTED);
    assert(r.handle == (static_cast<int>(c) > static_cast<int>(a) ? second : first));
  }
  b[0].effect=Effect::NEUTRAL; b[1].effect=Effect::SUPER;
  assert(picked().handle==second); // second super beats first normal
  b[0].effect=Effect::RESISTED; b[1].effect=Effect::NEUTRAL;
  assert(picked().handle==second); // second normal beats first resisted
  assert(choose(false, session, opponent, first, second, b).kind==ResultKind::LEGACY);
  b[1].ready=false; assert(picked().kind==ResultKind::LEGACY);
  b[1].ready=true; b[0].ready=false; assert(picked().kind==ResultKind::LEGACY);
  b[1].ready=false; assert(picked().kind==ResultKind::NONE_READY);
  b[0].ready=b[1].ready=true;
  b[1].effect=Effect::UNKNOWN; assert(picked().kind==ResultKind::LEGACY);
  b[1].effect=Effect::SUPER; b[1].effect_for_handle=first;
  assert(picked().kind==ResultKind::LEGACY); // misassociated classification
  b[1].effect_for_handle=second;
  assert(choose(true, session, opponent, second, first, b).kind==ResultKind::LEGACY); // swapped slot binding
  b[1].opponent=opponent+1;
  assert(picked().kind==ResultKind::LEGACY); // switched target, stale classification
  b[1].opponent=opponent; b[0].session=session+1;
  assert(picked().kind==ResultKind::LEGACY); // stale session
  b[0].session=session; b[1].handle=first; b[1].effect_for_handle=first;
  assert(picked().kind==ResultKind::LEGACY); // duplicate identity
  b[1].handle=second; b[1].effect_for_handle=second;
  // A synthetic executor applies the choice to the indicated object, but uses
  // its original first-ready path whenever the policy says LEGACY. This does
  // not compile or execute either real game executor.
  auto fake_tick = [&](bool enabled, uint64_t current_opponent) -> uintptr_t {
    auto r = choose(enabled, session, current_opponent, first, second, b);
    if (r.kind == ResultKind::SELECTED) return r.handle;
    if (r.kind == ResultKind::NONE_READY) return 0; // caller continues fast attack
    return b[0].ready ? b[0].handle : b[1].ready ? b[1].handle : 0;
  };
  b[0].effect=Effect::NEUTRAL; b[1].effect=Effect::SUPER;
  assert(fake_tick(true, opponent)==second);
  assert(fake_tick(false, opponent)==first);
  assert(fake_tick(true, opponent+1)==first); // stale opponent => original path
  b[0].opponent=b[1].opponent=opponent+1;
  b[0].effect=Effect::SUPER; b[1].effect=Effect::RESISTED;
  assert(fake_tick(true, opponent+1)==first); // refreshed classifications
  b[0].opponent=b[1].opponent=opponent;
  b[1].effect_for_handle=first; assert(fake_tick(true, opponent)==first);
  b[1].effect_for_handle=second;
  b[0].ready=false; assert(fake_tick(true, opponent)==second);
  b[1].ready=false; assert(fake_tick(true, opponent)==0);
  b[0].ready=b[1].ready=true;
  assert(choose(true, 0, opponent, first, second, b).kind==ResultKind::LEGACY);
  assert(choose(true, session, 0, first, second, b).kind==ResultKind::LEGACY);
  assert(choose(true, session, opponent, first, second, nullptr).kind==ResultKind::LEGACY);
  b[1].effect=static_cast<Effect>(255); assert(picked().kind==ResultKind::LEGACY);

  int fire[] = {10, 0};
  int fire_grass[] = {10, 12}; // water: super vs fire, resisted by grass => neutral
  int rock_ground[] = {6, 5};
  int ghost_dark[] = {8, 17};
  int flying_ground[] = {3, 5}; // electric: one weakness, one immunity => resisted
  int unknown[] = {0, 0};
  int duplicate[] = {10, 10};
  assert(from_types(11, fire)==Effect::SUPER);
  assert(from_types(11, fire_grass)==Effect::NEUTRAL);
  assert(from_types(11, rock_ground)==Effect::SUPER);
  assert(from_types(1, ghost_dark)==Effect::RESISTED);
  assert(from_types(13, flying_ground)==Effect::RESISTED);
  assert(from_types(0, fire)==Effect::UNKNOWN);
  assert(from_types(11, unknown)==Effect::UNKNOWN);
  assert(from_types(11, duplicate)==Effect::UNKNOWN);
  assert(from_types(11, nullptr)==Effect::UNKNOWN);
  // New VNext path: only two equally fresh, correctly labelled display
  // instances may override the legacy first-ready order.
  using pgr_charge_effect_ui::Slot;
  using pgr_charge_effect_ui::Tier;
  DisplayCandidate pair[2] = {
      {{Slot::SECOND,Tier::SUPER,91},1000,true},
      {{Slot::FIRST,Tier::NEUTRAL,91},1001,true}
  };
  assert(choose_display(true,1020,pair,2)==0); // never trust capture order
  pair[0].sample.tier=Tier::RESISTED;
  assert(choose_display(true,1020,pair,2)==1);
  pair[0].sample.tier=Tier::NEUTRAL;
  assert(choose_display(true,1020,pair,2)==1); // first-slot tie
  pair[1].sample.tier=Tier::SUPER;
  assert(choose_display(true,1020,pair,2)==1);
  assert(choose_display(false,1020,pair,2)==-1);
  assert(choose_display(true,1020,pair,1)==-1);
  pair[1].ready=false;
  assert(choose_display(true,1020,pair,2)==-1); // only one ready -> legacy
  pair[1].ready=true;
  pair[1].sample.batch_token=92;
  assert(choose_display(true,1020,pair,2)==-1); // unequal battle-state observations
  pair[1].sample.batch_token=91;
  pair[1].observed_ms=700;
  assert(choose_display(true,1020,pair,2)==-1); // stale target/label
  pair[1].observed_ms=1001;
  pair[1].sample.slot=Slot::SECOND;
  assert(choose_display(true,1020,pair,2)==-1); // duplicate button identity
  pair[1].sample.slot=Slot::FIRST;
  pair[0].sample.tier=Tier::UNKNOWN;
  assert(choose_display(true,1020,pair,2)==1); // other has verified SUPER
  pair[0].sample.tier=Tier::RESISTED;
  assert(choose_display(true,900,pair,2)==-1); // clock inversion
  // Diagnostic reason must explain why the original first-ready choice won.
  DisplayCandidate diag[2] = {
      {{Slot::FIRST,Tier::NEUTRAL,14},1000,true},
      {{Slot::SECOND,Tier::SUPER,14},1000,true}
  };
  assert(inspect_display(true,1020,diag,2).reason==DisplayReason::EFFECTIVENESS);
  assert(inspect_display(true,1020,diag,2).index==1);
  assert(inspect_display(false,1020,diag,2).reason==DisplayReason::OFF);
  assert(inspect_display(true,1020,diag,1).reason==DisplayReason::UNAVAILABLE_PAIR);
  diag[1].ready=false;
  assert(inspect_display(true,1020,diag,2).reason==DisplayReason::ONLY_ONE_READY);
  diag[1].ready=true;diag[1].sample.tier=Tier::UNKNOWN;
  assert(inspect_display(true,1020,diag,2).reason==DisplayReason::UNKNOWN_EFFECT);
  diag[1].sample.tier=Tier::SUPER;diag[1].observed_ms=100;
  assert(inspect_display(true,1020,diag,2).reason==DisplayReason::STALE_EFFECT);
  diag[1].observed_ms=1000;diag[1].sample.batch_token=15;
  assert(inspect_display(true,1020,diag,2).reason==DisplayReason::UNVERIFIED_SLOTS);
  // Rocket alone decides whether to keep charging a stronger, not-yet-ready
  // move; shared PvP/NPC scopes must never have their own move selector.
  DisplayCandidate energy[2] = {
      {{Slot::FIRST,Tier::RESISTED,99},2000,true},
      {{Slot::SECOND,Tier::SUPER,99},2001,false}
  };
  assert(better_unready(true,2030,energy,2));
  assert(!better_unready(false,2030,energy,2));
  energy[1].sample.tier=Tier::NEUTRAL;
  assert(better_unready(true,2030,energy,2));
  energy[0].sample.tier=Tier::SUPER;
  assert(!better_unready(true,2030,energy,2));
  energy[0].sample.tier=Tier::NEUTRAL;
  assert(!better_unready(true,2030,energy,2)); // tied move: first-ready immediate
  energy[1].sample.tier=Tier::SUPER;
  energy[0].sample.tier=Tier::RESISTED;
  energy[1].ready=true;
  assert(!better_unready(true,2030,energy,2)); // both charged -> direct best tap
  energy[0].ready=false;
  assert(!better_unready(true,2030,energy,2)); // only better move ready -> no wait
  energy[0].ready=true;
  energy[1].ready=false;
  energy[1].observed_ms=1000;
  assert(!better_unready(true,2030,energy,2)); // stale => original immediate
  energy[1].observed_ms=2001;
  energy[1].sample.batch_token=100;
  assert(!better_unready(true,2030,energy,2)); // opponent display mismatch
  energy[1].sample.batch_token=99;
  energy[1].sample.slot=Slot::FIRST;
  assert(!better_unready(true,2030,energy,2)); // unverified mapping
  // Reproduction: the first charged button has no displayed effectiveness
  // subtitle, while Charge2 shows SuperEffective. Never manufacture Normal.
  DisplayCandidate hidden[2] = {
      {{Slot::FIRST,Tier::UNKNOWN,55},4000,true},
      {{Slot::SECOND,Tier::SUPER,55},4001,true}
  };
  assert(inspect_display(true,4020,hidden,2).reason==DisplayReason::KNOWN_SUPER);
  assert(choose_display(true,4020,hidden,2)==1);
  hidden[1].ready=false;
  assert(better_unready(true,4020,hidden,2)); // keep doing quick until Charge2
  hidden[1].ready=true;hidden[0].ready=false;
  assert(!better_unready(true,4020,hidden,2));
  hidden[0].ready=true;hidden[0].sample.batch_token=56;
  assert(choose_display(true,4020,hidden,2)==-1); // mismatch never preferred
  hidden[0].sample.batch_token=55;
  hidden[0].observed_ms=3700;
  assert(choose_display(true,4020,hidden,2)==-1); // stale never preferred
  hidden[0].observed_ms=4000;
  hidden[1].sample.tier=Tier::UNKNOWN;
  assert(choose_display(true,4020,hidden,2)==-1); // no verified SUPER
  hidden[0].sample.tier=Tier::NEUTRAL;
  assert(choose_display(true,4020,hidden,2)==-1); // Normal vs hidden != ranked
  // Final authorization is distinct from candidate ordering. The old
  // behavior used the first-ready button when labels aged past 200 ms,
  // when just one charged attack was ready or when metadata was incomplete.
  DisplayCandidate guarded[2] = {
      {{Slot::FIRST,Tier::NEUTRAL,77},5000,true},
      {{Slot::SECOND,Tier::SUPER,77},5000,true}
  };
  auto allowed=admit_display(true,5050,guarded,2);
  assert(!allowed.hold&&allowed.index==1);
  guarded[1].ready=false;
  allowed=admit_display(true,5050,guarded,2);
  assert(allowed.hold&&allowed.index==-1&&allowed.reason==DisplayReason::ONLY_ONE_READY); // reserve for confirmed super
  guarded[1].ready=true;
  allowed=admit_display(true,5300,guarded,2);
  assert(allowed.hold&&allowed.index==-1&&allowed.reason==DisplayReason::STALE_EFFECT);
  guarded[1].observed_ms=5300;guarded[0].observed_ms=5300;
  guarded[1].sample.batch_token=78;
  allowed=admit_display(true,5320,guarded,2);
  assert(allowed.hold&&allowed.reason==DisplayReason::UNVERIFIED_SLOTS);
  guarded[1].sample.batch_token=77;
  guarded[0].sample.tier=Tier::UNKNOWN;
  allowed=admit_display(true,5320,guarded,2);
  assert(!allowed.hold&&allowed.index==1&&allowed.reason==DisplayReason::KNOWN_SUPER);
  guarded[1].ready=false;
  allowed=admit_display(true,5320,guarded,2);
  assert(allowed.hold&&allowed.index==-1); // ready UNKNOWN is not treated as neutral
  guarded[1].ready=true;
  guarded[1].sample.tier=Tier::UNKNOWN;
  assert(admit_display(true,5320,guarded,2).hold); // no fabricated neutral
  guarded[0].sample.tier=Tier::NEUTRAL;
  assert(admit_display(true,5320,guarded,2).hold); // Normal vs unknown not ranked
  guarded[1].sample.tier=Tier::NEUTRAL;
  guarded[0].ready=false;
  allowed=admit_display(true,5320,guarded,2);
  assert(!allowed.hold&&allowed.index==1); // equal tier: use ready one
  // READY policy: reserve for confirmed SUPER, preserve immediate fallback
  // when neither move is SUPER and classifications are known.
  guarded[0].ready=true;
  guarded[0].sample.tier=Tier::RESISTED;
  guarded[1].sample.tier=Tier::NEUTRAL;
  guarded[1].ready=false;
  allowed=admit_display(true,5320,guarded,2);
  assert(!allowed.hold&&allowed.index==0); // resisted ready, neutral charging
  guarded[0].sample.tier=Tier::NEUTRAL;
  guarded[1].sample.tier=Tier::SUPER;
  allowed=admit_display(true,5320,guarded,2);
  assert(allowed.hold&&allowed.index==-1); // neutral ready, super charging
  guarded[0].ready=false;
  guarded[1].ready=true;
  allowed=admit_display(true,5320,guarded,2);
  assert(!allowed.hold&&allowed.index==1); // super ready, neutral charging
  guarded[0].ready=true;
  allowed=admit_display(true,5320,guarded,2);
  assert(!allowed.hold&&allowed.index==1); // super wins when both ready
  guarded[0].ready=false;guarded[1].ready=false;
  assert(admit_display(true,5320,guarded,2).hold); // neither ready: fast remains available
  guarded[0].ready=true;
  assert(!admit_display(false,5320,guarded,2).hold); // OFF: original order
  assert(admit_display(true,5320,guarded,1).hold); // engine handles actual single-display separately
  std::puts("smart attack priority: PASS (pure synthetic policy; no game action)");
}
