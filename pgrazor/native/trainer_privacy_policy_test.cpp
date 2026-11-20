#include "trainer_privacy_policy.h"
#include <cassert>
#include <cstdint>
#include <iostream>
int main() {
    const uint16_t name[]={'T','r','e','i','n','a','d','o','r','4','2'};
    const uint16_t another[]={'T','r','e','i','n','a','d','o','r','4','3'};
    const uint16_t quoted[]={'X','T','r','e','i','n','a','d','o','r','4','2'};
    const uint16_t accents[]={'J',0x00E3,'o'};
    const uint16_t wrong[]={'J','a','o'};
    assert(pgrazor_trainer_exact_name(name,11,name,11));
    assert(!pgrazor_trainer_exact_name(name,11,another,11));
    assert(!pgrazor_trainer_exact_name(quoted,12,name,11));
    assert(!pgrazor_trainer_exact_name(name,11,nullptr,0));
    assert(!pgrazor_trainer_exact_name(name,0,name,11));
    assert(pgrazor_trainer_exact_name(accents,3,accents,3));
    assert(!pgrazor_trainer_exact_name(accents,3,wrong,3));
    std::cout << "PASS: only exact authenticated trainer text can be masked\n";
    return 0;
}
