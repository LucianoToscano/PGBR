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
int32_t pgr_smart_battle_vnext_actor_stamp(void *battle_state);
