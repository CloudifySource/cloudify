/*******************************************************************************
 * Copyright 2011 GigaSpaces Technologies Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.openspaces.shell;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.SequenceInputStream;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import jline.Terminal;
import jline.console.ConsoleReader;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.runtime.CommandProcessorImpl;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.console.Main;
import org.apache.karaf.shell.console.jline.Console;
import org.fusesource.jansi.Ansi;
import org.openspaces.shell.logging.ShellErrorManager;
import org.openspaces.shell.logging.ShellFormatter;

//declared as command so that it can be used in the context of another shell
@Command(name = "cloudify", scope = "cloudify", description = "Executes a cloudify command interpreter")
public class GigaShellMain extends Main implements Action {

	private static final String EXIT_COMMAND = "exit\n";
	private static GigaShellMain instance;
	private ConsoleWithProps console;
	private final boolean isInteractive;
	
	@Argument(name = "args", description = "Cloudify sub command arguments", multiValued = true)
	String[] args;

	public static void main(String[] args) throws Exception {
		initializeLogConfiguration();

		InputStream is = null;
		SequenceInputStream sis = null;
		InputStream exitInputStream = new ByteArrayInputStream(EXIT_COMMAND.getBytes());
		boolean isInteractive = true;
		
		try{
			if (args.length > 0){

			    isInteractive = false;
			    
				if (args[0].startsWith("-f=")){
					String filename = args[0].substring("-f=".length());
					File file = new File(filename);
					if (!file.exists()){
						throw new IllegalArgumentException(filename + " does not exist");
					}

					is = new FileInputStream(filename);
				}else{
					String commandString = "";
					for (int i = 0; i < args.length; i++){
						commandString = commandString.concat(args[i] + " ");
					}
					if (!commandString.endsWith(";")){
						commandString = commandString.concat(";");
					}
					commandString = commandString.replace(";", System.getProperty("line.separator"));

					is = new ByteArrayInputStream(commandString.getBytes("UTF-8"));
				}

				sis = new SequenceInputStream(is, exitInputStream);
				System.setIn(sis);
				args = new String[0];

			}

			instance = new GigaShellMain(isInteractive);
			instance.setApplication("cloudify");
			Ansi.ansi();
			instance.run(args);
		}finally{
			if (is != null){
				is.close();
			}
			if (sis != null){
				sis.close();
			}
			exitInputStream.close();
		}
	}

	private GigaShellMain(boolean isInteractive) {
	    this.isInteractive = isInteractive;
	}
	
	private static void initializeLogConfiguration() throws SecurityException, IOException{

		//Replace the console Handler's formatter.
		Handler[] handlers = Logger.getLogger("").getHandlers();
		for (Handler handler : handlers) {
			if (handler.getClass().getName() == ConsoleHandler.class.getName()){
				handler.setFormatter(new ShellFormatter());
				handler.setErrorManager(new ShellErrorManager());
				handler.setLevel(Level.INFO);
				break;
			}
		}
	}

	public static GigaShellMain getInstance() {
		return instance;
	}

	@Override
	public Object execute(CommandSession session) throws Exception {
		run(session, args);
		return null;
	}

	@Override
	protected Console createConsole(CommandProcessorImpl commandProcessor, InputStream input,
			PrintStream output, PrintStream err, Terminal terminal) throws Exception {
		//Disable PC speaker beep
		System.setProperty(ConsoleReader.JLINE_NOBELL, Boolean.toString(true));
		CloseCallback callback = new CloseCallback();
		console = new ConsoleWithProps(commandProcessor, input, output, err, terminal, callback, isInteractive);
		return console;
	}

	public void setCurrentApplicationName(String applicationName) {
		console.setCurrentApplicationName(applicationName);
	}

	@Override
	public String getDiscoveryResource() {
		return "META-INF/shell/commands";
	}

	@Override
	public boolean isMultiScopeMode() {
		return false;
	}
}



