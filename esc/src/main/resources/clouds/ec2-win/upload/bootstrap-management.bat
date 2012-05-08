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


rem Setting the execution policy has to happen outside the powershell script.
echo Setting execution policy
powershell Set-ExecutionPolicy Unrestricted

powershell %WORKING_HOME_DIRECTORY%\bootstrap-management.ps1 
