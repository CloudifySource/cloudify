/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.rest.validators;

/**
 * A POJO for holding dump-machine/s validator's parameters.
 * 
 * @author yael
 * @since 2.7.0
 */
public class DumpMachineValidationContext {
	private String[] processors;

	public String[] getProcessors() {
		return processors;
	}

	public void setProcessors(final String[] processors) {
		this.processors = processors;
	}
}
