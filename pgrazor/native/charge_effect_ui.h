#pragma once
// Optional version-specific VNext charged-display classifier (0.437.0).
// Resolves fields and enum literals by exact runtime metadata, never by guessed offsets.
// Only observations produced AFTER ObserveBattleState can be used. A state token is
// a short-lived display-update batch hint, NOT a verified opponent identifier.
#include "runtime_bootstrap.h"
#include <cstdint>
#include <cstring>

namespace pgr_charge_effect_ui {
enum class Tier : uint8_t { UNKNOWN=0, RESISTED=1, NEUTRAL=2, SUPER=3 };
enum class Slot : uint8_t { UNKNOWN=0, FIRST=1, SECOND=2 };
struct Sample {
  Slot slot = Slot::UNKNOWN;
  Tier tier = Tier::UNKNOWN;
  uint32_t batch_token = 0;
};
struct Binding {
  const void *slot_field = nullptr, *tier_field = nullptr, *show_field = nullptr;
  int32_t slot_first = 0, slot_second = 0;
  int32_t tier_resisted = 0, tier_neutral = 0, tier_super = 0;
  bool valid = false;
};

// This compact payload reuses the previously ignored aux of PGR_RC_OBS_CHARGE.
// 0 == unavailable; all other payloads must contain nonzero slot, tier and batch.
// No pointer or Pokemon/account identifier crosses the leaf boundary.
inline int32_t pack(Sample s) {
  const unsigned slot=static_cast<unsigned>(s.slot),tier=static_cast<unsigned>(s.tier);
  if(!s.batch_token||s.batch_token>0x1FFFFFu||slot<1||slot>2||tier<1||tier>3)return 0;
  return static_cast<int32_t>((s.batch_token<<4)|(tier<<2)|slot);
}
inline Sample unpack(int32_t aux) {
  if(aux<=0)return {};
  uint32_t v=static_cast<uint32_t>(aux);
  Sample s{static_cast<Slot>(v&3),static_cast<Tier>((v>>2)&3),v>>4};
  return pack(s)==aux?s:Sample{};
}
inline uint32_t state_batch_token(const void *state) {
  if(!state)return 0;
  auto x=static_cast<uint64_t>(reinterpret_cast<uintptr_t>(state));
  // Mix full pointer bits; collisions only cause fallback to be less strict,
  // never establish the identity of a Pokemon or the effective target.
  x=(x>>4)^(x>>23)^(x>>41);
  const uint32_t token=static_cast<uint32_t>(x&0x1FFFFFu);
  return token?token:1u;
}

inline bool type_named(const PgrMetaExports &m,const void *type,const char *expected) {
  if(!type||!m.type_get_name||!m.mem_free)return false;
  auto get=reinterpret_cast<char *(*)(const void *)>(m.type_get_name);
  auto free=reinterpret_cast<void (*)(void *)>(m.mem_free);
  char *name=get(type);
  if(!name)return false;
  const bool match=std::strcmp(name,expected)==0;
  free(name);
  return match;
}

inline bool resolve(const PgrBootBinding &boot,const void *display_class,Binding &out) {
  out={};
  const auto &m=boot.meta;
  if(!display_class||boot.status!=PGR_BOOT_OK||
     !m.class_get_field_from_name||!m.field_get_parent||!m.field_get_flags||
     !m.field_get_type||!m.class_from_type||!m.class_is_enum||
     !m.class_enum_basetype||!m.type_get_name||!m.mem_free||
     !boot.runtime.field_static_get_value)return false;
  auto find=reinterpret_cast<const void *(*)(const void *,const char *)>(m.class_get_field_from_name);
  auto parent=reinterpret_cast<const void *(*)(const void *)>(m.field_get_parent);
  auto flags=reinterpret_cast<int (*)(const void *)>(m.field_get_flags);
  auto field_type=reinterpret_cast<const void *(*)(const void *)>(m.field_get_type);
  auto from_type=reinterpret_cast<const void *(*)(const void *)>(m.class_from_type);
  auto is_enum=reinterpret_cast<bool (*)(const void *)>(m.class_is_enum);
  auto enum_base=reinterpret_cast<const void *(*)(const void *)>(m.class_enum_basetype);
  auto static_get=reinterpret_cast<void (*)(const void *,void *)>(boot.runtime.field_static_get_value);
  const void *slot=find(display_class,"ervu");
  const void *tier=find(display_class,"erwh");
  const void *show=find(display_class,"showEffectiveness");
  if(!slot||!tier||!show||parent(slot)!=display_class||
     parent(tier)!=display_class||parent(show)!=display_class||
     (flags(slot)&0x50)||(flags(tier)&0x50)||(flags(show)&0x50)||
     !type_named(m,field_type(show),"System.Boolean"))return false;
  const void *slot_cls=from_type(field_type(slot));
  const void *tier_cls=from_type(field_type(tier));
  if(!slot_cls||!tier_cls||!is_enum(slot_cls)||!is_enum(tier_cls)||
     !type_named(m,enum_base(slot_cls),"System.Int32")||
     !type_named(m,enum_base(tier_cls),"System.Int32"))return false;
  auto literal=[&](const void *cls,const char *name,int32_t &v)->bool{
    const void *f=find(cls,name);
    if(!f||parent(f)!=cls||(static_cast<uint32_t>(flags(f))&0x8050u)!=0x8050u)return false;
    static_get(f,&v);
    return true;
  };
  if(!literal(slot_cls,"Charge",out.slot_first)||
     !literal(slot_cls,"Charge2",out.slot_second)||
     !literal(tier_cls,"Normal",out.tier_neutral)||
     !literal(tier_cls,"SuperEffective",out.tier_super)||
     !literal(tier_cls,"NotVeryEffective",out.tier_resisted)||
     out.slot_first==out.slot_second||
     out.tier_neutral==out.tier_super||out.tier_neutral==out.tier_resisted||
     out.tier_super==out.tier_resisted)return false;
  out.slot_field=slot;out.tier_field=tier;out.show_field=show;out.valid=true;
  return true;
}

inline Sample observe(const Binding &binding,void *display,void *state,
                     void (*field_get_value)(void *,void *,void *)) {
  if(!binding.valid||!display||!state||!field_get_value)return {};
  uint8_t show=0;
  field_get_value(display,const_cast<void *>(binding.show_field),&show);
  // False/ambiguous can be a stale hidden default, NOT confirmed neutral.
  if(show!=1)return {};
  int32_t slot=0,effect=0;
  field_get_value(display,const_cast<void *>(binding.slot_field),&slot);
  field_get_value(display,const_cast<void *>(binding.tier_field),&effect);
  Sample s{};
  s.slot=slot==binding.slot_first?Slot::FIRST:
         slot==binding.slot_second?Slot::SECOND:Slot::UNKNOWN;
  s.tier=effect==binding.tier_super?Tier::SUPER:
         effect==binding.tier_neutral?Tier::NEUTRAL:
         effect==binding.tier_resisted?Tier::RESISTED:Tier::UNKNOWN;
  s.batch_token=state_batch_token(state);
  return s;
}
}  // namespace pgr_charge_effect_ui
