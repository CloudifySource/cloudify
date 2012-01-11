package org.cloudifysource.esc.util;

public class ShellCommandBuilder {

    public static final String COMMAND_SEPARATOR = ";";
    
    private StringBuilder sb = new StringBuilder();
    
    public ShellCommandBuilder call(String str) {
        sb.append(str);
        return this;
    }
    
    public ShellCommandBuilder separate() {
        sb.append(COMMAND_SEPARATOR);
        return this;
    }
    
    public ShellCommandBuilder exportVar(String name, String value) {
        if (value == null) {
            value = "";
        }
        sb.append("export " + name + "=" + value);
        separate();
        return this;
    }
 
    public ShellCommandBuilder chmodExecutable(String path) {
        sb.append("chmod +x " + path);
        separate();
        return this;
    }
    
    public ShellCommandBuilder runInBackground() {
        sb.append(" &");
        return this;
    }
    
    @Override
    public String toString() {
        return sb.toString();
    }
    
    
}
