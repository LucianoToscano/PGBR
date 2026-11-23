#pragma once
#include <stdint.h>
struct PgrSharedMetadataOutput;
// Smart battling: picks counters for the Pokemon you are fighting. Before a gym, raid or typed Rocket
// grunt battle the game's recommended party is replaced with the best counters from storage. During
// gym and Team GO Rocket battles the team member best suited to the current opponent is swapped in,
// when a new opponent appears or yours faints. One setting, off by default, on transport string slot
// 21 (F2 legacy or F3 battle preferences). Unity owner thread only.
void pgr_smart_battle_tick(uint64_t tid,const PgrSharedMetadataOutput *metadata);
void pgr_smart_battle_stop(uint64_t tid);
bool pgr_smart_battle_enabled();
// Rocket "choose your next Pokemon" prompt: of these SwapButtonVNext buttons (all usable), the index
// of the best counter to the current opponent, or -1 when that cannot be worked out.
int pgr_smart_battle_pick_prompt(void *const *buttons,int count);

// Read-only VNext actor-pair identity for the shared Rocket charged selector.
// 0 means unknown: the caller must not reuse a former opponent's effectiveness.
// Positive value is a process-local, non-reversible 31-bit change stamp, not a
// Pokemon ID. Works independently of the Smart Battling (swap) toggle.
// Invoke only on the attached Unity owner thread with an already rooted state.
// Read-only failure stages, never containing raw Pokemon/actor/account IDs.
// Useful when the opponent list exposed by training does not match the Rocket
// layout. Do not mistake successful metadata binding for successful actor read.
enum PgrSmartActorProbe {
 PGR_ACTOR_PROBE_NONE=0,
 PGR_ACTOR_PROBE_OK=1,
 PGR_ACTOR_PROBE_NO_STATE=2,
 PGR_ACTOR_PROBE_BUSY=3,
 PGR_ACTOR_PROBE_NO_BOOT=4,
 PGR_ACTOR_PROBE_NO_LOCAL_ACTOR=5,
 PGR_ACTOR_PROBE_NO_LOCAL_POKEMON=6,
 PGR_ACTOR_PROBE_NO_OPPONENTS=7,
 PGR_ACTOR_PROBE_NO_OPPONENT_ACTOR=8,
 PGR_ACTOR_PROBE_NO_OPPONENT_POKEMON=9,
 PGR_ACTOR_PROBE_EXCEPTION=10
};
int32_t pgr_smart_battle_vnext_actor_probe(void *battle_state,int32_t *reason);
int32_t pgr_smart_battle_vnext_actor_stamp(void *battle_state);

// Optional early probe on the live VNext GUI after its update callback.
// Uses the same battle-director state lookup already used by Smart Battling;
// returns 0 for unsupported GUI layouts, without guessing an actor ID.
int32_t pgr_smart_battle_vnext_gui_actor_stamp(void *vnext_gui);
