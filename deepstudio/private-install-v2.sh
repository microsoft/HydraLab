#!/usr/bin/env bash
set -euo pipefail

# ---------------------------
# Settings / Feature flags
# ---------------------------
PKG="${DEEPSTUDIO_PKG:-deepstudio-server}"
VERBOSE="${DEEPSTUDIO_VERBOSE:-0}"
DRY_RUN="${DEEPSTUDIO_DRY_RUN:-0}"
ENABLE_LOG="${DEEPSTUDIO_LOG:-0}"
REGISTRY_FROM_ENV="${DEEPSTUDIO_REGISTRY:-}"
LOG_PATH="${DEEPSTUDIO_LOG_PATH:-$(pwd)/deepstudio-install-$(date +%Y%m%d-%H%M%S).log}"
MIN_NODE_VERSION=22

# Default registry (base64-encoded)
DEFAULT_REGISTRY_B64="aHR0cHM6Ly9taWNyb3NvZnQucGtncy52aXN1YWxzdHVkaW8uY29tL09TL19wYWNrYWdpbmcvRGVlcFN0dWRpby9ucG0vcmVnaXN0cnkv"

# ---------------------------
# Color helpers
# ---------------------------
RED='\033[0;31m'
YELLOW='\033[0;33m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
BLUE='\033[0;34m'
DIM='\033[0;90m'
WHITE='\033[1;37m'
NC='\033[0m' # No Color

info()    { printf "  ${CYAN}%s${NC}\n" "$*"; }
warn()    { printf "  ${YELLOW}⚠  %s${NC}\n" "$*"; }
fail()    { printf "  ${RED}✖  %s${NC}\n" "$*"; }
success() { printf "  ${GREEN}✔  %s${NC}\n" "$*"; }
dim()     { printf "  ${DIM}%s${NC}\n" "$*"; }

show_banner() {
  local lines=(
    '  ____                  ____  _             _ _       '
    ' |  _ \  ___  ___ _ __ / ___|| |_ _   _  __| (_) ___  '
    ' | | | |/ _ \/ _ \ '"'"'_ \\___ \| __| | | |/ _` | |/ _ \ '
    ' | |_| |  __/  __/ |_) |___) | |_| |_| | (_| | | (_) |'
    ' |____/ \___|\___| .__/|____/ \__|\__,_|\__,_|_|\___/ '
    '                 |_|          I n s t a l l e r  v2    '
  )
  local colors=("$RED" "$YELLOW" "$GREEN" "$CYAN" "$BLUE" "$MAGENTA")
  echo ""
  for i in "${!lines[@]}"; do
    printf "%s%s%s\n" "${colors[$((i % 6))]}" "${lines[$i]}" "$NC"
  done
  echo ""
}

