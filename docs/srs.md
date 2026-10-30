# Especificação de Requisitos de Software: HQR

> Versão 1.0 · 2026-10-29 · Autor: Tiago Silva (com Claude)
> Sintaxe: enunciado EARS ("deve", "quando", "enquanto", "se… então") + critério Dado/Quando/Então

## 1. Introdução

### 1.1 Propósito

Define o que o HQR 1.0 faz, para quem e com que qualidade. É a fonte de `docs/architecture.md`, `docs/ux-foundations.md` e `docs/implementation-plan.md`.

### 1.2 Visão e escopo

Leitor gratuito e sem anúncios de HQs e mangás. O conteúdo vem de fontes embutidas no app, dos arquivos do próprio usuário e do servidor do usuário. O app não hospeda, modifica nem redistribui conteúdo. A lógica de fontes, leitor, biblioteca, downloads e backup se inspira no Mihon/TachiyomiSY. Interface e marca são próprias, em Material 3 no Android e seguindo a HIG no iOS. Não há servidor próprio.

- **Distribuição da 1.0:** um app só, por APK no GitHub Releases, com atualizador próprio. Sem publicação em loja na 1.0.
- **Fontes:** MangaDex, HQ Now, Arquivos locais e Servidores (OPDS: Komga, Kavita e outros), e as adicionais da 1.1: Internet Archive (domínio público), NamiComi, MangaFlix, Yomu Mangas, Weeb Central e XOXO Comics (§3.1.18).
- **Loja (talvez, depois):** a separação das fontes em módulos (ADR-003) permite um sabor de loja sem fontes de terceiros, sem refazer o app.
- **Plataformas:** Android primeiro (celular, tablet, dobrável); depois a web (navegador do computador, instalável como PWA, §3.1.19) e iPhone e iPad, com o mesmo núcleo.

**Na 1.0:** fontes, explorar, busca global e por fonte, buscas salvas e feed, detalhes, leitor, biblioteca com categorias, histórico, downloads offline, arquivos locais, servidores OPDS, juntar e migrar obras, backup e restauração, ajustes, Sobre e privacidade, e o atualizador.

**Marco 1.0:** todos os Must (60). Should entram na 1.1 e Could depois, salvo decisão em contrário.

**Marco Web:** os requisitos de §3.1.19, depois do 1.0 do Android.

**Fora do escopo:** conta, login e sincronização na nuvem; servidor próprio (a ponte da web é do próprio usuário, C-12); anúncios, compras e assinatura; extensões instaláveis e repositórios de fontes; verificação de capítulos novos em segundo plano e notificações de capítulo novo; trackers (AniList, MyAnimeList, Kitsu); edição de metadados; recomendações; comentários e recursos sociais; tradução de páginas; trazer a biblioteca da 0.0.5.

### 1.3 Metas de produto

| Meta | Métrica | Alvo |
| :-- | :-- | :-- |
| Adoção | Downloads do APK por versão no GitHub Releases | crescente a cada versão |
| Estabilidade em uso | Usuários sem falha (Crashlytics, 28 dias) | ≥ 99,5% |
| Continuidade | Repositório de distribuição fora do ar por DMCA | 0 |

As metas de estabilidade e desempenho estão nos RNF-01 a RNF-08.

### 1.4 Referências

- Benchmark: `docs/benchmark.md`
- MangaDex API e política de uso: https://api.mangadex.org/docs/
- Política de PI do Google Play: https://support.google.com/googleplay/android-developer/answer/9888072
- Mihon (Apache-2.0): https://github.com/mihonapp/mihon · TachiyomiSY: https://github.com/jobobby04/TachiyomiSY
- OPDS 1.2: https://specs.opds.io/opds-1.2 · Page Streaming Extension: https://github.com/anansi-project/opds-pse

## 2. Descrição geral

### 2.1 Perspectiva do produto

O app roda só no aparelho. Ele conversa com a API do MangaDex, com o GraphQL do HQ Now, com os servidores OPDS que o usuário cadastrar, com o GitHub Releases (atualizador) e com o Firebase Crashlytics (Android). Ele substitui o HQR 0.0.5, que saiu em 2019 e era distribuído pelo GitHub. As duas fontes da 0.0.5 (HQUltimate e ReadComicsOnline) estão fora do ar. A troca técnica está no Apêndice C. Na web, o app roda no navegador e fala com as fontes online pela ponte do próprio usuário (§3.1.19).

### 2.2 Classes de usuário

| Classe | Contexto | Precisa de |
| :-- | :-- | :-- |
| P1 Leitor de mangá | Acompanha séries no MangaDex, no celular, em vários idiomas | Busca, leitura D→E e webtoon, retomar, biblioteca, downloads para o transporte |
| P2 Leitor de HQ ocidental | Lê Marvel, DC e independentes em português; tem CBZ e CBR no aparelho ou usa o HQ Now (APK) | Arquivos locais, leitura E→D, página dupla no tablet |
| P3 Colecionador com servidor | Mantém a coleção no Komga ou Kavita | OPDS com login, streaming de páginas, offline |
| P4 Pessoa com deficiência (transversal) | Usa leitor de tela, fonte grande, teclado ou controle | Navegação acessível fora das páginas e leitor operável sem gestos finos |

### 2.3 Ambiente de operação

Android 8.0 (API 26) ou superior; iOS e iPadOS 17 ou superior. Celulares, tablets, dobráveis, janelas redimensionáveis, teclado, mouse e controle. Web: navegadores de RNF-31 no computador; em janela estreita, o layout de celular. Sem internet, funcionam a biblioteca, o histórico, os capítulos baixados e os arquivos locais.

### 2.4 Restrições

| ID | Restrição |
| :-- | :-- |
| C-01 | Nenhum servidor próprio e nenhum custo recorrente. |
| C-02 | App gratuito, sem anúncios, compras nem assinatura (política do MangaDex); doação é permitida. |
| C-03 | A 1.0 é distribuída só pelo GitHub (APK). Cada fonte de terceiros fica num módulo próprio, para que um sabor de loja, se vier, compile sem elas. A distribuição do iOS está em aberto (Q-09). |
| C-04 | O app não hospeda, altera nem redistribui conteúdo, e credita as fontes e os grupos de scan. |
| C-05 | Mesma organização e tecnologia do Script Reader: `android/`, `shared/` (KMP: `domain`, `data`, `presentation`) e `ios/` depois; Kotlin, Compose e Material 3, Koin, Room, Ktor, Coil 3, Ksoup e o mesmo catálogo de versões. |
| C-06 | A lógica segue o desenho do Mihon/TachiyomiSY (Apache-2.0). Todo trecho de código derivado deles mantém o aviso de licença, e o Sobre traz o NOTICE. |
| C-07 | Código, pastas e comentários em inglês; documentos em português. |
| C-08 | GitHub sem custo: CI só no runner do Mac do Tiago (sem minutos pagos) ou por disparo manual dentro da cota gratuita. |
| C-09 | Android primeiro. O iOS só começa depois que o Tiago aprovar as telas do Android. |
| C-10 | Desenvolvedor solo. |
| C-11 | Nova chave de assinatura e `applicationId` `com.tiagohs.hqr.reader`, registrados na verificação de desenvolvedor do Android; a chave da 0.0.5 nunca é registrada nem usada (ela está no histórico público). A 0.0.5 segue `com.tiagohs.hqr`, e os dois instalam lado a lado. |
| C-12 | A web é hospedada de graça no GitHub Pages, publicada pelo CI do Mac do Tiago (branch `gh-pages`). O projeto não opera ponte nem proxy: cada usuário publica a sua no plano grátis do Cloudflare Workers. |

### 2.5 Premissas e dependências

| ID | Premissa |
| :-- | :-- |
| A-01 | A API v5 e a política de uso do MangaDex seguem como em out/2026. |
| A-02 | O GraphQL do HQ Now (`admin.hq-now.com/graphql`) não é documentado e pode mudar sem aviso; o app aceita essa instabilidade (RF-05). |
| A-03 | O Tiago aceita o risco jurídico do APK com MangaDex e HQ Now. Esse sabor é assinado pela conta de desenvolvedor verificada, que o Android exige no Brasil desde 30/09/2026. |
| A-04 | Komga e Kavita expõem OPDS com o Page Streaming Extension (OPDS-PSE). |

