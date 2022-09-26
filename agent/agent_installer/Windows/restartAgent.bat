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
set newfile=%1
net stop "Hydra Lab Agent Service"
if "%newfile%"=="" ( echo "No need to update" ) else (
    if not exist "%newfile%" ( echo "%newfile% not exist" ) else (
        echo "Updating"
        del agent.jar
        ren "%newfile%" agent.jar
    )
)
net start "Hydra Lab Agent Service"