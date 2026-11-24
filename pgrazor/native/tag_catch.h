#pragma once
#include <stdint.h>
struct PgrSharedMetadataOutput;
void pgr_tag_catch_tick(uint64_t,const PgrSharedMetadataOutput*);
void pgr_tag_catch_stop(uint64_t);
