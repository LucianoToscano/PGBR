// All site copy lives here so the components stay presentational and routine
// edits (wording, new settings, extra screenshots) never touch layout code.

export const links = {
  releasesLatest: 'https://github.com/LucianoToscano/PGBR/actions/workflows/pgbr-build-full-ptbr-candidate.yml',
  releasesCurrent:
    'https://github.com/LucianoToscano/PGBR/actions/workflows/pgbr-build-full-ptbr-candidate.yml',
  github: 'https://github.com/LucianoToscano/PGBR',
  upstream: 'https://github.com/jpitty03/PGRazor',
  licenses: 'https://github.com/LucianoToscano/PGBR/blob/main/THIRD_PARTY.md',
};

export const nav = [
  { href: '#features', label: 'Recursos' },
  { href: '?guide=all', label: 'Guias de recursos' },
  { href: '#cooldown', label: 'Tempo de espera' },
  { href: '#get-started', label: 'Primeiros passos' },
];

export const heroFacts = [
  { term: 'Compatível com', detail: 'Android ARM64 e MuMu' },
  { term: 'Opção Root', detail: 'PGBR Root para o jogo oficial (experimental)' },
  { term: 'Licença', detail: 'MIT (código de origem)' },
];

// The feature preview is a settings guide, not a live app. Each group maps
// to a real section of the in-app settings and lists actual control names.
export const settingGroups = [
  {
    id: 'catching',
    label: 'Capturas',
    intro: 'Disponível em Captura, Arremessos e Configurações gerais > Exibição de IVs.',
    items: [
      { name: 'Captura rápida', detail: 'Reduz animações de captura e ignora telas exibidas após capturar.' },
      { name: 'Giro rápido', detail: 'Gira Poképaradas abertas e fecha a tela de recompensas, quando o jogo permitir.' },
      { name: 'Qualidade e variedade do arremesso', detail: 'Escolha Bom, Ótimo ou Excelente, mantenha a bola curva ou alterne a qualidade dos arremessos.' },
      { name: 'Arremesso automático', detail: 'Desligado por padrão. Arremessa automaticamente usando suas opções de bola e fruta. Encontros de pesquisa e Passe GO ficam para captura manual.' },
      { name: 'Captura pelo mapa', detail: 'Com arremesso automático ativo, toque em um Pokémon selvagem, de Módulo Atrair ou Incenso para capturá-lo pelo mapa. Um marcador indica a captura atual; ações aguardam o VGP+ quando ocupado.' },
      { name: 'Seletor de frutas', detail: 'Defina até três frutas em ordem de prioridade na sobreposição de captura ou nas configurações.' },
      { name: 'Chance de captura e bola automática', detail: 'Consulte a chance estimada de cada bola e deixe a seleção automática escolher Poké Bola, Grande Bola ou Ultra Bola conforme seu objetivo. Nunca usa Master Ball.' },
      { name: 'Exibição de IVs', detail: 'Exibe nível, IVs e posições opcionais do PvP na captura, nos atributos e no armazenamento.' },
      { name: 'Transferir após capturar', detail: 'Transfere capturas novas abaixo de 90% IV, respeitando proteção para brilhantes, lendários, XXS, XXL, IV 0%, bons IVs de PvP e espécies selecionadas.' },
      { name: 'Evoluir antes de transferir', detail: 'Evolui capturas novas uma vez ou até a última etapa disponível antes da transferência, seguindo regras ou espécies selecionadas.' },
      { name: 'Botão Manter todos', detail: 'Botão flutuante que suspende transferências de encontros manuais sem alterar as regras do VGP/VGP+.' },
    ],
  },
  {
    id: 'explore',
    label: 'Exploração',
    intro: 'Disponível em Configurações gerais, no mapa e nos locais populares do menu rápido.',
    items: [
      { name: 'Joystick e toques', detail: 'Joystick que acompanha a câmera, com toque para caminhar e teletransporte por toque duplo.' },
      { name: 'Velocidades predefinidas', detail: 'Alterne entre 0, 9,3, 16 e 30 km/h.' },
      { name: 'Favoritos e caminhada automática', detail: 'Salve locais favoritos e crie rotas circulares. O padrão é até 50 Poképaradas e ginásios próximos, com limite ajustável.' },
      { name: 'Busca de locais e pontos populares', detail: 'Inclui busca por locais, ponto azul da localização e 14 destinos populares, com estimativa de espera antes de teletransportar.' },
      { name: 'Camadas do mapa', detail: 'Mostra Poképaradas, ginásios e Reides carregados pelo jogo, incluindo equipes, categorias e Equipe GO Rocket. Escolha caminhar ou teletransportar.' },
      { name: 'Aumento de aparições (80 m)', detail: 'Opção experimental para ampliar a distância de aparições próximas; não garante aumento no servidor e pode causar lentidão.' },
      { name: 'Estimativa de espera', detail: 'Cronômetro local com histórico; é uma estimativa, não uma garantia.' },
      { name: 'Horário local', detail: 'Relógio flutuante opcional para o horário da localização selecionada.' },
      { name: 'Feeds', detail: 'Aparições remotas do LivePokeMap (assinatura separada) ou PoGoCoordinates (login pelo Discord ou convidado), com filtros de IVs, nível e posição PvP.' },
    ],
  },
  {
    id: 'automate',
    label: 'Automação',
    intro: 'Disponível nas áreas do Go Plus Virtual, ovos, companheiro, presentes, Equipe GO Rocket, ginásios, Reides, Dinamax, itens, eventos e pesquisas. Inclui tarefas iniciadas manualmente, como Reides automáticas e caçador de Shundo.',
    items: [
      { name: 'Go Plus Virtual', detail: 'Captura Pokémon próximos com Poké Bolas e gira Poképaradas. Inicia desligado e para em segundo plano.' },
      { name: 'VGP+', detail: 'Inclui regras de bolas, frutas, arremessos, bônus e doces; espécies selecionadas; capturas de Incenso; alcance ampliado e giro opcional de ginásios.' },
      { name: 'Auto-Raid', detail: 'Encontra uma Reide compatível, desloca-se até ela, aguarda jogadores suficientes, batalha com ataques automáticos e tenta a captura bônus. Não usa passes remotos.' },
      { name: 'Caçador de Shundo (experimental)', detail: 'Teletransporta para Pokémon com IV 100%, verifica se são brilhantes e aguarda o tempo de espera. Teletransportes contínuos aumentam o risco de advertência ou banimento.' },
      { name: 'Incubar ovos automaticamente', detail: 'Usa somente a incubadora infinita, em ordem de menor ou maior distância.' },
      { name: 'Emoção do companheiro', detail: 'Inclui fotos, carinho e frutas conforme reserva, com coleta opcional de presentes.' },
      { name: 'Ocupar ginásios e alimentar defensores', detail: 'Coloca defensores marcados em vagas de ginásios da equipe e alimenta defensores, com controles independentes.' },
      { name: 'Sessão do Incenso de Aventura Diário', detail: 'Inicia uma caminhada de até 15 minutos. Ao detectar aves de Galar carregadas, para caminhada e VGP e abre o encontro para captura manual. Integração ainda não validada ao vivo.' },
      { name: 'Enviar e abrir presentes', detail: 'Executa um lote de presentes com contadores de enviados e abertos e botão Parar, respeitando limites do jogo.' },
      { name: 'Alimentação remota de ginásios', detail: 'Alimenta defensores em ginásios usando excedentes de Frutas Nanab, Frambo e Caxí, respeitando reservas e limites do jogo.' },
      { name: 'Evolução em massa', detail: 'Exibe uma prévia e evolui Pokémon elegíveis uma vez, sem animação, sem ativar Ovo da Sorte.' },
      { name: 'Presentes de amigos', detail: 'Abra e envie presentes, com adesivos opcionais.' },
      { name: 'Batalhas', detail: 'Auxilia contra Equipe GO Rocket, Reides, ginásios e Dinamax/Max; troca Pokémon, repete rodadas e coleta energia Max conforme o recurso.' },
      { name: 'Batalha inteligente', detail: 'Seleciona Pokémon com vantagens de tipo e troca para o melhor contra-ataque durante batalhas compatíveis.' },
      { name: 'Cura inteligente', detail: 'Cura Pokémon após batalhas ou periodicamente, escolhendo poções e reviver para reduzir desperdício.' },
      { name: 'Pesquisas', detail: 'Resgata pesquisas de campo concluídas, remove tarefas selecionadas e captura encontros pendentes.' },
      { name: 'Eventos', detail: 'O evento GO Wild de novembro de 2026 permite definir Bolas Safári para Pokémon Poderosos. O recurso de caixas do Halloween permanece disponível.' },
    ],
  },
  {
    id: 'personalize',
    label: 'Personalização',
    intro: 'Disponível nas telas do aplicativo e nas Configurações gerais.',
    items: [
      { name: 'Temas claro e escuro', detail: 'A interface segue o tema escolhido do PGBR.' },
      { name: 'Tamanho das sobreposições', detail: 'Ajuste botão, menu rápido, joystick, cronômetro e sobreposições para 100, 75, 50 ou 25%.' },
      { name: 'Reposicionar sobreposições', detail: 'Arraste o cronômetro, relógio, ícone VGP e botão Manter todos.' },
      { name: 'Menu rápido', detail: 'Acesse mapa, velocidade, rotas, favoritos, teletransporte, automação, pesquisas, eventos, contas e configurações.' },
      { name: 'Pular animações', detail: 'Ignora animações de evolução, ovos, espécies, fortalecimento, nível, medalhas e Equipe GO Rocket; as batalhas continuam manuais para começar.' },
      { name: 'Economia de bateria', detail: 'Reduza resolução, limite a 20 FPS ou use gráficos simplificados.' },
      { name: 'Ocultar sobreposições', detail: 'Oculta botões, indicadores e textos de IVs; toque com três dedos para reexibir.' },
      { name: 'Backup das configurações', detail: 'Exporta e importa configurações em JSON, sem tokens, localização e rotas.' },
    ],
  },
];

