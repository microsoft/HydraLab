param(
  [switch]$VerboseInstall
)

$ErrorActionPreference = "Stop"

# ---------------------------
# Settings / Feature flags
# ---------------------------
$PackageName = if ($env:DEEPSTUDIO_PKG) { $env:DEEPSTUDIO_PKG } else { "deepstudio-server" }

# Support both: param -VerboseInstall and env DEEPSTUDIO_VERBOSE=1
$VerboseInstall = $VerboseInstall -or ($env:DEEPSTUDIO_VERBOSE -eq "1")

# Dry run: only print commands, do not modify npm config or install
$DryRun = ($env:DEEPSTUDIO_DRY_RUN -eq "1")

# Save logs to file when DEEPSTUDIO_LOG=1
$EnableLog = ($env:DEEPSTUDIO_LOG -eq "1")

# Registry can be provided by env var (best for CI / automation)
$RegistryFromEnv = $env:DEEPSTUDIO_REGISTRY

# By default, do NOT inherit system/global proxy for install.
# Set DEEPSTUDIO_USE_SYSTEM_PROXY=1 if the user explicitly wants to keep it.
$UseSystemProxy = ($env:DEEPSTUDIO_USE_SYSTEM_PROXY -eq "1")

# Optional strict ssl override. Default = true.
$StrictSsl = if ($env:DEEPSTUDIO_STRICT_SSL -eq "0") { $false } else { $true }

# Optional override for log path
$LogPath = if ($env:DEEPSTUDIO_LOG_PATH) { $env:DEEPSTUDIO_LOG_PATH } else {
  Join-Path (Get-Location) ("deepstudio-install-" + (Get-Date -Format "yyyyMMdd-HHmmss") + ".log")
}

# Default registry for DeepStudio (stored as base64)
$DefaultRegistryB64 = "aHR0cHM6Ly9taWNyb3NvZnQucGtncy52aXN1YWxzdHVkaW8uY29tL09TL19wYWNrYWdpbmcvRGVlcFN0dWRpby9ucG0vcmVnaXN0cnkv"
$DefaultRegistry = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($DefaultRegistryB64))

# ---------------------------
# Helpers
# ---------------------------
function Info([string]$msg) { Write-Host "  $msg" -ForegroundColor Cyan }
function Warn([string]$msg) { Write-Host "  ⚠  $msg" -ForegroundColor Yellow }
function Fail([string]$msg) { Write-Host "  ✖  $msg" -ForegroundColor Red }
function Success([string]$msg) { Write-Host "  ✔  $msg" -ForegroundColor Green }
function Dim([string]$msg) { Write-Host "  $msg" -ForegroundColor DarkGray }

function Show-Banner {
  $lines = @(
    "  ____                  ____  _             _ _       "
    " |  _ \  ___  ___ _ __ / ___|| |_ _   _  __| (_) ___  "
    " | | | |/ _ \/ _ \ '_ \\___ \| __| | | |/ _`` | |/ _ \ "
    " | |_| |  __/  __/ |_) |___) | |_| |_| | (_| | | (_) |"
    " |____/ \___|\___| .__/|____/ \__|\__,_|\__,_|_|\___/ "
    "                 |_|          I n s t a l l e r  v2.1  "
  )
  $colors = @("Red", "Yellow", "Green", "Cyan", "Blue", "Magenta")
  Write-Host ""
  for ($i = 0; $i -lt $lines.Count; $i++) {
    Write-Host $lines[$i] -ForegroundColor $colors[$i % $colors.Count]
  }
  Write-Host ""
}

function Mask-Url([string]$url) {
  if ([string]::IsNullOrEmpty($url)) { return "<empty>" }
  try {
    $u = [Uri]$url
    $host_ = $u.Host
    if ($host_.Length -gt 8) {
      $host_ = $host_.Substring(0, 4) + "****" + $host_.Substring($host_.Length - 4)
    }
    return "$($u.Scheme)://$host_/****"
  } catch {
    if ($url.Length -le 10) { return ("*" * $url.Length) }
    return $url.Substring(0, 5) + "****" + $url.Substring($url.Length - 4)
  }
}

