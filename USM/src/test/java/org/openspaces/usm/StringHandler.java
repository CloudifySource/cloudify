package org.openspaces.usm;

import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

public class StringHandler extends StreamHandler {

	private StringBuilder sb = new StringBuilder();
	
	@Override
	public void publish(LogRecord record) {
		sb.append(record.getMessage() + System.getProperty("line.separator"));
		flush();
	}
	
	public String getLoggedMessages(){
		return sb.toString();
	}
}
