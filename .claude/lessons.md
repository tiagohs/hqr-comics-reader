# Armadilhas e soluções

Cada item custou tempo uma vez. Problema → solução.

## Kotlin, Compose e testes

- **Propriedade declarada depois do `init`:** um `viewModelScope.launch` no `init` pode rodar antes de o construtor
  terminar (Compose usa `Main.immediate`). Uma `val` declarada depois do `init` ainda é `null` nesse momento.
  → Declare antes do `init` tudo que o `init` usa.
- **ViewModel que carrega só no `init`:** ele não vê o que mudou em outra tela (o feed ignorava uma busca salva
  adicionada pela fonte). → Observe a fonte de verdade (`observeFeed().distinctUntilChanged().collect { load() }`).
- **Testes de tela no CI:** uma máquina rápida expõe corridas que a lenta esconde, e uma lenta estoura o tempo.
  → Esperas de 10 s, ações por semântica (`SemanticsActions.OnClick`) quando a barra cobre o item, e
  `performScrollToNode` para itens fora da tela.
- **`.uppercase()` no texto:** quebra a busca dos testes. → A fonte Bangers já desenha em caixa alta.
- **detekt:**
  - `TooManyFunctions`: divida o arquivo.
  - `MatchingDeclarationName`: o arquivo leva o nome do objeto principal.
  - `ExplicitItLambdaParameter`: nomeie o parâmetro.
- **ktlintFormat:** às vezes remove um import ainda usado. → Recompile depois de formatar.
- **`drawBehind`:** desenha fora dos limites (as linhas de velocidade vazaram sobre o título). → `clipToBounds()`
  antes do desenho.
- **Animação barata:** leia o valor animado só na fase de desenho (`graphicsLayer {}` ou uma lambda no
  `drawBehind`), não na composição.

## Web

- **OPFS em duas abas:** o pool SAH do SQLite só abre numa aba; a segunda caía num banco vazio em memória e, com o
  cursor da sincronização no `localStorage` (compartilhado), recebia "nada mudou". → Estado que descreve o banco mora
  no banco (`sync_meta`), e o worker espera uma trava Web Locks antes de abrir o OPFS.

## Kotlin/Native e SwiftUI

- **Nomes que mudam no Swift:**
  - Classe aninhada vira nome colado (`SourceFilter.Option` → `SourceFilterOption`).
  - O companion vira `.Companion.shared` ou `SourceFilterCompanion.shared`.
  - Palavras reservadas de C/Swift ganham `_` (`AUTO` → `.auto_`, `DEFAULT` → `.default_`).
  - Nomes que começam com `new`, `copy`, `alloc` ou `init` ganham `the`/`do` (`NEW_CHAPTERS` → `.theNewChapters`, `copy()` → `doCopy`).
- **Nomes ambíguos:** `Category` e `LibraryItem` colidem com tipos do sistema. → Use `Shared.Category`.
- **Parâmetros com valor padrão:** o Swift não os enxerga, nem o `copy()` de data class.
  → Funções de edição no núcleo (`withSort`, `toggle`, `cycle`) ou uma função na ponte.
- **Fluxo anulável:** `StateFlow<T?>` não passa pelo `Observer<T : Any>`. → Mapeie para lista de zero ou um item.
- **Um ViewModel por tela:** o `init` de uma View SwiftUI roda várias vezes. → Crie o ViewModel dentro de um
  `ObservableObject` (`MangaModels`, `LibraryModels`) guardado em `@StateObject`.
- **`ToolbarContent`:** não se apaga com `AnyView`. → Use `@ToolbarContentBuilder` com `if/else`.
- **Animação com `Canvas`:** a View precisa ser `Animatable` (`animatableData`). Senão o valor salta.
- **Transição de zoom:** `matchedTransitionSource` e `navigationTransition(.zoom)` só existem no iOS 18.
  → Proteja com `#available`.

## Xcode, Mac e CI

- **`device_commit_files` com o mesmo nome:** pode entregar a versão antiga do arquivo.
  → Dê nome único a cada pacote (`hqr-sync-<hora>.tgz`).
- **`docs/` sem permissão no Mac:** ninguém compila os docs. → Tire-os do pacote ou ignore o erro.
- **Chamadas do Xcode MCP que passam de 60 s:** a operação continua rodando. → Acompanhe com `GetBuildLog` e leia o
  resultado no `.xcresult` (`Data` comprimido com zstd) em vez de repetir a chamada.
- **`project.pbxproj` reescrito pelo Xcode:** ao abrir, ele muda o arquivo só no formato. O projeto é gerado por
  script. → `git checkout --` no arquivo, depois feche e reabra o projeto.
- **Git no Mac por shell sem permissão de exclusão:** o git deixa `.lock` e falha. → Peça a permissão uma vez.
  Compare os arquivos soltos com o remoto antes de apagar.
- **Testes instáveis:** antes de mudar código, rode o teste sozinho algumas vezes. Se passa sozinho e falha sob
  carga, é tempo; se falha sempre, é bug, e o bug ganha teste.
