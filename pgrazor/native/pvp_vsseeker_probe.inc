// Optional, observe-only VsSeekerCombatState lifecycle from PGRazor-Savant.
// Included by combat_host.cpp after install_hook_group() is declared.
// Does not enter a Rocket scope or change action masks, settings or ABI.
void hook_vsseeker_enter(void *self,void *api,void *data,void *method) {
  auto original=reinterpret_cast<Fn4Ptr>(load_original(HK_PVP_VSSEEKER_ENTER));
  FrameGuard frame;uint64_t tid=0;
  if(!pvp_gate(&tid)){original(self,api,data,method);return;}
  PinnedArgs pins{self,api,data};
  if(!pins.valid){original(self,api,data,method);return;}
  // Keep the game's state transition authoritative; witness only afterwards.
  original(pins.get(0),pins.get(1),pins.get(2),method);
  if(!pvp_gate(&tid)||!pve_exclusion_clear(&tid))return;
  if(g_pvp_scene.verified_vs_seeker(now_ms()))
    queue_event(PGR_CBH_EV_BATTLE_ACTION,0,0,tid,
      R"({"pvpObserved":true,"source":"VsSeekerCombatState.EnterState","executorReady":false})");
}

bool install_optional_vsseeker_probe() {
  const auto *meta=api_table<PgrMetaApi>(pgrazor_runtime_metadata_api(1));
  const auto *boot=api_table<PgrBootApi>(pgrazor_runtime_bootstrap_api(1));
  PgrBootBinding binding{};
  if(!meta||!meta->resolve_class||!meta->validate_method||!meta->validate_code||
     !boot||!boot->snapshot||boot->snapshot(&binding)!=PGR_BOOT_OK||
     !binding.meta.debug_get_method_info)return false;
  PgrMetaClassQuery q{};q.version=1;q.size=sizeof(q);
  q.name_space="Niantic.Holoholo.Combat.Contexts";
  q.name="VsSeekerCombatState";q.canonical_match=PGR_MD_MATCH_EXACT;
  PgrMetaClassResult klass{};
  if(meta->resolve_class(&q,&klass)!=PGR_MD_OK||!klass.klass)return false;
  PgrMetaMethodQuery m{};m.version=1;m.size=sizeof(m);
  m.owner=klass.klass;m.name="EnterState";m.return_type="System.Void";
  m.return_match=PGR_MD_MATCH_EXACT;m.param_count=2;
  m.param_types[0]="Niantic.Platform.GameStates.StateLogic.IStateApi";
  m.param_types[1]="Niantic.Holoholo.Combat.Contexts.VsSeekerCombatState.Data";
  m.param_match[0]=m.param_match[1]=PGR_MD_MATCH_EXACT;
  PgrMetaMethodResult found{};
  if(meta->validate_method(&m,&found)!=PGR_MD_OK||!found.method)return false;
  void *info[3]{};
  auto debug=reinterpret_cast<bool(*)(const void*,void*)>(binding.meta.debug_get_method_info);
  if(!debug(found.method,info)||!info[0])return false;
  PgrMetaCodeQuery code{};code.version=1;code.size=sizeof(code);
  code.address=info[0];code.length=16;
  if(meta->validate_code(&code)!=PGR_MD_OK)return false;
  // Avoid inline patching a short return or unconditional tail branch.
  for(unsigned i=0;i<4;++i) {
    uint32_t insn=0;
    std::memcpy(&insn,static_cast<char*>(info[0])+i*4,4);
    if((insn&0xfffffc1fu)==0xd65f0000u ||
       (insn&0xfc000000u)==0x14000000u)return false;
  }
  const HookEntry entry{HK_PVP_VSSEEKER_ENTER,info[0],
                        reinterpret_cast<void*>(hook_vsseeker_enter)};
  return install_hook_group(&entry,1);
}
