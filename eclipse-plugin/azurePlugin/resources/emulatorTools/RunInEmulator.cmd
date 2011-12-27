@ECHO OFF

@REM Require elevation
if "%_ELEVATED%"=="" (goto:Elevate) else (goto:Deploy)
:Elevate
start /min cscript /NoLogo "%~dp0.elevate.vbs" %~f0
exit

@REM Deploy the package to emulator
:Deploy
ant package.xml
"C:\Program Files\Windows Azure Emulator\emulator\csrun.exe" "D:\Gigaspaces\workspaces\eclipse4azure\azurePlugin\deploy\WindowsAzurePackage.cspkg" "D:\Gigaspaces\workspaces\eclipse4azure\azurePlugin\deploy\ServiceConfiguration.cscfg"

@REM Ensure that emulator UI is running
for /f %%G in ('tasklist ^| find /I /C "dfui.exe"') do set _PROCCOUNT=%%G
if NOT %_PROCCOUNT%==0 goto:Bye
cd /d "C:\Program Files\Windows Azure Emulator\emulator"
@REM start dfui.exe

choice /d y /t 5 /c Y /N

