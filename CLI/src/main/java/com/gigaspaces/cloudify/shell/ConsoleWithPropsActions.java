package com.gigaspaces.cloudify.shell;

public interface ConsoleWithPropsActions {

    String getPromptInternal(String currentAppName);
    
    String getBrandingPropertiesResourcePath();
    
}
