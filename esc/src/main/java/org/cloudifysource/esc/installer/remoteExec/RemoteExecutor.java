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

package org.cloudifysource.esc.installer.remoteExec;

import java.util.concurrent.TimeoutException;

import org.cloudifysource.esc.installer.AgentlessInstaller;
import org.cloudifysource.esc.installer.InstallationDetails;
import org.cloudifysource.esc.installer.InstallerException;

public interface RemoteExecutor {

	public void execute(String targetHost, final InstallationDetails details, final String command, final long endTimeMillis)
			throws InstallerException, TimeoutException, InterruptedException;

	public boolean isRunInBackground();

	public RemoteExecutor runInBackground();

	public RemoteExecutor chmodExecutable(final String path);

	public RemoteExecutor exportVar(final String name, final String value);

	public RemoteExecutor separate();

	public RemoteExecutor call(final String str);

	public void initialize(final AgentlessInstaller installer, final InstallationDetails details);

}
