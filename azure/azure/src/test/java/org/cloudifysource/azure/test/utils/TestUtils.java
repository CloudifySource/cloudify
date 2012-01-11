package org.cloudifysource.azure.test.utils;

import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

public class TestUtils {

    public static void repetativeAssertTrue(String message, RepetativeConditionProvider condition, 
            long pollingInterval, long timeout, TimeUnit timeUnit) throws InterruptedException {
        long timeoutMillis = timeUnit.toMillis(timeout);
        long pollingIntervalMillis = timeUnit.toMillis(pollingInterval);
        long end = System.currentTimeMillis() + timeoutMillis;
        
        boolean isConditionMet = condition.getCondition();
        
        while(!isConditionMet && System.currentTimeMillis() < end) {
            isConditionMet = condition.getCondition();
            Thread.sleep(pollingIntervalMillis);
        }
        Assert.assertTrue(message, isConditionMet);
    }
    
}