function Install-NodeViaWinget {
  if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
    Fail "winget not found — cannot auto-install Node.js."
    Info "Please install Node.js manually:"
    Info "👉 https://nodejs.org/en/download"
    Write-Host ""
    exit 1
  }

  Info "📥 Installing Node.js LTS via winget..."
  Write-Host ""
  try {
    & winget install --id OpenJS.NodeJS.LTS --accept-source-agreements --accept-package-agreements
    if ($LASTEXITCODE -ne 0 -and $LASTEXITCODE -ne -1978335189) {
      Fail "winget install returned exit code $LASTEXITCODE."
      exit 1
    }
    $env:PATH = [System.Environment]::GetEnvironmentVariable("PATH", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("PATH", "User")
    if (Get-Command node -ErrorAction SilentlyContinue) {
      Success "Node.js is ready."
    } else {
      Warn "Node.js installed but 'node' not found in PATH. You may need to restart your terminal."
      exit 1
    }
  }
  catch {
    Fail "Failed to install Node.js: $($_.Exception.Message)"
    exit 1
  }
}

function Upgrade-NodeViaWinget {
  if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
    Fail "winget not found — cannot auto-upgrade Node.js."
    Info "Please upgrade Node.js manually:"
    Info "👉 https://nodejs.org/en/download"
    Write-Host ""
    exit 1
  }

  Info "📥 Upgrading Node.js LTS via winget..."
  Write-Host ""
  try {
    & winget upgrade --id OpenJS.NodeJS.LTS --accept-source-agreements --accept-package-agreements
    if ($LASTEXITCODE -ne 0 -and $LASTEXITCODE -ne -1978335189) {
      Warn "winget upgrade returned exit code $LASTEXITCODE. Trying reinstall..."
      & winget uninstall --id OpenJS.NodeJS.LTS --accept-source-agreements 2>$null
      & winget install --id OpenJS.NodeJS.LTS --accept-source-agreements --accept-package-agreements
      if ($LASTEXITCODE -ne 0 -and $LASTEXITCODE -ne -1978335189) {
        Fail "winget reinstall returned exit code $LASTEXITCODE."
        exit 1
      }
    }
    $env:PATH = [System.Environment]::GetEnvironmentVariable("PATH", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("PATH", "User")
    if (Get-Command node -ErrorAction SilentlyContinue) {
      $newVersionStr = (& node --version 2>$null) -replace '^v', ''
      Success "Node.js is now at v$newVersionStr."
    } else {
      Warn "Node.js upgraded but 'node' not found in PATH. You may need to restart your terminal."
      exit 1
    }
  }
  catch {
    Fail "Failed to upgrade Node.js: $($_.Exception.Message)"
    exit 1
  }
}

function Require-Node {
  $MinNodeVersion = 22
  $nodeAvailable = [bool](Get-Command node -ErrorAction SilentlyContinue)

  if (-not $nodeAvailable) {
    Write-Host ""
    Write-Host "  ┌─────────────────────────────────────────────────────┐" -ForegroundColor Yellow
    Write-Host "  │  ⚙️  Node.js is not installed                       │" -ForegroundColor Yellow
    Write-Host "  │  Node.js >= $MinNodeVersion is required to continue.             │" -ForegroundColor Yellow
    Write-Host "  └─────────────────────────────────────────────────────┘" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "  📥 " -NoNewline -ForegroundColor Cyan
    $installChoice = Read-Host "Install Node.js via winget? [Y/n]"
    if ([string]::IsNullOrWhiteSpace($installChoice) -or $installChoice -match '^[Yy]') {
      Install-NodeViaWinget
      $nodeAvailable = [bool](Get-Command node -ErrorAction SilentlyContinue)
    } else {
      Fail "Node.js is required. Please install it and try again."
      Info "👉 https://nodejs.org/en/download"
      Write-Host ""
      exit 1
    }
  }

  if ($nodeAvailable) {
    try {
      $versionStr = & node --version 2>$null
      $versionStr = $versionStr -replace '^v', ''
      $major = [int]($versionStr.Split('.')[0])
      Dim "Node.js version: v$versionStr"

      if ($major -lt $MinNodeVersion) {
        Write-Host ""
        Write-Host "  ┌─────────────────────────────────────────────────────┐" -ForegroundColor Yellow
        Write-Host "  │  ⬆️  Node.js upgrade required                       │" -ForegroundColor Yellow
        Write-Host "  │  Current: v$versionStr" -NoNewline -ForegroundColor Yellow
        $pad = 39 - "Current: v$versionStr".Length
        if ($pad -lt 0) { $pad = 0 }
        Write-Host (" " * $pad + "│") -ForegroundColor Yellow
        Write-Host "  │  Required: >= v$MinNodeVersion.0.0                              │" -ForegroundColor Yellow
        Write-Host "  └─────────────────────────────────────────────────────┘" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "  ⬆️  " -NoNewline -ForegroundColor Cyan
        $upgradeChoice = Read-Host "Upgrade Node.js to latest LTS via winget? [Y/n]"
        if ([string]::IsNullOrWhiteSpace($upgradeChoice) -or $upgradeChoice -match '^[Yy]') {
          Upgrade-NodeViaWinget
        } else {
          Fail "Node.js >= v$MinNodeVersion is required. Please upgrade and try again."
          Info "👉 https://nodejs.org/en/download"
          Write-Host ""
          exit 1
        }
      }
    }
    catch {
      Warn "Could not determine Node.js version: $($_.Exception.Message)"
    }
  }

  if (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
    Fail "npm not found (Node.js may need a terminal restart)."
    exit 1
  }
}

