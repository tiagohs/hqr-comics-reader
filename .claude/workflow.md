# Como trabalhamos (modelo para outros apps)

O fluxo que levou o HQR de uma ideia a três plataformas. Cada etapa tem uma saída que o Tiago aprova antes da
seguinte.

## 1. Requisitos e arquitetura (plugin Agentic SDLC Kit)

1. **Benchmark curto** dos concorrentes, só como anexo ligado aos IDs dos requisitos.
2. **`requirements-engineering`** → `docs/srs.md`:
   - histórias "Eu, como usuário, quero X para Y";
   - RF numerados com prioridade (Must/Should/Could) e aceite Dado/Quando/Então;
   - RNF mensuráveis.
3. **`software-architecture`** → `docs/architecture.md` (C4, módulos, modelo de dados, um ADR por decisão).
4. **`ux-foundations`** → `docs/ux-foundations.md` (telas SCR, estados, mensagens), `docs/design.md` e `docs/tokens.json`.
5. **`implementation-planning`** → `docs/implementation-plan.md`: épicos, FEATs, esqueleto andante e marcos por prioridade.
6. **`docs/rtm.md`** gerado por script, nunca à mão.

Docs enxutos e sem seção "o que mudou". Um documento de produto no Claude Docs espelha os `docs/` para o Tiago
comentar; requisito, tela ou decisão que muda atualiza os dois no mesmo commit.

## 2. Design antes de código

- **Canvas de design** (artifact) com todas as telas e estados (vazio, carregando, erro, offline), em celular,
  tablet, iPhone, iPad e web.
- **Geradores em Python** montam as pranchas: refazer é rodar o script, não editar à mão.
- **Comentários do Tiago no canvas** viram mudanças. Republicar sempre por cima da versão viva, mesclando as edições
  dele, nunca sobrescrevendo.
- **Nenhuma tela nova é implementada sem ele ver o design antes.**
- **Equilíbrio** que funcionou: controles nativos (Material/HIG) e a identidade nos fundos, títulos, selos e
  movimento. Botões "muito gráficos" se afastam da plataforma.

## 3. Implementação

- **Testes antes do código**, com o nome começando pelo ID (`RF117_…`, `FEAT001_…`; Swift `test_RF81_…`). Todo bug
  ganha teste.
- **Código, pastas e comentários em inglês**; respostas e docs em português.
- **Textos de tela em três línguas** (en, pt, es), adicionados por script, com uma checagem que barra texto no código.
  O catálogo do iOS é gerado do mesmo `strings.xml`.
- **Cores e medidas só de `tokens.json`**, gerados para Kotlin e Swift.
- **Ordem:** Android e web primeiro (Compose Multiplatform, um código), depois iOS em SwiftUI sobre os mesmos
  ViewModels, com uma ponte (`IosBridge`/`SharedGraph`) para o que o Swift não enxerga bem.
- **SwiftUI:** toda view com `#Preview` claro e escuro (checado por script).
- **Antes de dizer que terminou:** testes, ktlint, detekt, lint, strings, build release e checagem do APK. A lista
  exata fica no `AGENTS.md`.

## 4. Nuvem, Mac e CI (sem custo no GitHub)

- **Claude trabalha numa nuvem Linux:**
  - Gradle com `--max-workers=1`, compilador in-process e `-Xmx2560m`.
  - Sem iOS lá (`-Phqr.ios=false`).
  - Sem o npm do GitHub, então a web completa só no CI.
- **CI só no Mac do Tiago** (runner self-hosted, minutos grátis):
  - Roda a cada push na branch de trabalho.
  - Nada de artifact do Actions; APK e web vão para uma release rascunho `ci-<branch>`.
  - Workflows novos ou disparos manuais só se ele pedir.
- **iOS:** as mudanças vão ao Mac por pacote `tar` e o Xcode compila e testa pelo MCP. Veja a skill
  `skills/ios-on-mac`.
- **Commit e push só na branch de trabalho**, com os testes verdes. Nunca na `master`.
- **Publicação:** só com o OK do Tiago.

## 5. Segurança

- Nenhum token, chave ou senha no código: só no `local.properties` e nos secrets do GitHub.
- A keystore vazada da versão antiga nunca é usada. A nova é criada por script e fica fora do git.
