#include "shared_auto_battle_gate.h"
#include <cassert>
#include <cstdio>
int main(){
  using Gate=PgrSharedAutoBattleGate;
  for(uint32_t mask=1;mask<=7;++mask)
    assert(Gate::effective(mask,true,true,true,false)==mask);
  assert(Gate::effective(7,true,true,false,false)==0); // outside any native scope
  assert(Gate::effective(7,true,false,true,false)==0); // lease expired
  assert(Gate::effective(7,false,true,true,false)==0); // host disarmed
  assert(Gate::effective(7,true,true,true,true)==0); // native executor failed
  assert(Gate::effective(0,true,true,true,false)==0); // all controls unchecked
  assert(Gate::effective(8,true,true,true,false)==0); // strategy-only bit excluded
  assert(Gate::effective(15,true,true,true,false)==0); // invalid/mixed bits fail closed
  assert(Gate::effective(5,true,true,true,false)==5); // never force charged/shield
  std::puts("PgrSharedAutoBattleGateTest passed");
}
