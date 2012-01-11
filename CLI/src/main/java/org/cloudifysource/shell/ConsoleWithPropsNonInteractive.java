package org.cloudifysource.shell;


public class ConsoleWithPropsNonInteractive implements ConsoleWithPropsActions {

    @Override
    public String getPromptInternal(String currentAppName) {
        return ">>> ";
    }

    @Override
    public String getBrandingPropertiesResourcePath() {
        return "META-INF/shell/noninteractive.branding.properties";
    }

    
}