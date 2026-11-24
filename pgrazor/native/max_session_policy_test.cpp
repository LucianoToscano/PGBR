#include "max_session_policy.h"
#include <cassert>
int main(){
 PgrMaxSessionPolicy p;assert(!p.arm(1,100,true));assert(!p.arm(512,100,true));assert(!p.arm(16,100,false));
 assert(p.arm(32|64,100,true));assert(p.state()==1);assert(!p.arm(16,101,true));
 assert(p.effective(1|16,false,-1,0)==17);
 p.sync(200,true,true,2,4,false,false);assert(p.state()==2);assert(p.effective(1|16,true,2,4)==97);
 assert(p.effective(17,true,0,4)==17);assert(p.effective(17,true,2,5)==17);
 auto generation=p.generation;p.sync(201,true,true,2,5,false,false);assert(p.state()==0&&p.generation!=generation);
 assert(p.arm(16,300,true));p.sync(301,true,true,0,6,false,false);assert(p.state()==0);
 assert(p.arm(16,400,true));p.sync(401,true,true,1,7,false,false);assert(!p.state());
 assert(p.arm(16,500,true));p.sync(501,true,true,2,8,true,false);assert(!p.state());
 assert(p.arm(16,600,true));p.sync(601,true,true,2,9,false,true);assert(!p.state());
 assert(p.arm(16,700,true));p.sync(701,false,true,2,10,false,false);assert(!p.state());
 assert(p.arm(16,800,true));p.sync(801,true,true,2,11,false,false);p.sync(802,true,false,-1,11,false,false);assert(!p.state());
 assert(p.arm(16,900,true));p.sync(600900,true,false,-1,0,false,false);assert(!p.state());
}