// Real screenshots bundled in public/images. Dimensions are the source pixels,
// used for width/height to avoid layout shift while loading.
export const screenshots = [
  {
    id: 'world-map',
    src: 'images/world-map.png',
    title: 'Mapa-múndi',
    caption: 'Mapa do PGBR com favoritos, rotas, locais próximos e controles de caminhada e teletransporte. Dados © OpenStreetMap.',
    width: 1080,
    height: 1920,
  },
  {
    id: 'encounter',
    src: 'images/encounter.png',
    title: 'Sobreposições de captura',
    caption:
      'Encontro do Pokémon GO com PC, nível, IVs, contador de frutas, proteção de transferência e estado do VGP sobre a tela do jogo.',
    width: 1080,
    height: 2404,
  },
  {
    id: 'main-menu',
    src: 'images/main-menu.png',
    title: 'Menu de configurações',
    caption:
      'Menu de configurações em tema escuro, com seções de localização, captura, automação e outras opções.',
    width: 1080,
    height: 2404,
  },
];

export const steps = [
  {
    n: '01',
    title: 'Baixar',
    body: 'Abra a última compilação aprovada no GitHub Actions, baixe o artefato PGBR-v0.00.02-LT-APKs e extraia PGBR-v0.00.02-LT.apk.',
  },
  {
    n: '02',
    title: 'Instalar',
    body: 'Abra o APK em um Android ARM64 ou no emulador MuMu e permita a instalação quando solicitado.',
  },
  {
    n: '03',
    title: 'Abrir',
    body: 'Abra Pokémon GO, toque na Poké Bola flutuante do PGBR e selecione Configurações.',
  },
];

