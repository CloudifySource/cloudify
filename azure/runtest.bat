echo on
rem cli log files under azure folder.
set path=%path%;C:\Windows\Microsoft.NET\Framework\v4.0.30319

set EXIT_CODE=0

set MS_BUILD_MODE=Release

set AZURE_BUILD_TYPE=%1
set GS_VERSION=%2
set BUILD_NUMBER=%3
set MILESTONE=%4

echo compiling GigaSpacesWorkerRoles
cd GigaSpacesWorkerRoles
msbuild GigaSpacesWorkerRoles\template.ccproj /p:Configuration=%MS_BUILD_MODE%
cd ..

echo creating azureconfig.exe
cd azureconfig
msbuild azureconfig\azureconfig.csproj /p:Configuration=%MS_BUILD_MODE%
cd ..

del /s /q localworkingdir
rmdir /s /q localworkingdir
mkdir localworkingdir
set LOCAL_WORKING_DIR=%~dp0localworkingdir\

copy %~dp0azureconfig\azureconfig\bin\%MS_BUILD_MODE%\azureconfig.exe* %LOCAL_WORKING_DIR% /Y

echo creating WorkerRoles.zip

mkdir WorkerRoles
mkdir WorkerRoles\RoleCommon
mkdir WorkerRoles\internal
mkdir WorkerRoles\ui
mkdir WorkerRoles\management
mkdir WorkerRoles\GigaSpacesWorkerRoles

xcopy GigaSpacesWorkerRoles\GigaSpacesWorkerRoles\ServiceConfiguration.Cloud.cscfg WorkerRoles\GigaSpacesWorkerRoles /Y
xcopy GigaSpacesWorkerRoles\GigaSpacesWorkerRoles\ServiceDefinition.csdef WorkerRoles\GigaSpacesWorkerRoles /Y
xcopy GigaSpacesWorkerRoles\RoleCommon\bin\%MS_BUILD_MODE% WorkerRoles\RoleCommon /Y
xcopy GigaSpacesWorkerRoles\ui\bin\%MS_BUILD_MODE% WorkerRoles\ui /Y /E
xcopy GigaSpacesWorkerRoles\management\bin\%MS_BUILD_MODE% WorkerRoles\management /Y
xcopy GigaSpacesWorkerRoles\internal\bin\%MS_BUILD_MODE% WorkerRoles\internal /Y

7za a -tzip WorkerRoles.zip WorkerRoles

move /Y WorkerRoles.zip %LOCAL_WORKING_DIR%

if "%AZURE_BUILD_TYPE%" == "" goto exit
if "%AZURE_BUILD_TYPE%" == "BUILD" goto exit
if "%AZURE_BUILD_TYPE%" == "build" goto exit
if "%AZURE_BUILD_TYPE%" == "TEST" goto test
if "%AZURE_BUILD_TYPE%" == "test" goto test

:test

cd ..
subst L: %CD%
cd /d L:\

set LOCAL_WORKING_DIR=l:\azure\localworkingdir\

set CLOUDIFY_REMOTE_PATH=\\tarzan\builds\cloudify\%GS_VERSION%\build_%BUILD_NUMBER%\cloudify

echo copying latest cloudify build
set GIGASPACES_XAP_REMOTE_PATH=%CLOUDIFY_REMOTE_PATH%\1.5\gigaspaces-cloudify-%GS_VERSION%-%MILESTONE%-b%BUILD_NUMBER%.zip
copy %GIGASPACES_XAP_REMOTE_PATH% %LOCAL_WORKING_DIR%gigaspaces-latest.zip /Y

REM Even though we create these files above, we are also testing that these files were actually copied to tarzan properly
echo copying WorkerRoles
set WORKER_ROLES_REMOTE_PATH=%CLOUDIFY_REMOTE_PATH%\azure\WorkerRoles.zip
copy %WORKER_ROLES_REMOTE_PATH% %LOCAL_WORKING_DIR%WorkerRoles.zip /Y

cd %LOCAL_WORKING_DIR%
7za -y x gigaspaces-latest.zip
move /Y gigaspaces-cloudify-%GS_VERSION%-%MILESTONE% gigaspaces

7za -y x WorkerRoles.zip
xcopy /s /e /i WorkerRoles gigaspaces\tools\cli\plugins\azure\WorkerRoles
rmdir /s /q WorkerRoles
cd ..

REM FOR TESTING
REM cd ..
REM call copy.bat
REM cd azure
REM END OF FOR TESTING

cd azure
call mvn -Dproject.version=2.0.0 -DskipTests=true install
call mvn -Dproject.version=2.0.0 -DskipTests=false -Dsurefire.useFile=false -Dlocal.working.dir=%LOCAL_WORKING_DIR% test
REM call mvn -Dtest.debug.mode=true -Dmaven.surefire.debug="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000 -Xnoagent -Djava.compiler=NONE" -DskipTests=false -DuseFile=false -Dlocal.working.dir=%LOCAL_WORKING_DIR% test

if ERRORLEVEL 1 goto err
goto :end
:err
set EXIT_CODE=1
:end

cd .. 

c:
subst /D L:

:exit
exit /B %EXIT_CODE%