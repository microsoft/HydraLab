@echo off
::check is admin
>nul 2>&1 "%SYSTEMROOT%\system32\cacls.exe" "%SYSTEMROOT%\system32\config\system"
if '%errorlevel%' NEQ '0' (
goto UACPrompt
) else ( goto gotAdmin )
:UACPrompt
echo Set UAC = CreateObject^("Shell.Application"^) > "%temp%\getadmin.vbs"
echo UAC.ShellExecute "%~s0", "%1", "", "runas", 1 >> "%temp%\getadmin.vbs"
"%temp%\getadmin.vbs"
exit /B
:gotAdmin
if exist "%temp%\getadmin.vbs" ( del "%temp%\getadmin.vbs" )

echo newfile = %1
set newfile = %1
::stop hydra lab agent service
net stop "Hydra Lab Agent Service"
::kill hydra lab agent java process
::Powershell -Command "& {Get-WmiObject Win32_Process -Filter \"name like '%%java%%' and CommandLine like '%%agent%%'\" | Select-Object ProcessId -OutVariable pids;if(-not $pids -eq '' ) {stop-process -id $pids.ProcessId}}"
if "%newfile%"=="" ( echo "No need to update" ) else (
    if not exist "%newfile%" ( echo "%newfile% not exist" ) else (
        echo "Updating"
        del agent.jar
        ren "%newfile%" agent.jar
    )
)
::start hydra lab agent in command mode
::java -Xms1024m -Xmx4096m -jar .\agent.jar
::start hydra lab agent in windows service mode
net start "Hydra Lab Agent Service"