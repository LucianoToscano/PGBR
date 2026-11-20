#pragma once
#include <stdint.h>

// Only the visual Unicode text sink may be replaced. Never call a game account
// getter, write player identity, or alter network / RPC contents.
int pgrazor_trainer_privacy_configure(int enabled, const uint16_t *trainer, uint32_t length);
int pgrazor_trainer_privacy_status();
void pgrazor_trainer_privacy_tick();
