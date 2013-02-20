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
package org.cloudifysource.shell;

import org.cloudifysource.shell.commands.CLIException;
import org.cloudifysource.shell.commands.CLIStatusException;

import java.io.File;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * @author rafi
 * @since 2.0.0
 *        <p/>
 *        This is an abstract implementation of the {@link AdminFacade} interface.
 */
public abstract class AbstractAdminFacade implements AdminFacade {

    private boolean connected = false;
    protected ResourceBundle messages = ShellUtils.getMessageBundle();
    protected static final Logger logger = Logger.getLogger(AbstractAdminFacade.class.getName());

    /**
     * {@inheritDoc}
     */
    @Override
    public String install(final String applicationName, final File packedFile) throws CLIException {
        // TODO impl this properly when feature is available through admin api
        // meaning, only "install" and dont start
        return doDeploy(applicationName, packedFile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void connect(final String user, final String password, final String url, final boolean sslUsed)
    		throws CLIException {
        if (!isConnected()) {
            doConnect(user, password, url, sslUsed);
            this.connected = true;
        } else {
            throw new CLIStatusException("already_connected");
        }
    }

    /**
     * Connects to the server, using the given credentials and URL.
     *
     * @param user     The user name, used to create the connection
     * @param password The user name, used to create the connection
     * @param url      The URL to connect to
     * @param isSecureConnection  Is this a secure connection (SSL)
     * @throws CLIException Reporting a failure to the connect to the server
     */
    protected abstract void doConnect(String user, String password, String url, boolean isSecureConnection)
    		throws CLIException;

    /**
     * {@inheritDoc}
     */
    @Override
    public void disconnect() throws CLIException {
        connected = false;
        doDisconnect();
    }

    /**
     * Disconnects from the server.
     *
     * @throws CLIException Reporting a failure to close the connection to the server
     */
    public abstract void doDisconnect() throws CLIException;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isConnected() throws CLIException {
        return connected;
    }

    /**
     * Installs and starts a service on a given application.
     *
     * @param applicationName The application the service will be deployed in
     * @param packedFile      The service file to deploy
     * @return Response from the server, or null if there was no response.
     * @throws CLIException Reporting a failure to install or start the given service on the specified application
     */
    protected abstract String doDeploy(final String applicationName, final File packedFile) throws CLIException;

}
