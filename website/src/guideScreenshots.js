// Real MuMu captures; original evidence and crop receipts are retained locally.
const shots = {
  "vgp-plus": {"id": "vgp-plus", "src": "images/guides/vgp-plus.webp", "width": 1040, "height": 1340, "title": "Configurações do VGP+ e discos de ginásios", "caption": "Configuração do VGP com alvos próximos, tempo estimado de espera, bolas, arremessos e giro automático de ginásios. Inicie ou pare na sobreposição do VGP."},
  "buddy-emotion": {"id": "buddy-emotion", "src": "images/guides/buddy-emotion.webp", "width": 1040, "height": 740, "title": "Cuidados com o companheiro", "caption": "Configuração do companheiro com reserva de frutas, coleta opcional de presentes e indicador de PE. Os dados exibidos são apenas deste exemplo."},
  "buddy-priority": {"id": "buddy-priority", "src": "images/guides/buddy-priority.webp", "width": 959, "height": 615, "title": "Escolha a ordem das frutas", "caption": "As frutas numeradas definem a prioridade: Nanab, Frambo e Caxí neste exemplo. Salvar aplica a escolha; Cancelar descarta."},
  "batch-evolution": {"id": "batch-evolution", "src": "images/guides/batch-evolution.webp", "width": 1015, "height": 872, "title": "Prévia de Pokémon marcados", "caption": "Antes da evolução em massa, informe a etiqueta Evoluir e escolha se deseja removê-la. O botão Iniciar permanece desativado até haver uma prévia válida."},
  "battles": {"id": "battles", "src": "images/guides/battles.webp", "width": 1040, "height": 895, "title": "Opções de automação de batalhas", "caption": "Controles separados de ataques rápidos, carregados e esquivas para Reides, ginásios e Dinamax."},
  "gym-slotter": {"id": "gym-slotter", "src": "images/guides/gym-slotter.webp", "width": 1015, "height": 905, "title": "Opções de ginásios e alimentação", "caption": "Opções de colocação e alimentação selecionadas com a execução desligada. São configurações de exemplo."},
  "event-boxes": {"id": "event-boxes", "src": "images/guides/event-boxes.webp", "width": 959, "height": 402, "title": "Coleta de caixas do Halloween", "caption": "Abre as caixas visíveis do evento de Halloween e pula encontros, sem capturar Pokémon."},
  "hotspots": {"id": "hotspots", "src": "images/guides/hotspots.webp", "width": 1080, "height": 1860, "title": "Escolher um ponto popular", "caption": "Lista de locais populares no menu rápido. Os fusos UTC são referenciais e podem variar ao longo do ano."},
  "hotspot-actions": {"id": "hotspot-actions", "src": "images/guides/hotspot-actions.webp", "width": 808, "height": 286, "title": "Caminhar ou teletransportar", "caption": "O Pier 39 oferece opções de caminhar ou teletransportar. O teletransporte pede confirmação do tempo de espera."},
  "teleport-cooldown": {"id": "teleport-cooldown", "src": "images/guides/teleport-cooldown.webp", "width": 808, "height": 306, "title": "Verificar a espera do destino", "caption": "Confirmação do tempo de espera para Pier 39. O zero exibido vale apenas para esta imagem; o teletransporte foi cancelado."},
  "world-map": {"id": "world-map", "src": "images/guides/world-map.webp", "width": 1080, "height": 1860, "title": "Selecionar um destino no mapa", "caption": "Pier 39 selecionado como destino vermelho. Escolher o marcador não move o personagem; caminhar e teletransportar são ações separadas. Dados: colaboradores do OpenStreetMap."},
};
export const guideScreenshots = {
  "vgp-plus": [shots["vgp-plus"]],
  "virtual-go-plus": [shots["vgp-plus"]],
  "buddy-emotion": [shots["buddy-emotion"], shots["buddy-priority"]],
  "batch-evolution": [shots["batch-evolution"]],
  "battles": [shots["battles"]],
  "gym-slotter": [shots["gym-slotter"]],
  "event-boxes": [shots["event-boxes"]],
  "hotspots-favorites": [shots["hotspots"], shots["hotspot-actions"]],
  "cooldown": [shots["teleport-cooldown"]],
  "world-map": [shots["world-map"]],
};
