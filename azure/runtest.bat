echo on

rem cli log files under azure folder.
set path=%path%;C:\Windows\Microsoft.NET\Framework\v4.0.30319

set EXIT_CODE=0

set MS_BUILD_MODE=Release

set AZURE_BUILD_TYPE=%1
set GS_VERSION=%2
set BUILD_NUMBER=%3
set MILESTONE=%4
set azure.blob.accountname=%5
set azure.blob.accountkey="%6"
set azure.services.subscription=%7
set azure.services.certificate=%8

echo compiling GigaSpacesWorkerRoles
cd GigaSpacesWorkerRoles
rmdir /s /q RoleCommon\bin
rmdir /s /q RoleCommon\obj
rmdir /s /q ui\bin
rmdir /s /q ui\obj
rmdir /s /q management\bin
rmdir /s /q management\obj
rmdir /s /q internal\bin
rmdir /s /q internal\obj

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

xcopy GigaSpacesWorkerRoles\GigaSpacesWorkerRoles\ServiceConfiguration.Cloud.cscfg WorkerRoles\GigaSpacesWorkerRoles /Y /E
xcopy GigaSpacesWorkerRoles\GigaSpacesWorkerRoles\ServiceDefinition.csdef WorkerRoles\GigaSpacesWorkerRoles /Y /E
xcopy GigaSpacesWorkerRoles\RoleCommon\bin\%MS_BUILD_MODE% WorkerRoles\RoleCommon /Y /E
xcopy GigaSpacesWorkerRoles\ui\bin\%MS_BUILD_MODE% WorkerRoles\ui /Y /E
xcopy GigaSpacesWorkerRoles\management\bin\%MS_BUILD_MODE% WorkerRoles\management /Y /E
xcopy GigaSpacesWorkerRoles\internal\bin\%MS_BUILD_MODE% WorkerRoles\internal /Y /E

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

cd %LOCAL_WORKING_DIR%
7za -y x gigaspaces-latest.zip
move /Y gigaspaces-cloudify-%GS_VERSION%-%MILESTONE% gigaspaces

REM FOR TESTING
REM 7za -y x WorkerRoles.zip
REM xcopy /y /s /e /i WorkerRoles gigaspaces\tools\cli\plugins\azure\WorkerRoles
REM xcopy /y azureconfig.exe gigaspaces\tools\cli\plugins\azure\azureconfig.exe
REM rmdir /s /q WorkerRoles
REM cd ..\..\
REM call copy.bat
REM cd azure\localworkingdir
REM END OF FOR TESTING

pushd l:\cloudify
call mvn -DskipTests=true clean install
popd

pushd l:\azure\azure
call mvn -DskipTests=false -Dsurefire.useFile=false -Dlocal.working.dir=%LOCAL_WORKING_DIR% -Dazure.blob.accountname=%azure.blob.accountname% -Dazure.blob.accountkey=%azure.blob.accountkey% -Dazure.services.subscription=%azure.services.subscription% -Dazure.services.certificate=%azure.services.certificate% test
REM call mvn -Dtest.debug.mode=true -Dmaven.surefire.debug="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000 -Xnoagent -Djava.compiler=NONE" -DskipTests=false -DuseFile=false -Dlocal.working.dir=%LOCAL_WORKING_DIR% test

if ERRORLEVEL 1 goto err
goto :end
:err
set EXIT_CODE=1
:end

popd

c:
subst /D L:

:exit
exit /B %EXIT_CODE%