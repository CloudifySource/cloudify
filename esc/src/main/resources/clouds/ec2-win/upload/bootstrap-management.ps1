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
CD $ENV:WORKING_HOME_DIRECTORY
$workDirectory= (Get-Location).Path

# Multicast is off by default in Cloudify
$Env:EXT_JAVA_OPTIONS="-Dcom.gs.multicast.enabled=false"

# Download Java
download "http://repository.cloudifysource.org/com/oracle/java/1.6.0_25/jdk1.6.0_25_x64.zip" $workDirectory\java.zip
# Unzip Java
unzip $workDirectory\java.zip $workDirectory\java
# move one folder up, to standardize across versions
move $workDirectory\java\*\* $workDirectory\java\

# Download Cloudify
download $ENV:CLOUDIFY_LINK $workDirectory\gigaspaces.zip
# unzip Cloudify
unzip $workDirectory\gigaspaces.zip $workDirectory\gigaspaces
# move one folder up, to standardize across versions
move $workDirectory\gigaspaces\*\* $workDirectory\gigaspaces\

# Download Cloudify Overrides
if(Test-Path Env:\CLOUDIFY_OVERRIDES_LINK) {
	download $ENV:CLOUDIFY_OVERRIDES_LINK ".\gigaspaces-overrides.zip"
	# unzip Cloudify-overrides
	unzip ".\gigaspaces-overrides.zip" ".\gigaspaces"

}

if(Test-Path $workDirectory\cloudify-overrides) {
	copy -Recurse -Force $workDirectory\cloudify-overrides\* $workDirectory\gigaspaces
}

# UPDATE SETENV SCRIPT...
Write-Host Updating environment script
insert-line $workDirectory\gigaspaces\bin\setenv.bat "set NIC_ADDR=$ENV:MACHINE_IP_ADDRESS"
insert-line $workDirectory\gigaspaces\bin\setenv.bat "set LOOKUPLOCATORS=$ENV:LUS_IP_ADDRESS"
insert-line $workDirectory\gigaspaces\bin\setenv.bat "set JAVA_HOME=$workDirectory\java"

Write-Host "Disabling local firewall"
$firewallCommand = "netsh advfirewall set allprofiles state off"
Set-Content -Encoding ASCII firewall.bat $firewallCommand
cmd.exe /c firewall.bat
rm -Force firewall.bat


# create the launch commandline
if ($ENV:GSA_MODE -eq "agent")
{
	Write-Host "Starting agent node"
	$commandLine = "$workDirectory\gigaspaces\tools\cli\cloudify.bat start-agent -timeout 30 --verbose -zone $ENV:MACHINE_ZONES -auto-shutdown"
}
else {
	# Cloud file in Java must use slash ('/') not back-slash ('\')
	$cloudFile = $ENV:CLOUD_FILE.replace("\", "/")

	if ($ENV:NO_WEB_SERVICES -eq "true") 
	{
		$commandLine = "$workDirectory\gigaspaces\tools\cli\\cloudify.bat start-management -no-web-services -no-management-space -timeout 30 --verbose -auto-shutdown -cloud-file $cloudFile"
	} 
	else {
		Write-Host "Starting management node"
		$commandLine = "$workDirectory\gigaspaces\tools\cli\cloudify.bat start-management -timeout 30 --verbose -auto-shutdown -cloud-file $cloudFile"
	}

}
Set-Content -Encoding ASCII $workDirectory\run.bat $commandLine

exit 0
