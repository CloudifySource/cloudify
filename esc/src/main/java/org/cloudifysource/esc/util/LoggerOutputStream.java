package org.cloudifysource.esc.util;

import java.io.OutputStream;
import java.util.logging.Logger;

public class LoggerOutputStream extends OutputStream {
    
    private String prefix = "";
    private final Logger logger;

    private StringBuilder sb;

    public LoggerOutputStream(Logger logger) {
        this.logger = logger;
        sb = new StringBuilder();
    }

    @Override
    public void write(int b) {
        char c = (char) b;
        if (c == '\n') {
            String s = sb.toString();
            logger.info(prefix + s);
            sb.setLength(0);
        } else
            sb.append(c);
    }

    @Override
    public void close()  {
        if (sb.length() > 0) {
            write('\n');
        }
    }

    @Override
    public void flush() {

    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

}