// Host-only policy validation: options, malformed inputs, snapshot freshness and tearing.
#include "battle_timing_policy.h"
#include <cassert>
#include <cstring>
#include <cstdio>

namespace {
PgrRuntimeTransportSample snap{};
const char *encoded="F2|1|1|1";
bool change_sequence=false,change_generation=false;
int sample(PgrRuntimeTransportSample *out) {
  *out=snap;
  if(change_sequence) ++snap.snapshot_seq;
  if(change_generation) ++snap.generation;
  return 0;
}
int get_string(uint32_t id,char *dest,uint32_t cap,uint32_t *size) {
  if(id!=21)return -1;
  const auto n=static_cast<uint32_t>(std::strlen(encoded));
  *size=n;if(n>cap)return -1;
  std::memcpy(dest,encoded,n);
  return 0;
}
}
int main() {
  using namespace pgr_battle_timing;
  const uint64_t expected_shield[]={0,1500,3000,1500};
  const uint64_t expected_swap[]={400,2000,6000,2000};
  for(uint32_t i=0;i<4;++i) {
    Preferences p;
    char raw[]="F2|1|0|0";raw[5]=static_cast<char>('0'+i);raw[7]=static_cast<char>('0'+i);
    assert(parse(raw,8,p) && p.smart_battle && p.shield_choice==i && p.fainted_choice==i);
    assert(shield_wait_ms(i)==expected_shield[i]);
    assert(fainted_wait_ms(i)==expected_swap[i]);
  }
  Preferences p;
  for(int battle=0;battle<2;++battle)for(int attack=0;attack<2;++attack)
    for(int shield=0;shield<4;++shield)for(int swap=0;swap<4;++swap){
      char raw[]="F3|0|0|0|0";
      raw[3]=char('0'+battle);raw[5]=char('0'+shield);raw[7]=char('0'+swap);raw[9]=char('0'+attack);
      assert(parse(raw,10,p) && p.smart_battle==bool(battle) && p.smart_attack==bool(attack));
      assert(p.shield_choice==unsigned(shield) && p.fainted_choice==unsigned(swap));
      assert(shield_wait_ms(p.shield_choice)==expected_shield[shield]);
      assert(fainted_wait_ms(p.fainted_choice)==expected_swap[swap]);
    }
  assert(parse("F3|1|2|0|1",10,p) && p.smart_battle && p.smart_attack && p.shield_choice==2 && p.fainted_choice==0);
  assert(parse("F3|0|2|0|1",10,p) && !p.smart_battle && p.smart_attack);
  assert(parse("F3|1|2|0|0",10,p) && p.smart_battle && !p.smart_attack);
  assert(parse("F3|0|1|1|1",10,p) && p.smart_attack);
  assert(parse("F2|0|1|1",8,p) && !p.smart_battle && !p.smart_attack); // never retain F3 on legacy
  assert(!parse("F3|1|1|1",8,p));
  assert(!parse("F3|1|1|1|2",10,p));
  assert(!parse("F3|1|1|1!1",10,p));
  assert(!parse("F2|1|1|1|1",10,p));
  assert(!parse("F1|1",4,p));
  assert(!parse("F2|1|4|1",8,p));
  assert(!parse("F2|1|1|4",8,p));
  assert(!parse("F2|x|1|1",8,p));
  assert(!parse("F2|1|1|1!",9,p));
  assert(!parse(nullptr,8,p));
  PgrRuntimeTransportApi transport{};
  transport.sample=sample;transport.get_string=get_string;
  snap.snapshot_fresh=1;snap.snapshot_seq=42;snap.generation=3;
  assert(read(&transport,p) && p.shield_choice==1 && p.fainted_choice==1);
  snap.snapshot_fresh=0;assert(!read(&transport,p));snap.snapshot_fresh=1;
  snap.fault=1;assert(!read(&transport,p));snap.fault=0;
  change_sequence=true;assert(!read(&transport,p));change_sequence=false;
  encoded="F2|1|3|0";assert(read(&transport,p) && !p.smart_attack && p.shield_choice==3 && p.fainted_choice==0 && shield_wait_ms(p.shield_choice)==1500);
  encoded="F3|0|2|0|1";assert(read(&transport,p) && !p.smart_battle && p.smart_attack && p.shield_choice==2 && p.fainted_choice==0);
  snap.snapshot_fresh=0;assert(!read(&transport,p));snap.snapshot_fresh=1;
  change_sequence=true;assert(!read(&transport,p));change_sequence=false;
  change_generation=true;assert(!read(&transport,p));change_generation=false;
  std::puts("PASS: battle settings, smart-attack transport and fail-closed snapshots");
}
