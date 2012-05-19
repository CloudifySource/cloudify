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
# 	LUS_IP_ADDRESS - Ip of the head node that runs a LUS and ESM. May be my IP. (Required)
#   GSA_MODE - 'agent' if this node should join an already running node. Otherwise, any value.
#	NO_WEB_SERVICES - 'true' if web-services (rest, webui) should not be deployed (only if GSA_MODE != 'agent')
#   MACHINE_IP_ADDRESS - The IP of this server (Useful if multiple NICs exist)
#	MACHINE_ZONES - This is required if this is not a management machine
# 	WORKING_HOME_DIRECTORY - This is where the files were copied to (cloudify installation, etc..)
#	CLOUDIFY_LINK - If this url is found, it will be downloaded to $WORKING_HOME_DIRECTORY/gigaspaces.zip
#	CLOUDIFY_OVERRIDES_LINK - If this url is found, it will be downloaded to $WORKING_HOME_DIRECTORY/gigaspaces.zip
#   CLOUD_FILE - File name of the cloud file, which should be placed in the WORKING_HOME_DIRECTORY
#   USERNAME - Username of the account.
#   PASSWORD - Password of the account.
#
# Author: barakm
# Since: 2.1
#############################################################################

Function unzip ($zipFile, $destinationDirName)
{
	Write-Host Extracting $zipFile to $destinationDirName
	$shellApplication = new-object -com shell.application 
	$zipPackage = $shellApplication.NameSpace($zipFile) 
	mkdir $destinationDirName | Out-Null

	$destinationFolder = $shellApplication.NameSpace($destinationDirName) 

	# CopyHere vOptions 
	# 4- Do not display a progress dialog box. 
	# 16 - Respond with "Yes to All" for any dialog box that is displayed. 
	$destinationFolder.CopyHere($zipPackage.Items(),0x14) 
	

}

Function download ($url, $destinationFile) 
{
	Write-Host Downloading $url to $destinationFile
	$webclient = New-Object System.Net.WebClient
	$webclient.DownloadFile($url,$destinationFile)
}

Function insert-line($file, $content) 
{
	Write-Host inserting $content to $file
	($content, (gc $file)) | Out-File -encoding ASCII $file 
}


$ErrorActionPreference="Stop"

# Can't set policy here, since the policy has to be set BEFORE the script runs.
# So we have a batch file that sets this and then calls the powershell script.
# Write-Host Setting execution policy
# Set-ExecutionPolicy Unrestricted

# Write-Host Moving to Directory: $ENV:WORKING_HOME_DIRECTORY
# cd $ENV:WORKING_HOME_DIRECTORY


CD $ENV:WORKING_HOME_DIRECTORY
$workDirectory= (Get-Location).Path
$parentDirectory = Split-Path -parent $workDirectory

$javaZip = "$parentDirectory\java.zip"
$javaDir = "$parentDirectory\java"


# Multicast is off by default in Cloudify
$Env:EXT_JAVA_OPTIONS="-Dcom.gs.multicast.enabled=false"

# Download Java
download "http://repository.cloudifysource.org/com/oracle/java/1.6.0_25/jdk1.6.0_25_x64.zip" $javaZip
# Unzip Java
unzip $javaZip $javaDir
# move one folder up, to standardize across versions
move $javaDir\*\* $javaDir

$cloudifyZip = "$parentDirectory\gigaspaces.zip"
$cloudifyDir = "$parentDirectory\gigaspaces"

# Download Cloudify
download $ENV:CLOUDIFY_LINK $cloudifyZip
# unzip Cloudify
unzip $cloudifyZip $cloudifyDir
# move one folder up, to standardize across versions
move $cloudifyDir\*\* $cloudifyDir

# Download Cloudify Overrides
if(Test-Path Env:\CLOUDIFY_OVERRIDES_LINK) {
	download $ENV:CLOUDIFY_OVERRIDES_LINK $parentDirectory\gigaspaces-overrides.zip
	# unzip Cloudify-overrides
	unzip $parentDirectory\gigaspaces-overrides.zip $cloudifyDir
}

if(Test-Path $workDirectory\cloudify-overrides) {
	copy -Recurse -Force $workDirectory\cloudify-overrides\* $cloudifyDir
}