## 3. Requisitos específicos

Prioridade: Must (lançamento), Should (importante), Could (desejável).

### 3.1 Requisitos funcionais

#### 3.1.1 Fontes

- **HU-01** Eu, como usuário, quero ler de várias fontes no mesmo app para não depender de um site só.

| ID | Requisito | Prior. | Critério de aceite |
| :-- | :-- | :-- | :-- |
| RF-01 | O núcleo deve oferecer um contrato único de fonte, com populares, recentes, busca com filtros, detalhes, capítulos e páginas. Explorar, busca, detalhes e leitor só conhecem esse contrato. | Must | Dado o MangaDex, um CBZ local e um servidor OPDS, quando o usuário abre a obra e lê um capítulo de cada um, então as mesmas telas atendem os três, sem código de tela específico por fonte. |
| RF-02 | O app deve trazer embutidas as fontes MangaDex, HQ Now, Arquivos locais e Servidores, cada uma num módulo próprio. | Must | Dado o APK da loja, quando a lista de fontes abre, então o HQ Now não aparece, e a busca por `hq-now` no APK não encontra nada (RNF-16). |
| RF-03 | O app deve listar as fontes por idioma e permitir ligar e desligar cada uma. Fontes desligadas saem de Explorar, da busca global e do feed. | Should | Dado o HQ Now desligado, quando o usuário faz uma busca global, então o HQ Now não é consultado. |
| RF-04 | O app deve respeitar o limite de pedidos de cada fonte: MangaDex até 5 por segundo e o servidor de imagens (at-home) até 40 por minuto; HQ Now até 1 a cada 2 segundos. Se a resposta for 429, então o app deve esperar o tempo de `Retry-After` (ou 60 s) antes de tentar de novo. | Must | Dado o download de 10 capítulos do MangaDex, quando a fila roda, então nenhum segundo tem mais de 5 pedidos à API e nenhuma resposta 429 fica sem espera. |
| RF-05 | Se uma fonte falhar (sem rede, erro 5xx, resposta em formato inesperado), então o app deve mostrar a falha só naquela fonte, com "Tentar de novo" e, quando houver página web, "Abrir no navegador", sem apagar nada da biblioteca. | Must | Dado o HQ Now fora do ar, quando o usuário abre uma obra dele que está na biblioteca, então os capítulos lidos e baixados continuam lá e aparece o aviso da fonte. |
| RF-06 | O app deve oferecer ajustes por fonte. No MangaDex: idiomas dos capítulos, qualidade das imagens (original ou econômica) e a classificação de conteúdo (RF-08). | Should | Dado só "pt-br" marcado, quando uma obra do MangaDex abre, então só capítulos em português aparecem. |

#### 3.1.2 MangaDex

- **HU-02** Eu, como usuário, quero ler mangás do MangaDex no idioma que escolho para acompanhar minhas séries.

| ID | Requisito | Prior. | Critério de aceite |
| :-- | :-- | :-- | :-- |
| RF-07 | O app deve creditar o MangaDex na obra e no Sobre, e o grupo de scan em cada capítulo, com o nome tocável que abre a página do grupo. | Must | Dado um capítulo do grupo "X Scans", quando a lista de capítulos aparece, então "X Scans" está na linha do capítulo. |
| RF-08 | O app deve pedir ao MangaDex conteúdo "safe" por padrão; "suggestive" só liga nos ajustes da fonte depois da confirmação de 18 anos ou mais; "erotica" e "pornographic" nunca são pedidos. | Must | Dado o padrão, quando qualquer pedido de lista ou busca é feito, então o parâmetro `contentRating[]` tem só `safe`. |
| RF-09 | Ao abrir um capítulo, o app deve pedir o servidor de imagens (`/at-home/server/{id}`) e pedir de novo aos 12 minutos de uso, ao receber 403 ou quando uma imagem falhar. Para cada imagem servida fora de `mangadex.org`, o app deve enviar o relatório at-home (sucesso, cache, bytes e duração). | Must | Dado um capítulo aberto há 16 minutos, quando a próxima página é pedida, então o app pede um novo servidor antes e a página carrega. |
| RF-10 | O app deve se identificar com o User-Agent `HQR/<versão> (<site>)` em todo pedido ao MangaDex. | Must | Dado o app instalado, quando qualquer pedido é feito ao MangaDex, então o cabeçalho User-Agent segue esse formato. |
| RF-11 | Quando um capítulo do MangaDex só existir em site externo (`externalUrl`), o app deve marcá-lo como "Externo" e abri-lo no navegador. | Should | Dado um capítulo licenciado com `externalUrl`, quando tocado, então o navegador abre esse endereço. |

#### 3.1.3 HQ Now

- **HU-03** Eu, como usuário, quero achar as HQs do HQ Now no app para ler com o leitor dele.

| ID | Requisito | Prior. | Critério de aceite |
| :-- | :-- | :-- | :-- |
| RF-12 | O app deve oferecer no HQ Now populares, atualizadas recentemente, busca por nome, lista por editora, detalhes com capítulos e páginas do capítulo. | Must | Dado o app instalado, quando o usuário busca "Batman" no HQ Now e abre o primeiro resultado, então os capítulos aparecem e o primeiro abre no leitor. |

#### 3.1.4 Arquivos locais

- **HU-04** Eu, como usuário, quero ler os CBZ, CBR e PDFs que já tenho para ter tudo num leitor só.

| ID | Requisito | Prior. | Critério de aceite |
| :-- | :-- | :-- | :-- |
| RF-13 | O app deve permitir escolher uma pasta (seletor do sistema) e mostrar como obras as subpastas e os arquivos dela. Formatos: CBZ/ZIP, pasta de imagens e PDF (Must, PDF pelo renderizador do sistema); CBR/RAR e CB7 (Should, só no Android); EPUB de quadrinhos (Could). | Must | Dado uma pasta com `Saga/` contendo `Saga 01.cbz` e `Saga 02.cbz`, quando o usuário escolhe a pasta, então aparece a obra "Saga" com 2 capítulos. |
| RF-14 | O app deve ler o `ComicInfo.xml` quando existir (título, série, número, autores, sinopse). Sem ele, usa o nome do arquivo ou da pasta, e a primeira imagem como capa. | Should | Dado um CBZ com `ComicInfo.xml` de série "Saga" e número 1, quando a pasta é lida, então o capítulo aparece como "Saga #1". |
| RF-15 | Quando o usuário puxar para atualizar a fonte local ou abrir uma obra local, o app deve reler a pasta e refletir arquivos novos e removidos. | Should | Dado um CBZ copiado para a pasta, quando o usuário puxa para atualizar, então o novo capítulo aparece. |
| RF-16 | O app só deve ler os arquivos locais; nunca alterar, mover, copiar ou apagar esses arquivos. | Must | Dado uma pasta local, quando o usuário lê, baixa e apaga obras dela no app, então a data de modificação dos arquivos não muda. |

#### 3.1.5 Servidores (OPDS, Komga, Kavita)

- **HU-05** Eu, como usuário, quero conectar meu Komga ou Kavita para ler minha coleção no celular.

| ID | Requisito | Prior. | Critério de aceite |
| :-- | :-- | :-- | :-- |
| RF-17 | O app deve permitir cadastrar servidores OPDS 1.2 (OPDS 2.0: Could) com nome, endereço e, se precisar, usuário e senha. A senha fica no armazenamento seguro do sistema. | Must | Dado um Komga com login, quando o usuário salva o servidor, então a senha não aparece no banco nem no backup (RF-72). |
| RF-18 | O app deve navegar pelos catálogos do servidor, buscar pelo OpenSearch quando o servidor oferecer, e ler as páginas por streaming (OPDS-PSE) ou, sem ele, baixando o arquivo do capítulo. | Must | Dado um Kavita com OPDS-PSE, quando um volume abre, então a primeira página aparece sem baixar o arquivo inteiro. |
| RF-19 | Ao salvar ou testar um servidor, o app deve conferir a conexão e dizer a causa da falha: endereço inalcançável, login recusado (401), certificado inválido ou resposta que não é OPDS. | Must | Dado a senha errada, quando o usuário toca "Testar", então aparece "Usuário ou senha incorretos". |
| RF-20 | Se o servidor for Komga ou Kavita, o app deve enviar ao servidor o progresso de leitura, pela API própria de cada um. | Could | Dado um volume lido até a página 12 no app, quando o usuário abre o Komga na web, então o progresso é 12. |

