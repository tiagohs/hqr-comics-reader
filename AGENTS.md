# HQR: instruções para agentes

> Documento de produto (fonte oficial): https://claude.ai/artifact/QTtDpo8C4mxXV3mmw4Uos4 ·
> Design: canvas "HQR — design" (https://claude.ai/artifact/2rAfx3MR9AKw7x4fmwTTjr).

## Documentos

`docs/README.md` é o índice: `benchmark.md`, `srs.md` (RF e RNF), `use-cases.md`, `architecture.md` (C4 e ADRs),
`ux-foundations.md` (telas SCR, estados, mensagens), `design.md` e `tokens.json` (design system),
`implementation-plan.md` (fases e FEATs), `rtm.md` (rastreabilidade, `scripts/docs/build_rtm.py`), `scaffold-notes.md`
(como a estrutura foi montada). Mudou requisito, tela ou decisão: atualizar o documento de produto e `docs/` no mesmo
commit.

## Regras

- Responder em português; **código em inglês** (pastas, classes, funções, variáveis, comentários, testes).
- Docs enxutos, sem seção de "o que mudou".
- Commit e push só na branch de trabalho (hoje `rewrite/v1`), com os testes verdes; nunca na `master`.
- Nenhum token, chave ou senha no código: `local.properties` e os secrets do GitHub.
- **CI no Mac do Tiago** (`.github/workflows/ci.yml`, runner `[self-hosted, macOS]`, sem custo): roda a cada push em
  `rewrite/**` e à mão. Nada de runner hospedado nem artifact do Actions; APKs e web vão para a release rascunho
  `ci-<branch>`, a web para a branch `gh-pages`.
- Cores e medidas de `docs/tokens.json`; o canvas é a referência de aparência e textos, o comportamento segue os
  requisitos.
- Textos de tela em `shared/ui/src/commonMain/composeResources/values{,-pt,-es}/strings.xml`, sempre nas três línguas:
  `python3 scripts/strings/add_strings.py` (JSON `{chave: [en, pt, es]}` pela entrada) e
  `python3 scripts/strings/check_strings.py`.
- Testes antes do código; o nome do teste começa pelo ID que cobre (`RF38_…`, `FEAT001_…`, `RNF23_…`). Todo bug ganha
  um teste.
- `shared/domain` não conhece rede, banco nem plataforma; o que muda por plataforma entra por interface
  (architecture §3). Fontes novas implementam `CatalogSource`/`SearchSource`/`ImageSource` em `sources/<nome>`.
- Telas compartilhadas (Compose Multiplatform) em `shared/ui`: o mesmo código no Android, tablet e web.
- Chave de release (C-11): `scripts/release/setup_signing.sh` cria `keystore/hqr-release.jks`, grava a senha no
  `local.properties` e os secrets do GitHub; a chave da 0.0.5 (`keystore.jks`, histórico público) nunca é usada.
- Release (RF-84): `python3 scripts/release/prepare_release.py` monta o rascunho `v<versão>` com o APK do CI (assinado
  com a chave do app) primeiro e o `.sha256` depois; notas em `scripts/release/notes/`. Publicar (`--publish`) só
  com o OK do Tiago.

## Módulos

`shared/domain` (modelos, regras, contratos) · `shared/data` (Room 3, Ktor, cache, preferências) ·
`shared/presentation` (ViewModels, cor da capa, leitor) · `shared/ui` (telas, tema, navegação, Koin) ·
`sources/local` (pasta, CBZ, PDF) · `android` (app) · `web` (wasmJs, SQLite em OPFS).

## Antes de dizer que terminou

```bash
./gradlew check detekt          # testes JVM e Android, telas (Compose no JVM), Kover, ktlint, detekt, lint
python3 scripts/strings/check_strings.py
./gradlew :android:assembleRelease :web:wasmJsBrowserDistribution
python3 scripts/release/check_apk.py android/build/outputs/apk/release/android-universal-release.apk
```

Nesta nuvem: `-Phqr.ios=false --max-workers=1 -Pkotlin.compiler.execution.strategy=in-process
"-Dorg.gradle.jvmargs=-Xmx2560m"`, módulo por módulo se a memória faltar, e `-x :kotlinWasmToolingSetup` (o
GitHub de pacotes npm é bloqueado aqui: a web completa só no CI). Capturas das telas: `./gradlew :shared:ui:jvmTest`,
em `shared/ui/build/screenshots/`, conferindo com o canvas.
