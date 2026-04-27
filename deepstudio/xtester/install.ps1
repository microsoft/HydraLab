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

$Script:InstallerVersion = "1.3.0"

# Force UTF-8 console output so winget's progress glyphs and other tool output
# are not rendered as mojibake in legacy code-page consoles.
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

function Invoke-WithCleanPythonPath([scriptblock]$Script) {
  $previousPythonPath = $env:PYTHONPATH
  try {
    Remove-Item Env:\PYTHONPATH -ErrorAction SilentlyContinue
    & $Script
  }
  finally {
    if ($null -ne $previousPythonPath) {
      $env:PYTHONPATH = $previousPythonPath
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

function Read-JsonObject([string]$Path, [hashtable]$DefaultValue) {
  if (-not (Test-Path -LiteralPath $Path)) { return $DefaultValue }
  $raw = Get-Content -Raw -LiteralPath $Path
  if ([string]::IsNullOrWhiteSpace($raw)) { return $DefaultValue }
  return ConvertTo-Hashtable ($raw | ConvertFrom-Json)
}

function Write-JsonObject([string]$Path, $Value) {
  $dir = Split-Path -Parent $Path
  if ($dir) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
  $Value | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $Path -Encoding UTF8
}

function Resolve-LocalPackagePath {
  if ($LocalPath) { return (Resolve-Path -LiteralPath $LocalPath).Path }
  $candidate = Join-Path $PSScriptRoot "..\DeepTest\XTester"
  return (Resolve-Path -LiteralPath $candidate).Path
}

function Get-HostArchitecture {
  try {
    return [System.Runtime.InteropServices.RuntimeInformation]::OSArchitecture.ToString()
  }
  catch {
    return $env:PROCESSOR_ARCHITECTURE
  }
}

function Test-IsWindowsArm64Host {
  $isWindowsHost = $false
  try { $isWindowsHost = $IsWindows } catch { }
  if (-not $isWindowsHost -and $env:OS -eq "Windows_NT") { $isWindowsHost = $true }
  if (-not $isWindowsHost) { return $false }

  $arch = Get-HostArchitecture
  return $arch -match "^(Arm64|AArch64)$"
}

function Get-PythonMachine([string]$FilePath, [string[]]$Arguments) {
  try {
    $output = & $FilePath @Arguments -c "import platform; print(platform.machine())" 2>&1
    if ($LASTEXITCODE -ne 0) { return $null }
    return ($output | Out-String).Trim()
  }
  catch {
    return $null
  }
}

function Test-IsUnsupportedWindowsArm64Python([string]$FilePath, [string[]]$Arguments) {
  if (-not (Test-IsWindowsArm64Host)) { return $false }
  $machine = Get-PythonMachine $FilePath $Arguments
  return $machine -match "^(ARM64|AARCH64)$"
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
  if ((Test-Path -LiteralPath $venvPython) -and (Test-IsUnsupportedWindowsArm64Python $venvPython @())) {
    Invoke-Step "Removing native ARM64 managed venv: $VenvPath" {
      Remove-Item -LiteralPath $VenvPath -Recurse -Force
    }
    Invoke-Step "Creating managed X-Tester venv: $VenvPath" {
      $venvArgs = @()
      if ($PythonInfo.Arguments) { $venvArgs += $PythonInfo.Arguments }
      $venvArgs += @("-m", "venv", $VenvPath)
      Invoke-External $PythonInfo.FilePath $venvArgs
    }
    $venvPython = Join-Path $VenvPath "Scripts\python.exe"
  }

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

function Install-XTesterIntoVenv([string]$VenvPython) {
  Invoke-WithCleanPythonPath {
    Invoke-Step "Upgrading pip and installing private-feed auth helpers" {
      Invoke-External $VenvPython @("-m", "pip", "install", "-U", "pip")
      Invoke-External $VenvPython @("-m", "pip", "install", "-U", "--prefer-binary", "keyring", "artifacts-keyring")
    }

    if ($Source -eq "local") {
      $path = Resolve-LocalPackagePath
      Invoke-Step "Installing X-Tester from local source: $path" {
        Invoke-External $VenvPython @("-m", "pip", "install", "-U", "-e", $path)
      }
      return
    }

    $packageSpec = "xtester"
    $installLabel = "latest X-Tester from private feed"
    if (-not [string]::IsNullOrWhiteSpace($Version)) {
      $packageSpec = "xtester==$Version"
      $installLabel = "X-Tester $packageSpec from private feed"
    }

    Invoke-Step "Installing $installLabel" {
      Invoke-External $VenvPython @(
        "-m", "pip", "install", "-U", "--prefer-binary",
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

function Ensure-CopilotCli {
  $cmd = Get-Command "copilot" -ErrorAction SilentlyContinue
  if ($cmd) { return $cmd.Source }
  if ($SkipClientInstall) { throw "copilot CLI was not found and -SkipClientInstall was supplied." }
  Require-Command "npm" "Install Node.js LTS, then re-run this installer."
  Invoke-Step "Installing GitHub Copilot CLI" {
    Invoke-External "npm" @("install", "-g", "@github/copilot")
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

function Register-ClaudeCode([string]$CommandPath) {
  $claude = Get-Command "claude" -ErrorAction SilentlyContinue
  if (-not $claude) {
    Warn "claude CLI was not found; skipping Claude Code registration."
    Warn "Use Claude Desktop registration or install Claude Code, then re-run with -Client claude-code."
    return
  }

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

function Register-VSCode([string]$CommandPath) {
  $repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
  $configPath = Join-Path $repoRoot ".vscode\mcp.json"
  $config = Read-JsonObject $configPath ([ordered]@{ servers = [ordered]@{} })
  if (-not $config.Contains("servers")) { $config["servers"] = [ordered]@{} }
  $config["servers"][$ServerName] = [ordered]@{
    command = $CommandPath
    env = [ordered]@{}
  }
  Write-JsonObject $configPath $config
  Success "Wrote VS Code MCP config: $configPath"
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

function Test-PythonExecutable([string]$FilePath, [string[]]$Arguments) {
  if (-not $FilePath) { return $null }
  try {
    $output = & $FilePath @Arguments --version 2>&1
  }
  catch {
    return $null
  }
  if ($LASTEXITCODE -ne 0) { return $null }
  $text = ($output | Out-String).Trim()
  if ([string]::IsNullOrWhiteSpace($text)) { return $null }
  if ($text -notmatch "Python\s+(\d+)\.(\d+)\.(\d+)") { return $null }
  $major = [int]$Matches[1]
  $minor = [int]$Matches[2]
  if ($major -lt 3 -or ($major -eq 3 -and $minor -lt 11)) { return $null }

  if (Test-IsUnsupportedWindowsArm64Python $FilePath $Arguments) {
    return $null
  }

  # Reject Microsoft Store Python distributions. They virtualize writes under
  # %LOCALAPPDATA% into %LOCALAPPDATA%\Packages\PythonSoftwareFoundation.Python.*\LocalCache\Local\,
  # which breaks `python -m venv` against our managed venv path.
  try {
    $sysExe = & $FilePath @Arguments -c "import sys; print(sys.executable)" 2>&1
    if ($LASTEXITCODE -eq 0) {
      $sysExeText = ($sysExe | Out-String).Trim()
      if ($sysExeText -match "\\WindowsApps\\" -or $sysExeText -match "\\Packages\\PythonSoftwareFoundation\.Python\.") {
        return $null
      }
    }
  } catch { }

  return [pscustomobject]@{
    FilePath  = $FilePath
    Arguments = $Arguments
    Version   = $text
  }
}

function Find-PythonCandidate {
  $candidates = @()
  # Skip the Microsoft Store WindowsApps stub which prints a help message and exits 9009.
  $isRealPython = {
    param($cmd)
    if (-not $cmd) { return $false }
    if ($cmd.Source -and $cmd.Source -match "WindowsApps\\python(3)?\.exe$") { return $false }
    return $true
  }

  foreach ($name in @("python", "python3")) {
    $cmd = Get-Command $name -ErrorAction SilentlyContinue
    if (& $isRealPython $cmd) {
      $candidates += ,@($cmd.Source, @())
    }
  }

  $py = Get-Command "py" -ErrorAction SilentlyContinue
  if ($py) {
    foreach ($flag in @("-3.12", "-3.11", "-3.13", "-3")) {
      $candidates += ,@($py.Source, @($flag))
    }
  }

  # Common per-user install locations winget uses for Python.Python.3.x.
  $localPython = Join-Path $env:LOCALAPPDATA "Programs\Python"
  if (Test-Path -LiteralPath $localPython) {
    $found = Get-ChildItem -LiteralPath $localPython -Directory -ErrorAction SilentlyContinue |
      Where-Object { $_.Name -match "^Python3(1[1-9]|[2-9][0-9])" } |
      Sort-Object Name -Descending
    foreach ($dir in $found) {
      $exe = Join-Path $dir.FullName "python.exe"
      if (Test-Path -LiteralPath $exe) { $candidates += ,@($exe, @()) }
    }
  }

  foreach ($pair in $candidates) {
    $result = Test-PythonExecutable $pair[0] $pair[1]
    if ($result) { return $result }
  }
  return $null
}

function Install-PythonViaWinget {
  $winget = Get-Command "winget" -ErrorAction SilentlyContinue
  if (-not $winget) {
    Warn "winget is not available; cannot auto-install Python."
    return $false
  }
  Invoke-Step "Installing Python 3.12 via winget (Python.Python.3.12)" {
    # Use --scope user so we do not require an elevation prompt; winget returns
    # non-zero for "already installed" too, so we tolerate any non-fatal exit.
    # --disable-interactivity suppresses winget's animated progress bar so the
    # console is not flooded with redraw lines (which can also render as
    # mojibake on legacy code pages).
    $wingetArgs = @("install", "-e", "--id", "Python.Python.3.12", "--scope", "user")
    if (Test-IsWindowsArm64Host) {
      $wingetArgs += @("--architecture", "x64")
    }
    $wingetArgs += @(
      "--accept-source-agreements", "--accept-package-agreements", "--silent",
      "--disable-interactivity"
    )
    & $winget.Source @wingetArgs 2>&1 | ForEach-Object {
        $line = [string]$_
        # Drop empty/whitespace lines and progress redraw lines (block glyphs,
        # carriage-return progress percentages, and MB/KB transfer counters).
        if ([string]::IsNullOrWhiteSpace($line)) { return }
        if ($line -match '^[\s\u2580-\u259F\u2588-\u259B█▒▓░\-\\|/]+\s*\d+\s*%\s*$') { return }
        if ($line -match '^[\s\u2580-\u259F\u2588-\u259B█▒▓░\-\\|/]+\s*\d[\d.,]*\s*(KB|MB|GB)\s*/\s*\d') { return }
        if ($line -match '^[\s\-\\|/]+$') { return }
        Write-Host "    $line"
    }
    if ($LASTEXITCODE -ne 0) {
      Warn "winget exited with code $LASTEXITCODE (this can mean 'already installed' or 'no upgrade needed' and is often safe to ignore)."
    }
  }
  # winget does not refresh the current session's PATH; pull the new locations in manually.
  $machinePath = [Environment]::GetEnvironmentVariable("Path", "Machine")
  $userPath = [Environment]::GetEnvironmentVariable("Path", "User")
  $env:Path = (@($machinePath, $userPath) | Where-Object { $_ }) -join ";"
  return $true
}

function Resolve-Python {
  $found = Find-PythonCandidate
  if ($found) { return $found }

  Warn "No working Python 3.11+ interpreter was found on PATH."
  Info "Attempting automatic install via winget..."
  $installed = Install-PythonViaWinget
  if ($installed) {
    $found = Find-PythonCandidate
    if ($found) { return $found }
    Warn "Python was installed but is not yet visible to this session."
    Info "Open a new PowerShell window and re-run this installer."
  }

  Fail "No working Python 3.11+ interpreter was found."
  Info "Tried: python, python3, py -3.12, py -3.11, py -3.13, py -3 plus %LOCALAPPDATA%\Programs\Python\Python3*."
  Info "The Microsoft Store stub at WindowsApps\python.exe is intentionally ignored."
  if (Test-IsWindowsArm64Host) {
    Info "Native ARM64 Python is intentionally ignored because required auth-helper wheels can fall back to cryptography/OpenSSL source builds."
    Info "Use x64 Python 3.12 on Windows ARM64 for this installer."
  }
  Info "Install Python manually with one of:"
  if (Test-IsWindowsArm64Host) {
    Info "  winget install -e --id Python.Python.3.12 --architecture x64"
  } else {
    Info "  winget install -e --id Python.Python.3.12"
  }
  Info "  choco install python --version=3.12"
  Info "  https://www.python.org/downloads/"
  Info "Then open a new terminal and re-run this installer."
  throw "No working Python 3.11+ interpreter was found."
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

$venvPython = New-ManagedVenv $pythonInfo
Install-XTesterIntoVenv $venvPython
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
    -and ($Host.UI.RawUI -ne $null) `
    -and (Get-Command 'claude' -ErrorAction SilentlyContinue)) {
  Write-Host ""
  $answer = Read-Host "Claude Code CLI detected. Also register X-Tester MCP with Claude Code? [Y/n]"
  if ([string]::IsNullOrWhiteSpace($answer) -or $answer -match '^(y|yes)$') {
    Register-ClaudeCode $xtesterMcp
  } else {
    Info "Skipping Claude Code registration. Re-run with -Client claude-code to add it later."
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
