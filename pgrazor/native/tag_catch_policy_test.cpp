#include "tag_catch_policy.h"
#include <cassert>
int main(){
 std::set<std::string> names;
 assert(pgr_tags::decode("",names)&&names.empty());
 assert(pgr_tags::decode("646f636573,61c3a7c3a36f",names)&&names.count("doces")&&names.count("ação"));
 assert(!pgr_tags::decode("ff,",names));assert(!pgr_tags::decode("646f636573,646f636573",names));
 assert(!pgr_tags::decode("00",names));assert(!pgr_tags::decode("x1",names));
 assert(pgr_tags::decide(true,true,{1},{1,2},1)==3);
 assert(pgr_tags::decide(true,true,{1},{2},2)==2);
 assert(pgr_tags::decide(true,false,{},{2},2)==3);
 assert(pgr_tags::decide(false,false,{},{},1)==-1);
 assert(pgr_tags::decide(true,false,{},{},0)==-1);
}
