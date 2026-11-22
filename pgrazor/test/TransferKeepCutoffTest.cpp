#include "transfer_policy.h"
#include <cassert>
#include <algorithm>
#include <cstdio>

int main() {
    const PgrTransferApi *api=static_cast<const PgrTransferApi*>(pgrazor_transfer_policy_api(1));
    assert(api && api->decide);
    PgrTransferInput v{1,sizeof(PgrTransferInput),0,0,0,25,0,255,0,0,0,0,nullptr};
    // No encoded setting is a backwards-compatible 90% cutoff.
    v.attack=14;v.defense=13;v.stamina=13;
    assert(api->decide(&v)==0);
    v.stamina=14;
    assert(api->decide(&v)==9);
    // All values 0..100 work, plus explicit-only legacy 101.
    for(unsigned cutoff=0;cutoff<=101;cutoff++) {
        v.reserved=cutoff+1;
        for(int sum=0;sum<=45;sum++) {
            v.attack=std::min(15,sum);
            v.defense=std::min(15,sum-v.attack);
            v.stamina=sum-v.attack-v.defense;
            const bool kept=cutoff<=100 && sum*100>=int(cutoff)*45;
            assert(api->decide(&v)==(kept?9:0));
        }
    }
    v.reserved=103;assert(api->decide(&v)==17); // Unknown config: fail closed.
    v.reserved=1;v.attack=0;v.defense=0;v.stamina=0;
    v.known=0;assert(api->decide(&v)==6); // Unknown facts still kept.
    v.known=255;v.flags=192;assert(api->decide(&v)==8); // Favorite/egg still kept.
    std::puts("PASS PGBR native IV cutoff 0..100, legacy protected-only, invalid=keep");
    return 0;
}
