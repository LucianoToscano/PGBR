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
// This never overrides the legacy path unless BOTH charged buttons are ready,
// recently observed together and their typed effect classifications are valid.
struct DisplayCandidate {
  pgr_charge_effect_ui::Sample sample{};
  uint64_t observed_ms = 0;
  bool ready = false;
};
// Why a charged tap used smart priority or the original first-ready path.
// Packed into optional action telemetry only; never governs a new timing gate.
enum class DisplayReason : uint8_t {
  OFF=0, EFFECTIVENESS=1, UNAVAILABLE_PAIR=2, ONLY_ONE_READY=3,
  UNKNOWN_EFFECT=4, STALE_EFFECT=5, UNVERIFIED_SLOTS=6, HOLD_EXPIRED=7
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
     pgr_charge_effect_ui::pack(a.sample)==0||
     pgr_charge_effect_ui::pack(b.sample)==0)
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
  return {static_cast<int>(tier2)>static_cast<int>(tier1)?second:first,
          DisplayReason::EFFECTIVENESS};
}
// Rocket VNext can continue its normal quick attacks briefly if an observed,
// strictly stronger loaded move is still building energy. This policy is the
// SAME for typed Rocket-derived sessions (League GO, friends, training); no
// separate PvP executor or tap location is inferred. Requires fresh, matching
// effectiveness readings for BOTH buttons, and exactly one ready button.
// The caller owns the bounded waiting clock and must revalidate on each tick.
inline bool better_unready(bool enabled,uint64_t now,
                           const DisplayCandidate *items,std::size_t count) {
  if(!enabled||!items||count!=2||items[0].ready==items[1].ready)return false;
  DisplayCandidate hypothetical[2]={items[0],items[1]};
  hypothetical[0].ready=hypothetical[1].ready=true;
  const auto rank=inspect_display(true,now,hypothetical,2);
  if(rank.reason!=DisplayReason::EFFECTIVENESS||rank.index<0)return false;
  const int ready=items[0].ready?0:1;
  // Strictly better: ties never justify withholding a ready charge.
  return rank.index!=ready &&
    static_cast<int>(items[rank.index].sample.tier)>
    static_cast<int>(items[ready].sample.tier);
}
inline int choose_display(bool enabled,uint64_t now,const DisplayCandidate *items,
                          std::size_t count) {
  return inspect_display(enabled,now,items,count).index;
}
} // namespace pgr_smart_priority
