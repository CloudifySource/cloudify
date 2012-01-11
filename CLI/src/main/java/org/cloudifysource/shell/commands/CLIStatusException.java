package org.cloudifysource.shell.commands;

public class CLIStatusException extends CLIException {
	
	private static final long serialVersionUID = -399277091070772297L;
	private String reasonCode;
    private Object[] args;


    public CLIStatusException(Throwable cause, String reasonCode, Object... args) {
        super("reasonCode: " + reasonCode, cause);
        this.args = args;
        this.reasonCode = reasonCode; 
    }

    public CLIStatusException(String reasonCode, Object... args) {
        super("reasonCode: " + reasonCode);
        this.reasonCode = reasonCode;
        this.args = args;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public Object[] getArgs() {
        return args;
    }
}
