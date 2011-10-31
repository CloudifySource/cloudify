package com.gigaspaces.cloudify.shell.commands;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.service.command.CommandSession;

@Command(scope = "cloudify", name = "clear", description = "clears the console.")
public class ClearScreen implements Action {

	@Override
	public Object execute(CommandSession arg0) throws Exception {
		System.out.print("\33[2J");
	    System.out.flush();
	    System.out.print("\33[1;1H");
	    System.out.flush();
	    return null;
	}
}
