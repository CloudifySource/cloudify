/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.esc.installer;

/**********************************
 * The agentless installer is responsible for installing cloudify on a remote machine using a remote login mechanism
 * (i.e. ssh) and starting the cloudify agent. The main class is AgentlessInstaller.
 *
 *
 * Agentless Installation: The agentless installation process is a two-phase process: 1. File copy: files are copied
 * from a local folder (directory on the machine running the AgentlessInstaller) to a remote directory on the remote
 * host. The following file transfer protocols are supported: - SCP: Used with *nix systems. Uses port 22 by default.
 * Files are encrypted over the wire. Supported authentication mechanisms are username/password and PEM certficate files
 * (files can't be secured with a password) - CIFS: Used with windows systems. Files are copied in the clear. Defaults
 * to port 445. Supports username/password authentication.
 *
 * The local directory is expected to hold at-least one file - the script that will be executed in the second phase.
 * Applications may add additional files to this directory, as well as sub-directories, which will be copied
 * recursively. These files will be copied to the new machine and can be made available to code running on them.
 *
 * Note that the installation process often runs on remote machines and over limited bandwidth, so it is not recommended
 * to transfer large files using this mechanism.
 *
 * 2. Remote execution: A bootstrap script is executed on the remote machine. This script is responsible for setting up
 * Java, Cloudify, running the cloudify agent and starting up any management services, if required. The following remote
 * execution protocols are supported:
 *
 * - SSH: Used with *nix systems. Uses port 22 by default. Commands are encrypted over the wire. Supported
 * authentication mechanisms are username/password and PEM certficate files (files can't be secured with a password).
 *
 * - WinRM: Used with Windows systems. Uses port 5985 by default. Commands are encrypted over the wire (configured on
 * the server). See more details below.
 *
 * When the second phase is finished, the remote node should be connected to the cloudify cluster.
 *
 * Windows Support: ---------------- Cloudify uses MS Powershell to remote execute commands on remote windows machines.
 * This means that running the installation process for a target windows machine requires that the client running the
 * installation process also run windows. This means that a windows machine can start a linux machine, but only a
 * windows machine can start another windows machine.
 *
 * Bootstrapping nodes running Windows requires some additional setup. First, the client machine (which will run the
 * bootstrap-cloud command) must be configured correctly for using winrm. Open a powershell window 'as administrator'
 * and execute the following two commands:
 *
 * set-item WSMan:\localhost\Client\TrustedHosts -Value * -Force set-item WSMan:\localhost\Shell\MaxMemoryPerShellMB
 * -Value 0 -Force
 *
 * If running on a 64-bit machine, it is a good idea to open an x86 powershell window and execute the above commands as
 * well.
 *
 * You only need to do this once.
 *
 * Important: Setting the TrustedHosts setting to '*' has some security implications. You may want to consult with your
 * system administrator about this setting.
 *
 *
 *
 *
 *
 *
 *
 */

