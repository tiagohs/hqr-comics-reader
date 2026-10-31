# Design: HQR

> Atualizado em: 2026-10-31 · Valores exatos em `docs/tokens.json` (W3C DTCG) · Telas: canvas "HQR — design"
> Direção (ADR-026): linguagem de HQ em papel e tinta nos títulos, selos, capas, painéis e fundos; controles, campos, chips e barras seguem o Material 3 (Android e web) e a HIG (iOS).

## 1. Marca

| Item | Decisão |
| :-- | :-- |
| Nome | HQR (Q-02) |
| Ícone | Balão de fala em papel com o rabicho em triângulo, retícula no canto, sobre fundo tinta; camada monocromática para ícone temático |
| Logotipo | "HQR" em Bangers (OFL), sem contorno |
| Voz | Curta e direta; onomatopeia ("OPS!", "CRI CRI…", "FIM!") só como enfeite, nunca como única informação |

## 2. Cores

Papel e tinta são fixos; só o destaque muda (violeta #5C51A8 fora da obra, cor da capa dentro dela, RF-98). Tema segue o sistema; escuro, claro e preto puro em Ajustes (RF-76).

| Papel | Claro (papel) | Escuro (tinta) |
| :-- | :-- | :-- |
| Fundo | #F6F1E7 | #16161A |
| Superfície mais baixa · baixa · média · alta · mais alta | #FFFDF7 · #FBF7EE · #F1ECE1 · #ECE6DA · #E5DFD2 | #0F0F12 · #1C1C21 · #222127 · #2A292F · #35343A |
| Texto · secundário | #16161A (16,0:1) · #55545A (6,7:1) | #F6F1E7 (16,0:1) · #C4C0C8 (10,1:1) |
| Linha · linha fraca | #77757C · #CFC8BB | #8F8C94 · #3E3D43 |
| Tinta dos quadros (bordas de capa, divisórias de lista, sombras deslocadas) | = texto | = texto |
| Destaque, texto nele, contêiner | da semente (violeta ou capa), tons 40/80 do M3 | idem |
| Erro | #BA1A1A · contêiner #FFDAD6 | #FFB4AB · contêiner #93000A |
| Legenda (caixa de título na capa, aviso da primeira abertura) | #FFF1A8 com tinta #16161A | igual |

- **Cor da capa (RF-98, ADR-021):** na obra, no leitor e no painel de detalhe; o tom claro do contêiner a 55% sobre o papel pinta o fundo da obra. Sem cor válida, violeta. Desligável em Ajustes.
- **Cores dinâmicas do sistema** (Android 12+) opcionais; substituem só o violeta.
- **Status da obra:** ponto verde #2E7D32 (Em lançamento), destaque (Finalizada), âmbar #B26A00 (Em hiato), erro (Cancelada).

## 3. Tipografia

| Uso | Fonte |
| :-- | :-- |
| Títulos de tela, título da obra, seções, números de capítulo, selos | Bangers (OFL), com sombra deslocada de 3 no destaque a 55% nos títulos |
| Título original vertical (mangá) | Dela Gothic One (OFL), recortada para kana e kanji JIS nível 1 (`scripts/fonts/subset_japanese.py`), contorno de tinta |
| Interface, corpo, botões, chips, campos | Roboto Flex na escala M3; títulos de barras pequenas em Roboto Flex largura 75 e peso 800 |
| iOS | SF Pro com Dynamic Type nos controles; Bangers nos títulos |

## 4. Forma, espaço e movimento

- **Quadro de HQ:** capas com cantos de 2 e borda de tinta de 2; painéis de destaque (Continuar lendo, Armazenamento, estado vazio) com borda de 3 e sombra deslocada de 4 a 6 em tinta.
- **Fundos:** retícula (pontos de 1,1 em passo de 6) e, na obra, linhas de velocidade a cada 7° (mangá, quando há título japonês) ou pontos Ben-Day na cor da capa (HQ).
- **Controles Material:** botões em pílula (48 a 52 de altura), botão de ícone redondo, chips de 8 dp, campos com contorno de 1, interruptores do M3; sem sombra deslocada, sem inclinação.
- Espaço: grade de 8 (margens 16, 8 entre itens, 16 entre blocos, 24 antes das seções); alvos de 48 (44 no iOS).
- **Movimento (RF-118; 150 a 480 ms; "reduzir movimento" vira esmaecimento de 150 ms):** capa → obra por elemento compartilhado girando 2,5°; linhas de velocidade finas (0,6 a 1 dp, tinta a 22%) só nas bordas do painel, entrando de fora para dentro; título vertical escreve de cima para baixo; selos entram como carimbo (mola); listas em cascata só ao abrir a tela (30 ms por linha, até 8); primeiro acesso: retícula desce e cartões se desenham a traço; folhas sobem. Tudo nativo (Compose, SwiftUI), sem Lottie.

## 5. Componentes

| Componente | Regras |
| :-- | :-- |
| Barra de navegação · trilho · gaveta | Biblioteca · Explorar · Histórico · Ajustes; indicador em pílula do M3 |
| Barra superior | Título em Bangers; ações em botões de ícone; na obra, voltar e ⋮ em botões de ícone redondos, barra fixa que assume o papel ao rolar |
| Capa | 2:3, quadro de tinta; explosão com não lidos no topo direito; no modo compacto, título em caixa de legenda amarela; "Na biblioteca" em etiqueta tinta |
| Continuar lendo | Balão: painel com borda e sombra, rabicho embaixo; capa, "Continuar lendo…", título em Bangers, "Cap. 12 · pág. 8", trilha de retícula e botão "Continuar" |
| Obra | Painel do topo (capa inclinada 2,5° com sombra, título vertical ou pontos Ben-Day, gêneros em etiquetas de tinta), linha de tinta de 3, título em Bangers, tags de assistência do M3 para autor (quando houver), status com ponto e fonte; sinopse inteira (dobra em 8 linhas acima de 600 caracteres, com "Mais"); botão "Ler cap. 1" / "Continuar · cap. 12" / "Ler de novo"; na biblioteca, botão de ícone redondo cheio; fora dela, "Adicionar à biblioteca" contornado em largura total; ⋮ com Atualizar, Baixar, Mudar categorias, Migrar e Abrir no navegador |
| Linha de capítulo | Número em Bangers 26 (novo no destaque, lido em secundário); selo "NOVO!" inclinado; divisória de tinta de 2; download à direita |
| Carregando (RF-117) | Esqueleto suave: cantos de 6, tinta a 8%, brilho que atravessa em 1,6 s, sem borda; três pontos pulsando no destaque com "Buscando no MangaDex…"; o resto da tela continua usável |
| Estado vazio | Quadro inclinado com retícula e onomatopeia ("CRI CRI…"), título em Bangers, frase e botão |
| Erro | Tela cheia: explosão "OPS!" no vermelho de erro, título, frase e botões empilhados. Dentro do conteúdo: faixa de 4 em erro, uma ação ao lado ou duas embaixo; erro de fonte: "Erro ao carregar o conteúdo de <fonte>. Tente novamente mais tarde." |
| Fonte (Explorar) | Seções "Favoritas" (entram no feed) e "Outras fontes"; monograma em quadro de tinta, cheio na cor do destaque quando favorita; estrela à direita (RF-115) |
| Feed | Linha "Das suas favoritas · atualizado há 12 min · puxe para atualizar" (RF-116) |
| Armazenamento (Downloads) | Painel com o total usado em Bangers, livre ao lado, barra dividida (baixados no destaque, cache em tinta, livre vazio), legenda com valores e "Limpar cache" |
| Primeira abertura | Topo em tinta com retícula; "Como você gosta de ler?"; cartões desenhados à mão (Página por página · Rolagem contínua) como rádio; "Selecione o idioma" com chips; aviso em legenda amarela; "Iniciar leitura" |
| Snackbar, folhas, diálogos, filtros, segmentados | Padrão do M3 (Android, web) e da HIG (iOS) |
| Web | `:focus-visible` com contorno de 2 no destaque; hover de 8% |

## 6. Voz e glossário

Curto e direto; botões no infinitivo; erro diz o que houve e como consertar; maiúscula só na primeira palavra; caixa alta só em grupos de data.

| Usar | Evitar |
| :-- | :-- |
| obra; capítulo (cap.), edição (ed.), volume (vol.) | série, título; número sem unidade |
| capítulo lido, edição lida | concluído (para o que foi lido) |
| Em lançamento, Finalizada, Cancelada, Em hiato (filtro "Estado": finalizadas) | Concluído, Em andamento como estado da obra, Status |
| biblioteca | estante, coleção |
| fonte (online), servidor, arquivos locais | site, provedor, extensão |
| baixar, baixado, na fila | download como verbo |
| ponte; chave de acesso (token), só na web | proxy, worker, CORS |
| "Pede verificação" | captcha; Cloudflare fora da ponte |
| "Usuário ou senha recusados" | login recusado |
| conteúdo sugestivo (18+) | conteúdo adulto |
| há 2 dias; Somente com Wi-Fi; Excluir após a leitura | 2 dias atrás; Só em Wi-Fi |
| Ajustes de leitura | Ajustes do leitor (como botão) |
| sem jargão | porta 443, assinatura, issue, OPFS, WebView |

## 7. Telas desenhadas

Canvas "HQR — design" (68 pranchas; Tweaks: tema papel/tinta e cor da capa), em fileiras: celular (primeira abertura e biblioteca; obra e todos os estados dela; leitor; explorar, fonte e busca; histórico, downloads e ajustes), tablet Android, iPhone e iPad, web, identidade e cor da capa, e o fluxo animado (biblioteca → obra com capítulos chegando depois → leitor quadro a quadro).
