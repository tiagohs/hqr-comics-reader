---
name: ios-on-mac
description: Leva as mudanças desta nuvem ao Mac do Tiago, compila e testa o app iOS pelo Xcode (MCP) e deixa a pasta do Mac alinhada com o git. Use ao mexer em ios/ ou em shared/app (iosMain).
---

# iOS no Mac

O Swift e o framework `Shared` só compilam no Mac. A nuvem edita; o Mac compila e testa.

## 1. Verificar na nuvem

```bash
python3 scripts/ios/check_previews.py --check      # toda view com #Preview claro e escuro
python3 scripts/ios/generate_strings.py --check    # Localizable.xcstrings igual ao strings.xml
./gradlew -Phqr.ios=false :shared:app:ktlintCheck  # Kotlin do iosMain
```

## 2. Levar ao Mac

- **Montar o pacote:** inclua os arquivos mudados desde o último commit que o Mac tem, e deixe `docs/` de fora (sem
  permissão lá).
  ```bash
  (git diff --name-only <base> HEAD; git diff --name-only; git ls-files --others --exclude-standard) \
    | grep -v '^docs/' | sort -u > /tmp/files.txt
  f=/mnt/user-data/outputs/hqr-sync-$(date +%H%M%S).tgz; tar czf $f -T /tmp/files.txt
  ```
- **Copiar:** `device_commit_files` para `<repo no Mac>/build/<mesmo nome>`. O nome precisa ser único, senão pode
  chegar o pacote antigo.
- **Extrair:** via `device_bash`, `tar xzf build/<pacote> --overwrite`.

## 3. Compilar e testar

1. **Achar o workspace:** `XcodeListWorkspaces` (o do HQR é `ios/HQR.xcodeproj`).
2. **Compilar:** `BuildProject` com `buildForTesting: true`. Se a chamada passar de 60 s, acompanhe com
   `GetBuildLog` até `buildIsRunning: false`.
3. **Testar:** `RunAllTests`. Se passar do tempo, leia o `.xcresult` mais novo em
   `~/Library/Developer/Xcode/DerivedData/HQR-*/Logs/Test/` (descomprima `Data/*` com `zstd -dc` e busque
   `passed`/`failed`).
4. **Erros comuns** (`lessons.md` §Kotlin/Native):
   - nome ambíguo: use `Shared.<Tipo>`;
   - palavra reservada exportada com `_` (`.auto_`);
   - classe aninhada com o nome colado (`SourceFilterOption`).

## 4. Deixar o Mac limpo

- **Commit e push:** pela nuvem, depois dos testes verdes.
- **Alinhar a pasta do Mac:**
  1. Compare cada arquivo solto com `origin/<branch>`; apague só os idênticos.
  2. Rode `git merge --ff-only origin/<branch>`.
  3. Descarte o `project.pbxproj` que o Xcode só reformatou.
  4. Apague os pacotes em `build/`.
  O git apaga `.lock`, então peça a permissão de exclusão uma vez.
- **Arquivos do Tiago** (como `log.txt`): não mexa.
