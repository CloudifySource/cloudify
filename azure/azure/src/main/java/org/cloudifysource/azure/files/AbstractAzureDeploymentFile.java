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
package org.cloudifysource.azure.files;

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
