#pragma once
#include <cstdint>
struct PgrSharedMetadataOutput;
struct PgrTmRow { int visible; float x,y; int count; };
void pgr_tm_tick(uint64_t tid,const PgrSharedMetadataOutput *metadata,bool allowed);
void pgr_tm_stop(uint64_t tid);
void pgr_tm_set_allowed(bool allowed);
uint64_t pgr_tm_state(PgrTmRow out[3]);
bool pgr_tm_request(int row,uint64_t token);
extern "C" void pgr_tm_note_panel(void *panel);
