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

# ---------------------------
# Helpers
# ---------------------------
function Info([string]$msg) { Write-Host $msg }
function Warn([string]$msg) { Write-Host $msg }
function Fail([string]$msg) { Write-Host $msg }

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

function Get-NpmPath {
  $cmd = Get-Command npm -ErrorAction Stop
  # On Windows this is typically ...\npm.cmd
  return $cmd.Source
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

function SetCfg([string]$k, [string]$v) {
  if ($DryRun) { Info "DRYRUN: npm config set --global $k <value>"; return }
  npm config set --global $k $v | Out-Null
  if ($LASTEXITCODE -ne 0) { throw "npm config set failed ($k). exit=$LASTEXITCODE" }
}

function DelCfg([string]$k) {
  if ($DryRun) { Info "DRYRUN: npm config delete --global $k"; return }
  try { npm config delete --global $k | Out-Null } catch {}
}

# IMPORTANT: Do NOT name parameters as $args (PowerShell automatic variable).
function Run-NpmInstall([string[]]$npmArgs) {
  $cmdLine = "npm " + ($npmArgs -join " ")

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
# Start
# ---------------------------
Require-Npm

Info ""
Info "=== DeepStudio npm installer ==="
Info "Package: $PackageName@latest"
Info ("VerboseInstall: " + $(if ($VerboseInstall) { "ON" } else { "OFF" }))
Info ("DryRun: " + $(if ($DryRun) { "ON" } else { "OFF" }))
Info ("LogToFile: " + $(if ($EnableLog) { "ON ($LogPath)" } else { "OFF" }))
Info ""

# Get registry: env > prompt
$registryInput = $RegistryFromEnv
if ([string]::IsNullOrWhiteSpace($registryInput)) {
  $registryInput = Read-Host "Enter npm registry URL (ex: https://xxx.pkgs.xxx.com/xxx/_packaging/xxx/npm/registry/)"
}
$registry = Ensure-Registry $registryInput

# derive auth prefix (npmrc style)
$uri = [Uri]$registry
$authPrefix = "//" + $uri.Host + $uri.AbsolutePath + "/"

# Read PAT securely then TRIM it (fix common 401 due to whitespace/newlines)
$patRaw = Read-Secure "Enter Azure DevOps PAT (Packaging:Read)"
if ([string]::IsNullOrWhiteSpace($patRaw)) { throw "PAT is empty." }

$pat = $patRaw.Trim()
$patRaw = $null

Info ""
Info ("PAT (masked): " + (Mask-Token $pat))
Info "Tip: if masked prefix/suffix looks wrong, you likely pasted extra chars/spaces."
Info ""

$patB64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($pat))
$pat = $null

# npm install args
$logLevel = if ($VerboseInstall) { "verbose" } else { "notice" }
$npmInstallArgs = @(
  "install", "-g", "$PackageName@latest",
  "--registry", "$registry/",
  "--loglevel", $logLevel
)

# Optional: extra debug signal for npm
if ($VerboseInstall) {
  $env:NPM_CONFIG_LOGLEVEL = "verbose"
  $env:NPM_CONFIG_PROGRESS = "false"
}

try {
  Info "Configuring npm auth for this registry..."
  SetCfg "registry" "$registry/"
  SetCfg "${authPrefix}:username" "ms"
  SetCfg "${authPrefix}:_password" $patB64
  SetCfg "${authPrefix}:email" "npm@example.com"
  # NOTE: always-auth intentionally not set (deprecated/invalid in modern npm)

  Info ""
  Info ("Command: npm " + ($npmInstallArgs -join " "))
  Info ""

  Run-NpmInstall $npmInstallArgs

  # Extra safety: verify it's actually installed
  if (-not $DryRun) {
    npm list -g --depth=0 "$PackageName" | Out-Null
    if ($LASTEXITCODE -ne 0) {
      throw "Install command finished but package not found in global npm list. Something is wrong."
    }
  }

  Info ""
  Info "✅ Installed $PackageName@latest successfully."
  Info "You can now run: $PackageName"
}
catch {
  Info ""
  Fail "❌ Installation failed."
  Fail ("Error: " + $_.Exception.Message)

  Warn ""
  Warn "Common causes for 401/403:"
  Warn "  - PAT missing Packaging:Read scope"
  Warn "  - PAT pasted with extra whitespace (we trimmed, but double-check the masked value)"
  Warn "  - Wrong registry URL (must be the npm/registry endpoint)"
  Warn "  - No permission to the Azure Artifacts feed"
  Warn "  - Corporate proxy/SSL interception issues"

  if ($VerboseInstall) {
    Info ""
    Info "---- Full exception ----"
    Info $_.Exception.ToString()
    Info "------------------------"
  } else {
    Warn ""
    Warn "Tip: re-run with verbose:"
    Warn "  `$env:DEEPSTUDIO_VERBOSE='1'; irm <url> | iex"
  }

  if ($EnableLog -and -not $DryRun) {
    Warn ""
    Warn "Log saved to: $LogPath"
  }

  throw
}
finally {
  Info ""
  Info "Cleaning up npm global config..."
  DelCfg "registry"
  DelCfg "${authPrefix}:username"
  DelCfg "${authPrefix}:_password"
  DelCfg "${authPrefix}:email"

  if ($VerboseInstall) {
    Remove-Item Env:\NPM_CONFIG_LOGLEVEL -ErrorAction SilentlyContinue
    Remove-Item Env:\NPM_CONFIG_PROGRESS -ErrorAction SilentlyContinue
  }

  Info "✅ Cleanup complete."
  Info ""
}
