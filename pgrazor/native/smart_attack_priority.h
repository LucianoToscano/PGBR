#pragma once
// Simple charged-attack priority. Pure policy: it neither observes nor invokes the game.
// Callers must establish the live button/slot association and revalidate before acting.
#include "battle_counter.h"
#include <cstdint>

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
} // namespace pgr_smart_priority
