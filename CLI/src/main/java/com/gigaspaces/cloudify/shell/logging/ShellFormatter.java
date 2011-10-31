package com.gigaspaces.cloudify.shell.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import com.gigaspaces.cloudify.shell.ShellUtils;

public class ShellFormatter extends SimpleFormatter {

	private String newLineChar = System.getProperty("line.separator");
	protected ResourceBundle messages = ShellUtils.getMessageBundle();

	public ShellFormatter() {
		super();
	}

	@Override
	public String format(LogRecord record) {
		//TODO: append exception message to output
		String outputMessage;
		//This means that an exception was thrown. print the ex message to the log.
		if (record.getThrown() != null){
			Throwable t = record.getThrown();
			
			String message;
			if (t.getStackTrace().length == 0) {
				message = t.getLocalizedMessage();
			}
			else {
				message = t.toString();
				StringWriter sw = new StringWriter();
				t.printStackTrace(new PrintWriter(sw));
				message = sw.toString();
			}
			outputMessage = MessageFormat.format(messages.getString("op_failed"), message);
			
			if (record.getMessage() != null && record.getMessage().length() > 0) {
			    outputMessage = super.formatMessage(record) + ": " + outputMessage;
			}
		}else{
			outputMessage = super.formatMessage(record);
		}
		
		//don't use message formatter here since outputMessage may contain illegal "{}" string.
		return outputMessage + newLineChar;
	}

}
