# HQR em uma página

Leitor de HQs e mangás online e offline, reescrito do zero em 2026 (repo `tiagohs/hqr-comics-reader`, branch
`rewrite/v1`). Grátis, sem anúncio, distribuído por APK no GitHub (sem loja por enquanto).

## Fontes da verdade

- Documento de produto (Claude Docs): https://claude.ai/artifact/QTtDpo8C4mxXV3mmw4Uos4
- Canvas de design "HQR — design" (68 pranchas: celular, tablet, iPhone, iPad, web, identidade, fluxo animado):
  https://claude.ai/artifact/2rAfx3MR9AKw7x4fmwTTjr
- `docs/`: `srs.md` (RF-01 a RF-118, RNF), `use-cases.md`, `architecture.md` (C4 e ADR-001 a ADR-026),
  `ux-foundations.md` (telas SCR), `design.md` e `tokens.json`, `implementation-plan.md` (FEAT-001 a FEAT-069),
  `rtm.md` (gerado por `scripts/docs/build_rtm.py`).

## Arquitetura

- Kotlin Multiplatform: `shared/domain` (regras puras) · `shared/data` (Room 3, Ktor, cache, preferências) ·
  `shared/presentation` (ViewModels) · `shared/app` (Koin; framework `Shared` do iOS) · `shared/ui` (Compose
  Multiplatform: Android, tablet e web com o mesmo código) · `sources/*` (uma fonte por módulo).
- Android (`android/`), web wasmJs (`web/`, SQLite em OPFS, ponte em `bridge-worker/` para as fontes online) e
  iOS/iPadOS em SwiftUI (`ios/`) sobre os mesmos ViewModels.
- Regra que mais rendeu: toda regra de negócio no núcleo, com teste; a tela só desenha. Quando o iOS precisou de algo
  que o Android fazia na tela (ordem da biblioteca, filtros da fonte, corte de bordas), a regra subiu para o núcleo e
  as duas plataformas passaram a usá-la.

## Linguagem visual (ADR-026, RF-118)

- Controles Material 3 / HIG intactos; o "HQ" mora em volta: papel `#F6F1E7` e tinta `#16161A`, destaque violeta ou
  tirado da capa (RF-98), Bangers nos títulos, título original vertical em Dela Gothic One (recorte japonês),
  retícula, linhas de velocidade finas só nas bordas, selos "NOVO!", explosão "OPS!", capas com moldura de nanquim,
  esqueleto suave no carregamento.
- Movimento (RF-118), nativo e sem Lottie: capa voa até a obra (elemento compartilhado; zoom no iOS 18+), linhas
  entram de fora para dentro, título vertical se escreve, carimbo com mola, cascata só ao abrir, primeiro acesso
  desenhado a traço; "reduzir movimento" vira esmaecimento. Regras de tempo em `ComicMotion` (Kotlin e Swift) com
  testes.
- Referência de movimento: "Manga Reader App Concept" (Olga Nikolaeva, Dribbble).

## Comportamentos que definem o app

- Obra abre na hora e carrega detalhes e capítulos em paralelo; validade de 6 h (RF-117).
- Fontes favoritas viram o feed; feed guardado por 30 min (RF-115, RF-116).
- Onboarding único: modo de leitura e idiomas (RF-81).

## Estado em 1º de novembro de 2026

- Android, tablet e web: 1.0 e 1.1 feitos; CI verde; APK e web no rascunho `ci-rewrite-v1` a cada push.
- iOS: no nível do Android (FEAT-060 a FEAT-064, FEAT-068); 15 testes XCTest. Falta ver as animações no simulador.
- Fora do iOS por serem do Android: cor dinâmica, preto puro, teclas de volume, atualizador do APK, ponte da web.
- Próximo: Tiago testar no aparelho; publicar `v1.0.0` (`scripts/release/prepare_release.py --publish`) só com o OK
  dele; conta Apple própria antes de distribuir o iOS.