export const faqs = [
  {
    q: 'Em quais aparelhos funciona?',
    a: 'Funciona em Android ARM64 e no emulador MuMu. A versão sem Root não exige modificações no aparelho; a opção Root experimental utiliza outro instalador.',
  },
  {
    q: 'Posso instalar sobre o aplicativo da Play Store?',
    a: 'Não diretamente. Como as assinaturas são diferentes, o PGBR não atualiza a instalação da Play Store; será necessária instalação separada conforme as instruções.',
  },
  {
    q: 'O que é o PGBR Root?',
    a: 'Instalador experimental para aparelhos com Root. Usa um módulo Zygisk para carregar recursos no aplicativo oficial, exigindo Magisk 27+, KernelSU ou APatch e versão de jogo compatível.',
  },
  {
    q: 'O LivePokeMap está incluído?',
    a: 'Não. Os feeds remotos exigem assinatura ativa do próprio LivePokeMap, contratada separadamente.',
  },
  {
    q: 'Garante capturas ou evita banimentos?',
    a: 'Não. Os recursos são experimentais, e o tempo de espera é apenas uma estimativa local; não há garantia contra sanções.',
  },
  {
    q: 'Funciona em qualquer versão do jogo?',
    a: 'Não. Recursos nativos dependem de versões específicas do Pokémon GO. Consulte as notas de cada lançamento.',
  },
];

