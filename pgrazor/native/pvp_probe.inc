// Include-only helper for combat_host.cpp, inside its anonymous namespace.
// Observe-only PvP probe: NO action dispatch, NO Rocket scope spoofing.
struct PvpScopeProbe {
  const void *combat_type_field=nullptr;
  const void *is_pvp_method=nullptr;
  bool ready=false;
  bool seen=false;
};
PvpScopeProbe g_pvp_probe{};

// Validate the exact 0.437.0 CombatType field and IsPvPCombat method before use.
// An incompatible metadata build leaves the whole probe disabled.
bool configure_pvp_observation() {
  const auto *api=api_table<PgrMetaApi>(pgrazor_runtime_metadata_api(1));
  const auto *boot=api_table<PgrBootApi>(pgrazor_runtime_bootstrap_api(1));
  PgrBootBinding b{};
  if(!api||!api->resolve_class||!api->validate_field||!api->validate_method||
     !boot||!boot->snapshot||boot->snapshot(&b)!=PGR_BOOT_OK||
     !b.meta.class_get_field_from_name||!b.meta.field_get_offset||
     !md_k(OC_RDIR)||!md_h(H_RK_DIRECTOR_M))return false;
  PgrMetaClassQuery q{};q.version=1;q.size=sizeof(q);
  q.name_space="Niantic.Holoholo.Combat";
  q.name="CombatTypeExtensions";
  q.canonical_match=PGR_MD_MATCH_EXACT;
  PgrMetaClassResult klass{};
  if(api->resolve_class(&q,&klass)!=0)return false;
  PgrMetaMethodQuery m{};m.version=1;m.size=sizeof(m);m.owner=klass.klass;
  m.name="IsPvPCombat";m.return_type="System.Boolean";
  m.return_match=PGR_MD_MATCH_EXACT;m.param_count=1;
  m.param_types[0]="Holoholo.Rpc.CombatType";
  m.param_match[0]=PGR_MD_MATCH_EXACT;
  m.is_static=1;
  PgrMetaMethodResult method{};
  if(api->validate_method(&m,&method)!=0||!method.method)return false;
  auto get_field=reinterpret_cast<void*(*)(const void*,const char*)>(b.meta.class_get_field_from_name);
  auto get_offset=reinterpret_cast<size_t(*)(const void*)>(b.meta.field_get_offset);
  void *field=get_field(md_k(OC_RDIR),"dohe"); // CombatDirectorV2 CombatType
  if(!field)return false;
  PgrMetaFieldQuery fq{};fq.version=1;fq.size=sizeof(fq);
  fq.owner=md_k(OC_RDIR);fq.name="dohe";fq.offset=get_offset(field);
  fq.forbidden_mask=0x50;fq.kind=PGR_MD_FIELD_ENUM_I32;
  fq.type_name="Holoholo.Rpc.CombatType";
  fq.type_match=PGR_MD_MATCH_EXACT;
  PgrMetaFieldResult result{};
  if(api->validate_field(&fq,&result)!=0||result.field!=field)return false;
  g_pvp_probe.combat_type_field=result.field;
  g_pvp_probe.is_pvp_method=method.method;
  g_pvp_probe.ready=true;
  return true;
}

void observe_pvp_legacy_ui(void *ui,uint64_t tid) {
  if(!g_pvp_probe.ready||!ui||!g_cfg.runtime_invoke||!g_cfg.field_get_value||
     !g_cfg.object_get_class||!g_cfg.object_unbox)return;
  auto invoke=reinterpret_cast<void*(*)(void*,void*,void**,void**)>(g_cfg.runtime_invoke);
  auto class_of=reinterpret_cast<void*(*)(void*)>(g_cfg.object_get_class);
  auto read_field=reinterpret_cast<void(*)(void*,void*,void*)>(g_cfg.field_get_value);
  auto unbox=reinterpret_cast<void*(*)(void*)>(g_cfg.object_unbox);
  void *exc=nullptr;
  void *director=invoke(md_h(H_RK_DIRECTOR_M),ui,nullptr,&exc);
  if(exc||!director||class_of(director)!=md_k(OC_RDIR))return;
  int32_t combat_type=0;
  read_field(director,const_cast<void*>(g_pvp_probe.combat_type_field),&combat_type);
  void *argument=&combat_type;
  void *boxed=invoke(const_cast<void*>(g_pvp_probe.is_pvp_method),nullptr,&argument,&exc);
  if(exc||!boxed||class_of(boxed)!=md_c(6))return;
  void *answer=unbox(boxed);
  if(!answer||*static_cast<const uint8_t*>(answer)!=1)return;
  // Type-positive live director is mandatory for private friend battles.
  // Never reinterpret a Rocket scope as PvP (even if a future build returns
  // the same CombatType category); never spoof a game mode or server data.
  pgr_rocket_combat_sample_v1 scope{};
  if(g_rocket_combat&&g_rocket_combat->sample(&scope)==PGR_RC_OK &&
     scope.scope_active&&!g_pvp_ui_root)return;
  g_pvp_scene.positive_pvp(now_ms());
  if(!g_pvp_probe.seen) {
    g_pvp_probe.seen=true;
    queue_event(PGR_CBH_EV_BATTLE_ACTION,1,0,tid,
      R"({"pvpObserved":true,"source":"CombatDirectorV2.IsPvPCombat","executorReady":false})");
  }
}
