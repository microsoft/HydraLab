$CurrentPath = split-path -parent $MyInvocation.MyCommand.Definition
$RootPath = $CurrentPath + "\HydraLab"
$PackagesPath = $RootPath + "\Packages"
$AndroidRoot = $PackagesPath + "\Android"
$Urls = @{}
$Arch = $null
$AgentServiceName = "Hydra Lab Agent Service"

# =======================================
# Functions
# =======================================
Function Get-File ($Url, $Path, $ExtractCurrent=$false)
{
    $fileName = $Url.Substring($Url.LastIndexOf('/') + 1)
    if ($Path.EndsWith('/') -or $Path.EndsWith('\'))
    {
        $Path += $fileName
    }
    else
    {
        $Path += '\'
        $Path += $fileName
    }
    Start-BitsTransfer -Source $url -Destination $Path
    if ($Path.EndsWith(".zip"))
    {
        if ($ExtractCurrent)
        {
            $PathExtract = $Path.Substring(0, $Path.LastIndexOf('\') + 1)
            Expand-Archive $Path -DestinationPath $PathExtract
        }
        else
        {
            Expand-Archive $Path -DestinationPath $Path.Substring(0, $Path.Length - ".zip".Length)
        }
        Remove-Item $Path
    }
}

Function Set-EnvironmentVar($Key, $Val)
{
    if (-Not $Val -or $Val.Length -lt 1)
    {
        throw "$Val is not supported"
    }
    if (-Not([Environment]::GetEnvironmentVariable($Key, 'Machine') -eq $Val))
    {
        [System.Environment]::SetEnvironmentVariable($Key, $Val, 'Machine')
    }
    if (-Not([Environment]::GetEnvironmentVariable($Key, 'User') -eq $Val))
    {
        [System.Environment]::SetEnvironmentVariable($Key, $Val, 'User')
    }
}

Function Add-EnvironmentOneVar($Key, $Val)
{
    if (-Not $Val -or $Val.Length -lt 1)
    {
        throw "$Val is not supported"
    }
    if (-Not([Environment]::GetEnvironmentVariable($Key, 'Machine').Split(';') -contains $Val))
    {
        if ([Environment]::GetEnvironmentVariable($Key, 'Machine').EndsWith(';'))
        {
            [System.Environment]::SetEnvironmentVariable($Key, "$([Environment]::GetEnvironmentVariable($Key, 'Machine'))$Val", 'Machine')
        }
        else
        {
            [System.Environment]::SetEnvironmentVariable($Key, "$([Environment]::GetEnvironmentVariable($Key, 'Machine'));$Val", 'Machine')
        }
    }
    if (-Not([Environment]::GetEnvironmentVariable($Key, 'User').Split(';') -contains $Val))
    {
        if ([Environment]::GetEnvironmentVariable($Key, 'User').EndsWith(';'))
        {
            [System.Environment]::SetEnvironmentVariable($Key, "$([Environment]::GetEnvironmentVariable($Key, 'User'))$Val", 'User')
        }
        else
        {
            [System.Environment]::SetEnvironmentVariable($Key, "$([Environment]::GetEnvironmentVariable($Key, 'User'));$Val", 'User')
        }
    }
}

Function Add-EnvironmentVar($Key, $Val)
{
    foreach ($One in $Val.Trim(';',' ').Split(';'))
    {
        Add-EnvironmentOneVar -Key $Key -Val $One
    }
}

Function Find-Folder($Parent, $Substr)
{
    foreach ($sub in Get-ChildItem -Path $Parent -Directory -Name)
    {
        if ($sub.Contains($Substr))
        {
            if ($Parent.EndsWith('\') -or $Parent.EndsWith('/'))
            {
                return $Parent + $sub
            }
            else
            {
                return $Parent + '\' + $sub
            }
        }
    }
    return $Parent
}

Function Find-File($Parent, $Substr)
{
    foreach ($sub in Get-ChildItem -Path $Parent -File -Name)
    {
        if ($sub.Contains($Substr))
        {
            if ($Parent.EndsWith('\') -or $Parent.EndsWith('/'))
            {
                return $Parent + $sub
            }
            else
            {
                return $Parent + '\' + $sub
            }
        }
    }
    return ""
}

Function Get-CommandlinetoolsUrl
{
    $url = "https://developer.android.com/studio#downloads"

    $request = Invoke-WebRequest $url
    $content = $request.Content

    $package = [regex]::Matches($content, 'https://dl.google.com/android/repository/commandlinetools-win-[0-9]*_latest.zip')
    return $package[0].Value
}

Function Import-DownloadUrls
{
    $CPUHash = @{0="x86";1="MIPS";2="Alpha";3="PowerPC";5="ARM";6="Itanium-based systems";9="x64"}
    $Wmi = Get-WMIObject -Class Win32_Processor -ComputerName $env:ComputerName
    $Arch = $CPUHash[[int]$Wmi.Architecture]
    if (($Arch -ne "x64") -and ($Arch -ne "ARM"))
    {
        throw "CPU Achitecture $Arch is not supported"
    }

    switch ($Arch)
    {
        "x64"
        {
            $Urls.Add("Python",            "https://www.python.org/ftp/python/3.11.2/python-3.11.2-embed-amd64.zip")
            $Urls.Add("Nodejs",            "https://nodejs.org/dist/v18.14.2/node-v18.14.2-win-x64.zip")
            $Urls.Add("WinAppDriver",      "https://github.com/microsoft/WinAppDriver/releases/download/v1.2.99/WindowsApplicationDriver-1.2.99-win-x64.exe")
            $Urls.Add("FFmpeg",            "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip")
            $Urls.Add("JDK",               "https://aka.ms/download-jdk/microsoft-jdk-17.0.6-windows-x64.zip")
            $Urls.Add("Commandlinetools",  $(Get-CommandlinetoolsUrl))
            break
        }
        "ARM"
        {
            $Urls.Add("Python",            "https://www.python.org/ftp/python/3.11.2/python-3.11.2-embed-arm64.zip")
            $Urls.Add("Nodejs",            "https://unofficial-builds.nodejs.org/download/release/v16.6.2/node-v16.6.2-win-arm64.zip")
            $Urls.Add("WinAppDriver",      "https://github.com/microsoft/WinAppDriver/releases/download/v1.2.99/WindowsApplicationDriver-1.2.99-win-arm64.exe")
            $Urls.Add("FFmpeg",            "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip")
            $Urls.Add("JDK",               "https://aka.ms/download-jdk/microsoft-jdk-17.0.6-windows-aarch64.zip")
            $Urls.Add("Commandlinetools",  $(Get-CommandlinetoolsUrl))
            break
        }
    }
}

# =======================================
# Evelate Administrator
# =======================================
if (-Not((New-Object Security.Principal.WindowsPrincipal $([Security.Principal.WindowsIdentity]::GetCurrent())).IsInRole([Security.Principal.WindowsBuiltinRole]::Administrator)))
{
    $Arguments = @()
    $Arguments += "-noprofile"
    $Arguments += "-noexit"
    $Arguments += "-file $($myinvocation.MyCommand.Definition)"
    $Arguments += "-elevated"

    Write-Host "Will run as administrator in a new window"

    Start-Process powershell.exe -Verb RunAs -ArgumentList $Arguments
    return
}

# =======================================
# Enable Long Paths
# =======================================
New-ItemProperty -Path "HKLM:\SYSTEM\CurrentControlSet\Control\FileSystem" -Name "LongPathsEnabled" -Value 1 -PropertyType DWORD -Force

# =======================================
# Check No HydraLab Folder exists
# =======================================
if (Test-Path -Path $RootPath)
{
    Remove-Item $RootPath -Recurse
}

# =======================================
# Copy Configuration File
# =======================================
if (-Not(Test-Path -Path "$CurrentPath\application.yml"))
{
    throw "Not found configuration file $CurrentPath\application.yml"
}
[void](New-Item -Path $RootPath -ItemType Directory)
Copy-Item "$CurrentPath\application.yml" -Destination "$RootPath\application.yml"

# =======================================
# Download HydraLab
# =======================================
$filesToDownload = "agent.jar", "HydraLab_Agent_Installer_Windows.zip"
$release = Invoke-RestMethod -Uri https://api.github.com/repos/microsoft/HydraLab/releases/latest
$releaseAssets = Invoke-RestMethod -Uri "$($release.url)/assets"
foreach ($ast in $releaseAssets)
{
    if ($filesToDownload -contains $ast.name)
    {
        Get-File -Url $ast.browser_download_url -Path $RootPath -ExtractCurrent $true
    }
}

# =======================================
# Packages Urls
# =======================================
[void](New-Item -Path $PackagesPath -ItemType Directory)
Import-DownloadUrls

# =======================================
# Python
# =======================================
Write-Host "Install Python..."
Get-File -Url $Urls["Python"] -Path $PackagesPath
$PythonRoot = Find-Folder -Parent $PackagesPath -Substr "python"
Add-EnvironmentVar -Key "PATH" -Val $PythonRoot

# =======================================
# Node.js
# =======================================
Write-Host "Install Node.js..."
Get-File -Url $Urls["Nodejs"] -Path $PackagesPath
$NODE_Root = Find-Folder -Parent $PackagesPath -Substr "node"
$NODE_Root = Find-Folder -Parent $NODE_Root    -Substr "node"
Add-EnvironmentVar -Key "PATH" -Val $NODE_Root
$NODE_Path = Find-Folder -Parent $NODE_Root -Substr "node_modules"
$NODE_Path = Find-Folder -Parent $NODE_Path -Substr "npm"
$NODE_Path = Find-Folder -Parent $NODE_Path -Substr "node_modules"
Add-EnvironmentVar -Key "PATH" -Val $NODE_Path

# =======================================
# Appium
# =======================================
Write-Host "Install Appium..."
$NpmPath = Find-file -Parent $NODE_Root -Substr "npm.cmd"
$Arguments = @()
$Arguments += "install"
$Arguments += "-g appium"
$proc = Start-Process -FilePath $NpmPath -ArgumentList $Arguments -Wait -PassThru
$proc.waitForExit()

# =======================================
# WinAppDriver
# =======================================
Write-Host "Install WinAppDriver..."
Get-File -Url $Urls["WinAppDriver"] -Path $PackagesPath
$WindowsAppDriverPath = Find-File -Parent $PackagesPath -Substr "WindowsApplicationDriver"
$proc = Start-Process -FilePath $WindowsAppDriverPath -ArgumentList "/s" -Wait -PassThru
$proc.waitForExit()

# =======================================
# FFmpeg
# =======================================
Write-Host "Install FFmpeg..."
Get-File -Url $Urls["FFmpeg"] -Path $PackagesPath
$FFmpegPath = Find-Folder -Parent $PackagesPath -Substr "ffmpeg"
$FFmpegPath = Find-Folder -Parent $FFmpegPath   -Substr "ffmpeg"
$FFmpegPath = Find-Folder -Parent $FFmpegPath   -Substr "bin"
Add-EnvironmentVar -Key "PATH" -Val $FFmpegPath

# =======================================
# Java SDK
# =======================================
Write-Host "Install Java SDK..."
Get-File -Url $Urls["JDK"] -Path $PackagesPath
$JDK_Root = Find-Folder -Parent $PackagesPath -Substr "jdk"
$JDK_Root = Find-Folder -Parent $JDK_Root     -Substr "jdk"
Set-EnvironmentVar -Key "JAVA_HOME" -Val $JDK_Root
Add-EnvironmentVar -Key "PATH"      -Val "%JAVA_HOME%\bin;"

# =======================================
# Android command-line-tools and platform-tools
# =======================================
Write-Host "Install Android command line tools and platform tools..."
[void](New-Item -Path $AndroidRoot -ItemType Directory)
Get-File -Url $Urls["Commandlinetools"] -Path $AndroidRoot
$CmdlineToolsPath = Find-Folder -Parent $AndroidRoot -Substr "commandlinetools"
$Arguments = @()
$Arguments += "--sdk_root=$AndroidRoot"
$Arguments += "platform-tools"
$Arguments += "emulator"
$Arguments += "build-tools;33.0.0"
$Arguments += "platforms;android-33"
$proc = Start-Process -FilePath "$CmdlineToolsPath/cmdline-tools/bin/sdkmanager.bat" -ArgumentList $Arguments -Wait -PassThru
$proc.waitForExit()

Set-EnvironmentVar -Key "ANDROID_HOME"             -Val $AndroidRoot
Set-EnvironmentVar -Key "ANDROID_CMDLINE_TOOLS"    -Val $CmdlineToolsPath
Set-EnvironmentVar -Key "ANDROID_PLATFORM_TOOLS"   -Val "%ANDROID_HOME%\platform-tools"
Add-EnvironmentVar -Key "PATH"                     -Val "%ANDROID_HOME%"
Add-EnvironmentVar -Key "PATH"                     -Val "%ANDROID_CMDLINE_TOOLS%"
Add-EnvironmentVar -Key "PATH"                     -Val "%ANDROID_PLATFORM_TOOLS%"

$AgentServiceXml = Get-Content -Path "$RootPath\AgentService.xml"
$AgentServiceXml = $AgentServiceXml -replace "java","$JDK_Root\bin\java.exe"
$AgentServiceXml = $AgentServiceXml -replace "{LOG_FILE_LOCATION}","Logs"
$AgentServiceXml | Set-Content -Path "$RootPath\AgentService.xml"

sc.exe delete $AgentServiceName
New-Service -Name $AgentServiceName -BinaryPathName "$RootPath\AgentService.exe"
Start-Service -Name $AgentServiceName