function Read-Secure([string]$msg) {
  $secure = Read-Host $msg -AsSecureString
  $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
  try { [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr) }
  finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }
}

function Mask-Token([string]$token) {
  if ([string]::IsNullOrEmpty($token)) { return "<empty>" }
  if ($token.Length -le 6) { return ("*" * $token.Length) }
  $head = $token.Substring(0, 3)
  $tail = $token.Substring($token.Length - 3, 3)
  return "$head" + ("*" * ($token.Length - 6)) + "$tail"
}

function Ensure-Registry([string]$reg) {
  if ([string]::IsNullOrWhiteSpace($reg)) { throw "Registry URL is required." }
  $reg = $reg.Trim().TrimEnd("/")
  try { [void]([Uri]$reg) } catch { throw "Invalid registry URL: $reg" }
  return $reg
}

function New-TempNpmrc {
  return Join-Path $env:TEMP ("deepstudio-npmrc-" + [Guid]::NewGuid().ToString("N") + ".npmrc")
}

function To-Base64Ascii([string]$text) {
  return [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($text))
}

function Write-IsolatedUserNpmrc([string]$path, [string]$registry, [string]$authPrefix, [string]$pat, [bool]$strictSsl) {
  # Azure Artifacts npm auth is most reliable with username + base64 PAT + always-auth.
  $patB64 = To-Base64Ascii $pat
  $content = @"
registry=$registry/
always-auth=true
strict-ssl=$($strictSsl.ToString().ToLowerInvariant())
${authPrefix}:username=deepstudio
${authPrefix}:_password=$patB64
${authPrefix}:email=deepstudio@example.invalid
"@

  if ($DryRun) {
    Info "DRYRUN: write isolated USER npmrc to $path"
    Info ("DRYRUN: npmrc content (password token masked):`nregistry=$registry/`nalways-auth=true`nstrict-ssl=$($strictSsl.ToString().ToLowerInvariant())`n${authPrefix}:username=deepstudio`n${authPrefix}:_password=$(Mask-Token $patB64)`n${authPrefix}:email=deepstudio@example.invalid")
    return
  }

  Set-Content -Path $path -Value $content -Encoding ASCII
}

function Write-IsolatedGlobalNpmrc([string]$path, [bool]$strictSsl) {
  $content = @"
strict-ssl=$($strictSsl.ToString().ToLowerInvariant())
fund=false
audit=false
"@
  if ($DryRun) {
    Info "DRYRUN: write isolated GLOBAL npmrc to $path"
    return
  }
  Set-Content -Path $path -Value $content -Encoding ASCII
}

function Get-NpmConfigValue([string]$key) {
  try {
    $value = & npm config get $key 2>$null
    if ($LASTEXITCODE -ne 0) { return $null }
    if ($null -eq $value) { return $null }
    return ($value | Out-String).Trim()
  } catch {
    return $null
  }
}

