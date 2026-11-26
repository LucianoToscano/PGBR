#include "charge_effect_ui.h"
#include <cassert>
#include <cstdio>
#include <cstring>
#include <cstdlib>

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
struct MetaClass;
struct MetaField { const char *name; MetaClass *owner; MetaClass *type; int flags; int32_t value; };
struct MetaClass { const char *name; bool is_enum; MetaField *fields[8]; };
MetaClass int_type{"System.Int32",false,{}}, bool_type{"System.Boolean",false,{}};
MetaClass slot_type{"Slot",true,{}},tier_type{"Tier",true,{}},display_type{"Display",false,{}};
MetaField charge{"Charge",&slot_type,&slot_type,0x8050,21},charge2{"Charge2",&slot_type,&slot_type,0x8050,22};
MetaField normal{"Normal",&tier_type,&tier_type,0x8050,0},super{"SuperEffective",&tier_type,&tier_type,0x8050,1},
          resisted{"NotVeryEffective",&tier_type,&tier_type,0x8050,-1};
MetaField renamed_slot{"renamed_slot",&display_type,&slot_type,0,0},
          renamed_tier{"renamed_tier",&display_type,&tier_type,0,0},
          show{"showEffectiveness",&display_type,&bool_type,0,0},
          duplicate{"duplicate",&display_type,&slot_type,0,0};
const void *find_field(const void *c,const char *name){
  for(auto *f:static_cast<const MetaClass *>(c)->fields)
    if(f&&std::strcmp(f->name,name)==0)return f;
  return nullptr;
}
const void *next_field(const void *c,void **iter){
  const auto i=reinterpret_cast<uintptr_t>(*iter);
  if(i>=8)return nullptr;
  *iter=reinterpret_cast<void *>(i+1);
  return static_cast<const MetaClass *>(c)->fields[i];
}
const void *owner(const void *f){return static_cast<const MetaField *>(f)->owner;}
int field_flags(const void *f){return static_cast<const MetaField *>(f)->flags;}
const void *type_of(const void *f){return static_cast<const MetaField *>(f)->type;}
const void *class_of(const void *t){return t;}
bool is_enum(const void *c){return static_cast<const MetaClass *>(c)->is_enum;}
const void *base_type(const void *){return &int_type;}
char *type_name(const void *t){
  const char *name=static_cast<const MetaClass *>(t)->name;
  auto *result=static_cast<char *>(std::malloc(std::strlen(name)+1));
  if(result)std::strcpy(result,name);
  return result;
}
void literal_value(const void *f,void *out){*static_cast<int32_t *>(out)=static_cast<const MetaField *>(f)->value;}

void verify_resolver(){
  using namespace pgr_charge_effect_ui;
  slot_type.fields[0]=&charge;slot_type.fields[1]=&charge2;
  tier_type.fields[0]=&normal;tier_type.fields[1]=&super;tier_type.fields[2]=&resisted;
  display_type.fields[0]=&renamed_slot;display_type.fields[1]=&renamed_tier;display_type.fields[2]=&show;
  PgrBootBinding boot{};boot.status=PGR_BOOT_OK;
  boot.meta.class_get_field_from_name=reinterpret_cast<void *>(find_field);
  boot.meta.field_get_parent=reinterpret_cast<void *>(owner);
  boot.meta.field_get_flags=reinterpret_cast<void *>(field_flags);
  boot.meta.field_get_type=reinterpret_cast<void *>(type_of);
  boot.meta.class_from_type=reinterpret_cast<void *>(class_of);
  boot.meta.class_is_enum=reinterpret_cast<void *>(is_enum);
  boot.meta.class_enum_basetype=reinterpret_cast<void *>(base_type);
  boot.meta.type_get_name=reinterpret_cast<void *>(type_name);
  boot.meta.mem_free=reinterpret_cast<void *>(std::free);
  boot.runtime.field_static_get_value=reinterpret_cast<void *>(literal_value);
  Binding b{};
  assert(!resolve(boot,&display_type,b)&&!b.valid);
  assert(resolve(boot,&display_type,b,next_field)&&b.valid);
  assert(b.slot_field==&renamed_slot&&b.tier_field==&renamed_tier&&b.show_field==&show);
  assert(b.slot_first==21&&b.slot_second==22&&b.tier_neutral==0&&b.tier_super==1&&b.tier_resisted==-1);
  display_type.fields[3]=&duplicate;
  assert(!resolve(boot,&display_type,b,next_field)&&!b.valid);
  display_type.fields[3]=nullptr;
  show.flags=0x10;
  assert(!resolve(boot,&display_type,b,next_field)&&!b.valid);
  show.flags=0;
  super.value=0;
  assert(!resolve(boot,&display_type,b,next_field)&&!b.valid);
  super.value=1;
  renamed_slot.name="ervu";renamed_tier.name="erwh";
  assert(resolve(boot,&display_type,b)&&b.valid);
}
}

int main() {
  verify_resolver();
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
