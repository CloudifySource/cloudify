@echo off
rem /*******************************************************************************
rem  * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
rem  *
rem  * Licensed under the Apache License, Version 2.0 (the "License");
rem  * you may not use this file except in compliance with the License.
rem  * You may obtain a copy of the License at
rem  *
rem  *       http://www.apache.org/licenses/LICENSE-2.0
rem  *
rem  * Unless required by applicable law or agreed to in writing, software
rem  * distributed under the License is distributed on an "AS IS" BASIS,
rem  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem  * See the License for the specific language governing permissions and
rem  * limitations under the License.
rem  *******************************************************************************/



rem Bootstrapping launcher for Windows based cloudify agent.
rem This file launches a power shell script that is responsible for downloading and setting up cloudify,
rem as well as creating the agent startup script which will be invoked asynchronously via the task scheduler.
rem Author: barakm
rem Since: 2.1


echo Setting execution policy
powershell Set-ExecutionPolicy Unrestricted

rem powershell set-item wsman:localhost\Shell\MaxMemoryPerShellMB 2048

echo Moving to Directory: %WORKING_HOME_DIRECTORY%
cd %WORKING_HOME_DIRECTORY%

echo executing bootstrap script
powershell .\bootstrap-management.ps1 
echo scheduling cloudify task
schtasks.exe /create /TN cloudify-task /SC ONSTART /TR %CD%\run.bat
echo running cloudify task
schtasks.exe /run /TN cloudify-task 

echo Cloudify Task scheduled for immediate execution. Please wait for Cloudify agent to become available.

rem schtasks.exe /delete /F /TN cloudify-task 
