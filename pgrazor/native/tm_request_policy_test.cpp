#include "tm_request_policy.h"
#include <cassert>
int main(){
 PgrTmRequest r{7,110,1};
 assert(pgr_tm_request_valid(r,7,100,2,2,120,true,true,1));
 assert(!pgr_tm_request_valid(r,8,100,2,2,120,true,true,1));
 assert(!pgr_tm_request_valid(r,7,100,2,3,120,true,true,1));
 assert(!pgr_tm_request_valid(r,7,100,2,2,120,false,true,1));
 assert(!pgr_tm_request_valid(r,7,100,2,2,120,true,false,1));
 assert(!pgr_tm_request_valid(r,7,100,2,2,120,true,true,0));
 assert(!pgr_tm_request_valid(r,7,100,2,2,601,true,true,1));
 assert(!pgr_tm_request_valid(r,7,100,2,2,109,true,true,1));
 r.row=3;assert(!pgr_tm_request_valid(r,7,100,2,2,120,true,true,1));
}
