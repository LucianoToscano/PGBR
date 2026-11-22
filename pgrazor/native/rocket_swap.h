#pragma once
#include <stdint.h>
// Team GO Rocket auto swap: when a Pokemon faints in a Rocket battle and the game asks for the next
// one, a Pokemon whose button can be pressed is picked, the same as tapping it: the best counter when
// Smart battling is on, otherwise the first available. Only while Smart battling or Team GO Rocket
// automation is on (any of auto quick attack, auto charged attack, auto shield).
void pgr_rocket_swap_tick(uint64_t tid);
void pgr_rocket_swap_stop();

// Owner-thread observation only. While the knockout selector is open, no
// strategic Smart Battle swap may preempt it (even with auto KO swap disabled).
bool pgr_rocket_swap_prompt_active();