function Show-ProxyDiagnostics {
  Write-Host ""
  Info "🔎 Checking proxy-related settings..."

  $pairs = [ordered]@{
    "npm config proxy"        = (Get-NpmConfigValue "proxy")
    "npm config https-proxy"  = (Get-NpmConfigValue "https-proxy")
    "HTTP_PROXY"              = $env:HTTP_PROXY
    "HTTPS_PROXY"             = $env:HTTPS_PROXY
    "ALL_PROXY"               = $env:ALL_PROXY
    "http_proxy"              = $env:http_proxy
    "https_proxy"             = $env:https_proxy
    "all_proxy"               = $env:all_proxy
    "NO_PROXY"                = $env:NO_PROXY
    "no_proxy"                = $env:no_proxy
  }

  $hasProxy = $false
  foreach ($k in $pairs.Keys) {
    $v = $pairs[$k]
    if (-not [string]::IsNullOrWhiteSpace($v) -and $v -ne "null") {
      $hasProxy = $true
      Dim ("  {0}: {1}" -f $k, $v)
    }
  }

  if (-not $hasProxy) {
    Success "No proxy settings detected."
  } elseif ($UseSystemProxy) {
    Warn "System/global proxy settings detected and will be preserved because DEEPSTUDIO_USE_SYSTEM_PROXY=1."
  } else {
    Warn "Proxy settings detected, but installer will ignore them for npm install to avoid broken corporate/global proxy issues."
  }
}

function Build-ProcessEnv([string]$userNpmrc, [string]$globalNpmrc) {
  $map = @{}

  # Hard isolate npm config sources for this child process.
  $map["NPM_CONFIG_USERCONFIG"] = $userNpmrc
  $map["npm_config_userconfig"] = $userNpmrc
  $map["NPM_CONFIG_GLOBALCONFIG"] = $globalNpmrc
  $map["npm_config_globalconfig"] = $globalNpmrc

  $map["NPM_CONFIG_FUND"] = "false"
  $map["NPM_CONFIG_AUDIT"] = "false"
  $map["NPM_CONFIG_UPDATE_NOTIFIER"] = "false"

  if ($VerboseInstall) {
    $map["NPM_CONFIG_LOGLEVEL"] = "verbose"
    $map["NPM_CONFIG_PROGRESS"] = "false"
  }

  if (-not $UseSystemProxy) {
    # Force npm/node not to inherit broken proxy values from machine/user/session.
    $proxyKeys = @(
      "HTTP_PROXY", "HTTPS_PROXY", "ALL_PROXY",
      "http_proxy", "https_proxy", "all_proxy",
      "NPM_CONFIG_PROXY", "NPM_CONFIG_HTTPS_PROXY",
      "npm_config_proxy", "npm_config_https_proxy"
    )
    foreach ($k in $proxyKeys) { $map[$k] = "" }
    # Explicitly override npm proxy resolution.
    $map["NPM_CONFIG_PROXY"] = "null"
    $map["NPM_CONFIG_HTTPS_PROXY"] = "null"
    $map["npm_config_proxy"] = "null"
    $map["npm_config_https_proxy"] = "null"
  }

  if (-not $StrictSsl) {
    $map["NPM_CONFIG_STRICT_SSL"] = "false"
    $map["npm_config_strict_ssl"] = "false"
  }

  return $map
}

