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
package org.cloudifysource.shell.commands;

public class CLIStatusException extends CLIException {
	
	private static final long serialVersionUID = -399277091070772297L;
	private String reasonCode;
    private Object[] args;


    public CLIStatusException(Throwable cause, String reasonCode, Object... args) {
        super("reasonCode: " + reasonCode, cause);
        this.args = args;
        this.reasonCode = reasonCode; 
    }

    public CLIStatusException(String reasonCode, Object... args) {
        super("reasonCode: " + reasonCode);
        this.reasonCode = reasonCode;
        this.args = args;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public Object[] getArgs() {
        return args;
    }
}
