#!/usr/bin/env bash
# Sets up the release key of HQR (C-11) once, on the developer's Mac:
#   1. creates keystore/hqr-release.jks with a random password (if it doesn't exist yet);
#   2. writes HQR_KEYSTORE_PATH and HQR_KEYSTORE_PASSWORD in local.properties (both ignored by git);
#   3. stores the key and the password as the GitHub secrets the CI signs with.
# Never uses the 0.0.5 key (keystore.jks / keystore/keystore.jks): it is in the public history.
set -euo pipefail
cd "$(dirname "$0")/../.."

KEY=keystore/hqr-release.jks
PROPS=local.properties
REPO=tiagohs/hqr-comics-reader

command -v keytool >/dev/null || { echo "keytool não encontrado (instale um JDK)"; exit 1; }
command -v gh >/dev/null || { echo "gh não encontrado: brew install gh && gh auth login"; exit 1; }

mkdir -p keystore
if [ -f "$KEY" ]; then
  PASS=$(grep '^HQR_KEYSTORE_PASSWORD=' "$PROPS" 2>/dev/null | cut -d= -f2- || true)
  [ -n "$PASS" ] || { echo "$KEY existe, mas a senha não está em $PROPS"; exit 1; }
  echo "Usando a chave que já existe: $KEY"
else
  PASS=$(openssl rand -hex 24)
  # Through the environment: never on the command line, where other processes can read it.
  HQR_PASS="$PASS" keytool -genkeypair -keystore "$KEY" -storetype PKCS12 -alias hqr -keyalg RSA -keysize 4096 \
    -validity 10000 -storepass:env HQR_PASS -dname "CN=Tiago Silva, O=HQR Comics Reader, C=BR" >/dev/null
  touch "$PROPS"
  grep -v '^HQR_KEYSTORE_PATH=\|^HQR_KEYSTORE_PASSWORD=' "$PROPS" > "$PROPS.tmp" || true
  printf 'HQR_KEYSTORE_PATH=%s\nHQR_KEYSTORE_PASSWORD=%s\n' "$KEY" "$PASS" >> "$PROPS.tmp"
  mv "$PROPS.tmp" "$PROPS"
  chmod 600 "$PROPS" "$KEY"
  echo "Chave nova criada: $KEY (senha em $PROPS)"
fi

git check-ignore -q "$KEY" && git check-ignore -q "$PROPS" || { echo "A chave ou o local.properties não está no .gitignore"; exit 1; }

base64 -i "$KEY" | gh secret set HQR_KEYSTORE_BASE64 -R "$REPO"
printf '%s' "$PASS" | gh secret set HQR_KEYSTORE_PASSWORD -R "$REPO"
echo "Secrets HQR_KEYSTORE_BASE64 e HQR_KEYSTORE_PASSWORD gravados em $REPO."
echo "Guarde uma cópia de $KEY e da senha fora do computador: sem elas não há como atualizar o app."
