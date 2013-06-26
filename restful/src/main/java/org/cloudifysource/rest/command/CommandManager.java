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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.cloudifysource.dsl.utils.IPUtils;
import org.springframework.web.servlet.HandlerMapping;
/**
 * CommandManager creates and runs commands, each command
 * depending on the previous command's output. 
 * 
 * The Command manager holds a list of commands where each
 * command holds a reference to the previous command.
 * 
 * The manager will output the last command's output object, as-well as
 * necessary information concerning the command's execution parameters.
 * 
 * @author adaml
 *
 */
public class CommandManager {
	
	private List<CommandObject> listOfCommands;
	private String commandURL;
	
	/**
	 * Constructor takes as input the entire commands URI, held in the request
	 * and the root object from which to begin invocation.
	 * @param request - the commands request 
	 * @param root - the root command's object
	 */
	public CommandManager(final HttpServletRequest request, final Object root) {
	    final String prefix = "/admin/";
        String executionPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        if (executionPath.endsWith("/")) {
        	executionPath = executionPath.substring(0, executionPath.length() - 1);
        }
        if (!executionPath.startsWith(prefix)) {
            throw new IllegalArgumentException("Bad request URL " + request.getRequestURL());
        }
        
        try {
        	String protocol = IPUtils.getRestProtocol();
            //String restUrl = "http://" + request.getLocalAddr() + ":" + request.getLocalPort() 
        	//	+ request.getContextPath();
        	URL restUrl = new URL(protocol, request.getLocalAddr(), request.getLocalPort(), request.getContextPath());
        	 this.commandURL = restUrl + executionPath;
        } catch (MalformedURLException e) {
        	// this is a bug since we formed the URL correctly
        	throw new IllegalStateException(e);
        }
       
        initilizeCommandList(executionPath.substring(prefix.length()), root);
	}
	
	/**
	 * run the initialized commands one by one, with each command 
	 * depending on the previous command's output.
	 */
	public void runCommands() {
	    for (CommandObject command : listOfCommands) {
            command.runCommand();
	    }
	}
	
	/**
	 * get the last command.
	 * @return the final object for parsing.
	 */
	public CommandObject getFinalCommand(){
		return this.listOfCommands.get(listOfCommands.size() - 1);
	}
	
	/**
	 * get the full execution URL for the command. 
	 * @return command URL.
	 */
	public String getCommandURL(){
		return this.commandURL;
	}
	
	/**
	 * get the last command's name.
	 * @return the name of the last command in the list.
	 */
	public String getFinalCommandName(){
		return this.listOfCommands.get(listOfCommands.size() - 1).getCommandName();
	}
	
	//Initialize the list of commands. each command will hold a reference to its previous command.
	private void initilizeCommandList(String commands, Object root) {
		this.listOfCommands = new ArrayList<CommandObject>();
		String[] delimitedCommands = commands.split("/");
		
		CommandObject commandObject;
		CommandObject previousCommand = new CommandObject(root);
		
		for(String rawCommandString : delimitedCommands){
			commandObject = new CommandObject(rawCommandString);
			commandObject.setPreviousCommandObject(previousCommand);
			listOfCommands.add(commandObject);
			previousCommand = commandObject;
		}
	}
}
