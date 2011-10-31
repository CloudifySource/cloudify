package com.gigaspaces.cloudify.shell;

import jline.Terminal;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.karaf.shell.console.jline.Console;

import com.gigaspaces.cloudify.shell.rest.RestAdminFacade;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Properties;

/**
 * @author uri
 */
public class ConsoleWithProps extends Console {

    private static final String DEFAULT_APP_NAME = "default";
    private String currentAppName = DEFAULT_APP_NAME;
    private final ConsoleWithPropsActions consoleActions;
    
    ConsoleWithProps(CommandProcessor commandProcessor, InputStream input,
                     PrintStream output, PrintStream err, Terminal terminal,
                     CloseCallback callback, boolean isInteractive) throws Exception {
        super(commandProcessor, input, output, err, terminal,
                callback);
        
        consoleActions = isInteractive ? new ConsoleWithPropsInteractive() :
            new ConsoleWithPropsNonInteractive();
        
        callback.setSession(session);
        //TODO choose default admin or make it configurable
        AdminFacade adminFacade = new RestAdminFacade();
        session.put(Constants.ADMIN_FACADE, adminFacade);
        session.put(Constants.RECIPES, new HashMap<String, File>());
        session.put(Constants.ACTIVE_APP, DEFAULT_APP_NAME);
        session.put(Constants.INTERACTIVE_MODE, isInteractive);
    }


    @Override
    protected String getPrompt() {
        return consoleActions.getPromptInternal(currentAppName);
    }

    public void setCurrentApplicationName(String currentAppName) {
        this.currentAppName = currentAppName;
    }

    public String getCurrentApplicationName() {
        return currentAppName;
    }

    @Override
    protected void setSessionProperties() {
    }

    @Override
    protected Properties loadBrandingProperties() {
        Properties props = new Properties();
        loadProps(props, consoleActions.getBrandingPropertiesResourcePath());
        return props;
    }
}