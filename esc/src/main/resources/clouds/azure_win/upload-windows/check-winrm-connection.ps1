###############################################################################
# Copyright (c) 2014 GigaSpaces Technologies Ltd. All rights reserved
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
# WinRM connection script for use with Cloudify.
# This script will attemp to connect to the given target machine with the given credentials
#
# Author: noak
# Since: 2.7.1
#############################################################################

param ([string]$target, [string]$username, [string]$password)

$ErrorActionPreference="Stop"

# Set up the password
$securePassword = ConvertTo-SecureString -AsPlainText -Force $password
$cred = New-Object System.Management.Automation.PSCredential $username, $securePassword

# Allow local machine to trust targeted machines (to execute bootstrap script)
Set-Item WSMan:\localhost\Client\TrustedHosts -Value $target -Force
Set-Item WSMan:\localhost\Shell\MaxMemoryPerShellMB -Value 0 -Force
Set-ExecutionPolicy -Force bypass

Write-Host "Connecting to management service of $target"
Connect-WSMan -UseSSL -Port 5986 -Credential $cred -ComputerName $target -SessionOption (New-WSManSessionOption -SkipCACheck -SkipCNCheck)