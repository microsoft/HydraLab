# =============================================================================
# X-Tester MCP installer
#
# Delivery model:
#   This script is designed to be run directly from a URL via `irm <url> | iex`
#   (or `iwr <url> | iex`). It therefore MUST stay fully self-contained: do NOT
#   add dot-sourcing of sibling files, module imports from the repo, relative
#   `$PSScriptRoot` references, or any other dependency that does not exist when
#   the script is piped into PowerShell from the network. Everything the
#   installer needs (Python detection, venv setup, client registration, etc.)
#   has to be inlined in this single file.
#
# Privacy & compliance:
#   Because this runs on end-user machines, be mindful of privacy compliance.
#   Do NOT collect, log, transmit, or persist any personal data, telemetry,
#   credentials, tokens, or machine identifiers. Keep output limited to local
#   install progress, and only contact the explicitly configured package feeds.
# =============================================================================

param(
  [ValidateSet("private-feed", "local")]
  [string]$Source = "private-feed",

  [ValidateSet("copilot", "claude-desktop", "claude-code", "vscode", "all")]
  [string[]]$Client = @("copilot"),

  [string]$Version,

  [string]$ServerName = "xtester",

  [string]$VenvPath = (Join-Path $env:LOCALAPPDATA "XTester\mcp"),

  [string]$LocalPath,

  [string]$FeedUrl = "https://microsoft.pkgs.visualstudio.com/OS/_packaging/DeepStudio/pypi/simple/",

  [string]$ExtraIndexUrl = "https://pypi.org/simple",

  [switch]$Force,

  [switch]$SkipClientInstall
)

$ErrorActionPreference = "Stop"

$Script:InstallerVersion = "1.10.0"
$Script:AgencyXTesterPluginPrompted = $false

