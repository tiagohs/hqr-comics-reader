# Design: HQR

> Atualizado em: 2026-10-29 · Valores exatos em `docs/tokens.json` (W3C DTCG) · Telas: canvas "HQR — design"
> Direção: Material 3 Expressive no Android e controles do iOS no iPhone; o "jeito de HQ" vem de forma, retícula, balão e ilustração, nunca da página da obra.
> Origem: pesquisa (`docs/benchmark.md` §5), referências do Tiago no Dribbble (Cstation app e web, UX Interactions Manga Reader, Manga Reader App Concept) e herança do Script Reader e da 0.0.5.

## 1. Marca

| Item | Decisão |
| :-- | :-- |
| Nome | HQR (Q-02) |
| Ícone | Balão de fala em papel com o rabicho em triângulo (herança do triângulo da 0.0.5), retícula de pontos no canto, sobre fundo tinta; camada monocromática para ícone temático |
| Logotipo | "HQR" em Bangers (OFL), inclinação natural da fonte, sem contorno |
| Voz | Curta e direta; nada de gíria de HQ em mensagens de erro; onomatopeia só como enfeite, nunca como única informação |

## 2. Cores

Base violeta em todo o app: esquema do M3 gerado a partir de #5C51A8 (neutros com croma 6, levemente frios). Tema segue o sistema; escuro, claro e preto puro em Ajustes (RF-76).

| Papel | Escuro | Claro |
| :-- | :-- | :-- |
| Fundo | #141318 | #FCF8FF |
| Superfície · alta · mais alta | #1C1B20 · #2A292F · #35343A | #F7F2FA · #F1ECF4 · #E5E1E9 |
| Texto · secundário | #E5E1E9 (14,3:1) · #C9C4D3 (10,8:1) | #1C1B20 (16,3:1) · #474551 (8,9:1) |
| Linha (bordas de controles) · linha fraca (divisórias) | #928F9C (5,8:1) · #474551 | #787582 (4,3:1) · #C9C4D3 |
| Destaque | #C7BFFF (10,8:1) | #5D52A9 (6,2:1) |
| Texto no destaque | #2E2078 | #FFFFFF |
| Contêiner do destaque · texto nele | #453990 · #E5DEFF | #E5DEFF · #180064 |
| Erro · texto nele | #FFB4AB · #690005 | #BA1A1A · #FFFFFF |
| Contêiner de erro · texto nele | #93000A · #FFDAD6 | #FFDAD6 · #410002 |

- **Cor da capa (RF-98, ADR-021):** na obra, no leitor (barras, folha de ajustes, transição) e no painel de detalhe em telas grandes, o esquema vem da capa: quantização e pontuação do M3 (MaterialKolor), pulando cores a até 35° da matiz do erro ou com croma abaixo de 16; sem cor válida, fica o violeta. Os papéis seguem os mesmos tons da tabela (primário 40/80, contêiner 90/30, superfícies com croma 6), então o contraste vale para qualquer capa. Troca de cor em até 300 ms; com "reduzir movimento", direta. Desligável em Ajustes. Fora da obra e do leitor, sempre violeta.
- **Cores dinâmicas do sistema** (Android 12+) opcionais (RF-76); a cor da capa tem prioridade dentro da obra.
- **Ilustração (fixa, nunca em controles):** tinta #16161A, papel #F6F1E7, retícula #8C877F e violeta #5C51A8 como única cor.
- **Fundo do leitor:** preto #000000, cinza #3A3A3C, branco #FFFFFF ou automático (RF-46).

## 3. Tipografia

| Uso | Fonte |
| :-- | :-- |
| Interface (Android) | Roboto Flex, escala M3; corpo e rótulos no padrão |
| Títulos (Display, Headline) | Roboto Flex com largura 75 e peso 800: lembra letreiro de HQ sem fonte nova; nunca em texto corrido |
| Marca e onomatopeia | Bangers: logotipo, carregamento longo, estados vazios, transição de capítulo |
| Web | Roboto Flex, igual ao Android (o Compose para Web desenha a interface) |
| iOS | SF Pro com Dynamic Type; títulos em SF Pro Condensed Heavy; Bangers só na marca |

## 4. Forma, espaço e movimento

