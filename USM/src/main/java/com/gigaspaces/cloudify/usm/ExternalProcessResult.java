package com.gigaspaces.cloudify.usm;

public class ExternalProcessResult {

	private int exitValue;
	public ExternalProcessResult(int exitValue, String output) {
		super();
		this.exitValue = exitValue;
		this.output = output;
	}
	private String output;
	public int getExitValue() {
		return exitValue;
	}
	public void setExitValue(int exitValue) {
		this.exitValue = exitValue;
	}
	public String getOutput() {
		return output;
	}
	public void setOutput(String output) {
		this.output = output;
	}
	
}
