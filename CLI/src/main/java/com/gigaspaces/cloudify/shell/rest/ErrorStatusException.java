package com.gigaspaces.cloudify.shell.rest;

import com.gigaspaces.cloudify.shell.commands.CLIException;

/**
 * @author uri
 */
public class ErrorStatusException extends CLIException {

    private String reasonCode;
    private Object[] args;


    public ErrorStatusException(String reasonCode, Throwable cause, Object... args) {
        super("reasonCode: " + reasonCode, cause);
        this.args = args;
        this.reasonCode = reasonCode; 
    }

    public ErrorStatusException(String reasonCode, Object... args) {
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