- **Corte de quadro:** capas com cantos de 10 e o canto inferior direito chanfrado em 14 (`shape.cover-cut`), em todas as grades; esqueletos com a mesma forma.
- **Balão:** dicas, contagem de não lidos e títulos de estado vazio em balão (canto 20, rabicho de 10); nunca em diálogos ou avisos.
- **Retícula:** pontos de 2 em passo de 6 para o "lido" sobre capas (45%), a trilha do progresso de leitura e de download, e o fundo das ilustrações.
- Cartões 16, chips 8, folhas 28 no topo, botões em pílula.
- Espaço: grade de 4; margens 16 · 24 · 32; 12 entre capas; alvos de 48.
- Movimento: molas do `motionScheme` expressivo do M3; capa → obra por elemento compartilhado; obra → leitor por expansão do botão "Continuar"; máximo 400 ms; "reduzir movimento" troca tudo por esmaecimento de 150 ms.

## 5. Componentes

| Componente | Regras |
| :-- | :-- |
| Barra de navegação · trilho · gaveta | Biblioteca · Explorar · Histórico · Ajustes; indicador padrão do M3 em contêiner do destaque; gaveta fixa na web e a partir de 1200 dp |
| Barra superior | Título em Headline condensado; ações: busca, downloads (com selo), ⋮ |
| Capa | Proporção 2:3, corte de quadro; balão de não lidos no topo esquerdo, baixado no topo direito; título em 2 linhas e uma linha de estado ("3 novos", "em dia", "vol. 2 de 4") embaixo |
| Continuar | Cartão largo no topo da Biblioteca: capa, título, "Cap. 12 · pág. 8 de 22", trilha de retícula e botão "Continuar" |
| Abas de categoria | Abas roláveis do M3 com contagem; exemplo: Todas, Lendo, Acompanhando, Para depois; a padrão se chama "Sem categoria" e fica escondida quando vazia; estado da obra nunca é categoria |
| Obra | Fundo: capa desfocada que esmaece para o fundo; capa com corte; título condensado; autores; estado e fonte em texto secundário; "Continuar cap. N · pág. P" (botão cheio, sempre o item em andamento da lista) + grupo de botões (Na biblioteca, Baixar, Abrir site, ⋮); obra de Arquivos: Na biblioteca e Detalhes (Exportar na web), sem Baixar nem Abrir site; sinopse em 3 linhas + "Mais"; contagem do título bate com a lista |
| Linha de capítulo | Número em Title condensado (novo no destaque, lido em secundário); em andamento = página e trilha de retícula na base; grupo tocável em peso 600 (RF-07); download à direita: ícone para baixar, etiqueta "Na fila" ou "Baixado" |
| Barra flutuante do leitor | Toolbar flutuante do M3 Expressive: capítulo anterior e próximo, trilha de páginas "X de Y", ajustes (alvo 48); na leitura da direita para a esquerda a trilha enche pela direita, com "22" à esquerda e "1" à direita, em todas as superfícies; nenhuma barra sobre a página: faixas superior e inferior reservadas |
| Transição de capítulo | Página cheia no fluxo: onomatopeia curta ("FIM!" / "THE END" por idioma), cap. atual e próximo com miniatura, selo Baixado/Online, aviso de lacuna em contêiner de erro suave |
| Estado vazio | Ilustração em quadro de HQ (traço de nanquim, retícula, uma cor), título em balão, frase e um botão |
| Carregando | Esqueleto com forma final; `LoadingIndicator` M3; onomatopeia ("ZOOM", "VRUUM") só depois de 1,5 s |
| Snackbar | Superfície inversa (escuro: #E5E1E9 / texto #313036 / ação #5D52A9; claro: #313036 / #F4EFF7 / #C7BFFF); "Desfazer" em toda remoção; erro com ícone em `--err` |
| Filtros | Folha com chips e "Mostrar N obras" fixo; chips de filtro do M3 com marca de seleção; gênero excluído: fundo superfície alta, contorno, ícone "−", "Sem Romance" sem risco (`aria-pressed="mixed"`) |
| Botões segmentados | Grupo conectado do M3 (gap 2, cantos externos 20, internos 8), check no selecionado, `radiogroup`; Modo: "Esq.→dir.", "Dir.→esq.", "Vertical", "Webtoon"; amostras de fundo de 48 com nome acessível |
| Erros | Dentro do conteúdo: fundo superfície alta, borda esquerda de 4 em `--err`, ícone `--err`, texto normal, ação como botão de texto que diz o conserto ("Corrigir login", "Tentar de novo"); bloco de erro cheio só quando o erro bloqueia a tela |
| Selo "Na biblioteca" | Contêiner do destaque, 600 11/16, padding 2×8, cantos 10/10/10/2; capa sem escurecer |
| Caixa de informação | Superfície alta, cantos 16, padding 12×16, ícone 20 e texto 13/18 em secundário, sem borda |
| Ícones de destino | Ajustes = engrenagem; "tune" só em filtros e ajustes de leitura; montar o feed = lista com lápis |
| Web | Ações em `<button>`/`<a>`/`<input>`; `:focus-visible` com contorno de 2 em `--pri` e afastamento 2; hover com 8% de `--on`; botões de hover nas capas ≥ 36 |
| Alvos | 48 no Android e na web, 44 no iOS; grade de espaço de 4 |
| Textos pequenos | Nada abaixo de 11 px (label-small) com peso 600 |
| Fonte | Linha com monograma de 2 letras (MD, HN, IA, NC, MF, YO, WC, XO; servidores KO, KA) em superfície alta e cantos 10/10/10/2, nome, idioma e chave; erro na própria linha com a ação abaixo do subtítulo, chave mantida; Weeb Central = "Pede verificação"; XOXO = "Pausada pelo HQR · o site mudou de endereço" |

## 6. Voz e glossário

Curto e direto; botões no infinitivo; erro diz o que houve e como consertar; maiúscula só na primeira palavra; caixa alta só em grupos de data.

| Usar | Evitar |
| :-- | :-- |
| obra; capítulo (cap.), edição (ed.), volume (vol.) | série, título; número sem unidade |
| capítulo lido, edição lida | concluído (para o que foi lido) |
| Em lançamento, Concluído, Cancelado, Hiato (filtro "Estado") | Em andamento como estado da obra (fica para o progresso), Status |
| biblioteca | estante, coleção |
| fonte (online), servidor, arquivos locais | site, provedor, extensão |
| baixar, baixado, na fila | download como verbo |
| ponte; chave de acesso (token), só na web | proxy, worker, CORS |
| "Pede verificação" | captcha; Cloudflare fora da ponte |
| "Usuário ou senha recusados" | login recusado |
| conteúdo sugestivo (18+) | conteúdo adulto |
| há 2 dias; Só no Wi‑Fi | 2 dias atrás; Só em Wi-Fi |
| Ajustes de leitura | Ajustes do leitor (como botão) |
| sem jargão | porta 443, assinatura, issue, OPFS, WebView |

## 7. Telas desenhadas

Canvas "HQR — design" (44 pranchas; ajustes Tema e Capa nos Tweaks):

| Fileira | Pranchas |
| :-- | :-- |
| Celular · biblioteca, obra e leitor | SCR-ONB-001 · Primeira abertura; SCR-LIB-001 · Biblioteca; SCR-LIB-001 · Seleção; SCR-LIB-002 · Filtrar, ordenar e exibir; SCR-LIB-003 · Categorias; SCR-OBR-001 · Obra com a cor da capa; SCR-OBR-002 · Migrar ou juntar; SCR-RDR-001 · Leitor com menu; SCR-RDR-001 · Modo webtoon; SCR-RDR-002 · Ajustes do leitor; SCR-RDR-003 · Transição de capítulo |
| Celular · explorar, histórico e ajustes | SCR-EXP-001 · Explorar e Feed; SCR-EXP-004 · Montar o feed; SCR-EXP-002 · Fonte; SCR-EXP-003 · Filtros da fonte; SCR-SRCH-001 · Busca global; SCR-HIS-001 · Histórico; SCR-DL-001 · Downloads; SCR-SET-001 · Ajustes; SCR-SRC-001 · Fontes, pastas e servidores; SCR-SRC-002 · Ajustes da fonte; SCR-SRV-001 · Adicionar servidor; SCR-SRV-002 · Catálogo do servidor; SCR-SET-002 · Backup e restauração; SCR-SET-003 · Sobre; SCR-SET-004 · Atualização do app |
| Estados | SCR-STA-001 · Vazio, sem conexão, carregando; SCR-STA-001 · Verificação do Cloudflare |
| Tablet Android | Tablet · Biblioteca + Obra (lista-detalhe); Tablet · Leitor em duas páginas; Tablet · Explorar e busca; Tablet · Ajustes (lista-detalhe) |
| iPhone e iPad (HIG) | iPhone · Obra; iPad · Biblioteca e obra; iPad · Leitor em duas páginas |
| Web (navegador) | Web · Biblioteca; Web · Obra; Web · Leitor; Web · Explorar; Web · Ponte e importar arquivos; Web · Janela estreita |
| Identidade e cor da capa | Marca, cores, tipografia e formas; RF-98 · Obra: regra × dominante; RF-98 · Cor da capa: regra e casos |
