#pragma once
#include <cstdint>
// Single explicitly armed Max encounter. Never affects saved battle masks.
struct PgrMaxSessionPolicy {
 static constexpr uint32_t MASK=(1u<<4)|(1u<<5)|(1u<<6)|(1u<<9);
 uint32_t requested=0;uint64_t serial=0,generation=0,expires=0;
 bool arm(uint32_t mask,uint64_t now,bool lease){
  if(requested||!now||!lease||(mask&~MASK)||!(mask&112u))return false;
  requested=mask;serial=0;expires=now+600000;++generation;return true;
 }
 void stop(){if(requested){requested=0;serial=0;expires=0;++generation;}}
 void sync(uint64_t now,bool lease,bool scope,int mode,uint64_t scope_serial,bool fault,bool rocket){
  if(!requested)return;
  if(!now||now>=expires||!lease||fault||rocket){stop();return;}
  if(serial){if(!scope||mode!=2||serial!=scope_serial)stop();return;}
  if(!scope)return;
  if(mode!=2||!scope_serial){stop();return;}
  serial=scope_serial;expires=now+1800000;++generation;
 }
 bool bound(bool scope,int mode,uint64_t scope_serial)const{return requested&&serial&&scope&&mode==2&&serial==scope_serial;}
 uint32_t effective(uint32_t saved,bool scope,int mode,uint64_t scope_serial)const{
  return bound(scope,mode,scope_serial)?((saved&~MASK)|requested):saved;
 }
 int state()const{return !requested?0:serial?2:1;}
};