#### 3.1.6 Explorar, busca e feed

- **HU-06** Eu, como usuário, quero buscar uma obra em todas as fontes de uma vez para achar onde ela está.
- **HU-07** Eu, como usuário, quero salvar buscas e ver um feed das minhas fontes para descobrir o que saiu.

| ID | Requisito | Prior. | Critério de aceite |
| :-- | :-- | :-- | :-- |
| RF-21 | O app deve mostrar, por fonte, as obras populares e as recentes, com carregamento contínuo ao chegar no fim. | Must | Dado "Populares" do MangaDex, quando o usuário chega ao fim, então a próxima página entra na lista sem tela de carregamento cheia. |
| RF-22 | O app deve buscar numa fonte com os filtros que ela oferece (no MangaDex: tags incluídas e excluídas, status, demografia, idioma original, ano e ordem). | Must | Dado a tag "Romance" incluída e "Horror" excluída, quando a busca roda, então nenhum resultado tem "Horror". |
| RF-23 | Quando o usuário parar de digitar por 400 ms na busca global, o app deve consultar em paralelo todas as fontes ligadas e mostrar os resultados agrupados por fonte. Cada grupo tem estado próprio (carregando, erro, vazio), e uma fonte lenta não segura as outras. | Must | Dado o HQ Now fora do ar, quando o usuário busca "one piece", então os resultados do MangaDex aparecem e o grupo do HQ Now mostra o erro com "Tentar de novo". |
| RF-24 | O app deve permitir salvar uma busca (termo e filtros) por fonte, aplicá-la com um toque, renomeá-la e apagá-la. | Should | Dado a busca salva "Romance concluído", quando tocada, então termo e filtros voltam e a lista é refeita. |
| RF-25 | O app deve montar um feed com até 5 itens escolhidos entre "recentes de uma fonte" e buscas salvas, e mostrar as obras atualizadas mais recentes de todos eles numa lista só; obras sem data vão para o fim. O feed é atualizado ao abrir e ao puxar, nunca em segundo plano. | Should | Dado o feed com "Recentes MangaDex" e "Romance concluído", quando aberto, então as obras dos dois aparecem juntas, das mais recentes para as mais antigas, cada uma com a etiqueta da origem. |
| RF-26 | O app deve marcar "Na biblioteca" nas obras que já estão nela, em toda lista de resultados. | Should | Dado "Frieren" na biblioteca, quando aparece numa busca, então a capa tem o selo. |
| RF-27 | O app deve guardar as 10 últimas buscas globais e permitir apagá-las. | Could | Dado 3 buscas feitas, quando o campo abre vazio, então as 3 aparecem, cada uma com "remover". |
| RF-28 | Se a busca não tiver resultado em nenhuma fonte, então o app deve mostrar o termo buscado e sugerir conferir as fontes ligadas. | Must | Dado nenhuma fonte com resultado, quando o usuário busca "xyzq", então aparece "Nada encontrado para "xyzq"" e o atalho para Fontes. |

#### 3.1.7 Detalhes da obra

- **HU-08** Eu, como usuário, quero ver sinopse, autores e capítulos antes de ler para decidir e continuar de onde parei.

| ID | Requisito | Prior. | Critério de aceite |
| :-- | :-- | :-- | :-- |
| RF-29 | O app deve mostrar capa, título, títulos alternativos, autores, artistas, status, gêneros, sinopse (recolhida em 3 linhas, com "Mais"), fonte e idioma. | Must | Dado uma obra do MangaDex, quando a tela da obra abre, então autor, artista e status aparecem, e a sinopse abre por inteiro ao tocar em "Mais". |
| RF-30 | O app deve listar os capítulos com número, título, grupo, data, estado (não lido, em andamento com a página, lido) e se está baixado; permitir ordem crescente ou decrescente e filtrar por não lidos, baixados e grupo. | Must | Dado uma obra com capítulos lidos, quando o usuário liga o filtro "Não lidos", então os lidos somem da lista. |
| RF-31 | O botão principal deve dizer "Ler" (nada lido), "Continuar cap. N" (capítulo em andamento ou o próximo não lido) ou "Ler de novo" (tudo lido), e abrir o leitor no ponto certo. | Must | Dado o cap. 12 lido até a página 8, quando a obra abre, então o botão diz "Continuar cap. 12" e, tocado, abre na página 8. |
| RF-32 | O app deve marcar como lido ou não lido um capítulo, vários selecionados, ou todos os anteriores a um capítulo. | Must | Dado o cap. 10, quando o usuário escolhe "marcar anteriores como lidos", então os caps. 1 a 9 ficam lidos. |
| RF-33 | Quando o usuário puxar para atualizar, o app deve reler detalhes e capítulos da fonte. Se falhar, então mantém o que estava, com aviso. | Must | Dado um capítulo novo na fonte, quando o usuário puxa, então ele aparece no topo, marcado como novo até a obra ser aberta de novo. |
| RF-34 | O app deve oferecer "Abrir no navegador" para obras com página web na fonte. | Should | Dado uma obra do MangaDex, quando tocado, então abre `mangadex.org/title/<id>`. |
| RF-35 | O app deve permitir esconder grupos de scan numa obra. | Could | Dado uma obra com o grupo "Y", quando o usuário esconde esse grupo, então os capítulos dele somem da lista e do "Continuar". |

#### 3.1.8 Leitor

- **HU-09** Eu, como usuário, quero ler no modo certo para cada obra (mangá, HQ ou webtoon) sem pensar nisso.
- **HU-10** Eu, como usuário, quero que o próximo capítulo e as próximas páginas já estejam prontos para ler sem esperar.

