$CurrentScriptPath = split-path -parent $MyInvocation.MyCommand.Definition
$ProgressPreference = 'SilentlyContinue'

if ((New-Object Security.Principal.WindowsPrincipal $([Security.Principal.WindowsIdentity]::GetCurrent())).IsInRole([Security.Principal.WindowsBuiltinRole]::Administrator)) {

    $E2E = 'Unknown'
    while (($E2E -ne 'Y') -and ($E2E -ne 'N') -and ($E2E -ne 'y') -and ($E2E -ne 'n'))
    {
        $E2E = Read-Host "Is E2E(End to End) Test needed in your test? (Y/N)"
    }
    if (($E2E -eq 'Y') -or ($E2E -eq 'y'))
    {
        $taskTrigger = New-ScheduledTaskTrigger -AtLogon
        $taskAction = New-ScheduledTaskAction -Execute "$CurrentScriptPath\restartAgent.bat"
        Register-ScheduledTask -TaskName "HydraLabAgentRestart" -Trigger $taskTrigger -Action $taskAction
        Copy-Item "$CurrentScriptPath\restartAgent_WindowsTaskScheduler.bat" -Destination "$CurrentScriptPath\restartAgent.bat" -Force

        Write-Host "E2E test need to logon Windows automatically, please input Windows User-Name and Password to set Auto-Logon"
        Write-Host

        $logonRegistryPath = "HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Winlogon"
        $logonUserName = Read-Host "Windows Logon User-Name: "
        $logonPassword = Read-Host "Windows Logon Password: "
        New-ItemProperty -Path $logonRegistryPath -Name "DefaultUserName" -Value $logonUserName -PropertyType String -Force | Out-Null
        New-ItemProperty -Path $logonRegistryPath -Name "DefaultPassword" -Value $logonPassword -PropertyType String -Force | Out-Null
        New-ItemProperty -Path $logonRegistryPath -Name "AutoAdminLogon" -Value "1" -PropertyType String -Force | Out-Null
    }
    else
    {
        Copy-Item "$CurrentScriptPath\restartAgent_WindowsService.bat" -Destination "$CurrentScriptPath\restartAgent.bat" -Force
    }

    $destination = Join-Path (Convert-Path "~") "Downloads"

    $choice = Read-Host "Please select a folder to download installation packages, the default is $destination"
    if ($choice) {
        $destination = $choice
    }

    Write-Host "Creating temporary download folder... `"$destination`""
    Write-Host

    if (!(Test-Path $destination)) {
        New-Item -Path $destination -ItemType Directory
        Write-Host
    }

    $Path = ''

    # 1. Install Java SDK

    Write-Host '1. Install Java SDK'
    Write-Host

    $skip = 'N'
    
    & java.exe -version
    if ($?) {
        Write-Host
        $skip = Read-Host "Java SDK already exists, skip the installation? (Y/N)"
        Write-Host
    }

    if ($skip -eq 'N') {
        Write-Host
        Write-Host "==============================================="
        Write-Host "Please select the JDK version to install:"
        Write-Host
        Write-Host "  1: Java SE Development Kit 17 (default)"
        Write-Host "  2: Java SE Development Kit 18"
        Write-Host "==============================================="
        Write-Host

        $choice = Read-Host "Please select the JDK version to install"
        Write-Host

        $JDK_VER = "17"

        if ($choice -eq '2') {
            $JDK_VER = "18"
        }

        $JDK_x64_Installer_Url = "https://download.oracle.com/java/$($JDK_VER)/latest/jdk-$($JDK_VER)_windows-x64_bin.exe"
        $JDK_x64_Installer_destination = Join-Path $destination "jdk-$($JDK_VER)_windows-x64_bin.exe"

        Write-Host "Downloading JDK_x64_MSI_Installer from $JDK_x64_Installer_Url to $JDK_x64_Installer_destination"
        Write-Host

        Invoke-WebRequest -Uri $JDK_x64_Installer_Url -OutFile $JDK_x64_Installer_destination
        if (!(Test-Path $JDK_x64_Installer_destination)) {
            Write-Host "Downloading $JDK_x64_Installer_destination failed"
            Exit
        }

        $Arguments = "/s REBOOT=ReallySuppress"

        try {
            Write-Host "Installing Java SE Development Kit $JDK_VER. This may take some time. Please wait..."
            $proc = Start-Process -FilePath "$JDK_x64_Installer_destination" -ArgumentList "$Arguments" -Wait -PassThru
            $proc.waitForExit()
            Write-Host 'Installation Done.'
            Write-Host
        } catch [exception] {
            write-host '$_ is' $_
            write-host '$_.GetType().FullName is' $_.GetType().FullName
            write-host '$_.Exception is' $_.Exception
            write-host '$_.Exception.GetType().FullName is' $_.Exception.GetType().FullName
            write-host '$_.Exception.Message is' $_.Exception.Message
        }

        Write-Host "Setting JAVA environment variables. This may take some time. Please wait..."
        Write-Host 

        $JDK_Root_Folder = ''

        foreach ($Sub_Folder in (Get-ChildItem 'C:\Program Files\Java' | Sort-Object -Property LastWriteTime -Descending)){
            if ($Sub_Folder.PSChildName.contains("jdk-$JDK_VER")){
                $JDK_Root_Folder =  Join-Path 'C:\Program Files\Java' $Sub_Folder.PSChildName
                break
            }
        }

        [System.Environment]::SetEnvironmentVariable("JAVA_HOME", $JDK_Root_Folder, 'Machine')
        [System.Environment]::SetEnvironmentVariable("JAVA_HOME", $JDK_Root_Folder, 'User')
        Write-Host "Set JAVA_HOME to $JDK_Root_Folder"
        [System.Environment]::SetEnvironmentVariable("CLASSPATH", "%JAVA_HOME%\lib\dt.jar;%JAVA_HOME%\lib\tools.jar;", 'Machine')
        [System.Environment]::SetEnvironmentVariable("CLASSPATH", "%JAVA_HOME%\lib\dt.jar;%JAVA_HOME%\lib\tools.jar;", 'User')
        Write-Host "Set CLASSPATH to `"%JAVA_HOME%\lib\dt.jar;%JAVA_HOME%\lib\tools.jar;`""
        Write-Host

        $Path += "%JAVA_HOME%\bin;"
    }

    Write-Host '2. Install Python'
    Write-Host

    $skip = 'N'

    & python.exe --version
    if ($?) {
        Write-Host
        $skip = Read-Host "Python already exists, skip the installation? (Y/N)"
        Write-Host
    }

    if ($skip -eq 'N') {
        Write-Host
        Write-Host "==============================================="
        Write-Host "Please select the Python version to install:"
        Write-Host
        Write-Host "  1: Python 3.9.13 (default)"
        Write-Host "  2: Python 3.8.13"
        Write-Host "  3: Python 3.7.13"
        Write-Host "  4: Other"
        Write-Host "==============================================="
        Write-Host

        $choice = Read-Host "Please select the Python version to install"
        Write-Host

        $Python_VER = "3.9.13"

        if ($choice -eq '2') {
            $Python_VER = "3.8.13"
        } elseif ($choice -eq '3') {
            $Python_VER = "3.7.13"
        } elseif ($choice -eq '4') {
            $Python_VER = Read-Host "Please input the Python version to install (for example 3.9.13)"
        }

        $Python_Installer_Url = "https://www.python.org/ftp/python/$($Python_VER)/python-$($Python_VER)-amd64.exe"
        $Python_Installer_destination = Join-Path $destination "python-$($Python_VER)-amd64.exe"

        Write-Host "Downloading Python_Installer from $Python_Installer_Url to $Python_Installer_destination"
        Write-Host

        Invoke-WebRequest -Uri $Python_Installer_Url -OutFile $Python_Installer_destination
        if (!(Test-Path $Python_Installer_destination)) {
            Write-Host "Downloading $Python_Installer_destination failed"
            Exit
        }

        $targetDir = "C:\Python"

        $Arguments = @()
        $Arguments += "/quiet"
        $Arguments += "/i"
        $Arguments += 'InstallAllUsers="1"'
        $Arguments += 'TargetDir="' + $targetDir + '"'
        $Arguments += 'DefaultAllUsersTargetDir="' + $targetDir + '"'
        $Arguments += 'AssociateFiles="1"'
        $Arguments += 'PrependPath="1"'
        $Arguments += 'Include_doc="1"'
        $Arguments += 'Include_debug="1"'
        $Arguments += 'Include_dev="1"'
        $Arguments += 'Include_exe="1"'
        $Arguments += 'Include_launcher="1"'
        $Arguments += 'InstallLauncherAllUsers="1"'
        $Arguments += 'Include_lib="1"'
        $Arguments += 'Include_pip="1"'
        $Arguments += 'Include_symbols="1"'
        $Arguments += 'Include_tcltk="1"'
        $Arguments += 'Include_test="1"'
        $Arguments += 'Include_tools="1"'
        $Arguments += 'Include_launcher="1"'
        $Arguments += 'Include_launcher="1"'
        $Arguments += 'Include_launcher="1"'
        $Arguments += 'Include_launcher="1"'
        $Arguments += 'Include_launcher="1"'
        $Arguments += 'Include_launcher="1"'
        $Arguments += "/passive"

        try {
            Write-Host "Installing Python $Python_VER to $targetDir. This may take some time. Please wait..."
            $proc = Start-Process -FilePath "$Python_Installer_destination" -ArgumentList $Arguments -Wait -PassThru
            $proc.waitForExit()
            Write-Host 'Installation Done.'
            Write-Host
        } catch [exception] {
            write-host '$_ is' $_
            write-host '$_.GetType().FullName is' $_.GetType().FullName
            write-host '$_.Exception is' $_.Exception
            write-host '$_.Exception.GetType().FullName is' $_.Exception.GetType().FullName
            write-host '$_.Exception.Message is' $_.Exception.Message
        }

        $Path += "$targetDir;$targetDir\Scripts;"
    }

    Write-Host '3. Install Android command line tools and platform tools'
    Write-Host 

    $skip = 'N'

    $ANDROID_HOME = Join-Path (Convert-Path "~") "AppData\Local\Android\Sdk"

    if (!(Test-Path $ANDROID_HOME)) {
        New-Item -Path $ANDROID_HOME -ItemType Directory
        Write-Host
    } else {
        $skip = Read-Host "~\AppData\Local\Android\Sdk already exists, skip the installation? (Y/N)"
        Write-Host
    }

    if ($skip -eq 'N') {

        $url = "https://developer.android.com/studio#downloads"

        $request = Invoke-WebRequest $url
        $content = $request.Content

        $SDK_tools_package = [regex]::Matches($content, 'https://dl.google.com/android/repository/commandlinetools-win-[0-9]*_latest.zip')
        $SDK_tools_package_url = $SDK_tools_package[0].Value
        $SDK_tools_package_destination = Join-Path $destination "command_line_tools.zip"

        Write-Host "Downloading SDK_tools_package from $SDK_tools_package_url to $SDK_tools_package_destination"
        Write-Host 

        Invoke-WebRequest -Uri $SDK_tools_package_url -OutFile $SDK_tools_package_destination

        Write-Host "Expanding archive $SDK_tools_package_destination to $ANDROID_HOME. This may take some time. Please wait..."
        Write-Host 

        Expand-Archive -Path $SDK_tools_package_destination -DestinationPath $ANDROID_HOME -Force

        $Arguments = @()
        $Arguments += "--sdk_root=$ANDROID_HOME"
        $Arguments += "platform-tools"
        $Arguments += "emulator"
        $Arguments += "build-tools;33.0.0"
        $Arguments += "platforms;android-33"

        try {
            Write-Host "Installing platform-tools, emulator, build-tools and platforms. This may take some time. Please wait..."
            & "$ANDROID_HOME/cmdline-tools/bin/sdkmanager.bat" $Arguments
            Write-Host 'Installation Done.'
            Write-Host 
        } catch [exception] {
            write-host '$_ is' $_
            write-host '$_.GetType().FullName is' $_.GetType().FullName
            write-host '$_.Exception is' $_.Exception
            write-host '$_.Exception.GetType().FullName is' $_.Exception.GetType().FullName
            write-host '$_.Exception.Message is' $_.Exception.Message
        }

        Write-Host "Setting Android Sdk environment variables. This may take some time. Please wait..."
        Write-Host 

        [System.Environment]::SetEnvironmentVariable("ANDROID_HOME", $ANDROID_HOME, 'Machine')
        [System.Environment]::SetEnvironmentVariable("ANDROID_HOME", $ANDROID_HOME, 'User')
        Write-Host "Set ANDROID_HOME to $ANDROID_HOME"
        [System.Environment]::SetEnvironmentVariable("ANDROID_PLATFORM_TOOLS", "%ANDROID_HOME%\platform-tools", 'Machine')
        [System.Environment]::SetEnvironmentVariable("ANDROID_PLATFORM_TOOLS", "%ANDROID_HOME%\platform-tools", 'User')
        Write-Host "Set ANDROID_PLATFORM_TOOLS to `"%JAVA_HOME%\platform-tools`""
        Write-Host

        $Path += "%ANDROID_HOME%;%ANDROID_PLATFORM_TOOLS%;"
    }

    Write-Host '4. Install Node.js'
    Write-Host

    $skip = 'N'

    & node.exe --version
    if ($?) {
        Write-Host
        $skip = Read-Host "NodeJs already exists, skip the installation? (Y/N)"
        Write-Host
    }

    if ($skip -eq 'N') {
        $url = "https://nodejs.org/en/download"

        $request = Invoke-WebRequest $url
        $content = $request.Content

        $Nodejs_Installer_Urls = [regex]::Matches($content, 'https://nodejs.org/dist/v[0-9.]+/node-v[0-9.]+-x64.msi')
        $Nodejs_Installer_Url = $Nodejs_Installer_Urls[0].Value

        $Nodejs_Installer_destination = Join-Path $destination $Nodejs_Installer_Url.Split('/')[-1]

        Write-Host "Downloading Python_Installer from $Nodejs_Installer_Url to $Nodejs_Installer_destination"
        Write-Host

        Invoke-WebRequest -Uri $Nodejs_Installer_Url -OutFile $Nodejs_Installer_destination
        if (!(Test-Path $Nodejs_Installer_destination)) {
            Write-Host "Downloading $Nodejs_Installer_destination failed"
            Exit
        }

        $Arguments = @()
        $Arguments += "/i"
        $Arguments += "`"$(Resolve-Path $Nodejs_Installer_destination)`""
        $Arguments += '/quiet'
        $Arguments += '/qn'

        try {
            Write-Host "Installing Node.js $($Nodejs_Installer_Url.Split('/')[-1]). This may take some time. Please wait..."
            $proc = Start-Process "msiexec.exe" -ArgumentList $Arguments -Wait -PassThru
            $proc.waitForExit()
            Write-Host 'Installation Done.'
            Write-Host
        } catch [exception] {
            write-host '$_ is' $_
            write-host '$_.GetType().FullName is' $_.GetType().FullName
            write-host '$_.Exception is' $_.Exception
            write-host '$_.Exception.GetType().FullName is' $_.Exception.GetType().FullName
            write-host '$_.Exception.Message is' $_.Exception.Message
        }

        Write-Host "Setting Node.js environment variables. This may take some time. Please wait..."
        Write-Host 

        [System.Environment]::SetEnvironmentVariable("NODE_PATH", "$(Join-Path (Convert-Path "~") "AppData\Roaming\npm\node_modules")", 'Machine')
        [System.Environment]::SetEnvironmentVariable("NODE_PATH", "$(Join-Path (Convert-Path "~") "AppData\Roaming\npm\node_modules")", 'User')
        Write-Host "Set NODE_PATH to $(Join-Path (Convert-Path "~") "AppData\Roaming\npm\node_modules")"
        Write-Host

        $Path += "$(Join-Path (Convert-Path "~") "AppData\Roaming\npm\node_modules");C:\Program Files\nodejs;"
    }

    Write-Host '5. Install Appium'
    Write-Host

    $skip = 'N'

    & appium -v
    if ($?) {
        Write-Host
        $skip = Read-Host "Appium already exists, skip the installation? (Y/N)"
        Write-Host
    }

    if ($skip -eq 'N') {
        $Arguments = @()
        $Arguments += "install"
        $Arguments += "-g appium"

        try {
            Write-Host "Installing Appium. This may take some time. Please wait..."
            $proc = Start-Process -FilePath "C:\Program Files\nodejs\npm" -ArgumentList $Arguments -Wait -PassThru
            $proc.waitForExit()
            Write-Host 'Installation Done.'
            Write-Host
        } catch [exception] {
            write-host '$_ is' $_
            write-host '$_.GetType().FullName is' $_.GetType().FullName
            write-host '$_.Exception is' $_.Exception
            write-host '$_.Exception.GetType().FullName is' $_.Exception.GetType().FullName
            write-host '$_.Exception.Message is' $_.Exception.Message
        }

        [System.Environment]::SetEnvironmentVariable("APPIUM_BINARY_PATH", "$(Join-Path (Convert-Path "~") "AppData\Roaming\npm\node_modules\appium\build\lib\main.js")", 'Machine')
        [System.Environment]::SetEnvironmentVariable("APPIUM_BINARY_PATH", "$(Join-Path (Convert-Path "~") "AppData\Roaming\npm\node_modules\appium\build\lib\main.js")", 'User')
        Write-Host "Set APPIUM_BINARY_PATH to $(Join-Path (Convert-Path "~") "AppData\Roaming\npm\node_modules\appium\build\lib\main.js")"
        Write-Host
    }

    Write-Host '6. Install WinAppDriver'
    Write-Host

    $WinAppDriver_Installer_Url = "https://github.com/microsoft/WinAppDriver/releases/download/v1.2.1/WindowsApplicationDriver_1.2.1.msi"
    $WinAppDriver_Installer_destination = Join-Path $destination "WindowsApplicationDriver_1.2.1.msi"

    Write-Host "Downloading WinAppDriver Installer from $WinAppDriver_Installer_Url to $WinAppDriver_Installer_destination"
    Write-Host

    Invoke-WebRequest -Uri $WinAppDriver_Installer_Url -OutFile $WinAppDriver_Installer_destination
    if (!(Test-Path $WinAppDriver_Installer_destination)) {
        Write-Host "Downloading $WinAppDriver_Installer_destination failed"
        Exit
    }

    $Arguments = @()
    $Arguments += "/i"
    $Arguments += "`"$(Resolve-Path $WinAppDriver_Installer_destination)`""
    $Arguments += '/quiet'

    try {
        Write-Host "Installing WinAppDriver 1.2.1. This may take some time. Please wait..."
        $proc = Start-Process "msiexec.exe" -ArgumentList $Arguments -Wait -PassThru
        $proc.waitForExit()
        Write-Host 'Installation Done.'
        Write-Host
    } catch [exception] {
        write-host '$_ is' $_
        write-host '$_.GetType().FullName is' $_.GetType().FullName
        write-host '$_.Exception is' $_.Exception
        write-host '$_.Exception.GetType().FullName is' $_.Exception.GetType().FullName
        write-host '$_.Exception.Message is' $_.Exception.Message
    }

    Write-Host '7. Install FFmpeg'
    Write-Host

    $skip = 'N'

    & ffmpeg -version
    if ($?) {
        Write-Host
        $skip = Read-Host "FFmpeg already exists, skip the installation? (Y/N)"
        Write-Host
    }

    if ($skip -eq 'N') {
        $USER_HOME = Convert-Path "~"

        $url = "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip"

        $FFmpeg_package_url = "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip"
        $FFmpeg_package_destination = Join-Path $destination "ffmpeg-release-essentials.zip"

        Write-Host "Downloading FFmpeg from $FFmpeg_package_url to $FFmpeg_package_destination"
        Write-Host 

        Invoke-WebRequest -Uri $FFmpeg_package_url -OutFile $FFmpeg_package_destination

        Write-Host "Expanding archive $FFmpeg_package_destination to $USER_HOME. This may take some time. Please wait..."
        Write-Host 

        Expand-Archive -Path $FFmpeg_package_destination -DestinationPath $USER_HOME -Force

        $FFmpeg_Root_Folder = ''

        foreach ($Sub_Folder in (Get-ChildItem $USER_HOME | Sort-Object -Property LastWriteTime -Descending)){
            if ($Sub_Folder.PSChildName.contains("ffmpeg")){
                $FFmpeg_Root_Folder = Join-Path $USER_HOME $Sub_Folder.PSChildName
                break
            }
        }

        $Path += "$(Join-Path $FFmpeg_Root_Folder 'bin');"
    }

    Write-Host "8. Setting PATH. This may take some time. Please wait..."
    Write-Host

    Write-Host "Adding $Path to PATH."
    Write-Host

    [System.Environment]::SetEnvironmentVariable("PATH", "$([Environment]::GetEnvironmentVariable('Path', 'Machine'));$Path", 'Machine')
    [System.Environment]::SetEnvironmentVariable("PATH", "$([Environment]::GetEnvironmentVariable('Path', 'User'));$Path", 'User')

    Write-Host "User PATH:"

    foreach ($user_PATH in [Environment]::GetEnvironmentVariable('Path', 'User').Split(';')) {
        if ($user_PATH) {
            Write-Host $user_PATH
        }
    }
    Write-Host

    Write-Host "Machine PATH:"

    foreach ($machine_PATH in [Environment]::GetEnvironmentVariable('Path', 'Machine').Split(';')) {
        if ($machine_PATH) {
            Write-Host $machine_PATH
        }
    }
    Write-Host

    Write-Host "The agent deployment is complete, please reboot before start the agent"
    Write-Host

} else {
    $Arguments = @()
    $Arguments += "-noprofile"
    $Arguments += "-noexit"
    $Arguments += "-file $($myinvocation.MyCommand.Definition)"
    $Arguments += '-elevated'

    Write-Host "Will run as administrator in a new window"
    Write-Host

    Start-Process powershell.exe -Verb RunAs -ArgumentList $Arguments
}

