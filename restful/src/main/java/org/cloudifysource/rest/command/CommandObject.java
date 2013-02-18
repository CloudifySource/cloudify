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
package org.cloudifysource.rest.command;

import java.util.List;
import java.util.Map;

import org.cloudifysource.rest.out.OutputUtils;
import org.cloudifysource.rest.util.NotFoundHttpException;


/**
 * CommandObject is initializes with a command name.
 * The CommandObject uses the previous command's output
 * and it's current command's name in order get the correct 
 * getter method signature and invoke it on the previous command's output object.   
 * 
 * @author adaml
 *
 */
public class CommandObject {

	private String commandName;
	private Object commandObjectOutput;
	private CommandObject previousCommand;

	/**
	 * Constructor - Create a new command using command name.
	 * @param command name
	 */
	public CommandObject(String command) {
		this.commandName = command;
	}
	
	/**
	 * Constructor - construct the root command with and output object.
	 * @param rootObject
	 */
	public CommandObject(Object rootObject){
		this.commandObjectOutput = rootObject;
	}
	
	/**
	 * run the command according to the type of object returned in the previous command's run.
	 * The command's purpose will be determined by the previous command's return Type.
	 * 
	 * for example: if the previous command returned an object of type Array, it's next command,
	 * if exists, will have to be an array index and will be treated as such.
	 */
	public void runCommand() {
		
		Object previousCommandObject = previousCommand.getCommandObject(); 
		if (OutputUtils.isNull(previousCommandObject)) {
			throw new NotFoundHttpException("Method invocation returned null value: " + previousCommand.commandName);
		}
			
		Class<?> previousCommandObjectClass = previousCommandObject.getClass();
	
		if (previousCommandObjectClass.isArray()){
			this.commandObjectOutput = CommandUtils.getArrayClassObject(commandName, previousCommandObject);
		}
		else if (List.class.isAssignableFrom(previousCommandObjectClass)){
			this.commandObjectOutput = CommandUtils.getListClassObject(commandName, previousCommandObject);
		}
		else if (Map.class.isAssignableFrom(previousCommandObjectClass)){
			this.commandObjectOutput = CommandUtils.getMapObject(commandName, previousCommandObject);
		}
		else{
			this.commandObjectOutput = CommandUtils.getObjectByCommand(commandName, previousCommandObject);
		}

	}
	
	/**
	 * Set a reference to the Previous command. 
	 * @param previousCommand
	 */
	public void setPreviousCommandObject(CommandObject previousCommand){
		this.previousCommand = previousCommand;
	}
	
	public Object getPreviousCommandObject(){
		return this.previousCommand.getCommandObject();
	}
	/**
	 * get the command's output object.
	 * @return command's return object.
	 */
	public Object getCommandObject(){
		return commandObjectOutput;
	}
	
	/**
	 * get the command name.
	 * @return command name.
	 */
	public String getCommandName(){
		return this.commandName;
	}

}