| ID | Requisito | Prior. | Critério de aceite |
| :-- | :-- | :-- | :-- |
| RF-36 | O leitor deve oferecer os modos paginado da esquerda para a direita, paginado da direita para a esquerda, vertical paginado e webtoon (rolagem contínua). | Must | Dado o modo webtoon, quando o usuário rola, então as páginas se juntam sem espaço e, no fim do capítulo, a transição (RF-42) aparece como tela própria. |
| RF-37 | O app deve ter um modo padrão global e um por obra. Na primeira leitura, deve sugerir: mangá japonês, direita para a esquerda; manhwa ou manhua com a tag "Long Strip", webtoon; HQ ocidental e arquivos locais, esquerda para a direita. | Should | Dado uma obra do MangaDex com idioma original `ja`, quando aberta pela primeira vez, então o leitor usa direita para a esquerda. |
| RF-38 | O leitor deve virar a página por zonas de toque (L, Kindle, Bordas, Esquerda e direita, ou desligado), com opção de inverter, e abrir o menu ao tocar no centro. | Must | Dado o esquema "Bordas", quando o usuário toca na borda direita, então avança uma página. |
| RF-39 | O leitor deve ter zoom por pinça e toque duplo, escala (ajustar à tela, à largura, à altura ou original) e zoom inicial (automático, esquerda, direita ou centro). | Must | Dado uma página alta, quando o usuário escolhe "ajustar à largura", então a página ocupa a largura e rola na vertical. |
| RF-40 | O menu do leitor deve ter título e capítulo, barra de páginas com "X de Y", anterior e próximo capítulo e ajustes rápidos (modo, escala, zonas e fundo). | Must | Dado o menu aberto, quando o usuário arrasta a barra até 20 e solta, então a página 20 aparece. |
| RF-41 | O leitor deve pré-carregar as próximas N páginas (padrão 4, de 2 a 10) e, a 3 páginas do fim, a lista de páginas do próximo capítulo; com a opção "só em rede não tarifada", fora dela pré-carrega 1 página. | Must | Dado o pré-carregamento padrão, quando o usuário chega à página 5, então as páginas 6 a 9 já estão no cache. |
| RF-42 | Entre capítulos, o leitor deve mostrar "Fim do cap. N · Próximo: cap. M", avisar se faltam capítulos no meio (ex.: do 10 para o 12) e dizer se o próximo está baixado. Há lacuna quando a parte inteira do próximo número menos a do atual é maior que 1 (10 → 10.5 → 11 não tem lacuna). | Must | Dado o próximo capítulo sendo o 12, quando o usuário termina o cap. 10, então a transição mostra "Falta 1 capítulo". |
| RF-43 | O app deve gravar a página atual 500 ms depois da última troca e ao sair do leitor, e marcar o capítulo como lido ao chegar na última página. | Must | Dado o app fechado na página 7, quando reaberto, então o capítulo continua na 7. |
| RF-44 | Em paisagem, em tablet ou em dobrável aberto como livro, o leitor deve poder mostrar duas páginas lado a lado. Em retrato, deve poder dividir páginas largas em duas. As duas opções podem inverter a ordem. | Should | Dado um tablet em paisagem com página dupla ligada, quando o capítulo abre, então as páginas 2 e 3 aparecem juntas, e uma página larga aparece sozinha. |
| RF-45 | O leitor deve poder cortar bordas brancas ou pretas das páginas. | Should | Dado uma página com margem branca uniforme, quando o usuário liga "cortar bordas", então a margem some e o conteúdo ocupa a tela. |
| RF-46 | O leitor deve ter fundo preto, branco, cinza ou automático, tela sempre ligada, número da página visível e modo imersivo que usa a área do recorte da câmera. | Should | Dado "tela sempre ligada", quando o usuário lê sem tocar por 5 minutos, então a tela não apaga. |
| RF-47 | O leitor deve virar a página pelas teclas de volume (opcional), pelas setas, pela barra de espaço e pelos botões de um controle. | Should | Dado um teclado conectado, quando o usuário aperta a seta direita no modo esquerda para a direita, então avança uma página. |
| RF-48 | No modo webtoon, o leitor deve permitir margem lateral de 0% a 25%. | Could | Dado o modo webtoon, quando o usuário ajusta a margem para 15%, então as páginas ficam com 15% de margem de cada lado. |
| RF-49 | O leitor deve ter rolagem automática nos modos vertical e webtoon, com velocidade ajustável e pausa ao tocar. | Could | Dado a rolagem automática ligada, quando o usuário toca, então ela pausa. |
| RF-50 | O leitor deve ter filtro de cor próprio: escurecer, sépia e escala de cinza. | Could | Dado uma página colorida, quando o usuário liga "escala de cinza", então as páginas aparecem sem cor. |
| RF-51 | Se uma página não carregar, então o leitor deve mostrar "Tentar de novo" só nela, sem perder as outras. Se o capítulo não tiver páginas, então deve mostrar "Capítulo indisponível na fonte", com "Voltar" e "Abrir no navegador". | Must | Dado a página 4 com erro, quando o usuário toca "Tentar de novo", então só ela é pedida de novo. |
| RF-97 | O leitor deve oferecer a leitura guiada por quadros: pelo botão "Quadro a quadro" no menu do leitor, a câmera vai de quadro em quadro, detectados no aparelho e guardados por página; sem quadros detectados, mostra a página inteira. | Could | Dado uma página com 5 quadros detectados, quando o usuário liga "Quadro a quadro" e avança 5 vezes, então cada quadro aparece sozinho na ordem de leitura do modo e o sexto avanço vai para a próxima página. |
| RF-98 | Na tela da obra, no leitor (barras, folha de ajustes e transição) e no painel de detalhe da obra em telas grandes, o app deve usar um esquema de cores tirado da capa: quantização e pontuação do M3 (MaterialKolor), pulando cores a até 35° da matiz do erro ou com croma abaixo de 16; sem cor válida, usa o violeta base. O esquema é guardado por obra e recalculado quando a capa muda; a troca de cor dura até 300 ms (troca direta com "reduzir movimento"); desligável em Ajustes. O resto do app fica no violeta. | Must | Dado a capa do Homem-Aranha (vermelho dominante, demais cores apagadas), quando a obra abre, então ela usa o violeta base; dado a capa do Invencível, quando a obra abre, então o botão "Continuar" usa verde-água em tom M3 e o texto nele passa em 4,5:1; dado a opção desligada, quando qualquer obra abre, então ela usa o violeta. |

#### 3.1.9 Biblioteca e categorias

- **HU-11** Eu, como usuário, quero organizar o que leio em categorias para achar rápido o que estou acompanhando.

| ID | Requisito | Prior. | Critério de aceite |
| :-- | :-- | :-- | :-- |
| RF-52 | O app deve permitir pôr e tirar uma obra da biblioteca nos detalhes e com toque longo nos resultados. Ao tirar, oferece "Desfazer". | Must | Dado uma obra na biblioteca com 2 categorias, quando o usuário toca "Remover" e depois "Desfazer", então a obra volta com as mesmas categorias. |
| RF-53 | O app deve permitir criar, renomear, reordenar e apagar categorias, e pôr uma obra em várias. Ao apagar uma categoria, as obras só perdem essa categoria; sem nenhuma, aparecem em "Sem categoria". Apagar tem "Desfazer". A biblioteca mostra uma aba por categoria. | Must | Dado a categoria "Lendo" com obras, quando o usuário a apaga, então as obras que não têm outra categoria aparecem em "Sem categoria". |
| RF-54 | A biblioteca deve oferecer grade compacta, grade confortável e lista; número de colunas pela largura da tela; e selos de não lidos, baixados e fonte, cada um desligável. | Must | Dado uma obra com 3 capítulos não lidos, quando a biblioteca abre, então o selo mostra "3". |
| RF-55 | A biblioteca deve filtrar (não lidos, baixados, iniciados, concluídos, fonte) e ordenar (A a Z, última leitura, último capítulo, total de capítulos, não lidos, data de entrada), por categoria ou para todas. | Must | Dado obras com e sem não lidos, quando o usuário liga "Não lidos" e ordena por "última leitura", então só obras com não lidos aparecem, da leitura mais recente para a mais antiga. |
| RF-56 | O app deve buscar na biblioteca por título, autor e gênero. | Should | Dado obras de Eiichiro Oda na biblioteca, quando o usuário busca "oda", então elas aparecem. |
| RF-57 | O app deve permitir selecionar várias obras para mudar a categoria, marcar como lidas, baixar os não lidos ou remover (com "Desfazer"). | Should | Dado 3 obras selecionadas, quando o usuário escolhe "Mover para Concluídas", então as 3 vão para "Concluídas". |
| RF-58 | Quando o usuário puxar para atualizar a biblioteca, o app deve buscar capítulos novos das obras online, uma fonte por vez e respeitando o RF-04, mostrar o progresso e, no fim, quantos capítulos novos chegaram. Se o app for para segundo plano, a atualização para e o que já chegou fica salvo. | Should | Dado 2 obras com capítulo novo, quando a atualização acaba, então aparece "2 capítulos novos" e os selos mudam. |

#### 3.1.10 Histórico

- **HU-12** Eu, como usuário, quero ver o que li por último para voltar a qualquer leitura.

| ID | Requisito | Prior. | Critério de aceite |
| :-- | :-- | :-- | :-- |
| RF-59 | O app deve listar as leituras agrupadas por dia, com capa, obra, capítulo, página e hora; o toque retoma a leitura. | Must | Dado o cap. 5 lido ontem até a página 3, quando tocado, então o leitor abre nessa página. |
| RF-60 | O app deve permitir remover um item e limpar tudo, com confirmação para limpar tudo. | Must | Dado um histórico com leituras, quando o usuário confirma "Limpar tudo", então a lista fica vazia e os capítulos lidos continuam marcados. |
| RF-61 | O app deve ter modo anônimo, em que leituras não entram no histórico nem mudam o progresso. | Could | Dado o modo anônimo ligado, quando um capítulo é lido, então ele não aparece no histórico. |

#### 3.1.11 Downloads

- **HU-13** Eu, como usuário, quero baixar capítulos para ler sem internet no transporte.

