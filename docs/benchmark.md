# Benchmark e boas práticas: HQR

> Atualizado em: 2026-10-29 · Pesquisa em fontes públicas e no código do Mihon/TachiyomiSY. **[NC]** = não confirmado em fonte primária.
> Apoio ao `docs/srs.md`; cada achado aponta a área de requisitos que alimenta.

## 1. Leitores existentes

| App | Plataforma · licença | Estado (out/2026) | Fontes | Loja |
| :-- | :-- | :-- | :-- | :-- |
| [Mihon](https://github.com/mihonapp/mihon) | Android · Apache-2.0 | Ativo (v0.21.x) | Extensões APK de repositórios de terceiros; nenhuma embutida | Fora da Play |
| [TachiyomiSY](https://github.com/jobobby04/TachiyomiSY) | Android · Apache-2.0 | Ativo (1.13.2, base Mihon 0.20.1) | APK + fontes delegadas embutidas (MangaDex etc.) | Fora da Play |
| [Komikku](https://github.com/komikku-app/komikku) | Android 8+ · Apache-2.0 | Ativo (v1.14.1) | APK (base Mihon + SY) | Fora da Play |
| [TachiyomiJ2K](https://github.com/Jays2Kings/tachiyomiJ2K) | Android | Ativo, ritmo lento | APK | Fora da Play |
| [Kotatsu](https://github.com/KotatsuApp/Kotatsu) | Android · GPL-3.0 | **Encerrado** (nov/2025, pressão da Kakao) | 1.200+ parsers embutidos | Fora da Play |
| [Aidoku](https://github.com/Aidoku/Aidoku) | iOS · GPL-3.0 | Ativo (v0.9) | Fontes WASM (Rust) | TestFlight/AltStore, fora da App Store |
| [Paperback](https://apps.apple.com/us/app/id1626613373) | iOS · fechado | Ativo | Na loja como "cliente Komga"; fontes vêm por fora | App Store |
| [Tachimanga](https://apps.apple.com/us/app/tachimanga/id6447486175) | iOS · fechado, assinatura | Ativo (v5.2) | Arquivos locais, Komga, Kavita, Suwayomi | App Store (17+) |
| Perfect Viewer | Android · fechado | Ativo, ~6,6 mi instalações | Só arquivos locais e de rede (SMB, OPDS) | Play |
| [Kavita](https://github.com/Kareadita/Kavita) / [Komga](https://github.com/gotson/komga) | Servidor próprio | Ativos | Biblioteca do usuário; OPDS e API REST | — |

**Padrão do mercado:** quem agrega sites de terceiros fica fora das lojas; quem está nas lojas lê arquivos do usuário ou servidores dele (Komga, Kavita, OPDS) e não traz fontes de terceiros embutidas.

## 2. O que reaproveitar da lógica do Mihon/TachiyomiSY

| Tema | Como eles fazem | Requisitos |
| :-- | :-- | :-- |
| Contrato de fonte | `getPopularManga`, `getLatestUpdates`, `getSearchManga(page, query, filters)`, `getMangaUpdate` (detalhes + capítulos), `getPageList(chapter)`; modelos `SManga`, `SChapter`, `Page`, `MangasPage`; preferências por fonte | RF-01, RF-06 |
| Dados | `manga`, `chapter`, `category`, `manga_category`, `history`, `manga_track`, `source`, `excluded_scanlator` | RF-30, RF-35, RF-53 |
| Leitor | Modos LTR, RTL, vertical, webtoon (rolagem contínua); zonas de toque (L, Kindle, bordas); cortar bordas; dividir/juntar páginas duplas; zoom inicial; filtros de cor; teclas de volume; tela sempre ligada; pré-carregamento ajustável | RF-36 a RF-51 |
| Downloads | Fila em WorkManager, retomada de imagem, cache de downloads, CBZ opcional | RF-62 a RF-68 |
| Atualização da biblioteca | Worker periódico que busca capítulos novos e notifica | RF-58 (só manual na 1.0) |
| Trackers | MyAnimeList, AniList, Kitsu, MangaUpdates (+ Komga/Kavita) | Fora da 1.0; RF-20 (Komga/Kavita) |
| Backup | Arquivo ProtoBuf compactado (`.tachibk`) | RF-72 a RF-75 (formato próprio, igual no iOS) |
| Exclusivos SY | Juntar a mesma obra de várias fontes, editar metadados, buscas salvas, feed de até 5 fontes, migração em lote, categorias dinâmicas, autoscroll, detecção de webtoon | RF-24, RF-25, RF-37, RF-49, RF-69 a RF-71 (edição de metadados fora) |

**Limite para reaproveitar:** o `source-api` do Mihon é só Android (RxJava + jsoup). No HQR o contrato vai para o núcleo KMP (`shared/`), com Ktor + Ksoup, para valer no iOS. Reaproveita-se o desenho e a lógica, não o código.

## 3. Fontes pedidas

| Fonte | Acesso | Regras | Consequência |
| :-- | :-- | :-- | :-- |
| [MangaDex API v5](https://api.mangadex.org/docs/) | REST oficial: `/manga`, `/manga/{id}/feed`, `/at-home/server/{chapterId}` (URL das imagens válida ~15 min) | ~5 req/s por IP; at-home 40/min; User-Agent próprio; relatório at-home para imagens fora de `mangadex.org`; **creditar MangaDex e grupos de scan e atender remoções; proibido anúncio ou serviço pago** (doação permitida) | RF-07 a RF-11; C-02 (sem anúncios) |
| HQ Now ([extensão Mihon](https://github.com/keiyoushi/extensions-source/tree/main/src/pt/hqnow)) | GraphQL interno não documentado (`admin.hq-now.com/graphql`): populares, recentes, busca, detalhes, páginas | A extensão limita a 1 pedido a cada 2 s; sem termos para terceiros [NC]; `hqnow.cc` vs `hq-now.com` [NC] | RF-12 só no sabor GitHub; C-03 |

**Adicionais escolhidas (§3.1.18 do SRS):** Internet Archive (domínio público, API oficial, RF-99), NamiComi (API, criadores, RF-100), MangaFlix e Yomu Mangas (pt-BR, API JSON interna, RF-101 e RF-102), Weeb Central (mangá em inglês, HTML com Cloudflare leve, RF-103), XOXO Comics (HQ em inglês, HTML, RF-104). Descartadas: GetComics (só links de download), ReadComicOnline (anti-bot pesado), MangaPark e Comick (fora do ar ou cópias). Fonte: extensões do [keiyoushi](https://github.com/keiyoushi/extensions-source) e sondagem em 29/10/2026.

## 4. Lojas e direitos autorais

- **Google Play** ([política de PI](https://support.google.com/googleplay/android-developer/answer/9888072)): proíbe apps com arte de quadrinhos sem direito e apps que induzem infração; remoção por notificação DMCA. Strike recai na **conta**, que publica os outros apps do Tiago.
- **Precedentes:** Tachiyomi (C&D da Kakao, jan/2024) e Kotatsu (encerrado em 2025), ambos fora da loja.
- **Sideload no Brasil:** desde 30/09/2026 o Android exige desenvolvedor verificado também fora da Play ([Next.ink](https://next.ink/brief-article/android-la-verification-des-developpeurs-tiers-debute-dans-quatre-pays/)); o APK pelo GitHub continua possível com a conta verificada.
- **App Store:** quem entra se apresenta como leitor de arquivos/servidor próprio (Paperback, Tachimanga).
- **Efeito no HQR:** 1.0 só pelo GitHub (C-03); fontes em módulos para uma loja futura (ADR-003); risco de DMCA no GitHub (R-01, ADR-016, RF-105).

## 5. UI

| Tema | Prática | Requisitos |
| :-- | :-- | :-- |
| Navegação | Mihon: Biblioteca · Novidades · Histórico · Explorar · Mais, com `NavigationSuiteScaffold`; no HQR, quatro destinos (sem Novidades, fora da 1.0) | RNF-12, ADR-012 |
| Biblioteca | Grade compacta/confortável ou lista; selos de não lidos e baixados; categorias em abas; filtros e ordem em folha; "Continuar" | RF-54, RF-55, RF-31 |
| Leitor | Imersivo, edge-to-edge, recorte da câmera; barra de páginas e ajustes rápidos sobrepostos; toque duplo e pinça; página dupla em paisagem e dobrável | RF-38 a RF-46 |
| Telas grandes | Classes de largura Compact/Medium/Expanded; lista-detalhe na obra; leitor em duas páginas no tablet | RF-44, RNF-12 |
| iOS | `TabView` no iPhone, barra lateral (`NavigationSplitView`) no iPad [NC: sem guia específica da HIG para leitores] | C-09 |
