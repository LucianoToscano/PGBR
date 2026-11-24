// Read-only tag/family adapter. Reflection helpers follow the already-bound batch evolution path.
#include "game_bindings.h"
#include "tag_catch.h"
#include "tag_catch_policy.h"
#include "smart_catch.h"
#include "shared_metadata.h"
#include "runtime_bootstrap.h"
#include "runtime_transport.h"
#include <android/log.h>
#include <dlfcn.h>
#include <unistd.h>
#include <time.h>
#include <cstring>
#include <map>
#include <set>
#include <string>
namespace {
using namespace pgr_shared_meta;
struct Failure {const char *where;};
void require(bool ok,const char *where){if(!ok)throw Failure{where};}
PgrBootBinding boot{};const PgrSharedMetadataOutput *md=nullptr;
bool initialized=false,busy=false;uint64_t owner=0;
const char *managed_context="tag read failed";
uint64_t clock_ms(){timespec t{};clock_gettime(CLOCK_BOOTTIME,&t);return uint64_t(t.tv_sec)*1000+t.tv_nsec/1000000;}
template<class F>F fn(void *p){require(p,"missing runtime export");return reinterpret_cast<F>(p);}
void *exp(const char *name){std::string n="il2cpp_";n+=name;void *p=dlsym(boot.handle,n.c_str());require(p,name);return p;}
struct Root {
 uintptr_t h=0;
 Root()=default;explicit Root(void *v){if(v){h=fn<uintptr_t(*)(void*,int)>(boot.runtime.gchandle_new)(v,0);require(h,"GC root");}}
 Root(const Root&)=delete;Root& operator=(const Root&)=delete;
 Root(Root &&r) noexcept:h(r.h){r.h=0;}
 Root& operator=(Root &&r) noexcept {if(this!=&r){reset();h=r.h;r.h=0;}return *this;}
 ~Root(){reset();}void reset(){if(h){fn<void(*)(uintptr_t)>(boot.runtime.gchandle_free)(h);h=0;}}
 void *get()const{return h?fn<void*(*)(uintptr_t)>(boot.runtime.gchandle_get_target)(h):nullptr;}
 explicit operator bool()const{return get()!=nullptr;}
};
void *klass(void *o){require(o,"null object");return fn<void*(*)(void*)>(boot.runtime.object_get_class)(o);}
void *cls(const char *ns,const char *name){
 static std::map<std::string,void*> cache;std::string key=std::string(ns)+"."+name;auto it=cache.find(key);if(it!=cache.end())return it->second;
 const auto *api=static_cast<const PgrMetaApi*>(pgrazor_runtime_metadata_api(1));require(api,"metadata API");
 PgrMetaClassQuery q{};q.version=1;q.size=sizeof(q);q.name_space=ns;q.name=name;q.canonical_match=PGR_MD_MATCH_EXACT;
 PgrMetaClassResult out{};require(api->resolve_class(&q,&out)==0,name);return cache[key]=const_cast<void*>(out.klass);
}
std::string typename_of(void *type){char *p=fn<char*(*)(void*)>(boot.meta.type_get_name)(type);require(p,"type name");std::string s=p;fn<void(*)(void*)>(boot.meta.mem_free)(p);for(size_t pos=0;(pos=s.find("`1<",pos))!=std::string::npos;)s.erase(pos,2);return s;}
void *field_info(void *k,const char *name,const char *type){
 void *f=fn<void*(*)(void*,const char*)>(boot.meta.class_get_field_from_name)(k,name);require(f,name);
 auto ft=fn<void*(*)(void*)>(boot.meta.field_get_type)(f);require(typename_of(ft)==type,name);
 require(!(fn<int(*)(void*)>(boot.meta.field_get_flags)(f)&0x10),"instance field required");return f;
}
template<class T>T field(void *o,const char *name,const char *type){T value{};auto f=field_info(klass(o),name,type);fn<void(*)(void*,void*,void*)>(boot.runtime.field_get_value)(o,f,&value);return value;}
void *method(void *k,const char *name,int count){void *m=fn<void*(*)(void*,const char*,int)>(boot.meta.class_get_method_from_name)(k,name,count);require(m,name);require(fn<unsigned(*)(void*)>(boot.meta.method_get_param_count)(m)==unsigned(count),name);return m;}
void *invoke(void *m,void *o,void **args=nullptr){void *ex=nullptr;void *v=fn<void*(*)(void*,void*,void**,void**)>(boot.runtime.runtime_invoke)(m,o,args,&ex);if(ex){char detail[512]{};auto format=reinterpret_cast<void(*)(void*,char*,int)>(dlsym(boot.handle,"il2cpp_format_exception"));if(format)format(ex,detail,sizeof(detail));__android_log_print(ANDROID_LOG_ERROR,"PGRazorTagCatch","%s: %s",managed_context,detail);throw Failure{managed_context};}return v;}
void *call(void *o,const char *name,int n=0,void **args=nullptr){return invoke(method(klass(o),name,n),o,args);}
template<class T>T boxed(void *o,const char *type){require(o&&typename_of(fn<void*(*)(void*)>(boot.meta.class_get_type)(klass(o)))==type,type);T v{};auto p=fn<void*(*)(void*)>(boot.runtime.object_unbox)(o);require(p,"boxed value");memcpy(&v,p,sizeof(v));return v;}
std::string string(void *o){
 if(!o)return {};require(klass(o)==cls("System","String"),"string identity");int n=fn<int(*)(void*)>(exp("string_length"))(o);require(n>=0&&n<=512,"string bound");auto p=fn<const uint16_t*(*)(void*)>(boot.runtime.string_chars)(o);std::string s;
 for(int i=0;i<n;i++){uint32_t c=p[i];if(c>=0xd800&&c<=0xdbff){require(i+1<n&&p[i+1]>=0xdc00&&p[i+1]<=0xdfff,"invalid UTF16");c=0x10000+((c-0xd800)<<10)+(p[++i]-0xdc00);}else require(c<0xdc00||c>0xdfff,"invalid UTF16");
 if(c<128)s+=char(c);else if(c<2048){s+=char(0xc0|(c>>6));s+=char(0x80|(c&63));}else if(c<65536){s+=char(0xe0|(c>>12));s+=char(0x80|((c>>6)&63));s+=char(0x80|(c&63));}else{s+=char(0xf0|(c>>18));s+=char(0x80|((c>>12)&63));s+=char(0x80|((c>>6)&63));s+=char(0x80|(c&63));}}
 return s;
}
void *h(unsigned id){return const_cast<void*>(md->opt_handles[id]);}
Root type_object(void *k){return Root(fn<void*(*)(void*)>(exp("type_get_object"))(fn<void*(*)(void*)>(boot.meta.class_get_type)(k)));}
Root resolve(void *contract){
 Root type=type_object(h(H_IL_MAP));void *args[]={type.get()};Root map(invoke(h(H_IL_FIND_M),nullptr,args));if(!map)return {};
 Root container(invoke(h(H_IL_CONTAINER_M),map.get()));if(!container)return {};
 Root target=type_object(contract);void *resolve_args[]={target.get()};return Root(invoke(h(H_IL_RESOLVE_M),container.get(),resolve_args));
}
Root resolve(const char *ns,const char *name){return resolve(cls(ns,name));}
int count(Root &list,int cap){int n=boxed<int>(call(list.get(),"get_Count"),"System.Int32");require(n>=0&&n<=cap,"collection bound");return n;}
Root item(Root &list,int index){void *args[]={&index};return Root(call(list.get(),"get_Item",1,args));}
Root reference(void *o,const char *name,const char *prefix){
 auto f=fn<void*(*)(void*,const char*)>(boot.meta.class_get_field_from_name)(klass(o),name);require(f,name);
 void *ft=fn<void*(*)(void*)>(boot.meta.field_get_type)(f);void *k=fn<void*(*)(void*)>(boot.meta.class_from_type)(ft);
 require(k&&!fn<bool(*)(void*)>(boot.meta.class_is_valuetype)(k)&&typename_of(ft).find(prefix)==0,name);
 void *v=nullptr;fn<void(*)(void*,void*,void*)>(boot.runtime.field_get_value)(o,f,&v);return Root(v);
}

struct Config {bool valid=false;uint64_t generation=0;std::string c,d;};
Config config(){
 Config p;auto t=static_cast<const PgrRuntimeTransportApi*>(pgrazor_runtime_transport_api(1));PgrRuntimeTransportSample a{},z{};int allowed=0;char buf[PGR_SMART_CONFIG_MAX+1];uint32_t n=0;
 if(!t||t->sample(&a)||!a.snapshot_fresh||a.fault||t->get_bool(36,&allowed)||!allowed||t->get_string(7,buf,sizeof(buf),&n)||n>PGR_SMART_CONFIG_MAX||t->sample(&z)||!z.snapshot_fresh||z.fault||z.generation!=a.generation||z.snapshot_seq!=a.snapshot_seq)return p;
 PgrSmartSettings s{};pgr_smart_parse_settings(buf,n,&s);
 if(!s.enabled||(!s.catch_tags[0]&&!s.candy_tags[0]))return p;
 p.valid=true;p.generation=a.generation;p.c=s.catch_tags;p.d=s.candy_tags;return p;
}
struct Scan {
 Config cfg;std::string account;Root tags,gm,rows;int tag_count=0,tag_index=0,row_count=0,row_index=0;
 std::set<std::string> catch_names,candy_names,found;
 std::set<int> catch_families,candy_families;bool current_c=false,current_d=false,ready=false;
 uint64_t expiry=0,started=0;
};
Scan scan;uint64_t retry_at=0;
bool same(const Config&a,const Config&b){return a.valid&&b.valid&&a.generation==b.generation&&a.c==b.c&&a.d==b.d;}
std::string account(){Root player=resolve("Niantic.Holoholo","IPlayerService");if(!player)return {};Root id(call(player.get(),"get_PlayerId"));return string(id.get());}
int family(void *pokemon,Root &gm){
 require(pokemon&&klass(pokemon)==cls("Holoholo.Rpc","PokemonProto")&&gm,"Pokemon family identity");
 Root rooted(pokemon);void *args[]={gm.get()};Root result(call(rooted.get(),"GetFamilyId",1,args));
 require(result&&fn<bool(*)(void*)>(boot.meta.class_is_enum)(klass(result.get())),"family enum");
 void *base=fn<void*(*)(void*)>(exp("class_enum_basetype"))(klass(result.get()));require(base&&typename_of(base)=="System.Int32","family enum width");
 auto p=fn<void*(*)(void*)>(boot.runtime.object_unbox)(result.get());require(p,"family value");int v=0;memcpy(&v,p,sizeof(v));require(v>0,"unknown family");return v;
}
void begin(const Config &p){
 scan=Scan{};scan.cfg=p;scan.started=clock_ms();scan.account=account();require(!scan.account.empty(),"account not ready");
 require(pgr_tags::decode(p.c,scan.catch_names)&&pgr_tags::decode(p.d,scan.candy_names),"tag syntax");
 Root bag=resolve("Niantic.Holoholo","IPokemonBag");require(bool(bag),"Pokemon bag not ready");scan.gm=reference(bag.get(),PGR_GAME_BATCH_EVOLUTION_CPP_ERIK_NAME,"IGameMasterData");require(bool(scan.gm),"game master not ready");
 scan.tags=resolve("Niantic.Holoholo.Tagging","ITaggingRpcService");require(bool(scan.tags),"tag service unavailable");
 Root ready(call(scan.tags.get(),"WaitForTagCacheInitialized"));require(ready&&field<bool>(ready.get(),"completeCalled","System.Boolean")&&!field<bool>(ready.get(),"errorCalled","System.Boolean"),"tag cache not ready");
 scan.tag_count=boxed<int>(call(scan.tags.get(),"get_NumberOfTags"),"System.Int32");require(scan.tag_count>=0&&scan.tag_count<=1000,"tag count");
}
void step(){
 if(scan.rows&&scan.row_index<scan.row_count){
  Root member=item(scan.rows,scan.row_index++);int f=family(member.get(),scan.gm);if(scan.current_c)scan.catch_families.insert(f);if(scan.current_d)scan.candy_families.insert(f);return;
 }
 if(scan.rows){require(count(scan.rows,25000)==scan.row_count,"tag changed during read");scan.rows.reset();}
 if(scan.tag_index<scan.tag_count){
  int i=scan.tag_index++;void *args[]={&i};Root row(call(scan.tags.get(),"GetCachedTagToPokemonBindingForIndex",1,args));require(bool(row),"tag row unavailable");
  Root tag(field<void*>(row.get(),"TagProto","Holoholo.Rpc.PokemonTagProto"));require(bool(tag),"tag identity");Root name_value(field<void*>(tag.get(),"Name","System.String"));std::string name=string(name_value.get());
  scan.current_c=scan.catch_names.count(name);scan.current_d=scan.candy_names.count(name);if(!scan.current_c&&!scan.current_d)return;
  require(scan.found.insert(name).second,"ambiguous tag name");scan.rows=reference(row.get(),"PokemonProtos","System.Collections.Generic.List");require(bool(scan.rows),"tag members unavailable");
  scan.row_count=count(scan.rows,25000);scan.row_index=0;return;
 }
 for(const auto& n:scan.catch_names)require(scan.found.count(n),"catch tag not found");
 for(const auto& n:scan.candy_names)require(scan.found.count(n),"candy tag not found");
 require(account()==scan.account&&boxed<int>(call(scan.tags.get(),"get_NumberOfTags"),"System.Int32")==scan.tag_count&&same(config(),scan.cfg),"tag snapshot changed");
 scan.ready=true;scan.expiry=clock_ms()+30000;scan.tags.reset();scan.rows.reset();
}
int decision(void *pokemon,uint64_t tid,const PgrSmartSettings &s){
 if(tid!=owner||tid!=uint64_t(gettid())||!scan.ready||clock_ms()>=scan.expiry||scan.cfg.c!=s.catch_tags||scan.cfg.d!=s.candy_tags)return -1;
 try{if(!same(config(),scan.cfg)||account()!=scan.account)return -1;int f=family(pokemon,scan.gm);if(!same(config(),scan.cfg)||account()!=scan.account)return -1;return pgr_tags::decide(true,!scan.catch_names.empty(),scan.catch_families,scan.candy_families,f);}catch(...){return -1;}
}
} // namespace
void pgr_tag_catch_tick(uint64_t tid,const PgrSharedMetadataOutput *metadata){
 if(busy||!tid||tid!=uint64_t(gettid())||(owner&&owner!=tid))return;
 struct Guard{Guard(){busy=true;}~Guard(){busy=false;}}guard;
 auto p=config();if(!p.valid){scan=Scan{};retry_at=0;return;}
 try{
  if(!initialized){auto api=static_cast<const PgrBootApi*>(pgrazor_runtime_bootstrap_api(1));boot.version=1;boot.size=sizeof(boot);require(api&&api->snapshot(&boot)==0&&metadata,"bootstrap");md=metadata;owner=tid;initialized=true;pgr_smart_set_tag_source(decision);}
  if(!same(p,scan.cfg)){scan=Scan{};retry_at=0;}
  uint64_t now=clock_ms();if(now<retry_at)return;
  if(scan.ready&&now<scan.expiry)return;
  if(!scan.started||scan.ready){begin(p);return;}
  require(now-scan.started<120000,"tag scan timeout");
  const auto start=clock_ms();for(int i=0;i<4&&!scan.ready&&clock_ms()-start<3;i++)step();
 }catch(const Failure &f){scan=Scan{};scan.cfg=p;retry_at=clock_ms()+5000;__android_log_print(ANDROID_LOG_WARN,"PGRazorTagCatch","tag snapshot unavailable: %s",f.where);}catch(...){scan=Scan{};scan.cfg=p;retry_at=clock_ms()+5000;}
}
void pgr_tag_catch_stop(uint64_t tid){if(initialized&&tid==owner&&tid==uint64_t(gettid())){scan=Scan{};retry_at=0;}}