| ID | Requisito | Prior. | Critério de aceite |
| :-- | :-- | :-- | :-- |
| RF-62 | O app deve baixar um capítulo, os selecionados, os próximos N não lidos ou todos os não lidos. | Must | Dado uma obra com 8 não lidos, quando o usuário escolhe "próximos 5", então os 5 primeiros não lidos entram na fila. |
| RF-63 | O app deve baixar em segundo plano, numa fila com notificação de progresso, que pode ser pausada, retomada, cancelada e reordenada, e que continua depois de reiniciar o app (Android: também depois de reiniciar o aparelho; iOS: segue com o app ativo ou quando o sistema der tempo em segundo plano, RNF-28). | Must | Dado o app fechado no meio da fila, quando reaberto, então a fila continua do capítulo em que parou. |
| RF-64 | Por padrão, o app deve baixar só em rede não tarifada (Wi-Fi; opção desligável). | Must | Dado só dados móveis, quando a fila tem capítulos, então ela fica em "Esperando Wi-Fi". |
| RF-65 | O leitor deve usar o capítulo baixado em vez da rede, também sem internet. | Must | Dado um capítulo baixado e o modo avião, quando o usuário o abre, então todas as páginas aparecem. |
| RF-66 | O app deve apagar baixados por capítulo, por obra, depois de lidos (opcional) ou todos, mostrando o espaço usado. | Must | Dado "apagar depois de lido" ligado, quando o capítulo é marcado como lido, então os arquivos dele somem. |
| RF-67 | O app deve guardar os capítulos na pasta do app (padrão, lida só pelo HQR), salvar como CBZ (opcional) e, se o usuário escolher uma pasta, copiar para lá cada capítulo em `.cbz`, numa pasta por obra, para outros leitores. | Should | Dado "salvar como CBZ" ligado, quando um capítulo termina de baixar, então ele vira um `.cbz`. |
| RF-68 | Se o espaço livre ficar abaixo de 200 MB, então o app deve pausar a fila e avisar. | Should | Dado uma fila em andamento, quando o espaço livre cai para 150 MB, então a fila pausa com "Pouco espaço". |

#### 3.1.12 Juntar e migrar

- **HU-14** Eu, como usuário, quero passar uma obra para outra fonte, ou juntar duas, sem perder o que já li.

| ID | Requisito | Prior. | Critério de aceite |
| :-- | :-- | :-- | :-- |
| RF-69 | O app deve migrar uma obra para outra fonte: busca o título nas fontes escolhidas, mostra os candidatos e, confirmada a escolha, leva categorias, capítulos lidos (pelo número) e histórico; apagar os baixados antigos é opcional. | Should | Dado uma obra com os caps. 1 a 20 lidos, quando o usuário a migra para outra fonte, então os caps. 1 a 20 da nova fonte ficam lidos. |
| RF-70 | O app deve migrar várias obras de uma vez (as selecionadas ou todas de uma fonte), mostrando o resultado de cada uma. | Could | Dado 10 obras do HQ Now selecionadas, quando o usuário migra todas, então a tela mostra quais acharam par e quais não. |
| RF-71 | O app deve juntar a mesma obra de duas ou mais fontes numa só entrada. Os capítulos de todas aparecem por número; um número repetido usa a fonte de maior prioridade, definida pelo usuário. A junção pode ser desfeita. | Could | Dado a obra do MangaDex (caps. 1 a 50) e a de outra fonte (caps. 45 a 60), quando o usuário as junta, então a lista vai do 1 ao 60, sem repetir do 45 ao 50. |

#### 3.1.13 Backup e restauração

- **HU-15** Eu, como usuário, quero salvar e restaurar minha biblioteca para trocar de aparelho sem perder nada.

| ID | Requisito | Prior. | Critério de aceite |
| :-- | :-- | :-- | :-- |
| RF-72 | O app deve criar um backup num arquivo escolhido pelo usuário, com biblioteca, categorias, capítulos lidos e progresso, histórico, buscas salvas, feed, junções, ajustes e servidores. As senhas ficam de fora. | Must | Dado um servidor com senha cadastrado, quando o usuário cria um backup, então o arquivo não contém nenhuma senha. |
| RF-73 | O app deve restaurar um backup somando ao que já existe, sem apagar nada. No fim, mostra um resumo e lista as obras de fontes que este app não tem (ex.: uma fonte retirada do app), que ficam na biblioteca como "Fonte indisponível", com o nome da fonte lido do próprio backup (o app da loja não conhece o HQ Now pelo nome). | Must | Dado um backup do sabor GitHub, quando ele é restaurado no sabor loja, então as obras do HQ Now aparecem como "Fonte indisponível" e as do MangaDex funcionam. |
| RF-74 | O backup deve ter formato versionado e ser o mesmo no Android e no iOS. | Must | Dado um backup do Android, quando restaurado no iPhone, então biblioteca e progresso aparecem iguais. |
| RF-75 | O app deve fazer backup automático (diário ou semanal) numa pasta escolhida, guardando os N últimos. | Could | Dado o backup semanal com N = 3, quando o quarto backup é criado, então o mais antigo é apagado e ficam 3 arquivos. |

#### 3.1.14 Ajustes, Sobre e privacidade

- **HU-16** Eu, como usuário, quero ajustar tema e idioma e saber de onde vem o conteúdo.

| ID | Requisito | Prior. | Critério de aceite |
| :-- | :-- | :-- | :-- |
| RF-76 | O app deve oferecer tema do sistema, claro ou escuro, preto puro e, no Android 12 ou superior, cores dinâmicas. | Should | Dado o tema escuro, quando o usuário liga "preto puro", então o fundo é `#000000`. |
| RF-77 | O app deve estar em português do Brasil, inglês e espanhol, seguindo o idioma do aparelho, com troca dentro do app. | Must | Dado o aparelho em espanhol, quando o app abre, então os textos estão em espanhol. |
| RF-78 | A tela Sobre deve ter versão, sabor, créditos das fontes (MangaDex obrigatório), licenças de código aberto com o NOTICE do Mihon/TachiyomiSY, política de privacidade e o aviso de que o app não hospeda conteúdo. | Must | Dado o app instalado, quando o usuário abre o Sobre, então "MangaDex" e as licenças estão listados. |
| RF-79 | O app deve ter um link de doação no Sobre (serviço em Q-04); num eventual sabor de loja, não tem. | Could | Dado o app instalado, quando o usuário toca em "Apoiar", então o navegador abre a página de doação. |
| RF-80 | O app deve mostrar o tamanho do cache de imagens e permitir limpá-lo. | Must | Dado 300 MB de cache, quando o usuário toca "Limpar cache", então o tamanho mostrado vai a 0 e os baixados continuam. |
| RF-81 | Na primeira abertura, o app deve pedir os idiomas de conteúdo e mostrar uma vez o aviso de que não hospeda conteúdo, sem pedir conta. | Should | Dado a primeira abertura, quando o usuário escolhe "pt-br" e "en", então o MangaDex usa esses idiomas (RF-06). |

#### 3.1.15 Distribuição e atualização do app

- **HU-17** Eu, como usuário do APK, quero saber quando sai uma versão nova e instalá-la pelo app.

| ID | Requisito | Prior. | Critério de aceite |
| :-- | :-- | :-- | :-- |
| RF-82 | O app deve conferir o GitHub Releases ao abrir (no máximo uma vez por dia) e quando o usuário pedir. Havendo versão nova, mostra as novidades, baixa o APK e abre o instalador do sistema. | Must | Dado uma versão nova publicada, quando o app abre no dia seguinte, então aparece "Nova versão" com as novidades. |
| RF-83 | Removido: sem sabor de loja na 1.0 (C-03). | — | — |

#### 3.1.16 Passagem da versão 0.0.5

- **HU-18** Eu, como usuário da 0.0.5, quero chegar à versão nova sem confusão.

