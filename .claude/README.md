# .claude: memória do projeto

O que construímos no HQR e como, para retomar depois ou copiar para outro projeto. As regras do dia a dia ficam em
`AGENTS.md` (o `CLAUDE.md` aponta para ele); os requisitos e decisões, em `docs/`.

| Arquivo | Para quê | Serve a outros projetos? |
| :-- | :-- | :-- |
| `project.md` | O HQR em uma página: produto, arquitetura, linguagem visual, estado e próximos passos | Não (específico) |
| `workflow.md` | O processo inteiro, da ideia à release: requisitos, design aprovado, testes antes, Android e web, iOS, CI no Mac | Sim: é o modelo |
| `lessons.md` | Armadilhas técnicas que custaram tempo (KMP, Compose, SwiftUI, Xcode, CI) e a solução | Sim |
| `skills/ios-on-mac/` | Skill: levar as mudanças ao Mac, compilar e testar o iOS pelo Xcode | Sim, trocando os caminhos |

Para começar outro app no mesmo molde: copie `workflow.md`, `lessons.md` e `skills/`, troque nomes e caminhos, e
escreva um `AGENTS.md` no formato do deste repositório.
