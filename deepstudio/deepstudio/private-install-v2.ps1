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
    "                 |_|          I n s t a l l e r  v2    "
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

function Require-Node {
  $nodeAvailable = [bool](Get-Command node -ErrorAction SilentlyContinue)
  $MinNodeVersion = 22

  if (-not $nodeAvailable) {
    Write-Host ""
    Write-Host "  ┌─────────────────────────────────────────────────────┐" -ForegroundColor Yellow
    Write-Host "  │  ⚙️  Node.js is not installed                       │" -ForegroundColor Yellow
    Write-Host "  │  Node.js >= $MinNodeVersion is required to continue.             │" -ForegroundColor Yellow
    Write-Host "  └─────────────────────────────────────────────────────┘" -ForegroundColor Yellow
    Write-Host ""

    if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
      Fail "winget not found — cannot auto-install Node.js."
      Info "Please install Node.js manually:"
      Info "👉 https://nodejs.org/en/download"
      Write-Host ""
      exit 1
    }

    Write-Host "  📥 " -NoNewline -ForegroundColor Cyan
    $installChoice = Read-Host "Install Node.js via winget? [Y/n]"
    if ([string]::IsNullOrWhiteSpace($installChoice) -or $installChoice -match '^[Yy]') {
      Info "📥 Installing Node.js via winget..."
      Write-Host ""
      try {
        & winget install --id OpenJS.NodeJS.LTS --accept-source-agreements --accept-package-agreements
        if ($LASTEXITCODE -ne 0) {
          Fail "winget install returned exit code $LASTEXITCODE."
          exit 1
        }
        # Refresh PATH so node/npm are available in the current session
        $env:PATH = [System.Environment]::GetEnvironmentVariable("PATH", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("PATH", "User")
        if (Get-Command node -ErrorAction SilentlyContinue) {
          Success "Node.js installed successfully."
          $nodeAvailable = $true
        } else {
          Warn "Node.js installed but 'node' not found in PATH. You may need to restart your terminal."
          exit 1
        }
      }
      catch {
        Fail "Failed to install Node.js: $($_.Exception.Message)"
        exit 1
      }
    } else {
      Fail "Node.js is required. Please install it and try again."
      Info "👉 https://nodejs.org/en/download"
      Write-Host ""
      exit 1
    }
  }

  # Check Node.js version — must be >= MinNodeVersion
  if ($nodeAvailable) {
    try {
      $versionStr = & node --version 2>$null
      # node --version returns e.g. "v22.1.0"
      $versionStr = $versionStr -replace '^v', ''
      $major = [int]($versionStr.Split('.')[0])
      Dim "Node.js version: v$versionStr"

      if ($major -lt $MinNodeVersion) {
        Write-Host ""
        Write-Host "  ┌─────────────────────────────────────────────────────┐" -ForegroundColor Yellow
        Write-Host "  │  ⬆️  Node.js upgrade recommended                    │" -ForegroundColor Yellow
        Write-Host "  │  Current: v$versionStr" -NoNewline -ForegroundColor Yellow
        # Pad to fill the box
        $pad = 39 - "Current: v$versionStr".Length
        if ($pad -lt 0) { $pad = 0 }
        Write-Host (" " * $pad + "│") -ForegroundColor Yellow
        Write-Host "  │  Required: >= v$MinNodeVersion.0.0                              │" -ForegroundColor Yellow
        Write-Host "  └─────────────────────────────────────────────────────┘" -ForegroundColor Yellow
        Write-Host ""

        if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
          Fail "winget not found — cannot auto-upgrade Node.js."
          Info "Please upgrade Node.js manually:"
          Info "👉 https://nodejs.org/en/download"
          Write-Host ""
          exit 1
        }

        Write-Host "  ⬆️  " -NoNewline -ForegroundColor Cyan
        $upgradeChoice = Read-Host "Upgrade Node.js to latest LTS via winget? [Y/n]"
        if ([string]::IsNullOrWhiteSpace($upgradeChoice) -or $upgradeChoice -match '^[Yy]') {
          Info "📥 Upgrading Node.js via winget..."
          Write-Host ""
          try {
            & winget upgrade --id OpenJS.NodeJS.LTS --accept-source-agreements --accept-package-agreements
            if ($LASTEXITCODE -ne 0) {
              Fail "winget upgrade returned exit code $LASTEXITCODE."
              exit 1
            }
            # Refresh PATH
            $env:PATH = [System.Environment]::GetEnvironmentVariable("PATH", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("PATH", "User")
            $newVersionStr = (& node --version 2>$null) -replace '^v', ''
            $newMajor = [int]($newVersionStr.Split('.')[0])
            if ($newMajor -ge $MinNodeVersion) {
              Success "Node.js upgraded to v$newVersionStr."
            } else {
              Warn "Node.js is now v$newVersionStr but still below v$MinNodeVersion. You may need to restart your terminal."
              exit 1
            }
          }
          catch {
            Fail "Failed to upgrade Node.js: $($_.Exception.Message)"
            exit 1
          }
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

  # Final check: npm must be available
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
  # Use _authToken to avoid conflicts with user's existing .npmrc and avoid base64 encoding pitfalls.
  # NOTE: This writes token into a temporary file; we delete it in finally.
  $content = @"
registry=$registry/
${authPrefix}:_authToken=$pat
"@

  if ($DryRun) {
    Info "DRYRUN: write isolated npmrc to $path"
    Info ("DRYRUN: npmrc content (token masked):`nregistry=$registry/`n${authPrefix}:_authToken=$(Mask-Token $pat)")
    return
  }

  # ASCII is safest for .npmrc formatting
  Set-Content -Path $path -Value $content -Encoding ASCII
}

function Run-NpmViaCmd([string]$cmdLine) {
  if ($DryRun) {
    Info ("DRYRUN: " + $cmdLine)
    return
  }

  if (-not $EnableLog) {
    & cmd.exe /d /s /c $cmdLine
    if ($LASTEXITCODE -ne 0) {
      throw "npm failed with exit code $LASTEXITCODE. (Tip: set DEEPSTUDIO_LOG=1 to capture full logs.)"
    }
    return
  }

  Info "Logging enabled. Log file: $LogPath"
  Info ("Command via cmd.exe: " + $cmdLine)

  $psi = New-Object System.Diagnostics.ProcessStartInfo
  $psi.FileName = "cmd.exe"
  $psi.Arguments = "/d /s /c " + $cmdLine
  $psi.RedirectStandardOutput = $true
  $psi.RedirectStandardError  = $true
  $psi.UseShellExecute = $false
  $psi.CreateNoWindow = $true

  $p = New-Object System.Diagnostics.Process
  $p.StartInfo = $psi
  [void]$p.Start()

  $stdout = $p.StandardOutput.ReadToEnd()
  $stderr = $p.StandardError.ReadToEnd()
  $p.WaitForExit()

  if ($stdout) { Write-Host $stdout }
  if ($stderr) { Write-Host $stderr }

  $content = @(
    "=== DeepStudio install log ==="
    "Time: $(Get-Date -Format o)"
    "Package: $PackageName@latest"
    "VerboseInstall: $VerboseInstall"
    "Registry: $registry/"
    "Command: $cmdLine"
    ""
    "---- STDOUT ----"
    $stdout
    ""
    "---- STDERR ----"
    $stderr
    ""
    "ExitCode: $($p.ExitCode)"
  ) -join "`r`n"

  Set-Content -Path $LogPath -Value $content -Encoding UTF8

  if ($p.ExitCode -ne 0) {
    throw "npm failed with exit code $($p.ExitCode). See log: $LogPath"
  }
}

# ---------------------------
# Azure CLI installation
# ---------------------------
function Install-AzureCli {
  # Attempt to install Azure CLI via winget
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
    # Refresh PATH so az is available in the current session
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
  # Try to obtain a temporary access token via Azure CLI for Azure DevOps.
  # The resource ID 499b84ac-1321-427f-aa17-267ca6975798 is the well-known
  # resource identifier for Azure DevOps (Azure Artifacts / Packaging).
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
      # Retry after login
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

# Get registry: env > construct from org name using default template
$registryInput = $RegistryFromEnv
if ([string]::IsNullOrWhiteSpace($registryInput)) {
  Write-Host "  🏢 " -NoNewline -ForegroundColor Cyan
  $adoOrg = Read-Host "Enter ADO org name [microsoft]"
  if ([string]::IsNullOrWhiteSpace($adoOrg)) { $adoOrg = "microsoft" }
  # Construct registry URL from org name using the base64 template
  # Template: https://{org}.pkgs.visualstudio.com/OS/_packaging/DeepStudio/npm/registry/
  $registryInput = $DefaultRegistry -replace "microsoft\.pkgs", "$adoOrg.pkgs"
  Dim "Using org: $adoOrg"
}
$registry = Ensure-Registry $registryInput

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

# npm install args / loglevel
$logLevel = if ($VerboseInstall) { "verbose" } else { "notice" }

# Create isolated npmrc to avoid user's existing ~/.npmrc conflicts
$tmpNpmrc = New-TempNpmrc

# Optional: extra debug signal for npm
if ($VerboseInstall) {
  $env:NPM_CONFIG_LOGLEVEL = "verbose"
  $env:NPM_CONFIG_PROGRESS = "false"
}

try {
  Info "📄 Preparing isolated npm config..."
  Write-IsolatedNpmrc -path $tmpNpmrc -registry $registry -authPrefix $authPrefix -pat $pat

  # Build command line (use cmd.exe to avoid PowerShell alias/function issues with npm)
  # IMPORTANT: Use --userconfig so npm only reads our isolated temp npmrc for this run.
  $installCmd = 'npm install -g "{0}@latest" --registry "{1}/" --loglevel {2} --userconfig "{3}"' -f $PackageName, $registry, $logLevel, $tmpNpmrc

  if ($VerboseInstall) {
    Dim ("Command: " + $installCmd)
  }
  Write-Host ""
  Info "📦 Installing $PackageName@latest ..."
  Write-Host ""

  Run-NpmViaCmd $installCmd

  # Extra safety: verify it's actually installed (still using isolated userconfig is ok)
  if (-not $DryRun) {
    $verifyCmd = 'npm list -g --depth=0 "{0}" --userconfig "{1}"' -f $PackageName, $tmpNpmrc
    Run-NpmViaCmd $verifyCmd
  }

  Write-Host ""
  Write-Host "  ┌─────────────────────────────────────────────────┐" -ForegroundColor Green
  Write-Host "  │  🎉 $PackageName@latest installed successfully! │" -ForegroundColor Green
  Write-Host "  └─────────────────────────────────────────────────┘" -ForegroundColor Green
  Write-Host ""

  # Ask user whether to start deepstudio-server
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
  Warn "Common causes for 401/403:"
  Dim "  • Azure CLI token expired (try: az login)"
  Dim "  • PAT missing Packaging:Read scope"
  Dim "  • PAT pasted with extra whitespace"
  Dim "  • Wrong registry URL"
  Dim "  • No permission to the Azure Artifacts feed"
  Dim "  • Corporate proxy/SSL interception issues"

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
  }

  throw
}
finally {
  # Clean up temp npmrc and env vars
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
