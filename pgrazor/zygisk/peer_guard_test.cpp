#include "peer_guard.h"
#include <cassert>
#include <cstring>
#include <cstdio>
using pgbr_root_peer::Kind;
int main(){
  const Kind states[]={Kind::missing,Kind::file,Kind::directory,Kind::unknown};
  unsigned checked=0;
  for(auto parent:states)for(auto peer:states)for(auto off:states){
    auto probe=[&](const char*path)->Kind{
      if(std::strcmp(path,"/data/adb/modules")==0)return parent;
      if(std::strcmp(path,"/data/adb/modules/pgsavant")==0)return peer;
      assert(std::strcmp(path,"/data/adb/modules/pgsavant/pgrazor/injection-off")==0);
      return off;
    };
    const bool expected=parent==Kind::directory &&
      (peer==Kind::missing || (peer==Kind::directory && off==Kind::file));
    assert(pgbr_root_peer::permits(probe)==expected);
    ++checked;
  }
  assert(checked==64);
  std::puts("PASS peer guard: 64 fail-closed path configurations");
}
