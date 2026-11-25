#pragma once
// What the combat host must do with an ALREADY OPEN shared battle executor (PvP or NPC
// training) on this tick.
//
// Before this policy existed the host answered "may I act?" with a single boolean. Any
// transient "no" — the PvE exclusion classifier failing to sample, the user toggling an
// action off for a moment, an option lease slipping by one frame — tore the verified
// scope down (`pvp_leave`). Re-arming then required a brand-new GUI sequence
// (Hub -> Preparation -> VNext), which does not happen for the battle already running,
// so the assist stayed dead until the game/app was restarted (issue #105).
//
// The distinction that was missing:
//   * AUTHORITATIVE end: host stopping/failed, the typed PvP/training scene lease
//     expired, or the PvE classifier positively reports raid/gym/Max -> leave, free the
//     root, publish `pvpExecutorReady:false`;
//   * UNVERIFIABLE for now: the classifier could not be read -> SUSPEND: no action of
//     any kind may run, but the verified scope stays anchored so the same battle can
//     resume as soon as the classifier is readable again;
//   * user turned the assistant off -> SUSPEND as well (the scope is still the same
//     verified battle; re-enabling during the battle must not need a new entry).
//
// Actions are allowed ONLY on KEEP. This never widens the automation: the executor acts
// exactly as before, and every suspension is a strict "do nothing" frame.
#include <cstdint>

namespace pgr_pvp_exec {

enum class Classifier : uint8_t {
  CLEAR = 0,        // typed sample succeeded and no PvE scope is active
  PVE_ACTIVE = 1,   // typed sample succeeded and a raid/gym/Max scope is active
  UNREADABLE = 2    // typed sample failed / the battle feature is unavailable
};

enum class Action : uint8_t {
  KEEP = 0,     // keep acting (the only state in which a game method may be invoked)
  SUSPEND = 1,  // keep the verified scope, withhold every action
  LEAVE = 2     // authoritative end: release the scope and the GC root
};

inline Action decide(bool terminal, bool scene_live, bool policy_live,
                     Classifier classifier) {
  if (terminal) return Action::LEAVE;
  if (!scene_live) return Action::LEAVE;
  if (classifier == Classifier::PVE_ACTIVE) return Action::LEAVE;
  if (classifier == Classifier::UNREADABLE) return Action::SUSPEND;
  if (!policy_live) return Action::SUSPEND;
  return Action::KEEP;
}

// Fail closed: nothing may be invoked unless the frame verdict is KEEP.
inline bool actions_allowed(Action action) { return action == Action::KEEP; }

// The leave reason is user-visible diagnostics only; it never carries identity data.
inline const char *leave_reason(bool terminal, bool scene_live, Classifier classifier) {
  if (terminal) return "host-stopped";
  if (!scene_live) return "scope-ended";
  if (classifier == Classifier::PVE_ACTIVE) return "battle-ended";
  return "scope-ended";
}

}  // namespace pgr_pvp_exec
