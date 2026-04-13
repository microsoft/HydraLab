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

# Optional override for log path
$LogPath = if ($env:DEEPSTUDIO_LOG_PATH) { $env:DEEPSTUDIO_LOG_PATH } else {
  Join-Path (Get-Location) ("deepstudio-install-" + (Get-Date -Format "yyyyMMdd-HHmmss") + ".log")
}

# Installer version
$InstallerVersion = "2.6"

# Default registry for DeepStudio (stored as base64)
$DefaultRegistryB64 = "aHR0cHM6Ly9taWNyb3NvZnQucGtncy52aXN1YWxzdHVkaW8uY29tL09TL19wYWNrYWdpbmcvRGVlcFN0dWRpby9ucG0vcmVnaXN0cnkv"
$DefaultRegistry = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($DefaultRegistryB64))

# Will be set later after registry is resolved
$script:ResolvedRegistry = $null

# Rolling in-memory log buffer (last N lines) — dumped on error
$script:LogBuffer = [System.Collections.Generic.Queue[string]]::new()
$script:LogBufferMax = 100

# ---------------------------
# Helpers
# ---------------------------
function Info([string]$msg) { Write-Host "  $msg" -ForegroundColor Cyan }
function Warn([string]$msg) { Write-Host "  ⚠  $msg" -ForegroundColor Yellow }
function Fail([string]$msg) { Write-Host "  ✖  $msg" -ForegroundColor Red }
function Success([string]$msg) { Write-Host "  ✔  $msg" -ForegroundColor Green }
function Dim([string]$msg) { Write-Host "  $msg" -ForegroundColor DarkGray }

function Add-ToLogBuffer([string]$line) {
  $script:LogBuffer.Enqueue($line)
  while ($script:LogBuffer.Count -gt $script:LogBufferMax) {
    [void]$script:LogBuffer.Dequeue()
  }
}

function Dump-LogBuffer {
  if ($script:LogBuffer.Count -eq 0) { return }
  Write-Host ""
  Warn "Recent output (last $($script:LogBuffer.Count) lines):"
  Write-Host "  ────────────────────────────────────────────────" -ForegroundColor DarkGray
  foreach ($line in $script:LogBuffer) {
    Dim $line
  }
  Write-Host "  ────────────────────────────────────────────────" -ForegroundColor DarkGray
}