function Run-ExternalCommand {
  param(
    [Parameter(Mandatory = $true)][string]$FileName,
    [Parameter(Mandatory = $true)][string[]]$Arguments,
    [hashtable]$EnvironmentOverrides,
    [string]$LogContext = ""
  )

  $printable = $FileName + " " + (($Arguments | ForEach-Object {
    if ($_ -match '\s') { '"' + $_ + '"' } else { $_ }
  }) -join " ")

  if ($DryRun) {
    Info ("DRYRUN: " + $printable)
    return
  }

  if ($EnableLog) {
    Info "Logging enabled. Log file: $LogPath"
  }
  if ($VerboseInstall) {
    Dim ("Command: " + $printable)
  }

  $psi = New-Object System.Diagnostics.ProcessStartInfo
  $psi.FileName = $FileName
  foreach ($arg in $Arguments) { [void]$psi.ArgumentList.Add($arg) }
  $psi.RedirectStandardOutput = $true
  $psi.RedirectStandardError  = $true
  $psi.UseShellExecute = $false
  $psi.CreateNoWindow = $true

  if ($EnvironmentOverrides) {
    foreach ($k in $EnvironmentOverrides.Keys) {
      $psi.Environment[$k] = [string]$EnvironmentOverrides[$k]
    }
  }

  $p = New-Object System.Diagnostics.Process
  $p.StartInfo = $psi
  [void]$p.Start()

  $stdout = $p.StandardOutput.ReadToEnd()
  $stderr = $p.StandardError.ReadToEnd()
  $p.WaitForExit()

  if ($stdout) { Write-Host $stdout }
  if ($stderr) { Write-Host $stderr }

  if ($EnableLog) {
    $content = @(
      "=== DeepStudio install log ==="
      "Time: $(Get-Date -Format o)"
      "Package: $PackageName@latest"
      "VerboseInstall: $VerboseInstall"
      "DryRun: $DryRun"
      "UseSystemProxy: $UseSystemProxy"
      "StrictSsl: $StrictSsl"
      if ($LogContext) { "Context: $LogContext" }
      "Command: $printable"
      ""
      "---- STDOUT ----"
      $stdout
      ""
      "---- STDERR ----"
      $stderr
      ""
      "ExitCode: $($p.ExitCode)"
      ""
    ) -join "`r`n"
    Add-Content -Path $LogPath -Value $content -Encoding UTF8
  }

  if ($p.ExitCode -ne 0) {
    throw "Command failed with exit code $($p.ExitCode): $printable"
  }
}

function Install-AzureCli {
  if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
    Warn "winget not found — cannot auto-install Azure CLI."
    return $false
  }

  Info "📥 Installing Azure CLI via winget..."
  Write-Host ""
  try {
    & winget install --id Microsoft.AzureCLI --accept-source-agreements --accept-package-agreements
    if ($LASTEXITCODE -ne 0) {
      Warn "winget install returned exit code $LASTEXITCODE."
      return $false
    }
    $env:PATH = [System.Environment]::GetEnvironmentVariable("PATH", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("PATH", "User")
    if (Get-Command az -ErrorAction SilentlyContinue) {
      Success "Azure CLI installed successfully."
      return $true
    } else {
      Warn "Azure CLI installed but 'az' not found in PATH. You may need to restart your terminal."
      return $false
    }
  }
  catch {
    Warn "Failed to install Azure CLI: $($_.Exception.Message)"
    return $false
  }
}

function Ensure-AzLogin {
  $azAvailable = [bool](Get-Command az -ErrorAction SilentlyContinue)

  if (-not $azAvailable) {
    Write-Host ""
    Write-Host "  ┌─────────────────────────────────────────────────────┐" -ForegroundColor Yellow
    Write-Host "  │  🔧 Azure CLI (az) is not installed                 │" -ForegroundColor Yellow
    Write-Host "  │  Optional: useful for login validation only.        │" -ForegroundColor Yellow
    Write-Host "  └─────────────────────────────────────────────────────┘" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "  📥 " -NoNewline -ForegroundColor Cyan
    $installChoice = Read-Host "Install Azure CLI now via winget? [Y/n]"
    if ([string]::IsNullOrWhiteSpace($installChoice) -or $installChoice -match '^[Yy]') {
      [void](Install-AzureCli)
    } else {
      Dim "Skipping Azure CLI installation."
      return
    }
  }

  if (Get-Command az -ErrorAction SilentlyContinue) {
    try {
      $account = & az account show --query "user.name" -o tsv 2>$null
      if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($account)) {
        Info "🔐 Please log in to Azure..."
        & az login
      } else {
        Success "Azure CLI session detected: $account"
      }
    } catch {
      Warn "Azure CLI login check failed: $($_.Exception.Message)"
    }
  }
}

# ---------------------------
# Start
# ---------------------------
Require-Node
Show-Banner

