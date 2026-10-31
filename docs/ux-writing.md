# UX writing

Vale para `shared/ui/.../values{,-pt,-es}/strings.xml` (pt-BR, en-US, es-419). Um texto por ideia, com o mesmo sentido nas três línguas.

**Voz e tom.** Direto, neutro e útil. Trate a pessoa por "você" / "you" / "tú". Sem "nós", sem gíria ("dá para", "não deu", "pôr", "tirar"), sem piada nem interjeição em erro. Presente do indicativo para o que o app faz; sem gerundismo ("vai estar baixando").

**Ações.** Botões, itens de menu e rótulos de acessibilidade começam pelo verbo no infinitivo (pt) ou imperativo (en, es): "Adicionar à biblioteca", "Add to library", "Agregar a la biblioteca". Curtos (até ~3 palavras) e com o objeto quando houver mais de um possível. Mensagens que pedem algo usam o imperativo: "Tente novamente", "Revisa tu conexión".

| Ação | en | pt-BR | es-419 |
|---|---|---|---|
| incluir numa coleção | Add | Adicionar | Agregar |
| tirar de uma coleção (reversível) | Remove | Remover | Quitar |
| apagar dados ou arquivos | Delete | Excluir | Eliminar |
| esvaziar (filtros, cache, seleção) | Clear | Limpar | Borrar |
| baixar | Download | Baixar | Descargar |
| repetir após falha | Try again | Tentar novamente | Reintentar |
| desfazer / cancelar / salvar | Undo / Cancel / Save | Desfazer / Cancelar / Salvar | Deshacer / Cancelar / Guardar |
| abrir / ler / continuar | Open / Read / Continue | Abrir / Ler / Continuar | Abrir / Leer / Continuar |
| marcar como lido | Mark as read | Marcar como lido | Marcar como leído |
| ligar / desligar recurso | Turn on / Turn off | Ativar / Desativar | Activar / Desactivar |
| atualizar / verificar atualizações | Update / Check for updates | Atualizar / Verificar atualizações | Actualizar / Buscar actualizaciones |
| backup | Backup | Backup | Copia de seguridad |

Termos fixos: obra, capítulo, fonte, servidor, ponte, biblioteca, feed, idiomas de conteúdo, Ajustes, aparelho (es: dispositivo, celular).

**Maiúsculas.** Sentence case em tudo (títulos, botões, abas, menus); maiúscula só no início e em nomes próprios e de seções do app (Biblioteca, Explorar, Ajustes). Caixa alta só em efeitos de quadrinho ("FIM!", "NOVO").

**Pontuação.** Rótulos, botões, títulos e frases soltas de uma linha sem ponto final; texto com mais de uma frase leva ponto. Aspas curvas “ ”. Reticências (…) só para processo em andamento ("Procurando versão nova…"). Sem "!" fora de comemoração. Dois-pontos antes de valor ou lista ("Falha: %1$s").

**Erros.** O que aconteceu + como resolver, sem culpar a pessoa: "Não foi possível atualizar os capítulos. Verifique a conexão e tente novamente." Padrões: pt "Não foi possível…", en "Couldn't…", es "No se pudo…". Se não há o que fazer, diga quando tentar ("Tente novamente mais tarde"). Erro de campo diz o que digitar ("Digite um nome").

**Estados vazios.** Título curto do estado + uma frase com o próximo passo e, se houver, o botão que o faz ("Seu feed está vazio" + "Adicione até 5 itens…").

**Confirmações e snackbars.** O título da caixa diz a ação ("Excluir todos os capítulos baixados?"); o botão repete o verbo, nunca "Sim". Snackbar no particípio com o objeto ("Obra removida da biblioteca") e ação "Desfazer".

**Números e plurais.** Algarismos ("5 itens"). Contagem variável vai em `<plurals>`; nada de "(s)". Nunca mudar ordem, quantidade ou tipo de `%1$s` / `%1$d`.

**Acessibilidade.** `contentDescription` / `accessibilityLabel` dizem ação + objeto ("Remover %1$s do histórico"), sem o tipo do controle ("botão", "ícone") e únicos numa lista.

Fontes: [Material Design — Writing](https://m1.material.io/style/writing.html) · [Material communication codelab](https://codelabs.developers.google.com/codelabs/material-communication-guidance) · [Apple HIG — Writing](https://developer.apple.com/design/human-interface-guidelines/writing) · [Apple HIG — Buttons](https://developer.apple.com/design/human-interface-guidelines/buttons) · [Apple HIG — VoiceOver](https://developer.apple.com/design/human-interface-guidelines/voiceover) · [Android — accessible apps](https://developer.android.com/guide/topics/ui/accessibility/apps) · [Google — error message tone](https://developers.google.com/tech-writing/error-messages/set-tone) · [Google — UI elements](https://developers.google.com/style/ui-elements) · [Microsoft pt-BR style guide](https://download.microsoft.com/download/8/e/3/8e349e32-9eb9-4b63-9909-7586b94a24dd/por-bra-StyleGuide.pdf) · [Microsoft es style guide](https://download.microsoft.com/download/4/3/1/431d9836-40dc-4f2b-aafc-def5c7b5d0f3/spa-neu-StyleGuide.pdf) · termos pt-BR/es-419 do Android (AOSP `frameworks/base/core/res`).