# UPDATE SETENV SCRIPT...
Write-Host Updating environment script
insert-line $cloudifyDir\bin\setenv.bat "set NIC_ADDR=$ENV:MACHINE_IP_ADDRESS"
insert-line $cloudifyDir\bin\setenv.bat "set LOOKUPLOCATORS=$ENV:LUS_IP_ADDRESS"
insert-line $cloudifyDir\bin\setenv.bat "set JAVA_HOME=$javaDir"
insert-line $cloudifyDir\bin\setenv.bat 'set CLOUDIFY_AGENT_ENV_PRIVATE_IP=$ENV:CLOUDIFY_AGENT_ENV_PRIVATE_IP'
insert-line $cloudifyDir\bin\setenv.bat 'set CLOUDIFY_AGENT_ENV_PUBLIC_IP=$ENV:CLOUDIFY_AGENT_ENV_PUBLIC_IP'

Write-Host "Disabling local firewall"
$firewallCommand = "netsh advfirewall set allprofiles state off"
Set-Content -Encoding ASCII firewall.bat $firewallCommand
cmd.exe /c firewall.bat
rm -Force firewall.bat

# create the launch commandline
if ($ENV:GSA_MODE -eq "agent")
{
	Write-Host "Starting agent node"
	$cloudifyCommand = "$cloudifyDir\tools\cli\cloudify.bat start-agent -timeout 30 --verbose -zone $ENV:MACHINE_ZONES -auto-shutdown"
}
else {
	
	# Cloud file in Java must use slash ('/') not back-slash ('\')
	$cloudFile = $ENV:CLOUD_FILE.replace("\", "/")

	Write-Host "Starting management node"
	if ($ENV:NO_WEB_SERVICES -eq "true") 
	{
		$cloudifyCommand = "cmd.exe /c $cloudifyDir\tools\cli\cloudify.bat start-management -no-web-services -no-management-space -timeout 30 --verbose -auto-shutdown -cloud-file $cloudFile"
	} 
	else {
		$cloudifyCommand = "cmd.exe /c $cloudifyDir\tools\cli\cloudify.bat start-management -timeout 30 --verbose -auto-shutdown -cloud-file $cloudFile"
	}

}

# Invoke-Command -ComputerName localhost -ScriptBlock {Invoke-Expression $args[0]} -ArgumentList $cloudifyCommand

# Executing the $cloudifyCommand command will work, but on EC2 we get error 1816 on one of the started processes.
# this prevents the managememnt server from starting correctly.
# So the actual start command needs to run in a separate session. Tried doing this with a remote command to 'localhost'
# but we get an access denied error. So this has to happen with a scheduled task.
# here we create the batch file that the task runs
Set-Content -Encoding ASCII -Force -Value $cloudifyCommand run.bat

Write-Host scheduling cloudify task with password $ENV:PASSWORD
schtasks.exe /create /TN cloudify-task /SC ONSTART /TR $ENV:WORKING_HOME_DIRECTORY\run.bat /RU "$ENV:USERNAME" /RP "$ENV:PASSWORD"
Write-Host running cloudify task
schtasks.exe /run /TN cloudify-task 

# Note that the password environment variable is NOT passed to the agent, as the agent runs in a different session, spawned from the task scheduler!
Write-Host Cloudify Task scheduled for immediate execution. Please wait for Cloudify agent to become available.

$endTime = (get-date).addMinutes(1)

while($true) {
	schtasks /query | select-string cloudify-task | %{$found = $true}
	
	if($found -eq $true) {
		break
	} else {
		Write-Host "Cloudify task still not available"
		if((get-date) -gt $endTime) {
			Write-Host "Timeout while waiting for cloudify task to become available"
			exit(100)
		}
	}
	 start-sleep -s 5
	
}

Write-Host "Cloudify Task is available"

$endTime = (get-date).addMinutes(10)

while($true) {
	schtasks /query | select-string cloudify-task |%{
		if ($_.tostring().contains("Running")) {
			Write-Host "Cloudify startup is still executing"
		} else  {
			break
		}
	}

	if((get-date) -gt $endTime) {
		Write-Host "Timeout while waiting for cloudify task to finish"
		exit(100)
	}
	 start-sleep -s 10
	
}

exit 0
