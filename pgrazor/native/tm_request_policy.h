#pragma once
#include <cstdint>
struct PgrTmRequest {uint64_t token=0,at=0;int row=-1;};
inline bool pgr_tm_request_valid(const PgrTmRequest &r,uint64_t token,uint64_t sampled,
    uint64_t old_session,uint64_t current_session,uint64_t now,bool same,bool visible,int stock){
  return token&&r.token==token&&r.row>=0&&r.row<3&&same&&visible&&stock>0&&old_session==current_session&&
      now>=sampled&&now-sampled<=500&&now>=r.at&&now-r.at<=500;
}