function Show-Banner {
  $lines = @(
    "  ____                  ____  _             _ _       "
    " |  _ \  ___  ___ _ __ / ___|| |_ _   _  __| (_) ___  "
    " | | | |/ _ \/ _ \ '_ \\___ \| __| | | |/ _`` | |/ _ \ "
    " | |_| |  __/  __/ |_) |___) | |_| |_| | (_| | | (_) |"
    " |____/ \___|\___| .__/|____/ \__|\__,_|\__,_|_|\___/ "
    "                 |_|          I n s t a l l e r  v$InstallerVersion    "
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
  $path = Join-Path $env:TEMP ("deepstudio-npmrc-" + [Guid]::NewGuid().ToString("N") + ".npmrc")
  return $path
}

function Write-IsolatedNpmrc([string]$path, [string]$registry, [string]$authPrefix, [string]$pat) {
  $content = @"
registry=$registry/
always-auth=true
${authPrefix}:_authToken=$pat
"@

  if ($DryRun) {
    Info "DRYRUN: write isolated npmrc to $path"
    Info ("DRYRUN: npmrc content (token masked):`nregistry=$registry/`nalways-auth=true`n${authPrefix}:_authToken=$(Mask-Token $pat)")
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

  # PowerShell hashtable keys are case-insensitive, so don't include both HTTP_PROXY and http_proxy as separate keys.
  $pairs = [ordered]@{
    "npm config proxy"       = (Get-NpmConfigValue "proxy")
    "npm config https-proxy" = (Get-NpmConfigValue "https-proxy")
    "HTTP_PROXY"             = if ($env:HTTP_PROXY) { $env:HTTP_PROXY } else { $env:http_proxy }
    "HTTPS_PROXY"            = if ($env:HTTPS_PROXY) { $env:HTTPS_PROXY } else { $env:https_proxy }
    "ALL_PROXY"              = if ($env:ALL_PROXY) { $env:ALL_PROXY } else { $env:all_proxy }
    "NO_PROXY"               = if ($env:NO_PROXY) { $env:NO_PROXY } else { $env:no_proxy }
  }

  $hasProxy = $false
  foreach ($entry in $pairs.GetEnumerator()) {
    $k = $entry.Key
    $v = $entry.Value
    if (-not [string]::IsNullOrWhiteSpace($v) -and $v -ne "null") {
      $hasProxy = $true
      Dim ("  {0}: {1}" -f $k, $v)
    }
  }

  if (-not $hasProxy) {
    Success "No proxy settings detected."
  } else {
    Warn "Proxy settings detected. If install fails with npm network/proxy errors, check npm proxy configuration."
  }
}

function Show-NpmConfigConflicts {
  Write-Host ""
  Info "🔎 Checking for conflicting npm configurations..."
  $conflicts = 0

  # 1. Check NPM_CONFIG_REGISTRY env var (overrides --registry in some npm versions)
  $envRegistry = $env:NPM_CONFIG_REGISTRY
  if (-not [string]::IsNullOrWhiteSpace($envRegistry)) {
    $conflicts++
    Warn "NPM_CONFIG_REGISTRY env var is set: $envRegistry"
    Warn "  This can override --registry flag. Will be cleared during install."
  }

  # 2. Check for project-level .npmrc in cwd and parent directories
  $searchDir = Get-Location
  $projectNpmrcs = @()
  while ($null -ne $searchDir -and $searchDir.Path.Length -gt 3) {
    $candidate = Join-Path $searchDir.Path ".npmrc"
    if (Test-Path $candidate) {
      $projectNpmrcs += $candidate
    }
    $searchDir = Split-Path $searchDir.Path -Parent | Get-Item -ErrorAction SilentlyContinue
  }
  if ($projectNpmrcs.Count -gt 0) {
    foreach ($p in $projectNpmrcs) {
      $conflicts++
      Warn "Project .npmrc found: $p"
      # Check if it sets a registry
      $content = Get-Content $p -Raw -ErrorAction SilentlyContinue
      if ($content -match '(?m)^\s*registry\s*=') {
        Warn "  ↳ Contains 'registry=' — this WILL override the install registry!"
      }
      if ($content -match '(?m)^\s*//.*:_auth') {
        Dim "  ↳ Contains auth tokens"
      }
    }
  }

  # 3. Check for global npmrc
  try {
    $globalNpmrc = & npm config get globalconfig 2>$null
    if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($globalNpmrc) -and (Test-Path $globalNpmrc)) {
      $content = Get-Content $globalNpmrc -Raw -ErrorAction SilentlyContinue
      if ($content -match '(?m)^\s*registry\s*=') {
        $conflicts++
        Warn "Global .npmrc has 'registry=' set: $globalNpmrc"
        Warn "  ↳ This can override the install registry!"
      }
    }
  } catch {}

  # 4. Check the user-level npmrc for conflicting registry
  try {
    $userNpmrc = & npm config get userconfig 2>$null
    if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($userNpmrc) -and (Test-Path $userNpmrc)) {
      $content = Get-Content $userNpmrc -Raw -ErrorAction SilentlyContinue
      if ($content -match '(?m)^\s*registry\s*=') {
        Dim "User .npmrc has 'registry=' set: $userNpmrc (will be overridden by --userconfig)"
      }
    }
  } catch {}

  # 5. Check current effective registry
  $effectiveRegistry = Get-NpmConfigValue "registry"
  if (-not [string]::IsNullOrWhiteSpace($effectiveRegistry)) {
    Dim "Current effective registry: $effectiveRegistry"
  }

  if ($conflicts -eq 0) {
    Success "No conflicting npm configurations found."
  } else {
    Write-Host ""
    Warn "$conflicts conflict(s) detected — the installer will attempt to override them."
  }

  return $conflicts
}

