package com.gigaspaces.cloudify.rest.command;

import java.util.List;
import java.util.Map;

import com.gigaspaces.cloudify.rest.util.NotFoundHttpException;

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
	public void runCommand(){
		
		Object previousCommandObject = previousCommand.getCommandObject(); 
		if (previousCommandObject == null) {
			throw new NotFoundHttpException();
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
