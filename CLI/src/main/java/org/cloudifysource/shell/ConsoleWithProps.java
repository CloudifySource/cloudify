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

import com.j_spaces.kernel.PlatformVersion;
import jline.Terminal;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.karaf.shell.console.jline.Console;
import org.cloudifysource.shell.rest.RestAdminFacade;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author uri
 * @since 2.0.0
 * 
 *        Extends the karaf framework Console Object. This class adds some branding and functionality on top
 *        of the base class, e.g., adds a default adminFacade to the session, overrides the get prompt method,
 *        set a default application and more.
 */
public class ConsoleWithProps extends Console {

	private static final String DEFAULT_APP_NAME = "default";
    public static final int VERSION_CHECK_READ_TIMEOUT = 5000;
    private String currentAppName = DEFAULT_APP_NAME;
	private final ConsoleWithPropsActions consoleActions;

    private final Logger logger = Logger.getLogger(getClass().getName());
    private static final long TWO_WEEKS_IN_MILLIS = 86400000L * 14L;
    public static final File VERSION_CHECK_FILE = new File(System.getProperty("user.home") + "/.karaf/lastVersionCheckTimestamp");

    ConsoleWithProps(final CommandProcessor commandProcessor, final InputStream input, final PrintStream output,
			final PrintStream err, final Terminal terminal, final CloseCallback callback, final boolean isInteractive)
			throws Exception {
		super(commandProcessor, input, output, err, terminal, callback);

		consoleActions = isInteractive ? new ConsoleWithPropsInteractive() : new ConsoleWithPropsNonInteractive();

		callback.setSession(session);
		// TODO choose default admin or make it configurable
		final AdminFacade adminFacade = new RestAdminFacade();
		session.put(Constants.ADMIN_FACADE, adminFacade);
		session.put(Constants.RECIPES, new HashMap<String, File>());
		session.put(Constants.ACTIVE_APP, DEFAULT_APP_NAME);
		session.put(Constants.INTERACTIVE_MODE, isInteractive);
	}

    @Override
    protected void welcome() {
        Properties props = loadBrandingProperties();
        String welcome = props.getProperty("welcome");

        if (welcome != null && welcome.length() > 0) {
            session.getConsole().println(welcome);
        }
        doVersionCheckIfNeeded();

    }

    private void doVersionCheckIfNeeded() {
        long lastAskedTS = getLastTimeAskedAboutVersionCheck();
        //check only if checked over a month ago and user agrees
        try {
            if (lastAskedTS <= (System.currentTimeMillis() - TWO_WEEKS_IN_MILLIS) && ShellUtils.promptUser(session, "version_check_confirmation")) {
                session.getConsole().println("Checking version...");
                String currentBuildStr = PlatformVersion.getBuildNumber();
                if(currentBuildStr.contains("-")) {
                    currentBuildStr = currentBuildStr.substring(0, currentBuildStr.indexOf("-"));
                }
                int currentVersion = Integer.parseInt(currentBuildStr);
                int latestBuild = getLatestBuildNumber(currentVersion);
                String message;
                if (latestBuild == -1) {
                    message = ShellUtils.getFormattedMessage("could_not_get_version");
                } else if (latestBuild > currentVersion) {
                    message = ShellUtils.getFormattedMessage("newer_version_exists");
                } else {
                    message = ShellUtils.getFormattedMessage("version_up_to_date");
                }
                registerVersionCheck();
                session.getConsole().println(message);
                session.getConsole().println();
            }
        } catch (IOException e) {
            logger.log(Level.FINE, "Failed to prompt user", e);
        }
    }

    private int getLatestBuildNumber(int currentVersion) {
        try {
            HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
            requestFactory.setReadTimeout(VERSION_CHECK_READ_TIMEOUT);
            RestTemplate template = new RestTemplate(requestFactory);
            String versionStr = template.getForObject("http://www.gigaspaces.com/downloadgen/latest-cloudify-version?build="+currentVersion, String.class);
            logger.fine("Latest cloudify version is " + versionStr);
            return Integer.parseInt(versionStr);
        } catch (RestClientException e) {
            logger.log(Level.FINE, "Could not get version from server", e);
            return -1;
        } catch (NumberFormatException e) {
            logger.fine("Get version response is not a number");
            return -1;
        }

    }

    private long getLastTimeAskedAboutVersionCheck() {
        long lastVersionCheckTS = 0;
        if (VERSION_CHECK_FILE.exists()){
            DataInputStream dis = null;
            try {
                dis = new DataInputStream(new FileInputStream(VERSION_CHECK_FILE));
                lastVersionCheckTS = dis.readLong();
            } catch (IOException e) {
                logger.log(Level.FINE, "failed to read last checked version timestamp file", e);
            } finally {
                if (dis != null) {
                    try {
                        dis.close();
                    } catch (IOException e) {}
                }
            }
        }
        return lastVersionCheckTS;
    }

    private void registerVersionCheck() {
        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(new FileOutputStream(VERSION_CHECK_FILE));
            dos.writeLong(System.currentTimeMillis());
        } catch (IOException e) {
            logger.log(Level.FINE, "failed to write last checked version timestamp file", e);
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException e) {}
            }
        }
    }


    /**
	 * {@inheritDoc}
	 */
	@Override
	protected String getPrompt() {
		return consoleActions.getPromptInternal(currentAppName);
	}

	/**
	 * Sets the application name.
	 * 
	 * @param currentAppName
	 *            The application name to set
	 */
	public void setCurrentApplicationName(final String currentAppName) {
		this.currentAppName = currentAppName;
	}

	/**
	 * Gets the application name.
	 * 
	 * @return The current application name
	 */
	public String getCurrentApplicationName() {
		return currentAppName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void setSessionProperties() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Properties loadBrandingProperties() {
		final Properties props = new Properties();
		loadProps(props, consoleActions.getBrandingPropertiesResourcePath());
		return props;
	}
}