function Run-NpmViaCmd([string]$cmdLine) {
  if ($DryRun) {
    Info ("DRYRUN: " + $cmdLine)
    return
  }

  Add-ToLogBuffer "[cmd] $cmdLine"

  if ($EnableLog) {
    Info "Logging enabled. Log file: $LogPath"
  }

  # Use streaming output so the user sees progress in real-time
  $psi = New-Object System.Diagnostics.ProcessStartInfo
  $psi.FileName = "cmd.exe"
  $psi.Arguments = "/d /s /c " + $cmdLine
  $psi.RedirectStandardOutput = $true
  $psi.RedirectStandardError  = $true
  $psi.UseShellExecute = $false
  $psi.CreateNoWindow = $true

  $stdoutLines = [System.Collections.Generic.List[string]]::new()
  $stderrLines = [System.Collections.Generic.List[string]]::new()

  $p = New-Object System.Diagnostics.Process
  $p.StartInfo = $psi
  $p.EnableRaisingEvents = $true

  # Stream stdout line-by-line
  $outAction = {
    if ($null -ne $EventArgs.Data) {
      $Event.MessageData.OutLines.Add($EventArgs.Data)
      Write-Host $EventArgs.Data
    }
  }
  $errAction = {
    if ($null -ne $EventArgs.Data) {
      $Event.MessageData.ErrLines.Add($EventArgs.Data)
      Write-Host $EventArgs.Data -ForegroundColor DarkYellow
    }
  }

  $msgData = [PSCustomObject]@{ OutLines = $stdoutLines; ErrLines = $stderrLines }
  $outEvent = Register-ObjectEvent -InputObject $p -EventName OutputDataReceived -Action $outAction -MessageData $msgData
  $errEvent = Register-ObjectEvent -InputObject $p -EventName ErrorDataReceived  -Action $errAction -MessageData $msgData

  [void]$p.Start()
  $p.BeginOutputReadLine()
  $p.BeginErrorReadLine()
  $p.WaitForExit()

  # Give events a moment to flush
  Start-Sleep -Milliseconds 200

  Unregister-Event -SourceIdentifier $outEvent.Name
  Unregister-Event -SourceIdentifier $errEvent.Name

  # Feed output into rolling in-memory buffer
  foreach ($line in $stdoutLines) {
    if ($line) { Add-ToLogBuffer $line }
  }
  foreach ($line in $stderrLines) {
    if ($line) { Add-ToLogBuffer "[ERR] $line" }
  }

  # Write to log file if enabled
  if ($EnableLog) {
    $content = @(
      "=== DeepStudio install log ==="
      "Time: $(Get-Date -Format o)"
      "Package: $PackageName@latest"
      "VerboseInstall: $VerboseInstall"
      "Registry: $($script:ResolvedRegistry)/"
      "Command: $cmdLine"
      ""
      "---- STDOUT ----"
      ($stdoutLines -join "`r`n")
      ""
      "---- STDERR ----"
      ($stderrLines -join "`r`n")
      ""
      "ExitCode: $($p.ExitCode)"
    ) -join "`r`n"

    Set-Content -Path $LogPath -Value $content -Encoding UTF8
  }

  if ($p.ExitCode -ne 0) {
    $errMsg = "npm failed with exit code $($p.ExitCode)."
    if ($EnableLog) { $errMsg += " See log: $LogPath" }
    throw $errMsg
  }
}

# ---------------------------
# Azure CLI installation
# ---------------------------
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

