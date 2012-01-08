@ECHO OFF

@REM Require elevation
@REM if "%_ELEVATED%"=="" (goto:Elevate) else (goto:Deploy)
@REM :Elevate
@REM start /min cscript /NoLogo "%~dp0.elevate.vbs" %~f0
@REM exit

@REM Deploy the package to emulator
:Deploy
"${EmulatorDir}\csrun.exe" "${PackageDir}\${PackageFileName}" "${PackageDir}\${ConfigurationFileName}"

@REM Ensure that emulator UI is running
@REM for /f %%G in ('tasklist ^| find /I /C "dfui.exe"') do set _PROCCOUNT=%%G
@REM if NOT %_PROCCOUNT%==0 goto:Bye
@REM cd /d "${EmulatorDir}"
@REM start dfui.exe

@REM choice /d y /t 5 /c Y /N

