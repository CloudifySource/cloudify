@echo off
set JSHOMEDIR=d:\gigaspaces-cloudify-8.0.4-m3
set NIC_ADDR=localhost
set LOOKUPLOCATORS=localhost


echo terminating all java processes
taskkill /im java.exe /f >nul 2>&1

echo starting agent, rest and ui servers

pushd %JSHOMEDIR%\bin
start /min cmd /c "gs-agent.bat gsa.global.esm 1 gsa.gsc 0 gsa.global.gsm 1 gsa.global.lus 1
popd

pushd %JSHOMEDIR%\tools\gs-webui
start /min gs-webui.bat
popd

pushd %JSHOMEDIR%\tools\rest
start /min %JSHOMEDIR%\tools\rest\restful.bat
popd

cls
start %JSHOMEDIR%\tools\cli\cloudify.bat

ping 127.0.0.1 -n 5 -w 1000 > NUL

start http://localhost:8099
start http://localhost:8100

:end