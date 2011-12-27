@ECHO OFF

@REM Require elevation
if "%_ELEVATED%"=="" (goto:Elevate) else (goto:Deploy)
:Elevate
start /min cscript /NoLogo "%~dp0.elevate.vbs" %~f0
exit

@REM Deploy the package to emulator
:Deploy
ant package.xml
"${EmulatorDir}\csrun.exe" "${PackageDir}\${PackageFileName}" "${PackageDir}\${ConfigurationFileName}"

@REM Ensure that emulator UI is running
for /f %%G in ('tasklist ^| find /I /C "dfui.exe"') do set _PROCCOUNT=%%G
if NOT %_PROCCOUNT%==0 goto:Bye
cd /d "${EmulatorDir}"
@REM start dfui.exe

choice /d y /t 5 /c Y /N

