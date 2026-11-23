#include "charge_effect_ui.h"
#include <cassert>
#include <cstdio>
#include <cstring>

namespace {
int field_slot=0,field_tier=0,field_show=0;
struct Display {
  int32_t slot=0;
  int32_t tier=0;
  uint8_t show=0;
};
void get(void *obj,void *field,void *out) {
  auto *d=static_cast<Display *>(obj);
  if(field==&field_slot)*static_cast<int32_t *>(out)=d->slot;
  else if(field==&field_tier)*static_cast<int32_t *>(out)=d->tier;
  else if(field==&field_show)*static_cast<uint8_t *>(out)=d->show;
}
}

int main() {
  using namespace pgr_charge_effect_ui;
  Binding b{};
  b.slot_field=&field_slot;b.tier_field=&field_tier;b.show_field=&field_show;
  b.slot_first=21;b.slot_second=22;
  b.tier_neutral=0;b.tier_super=1;b.tier_resisted=-1;b.valid=true;
  Display d{22,1,1};
  int same_state=42,other_state=43;
  auto s=observe(b,&d,&same_state,get);
  assert(s.slot==Slot::SECOND && s.tier==Tier::SUPER && s.batch_token);
  auto wire=pack(s);
  assert(wire>0);
  auto round=unpack(wire);
  assert(round.slot==Slot::SECOND && round.tier==Tier::SUPER && round.batch_token==s.batch_token);
  d.slot=21;d.tier=-1;
  s=observe(b,&d,&same_state,get);
  assert(s.slot==Slot::FIRST && s.tier==Tier::RESISTED);
  d.tier=0;s=observe(b,&d,&same_state,get);
  assert(s.tier==Tier::NEUTRAL);
  d.show=0;
  s=observe(b,&d,&same_state,get);
  assert(pack(s)==0); // hidden != neutral
  assert(s.slot==Slot::FIRST && s.tier==Tier::UNKNOWN && s.batch_token);
  wire=pack_observation(s);
  assert(wire>0 && unpack(wire).slot==Slot::UNKNOWN);
  round=unpack_observation(wire);
  assert(round.slot==Slot::FIRST && round.tier==Tier::UNKNOWN &&
         round.batch_token==s.batch_token); // preserves slot without guessing effect
  d.show=1;d.tier=99;
  assert(pack(observe(b,&d,&same_state,get))==0);
  d.tier=0;d.slot=99;
  assert(pack(observe(b,&d,&same_state,get))==0);
  d.slot=21;
  assert(pack(observe(b,&d,nullptr,get))==0);
  assert(pack(observe(b,&d,&other_state,nullptr))==0);
  assert(pack({Slot::FIRST,Tier::NEUTRAL,0})==0);
  assert(pack({Slot::FIRST,Tier::UNKNOWN,50})==0);
  assert(pack({Slot::FIRST,Tier::SUPER,0x200000u})==0);
  assert(pack(unpack(-1))==0);
  assert(pack_observation({Slot::UNKNOWN,Tier::UNKNOWN,50})==0);
  assert(pack_observation({Slot::FIRST,Tier::UNKNOWN,0})==0);
  assert(pack_observation({Slot::FIRST,Tier::UNKNOWN,50})>0);
  assert(unpack_observation(-1).slot==Slot::UNKNOWN);
  assert(!resolve(PgrBootBinding{},nullptr,b)); // missing runtime never enables
  assert(!b.valid);
  std::puts("charge effect UI: PASS (synthetic metadata fixture)");
}
