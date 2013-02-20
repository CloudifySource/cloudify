package org.cloudifysource.shell.commands;

/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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

import com.j_spaces.kernel.PlatformVersion;
import org.apache.felix.gogo.commands.Command;
import org.cloudifysource.shell.ShellUtils;

/**
 * @author rafi, barakm
 * @since 2.0.0
 *        <p/>
 *        Displays the XAP and cloudify versions.
 *        <p/>
 *        Command syntax: version
 */
@Command(scope = "cloudify", name = "version", description = "Displays the Cloudify versions")
public class Version extends AbstractGSCommand {

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object doExecute() throws Exception {

        final String platformInfo = PlatformVersion.getOfficialVersion();
        session.getConsole().println(platformInfo);
        if (ShellUtils.promptUser(session, "version_check_confirmation")) {
            ShellUtils.registerVersionCheck();
            ShellUtils.doVersionCheck(session);
        }
        return null;
    }
}