| ID | Requisito | Prior. | Critério de aceite |
| :-- | :-- | :-- | :-- |
| RF-84 | A primeira versão deve ser publicada no mesmo repositório como release normal (não prerelease), com a tag `v1.0.0` e o APK como primeiro arquivo, porque o atualizador da 0.0.5 lê `releases/latest`, compara só os dígitos do `tag_name` e baixa o primeiro arquivo; o SHA-256 vem depois. A notícia explica que é um app novo, instalável ao lado do antigo, que pede Android 8 ou superior, e que a biblioteca antiga não vem junto. | Must | Dado a 0.0.5 instalada, quando ela confere atualizações, então encontra a versão nova e a instala como app separado. |
| RF-85 | O app deve oferecer, nos Arquivos locais, a importação da pasta de downloads da 0.0.5, quando o usuário a escolher. | Could | Dado a pasta de downloads da 0.0.5, quando o usuário a escolhe, então os capítulos baixados aparecem como obras locais. |

#### 3.1.17 Estados, robustez e integração com o sistema

- **HU-19** Eu, como usuário, quero que o app se comporte bem sem internet, depois de ser fechado pelo sistema e com dados vindos de outro aparelho.

| ID | Requisito | Prior. | Critério de aceite |
| :-- | :-- | :-- | :-- |
| RF-86 | Toda tela com dados remotos deve ter estados de carregando, vazio, erro e sem conexão, com ação de saída em cada um. | Must | Dado o modo avião, quando Explorar abre, então aparece "Sem conexão" com "Tentar de novo" e o atalho para Baixados. |
| RF-87 | Enquanto estiver sem conexão, o app deve mostrar um aviso discreto, pôr a fila de downloads em espera e não alterar nada ao puxar para atualizar. | Must | Dado o modo avião, quando o usuário puxa a biblioteca, então aparece "Sem conexão" e nenhum dado muda. |
| RF-88 | Quando o sistema encerrar o processo, o app deve voltar à mesma tela, página do leitor, filtros e seleção. | Must | Dado "Não manter atividades" ligado, quando o usuário volta ao leitor, então ele está na mesma página. |
| RF-89 | Ao restaurar um backup sobre uma obra que já existe, o app deve unir capítulos lidos e categorias, manter o maior progresso e os ajustes locais, tudo numa só transação. | Must | Dado o cap. 5 lido só no backup, quando restaurado, então o cap. 5 fica lido e nenhum progresso local diminui. |
| RF-90 | Toda fonte embutida deve ter um id constante no código; cada servidor OPDS ganha um id gerado no cadastro e guardado; cada obra e capítulo usa a chave da fonte; na fonte local, a chave é o caminho relativo à pasta escolhida. | Must | Dado o nome de exibição de uma fonte trocado numa versão nova, quando o app atualiza, então a biblioteca continua igual. |
| RF-91 | Quando o formato da chave de uma fonte mudar, o app deve migrar as chaves salvas pela versão de esquema da fonte. | Should | Dado o esquema do HQ Now da v1 para a v2, quando o app atualiza, então as obras abrem sem migração manual. |
| RF-92 | Se um capítulo sumir da fonte, então o app deve manter progresso e arquivos baixados e marcá-lo "Removido na fonte". Números repetidos de grupos diferentes contam como um só para "Continuar" e lacunas. | Must | Dado o cap. 7 removido na fonte, quando a obra é atualizada, então o cap. 7 continua lido e marcado. |
| RF-93 | O app deve abrir CBZ e PDF por "Abrir com" e links `mangadex.org/title/*`. | Could | Dado o app instalado, quando o usuário toca num CBZ no gerenciador de arquivos e escolhe o HQR, então o leitor abre. |
| RF-94 | No Android 13 ou superior, o app deve pedir a permissão de notificação no primeiro download; negada, a fila segue e o progresso fica na tela Downloads. | Must | Dado a permissão negada, quando a fila roda, então os capítulos baixam. |
| RF-95 | Para servidores OPDS, o app deve aceitar só `http` e `https`, recusar usuário e senha escritos no endereço e nunca reenviar a credencial num redirecionamento para outro host. | Must | Dado um servidor com login, quando ele redireciona para outro domínio, então o pedido sai sem credencial. |
| RF-96 | O app deve aceitar certificado autoassinado de um servidor depois da confirmação do usuário, fixando-o; um certificado diferente depois é recusado. | Could | Dado um Komga autoassinado confirmado, quando o certificado muda, então a conexão é recusada com aviso. |

#### 3.1.18 Fontes adicionais

- **HU-20** Eu, como usuário, quero mais fontes em português e inglês, e uma legal, para achar mais obras sem sair do app.

| ID | Requisito | Prior. | Critério de aceite |
| :-- | :-- | :-- | :-- |
| RF-99 | O app deve oferecer o Internet Archive como fonte de HQs em domínio público, pela API oficial de busca e metadados, filtrando só itens com licença de domínio público na coleção de quadrinhos. | Should | Dado a fonte ligada, quando o usuário busca "Superman", então só aparecem itens com licença de domínio público, e o primeiro abre no leitor. |
| RF-100 | O app deve oferecer o NamiComi (mangá e HQ independentes, com pt-BR) pela API dele, reaproveitando o cliente do MangaDex onde o formato coincidir. | Should | Dado o idioma pt-BR, quando o usuário abre "Recentes" do NamiComi, então aparecem obras com capítulos em português. |
| RF-101 | O app deve oferecer o MangaFlix (mangá em pt-BR) pela API JSON do site, com no máximo 1 pedido por segundo. | Should | Dado a fonte ligada, quando o usuário abre "Populares", então as obras aparecem e nenhum segundo tem mais de 1 pedido. |
| RF-102 | O app deve oferecer o Yomu Mangas (mangá em pt-BR) pela API JSON do site, com no máximo 1 pedido por segundo, como alternativa ao MangaFlix para a migração (RF-69). | Should | Dado uma obra do MangaFlix fora do ar, quando o usuário migra para o Yomu, então a obra é encontrada e os lidos são levados. |
| RF-103 | O app deve oferecer o Weeb Central (mangá em inglês) por leitura das páginas do site, com no máximo 1 pedido a cada 2 segundos. | Should | Dado a fonte ligada, quando o usuário busca "one piece", então a obra aparece e o último capítulo abre no leitor. |
| RF-104 | O app deve oferecer o XOXO Comics (HQ ocidental em inglês) por leitura das páginas do site, com no máximo 1 pedido por segundo. | Should | Dado a fonte ligada, quando o usuário abre "Recentes", então aparecem edições com capa e número. |
| RF-105 | O app deve ler, uma vez por dia, uma lista de fontes publicada como arquivo estático no repositório (`sources.json`), que pode desligar uma fonte ou trocar o endereço dela, sem versão nova. Se a lista não puder ser lida, o app usa a última que guardou. | Must | Dado `sources.json` com o Weeb Central desligado, quando o app atualiza a lista, então a fonte some de Explorar e da busca, e as obras dela ficam como "Fonte indisponível". |
| RF-106 | Quando uma fonte pedir verificação do Cloudflare, o app deve abrir a página da fonte num navegador embutido para o usuário passar pela verificação e reaproveitar os cookies nos pedidos seguintes. | Should | Dado o Weeb Central respondendo com o desafio (`cf-mitigated: challenge`), quando o usuário toca "Verificar" e o desafio passa, então a lista carrega sem novo desafio na mesma sessão. |

#### 3.1.19 App web

- **HU-21** Eu, como usuário, quero ler no navegador do computador para continuar minhas leituras numa tela grande sem instalar nada.
- **HU-22** Eu, como usuário da web, quero ligar as fontes online pela minha própria ponte para ler MangaDex e HQ Now no navegador.