// Community cooldown guidance. Prose is rephrased from player references, kept
// short, and framed as estimates — not an official server guarantee. See the
// Cooldown component for how trigger / safe groups and the chart are rendered.
export const cooldown = {
  intro:
    'Considere a distância entre a última ação que envolve localização e a próxima. O tempo já aguardado conta; teletransportar sem agir não reinicia o cronômetro. A tabela da comunidade estima até duas horas de espera. Agir cedo demais pode causar fugas ou impedir giros. O PGBR não consulta um cronômetro no servidor.',
  // Actions widely reported to start the clock.
  triggers: [
    'Capturas de Pokémon selvagens, incluindo Incenso e Módulo Atrair',
    'Arremessar ou deixar cair qualquer bola',
    'Usar uma fruta em encontros, incluindo Reides',
    'Girar Poképaradas, mesmo após o limite diário',
    'Ações em ginásios no alcance: colocar, batalhar ou alimentar',
    'Fuga causada pelo limite de capturas',
    'Captura ou giro automático',
  ],
  // Actions widely reported not to start the clock.
  safe: [
    'Teletransportar sem realizar outra ação',
    'Abrir um encontro sem arremessar bola ou usar fruta',
    'Caminhar ou chocar ovos',
    'Alimentar ginásios remotamente fora do alcance do radar',
    'Resgatar recompensas de pesquisas ou semanais, incluindo capturas de pesquisas',
    'Presentes, trocas, evoluções, fortalecimento, transferências e fotos',
    'Troca de companheiro e doces',
    'Itens de Pokémon ou um segundo ataque carregado',
    'Ativar Incenso, Ovo da Sorte ou Pedaço de Estrela',
    'Excluir itens do inventário',
    'PvP e batalhas entre treinadores',
    'Colocar Módulos Atrair',
    'Alterar itens da loja e avatar',
    'Transferências para Switch, passes gratuitos e aparições em fotos',
    'Reides rápidas após a espera anterior, sem arremessar bola ou usar fruta no encontro da Reide',
  ],
  // Distance (km) → community-reported wait. Values are estimates; round up to
  // the next listed distance. "<" means reported as less than that many minutes.
  chart: [
    { km: '1', wait: '<1 min' },
    { km: '2', wait: '1 min' },
    { km: '3', wait: '<2 min' },
    { km: '5', wait: '2 min' },
    { km: '7', wait: '5 min' },
    { km: '9', wait: '<7 min' },
    { km: '10', wait: '7 min' },
    { km: '12', wait: '8 min' },
    { km: '18', wait: '10 min' },
    { km: '26', wait: '15 min' },
    { km: '42', wait: '19 min' },
    { km: '65', wait: '22 min' },
    { km: '76', wait: '<25 min' },
    { km: '81', wait: '25 min' },
    { km: '100', wait: '35 min' },
    { km: '220', wait: '<40 min' },
    { km: '250', wait: '45 min' },
    { km: '350', wait: '<51 min' },
    { km: '375', wait: '54 min' },
    { km: '460', wait: '62 min' },
    { km: '500', wait: '<65 min' },
    { km: '565', wait: '69 min' },
    { km: '700', wait: '78 min' },
    { km: '800', wait: '84 min' },
    { km: '900', wait: '92 min' },
    { km: '1000', wait: '99 min' },
    { km: '1100', wait: '107 min' },
    { km: '1200', wait: '114 min' },
    { km: '1300', wait: '117 min' },
    { km: '1350+', wait: '120 min' },
  ],
  reviewed: '2026-10-29',
};
