$ErrorActionPreference = "Stop"

$PackageName = if ($env:DEEPSTUDIO_PKG) { $env:DEEPSTUDIO_PKG } else { "deepstudio-server" }

function Require-Npm {
  if (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
    Write-Host ""
    Write-Host "❌ Node.js / npm not found."
    Write-Host "Please install Node.js first:"
    Write-Host "https://nodejs.org/en/download"
    Write-Host ""
    exit 1
  }
}

function Read-Secure([string]$msg) {
  $secure = Read-Host $msg -AsSecureString
  $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
  try { [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr) }
  finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }
}

function SetCfg($k, $v) { npm config set --global $k $v | Out-Null }
function DelCfg($k) { try { npm config delete --global $k | Out-Null } catch {} }

Require-Npm

$registry = Read-Host "Enter npm registry URL (for ex: https://xxx.pkgs.xxx.com/xxx/_packaging/xxx/npm/registry/)"
if ([string]::IsNullOrWhiteSpace($registry)) {
  throw "Registry URL is required."
}

# normalize registry
$registry = $registry.TrimEnd("/")

# derive auth prefix
$uri = [Uri]$registry
$authPrefix = "//" + $uri.Host + $uri.AbsolutePath + "/"

$pat = Read-Secure "Enter Azure DevOps PAT (Packaging:Read)"
if ([string]::IsNullOrWhiteSpace($pat)) {
  throw "PAT is empty."
}

$patB64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($pat))
$pat = $null

try {
  Write-Host "Installing $PackageName@latest"

  SetCfg "registry" "$registry/"
  SetCfg "${authPrefix}:username" "ms"
  SetCfg "${authPrefix}:_password" $patB64
  SetCfg "${authPrefix}:email" "npm@example.com"

  npm install -g "$PackageName@latest" --registry "$registry/"
  Write-Host "✅ Installed $PackageName@latest successfully."
  Write-Host "You can now run $PackageName to launch it."
}
finally {
  DelCfg "registry"
  DelCfg "${authPrefix}:username"
  DelCfg "${authPrefix}:_password"
  DelCfg "${authPrefix}:email"
  Write-Host "✅ Cleanup complete."
}