Write-Host "  ┌─────────────────────────────────────────┐" -ForegroundColor DarkCyan
Write-Host "  │  📦 Package:  " -NoNewline -ForegroundColor DarkCyan
Write-Host "$PackageName@latest" -NoNewline -ForegroundColor White
Write-Host "        │" -ForegroundColor DarkCyan
Write-Host "  │  🔧 Verbose:  " -NoNewline -ForegroundColor DarkCyan
Write-Host $(if ($VerboseInstall) { "ON " } else { "OFF" }) -NoNewline -ForegroundColor $(if ($VerboseInstall) { "Green" } else { "DarkGray" })
Write-Host "                       │" -ForegroundColor DarkCyan
Write-Host "  │  🧪 DryRun:   " -NoNewline -ForegroundColor DarkCyan
Write-Host $(if ($DryRun) { "ON " } else { "OFF" }) -NoNewline -ForegroundColor $(if ($DryRun) { "Yellow" } else { "DarkGray" })
Write-Host "                       │" -ForegroundColor DarkCyan
Write-Host "  │  📝 LogFile:  " -NoNewline -ForegroundColor DarkCyan
Write-Host $(if ($EnableLog) { "ON " } else { "OFF" }) -NoNewline -ForegroundColor $(if ($EnableLog) { "Green" } else { "DarkGray" })
Write-Host "                       │" -ForegroundColor DarkCyan
Write-Host "  │  🌐 Proxy:    " -NoNewline -ForegroundColor DarkCyan
Write-Host $(if ($UseSystemProxy) { "SYSTEM" } else { "IGNORED" }) -NoNewline -ForegroundColor $(if ($UseSystemProxy) { "Yellow" } else { "Green" })
Write-Host "                    │" -ForegroundColor DarkCyan
Write-Host "  │  🔒 StrictSSL: " -NoNewline -ForegroundColor DarkCyan
Write-Host $(if ($StrictSsl) { "ON " } else { "OFF" }) -NoNewline -ForegroundColor $(if ($StrictSsl) { "Green" } else { "Yellow" })
Write-Host "                       │" -ForegroundColor DarkCyan
Write-Host "  └─────────────────────────────────────────┘" -ForegroundColor DarkCyan
Write-Host ""

Show-ProxyDiagnostics

# Get registry
$registryInput = $RegistryFromEnv
if ([string]::IsNullOrWhiteSpace($registryInput)) {
  Write-Host "  🏢 " -NoNewline -ForegroundColor Cyan
  $adoOrg = Read-Host "Enter ADO org name [microsoft]"
  if ([string]::IsNullOrWhiteSpace($adoOrg)) { $adoOrg = "microsoft" }
  $registryInput = $DefaultRegistry -replace "microsoft\.pkgs", "$adoOrg.pkgs"
  Dim "Using org: $adoOrg"
}
$registry = Ensure-Registry $registryInput

Dim "Registry: $(Mask-Url $registry)"
Write-Host ""

# derive auth prefix (npmrc style)
$uri = [Uri]$registry
$authPrefix = "//" + $uri.Host + $uri.AbsolutePath.TrimEnd("/") + "/"

# Azure CLI is optional and now only used for login validation/help, not npm auth
Ensure-AzLogin

Write-Host ""
Write-Host "  ┌─────────────────────────────────────────────────────┐" -ForegroundColor Yellow
Write-Host "  │  🔑 Azure DevOps PAT required                       │" -ForegroundColor Yellow
Write-Host "  │  Create one at:                                     │" -ForegroundColor Yellow
Write-Host "  │  https://dev.azure.com/ > User Settings > PATs      │" -ForegroundColor Yellow
Write-Host "  │  Scope: Packaging > Read                            │" -ForegroundColor Yellow
Write-Host "  └─────────────────────────────────────────────────────┘" -ForegroundColor Yellow
Write-Host ""

$patRaw = Read-Secure "  🔑 Enter Azure DevOps PAT (Packaging:Read)"
if ([string]::IsNullOrWhiteSpace($patRaw)) { throw "PAT is empty." }
$pat = $patRaw.Trim()
$patRaw = $null

Write-Host ""
Dim ("Token (masked): " + (Mask-Token $pat))
Write-Host ""

$logLevel = if ($VerboseInstall) { "verbose" } else { "notice" }

$tmpUserNpmrc = New-TempNpmrc
$tmpGlobalNpmrc = New-TempNpmrc