| ID | Requisito | Prior. | Critério de aceite |
| :-- | :-- | :-- | :-- |
| RF-107 | O app deve rodar no navegador (Compose para Web) num endereço estático do GitHub Pages e poder ser instalado como PWA, com biblioteca, histórico, ajustes e backup guardados no próprio navegador. | Should | Dado o Chrome no computador, quando o usuário abre o endereço e instala, então o app abre em janela própria com a biblioteca da visita anterior. |
| RF-108 | O app web deve permitir configurar uma ponte (endereço e token do proxy pessoal do usuário) com botão "Testar"; sem ponte válida, as fontes online aparecem em Explorar com o selo "Precisa da ponte" e a ação "Configurar ponte". | Should | Dado nenhuma ponte, quando o usuário abre o MangaDex, então vê "Precisa da ponte" e nenhum pedido sai para o MangaDex; dado um token errado, quando toca "Testar", então vê "Token recusado pela ponte". |
| RF-109 | O repositório deve trazer o código da ponte (Cloudflare Worker) e um passo a passo para o usuário publicá-la na própria conta grátis; a ponte só aceita pedidos com o token, só repassa para os hosts das fontes, repassa também as imagens, não envia o cabeçalho `Via` e não guarda conteúdo. | Should | Dado a ponte publicada, quando chega um pedido sem token ou para um host fora da lista, então ela responde 403; quando repassa ao MangaDex, então o pedido sai sem `Via` e com o User-Agent do HQR. |
| RF-110 | O app web deve importar CBZ e PDF por seletor ou arrastar e soltar para o armazenamento do navegador (OPFS), pedir armazenamento persistente e mostrar o espaço usado e livre; CBR e CB7 são recusados com o motivo. | Should | Dado um CBZ de 80 MB, quando o usuário o solta na janela, então a obra aparece em Arquivos e o espaço usado sobe 80 MB; dado um CBR, quando importado, então aparece "CBR não abre no navegador; converta para CBZ". |
| RF-111 | O app web deve conectar servidores OPDS, Komga e Kavita que liberem a origem do app (CORS); se o navegador bloquear, o teste da conexão explica como liberar a origem no servidor. | Should | Dado um Komga sem a origem liberada, quando o usuário testa, então vê "O servidor não liberou este endereço" com o valor a configurar; liberada a origem, quando testa de novo, então o catálogo abre. |
| RF-112 | No app web, o leitor deve funcionar por teclado e mouse: setas e espaço viram a página, a roda rola no webtoon, F alterna tela cheia, "?" mostra os atalhos, e os controles aparecem ao mover o mouse e somem depois de 2 s parado. | Should | Dado o leitor aberto, quando o usuário aperta → três vezes, então avança três páginas no sentido da leitura; quando aperta "?", então a lista de atalhos aparece. |
| RF-113 | O app web deve ter um endereço por tela, obra e capítulo, com voltar e avançar do navegador; recarregar mantém a tela e a página do leitor. | Should | Dado o capítulo 12 na página 8, quando o usuário recarrega, então volta ao capítulo 12 na página 8. |
| RF-114 | No app web, fontes e recursos que não funcionam no navegador (fontes com verificação do Cloudflare, CBR e CB7, downloads para ler sem internet, relatórios de falha) devem aparecer com o motivo em vez de sumir. | Should | Dado o Weeb Central, quando o usuário abre Explorar na web, então ele aparece desativado com "Só no app do celular"; dado Ajustes na web, quando aberto, então Downloads aparece com "Só no app do celular" e o motivo. |

### 3.2 Interfaces externas

| ID | Interface | Uso |
| :-- | :-- | :-- |
| IF-01 | MangaDex API v5 (`api.mangadex.org`) e servidores at-home | Listas, busca, obra, capítulos, servidor de imagens; relatório em `api.mangadex.network/report` (RF-07 a RF-11) |
| IF-02 | HQ Now GraphQL (`admin.hq-now.com/graphql`) | `getHqsByFilters`, `getRecentlyUpdatedHqs`, `getHqsByName`, `getHqsById`, `getChapterById` (RF-12) |
| IF-09 | Internet Archive (advancedsearch, metadata), NamiComi (API), MangaFlix e Yomu (API JSON interna), Weeb Central e XOXO (HTML) | Fontes adicionais (RF-99 a RF-104) |
| IF-10 | `sources.json` estático no repositório (raw do GitHub) | Desligar fonte ou trocar endereço (RF-105) |
| IF-11 | Ponte do usuário (Cloudflare Worker, código no repositório) | Fontes online no app web (RF-108, RF-109) |
| IF-12 | Armazenamento do navegador (OPFS, File API, Storage API) | Banco, arquivos importados e cota na web (RF-107, RF-110) |
| IF-03 | OPDS 1.2/2.0, OpenSearch e OPDS-PSE; APIs de progresso do Komga e do Kavita | Servidores do usuário (RF-17 a RF-20) |
| IF-04 | Seletor de pastas do sistema (Storage Access Framework; `UIDocumentPickerViewController`) | Arquivos locais, pasta de downloads e backup (RF-13, RF-67, RF-72) |
| IF-05 | Armazenamento seguro (Android Keystore; Keychain) | Senhas dos servidores (RF-17) |
| IF-06 | GitHub Releases API (`api.github.com/repos/tiagohs/hqr-comics-reader/releases/latest`) | Atualizador (RF-82) |
| IF-07 | Firebase Crashlytics (só Android) | Relatórios de falha (RNF-19) |
| IF-08 | Agendador de segundo plano (WorkManager; `BGTaskScheduler` e `URLSession` em segundo plano) | Downloads e backup automático (RF-63, RF-75) |

### 3.3 Requisitos não funcionais

