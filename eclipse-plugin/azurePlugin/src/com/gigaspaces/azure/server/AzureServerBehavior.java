package com.gigaspaces.azure.server;

import java.io.IOException;
import java.util.Arrays;

import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

public class AzureServerBehavior extends ServerBehaviourDelegate {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		System.out.println("AzureServerBehavior " + Arrays.toString(args));

		if (args.length > 0) {
			if (args[0].equals("start")) {
				start();
			}
		}

	}

	private static void start() throws IOException {
		Runtime.getRuntime().exec("emulatorTools/RunInEmulator.cmd");
	}

	@Override
	public void stop(boolean force) {
		// TODO Auto-generated method stub
		
	}

}