mask_url() {
  local url="$1"
  if [ -z "$url" ]; then echo "<empty>"; return; fi
  # Extract host, mask middle
  local host
  host=$(echo "$url" | sed -n 's|.*://\([^/]*\).*|\1|p')
  if [ ${#host} -gt 8 ]; then
    local head="${host:0:4}"
    local tail="${host: -4}"
    echo "https://${head}****${tail}/****"
  else
    echo "https://****/****"
  fi
}

mask_token() {
  local token="$1"
  if [ -z "$token" ]; then echo "<empty>"; return; fi
  local len=${#token}
  if [ "$len" -le 6 ]; then
    printf '%*s' "$len" '' | tr ' ' '*'
    echo ""
    return
  fi
  local head="${token:0:3}"
  local tail="${token: -3}"
  local mid_len=$((len - 6))
  local mid
  mid=$(printf '%*s' "$mid_len" '' | tr ' ' '*')
  echo "${head}${mid}${tail}"
}

b64_decode() {
  if command -v base64 >/dev/null 2>&1; then
    echo -n "$1" | base64 -d 2>/dev/null || echo -n "$1" | base64 -D 2>/dev/null
  elif command -v openssl >/dev/null 2>&1; then
    echo -n "$1" | openssl base64 -d -A
  else
    fail "No base64 decoder found."
    exit 1
  fi
}

# ---------------------------
# Detect package manager
# ---------------------------
detect_pkg_manager() {
  if command -v brew >/dev/null 2>&1; then
    echo "brew"
  elif command -v apt-get >/dev/null 2>&1; then
    echo "apt"
  elif command -v dnf >/dev/null 2>&1; then
    echo "dnf"
  elif command -v yum >/dev/null 2>&1; then
    echo "yum"
  elif command -v pacman >/dev/null 2>&1; then
    echo "pacman"
  else
    echo "none"
  fi
}

# ---------------------------
# Node.js management
# ---------------------------
install_node() {
  local mgr
  mgr=$(detect_pkg_manager)
  case "$mgr" in
    brew)
      info "📥 Installing Node.js via Homebrew..."
      brew install node@${MIN_NODE_VERSION} || brew install node
      brew link --overwrite node@${MIN_NODE_VERSION} 2>/dev/null || true
      ;;
    apt)
      info "📥 Installing Node.js via apt..."
      # Use NodeSource for latest LTS
      if command -v curl >/dev/null 2>&1; then
        curl -fsSL "https://deb.nodesource.com/setup_${MIN_NODE_VERSION}.x" | sudo -E bash -
        sudo apt-get install -y nodejs
      else
        sudo apt-get update && sudo apt-get install -y nodejs npm
      fi
      ;;
    dnf)
      info "📥 Installing Node.js via dnf..."
      if command -v curl >/dev/null 2>&1; then
        curl -fsSL "https://rpm.nodesource.com/setup_${MIN_NODE_VERSION}.x" | sudo bash -
      fi
      sudo dnf install -y nodejs
      ;;
    yum)
      info "📥 Installing Node.js via yum..."
      if command -v curl >/dev/null 2>&1; then
        curl -fsSL "https://rpm.nodesource.com/setup_${MIN_NODE_VERSION}.x" | sudo bash -
      fi
      sudo yum install -y nodejs
      ;;
    pacman)
      info "📥 Installing Node.js via pacman..."
      sudo pacman -Sy --noconfirm nodejs npm
      ;;
    *)
      fail "No supported package manager found."
      info "Please install Node.js manually:"
      info "👉 https://nodejs.org/en/download"
      exit 1
      ;;
  esac
}

upgrade_node() {
  local mgr
  mgr=$(detect_pkg_manager)
  case "$mgr" in
    brew)
      info "📥 Upgrading Node.js via Homebrew..."
      brew upgrade node 2>/dev/null || brew install node@${MIN_NODE_VERSION}
      brew link --overwrite node@${MIN_NODE_VERSION} 2>/dev/null || true
      ;;
    apt)
      info "📥 Upgrading Node.js via apt..."
      if command -v curl >/dev/null 2>&1; then
        curl -fsSL "https://deb.nodesource.com/setup_${MIN_NODE_VERSION}.x" | sudo -E bash -
        sudo apt-get install -y nodejs
      else
        sudo apt-get update && sudo apt-get install -y nodejs
      fi
      ;;
    dnf)
      info "📥 Upgrading Node.js via dnf..."
      if command -v curl >/dev/null 2>&1; then
        curl -fsSL "https://rpm.nodesource.com/setup_${MIN_NODE_VERSION}.x" | sudo bash -
      fi
      sudo dnf install -y nodejs
      ;;
    yum)
      info "📥 Upgrading Node.js via yum..."
      if command -v curl >/dev/null 2>&1; then
        curl -fsSL "https://rpm.nodesource.com/setup_${MIN_NODE_VERSION}.x" | sudo bash -
      fi
      sudo yum install -y nodejs
      ;;
    pacman)
      info "📥 Upgrading Node.js via pacman..."
      sudo pacman -Sy --noconfirm nodejs npm
      ;;
    *)
      fail "No supported package manager found."
      info "Please upgrade Node.js manually:"
      info "👉 https://nodejs.org/en/download"
      exit 1
      ;;
  esac
}

