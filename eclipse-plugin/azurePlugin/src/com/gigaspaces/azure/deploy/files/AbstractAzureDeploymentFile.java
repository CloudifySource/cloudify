package com.gigaspaces.azure.deploy.files;

import java.io.File;

public abstract class AbstractAzureDeploymentFile {

    private final XMLXPathEditor editor;
    private final File cscfgFile;
    
    public AbstractAzureDeploymentFile(File cscfgFile) throws XMLXPathEditorException {
        editor = new XMLXPathEditor(cscfgFile);
        this.cscfgFile = cscfgFile;
    }
    
    public void flush() throws XMLXPathEditorException {
        writeTo(this.cscfgFile);
    }
    
    public void writeTo(File outputFile) throws XMLXPathEditorException {
        editor.writeXmlFile(outputFile);
    }
    
    protected XMLXPathEditor getEditor() {
        return editor;
    }
 
    public File getFile() {
        return cscfgFile;
    }
    
}
