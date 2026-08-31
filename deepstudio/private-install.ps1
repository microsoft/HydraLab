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
  # NOTE: This writes PAT into a temporary file; we delete it in finally.
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

# Read PAT securely then TRIM it (fix common whitespace/newlines)
$patRaw = Read-Secure "Enter Azure DevOps PAT (Packaging:Read)"
if ([string]::IsNullOrWhiteSpace($patRaw)) { throw "PAT is empty." }

$pat = $patRaw.Trim()
$patRaw = $null

Info ""
Info ("PAT (masked): " + (Mask-Token $pat))
Info "Tip: if masked prefix/suffix looks wrong, you likely pasted extra chars/spaces."
Info ""

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
  Info "Preparing isolated npm config (ignores your existing .npmrc)..."
  Write-IsolatedNpmrc -path $tmpNpmrc -registry $registry -authPrefix $authPrefix -pat $pat

  # Build command line (use cmd.exe to avoid PowerShell alias/function issues with npm)
  # IMPORTANT: Use --userconfig so npm only reads our isolated temp npmrc for this run.
  $installCmd = 'npm install -g "{0}@latest" --registry "{1}/" --loglevel {2} --userconfig "{3}"' -f $PackageName, $registry, $logLevel, $tmpNpmrc

  Info ""
  Info ("Command: " + $installCmd)
  Info ""

  Run-NpmViaCmd $installCmd

  # Extra safety: verify it's actually installed (still using isolated userconfig is ok)
  if (-not $DryRun) {
    $verifyCmd = 'npm list -g --depth=0 "{0}" --userconfig "{1}"' -f $PackageName, $tmpNpmrc
    Run-NpmViaCmd $verifyCmd
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
    Warn "  `$env:DEEPSTUDIO_VERBOSE='1'; `$env:DEEPSTUDIO_LOG='1'; irm <url> | iex"
  }

  if ($EnableLog -and -not $DryRun) {
    Warn ""
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
  Info ""
  Info "✅ Cleanup complete."
  Info ""
}
