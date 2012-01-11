package org.cloudifysource.esc.util;

import java.util.concurrent.TimeoutException;

public class Utils {

    public static long millisUntil(long end)
            throws TimeoutException {
        long millisUntilEnd = end - System.currentTimeMillis();
        if (millisUntilEnd < 0) {
            throw new TimeoutException("Cloud operation timed out");
        }
        return millisUntilEnd;
    }

}
