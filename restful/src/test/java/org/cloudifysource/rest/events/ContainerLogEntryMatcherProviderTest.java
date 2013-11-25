package org.cloudifysource.rest.events;

import com.gigaspaces.log.LogEntryMatcher;
import junit.framework.Assert;
import org.cloudifysource.rest.events.cache.EventsCacheKey;
import org.junit.Test;
import org.mockito.Mockito;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.zone.config.ExactZonesConfig;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 6/8/13
 * Time: 2:28 PM
 */
public class ContainerLogEntryMatcherProviderTest {

    @Test
    public void testRemoveAll() throws Exception {

        ContainerLogEntryMatcherProvider provider = new ContainerLogEntryMatcherProvider();

        GridServiceContainer mockContainer1 = createMockContainer("containerId1");
        GridServiceContainer mockContainer2 = createMockContainer("containerId2");

        ContainerLogEntryMatcherProviderKey key1 = createKey(mockContainer1, "deploymentId1");
        ContainerLogEntryMatcherProviderKey key2 = createKey(mockContainer2, "deploymentId1");

        // create two entries for the same deployment
        LogEntryMatcher logEntryMatcher1BeforeRemoval = getLogEntryMatcher(provider, key1);
        LogEntryMatcher logEntryMatcher2BeforeRemoval = getLogEntryMatcher(provider, key2);

        // now remove all instances fot the deploymentId
        EventsCacheKey key = new EventsCacheKey("deploymentId1");
        provider.removeAll(key);

        // create two instances for the deployment again/
        LogEntryMatcher logEntryMatcher1AfterRemoval = getLogEntryMatcher(provider, key1);
        LogEntryMatcher logEntryMatcher2AfterRemoval = getLogEntryMatcher(provider, key2);

        // make sure before and after are different instances
        Assert.assertNotSame(logEntryMatcher1BeforeRemoval, logEntryMatcher1AfterRemoval);
        Assert.assertNotSame(logEntryMatcher2BeforeRemoval, logEntryMatcher2AfterRemoval);
    }

    @Test
    public void testGet() throws Exception {

        ContainerLogEntryMatcherProvider provider = new ContainerLogEntryMatcherProvider();

        GridServiceContainer mockContainer = createMockContainer("containerUid");

        ContainerLogEntryMatcherProviderKey key1 = createKey(mockContainer, "deploymentId1");
        ContainerLogEntryMatcherProviderKey key2 = createKey(mockContainer, "deploymentId2");

        // test get with existing entry returns the existing entry
        Assert.assertEquals(getLogEntryMatcher(provider, key1), getLogEntryMatcher(provider, key1));

        // test get for two different keys returns different instances
        Assert.assertNotSame(getLogEntryMatcher(provider, key1), getLogEntryMatcher(provider, key2));

    }

    private LogEntryMatcher getLogEntryMatcher(final ContainerLogEntryMatcherProvider provider,
                               final ContainerLogEntryMatcherProviderKey key) {
        return provider.get(key);
    }

    private ContainerLogEntryMatcherProviderKey createKey(final GridServiceContainer mockContainer,
                                                 final String deploymentId) {
        ContainerLogEntryMatcherProviderKey key = new ContainerLogEntryMatcherProviderKey();
        key.setContainer(mockContainer);
        key.setDeploymentId(deploymentId);
        return key;
    }

    private GridServiceContainer createMockContainer(final String containerUid) {
        GridServiceContainer mockContainer = Mockito.mock(GridServiceContainer.class);
        Mockito.when(mockContainer.getUid()).thenReturn(containerUid);
        Mockito.when(mockContainer.getExactZones()).thenReturn(new ExactZonesConfig());
        return mockContainer;
    }

}
