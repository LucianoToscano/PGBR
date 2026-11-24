#pragma once
#include <string>
#include <set>
#include <cstdint>
namespace pgr_tags {
constexpr size_t MAX_WIRE=528;
// Hex encodes UTF-8 names, avoiding separators in the native settings wire.
inline bool decode(const std::string &wire,std::set<std::string>& out){
 out.clear();if(wire.empty())return true;if(wire.size()>MAX_WIRE)return false;
 size_t at=0;while(at<wire.size()){
  size_t end=wire.find(',',at);if(end==std::string::npos)end=wire.size();
  if(end==at||(end-at)%2||end-at>64||out.size()>=8)return false;
  std::string name;for(size_t i=at;i<end;i+=2){
   auto digit=[](char c)->int{return c>='0'&&c<='9'?c-'0':c>='a'&&c<='f'?c-'a'+10:-1;};
   int a=digit(wire[i]),b=digit(wire[i+1]);if(a<0||b<0)return false;
   unsigned char ch=static_cast<unsigned char>(a*16+b);if(ch<32||ch==127)return false;name+=char(ch);
  }
  if(!out.insert(name).second)return false;
  if(end==wire.size())return true;at=end+1;
 }
 return false;
}
inline int decide(bool ready,bool restricted,const std::set<int>& catch_families,const std::set<int>& candy_families,int family){
 if(!ready||family<=0)return -1;
 return ((!restricted||catch_families.count(family))?1:0)|(candy_families.count(family)?2:0);
}
}