require_node() {
  if ! command -v node >/dev/null 2>&1; then
    echo ""
    printf "  ${YELLOW}┌─────────────────────────────────────────────────────┐${NC}\n"
    printf "  ${YELLOW}│  ⚙️  Node.js is not installed                       │${NC}\n"
    printf "  ${YELLOW}│  Node.js >= %s is required to continue.             │${NC}\n" "$MIN_NODE_VERSION"
    printf "  ${YELLOW}└─────────────────────────────────────────────────────┘${NC}\n"
    echo ""
    printf "  ${CYAN}📥 ${NC}"
    read -r -p "Install Node.js now? [Y/n] " install_choice
    if [ -z "$install_choice" ] || echo "$install_choice" | grep -qi '^y'; then
      install_node
      if ! command -v node >/dev/null 2>&1; then
        fail "Node.js installed but 'node' not found in PATH. You may need to restart your terminal."
        exit 1
      fi
      success "Node.js is ready."
    else
      fail "Node.js is required. Please install it and try again."
      info "👉 https://nodejs.org/en/download"
      exit 1
    fi
  fi

  # Check version
  local version_str major
  version_str=$(node --version 2>/dev/null | sed 's/^v//')
  major=$(echo "$version_str" | cut -d. -f1)
  dim "Node.js version: v${version_str}"

  if [ "$major" -lt "$MIN_NODE_VERSION" ] 2>/dev/null; then
    echo ""
    printf "  ${YELLOW}┌─────────────────────────────────────────────────────┐${NC}\n"
    printf "  ${YELLOW}│  ⬆️  Node.js upgrade required                       │${NC}\n"
    printf "  ${YELLOW}│  Current: v%-40s│${NC}\n" "$version_str"
    printf "  ${YELLOW}│  Required: >= v%s.0.0                              │${NC}\n" "$MIN_NODE_VERSION"
    printf "  ${YELLOW}└─────────────────────────────────────────────────────┘${NC}\n"
    echo ""
    printf "  ${CYAN}⬆️  ${NC}"
    read -r -p "Upgrade Node.js to >= v${MIN_NODE_VERSION}? [Y/n] " upgrade_choice
    if [ -z "$upgrade_choice" ] || echo "$upgrade_choice" | grep -qi '^y'; then
      upgrade_node
      version_str=$(node --version 2>/dev/null | sed 's/^v//')
      major=$(echo "$version_str" | cut -d. -f1)
      if [ "$major" -ge "$MIN_NODE_VERSION" ] 2>/dev/null; then
        success "Node.js upgraded to v${version_str}."
      else
        warn "Node.js is now v${version_str} but still below v${MIN_NODE_VERSION}. You may need to restart your terminal."
        exit 1
      fi
    else
      fail "Node.js >= v${MIN_NODE_VERSION} is required. Please upgrade and try again."
      info "👉 https://nodejs.org/en/download"
      exit 1
    fi
  fi

  # Final check: npm must be available
  if ! command -v npm >/dev/null 2>&1; then
    fail "npm not found (Node.js may need a terminal restart)."
    exit 1
  fi
}

# ---------------------------
# Azure CLI management
# ---------------------------
install_az_cli() {
  local mgr
  mgr=$(detect_pkg_manager)
  case "$mgr" in
    brew)
      info "📥 Installing Azure CLI via Homebrew..."
      brew install azure-cli
      ;;
    apt)
      info "📥 Installing Azure CLI via apt..."
      if command -v curl >/dev/null 2>&1; then
        curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash
      else
        fail "curl not found — cannot install Azure CLI automatically."
        return 1
      fi
      ;;
    *)
      info "📥 Installing Azure CLI via pip..."
      if command -v pip3 >/dev/null 2>&1; then
        pip3 install azure-cli
      elif command -v pip >/dev/null 2>&1; then
        pip install azure-cli
      else
        fail "No supported method to install Azure CLI."
        info "👉 https://learn.microsoft.com/cli/azure/install-azure-cli"
        return 1
      fi
      ;;
  esac
  if command -v az >/dev/null 2>&1; then
    success "Azure CLI installed successfully."
    return 0
  else
    warn "Azure CLI installed but 'az' not found in PATH. You may need to restart your terminal."
    return 1
  fi
}

get_az_access_token() {
  # Try to obtain a temporary access token via Azure CLI for Azure DevOps.
  local az_available=false
  if command -v az >/dev/null 2>&1; then
    az_available=true
  fi

  if [ "$az_available" = false ]; then
    echo ""
    printf "  ${YELLOW}┌─────────────────────────────────────────────────────┐${NC}\n"
    printf "  ${YELLOW}│  🔧 Azure CLI (az) is not installed                 │${NC}\n"
    printf "  ${YELLOW}│  It is recommended for automatic token auth.        │${NC}\n"
    printf "  ${YELLOW}└─────────────────────────────────────────────────────┘${NC}\n"
    echo ""
    printf "  ${CYAN}📥 ${NC}"
    read -r -p "Install Azure CLI now? [Y/n] " install_choice
    if [ -z "$install_choice" ] || echo "$install_choice" | grep -qi '^y'; then
      if install_az_cli; then
        az_available=true
      else
        dim "Continuing without Azure CLI."
      fi
    else
      dim "Skipping Azure CLI installation."
    fi
  fi

  if [ "$az_available" = false ]; then
    dim "Will fall back to manual PAT entry."
    echo ""
    return 1
  fi

  info "🔍 Azure CLI found. Checking login session..."

  local token
  token=$(az account get-access-token --resource "499b84ac-1321-427f-aa17-267ca6975798" --query "accessToken" -o tsv 2>/dev/null) || true

  if [ -z "$token" ]; then
    warn "No valid az login session found."
    echo ""
    info "🔐 Please log in to Azure to continue..."
    echo ""
    az login || {
      warn "az login failed — will fall back to manual PAT entry."
      return 1
    }
    # Retry after login
    token=$(az account get-access-token --resource "499b84ac-1321-427f-aa17-267ca6975798" --query "accessToken" -o tsv 2>/dev/null) || true
    if [ -z "$token" ]; then
      warn "Still unable to get token after login — will fall back to manual PAT entry."
      return 1
    fi
  fi

  success "Obtained temporary access token from Azure CLI."
  AZ_TOKEN="$token"
  return 0
}

