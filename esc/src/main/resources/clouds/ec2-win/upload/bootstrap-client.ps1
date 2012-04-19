###############################################################################
# Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
################################################################################


#############################################################################
# This script starts a Gigaspaces agent for use with the Gigaspaces
# Cloudify. The agent will function as management depending on the value of $GSA_MODE
#
# Example:
# Invoke-Command -ComputerName ec2-107-21-132-11.compute-1.amazonaws.com -Credential $cred -ScriptBlock {#$ENV:LUS_IP_ADDRESS=10.46.178;$ENV:GSA_MODE=lus;$ENV:NO_WEB_SERVICES=false;$ENV:MACHINE_IP_ADDRESS=10.46.178.72;$ENV:MACHINE_ZONES="ZONE";$ENV:WORKING_HOME_DIRECTORY="C:\Users\Administrator\Documents";$ENV:CLOUDIFY_LINK="http://repository.cloudifysource.org/org/cloudifysource/2.1.0/gigaspaces-cloudify-2.1.0-m2-b1193-82.zip";$ENV:CLOUD_FILE="ec2-cloud.groovy";.\bootstrap-management.ps1 }
#  
#
# The following environment variables should be set before calling this script:
# 	$LUS_IP_ADDRESS - Ip of the head node that runs a LUS and ESM. May be my IP. (Required)
#   $GSA_MODE - 'agent' if this node should join an already running node. Otherwise, any value.
#	$NO_WEB_SERVICES - 'true' if web-services (rest, webui) should not be deployed (only if GSA_MODE != 'agent')
#   $MACHINE_IP_ADDRESS - The IP of this server (Useful if multiple NICs exist)
#	$MACHINE_ZONES - This is required if this is not a management machine
# 	$WORKING_HOME_DIRECTORY - This is where the files were copied to (cloudify installation, etc..)
#	$CLOUDIFY_LINK - If this url is found, it will be downloaded to $WORKING_HOME_DIRECTORY/gigaspaces.zip
#	$CLOUDIFY_OVERRIDES_LINK - If this url is found, it will be downloaded to $WORKING_HOME_DIRECTORY/gigaspaces.zip
#   $CLOUD_FILE - File name of the cloud file, which should be placed in the WORKING_HOME_DIRECTORY
#
# Author: barakm
# Since: 2.1
#############################################################################

param ([string]$target, [string]$username, [string]$password, [string]$command)

$ErrorActionPreference="Stop"
$securePassword = ConvertTo-SecureString -AsPlainText -Force "$password"
$cred = New-Object System.Management.Automation.PSCredential $username, $securePassword

Write-Host "Connecting to management service of $target"
Connect-WSMan -Credential $cred $target 

set-item WSMan:\$target\Client\TrustedHOsts -Value * -Force
set-item WSMan:\$target\Shell\MaxMemoryPerShellMB -Value 0 -Force

Write-Host Invoking command on Remote host $target
Invoke-Command -ComputerName $target -Credential $cred  -ScriptBlock { 	
	Invoke-Expression $args[0]
} -ArgumentList $command
Write-Host "Command finished"