| ID | Categoria | Requisito | Medida |
| :-- | :-- | :-- | :-- |
| RNF-01 | Desempenho | Abertura fria até a biblioteca utilizável | p90 ≤ 1,5 s em aparelho intermediário (classe Pixel 7a), medido com Macrobenchmark |
| RNF-02 | Desempenho | Primeira página de um capítulo online aparece | p90 ≤ 2 s com rede de 10 Mbps; capítulo baixado ≤ 500 ms |
| RNF-03 | Desempenho | Virar para página pré-carregada; rolagem no webtoon | ≤ 100 ms do toque ao quadro (trace do Macrobenchmark); ≤ 5% de quadros lentos (JankStats) num capítulo de 60 páginas |
| RNF-04 | Desempenho | Memória do leitor | Nenhum OutOfMemory num capítulo webtoon de 200 páginas; imagens acima de 4096 px decodificadas por partes, no tamanho da tela |
| RNF-05 | Desempenho | Busca global | Primeiro grupo de resultados em ≤ 3 s; tempo limite de 15 s por fonte, sem bloquear as outras |
| RNF-06 | Desempenho | Biblioteca grande | 1.000 obras: abre em ≤ 1 s e rola com ≤ 5% de quadros lentos |
| RNF-07 | Confiabilidade | Estabilidade | Usuários sem falha ≥ 99,5% e sessões sem ANR ≥ 99,8% (Crashlytics, 28 dias; sem Android vitals fora da Play) |
| RNF-08 | Eficiência | Tamanho | APK por ABI (arm64) ≤ 20 MB; APK universal ≤ 30 MB |
| RNF-09 | Segurança | Rede e segredos | Só HTTPS, exceto servidor OPDS em `http` em rede local (faixas RFC 1918 e `.local`), cadastrado pelo usuário com aviso e bloqueado no código para outros hosts; senhas só no armazenamento seguro; nenhuma chave, senha ou keystore no git |
| RNF-10 | Privacidade | Dados coletados | Android: só relatórios de falha do Crashlytics (rastro, aparelho, id de instalação), desligáveis nos Ajustes; nenhum analytics. iOS: "nenhum dado coletado". Data safety e etiqueta de privacidade coerentes com isso (RNF-29) |
| RNF-11 | Acessibilidade | Telas fora do conteúdo | Contraste ≥ 4,5:1, alvos ≥ 48 dp / 44 pt, TalkBack e VoiceOver em todos os controles, fonte a 200% sem corte; o leitor anuncia "Página X de Y" e é operável por teclado e controle (RF-47) |
| RNF-12 | Usabilidade | Telas grandes | Nível 2 (*adaptive optimized*) das diretrizes de telas grandes; testado em 411×891, 841×701 e 1280×800 dp; iPad em 1194×834 pt; web em 1280×720, 1440×900 e 1920×1080 px |
| RNF-13 | Compatibilidade | Plataformas | Android 8.0+ (API 26), targetSdk exigido pela Play, páginas de 16 KB, edge-to-edge e voltar preditivo; iOS e iPadOS 17+; web conforme RNF-31 |
| RNF-14 | Localização | Idiomas e formatos | pt-BR, en-US e es-419, com 100% dos textos traduzidos; datas e números pelo idioma do aparelho |
| RNF-15 | Manutenibilidade | Qualidade do código | `shared/domain` sem rede, banco nem plataforma; cobertura de linhas ≥ 80% em `domain` e ≥ 60% em `data` (Kover), com respostas das fontes gravadas como fixtures e nenhuma chamada real de rede no CI; ktlint, detekt e lint sem erros; nome do teste começa pelo ID que cobre |
| RNF-16 | Manutenibilidade | Fontes isoladas | Cada fonte em módulo próprio; um teste de build compila o app sem os módulos de fontes de terceiros e falha se `hq-now` ou `mangadex` sobrar no APK |
| RNF-17 | Confiabilidade | Isolamento de falhas | Falha de rede ou de fonte nunca apaga biblioteca, progresso, histórico ou baixados (testado com a fonte falhando em cada etapa) |
| RNF-18 | Confiabilidade | Banco de dados | Toda versão do esquema tem migração testada; zero perda de dados entre versões |
| RNF-19 | Operação | Observabilidade | Crashlytics no Android, com o mapping do R8 enviado a cada versão; nenhum endereço de conteúdo nos registros |
| RNF-20 | Custo | Operação | Zero servidor e zero custo recorrente; CI no runner do Mac do Tiago ou manual, dentro da cota gratuita |
| RNF-21 | Confiabilidade | Downloads | Retomam depois de falha de rede sem baixar de novo as páginas completas; cada imagem salva é conferida (decodifica) |
| RNF-22 | Eficiência | Bateria | Downloads e backup só pelo agendador do sistema, com as restrições de rede; nenhum wake lock adquirido diretamente pelo app |
| RNF-23 | Eficiência | Cache de imagens | Disco: LRU de 300 MB (ajustável de 100 MB a 2 GB); memória: até 25% do heap; capas da biblioteca fora do LRU |
| RNF-24 | Confiabilidade | Política de rede | Conexão 10 s, leitura 30 s; até 3 tentativas com espera crescente e aleatória, só em GET; um limitador por fonte compartilhado por leitor, busca, downloads e atualização |
| RNF-25 | Confiabilidade | Consistência | O banco é a fonte da verdade; um capítulo só conta como baixado depois do `rename` do `.tmp`; nenhuma escrita em disco na thread principal (StrictMode) |
| RNF-26 | Segurança | Arquivos não confiáveis | Recusa `..` e caminhos absolutos em arquivos compactados; aborta com compressão acima de 100:1 ou mais de 4 GB descompactados; imagens acima de 100 MP não são decodificadas; XML sem DTD nem entidades externas; backup com tamanho máximo de 200 MB e esquema validado |
| RNF-27 | Compatibilidade | Downloads no Android | Trabalho de transferência iniciado pelo usuário (API 34+) ou serviço em primeiro plano `dataSync`, respeitando o teto de 6 h por 24 h do Android 15; a fila pausa e retoma sem falha |
| RNF-28 | Compatibilidade | Downloads no iOS | Imagens por `URLSession` em segundo plano; a fila avança com o app ativo ou em `BGProcessingTask`; a diferença para o Android fica dita no app |
| RNF-29 | Conformidade | Distribuição | Política de privacidade publicada e citada no app; APK assinado pela identidade verificada; SHA-256 de cada APK na release; se houver loja ou iOS, manifesto de privacidade e classificação etária revisados antes |
| RNF-30 | Desempenho | App web: download e primeira carga | Arquivos do app ≤ 12 MB comprimidos; primeira carga até a biblioteca utilizável ≤ 3 s com 50 Mbps num computador intermediário; com cache ≤ 1,5 s (Lighthouse, 5 medições) |
| RNF-31 | Compatibilidade | Navegadores | Navegadores com WasmGC: Chrome e Edge 119+, Firefox 120+, Safari 18.2+ [verificar]; navegador sem suporte vê uma página com o motivo e o link do APK |
| RNF-32 | Segurança | Ponte e app web | A ponte exige token de ≥ 32 caracteres aleatórios, aceita só os hosts das fontes, não faz cache nem registra endereços; o app web tem Content-Security-Policy restrita à própria origem, à ponte e aos servidores cadastrados; o token fica só no navegador |
| RNF-33 | Privacidade | App web | Nenhuma coleta e nenhum relatório de falha; dados só no navegador (OPFS); limpar os dados do site apaga tudo, e o app diz isso ao lado do backup |

### 3.4 Outros requisitos

- **Conteúdo e lojas:** RNF-29.
- **Política de privacidade:** publicada no site institucional (`website-cb5.pages.dev/hqr/privacidade/`), citando MangaDex, HQ Now, Crashlytics, a ausência de conta e, na web, a ponte do próprio usuário.

## Apêndice A. Glossário

| Termo | Significado |
| :-- | :-- |
| Obra | HQ, mangá, manhwa ou webtoon; tem capítulos |
| Capítulo | Unidade de leitura: capítulo, edição ou volume |
| Fonte | De onde vêm obras e páginas: site, pasta local ou servidor |
| Sabor | Variante do app: loja (Play e App Store) ou GitHub (APK) |
| Grupo de scan | Grupo que traduziu e editou um capítulo |
| OPDS | Padrão de catálogo de publicações usado por Komga, Kavita e outros |
| Feed | Lista única das obras recentes de até 5 fontes ou buscas salvas |
| Junção | Uma obra vista como uma só a partir de várias fontes |
| Ponte | Proxy pessoal do usuário (Cloudflare Worker) que o app web usa para falar com as fontes online |
| Cor da capa | Esquema de cores do M3 tirado da capa da obra, usado só na obra e no leitor |

## Apêndice B. Perguntas em aberto

| ID | Pergunta | Recomendação |
| :-- | :-- | :-- |
| Q-02 | Nome e marca | Manter "HQR" (sem conflito encontrado) |
| Q-04 | Doação | "Apoiar" abrindo uma página do site com PIX e GitHub Sponsors, trocável sem versão nova |
| Q-09 | iOS sem App Store | AltStore/sideload com conta de desenvolvedor paga (assinatura de 1 ano) ou TestFlight; decidir antes de começar o iOS |

## Apêndice C. Atualização técnica (0.0.5 → 1.0)

| Tema | Hoje (0.0.5, 2018–2019) | 1.0 |
| :-- | :-- | :-- |
| Build | Gradle 4.4, AGP 3.1, Groovy, jcenter, Fabric | Gradle e AGP atuais, Kotlin DSL, `build-logic`, catálogo de versões (como o Script Reader) |
| Linguagem | Kotlin 1.2 + coroutines experimentais + RxJava 2 | Kotlin 2.x, coroutines e Flow, KMP |
| SDK | minSdk 17, target 28 | minSdk 26, target exigido pela Play, 16 KB, edge-to-edge |
| UI | Views, XML, Butterknife, FlexibleAdapter, Material Dialogs | Compose e Material 3 adaptativo; SwiftUI no iOS |
| DI | Dagger 2 | Koin |
| Banco | Realm 5 (com sync ligado) | Room 3 (KMP, também na web) |
| Rede e HTML | Retrofit, Jspoon, Gson, Jackson | Ktor, kotlinx.serialization, Ksoup |
| Imagens | Picasso, PhotoView | Coil 3 e um visualizador com zoom e decodificação por partes |
| Trabalho em segundo plano | android-job, GCM | WorkManager; BGTaskScheduler no iOS |
| Falhas | Crashlytics do Fabric (desativado) | Firebase Crashlytics |
| Anúncios | AdMob (banner e intersticial no leitor) | Nenhum (C-02) |
| Rede | `usesCleartextTraffic=true` | Só HTTPS (RNF-09) |
| Assinatura | Keystore no histórico público e senha no `build.gradle` | Chave nova; senha nos secrets e em `local.properties` (C-11) |
| CI | Travis e CircleCI | GitHub Actions no runner do Mac do Tiago (C-08) |
| Fontes | HQUltimate e ReadComicsOnline (fora do ar) | MangaDex, Arquivos locais, Servidores; HQ Now no GitHub |
| Testes | Modelo vazio | Testes por ID, Roborazzi, Kover (RNF-15) |