# ---------------------------
# npmrc helpers
# ---------------------------
new_temp_npmrc() {
  local path
  path=$(mktemp /tmp/deepstudio-npmrc-XXXXXXXXXX.npmrc)
  echo "$path"
}

write_isolated_npmrc() {
  local path="$1" registry="$2" auth_prefix="$3" token="$4"
  cat > "$path" <<EOF
registry=${registry}/
${auth_prefix}:_authToken=${token}
EOF
}

run_npm() {
  local cmd_line="$1"
  if [ "$DRY_RUN" = "1" ]; then
    info "DRYRUN: $cmd_line"
    return 0
  fi

  if [ "$ENABLE_LOG" = "1" ]; then
    info "Logging enabled. Log file: $LOG_PATH"
    dim "Command: $cmd_line"
    eval "$cmd_line" > >(tee -a "$LOG_PATH") 2> >(tee -a "$LOG_PATH" >&2)
    local exit_code=$?
    if [ $exit_code -ne 0 ]; then
      fail "npm failed with exit code $exit_code. See log: $LOG_PATH"
      return $exit_code
    fi
  else
    eval "$cmd_line"
    local exit_code=$?
    if [ $exit_code -ne 0 ]; then
      fail "npm failed with exit code $exit_code. (Tip: set DEEPSTUDIO_LOG=1 to capture full logs.)"
      return $exit_code
    fi
  fi
  return 0
}

# ---------------------------
# Cleanup
# ---------------------------
TMP_NPMRC=""
cleanup() {
  if [ -n "$TMP_NPMRC" ] && [ -f "$TMP_NPMRC" ] && [ "$DRY_RUN" != "1" ]; then
    rm -f "$TMP_NPMRC"
  fi
  echo ""
  success "Cleanup complete."
  echo ""
}
trap cleanup EXIT

# ---------------------------
# Start
# ---------------------------
require_node

show_banner

# Settings panel
printf "  ${DIM}┌─────────────────────────────────────────┐${NC}\n"
printf "  ${DIM}│${NC}  📦 Package:  ${WHITE}%s@latest${NC}        ${DIM}│${NC}\n" "$PKG"
if [ "$VERBOSE" = "1" ]; then
  printf "  ${DIM}│${NC}  🔧 Verbose:  ${GREEN}ON ${NC}                       ${DIM}│${NC}\n"
else
  printf "  ${DIM}│${NC}  🔧 Verbose:  ${DIM}OFF${NC}                       ${DIM}│${NC}\n"
fi
if [ "$DRY_RUN" = "1" ]; then
  printf "  ${DIM}│${NC}  🧪 DryRun:   ${YELLOW}ON ${NC}                       ${DIM}│${NC}\n"
else
  printf "  ${DIM}│${NC}  🧪 DryRun:   ${DIM}OFF${NC}                       ${DIM}│${NC}\n"
fi
if [ "$ENABLE_LOG" = "1" ]; then
  printf "  ${DIM}│${NC}  📝 LogFile:  ${GREEN}ON ${NC}                       ${DIM}│${NC}\n"
else
  printf "  ${DIM}│${NC}  📝 LogFile:  ${DIM}OFF${NC}                       ${DIM}│${NC}\n"
fi
printf "  ${DIM}└─────────────────────────────────────────┘${NC}\n"
echo ""

# Get registry: env > construct from org name using default template
DEFAULT_REGISTRY=$(b64_decode "$DEFAULT_REGISTRY_B64")
REGISTRY_INPUT="$REGISTRY_FROM_ENV"
if [ -z "$REGISTRY_INPUT" ]; then
  printf "  ${CYAN}🏢 ${NC}"
  read -r -p "Enter ADO org name [microsoft] " ado_org
  if [ -z "$ado_org" ]; then ado_org="microsoft"; fi
  REGISTRY_INPUT=$(echo "$DEFAULT_REGISTRY" | sed "s/microsoft\.pkgs/${ado_org}.pkgs/")
  dim "Using org: $ado_org"
