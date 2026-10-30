# Notas da estrutura: HQR

Como o repositório foi montado na fase 0 (implementation-plan §Fase 0) e o que vale saber para mexer nele.

## Base

- Kotlin 2.4, AGP 9.4, Gradle 9.8; compileSdk/targetSdk 37, minSdk 26, JVM 17. Versões em `gradle/libs.versions.toml`.
- `build-logic/`: plugins de convenção `hqr.kmp-library` (Android, JVM e wasmJs; iOS só com `-Phqr.ios=true`),
  `hqr.compose-library` e `hqr.android-app`. Kover ignora código gerado (Room, serialização, recursos).
- Alvo JVM em todos os módulos compartilhados: os testes rodam rápido, com o SQLite de verdade (bundled) e as telas
  desenhadas pelo Compose Desktop.

## Decisões de implementação

- Banco: Room 3 (`androidx.room3`) em Android, JVM e web; na web, `WebWorkerSQLiteDriver` com um worker próprio
  (`web/src/wasmJsMain/resources/sqlite-worker.js`, SQLite oficial em WASM, OPFS SAH pool).
- Preferências: multiplatform-settings (SharedPreferences no Android, localStorage na web) no lugar do DataStore, que
  não roda na web (ADR-004 mantém o papel).
- Indicador de carregamento: `BurstIndicator` próprio, porque o Material 3 Expressive do Compose Multiplatform ainda
  não expõe o `LoadingIndicator`.
- Cor da capa (RF-98): `CoverColor` com o quantizador e o score do Material (materialkolor); o teste confere os valores
  do canvas.
- Gravação do progresso do leitor: o atraso de 500 ms é cancelável, a escrita no banco não (`NonCancellable`).

## Testes

- `commonTest`/`jvmTest` por módulo; integração com Room em memória (`inMemoryDatabase()`).
- Telas: `shared/ui/src/jvmTest` sobe o app inteiro (`HqrApp`) com Koin, banco em memória e uma fonte de demonstração
  (`DemoSource`, capas desenhadas, sem rede), navega e salva capturas em `shared/ui/build/screenshots/`.
- Testes de tela usam um `Dispatchers.Main` de teste (o teste dispara o toque na própria thread).

## Rede no ambiente da nuvem

- O Maven Central limita pedidos: `~/.gradle/init.d/mirror.gradle` põe o espelho do Google na frente.
- O npm do Kotlin precisa de um pacote do GitHub (codeload) bloqueado aqui; a distribuição web sai no CI do Mac.
