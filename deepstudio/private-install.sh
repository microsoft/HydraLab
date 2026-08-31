#!/usr/bin/env bash
set -euo pipefail

PKG="${DEEPSTUDIO_PKG:-deepstudio-server}"

require_npm() {
  if ! command -v npm >/dev/null 2>&1; then
    echo ""
    echo "❌ Node.js / npm not found."
    echo "Please install Node.js first:"
    echo "https://nodejs.org/en/download"
    echo ""
    exit 1
  fi
}

b64() {
  if command -v openssl >/dev/null 2>&1; then
    printf '%s' "$1" | openssl base64 -A
  else
    printf '%s' "$1" | base64
  fi
}

cleanup() {
  npm config delete --global registry >/dev/null 2>&1 || true
  npm config delete --global "${AUTH_PREFIX}:username" >/dev/null 2>&1 || true
  npm config delete --global "${AUTH_PREFIX}:_password" >/dev/null 2>&1 || true
  npm config delete --global "${AUTH_PREFIX}:email" >/dev/null 2>&1 || true
  echo "✅ Cleanup complete."
}

require_npm

read -r -p "Enter npm registry URL (for ex: https://xxx.pkgs.xxx.com/xxx/_packaging/xxx/npm/registry/): " REGISTRY
REGISTRY="${REGISTRY%/}"

if [ -z "$REGISTRY" ]; then
  echo "Registry URL is required."
  exit 1
fi

# derive auth prefix
URI_NO_PROTO="${REGISTRY#https://}"
URI_NO_PROTO="${URI_NO_PROTO#http://}"
AUTH_PREFIX="//${URI_NO_PROTO}/"

trap cleanup EXIT

printf "Enter Azure DevOps PAT (Packaging:Read): "
stty -echo
read -r PAT
stty echo
printf "\n"

if [ -z "$PAT" ]; then
  echo "PAT is empty."
  exit 1
fi

PAT_B64="$(b64 "$PAT")"
unset PAT

npm config set --global registry "${REGISTRY}/" >/dev/null
npm config set --global "${AUTH_PREFIX}:username" microsoft >/dev/null
npm config set --global "${AUTH_PREFIX}:_password" "${PAT_B64}" >/dev/null
npm config set --global "${AUTH_PREFIX}:email" npm@example.com >/dev/null

npm install -g "${PKG}@latest" --registry "${REGISTRY}/"

echo ""
echo "✅ Installed ${PKG}@latest successfully."
echo "You can now run ${PKG} to launch it."
