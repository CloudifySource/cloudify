/*******************************************************************************
 * Copyright 2011 GigaSpaces Technologies Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.cloudifysource.shell;

import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import org.apache.http.HttpException;
import org.cloudifysource.shell.commands.CLIException;
import org.cloudifysource.shell.commands.CLIStatusException;


/**
 * @author rafi
 * @since 8.0.3
 */
public abstract class AbstractAdminFacade implements AdminFacade {

    private boolean connected = false;
    protected ResourceBundle messages = ShellUtils.getMessageBundle();
    protected static final Logger logger = Logger.getLogger(AbstractAdminFacade.class.getName());

    /**
     * @param applicationName
     * @param packedFile
     * @return
     * @throws CLIException 
     */
    public String install(String applicationName, File packedFile) throws CLIException {
        // TODO impl this properly when feature is available through admin api
        // meaning, only "install" and dont start
        return doDeploy(applicationName, packedFile);
    }

    /**
     * IMPORTANT: this method should set the field connected
     *
     * @param user
     * @param password
     * @return
     * @throws CLIException 
     * @throws IOException
     * @throws HttpException
     */
    public void connect(String user, String password, String url) throws CLIException {
        if (!isConnected()) {
            doConnect(user, password, url);
            this.connected = true;
        } else {
            throw new CLIStatusException("already_connected");
        }
    }

    protected abstract void doConnect(String user, String password, String url) throws CLIException;

    /**
     * IMPORTANT: this method should set the field connected
     *
     * @return
     */
    public void disconnect() throws CLIException {
   		connected = false;
   		doDisconnect();
    }

    public abstract void doDisconnect() throws CLIException;


    public boolean isConnected() throws CLIException {
        return connected;
    }


    protected abstract String doDeploy(String applicationName, File packedFile) throws CLIException;
}
