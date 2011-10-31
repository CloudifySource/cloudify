package com.gigaspaces.cloudify.shell;


public class ConsoleWithPropsInteractive implements ConsoleWithPropsActions {

    public String getPromptInternal(String currentAppName) {
        return "\u001B[1mcloudify" + (currentAppName != null ? "@" + currentAppName : "") + "> \u001B[0m";
    }

    public String getBrandingPropertiesResourcePath() {
         return "META-INF/shell/branding.properties";
    }
    
}