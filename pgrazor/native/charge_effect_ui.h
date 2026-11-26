#pragma once
// Optional VNext charged-display classifier; legacy names or unique typed fields.
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
// 0 == unavailable. Exact effect payloads contain slot, tier and batch.
 // Slot-only payloads preserve the game-verified Charge / Charge2 identity
 // when showEffectiveness is false: tier UNKNOWN is NOT silently made Normal.
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
// Transmit a verified display slot even when the game's efficacy label is
// hidden. Existing pack/unpack remain strict for confirmed classifications.
inline int32_t pack_observation(Sample s) {
  if(s.tier!=Tier::UNKNOWN)return pack(s);
  const auto slot=static_cast<unsigned>(s.slot);
  if(slot<1||slot>2||!s.batch_token||s.batch_token>0x1FFFFFu)return 0;
  return static_cast<int32_t>((s.batch_token<<4)|slot);
}
inline Sample unpack_observation(int32_t aux) {
  if(aux<=0)return {};
  const uint32_t v=static_cast<uint32_t>(aux);
  Sample s{static_cast<Slot>(v&3),static_cast<Tier>((v>>2)&3),v>>4};
  return pack_observation(s)==aux?s:Sample{};
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

// Optional field enumeration is used only if the historical field names no
// longer resolve. Accept an exact declaring class, instance field, Int32 enum
// and a unique semantic enum-literal signature. Ambiguity fails closed.
using FieldIterator = const void *(*)(const void *, void **);

inline bool resolve(const PgrBootBinding &boot,const void *display_class,Binding &out,
                    FieldIterator enumerate=nullptr) {
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
  auto instance=[&](const void *f)->bool{
    return f&&parent(f)==display_class&&!(flags(f)&0x50);
  };
  const void *show=find(display_class,"showEffectiveness");
  if(!instance(show)||!type_named(m,field_type(show),"System.Boolean"))return false;
  auto literal=[&](const void *cls,const char *name,int32_t &v)->bool{
    const void *f=find(cls,name);
    if(!f||parent(f)!=cls||(static_cast<uint32_t>(flags(f))&0x8050u)!=0x8050u)return false;
    static_get(f,&v);
    return true;
  };
  auto enum_class=[&](const void *f)->const void *{
    if(!instance(f))return nullptr;
    const void *type=field_type(f);
    const void *cls=type?from_type(type):nullptr;
    return cls&&is_enum(cls)&&type_named(m,enum_base(cls),"System.Int32")?cls:nullptr;
  };
  auto slot_match=[&](const void *f,Binding &v)->bool{
    const void *cls=enum_class(f);
    if(!cls||!literal(cls,"Charge",v.slot_first)||!literal(cls,"Charge2",v.slot_second)||
       v.slot_first==v.slot_second)return false;
    v.slot_field=f;return true;
  };
  auto tier_match=[&](const void *f,Binding &v)->bool{
    const void *cls=enum_class(f);
    if(!cls||!literal(cls,"Normal",v.tier_neutral)||
       !literal(cls,"SuperEffective",v.tier_super)||
       !literal(cls,"NotVeryEffective",v.tier_resisted)||
       v.tier_neutral==v.tier_super||v.tier_neutral==v.tier_resisted||
       v.tier_super==v.tier_resisted)return false;
    v.tier_field=f;return true;
  };
  Binding candidate{};
  // Preserve the previously verified 0.437.0 binding when still available.
  if(!slot_match(find(display_class,"ervu"),candidate)||
     !tier_match(find(display_class,"erwh"),candidate)||
     candidate.slot_field==candidate.tier_field) {
    if(!enumerate)return false;
    candidate={};
    void *iter=nullptr;
    unsigned count=0;
    while(const void *f=enumerate(display_class,&iter)) {
      if(++count>256)return false;
      Binding slot{},tier{};
      const bool is_slot=slot_match(f,slot),is_tier=tier_match(f,tier);
      if(is_slot&&is_tier)return false;
      if(is_slot){
        if(candidate.slot_field)return false;
        candidate.slot_field=f;candidate.slot_first=slot.slot_first;candidate.slot_second=slot.slot_second;
      }
      if(is_tier){
        if(candidate.tier_field)return false;
        candidate.tier_field=f;candidate.tier_neutral=tier.tier_neutral;
        candidate.tier_super=tier.tier_super;candidate.tier_resisted=tier.tier_resisted;
      }
    }
    if(!candidate.slot_field||!candidate.tier_field||
       candidate.slot_field==candidate.tier_field)return false;
  }
  candidate.show_field=show;candidate.valid=true;out=candidate;
  return true;
}

inline Sample observe(const Binding &binding,void *display,void *state,
                     void (*field_get_value)(void *,void *,void *)) {
  if(!binding.valid||!display||!state||!field_get_value)return {};
  int32_t slot=0;
  field_get_value(display,const_cast<void *>(binding.slot_field),&slot);
  Sample s{};
  s.slot=slot==binding.slot_first?Slot::FIRST:
         slot==binding.slot_second?Slot::SECOND:Slot::UNKNOWN;
  if(s.slot==Slot::UNKNOWN)return {};
  s.batch_token=state_batch_token(state);
  uint8_t show=0;
  field_get_value(display,const_cast<void *>(binding.show_field),&show);
  // The game may hide the text for a neutral attack. We preserve the verified
  // slot identity, but do NOT infer Normal when the label is not shown.
  if(show!=1)return s;
  int32_t effect=0;
  field_get_value(display,const_cast<void *>(binding.tier_field),&effect);
  s.tier=effect==binding.tier_super?Tier::SUPER:
         effect==binding.tier_neutral?Tier::NEUTRAL:
         effect==binding.tier_resisted?Tier::RESISTED:Tier::UNKNOWN;
  return s;
}
}  // namespace pgr_charge_effect_ui
