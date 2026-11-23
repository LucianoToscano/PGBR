#include "max_session_gate.h"
#include <cassert>
#include <cstdio>

int main() {
  using Gate=PgrMaxSessionGate;
  auto run=[](uint32_t mask,bool consent,bool host,bool scope,int mode,
              bool failed,uint64_t serial,uint64_t authorized,
              uint64_t now,uint64_t expiry) {
    return Gate::effective(mask,consent,host,scope,mode,failed,
                           serial,authorized,now,expiry);
  };
  assert(run(15,true,true,true,2,false,5,5,1000,2000)==15);
  for(uint32_t mask=1;mask<=15;++mask)
    assert(run(mask,true,true,true,2,false,5,5,1000,2000)==mask);
  assert(run(15,false,true,true,2,false,5,5,1000,2000)==0);
  assert(run(15,true,false,true,2,false,5,5,1000,2000)==0);
  assert(run(15,true,true,false,2,false,5,5,1000,2000)==0);
  for(int mode : {-1,0,1,3,4,6})
    assert(run(15,true,true,true,mode,false,5,5,1000,2000)==0);
  assert(run(15,true,true,true,2,true,5,5,1000,2000)==0);
  assert(run(15,true,true,true,2,false,0,0,1000,2000)==0);
  assert(run(15,true,true,true,2,false,5,4,1000,2000)==0);
  assert(run(15,true,true,true,2,false,5,6,1000,2000)==0);
  assert(run(15,true,true,true,2,false,5,5,0,2000)==0);
  assert(run(15,true,true,true,2,false,5,5,1000,1000)==0);
  assert(run(15,true,true,true,2,false,5,5,1000,999)==0);
  assert(run(15,true,true,true,2,false,5,5,1000,3501)==0);
  assert(run(15,true,true,true,2,false,5,5,1000,3500)==15);
  assert(run(0,true,true,true,2,false,5,5,1000,2000)==0);
  assert(run(16,true,true,true,2,false,5,5,1000,2000)==0);
  assert(run(31,true,true,true,2,false,5,5,1000,2000)==0);
  std::puts("PgrMaxSessionGateTest passed");
}