try {
  Info "📄 Preparing isolated npm config..."
  Write-IsolatedUserNpmrc -path $tmpUserNpmrc -registry $registry -authPrefix $authPrefix -pat $pat -strictSsl $StrictSsl
  Write-IsolatedGlobalNpmrc -path $tmpGlobalNpmrc -strictSsl $StrictSsl

  $childEnv = Build-ProcessEnv -userNpmrc $tmpUserNpmrc -globalNpmrc $tmpGlobalNpmrc

  Write-Host ""
  Info "📦 Installing $PackageName@latest ..."
  Write-Host ""

  Run-ExternalCommand `
    -FileName "npm.cmd" `
    -Arguments @(
      "install", "-g", "$PackageName@latest",
      "--registry", "$registry/",
      "--loglevel", $logLevel,
      "--userconfig", $tmpUserNpmrc,
      "--globalconfig", $tmpGlobalNpmrc
    ) `
    -EnvironmentOverrides $childEnv `
    -LogContext "npm install"

  if (-not $DryRun) {
    Run-ExternalCommand `
      -FileName "npm.cmd" `
      -Arguments @(
        "list", "-g", "--depth=0", $PackageName,
        "--userconfig", $tmpUserNpmrc,
        "--globalconfig", $tmpGlobalNpmrc
      ) `
      -EnvironmentOverrides $childEnv `
      -LogContext "npm verify"
  }

  Write-Host ""
  Write-Host "  ┌─────────────────────────────────────────────────┐" -ForegroundColor Green
  Write-Host "  │  🎉 $PackageName@latest installed successfully! │" -ForegroundColor Green
  Write-Host "  └─────────────────────────────────────────────────┘" -ForegroundColor Green
  Write-Host ""

  Write-Host "  🚀 " -NoNewline -ForegroundColor Magenta
  $startChoice = Read-Host "Start $PackageName now? [Y/n]"
  if ([string]::IsNullOrWhiteSpace($startChoice) -or $startChoice -match '^[Yy]') {
    Write-Host ""
    Write-Host "  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Magenta
    Success "Launching $PackageName (press Ctrl+C to stop)..."
    Write-Host "  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Magenta
    Write-Host ""
    if ($DryRun) {
      Dim "DRYRUN: would run $PackageName"
    } else {
      & $PackageName
    }
  } else {
    Write-Host ""
    Info "You can start it later by running: $PackageName"
  }
}
catch {
  Write-Host ""
  Write-Host "  ┌─────────────────────────────────────────────────┐" -ForegroundColor Red
  Write-Host "  │  ❌ Installation failed                         │" -ForegroundColor Red
  Write-Host "  └─────────────────────────────────────────────────┘" -ForegroundColor Red
  Fail ("Error: " + $_.Exception.Message)

  Write-Host ""
  Warn "Likely causes:"
  Dim "  • Broken npm/global/system proxy settings"
  Dim "  • PAT missing Packaging:Read scope"
  Dim "  • PAT pasted with extra whitespace"
  Dim "  • Wrong registry URL / org name"
  Dim "  • No permission to the Azure Artifacts feed"
  Dim "  • Corporate SSL interception (try DEEPSTUDIO_STRICT_SSL=0 only if necessary)"

  if ($VerboseInstall) {
    Write-Host ""
    Dim "---- Full exception ----"
    Dim $_.Exception.ToString()
    Dim "------------------------"
  } else {
    Write-Host ""
    Warn "Tip: re-run with verbose logging:"
    Dim "  `$env:DEEPSTUDIO_VERBOSE='1'; `$env:DEEPSTUDIO_LOG='1'; irm <url> | iex"
    Dim "  Optional: keep corporate proxy if needed:"
    Dim "  `$env:DEEPSTUDIO_USE_SYSTEM_PROXY='1'; irm <url> | iex"
  }

  if ($EnableLog -and -not $DryRun) {
    Write-Host ""
    Warn "Log saved to: $LogPath"
  }

  throw
}
finally {
  if (-not $DryRun) {
    Remove-Item $tmpUserNpmrc -Force -ErrorAction SilentlyContinue
    Remove-Item $tmpGlobalNpmrc -Force -ErrorAction SilentlyContinue
  }
  Write-Host ""
  Success "Cleanup complete."
  Write-Host ""
}