try {
  [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
  $OutputEncoding = [System.Text.Encoding]::UTF8
} catch { }

function Info([string]$Message) { Write-Host "  $Message" -ForegroundColor Cyan }
function Success([string]$Message) { Write-Host "  OK  $Message" -ForegroundColor Green }
function Warn([string]$Message) { Write-Host "  WARN $Message" -ForegroundColor Yellow }
function Fail([string]$Message) { Write-Host "  ERR $Message" -ForegroundColor Red }

function Invoke-Step([string]$Description, [scriptblock]$Script) {
  Info $Description
  & $Script
  Success $Description
}

function Invoke-External([string]$FilePath, [string[]]$Arguments) {
  & $FilePath @Arguments
  if ($LASTEXITCODE -ne 0) {
    throw "$FilePath $($Arguments -join ' ') failed with exit code $LASTEXITCODE"
  }
}

# winget renders its download spinner with `\r` redraws. When PowerShell wraps
# stdout (any `2>&1 | ...` pipeline counts as a non-TTY consumer) every frame
# is flushed with a newline, producing a vertical column of `-`,`\\`,`|`,`/`
# (and sometimes block glyphs) in the console. Suppress those frames so the
# operator only sees real status lines.
function Test-IsWingetSpinnerLine([string]$Line) {
  if ($null -eq $Line) { return $true }
  $trimmed = $Line.Trim()
  if ([string]::IsNullOrEmpty($trimmed)) { return $false }
  # Pure spinner / progress glyphs (single char per line is the worst case).
  if ($trimmed -match '^[\-\\|/\u2580-\u259F\u2588-\u259B\u2591\u2592\u2593\u2588]+$') { return $true }
  # Spinner glyphs followed by a percentage like ` -  42 %`.
  if ($trimmed -match '^[\-\\|/\u2580-\u259F\u2588-\u259B\u2591\u2592\u2593\u2588\s]+\d+\s*%\s*$') { return $true }
  # Spinner glyphs followed by a `12.3 MB / 45.6 MB` style download counter.
  if ($trimmed -match '^[\-\\|/\u2580-\u259F\u2588-\u259B\u2591\u2592\u2593\u2588\s]+\d[\d.,]*\s*(B|KB|MB|GB)\s*/\s*\d') { return $true }
  return $false
}

function Invoke-WingetQuiet([string]$WingetPath, [string[]]$Arguments) {
  & $WingetPath @Arguments 2>&1 | ForEach-Object {
    $line = [string]$_
    if ([string]::IsNullOrWhiteSpace($line)) { return }
    if (Test-IsWingetSpinnerLine $line) { return }
    Write-Host "    $line"
  }
}

function Invoke-WithCleanPythonPath([scriptblock]$Script) {
  $previousPythonPath = $env:PYTHONPATH
  # pip's text spinner relies on \r to overwrite a single line, but when stdout
  # is wrapped by PowerShell (especially in non-TTY hosts like the VS Code
  # PowerShell terminal) each frame is flushed with a newline, producing a
  # vertical column of '-' '\\' '|' '/' glyphs. Force pip into its no-spinner
  # output mode for the duration of the install.
  $previousPipProgressBar = $env:PIP_PROGRESS_BAR
  $previousPipNoInput = $env:PIP_NO_INPUT
  $previousPipVersionCheck = $env:PIP_DISABLE_PIP_VERSION_CHECK
  $previousPipKeyringProvider = $env:PIP_KEYRING_PROVIDER
  try {
    Remove-Item Env:\PYTHONPATH -ErrorAction SilentlyContinue
    $env:PIP_PROGRESS_BAR = "off"
    $env:PIP_NO_INPUT = "1"
    $env:PIP_DISABLE_PIP_VERSION_CHECK = "1"
    # PIP_NO_INPUT=1 makes pip skip its interactive keyring path, which the
    # Azure Artifacts credential provider (artifacts-keyring) depends on, so
    # private-feed requests would fail to authenticate. Forcing the "import"
    # keyring provider makes pip load keyring in-process (no prompt, MSAL
    # silent in practice), restoring feed auth for both version discovery and
    # the actual install while staying fully non-interactive.
    $env:PIP_KEYRING_PROVIDER = "import"
    & $Script
  }
  finally {
    if ($null -ne $previousPythonPath) {
      $env:PYTHONPATH = $previousPythonPath
    }
    if ($null -eq $previousPipProgressBar) { Remove-Item Env:\PIP_PROGRESS_BAR -ErrorAction SilentlyContinue }
    else { $env:PIP_PROGRESS_BAR = $previousPipProgressBar }
    if ($null -eq $previousPipNoInput) { Remove-Item Env:\PIP_NO_INPUT -ErrorAction SilentlyContinue }
    else { $env:PIP_NO_INPUT = $previousPipNoInput }
    if ($null -eq $previousPipVersionCheck) { Remove-Item Env:\PIP_DISABLE_PIP_VERSION_CHECK -ErrorAction SilentlyContinue }
    else { $env:PIP_DISABLE_PIP_VERSION_CHECK = $previousPipVersionCheck }
    if ($null -eq $previousPipKeyringProvider) { Remove-Item Env:\PIP_KEYRING_PROVIDER -ErrorAction SilentlyContinue }
    else { $env:PIP_KEYRING_PROVIDER = $previousPipKeyringProvider }
  }
}

function Stop-XTesterMcpProcesses {
  Invoke-Step "Stopping running X-Tester MCP processes" {
    try {
      $processes = @(Get-CimInstance Win32_Process -ErrorAction Stop | Where-Object {
        $_.Name -ieq "xtester-mcp.exe" -or ($_.CommandLine -and $_.CommandLine -match '(^|[\\/\s"''])(xtester-mcp)(\.exe)?([\s"'']|$)')
      })
    }
    catch {
      Warn "Could not inspect running processes: $($_.Exception.Message)"
      return
    }

    if ($processes.Count -eq 0) {
      Info "No running xtester-mcp processes found."
      return
    }

    foreach ($entry in $processes) {
      $label = "$($entry.Name) pid=$($entry.ProcessId)"
      Info "Stopping $label"
      try {
        $process = Get-Process -Id $entry.ProcessId -ErrorAction Stop
        if ($process.CloseMainWindow()) {
          if ($process.WaitForExit(2000)) { continue }
        }
        Stop-Process -Id $entry.ProcessId -Force -ErrorAction Stop
      }
      catch {
        Warn "Could not stop ${label}: $($_.Exception.Message)"
      }
    }
  }
}

function Require-Command([string]$Name, [string]$InstallHint) {
  $cmd = Get-Command $Name -ErrorAction SilentlyContinue
  if ($cmd) { return $cmd.Source }
  Fail "$Name was not found on PATH."
  if ($InstallHint) { Info $InstallHint }
  exit 1
}

function ConvertTo-Hashtable($Value) {
  if ($null -eq $Value) { return $null }
  if ($Value -is [System.Collections.IDictionary]) { return $Value }
  if ($Value -is [System.Collections.IEnumerable] -and $Value -isnot [string]) {
    $items = @()
    foreach ($item in $Value) { $items += (ConvertTo-Hashtable $item) }
    return $items
  }
  if ($Value.GetType().Name -eq "PSCustomObject") {
    $table = [ordered]@{}
    foreach ($property in $Value.PSObject.Properties) {
      $table[$property.Name] = ConvertTo-Hashtable $property.Value
    }
    return $table
  }
  return $Value
}

function Remove-JsonComments([string]$Text) {
  # VS Code writes mcp.json as JSONC: it permits `//` / `/* */` comments and
  # trailing commas, neither of which Windows PowerShell 5.1's ConvertFrom-Json
  # accepts. Strip them defensively while respecting string literals so an
  # otherwise-valid VS Code file still parses. PowerShell 7+ tolerates comments
  # natively, so this is a no-op-equivalent safety net there.
  $sb = [System.Text.StringBuilder]::new()
  $inString = $false
  $escaped = $false
  $i = 0
  $len = $Text.Length
  while ($i -lt $len) {
    $ch = $Text[$i]
    $next = if ($i + 1 -lt $len) { $Text[$i + 1] } else { [char]0 }
    if ($inString) {
      [void]$sb.Append($ch)
      if ($escaped) { $escaped = $false }
      elseif ($ch -eq '\') { $escaped = $true }
      elseif ($ch -eq '"') { $inString = $false }
      $i++
      continue
    }
    if ($ch -eq '"') { $inString = $true; [void]$sb.Append($ch); $i++; continue }
    if ($ch -eq '/' -and $next -eq '/') {
      while ($i -lt $len -and $Text[$i] -ne "`n") { $i++ }
      continue
    }
    if ($ch -eq '/' -and $next -eq '*') {
      $i += 2
      while ($i + 1 -lt $len -and -not ($Text[$i] -eq '*' -and $Text[$i + 1] -eq '/')) { $i++ }
      $i += 2
      continue
    }
    [void]$sb.Append($ch)
    $i++
  }
  # Remove trailing commas before a closing } or ] (JSONC allows them).
  return ([regex]::Replace($sb.ToString(), ',(\s*[}\]])', '$1'))
}

function Read-JsonObject([string]$Path, [hashtable]$DefaultValue) {
  if (-not (Test-Path -LiteralPath $Path)) { return $DefaultValue }
  $raw = Get-Content -Raw -LiteralPath $Path
  if ([string]::IsNullOrWhiteSpace($raw)) { return $DefaultValue }
  try {
    return ConvertTo-Hashtable ($raw | ConvertFrom-Json)
  }
  catch {
    # Retry once after stripping JSONC comments / trailing commas, which is the
    # common reason a hand- or VS Code-authored config fails strict parsing.
    try {
      return ConvertTo-Hashtable ((Remove-JsonComments $raw) | ConvertFrom-Json)
    }
    catch {
      throw "The existing config file is not valid JSON and could not be parsed: $Path`n" +
            "  Parser error: $($_.Exception.Message)`n" +
            "  Fix or delete that file (e.g. reset it to '{}') and re-run the installer."
    }
  }
}

function Write-JsonObject([string]$Path, $Value) {
  $dir = Split-Path -Parent $Path
  if ($dir) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
  $Value | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $Path -Encoding UTF8
}

function Resolve-LocalPackagePath {
  if (-not $LocalPath) {
    throw "-Source local requires -LocalPath pointing at the XTester package directory (e.g. -LocalPath C:\path\to\DeepTest\XTester)."
  }
  if (-not (Test-Path -LiteralPath $LocalPath)) {
    throw "Local package path does not exist: $LocalPath"
  }
  return (Resolve-Path -LiteralPath $LocalPath).Path
}

# Python detection / installation helpers (inlined and self-contained).
  function Write-PyInfo([string]$Message) { Write-Host "  [python] $Message" -ForegroundColor Cyan }
  function Write-PyWarn([string]$Message) { Write-Host "  [python] WARN $Message" -ForegroundColor Yellow }
  function Write-PyFail([string]$Message) { Write-Host "  [python] ERR  $Message" -ForegroundColor Red }
  function Write-PyOk  ([string]$Message) { Write-Host "  [python] OK   $Message" -ForegroundColor Green }

  function Get-HostArchitecture {
    try { return [System.Runtime.InteropServices.RuntimeInformation]::OSArchitecture.ToString() }
    catch { return $env:PROCESSOR_ARCHITECTURE }
  }

  function Test-IsWindowsArm64Host {
    $isWindowsHost = $false
    try { $isWindowsHost = $IsWindows } catch { }
    if (-not $isWindowsHost -and $env:OS -eq "Windows_NT") { $isWindowsHost = $true }
    if (-not $isWindowsHost) { return $false }
    return (Get-HostArchitecture) -match "^(Arm64|AArch64)$"
  }

  function Get-PythonMachine([string]$FilePath, [string[]]$Arguments) {
    try {
      $output = & $FilePath @Arguments -c "import platform; print(platform.machine())" 2>&1
      if ($LASTEXITCODE -ne 0) { return $null }
      return ($output | Out-String).Trim()
    } catch { return $null }
  }

  function Test-IsUnsupportedWindowsArm64Python([string]$FilePath, [string[]]$Arguments) {
    if (-not (Test-IsWindowsArm64Host)) { return $false }
    $machine = Get-PythonMachine $FilePath $Arguments
    return $machine -match "^(ARM64|AARCH64)$"
  }

  function Test-PythonExecutable([string]$FilePath, [string[]]$Arguments) {
    if (-not $FilePath) { return $null }
    $label = if ($Arguments -and $Arguments.Count -gt 0) { "$FilePath $($Arguments -join ' ')" } else { "$FilePath" }
    try { $output = & $FilePath @Arguments --version 2>&1 }
    catch { Write-PyWarn "Skipping ${label}: launcher threw $($_.Exception.Message)"; return $null }
    if ($LASTEXITCODE -ne 0) { Write-PyWarn "Skipping ${label}: '--version' exited with code $LASTEXITCODE"; return $null }
    $text = ($output | Out-String).Trim()
    if ([string]::IsNullOrWhiteSpace($text)) { return $null }
    if ($text -notmatch "Python\s+(\d+)\.(\d+)\.(\d+)") { Write-PyWarn "Skipping ${label}: unrecognised version output '$text'"; return $null }
    $major = [int]$Matches[1]; $minor = [int]$Matches[2]
    if ($major -lt 3 -or ($major -eq 3 -and $minor -lt 11)) { Write-PyWarn "Skipping ${label}: $text is older than Python 3.11"; return $null }
    if (Test-IsUnsupportedWindowsArm64Python $FilePath $Arguments) {
      Write-PyWarn "${label}: native ARM64 Python detected. Modern wheels for cryptography/keyring/artifacts-keyring exist on win_arm64; if pip later falls back to a source build, re-run with x64 Python 3.12."
    }
    try {
      $sysExe = & $FilePath @Arguments -c "import sys; print(sys.executable)" 2>&1
      if ($LASTEXITCODE -eq 0) {
        $sysExeText = ($sysExe | Out-String).Trim()
        if ($sysExeText -match "\\WindowsApps\\" -or $sysExeText -match "\\Packages\\PythonSoftwareFoundation\.Python\.") {
          Write-PyWarn "Skipping ${label}: Microsoft Store Python at '$sysExeText' redirects venv writes"
          return $null
        }
      }
    } catch { }
    return [pscustomobject]@{ FilePath = $FilePath; Arguments = $Arguments; Version = $text }
  }

  function Get-RegistryPythonInstalls {
    $results = @()
    $roots = @("HKCU:\Software\Python", "HKLM:\Software\Python", "HKLM:\Software\WOW6432Node\Python")
    foreach ($root in $roots) {
      if (-not (Test-Path -LiteralPath $root)) { continue }
      $companies = Get-ChildItem -LiteralPath $root -ErrorAction SilentlyContinue
      foreach ($company in $companies) {
        if ($company.PSChildName -ne "PythonCore") { continue }
        $tags = Get-ChildItem -LiteralPath $company.PSPath -ErrorAction SilentlyContinue
        foreach ($tag in $tags) {
          $installPathKey = Join-Path $tag.PSPath "InstallPath"
          if (-not (Test-Path -LiteralPath $installPathKey)) { continue }
          try {
            $props = Get-ItemProperty -LiteralPath $installPathKey -ErrorAction Stop
            $dir = $props."(default)"
            if (-not $dir) { $dir = $props."ExecutablePath" | Split-Path -Parent -ErrorAction SilentlyContinue }
            if ($dir -and (Test-Path -LiteralPath $dir)) {
              $exe = Join-Path $dir "python.exe"
              if (Test-Path -LiteralPath $exe) { $results += $exe }
            }
          } catch { }
        }
      }
    }
    return ($results | Select-Object -Unique)
  }

  function Find-PythonCandidate {
    $candidates = @()
    $isRealPython = {
      param($cmd)
      if (-not $cmd) { return $false }
      if ($cmd.Source -and $cmd.Source -match "WindowsApps\\python(3)?\.exe$") { return $false }
      return $true
    }
    foreach ($name in @("python", "python3")) {
      $cmd = Get-Command $name -ErrorAction SilentlyContinue
      if ($cmd -and -not (& $isRealPython $cmd)) {
        Write-PyWarn "Skipping ${name}: '$($cmd.Source)' is the Microsoft Store WindowsApps stub"
      } elseif (& $isRealPython $cmd) {
        $candidates += ,@($cmd.Source, @())
      }
    }
    $py = Get-Command "py" -ErrorAction SilentlyContinue
    if ($py) { foreach ($flag in @("-3.12", "-3.11", "-3.13", "-3.14", "-3")) { $candidates += ,@($py.Source, @($flag)) } }
    foreach ($exe in (Get-RegistryPythonInstalls)) { $candidates += ,@($exe, @()) }
    $installRoots = @(
      (Join-Path $env:LOCALAPPDATA "Programs\Python"),
      (Join-Path $env:ProgramFiles "Python"),
      $env:ProgramFiles,
      ${env:ProgramFiles(x86)}
    )
    foreach ($root in $installRoots) {
      if (-not $root -or -not (Test-Path -LiteralPath $root)) { continue }
      $found = Get-ChildItem -LiteralPath $root -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match "^Python3(1[1-9]|[2-9][0-9])(x64|-x64)?$" } |
        Sort-Object Name -Descending
      foreach ($dir in $found) {
        $exe = Join-Path $dir.FullName "python.exe"
        if (Test-Path -LiteralPath $exe) { $candidates += ,@($exe, @()) }
      }
    }
    $wingetPackages = Join-Path $env:LOCALAPPDATA "Microsoft\WinGet\Packages"
    if (Test-Path -LiteralPath $wingetPackages) {
      $pythonPkgs = Get-ChildItem -LiteralPath $wingetPackages -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -like "Python.Python.3.*" } | Sort-Object Name -Descending
      foreach ($pkg in $pythonPkgs) {
        $exes = Get-ChildItem -LiteralPath $pkg.FullName -Recurse -Filter python.exe -ErrorAction SilentlyContinue -Depth 4
        foreach ($exe in $exes) { $candidates += ,@($exe.FullName, @()) }
      }
    }
    $wingetLinks = Join-Path $env:LOCALAPPDATA "Microsoft\WinGet\Links"
    if (Test-Path -LiteralPath $wingetLinks) {
      foreach ($name in @("python.exe", "python3.exe", "python3.12.exe", "python3.11.exe")) {
        $exe = Join-Path $wingetLinks $name
        if (Test-Path -LiteralPath $exe) { $candidates += ,@($exe, @()) }
      }
    }
    # Scoop user-scope installs (no admin, common workaround on machines where
    # winget is blocked by org policy).
    $scoopApps = Join-Path $env:USERPROFILE "scoop\apps"
    if (Test-Path -LiteralPath $scoopApps) {
      $scoopPythonDirs = Get-ChildItem -LiteralPath $scoopApps -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match "^python(3(1[1-9]|[2-9][0-9])?)?$" } | Sort-Object Name -Descending
      foreach ($dir in $scoopPythonDirs) {
        $exe = Join-Path $dir.FullName "current\python.exe"
        if (Test-Path -LiteralPath $exe) { $candidates += ,@($exe, @()) }
      }
    }
    foreach ($exe in @("C:\tools\python", "C:\tools\python3")) {
      $exePath = Join-Path $exe "python.exe"
      if (Test-Path -LiteralPath $exePath) { $candidates += ,@($exePath, @()) }
    }
    $chocoVersioned = Get-ChildItem -LiteralPath "C:\" -Directory -ErrorAction SilentlyContinue |
      Where-Object { $_.Name -match "^Python3(1[1-9]|[2-9][0-9])$" } | Sort-Object Name -Descending
    foreach ($dir in $chocoVersioned) {
      $exe = Join-Path $dir.FullName "python.exe"
      if (Test-Path -LiteralPath $exe) { $candidates += ,@($exe, @()) }
    }
    if ($candidates.Count -eq 0) {
      Write-PyWarn "No Python candidates were discovered."
      Write-PyInfo "Scanned: PATH (python, python3), py.exe launcher, HKCU/HKLM\Software\Python\PythonCore registry, %LOCALAPPDATA%\Programs\Python, %ProgramFiles%\Python, %ProgramFiles%, %ProgramFiles(x86)%, %LOCALAPPDATA%\Microsoft\WinGet\Packages\Python.Python.3.*, %LOCALAPPDATA%\Microsoft\WinGet\Links, %USERPROFILE%\scoop\apps\python*, C:\tools\python*, C:\Python3*."
    }
    foreach ($pair in $candidates) {
      $result = Test-PythonExecutable $pair[0] $pair[1]
      if ($result) { return $result }
    }
    return $null
  }

  function Install-PythonViaWinget {
    param([switch]$ForceX64SideBySide)
    $winget = Get-Command "winget" -ErrorAction SilentlyContinue
    if (-not $winget) { Write-PyWarn "winget is not available; cannot auto-install Python."; return $false }
    $stepLabel = if ($ForceX64SideBySide) { "Installing x64 Python 3.12 alongside existing ARM64 (Python.Python.3.12 --architecture x64 --force)" } else { "Installing Python 3.12 via winget (Python.Python.3.12)" }
    Write-PyInfo $stepLabel
    $wingetArgs = @("install", "-e", "--id", "Python.Python.3.12", "--scope", "user")
    if (Test-IsWindowsArm64Host) { $wingetArgs += @("--architecture", "x64") }
    if ($ForceX64SideBySide) {
      $sideBySideRoot = Join-Path $env:LOCALAPPDATA "Programs\Python\Python312x64"
      $wingetArgs += @("--force", "--location", $sideBySideRoot)
    }
    $wingetArgs += @("--accept-source-agreements", "--accept-package-agreements", "--silent", "--disable-interactivity")
    $script:WingetBlockedByPolicy = $false
    & $winget.Source @wingetArgs 2>&1 | ForEach-Object {
      $line = [string]$_
      if ([string]::IsNullOrWhiteSpace($line)) { return }
      if ($line -match '^[\s\u2580-\u259F\u2588-\u259B█▒▓░\-\\|/]+\s*\d+\s*%\s*$') { return }
      if ($line -match '^[\s\u2580-\u259F\u2588-\u259B█▒▓░\-\\|/]+\s*\d[\d.,]*\s*(KB|MB|GB)\s*/\s*\d') { return }
      if ($line -match '^[\s\-\\|/]+$') { return }
      if ($line -match 'Organization policies are preventing installation' -or $line -match 'exit code:\s*1625') {
        $script:WingetBlockedByPolicy = $true
      }
      Write-Host "    $line"
    }
    if ($LASTEXITCODE -ne 0) {
      Write-PyWarn "winget exited with code $LASTEXITCODE (this can mean 'already installed' or 'no upgrade needed' and is often safe to ignore)."
    }
    # Clean up partial Python install roots winget left behind on policy / 1603 abort.
    $localProgramsPythonRoot = Join-Path $env:LOCALAPPDATA "Programs\Python"
    if (Test-Path -LiteralPath $localProgramsPythonRoot) {
      Get-ChildItem -LiteralPath $localProgramsPythonRoot -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match "^Python3(1[1-9]|[2-9][0-9])(x64|-x64)?$" } |
        ForEach-Object {
          $rootPython = Join-Path $_.FullName "python.exe"
          if (-not (Test-Path -LiteralPath $rootPython)) {
            Write-PyWarn "Cleaning up partial Python install (no python.exe at root): $($_.FullName)"
            try { Remove-Item -LiteralPath $_.FullName -Recurse -Force -ErrorAction Stop }
            catch { Write-PyWarn "Could not remove $($_.FullName): $($_.Exception.Message)" }
          }
        }
    }
    $machinePath = [Environment]::GetEnvironmentVariable("Path", "Machine")
    $userPath = [Environment]::GetEnvironmentVariable("Path", "User")
    $extra = @()
    $wingetLinks = Join-Path $env:LOCALAPPDATA "Microsoft\WinGet\Links"
    if (Test-Path -LiteralPath $wingetLinks) { $extra += $wingetLinks }
    $localProgramsPython = Join-Path $env:LOCALAPPDATA "Programs\Python"
    if (Test-Path -LiteralPath $localProgramsPython) {
      Get-ChildItem -LiteralPath $localProgramsPython -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match "^Python3(1[1-9]|[2-9][0-9])(x64|-x64)?$" } |
        ForEach-Object { $extra += $_.FullName; $extra += (Join-Path $_.FullName "Scripts") }
    }
    $scoopShims = Join-Path $env:USERPROFILE "scoop\shims"
    if (Test-Path -LiteralPath $scoopShims) { $extra += $scoopShims }
    $env:Path = (@($machinePath, $userPath) + $extra | Where-Object { $_ }) -join ";"
    return $true
  }

  function Install-Scoop {
    $scoopShim = Join-Path $env:USERPROFILE "scoop\shims\scoop.ps1"
    if (Test-Path -LiteralPath $scoopShim) { return $true }
    Write-PyInfo "scoop is not installed. Bootstrapping scoop (user-scope, no admin required)..."
    try {
      $current = Get-ExecutionPolicy -Scope CurrentUser -ErrorAction SilentlyContinue
      if ($current -eq "Restricted" -or $current -eq "AllSigned" -or $current -eq "Undefined") {
        Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned -Force -ErrorAction Stop
      }
      $installer = "$env:TEMP\install-scoop.ps1"
      Invoke-WebRequest -UseBasicParsing -Uri "https://get.scoop.sh" -OutFile $installer -ErrorAction Stop
      & powershell -NoProfile -ExecutionPolicy Bypass -File $installer -RunAsAdmin:$false 2>&1 | ForEach-Object {
        $line = [string]$_
        if ([string]::IsNullOrWhiteSpace($line)) { return }
        Write-Host "    $line"
      }
      Remove-Item -LiteralPath $installer -Force -ErrorAction SilentlyContinue
    } catch {
      Write-PyWarn "Failed to bootstrap scoop: $($_.Exception.Message)"
      return $false
    }
    $scoopShims = Join-Path $env:USERPROFILE "scoop\shims"
    if (Test-Path -LiteralPath $scoopShims) {
      $env:Path = ($env:Path.TrimEnd(';') + ";" + $scoopShims)
    }
    return (Test-Path -LiteralPath $scoopShim)
  }

  function Install-PythonViaScoop {
    $scoop = Get-Command "scoop" -ErrorAction SilentlyContinue
    if (-not $scoop) {
      if (-not (Install-Scoop)) {
        Write-PyInfo "scoop is not available and could not be bootstrapped automatically. Install scoop from https://scoop.sh and re-run, or install Python manually."
        return $false
      }
      $scoop = Get-Command "scoop" -ErrorAction SilentlyContinue
      if (-not $scoop) {
        $scoopPs1 = Join-Path $env:USERPROFILE "scoop\shims\scoop.ps1"
        if (Test-Path -LiteralPath $scoopPs1) {
          $scoop = [pscustomobject]@{ Source = $scoopPs1 }
        } else { return $false }
      }
    }
    Write-PyInfo "Falling back to scoop: scoop install python (user-scope, no admin required)"
    & $scoop.Source install python 2>&1 | ForEach-Object {
      $line = [string]$_
      if ([string]::IsNullOrWhiteSpace($line)) { return }
      Write-Host "    $line"
    }
    if ($LASTEXITCODE -ne 0) { Write-PyWarn "scoop install python exited with code $LASTEXITCODE." }
    $scoopShims = Join-Path $env:USERPROFILE "scoop\shims"
    if (Test-Path -LiteralPath $scoopShims) {
      $env:Path = ($env:Path.TrimEnd(';') + ";" + $scoopShims)
    }
    return $true
  }

  function Resolve-Python {
    $found = Find-PythonCandidate
    if ($found) { return $found }
    Write-PyWarn "No working Python 3.11+ interpreter was found on PATH."
    Write-PyInfo "Attempting automatic install via winget..."
    $installed = Install-PythonViaWinget
    if ($script:WingetBlockedByPolicy) {
      Write-PyWarn "winget reported the install was blocked by organization policy (1625)."
      if (Install-PythonViaScoop) {
        $found = Find-PythonCandidate
        if ($found) { return $found }
      }
    }
    if ($installed) {
      $found = Find-PythonCandidate
      if ($found) { return $found }
      if (Test-IsWindowsArm64Host) {
        Write-PyWarn "Existing Python.Python.3.12 appears to be ARM64; forcing a side-by-side x64 install."
        $forced = Install-PythonViaWinget -ForceX64SideBySide
        if ($forced) {
          $found = Find-PythonCandidate
          if ($found) { return $found }
        }
      }
      Write-PyWarn "Python was installed but is not yet visible to this session."
      Write-PyInfo "Open a new PowerShell window and re-run this installer."
    }
    if (-not $script:WingetBlockedByPolicy) {
      if (Install-PythonViaScoop) {
        $found = Find-PythonCandidate
        if ($found) { return $found }
      }
    }
    Write-PyFail "No working Python 3.11+ interpreter was found."
    Write-PyInfo "Tried: python, python3, py -3.12, py -3.11, py -3.13, py -3.14, py -3, HKCU/HKLM\Software\Python\PythonCore registry, %LOCALAPPDATA%\Programs\Python, %ProgramFiles%\Python, %ProgramFiles%, %ProgramFiles(x86)%, %LOCALAPPDATA%\Microsoft\WinGet\Packages\Python.Python.3.*, %LOCALAPPDATA%\Microsoft\WinGet\Links, %USERPROFILE%\scoop\apps\python*, C:\tools\python*, C:\Python3*."
    Write-PyInfo "The Microsoft Store stub at WindowsApps\python.exe is intentionally ignored."
    Write-PyInfo "Install Python manually with one of:"
    Write-PyInfo "  scoop install python   (user-scope, works when winget is blocked by org policy)"
    Write-PyInfo "  winget install -e --id Python.Python.3.12"
    Write-PyInfo "  choco install python --version=3.12"
    Write-PyInfo "  https://www.python.org/downloads/"
    Write-PyInfo "Then open a new terminal and re-run this installer."
    throw "No working Python 3.11+ interpreter was found."
  }

