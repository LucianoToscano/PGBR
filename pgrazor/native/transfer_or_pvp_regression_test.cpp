// Offline regression: independent OR protections, even with 100% IV keep threshold.
#include "transfer_policy.h"
#include "pvp_rank.h"
#include <cstring>
#include <cstdio>
int main(){
 const auto *api=static_cast<const PgrTransferApi*>(pgrazor_transfer_policy_api(1));
 if(!api||!api->decide)return 1;
 PgrTransferInput v{1,sizeof(PgrTransferInput),0,15,15,183,0,255,0,0,0,101,nullptr};
 // Threshold 100% does not independently protect Marill 0/15/15.
 if(api->decide(&v)!=0)return 2;
 int marill=-1;
 for(int i=0;i<pgr_pvp::species_count();++i)if(!std::strcmp(pgr_pvp::species_key(i),"marill"))marill=i;
 if(marill<0)return 3;
 pgr_pvp::Settings s{};s.rank[0]=1;s.meta_limit=150;
 pgr_pvp::Keep why{};
 // It is rank 1 for Great League as Azumarill after evolving.
 if(!pgr_pvp::keep(marill,false,0,15,15,0,s,&why)||why.league!=0||why.rank!=1)return 4;
 // Match the encounter_host.cpp order: keep if policy protected OR native PvP rank qualifies.
 const bool shouldKeep = api->decide(&v)!=0 || pgr_pvp::keep(marill,false,0,15,15,0,s,&why);
 if(!shouldKeep)return 5;
 // Turning PvP protection off makes this specific low-IV catch eligible for transfer.
 s.rank[0]=0;
 if(api->decide(&v)!=0 || pgr_pvp::keep(marill,false,0,15,15,0,s,&why))return 6;
 // A separate user-selected keep-list always overrides the IV threshold too.
 int32_t listed=183;v.keep_ids=&listed;v.keep_count=1;
 if(api->decide(&v)!=4)return 7;
 // Legacy protected categories remain independent of the IV setting.
 v.keep_count=0;v.keep_ids=nullptr;v.protect=2;v.flags=2;
 if(api->decide(&v)==0)return 8;
 std::puts("PASS: 100% IV limit and Top 1 PvP/keep list/categories are OR protections");
 return 0;
}
