package org.openspaces.cloud.azure.files;

public class XMLXPathEditorException extends Exception {
    private static final long serialVersionUID = 1L;
    public XMLXPathEditorException(Exception e) {
        super(e);
    }
    
    public XMLXPathEditorException(String message, Exception e) {
        super(message, e);
    }
}
