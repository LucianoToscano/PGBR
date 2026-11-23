#pragma once
// Simple charged-attack priority. Pure policy: it neither observes nor invokes the game.
// Callers must establish the live button/slot association and revalidate before acting.
#include "battle_counter.h"
#include <cstdint>
#include <cstddef>
#include "charge_effect_ui.h"

namespace pgr_smart_priority {

enum class Effect : uint8_t { UNKNOWN, RESISTED, NEUTRAL, SUPER };
enum class ResultKind : uint8_t { LEGACY, NONE_READY, SELECTED };

// A confirmed pair of opponent types is required. 0 is permitted only in the
// second slot, meaning the provider has confirmed a single-typed opponent.
inline Effect from_types(int move_type, const int opponent[2]) {
  if (!opponent || move_type < 1 || move_type > pgr_counter::kTypeCount ||
      opponent[0] < 1 || opponent[0] > pgr_counter::kTypeCount ||
      opponent[1] < 0 || opponent[1] > pgr_counter::kTypeCount ||
      opponent[1] == opponent[0]) return Effect::UNKNOWN;
  int total = pgr_counter::kChart[move_type - 1][opponent[0] - 1];
  if (opponent[1]) total += pgr_counter::kChart[move_type - 1][opponent[1] - 1];
  return total > 0 ? Effect::SUPER : total < 0 ? Effect::RESISTED : Effect::NEUTRAL;
}

struct Button {
  uintptr_t handle = 0;              // opaque live button/display identity, never a positional guess
  uintptr_t effect_for_handle = 0;   // identity explicitly associated with this classification
  uint64_t session = 0, opponent = 0;
  bool ready = false;                // checked by the existing executor
  Effect effect = Effect::UNKNOWN;
};
struct Result {
  ResultKind kind = ResultKind::LEGACY;
  uintptr_t handle = 0;
};

// buttons[0] and [1] MUST be the provider-verified first and second buttons,
// not their observation/capture order. LEGACY means leave the old executor
// unchanged; a single ready candidate needs no comparison and also uses LEGACY.
inline Result choose(bool enabled, uint64_t session, uint64_t opponent,
                     uintptr_t first, uintptr_t second, const Button buttons[2]) {
  if (!enabled || !buttons) return {};
  if (!buttons[0].ready && !buttons[1].ready) return {ResultKind::NONE_READY, 0};
  if (!buttons[0].ready || !buttons[1].ready) return {};
  // The expected slot bindings must come from the independently verified game
  // adapter; equality checks cannot make an unverified provider trustworthy.
  if (!session || !opponent || !first || !second || first == second ||
      buttons[0].handle != first || buttons[1].handle != second) return {};
  for (int i = 0; i < 2; ++i) {
    const auto& b = buttons[i];
    if (b.handle != b.effect_for_handle || b.session != session || b.opponent != opponent ||
        (b.effect != Effect::RESISTED && b.effect != Effect::NEUTRAL && b.effect != Effect::SUPER)) return {};
  }
  const int selected = static_cast<int>(buttons[1].effect) > static_cast<int>(buttons[0].effect) ? 1 : 0;
  return {ResultKind::SELECTED, buttons[selected].handle};
}

// Optional VNext display-only selector. Each candidate refers to a rooted
// ChargeAttackDisplayVNext already observed AFTER its original state update.
// A batch token is only an update-coherence hint, not an opponent ID.
// A strict hold is allowed with only one ready button when the OTHER button
// is verified as strictly more effective, and fast attacks can keep charging.
// A direct effectiveness selection still requires BOTH buttons ready. Never
// treat the capture order or a batch pointer as proof of opponent identity.
struct DisplayCandidate {
  pgr_charge_effect_ui::Sample sample{};
  uint64_t observed_ms = 0;
  bool ready = false;
};
// Why a charged tap used smart priority or the original first-ready path.
// Packed into optional action telemetry; no timer overrides a strict ranking.
enum class DisplayReason : uint8_t {
  OFF=0, EFFECTIVENESS=1, UNAVAILABLE_PAIR=2, ONLY_ONE_READY=3,
  UNKNOWN_EFFECT=4, STALE_EFFECT=5, UNVERIFIED_SLOTS=6,
  KNOWN_SUPER=7, ACTOR_UNKNOWN=8, ACTOR_REFRESH=9
};
struct DisplayDecision {
  int index=-1;
  DisplayReason reason=DisplayReason::OFF;
};
inline DisplayDecision inspect_display(bool enabled,uint64_t now,
                                       const DisplayCandidate *items,
                                       std::size_t count) {
  if(!enabled)return {-1,DisplayReason::OFF};
  if(!items||count!=2||now==0)return {-1,DisplayReason::UNAVAILABLE_PAIR};
  constexpr uint64_t kMaxAgeMs=200;
  const auto &a=items[0],&b=items[1];
  if(!a.ready||!b.ready)return {-1,DisplayReason::ONLY_ONE_READY};
  if(!a.sample.batch_token||!b.sample.batch_token||
     pgr_charge_effect_ui::pack_observation(a.sample)==0||
     pgr_charge_effect_ui::pack_observation(b.sample)==0)
    return {-1,DisplayReason::UNKNOWN_EFFECT};
  if(!a.observed_ms||!b.observed_ms||a.observed_ms>now||b.observed_ms>now||
     now-a.observed_ms>kMaxAgeMs||now-b.observed_ms>kMaxAgeMs||
     a.observed_ms>b.observed_ms+kMaxAgeMs||
     b.observed_ms>a.observed_ms+kMaxAgeMs)
    return {-1,DisplayReason::STALE_EFFECT};
  if(a.sample.batch_token!=b.sample.batch_token||
     a.sample.slot==b.sample.slot)
    return {-1,DisplayReason::UNVERIFIED_SLOTS};
  const int first=a.sample.slot==pgr_charge_effect_ui::Slot::FIRST?0:1;
  const int second=1-first;
  const auto tier1=items[first].sample.tier;
  const auto tier2=items[second].sample.tier;
  // When the game hides one label, UNKNOWN is not Normal. However, a
  // concurrently verified SUPER button is a positive and safer preference
  // over first-ready ordering. This is an opt-in best-known preference, not
  // an assertion of strictly higher damage or of the unknown button's tier.
  if(tier1==pgr_charge_effect_ui::Tier::UNKNOWN &&
     tier2==pgr_charge_effect_ui::Tier::SUPER)
    return {second,DisplayReason::KNOWN_SUPER};
  if(tier2==pgr_charge_effect_ui::Tier::UNKNOWN &&
     tier1==pgr_charge_effect_ui::Tier::SUPER)
    return {first,DisplayReason::KNOWN_SUPER};
  if(pgr_charge_effect_ui::pack(a.sample)==0||
     pgr_charge_effect_ui::pack(b.sample)==0)
    return {-1,DisplayReason::UNKNOWN_EFFECT};
  return {static_cast<int>(tier2)>static_cast<int>(tier1)?second:first,
          DisplayReason::EFFECTIVENESS};
}
// Rocket VNext can keep the normal quick attacks running while a strictly
// better, verified move is still building energy. The SAME Rocket executor
// serves typed battle variants; there is no independent PvP selector.
// The comparison must be rerun on every tick, with fresh matching samples.
// Missing/old classifications are rejected by admit_display when two slots
// are observed. The Rocket owner must keep the normal quick gesture available
// without automatically falling back to an unverified charged button.
inline bool better_unready(bool enabled,uint64_t now,
                           const DisplayCandidate *items,std::size_t count) {
  if(!enabled||!items||count!=2||items[0].ready==items[1].ready)return false;
  DisplayCandidate hypothetical[2]={items[0],items[1]};
  hypothetical[0].ready=hypothetical[1].ready=true;
  const auto rank=inspect_display(true,now,hypothetical,2);
  if((rank.reason!=DisplayReason::EFFECTIVENESS &&
      rank.reason!=DisplayReason::KNOWN_SUPER)||rank.index<0)return false;
  const int ready=items[0].ready?0:1;
  // Suppress only when the selected button has a verified higher tier or
  // a verified SUPER tier versus an explicitly UNKNOWN (hidden) other tier.
  // Never claim the hidden tier is Normal; both remain revalidated per tick.
  return rank.index!=ready &&
    static_cast<int>(items[rank.index].sample.tier)>
    static_cast<int>(items[ready].sample.tier);
}
// Final VNext charged-action authorization. Merely ordering the candidates
// is not sufficient: a fallback-to-first loop can issue a weaker charge even
// after the classifier rejected or lost its evidence. With Smart Attack ON,
// never automatically issue an unranked charge when a pair was observed.
// The quick gesture and manual input remain independent of this decision.
struct DisplayAdmission {
  int index=-1;                 // 0 / 1 only when this exact candidate may fire
  bool hold=false;              // true: do not invoke ANY charged button
  DisplayReason reason=DisplayReason::OFF;
};
inline DisplayAdmission admit_display(bool enabled,uint64_t now,
                                      const DisplayCandidate *items,
                                      std::size_t count) {
  if(!enabled)return {-1,false,DisplayReason::OFF}; // original-order mode
  if(!items||count!=2)return {-1,true,DisplayReason::UNAVAILABLE_PAIR};
  DisplayCandidate hypothetical[2]={items[0],items[1]};
  hypothetical[0].ready=hypothetical[1].ready=true;
  const auto ranking=inspect_display(true,now,hypothetical,2);
  // Old behavior fell back to first ready here; the new Smart Attack mode
  // fail-closes charged automation rather than undoing its own policy.
  if(ranking.index<0)return {-1,true,ranking.reason};
  const int best=ranking.index,other=1-best;
  if(items[best].ready)return {best,false,ranking.reason};
  const auto best_tier=items[best].sample.tier;
  const auto other_tier=items[other].sample.tier;
  // Equal-effect moves may use whichever is already ready. UNKNOWN never
  // becomes Normal; a confirmed SUPER still wins over an UNKNOWN alternative.
  if(items[other].ready&&best_tier==other_tier)
    return {other,false,ranking.reason};
  return {-1,true,DisplayReason::ONLY_ONE_READY};
}
inline int choose_display(bool enabled,uint64_t now,const DisplayCandidate *items,
                          std::size_t count) {
  return inspect_display(enabled,now,items,count).index;
}
} // namespace pgr_smart_priority