fi
# Trim trailing slash
REGISTRY="${REGISTRY_INPUT%/}"

if [ -z "$REGISTRY" ]; then
  fail "Registry URL is required."
  exit 1
fi

dim "Registry: $(mask_url "$REGISTRY")"
echo ""

# derive auth prefix (npmrc style)
URI_NO_PROTO="${REGISTRY#https://}"
URI_NO_PROTO="${URI_NO_PROTO#http://}"
AUTH_PREFIX="//${URI_NO_PROTO}/"

# Try Azure CLI token first, fall back to manual PAT
AZ_TOKEN=""
PAT=""
if get_az_access_token; then
  PAT="$AZ_TOKEN"
  success "Using temporary token from Azure CLI (no PAT creation needed)."
else
  echo ""
  printf "  ${YELLOW}┌─────────────────────────────────────────────────────┐${NC}\n"
  printf "  ${YELLOW}│  🔑 Manual PAT required                            │${NC}\n"
  printf "  ${YELLOW}│  Create one at:                                     │${NC}\n"
  printf "  ${YELLOW}│  https://dev.azure.com/ > User Settings > PATs      │${NC}\n"
  printf "  ${YELLOW}│  Scope: Packaging > Read                            │${NC}\n"
  printf "  ${YELLOW}└─────────────────────────────────────────────────────┘${NC}\n"
  echo ""
  printf "  🔑 Enter Azure DevOps PAT (Packaging:Read): "
  stty -echo 2>/dev/null || true
  read -r PAT
  stty echo 2>/dev/null || true
  printf "\n"

  if [ -z "$PAT" ]; then
    fail "PAT is empty."
    exit 1
  fi
fi

echo ""
dim "Token (masked): $(mask_token "$PAT")"
echo ""

# npm install args / loglevel
LOG_LEVEL="notice"
if [ "$VERBOSE" = "1" ]; then LOG_LEVEL="verbose"; fi

# Create isolated npmrc
TMP_NPMRC=$(new_temp_npmrc)

info "📄 Preparing isolated npm config..."
if [ "$DRY_RUN" = "1" ]; then
  info "DRYRUN: write isolated npmrc to $TMP_NPMRC"
  info "DRYRUN: npmrc content (token masked):"
  dim "  registry=${REGISTRY}/"
  dim "  ${AUTH_PREFIX}:_authToken=$(mask_token "$PAT")"
else
  write_isolated_npmrc "$TMP_NPMRC" "$REGISTRY" "$AUTH_PREFIX" "$PAT"
fi

# Build install command
INSTALL_CMD="npm install -g \"${PKG}@latest\" --registry \"${REGISTRY}/\" --loglevel ${LOG_LEVEL} --userconfig \"${TMP_NPMRC}\""

if [ "$VERBOSE" = "1" ]; then
  dim "Command: $INSTALL_CMD"
fi
echo ""
info "📦 Installing ${PKG}@latest ..."
echo ""

run_npm "$INSTALL_CMD"

# Verify installation
if [ "$DRY_RUN" != "1" ]; then
  VERIFY_CMD="npm list -g --depth=0 \"${PKG}\" --userconfig \"${TMP_NPMRC}\""
  run_npm "$VERIFY_CMD"
fi

echo ""
printf "  ${GREEN}┌─────────────────────────────────────────────────┐${NC}\n"
printf "  ${GREEN}│  🎉 %s@latest installed successfully! │${NC}\n" "$PKG"
printf "  ${GREEN}└─────────────────────────────────────────────────┘${NC}\n"
echo ""

# Ask user whether to start
printf "  ${MAGENTA}🚀 ${NC}"
read -r -p "Start ${PKG} now? [Y/n] " start_choice
if [ -z "$start_choice" ] || echo "$start_choice" | grep -qi '^y'; then
  echo ""
  printf "  ${MAGENTA}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"
  success "Launching ${PKG} (press Ctrl+C to stop)..."
  printf "  ${MAGENTA}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"
  echo ""
  if [ "$DRY_RUN" = "1" ]; then
    dim "DRYRUN: would run ${PKG}"
  else
    exec "$PKG"
  fi
else
  echo ""
  info "You can start it later by running: ${PKG}"
fi