# ---------------------------
# Azure CLI token acquisition
# ---------------------------
function Get-AzAccessToken {
  $azAvailable = [bool](Get-Command az -ErrorAction SilentlyContinue)

  if (-not $azAvailable) {
    Write-Host ""
    Write-Host "  ┌─────────────────────────────────────────────────────┐" -ForegroundColor Yellow
    Write-Host "  │  🔧 Azure CLI (az) is not installed                 │" -ForegroundColor Yellow
    Write-Host "  │  It is recommended for automatic token auth.        │" -ForegroundColor Yellow
    Write-Host "  └─────────────────────────────────────────────────────┘" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "  📥 " -NoNewline -ForegroundColor Cyan
    $installChoice = Read-Host "Install Azure CLI now via winget? [Y/n]"
    if ([string]::IsNullOrWhiteSpace($installChoice) -or $installChoice -match '^[Yy]') {
      $installed = Install-AzureCli
      if ($installed) {
        $azAvailable = $true
      } else {
        Dim "Continuing without Azure CLI."
      }
    } else {
      Dim "Skipping Azure CLI installation."
    }
  }

  if (-not $azAvailable) {
    Dim "Will fall back to manual PAT entry."
    return $null
  }

  Info "🔍 Azure CLI found. Checking login session..."

  try {
    $tokenJson = & az account get-access-token --resource "499b84ac-1321-427f-aa17-267ca6975798" --query "accessToken" -o tsv 2>&1
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($tokenJson)) {
      if ($VerboseInstall -and $tokenJson) { Dim "az output: $tokenJson" }
      Warn "No valid az login session found."
      Write-Host ""
      Info "🔐 Please log in to Azure to continue..."
      Write-Host ""
      & az login
      if ($LASTEXITCODE -ne 0) {
        Warn "az login failed — will fall back to manual PAT entry."
        return $null
      }
      $tokenJson = & az account get-access-token --resource "499b84ac-1321-427f-aa17-267ca6975798" --query "accessToken" -o tsv 2>&1
      if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($tokenJson)) {
        Warn "Still unable to get token after login — will fall back to manual PAT entry."
        return $null
      }
    }
    $token = $tokenJson.Trim()
    Success "Obtained temporary access token from Azure CLI."
    return $token
  }
  catch {
    Warn "Failed to get token from Azure CLI: $($_.Exception.Message)"
    Dim "Will fall back to manual PAT entry."
    return $null
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
Write-Host "  └─────────────────────────────────────────┘" -ForegroundColor DarkCyan
Write-Host ""

Show-ProxyDiagnostics
$configConflicts = Show-NpmConfigConflicts

# Get registry (always use default; org override only via DEEPSTUDIO_REGISTRY env var)
$registryInput = $RegistryFromEnv
if ([string]::IsNullOrWhiteSpace($registryInput)) {
  $registryInput = $DefaultRegistry
}
$registry = Ensure-Registry $registryInput
$script:ResolvedRegistry = $registry

Dim "Registry: $(Mask-Url $registry)"
Write-Host ""

# derive auth prefix (npmrc style)
$uri = [Uri]$registry
$authPrefix = "//" + $uri.Host + $uri.AbsolutePath.TrimEnd("/") + "/"

# Try Azure CLI token first, fall back to manual PAT
$pat = Get-AzAccessToken

if ([string]::IsNullOrWhiteSpace($pat)) {
  Write-Host ""
  Write-Host "  ┌─────────────────────────────────────────────────────┐" -ForegroundColor Yellow
  Write-Host "  │  🔑 Manual PAT required                            │" -ForegroundColor Yellow
  Write-Host "  │  Create one at:                                     │" -ForegroundColor Yellow
  Write-Host "  │  https://dev.azure.com/ > User Settings > PATs      │" -ForegroundColor Yellow
  Write-Host "  │  Scope: Packaging > Read                            │" -ForegroundColor Yellow
  Write-Host "  └─────────────────────────────────────────────────────┘" -ForegroundColor Yellow
  Write-Host ""

  $patRaw = Read-Secure "  🔑 Enter Azure DevOps PAT (Packaging:Read)"
  if ([string]::IsNullOrWhiteSpace($patRaw)) { throw "PAT is empty." }

  $pat = $patRaw.Trim()
  $patRaw = $null
} else {
  Success "Using temporary token from Azure CLI (no PAT creation needed)."
}

Write-Host ""
Dim ("Token (masked): " + (Mask-Token $pat))
Write-Host ""

$logLevel = if ($VerboseInstall) { "verbose" } else { "notice" }
$tmpNpmrc = New-TempNpmrc

if ($VerboseInstall) {
  $env:NPM_CONFIG_LOGLEVEL = "verbose"
  $env:NPM_CONFIG_PROGRESS = "false"
}

# Save and clear env vars that can override --registry
$script:SavedNpmConfigRegistry = $env:NPM_CONFIG_REGISTRY
$env:NPM_CONFIG_REGISTRY = $null

try {
  Info "📄 Preparing isolated npm config..."
  Write-IsolatedNpmrc -path $tmpNpmrc -registry $registry -authPrefix $authPrefix -pat $pat

  # --userconfig overrides ~/.npmrc, --globalconfig overrides {prefix}/etc/npmrc
  # NUL for globalconfig so npm doesn't see a conflicting global config
  # NOTE: --registry is intentionally omitted from the CLI flags.
  # It is already set inside the isolated npmrc. Passing it on the CLI as well
  # puts the registry at CLI precedence while _authToken stays at userconfig
  # precedence, and npm 11 may refuse to send userconfig auth for a CLI-level
  # registry.  Keeping both in the same npmrc avoids this precedence split.
  $installCmd = 'npm install -g "{0}@latest" --loglevel {1} --userconfig "{2}" --globalconfig NUL' -f $PackageName, $logLevel, $tmpNpmrc

  if ($VerboseInstall) {
    Dim ("Command: " + $installCmd)
  }
  Write-Host ""
  Info "📦 Installing $PackageName@latest ..."
  Write-Host ""

  Run-NpmViaCmd $installCmd

  if (-not $DryRun) {
    $verifyCmd = 'npm list -g --depth=0 "{0}"' -f $PackageName
    Run-NpmViaCmd $verifyCmd
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
    Write-Host "  🏢 " -NoNewline -ForegroundColor Cyan
    $adoOrg = Read-Host "Enter your ADO org name [microsoft]"
    if ([string]::IsNullOrWhiteSpace($adoOrg)) { $adoOrg = "microsoft" }
    $env:DEEPSTUDIO_ADO_ORG = $adoOrg
    Dim "ADO org: $adoOrg"
    Write-Host ""
    Write-Host "  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Magenta
    Success "Launching $PackageName (press Ctrl+C to stop)..."
    Write-Host "  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Magenta
    Write-Host ""
    if ($DryRun) {
      Dim "DRYRUN: would run $PackageName (ADO org: $adoOrg)"
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
  Warn "Common causes for 401/403:"
  Dim "  • Azure CLI token expired (try: az login)"
  Dim "  • PAT missing Packaging:Read scope"
  Dim "  • PAT pasted with extra whitespace"
  Dim "  • Wrong registry URL"
  Dim "  • No permission to the Azure Artifacts feed"
  Dim "  • Corporate proxy/SSL interception issues"
  Dim "  • npm 11+: built-in npmrc at <npm-prefix>/node_modules/npm/npmrc has conflicting auth"
  Dim "    (check: npm config get prefix  then inspect <prefix>/node_modules/npm/npmrc)"

  Write-Host ""
  Warn "Common causes for 404:"
  Dim "  • A project-level .npmrc in the current directory set a different registry"
  Dim "  • NPM_CONFIG_REGISTRY env var overrode the --registry flag"
  Dim "  • The ADO feed is missing the npmjs.org upstream source"
  Dim "  • Try running from a clean directory (e.g. cd $env:TEMP)"

  if ($VerboseInstall) {
    Write-Host ""
    Dim "---- Full exception ----"
    Dim $_.Exception.ToString()
    Dim "------------------------"
  } else {
    Write-Host ""
    Warn "Tip: re-run with verbose:"
    Dim "  `$env:DEEPSTUDIO_VERBOSE='1'; `$env:DEEPSTUDIO_LOG='1'; irm <url> | iex"
  }

  if ($EnableLog -and -not $DryRun) {
    Write-Host ""
    Warn "Log saved to: $LogPath"
  } else {
    Dump-LogBuffer
  }

  throw
}
finally {
  # Restore env var
  if ($script:SavedNpmConfigRegistry) {
    $env:NPM_CONFIG_REGISTRY = $script:SavedNpmConfigRegistry
  }
  if (-not $DryRun) {
    Remove-Item $tmpNpmrc -Force -ErrorAction SilentlyContinue
  }
  if ($VerboseInstall) {
    Remove-Item Env:\NPM_CONFIG_LOGLEVEL -ErrorAction SilentlyContinue
    Remove-Item Env:\NPM_CONFIG_PROGRESS -ErrorAction SilentlyContinue
  }
  Write-Host ""
  Success "Cleanup complete."
  Write-Host ""
}
