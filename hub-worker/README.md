# HQR: Worker pessoal

Um Cloudflare Worker com D1 (ADR-027 a ADR-029): login com Google trocado por sessão do HQR, sincronização entre
aparelhos, a ponte das fontes online na web e o próprio app web nos assets. Só os e-mails de `ALLOWED_EMAILS` entram.

| Rota | O quê |
| :-- | :-- |
| `POST /api/v1/auth/google` | `{idToken, device}` → `{token, email, expiresAt}`; 403 fora da lista |
| `POST /api/v1/auth/signout` | Encerra a sessão |
| `POST /api/v1/sync` | `{cursor, changes}` → `{cursor, changes, more}` |
| `GET /api/v1/config` | O client id web do Google (público) |
| `GET /api/v1/drive/works`, `works/:id`, `chapters/:id`, `files/:id` | O Meu Drive (ADR-030), com a sessão |
| `GET\|HEAD\|POST /bridge/fetch?url=…` | A ponte, com a sessão |
| o resto | O app web (`web/build/dist/wasmJs/productionExecutable`) |

## Uma vez só (Tiago)

1. **D1:** `npx wrangler@4.42.0 d1 create hqr`; o id vai no secret `HQR_D1_ID` do GitHub.
2. **Google Cloud › Credenciais:** três clientes OAuth.
   - **Web:** origem e redirecionamento `https://<worker>/` (o mesmo endereço do Worker).
   - **Android:** pacote `com.tiagohs.hqr.reader` e o SHA-1 da chave de release (`keytool -list -v -keystore keystore/hqr-release.jks`).
   - **iOS:** bundle `com.tiagohs.hqr.reader`.
3. **Secrets do Worker:** `npx wrangler@4.42.0 secret put ALLOWED_EMAILS` (o seu e-mail) e
   `npx wrangler@4.42.0 secret put GOOGLE_CLIENT_IDS` (web primeiro, depois iOS, separados por vírgula).
4. **Secrets do GitHub:**
   - `CLOUDFLARE_API_TOKEN` (modelo "Edit Cloudflare Workers" com D1), `CLOUDFLARE_ACCOUNT_ID` e `HQR_D1_ID`, que publicam
     o Worker a cada push;
   - `HQR_HUB_URL` (`https://<worker>`) e `HQR_GOOGLE_WEB_CLIENT_ID`, que entram no APK.
5. **iOS:** `ios/Config/Secrets.xcconfig` (fora do git) com `HQR_HUB_HOST = <worker sem https://>` e
   `HQR_GOOGLE_IOS_CLIENT_ID = <id>.apps.googleusercontent.com`.

Sem esses passos, o app funciona como antes, só que sem conta.

## Meu Drive (ADR-030)

1. **Google Cloud:** ative a *Google Drive API* e crie uma conta de serviço (IAM › Contas de serviço); em *Chaves*, crie uma chave JSON.
2. **Drive:** em `Documentos/hqr`, *Compartilhar* com o e-mail da conta de serviço (`…@….iam.gserviceaccount.com`) como **Leitor**.
3. **Secrets do Worker:**
   ```bash
   cd hub-worker
   npx wrangler@4.42.0 secret put DRIVE_SERVICE_ACCOUNT < caminho/da/chave.json
   npx wrangler@4.42.0 secret put DRIVE_ROOT_ID     # o id da pasta hqr: o trecho final de drive.google.com/drive/folders/<id>
   ```
4. **Pastas:** `hqr/mangas/<obra>/` e `hqr/hqs/<obra>/`, cada obra com `poster.png` (ou `.jpg`), `info.json` e as pastas de capítulo `01`, `02`… com as páginas (qualquer nome; a ordem é a dos números).

`info.json`, tudo opcional: `title`, `alternativeTitles`, `authors`, `artists`, `status` (`ongoing`, `completed`, `hiatus`, `cancelled`),
`genres`, `description`, `language` (`pt-br`), `chapters` (`{"01": "Título"}`).

## Desenvolvimento

```bash
npm ci && npm run typecheck && npm test   # vitest; D1 simulado sobre node:sqlite
```