function New-ManagedVenv($PythonInfo) {
  if ($Force -and (Test-Path -LiteralPath $VenvPath)) {
    Invoke-Step "Removing existing managed venv: $VenvPath" {
      Remove-Item -LiteralPath $VenvPath -Recurse -Force
    }
  }

  if (-not (Test-Path -LiteralPath $VenvPath)) {
    Invoke-Step "Creating managed X-Tester venv: $VenvPath" {
      $venvArgs = @()
      if ($PythonInfo.Arguments) { $venvArgs += $PythonInfo.Arguments }
      $venvArgs += @("-m", "venv", $VenvPath)
      Invoke-External $PythonInfo.FilePath $venvArgs
    }
  }

  $venvPython = Join-Path $VenvPath "Scripts\python.exe"
  if (-not (Test-Path -LiteralPath $venvPython)) {
    # Microsoft Store / UWP Python redirects writes under %LOCALAPPDATA% into
    # %LOCALAPPDATA%\Packages\PythonSoftwareFoundation.Python.*\LocalCache\Local\<rest>.
    # Try to locate the redirected venv and use that path instead.
    $redirected = $null
    $localAppData = $env:LOCALAPPDATA
    if ($localAppData -and $VenvPath.StartsWith($localAppData, [System.StringComparison]::OrdinalIgnoreCase)) {
      $relative = $VenvPath.Substring($localAppData.Length).TrimStart('\','/')
      $packagesRoot = Join-Path $localAppData "Packages"
      if (Test-Path -LiteralPath $packagesRoot) {
        $candidates = Get-ChildItem -LiteralPath $packagesRoot -Directory -ErrorAction SilentlyContinue |
          Where-Object { $_.Name -like "PythonSoftwareFoundation.Python.*" }
        foreach ($pkg in $candidates) {
          $candidateVenv = Join-Path $pkg.FullName (Join-Path "LocalCache\Local" $relative)
          $candidatePython = Join-Path $candidateVenv "Scripts\python.exe"
          if (Test-Path -LiteralPath $candidatePython) {
            $redirected = @{ Venv = $candidateVenv; Python = $candidatePython; Package = $pkg.Name }
            break
          }
        }
      }
    }
    if ($redirected) {
      Warn "Microsoft Store Python redirected the venv into $($redirected.Package)."
      Warn "Using redirected venv: $($redirected.Venv)"
      Info "To avoid this, install Python from https://www.python.org/downloads/ or via 'winget install -e --id Python.Python.3.12'."
      $Script:VenvPath = $redirected.Venv
      return $redirected.Python
    }
    throw "Managed venv is missing python.exe: $venvPython"
  }
  return $venvPython
}

function Get-LatestXTesterVersionFromFeed([string]$VenvPython) {
  # Ask the feed directly for the newest published version instead of trusting
  # pip's `-U` against a possibly stale cached/locally-known index. We then pin
  # the install to that exact version so the result is deterministic.
  #
  # `pip index versions` prints the latest as the first token, e.g.
  #   xtester (0.0.23)
  #   Available versions: 0.0.23, 0.0.22, ...
  # Feed auth works here because the surrounding Invoke-WithCleanPythonPath sets
  # PIP_KEYRING_PROVIDER=import (see that function for why).
  #
  # The Azure Artifacts credential provider writes benign informational lines
  # (e.g. "[Information] [CredentialProvider]... Acquired bearer token using
  # 'MSAL Silent'") to *stderr*. Under the script-wide $ErrorActionPreference =
  # "Stop", Windows PowerShell 5.1 turns native-command stderr captured via
  # `2>&1` into a terminating NativeCommandError, aborting the installer even
  # though the call succeeded. Relax the preference locally so those stderr
  # lines are captured as plain text and we can rely on $LASTEXITCODE instead.
  $raw = & {
    $ErrorActionPreference = "Continue"
    & $VenvPython @(
      "-m", "pip", "index", "versions", "xtester",
      "--no-cache-dir",
      "--index-url", $FeedUrl,
      "--extra-index-url", $ExtraIndexUrl
    ) 2>&1
  }
  if ($LASTEXITCODE -ne 0) { return $null }
  $text = ($raw | Out-String)
  $match = [regex]::Match($text, 'xtester\s*\(([^)]+)\)')
  if ($match.Success) { return $match.Groups[1].Value.Trim() }
  $match = [regex]::Match($text, 'LATEST:\s*([^\s]+)')
  if ($match.Success) { return $match.Groups[1].Value.Trim() }
  return $null
}

function Install-XTesterIntoVenv([string]$VenvPython) {
  Invoke-WithCleanPythonPath {
    Invoke-Step "Upgrading pip and installing private-feed auth helpers" {
      Invoke-External $VenvPython @("-m", "pip", "install", "--progress-bar", "off", "-U", "pip")
      Invoke-External $VenvPython @("-m", "pip", "install", "--progress-bar", "off", "-U", "--prefer-binary", "keyring", "artifacts-keyring")
    }

    if ($Source -eq "local") {
      $path = Resolve-LocalPackagePath
      Invoke-Step "Installing X-Tester from local source: $path" {
        Invoke-External $VenvPython @("-m", "pip", "install", "--progress-bar", "off", "-U", "-e", $path)
      }
      return
    }

    # Resolve the concrete version to install. Explicit -Version always wins;
    # otherwise discover the latest from the feed and pin to it. Pinning to a
    # discovered version avoids the failure mode where pip reports an older
    # release as "already satisfied" because its known index is stale.
    $targetVersion = $Version
    if ([string]::IsNullOrWhiteSpace($targetVersion)) {
      Info "Resolving latest X-Tester version from private feed"
      $targetVersion = Get-LatestXTesterVersionFromFeed $VenvPython
      if ([string]::IsNullOrWhiteSpace($targetVersion)) {
        Warn "Could not resolve latest version from feed; falling back to plain 'latest' install."
      } else {
        Success "Latest X-Tester on feed: $targetVersion"
      }
    }

    if ([string]::IsNullOrWhiteSpace($targetVersion)) {
      $packageSpec = "xtester"
      $installLabel = "latest X-Tester from private feed"
    } else {
      $packageSpec = "xtester==$targetVersion"
      $installLabel = "X-Tester $packageSpec from private feed"
    }

    Invoke-Step "Installing $installLabel" {
      # --no-cache-dir forces pip to re-fetch the simple index instead of
      # reusing a stale cached index page. Without it, a freshly published
      # version on the private feed can be ignored (pip's -U only upgrades
      # within the versions it already knows about), leaving the venv pinned
      # to an older release even though a newer one is available.
      Invoke-External $VenvPython @(
        "-m", "pip", "install", "--progress-bar", "off", "-U", "--prefer-binary", "--no-cache-dir",
        "--index-url", $FeedUrl,
        "--extra-index-url", $ExtraIndexUrl,
        $packageSpec
      )
    }
  }
}

function Resolve-XTesterMcpFromVenv {
  $exe = Join-Path $VenvPath "Scripts\xtester-mcp.exe"
  if (-not (Test-Path -LiteralPath $exe)) {
    throw "xtester-mcp.exe was not installed in the managed venv: $exe"
  }
  return (Resolve-Path -LiteralPath $exe).Path
}

function Ensure-XTesterCliPath([string]$VenvPython) {
  $venvScripts = Split-Path -Parent $VenvPython
  if (-not (Test-Path -LiteralPath $venvScripts)) {
    throw "Managed venv Scripts directory is missing: $venvScripts"
  }

  $ensureExe = Join-Path $venvScripts "xtester-ensure-path.exe"
  Invoke-Step "Ensuring xtester CLI path is on user PATH" {
    if (Test-Path -LiteralPath $ensureExe) {
      Invoke-External $ensureExe @()
    } else {
      Invoke-External $VenvPython @("-m", "xtester.install_path")
    }

    # Apply the same change to this installer process so immediate follow-up
    # commands in the same terminal can resolve xtester without reopening.
    $parts = @($env:Path -split ';' | Where-Object { $_ -and $_.Trim() })
    $normalizedExisting = $parts | ForEach-Object {
      $_.Trim().TrimEnd('\\').ToLowerInvariant()
    }
    $normalizedTarget = $venvScripts.Trim().TrimEnd('\\').ToLowerInvariant()
    if (-not ($normalizedExisting -contains $normalizedTarget)) {
      $env:Path = "$venvScripts;$env:Path"
    }
  }
}

function Ensure-CopilotCli {
  $cmd = Get-Command "copilot" -ErrorAction SilentlyContinue
  if ($cmd) { return $cmd.Source }
  if ($SkipClientInstall) { throw "copilot CLI was not found and -SkipClientInstall was supplied." }
  Require-Command "npm" "Install Node.js LTS, then re-run this installer."
  Invoke-Step "Installing GitHub Copilot CLI" {
    # --no-progress avoids npm's animated spinner being flushed line-by-line
    # in non-TTY PowerShell hosts (same root cause as the pip spinner column).
    Invoke-External "npm" @("install", "-g", "--no-progress", "--no-fund", "--no-audit", "@github/copilot")
  }
  $cmd = Get-Command "copilot" -ErrorAction SilentlyContinue
  if ($cmd) { return $cmd.Source }
  throw "GitHub Copilot CLI installed, but copilot is not visible on PATH. Restart the terminal and re-run this installer."
}

function Register-Copilot([string]$CommandPath) {
  $copilot = Ensure-CopilotCli
  try {
    Invoke-Step "Registering X-Tester MCP with GitHub Copilot CLI" {
      # If the server is already registered, remove it first so 'mcp add' does
      # not fail with 'Server "<name>" already exists'.
      $existing = & $copilot mcp list 2>&1
      if ($LASTEXITCODE -eq 0 -and ($existing | Out-String) -match ("(?m)^\s*" + [regex]::Escape($ServerName) + "(\s|$|:)")) {
        Info "Updating existing '$ServerName' entry in Copilot CLI"
        & $copilot mcp remove $ServerName 2>&1 | ForEach-Object { Write-Host "    $_" }
      }
      Invoke-External $copilot @("mcp", "add", $ServerName, "--", $CommandPath)
    }
  }
  catch {
    Warn "copilot mcp add failed: $($_.Exception.Message)"
    Warn "Writing ~/.copilot/mcp-config.json directly."
    $configPath = Join-Path (Join-Path $HOME ".copilot") "mcp-config.json"
    $config = Read-JsonObject $configPath ([ordered]@{ mcpServers = [ordered]@{} })
    if (-not $config.Contains("mcpServers")) { $config["mcpServers"] = [ordered]@{} }
    $config["mcpServers"][$ServerName] = [ordered]@{
      type = "local"
      command = $CommandPath
      args = @()
      tools = @("*")
    }
    Write-JsonObject $configPath $config
    Success "Wrote $configPath"
  }
}

function Register-ClaudeDesktop([string]$CommandPath) {
  $configPath = Join-Path $env:APPDATA "Claude\claude_desktop_config.json"
  $config = Read-JsonObject $configPath ([ordered]@{ mcpServers = [ordered]@{} })
  if (-not $config.Contains("mcpServers")) { $config["mcpServers"] = [ordered]@{} }
  $config["mcpServers"][$ServerName] = [ordered]@{
    command = $CommandPath
    args = @()
  }
  Write-JsonObject $configPath $config
  Success "Wrote Claude Desktop config: $configPath"
  Warn "Restart Claude Desktop to load the X-Tester MCP server."
}

function Offer-AgencyXTesterPluginUpdate {
  if ($Script:AgencyXTesterPluginPrompted) { return }
  $Script:AgencyXTesterPluginPrompted = $true

  $agency = Get-Command "agency" -ErrorAction SilentlyContinue
  if (-not $agency) { return }
  $agencyPath = $agency.Source
  if ($null -eq $Host.UI.RawUI) {
    Info "Agency CLI detected; skipping cxe-xtester plugin prompt in non-interactive host."
    return
  }

  Write-Host ""
  $answer = Read-Host "Agency CLI detected. Install/update xtester Agency plugin cxe-xtester from playground? [Y/n]"
  if (-not ([string]::IsNullOrWhiteSpace($answer) -or $answer -match '^(y|yes)$')) {
    Info "Skipping Agency cxe-xtester plugin update."
    return
  }

  Invoke-Step "Updating Agency cxe-xtester plugin" {
    & $agencyPath plugin uninstall cxe-xtester
    if ($LASTEXITCODE -ne 0) {
      Warn "agency plugin uninstall cxe-xtester exited with code $LASTEXITCODE; continuing with install."
    }
    Invoke-External $agencyPath @("plugin", "install", "mp:cxe-xtester@playground")
  }
}

function Register-ClaudeCode([string]$CommandPath) {
  $claude = Get-Command "claude" -ErrorAction SilentlyContinue
  if (-not $claude) {
    Warn "claude CLI was not found; skipping Claude Code registration."
    Warn "Use Claude Desktop registration or install Claude Code, then re-run with -Client claude-code."
    return
  }

  Offer-AgencyXTesterPluginUpdate

  try {
    Invoke-Step "Registering X-Tester MCP with Claude Code" {
      Invoke-External $claude.Source @("mcp", "add", $ServerName, $CommandPath)
    }
  }
  catch {
    Warn "claude mcp add failed: $($_.Exception.Message)"
    Warn "Run 'claude mcp --help' to confirm this Claude Code version supports MCP registration."
  }
}

function Get-VSCodeUserMcpConfigPath {
  # VS Code reads user-global (profile-wide) MCP servers from mcp.json in the
  # User config folder, not from a workspace .vscode/mcp.json. Registering there
  # makes X-Tester available in every workspace instead of only the directory
  # the installer happened to run in.
  if ($IsWindows -or $env:OS -eq "Windows_NT") {
    $base = if ($env:APPDATA) { $env:APPDATA } else { Join-Path $env:USERPROFILE "AppData\Roaming" }
    return Join-Path $base "Code\User\mcp.json"
  }
  if ($IsMacOS) {
    return Join-Path $HOME "Library/Application Support/Code/User/mcp.json"
  }
  $base = if ($env:XDG_CONFIG_HOME) { $env:XDG_CONFIG_HOME } else { Join-Path $HOME ".config" }
  return Join-Path $base "Code/User/mcp.json"
}

function New-XTesterMcpServerEntry([string]$CommandPath) {
  return [ordered]@{
    type = "stdio"
    command = $CommandPath
    args = @()
    env = [ordered]@{}
  }
}

function Register-VSCode([string]$CommandPath) {
  $configPath = Get-VSCodeUserMcpConfigPath
  try {
    $config = Read-JsonObject $configPath ([ordered]@{ servers = [ordered]@{} })
    if (-not $config.Contains("servers")) { $config["servers"] = [ordered]@{} }
    $config["servers"][$ServerName] = New-XTesterMcpServerEntry $CommandPath
    Write-JsonObject $configPath $config
    Success "Wrote VS Code user MCP config: $configPath"
  }
  catch {
    # The existing mcp.json is corrupt/unreadable (the original reported failure
    # mode was a truncated file). VS Code cannot load any server from a broken
    # file, so just skipping leaves the user with no X-Tester registration.
    # Recover by backing up the unreadable file and rewriting a minimal valid
    # config so registration actually succeeds. A failure of even that recovery
    # must not abort the whole installer (Python venv + other clients are done).
    Warn "Existing VS Code MCP config could not be parsed: $($_.Exception.Message)"
    try {
      if (Test-Path -LiteralPath $configPath) {
        $backupPath = "$configPath.bak"
        Copy-Item -LiteralPath $configPath -Destination $backupPath -Force -ErrorAction Stop
        Info "Backed up unreadable VS Code MCP config to: $backupPath"
      }
      $config = [ordered]@{ servers = [ordered]@{} }
      $config["servers"][$ServerName] = New-XTesterMcpServerEntry $CommandPath
      Write-JsonObject $configPath $config
      Success "Rewrote VS Code user MCP config: $configPath"
    }
    catch {
      Warn "Skipped VS Code registration: $($_.Exception.Message)"
      Info "Re-run with -Client vscode after fixing $configPath to add it later."
    }
  }
}

function Test-XTesterMcpSpawn([string]$CommandPath) {
  $stdinPath = [System.IO.Path]::GetTempFileName()
  $stdoutPath = [System.IO.Path]::GetTempFileName()
  $stderrPath = [System.IO.Path]::GetTempFileName()
  try {
    $process = Start-Process -FilePath $CommandPath -NoNewWindow -PassThru -RedirectStandardInput $stdinPath -RedirectStandardOutput $stdoutPath -RedirectStandardError $stderrPath
    Start-Sleep -Milliseconds 800
    if ($process.HasExited -and $process.ExitCode -ne 0) {
      $stderrText = Get-Content -Raw -LiteralPath $stderrPath -ErrorAction SilentlyContinue
      throw "xtester-mcp exited early with code $($process.ExitCode). $stderrText"
    }
    if (-not $process.HasExited) {
      $process.Kill()
      $process.WaitForExit()
    }
  }
  finally {
    Remove-Item $stdinPath, $stdoutPath, $stderrPath -Force -ErrorAction SilentlyContinue
  }
}

function Expand-Clients {
  if ($Client -contains "all") {
    return @("copilot", "claude-desktop", "claude-code", "vscode")
  }
  return $Client | Select-Object -Unique
}

# Entry point
Write-Host ""
Write-Host "  __  __    _____         _            " -ForegroundColor Magenta
Write-Host "  \ \/ /   |_   _|__  ___| |_ ___ _ __ " -ForegroundColor Magenta
Write-Host "   \  /_____| |/ _ \/ __| __/ _ \ '__|" -ForegroundColor Magenta
Write-Host "   /  \_____| |  __/\__ \ ||  __/ |   " -ForegroundColor Magenta
Write-Host "  /_/\_\    |_|\___||___/\__\___|_|   " -ForegroundColor Magenta
Write-Host ""
Write-Host "  X-Tester MCP installer  v$Script:InstallerVersion" -ForegroundColor Magenta
Write-Host ""

$pythonInfo = Resolve-Python
$pythonInvocation = @($pythonInfo.FilePath) + $pythonInfo.Arguments
Success "Using Python: $($pythonInfo.Version) ($($pythonInvocation -join ' '))"

# Pre-install optional runtime tools that XTester needs for specific flows:
#   adb (Google.PlatformTools) — required for Android device automation
#   ffmpeg (Gyan.FFmpeg)       — required for Windows screen recording
# The skills detect missing tools at runtime, but pre-installing avoids
# first-run friction for new users.
Invoke-Step "Ensuring adb (Android platform-tools) is available" {
  if (Get-Command "adb" -ErrorAction SilentlyContinue) {
    Info "adb already on PATH."
  } else {
    $winget = Get-Command "winget" -ErrorAction SilentlyContinue
    if ($winget) {
      Info "Installing Google.PlatformTools via winget..."
      Invoke-WingetQuiet $winget.Source @("install", "--id", "Google.PlatformTools", "-e", "--accept-source-agreements", "--accept-package-agreements", "--silent", "--disable-interactivity")
    } else {
      Warn "winget not available; install Android platform-tools manually and ensure adb is on PATH."
    }
  }
}
Invoke-Step "Ensuring ffmpeg is available" {
  if (Get-Command "ffmpeg" -ErrorAction SilentlyContinue) {
    Info "ffmpeg already on PATH."
  } else {
    $winget = Get-Command "winget" -ErrorAction SilentlyContinue
    if ($winget) {
      Info "Installing Gyan.FFmpeg via winget..."
      Invoke-WingetQuiet $winget.Source @("install", "--id", "Gyan.FFmpeg", "--accept-package-agreements", "--accept-source-agreements", "--silent", "--disable-interactivity")
    } else {
      Warn "winget not available; install ffmpeg manually and ensure it is on PATH."
    }
  }
}

Stop-XTesterMcpProcesses
$venvPython = New-ManagedVenv $pythonInfo
Install-XTesterIntoVenv $venvPython
Ensure-XTesterCliPath $venvPython
$xtesterMcp = Resolve-XTesterMcpFromVenv
Success "Using xtester-mcp: $xtesterMcp"

foreach ($target in (Expand-Clients)) {
  switch ($target) {
    "copilot" { Register-Copilot $xtesterMcp }
    "claude-desktop" { Register-ClaudeDesktop $xtesterMcp }
    "claude-code" { Register-ClaudeCode $xtesterMcp }
    "vscode" { Register-VSCode $xtesterMcp }
  }
}

# Opportunistic: if the user installed for copilot only and the Claude Code CLI
# is on PATH, offer to register there too. Skipped non-interactively (no host
# UI) and when the user already asked for claude-code or -SkipClientInstall.
$selectedClients = Expand-Clients
if (-not $SkipClientInstall `
    -and ($selectedClients -contains 'copilot') `
    -and (-not ($selectedClients -contains 'claude-code')) `
    -and ($null -ne $Host.UI.RawUI) `
    -and (Get-Command 'claude' -ErrorAction SilentlyContinue)) {
  Write-Host ""
  $answer = Read-Host "Claude Code CLI detected. Also register X-Tester MCP with Claude Code? [Y/n]"
  if ([string]::IsNullOrWhiteSpace($answer) -or $answer -match '^(y|yes)$') {
    Register-ClaudeCode $xtesterMcp
  } else {
    Info "Skipping Claude Code registration. Re-run with -Client claude-code to add it later."
  }
}

# Opportunistic: if the user installed for copilot only and VS Code is on PATH,
# offer to register the MCP server in the VS Code user profile (mcp.json) too,
# so it is available across all workspaces. Skipped non-interactively (no host
# UI) and when the user already asked for vscode or -SkipClientInstall.
if (-not $SkipClientInstall `
    -and ($selectedClients -contains 'copilot') `
    -and (-not ($selectedClients -contains 'vscode')) `
    -and ($null -ne $Host.UI.RawUI) `
    -and (Get-Command 'code' -ErrorAction SilentlyContinue)) {
  Write-Host ""
  $answer = Read-Host "VS Code detected. Also register X-Tester MCP with VS Code (user profile)? [Y/n]"
  if ([string]::IsNullOrWhiteSpace($answer) -or $answer -match '^(y|yes)$') {
    Register-VSCode $xtesterMcp
  } else {
    Info "Skipping VS Code registration. Re-run with -Client vscode to add it later."
  }
}

Invoke-Step "Checking xtester-mcp can spawn" {
  Test-XTesterMcpSpawn $xtesterMcp
}

Write-Host ""
Success "X-Tester MCP installation complete."
Info "Server name: $ServerName"
Info "Managed venv: $VenvPath"
Info "MCP command: $xtesterMcp"
