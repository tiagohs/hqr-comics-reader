# Ponte do HQR (Cloudflare Worker)

O MangaDex, o HQ Now e a maior parte das fontes da 1.1 não liberam CORS, e o MangaDex proíbe o cabeçalho `Via` e quebra imagens em hotlink; por isso o
navegador não fala com eles direto. A ponte é um Worker que você publica na **sua** conta grátis do Cloudflare: o app
web manda para ela os pedidos das fontes online (API e imagens) com um token, e ela os repassa. O projeto não hospeda
nada (ADR-023, RF-108, RF-109, RNF-32).

## 1. Gerar o token

Pelo menos 32 caracteres aleatórios. Com token ausente ou mais curto, a ponte recusa tudo (403).

```bash
openssl rand -hex 32          # macOS, Linux
```

No Windows (PowerShell): `-join ((1..64) | % { '{0:x}' -f (Get-Random -Max 16) })`. Guarde o valor: ele vai no
Cloudflare e no app.

## 2. Publicar

### Pelo painel (sem instalar nada)

1. Crie uma conta grátis em https://dash.cloudflare.com.
2. **Workers & Pages › Create application › Start with Hello World! › Get started**; nome `hqr-bridge`, **Deploy**.
3. **Edit code**: apague o conteúdo, cole o de `src/index.js` e clique **Deploy**.
4. **Settings › Variables and Secrets › Add**: tipo **Secret**, nome `TOKEN`, valor do passo 1. Salve e publique.
5. Anote o endereço, por exemplo `https://hqr-bridge.<sua-conta>.workers.dev`.

Ao atualizar o HQR, se o app disser que a ponte está desatualizada, repita o passo 3 com o `src/index.js` novo.

Sem `HOSTS` e `ORIGINS` no painel, a ponte usa os valores padrão (os mesmos do `wrangler.toml`).

### Pelo terminal (Node 18+)

```bash
cd bridge-worker
npx wrangler login
npx wrangler deploy
npx wrangler secret put TOKEN     # cola o token do passo 1
```

O endereço aparece no fim do `deploy`.

Para conferir: `curl -H "X-HQR-Token: <token>" https://hqr-bridge.<sua-conta>.workers.dev/health` responde
`{"ok":true,"version":1}`.

## 3. Ligar no HQR web

**Fontes › Ponte**: cole o endereço e o token e toque **Testar**. "Token recusado pela ponte" é token diferente do
secret `TOKEN`. O token fica só no navegador.

## Usar outro endereço do app

Se você hospeda o HQR web em outro lugar, inclua a origem (protocolo, host e porta, sem barra no fim) em `ORIGINS`,
separada por vírgula:

- painel: **Settings › Variables and Secrets**, variável de texto `ORIGINS` =
  `https://tiagohs.github.io,https://hqr.meusite.com`;
- terminal: edite `ORIGINS` no `wrangler.toml` e rode `npx wrangler deploy`.

`HOSTS` funciona igual (aceita `*.exemplo.com`), mas só mude se uma fonte trocar de domínio.

## O que a ponte não faz

- Não guarda nada: sem cache, sem KV, sem log de endereços (`observability` desligado no `wrangler.toml`; no painel,
  deixe **Logs** desligado).
- Só repassa para os hosts de `HOSTS`, só em https, e confere de novo cada redirecionamento.
- Não envia `Via`, `X-Forwarded-For`, `Cookie`, `Authorization` nem o token; o User-Agent é o do HQR. O Cloudflare
  acrescenta sozinho, sem como tirar, `CF-Worker` (o domínio `workers.dev` da sua conta) e, quando o destino também
  usa o Cloudflare, `CF-Connecting-IP` com o endereço interno dos Workers (nunca o seu IP) e `CF-EW-Via` (detecção
  de laço; não é o `Via`).
- Os limites do plano grátis (100 mil pedidos por dia) são da sua conta; cada página de capítulo é um pedido.

## Testes

```bash
cd bridge-worker && node --test
```
