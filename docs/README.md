# Documentação: HQR

Documento de produto (para iterar): https://claude.ai/artifact/QTtDpo8C4mxXV3mmw4Uos4 · Telas: canvas "HQR — design" (https://claude.ai/artifact/2rAfx3MR9AKw7x4fmwTTjr). Mudou requisito, tela ou decisão: atualizar o documento de produto e `docs/` juntos.

Leia nesta ordem. Cada documento cita os IDs do anterior.

| Documento | Conteúdo |
| :-- | :-- |
| [benchmark.md](benchmark.md) | Leitores existentes, lógica do Mihon/TachiyomiSY, MangaDex e HQ Now, lojas e direitos autorais, padrões de UI |
| [srs.md](srs.md) | Escopo, distribuição pelo GitHub, restrições, 114 requisitos funcionais (RF), 33 não funcionais (RNF), marcos 1.0 e Web, atualização técnica (Apêndice C) |
| [use-cases.md](use-cases.md) | 14 casos de uso (UC) e diagrama |
| [architecture.md](architecture.md) | Contexto, containers e módulos, execução, dados, segurança, distribuição, riscos e 23 decisões (ADR) |
| [ux-foundations.md](ux-foundations.md) | Princípios, navegação, 29 telas (SCR) e variações por superfície (Android, web, iPhone, iPad), fluxos, tamanhos de tela, leitor, estados, acessibilidade |
| [ux-writing.md](ux-writing.md) | Guia de textos de tela: voz, glossário de verbos (pt/en/es), maiúsculas, erros, estados vazios, acessibilidade |
| [design.md](design.md) e [tokens.json](tokens.json) | Marca, cores (violeta base e cor da capa), tipografia, formas, componentes |
| [implementation-plan.md](implementation-plan.md) | Fases, FEATs e o que entra em cada versão |
| [scaffold-notes.md](scaffold-notes.md) | Como a estrutura do código foi montada e decisões de implementação |
| [rtm.md](rtm.md) | Rastreabilidade RF/RNF → UC → componente/ADR → tela (gerada por `scripts/docs/build_rtm.py`) |

