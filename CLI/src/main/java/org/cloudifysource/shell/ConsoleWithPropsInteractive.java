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


public class ConsoleWithPropsInteractive implements ConsoleWithPropsActions {

    public String getPromptInternal(String currentAppName) {
        return "\u001B[1mcloudify" + (currentAppName != null ? "@" + currentAppName : "") + "> \u001B[0m";
    }

    public String getBrandingPropertiesResourcePath() {
         return "META-INF/shell/branding.properties";
    }
    
}