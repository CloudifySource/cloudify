package org.cloudifysource.rest.events;

import com.gigaspaces.log.LogEntry;
import junit.framework.Assert;
import org.cloudifysource.dsl.rest.response.DeploymentEvent;
import org.cloudifysource.dsl.rest.response.DeploymentEvents;
import org.junit.Test;
import org.mockito.Mockito;
import org.openspaces.admin.gsc.GridServiceContainer;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 6/7/13
 * Time: 7:51 PM
 */
public class EventsUtilsTest {

    public static final String MOCK_EVENT = "MOCK_EVENT";

    @Test
    public void testLogToEvent() throws Exception {

        LogEntry entryMock = Mockito.mock(LogEntry.class);
        Mockito.when(entryMock.getText()).thenReturn("USMLOGGER" + " - " + MOCK_EVENT);

        String hostName = "hostname";
        String hostAddress = "hostaddress";

        String expectedEventString = "[" + hostName + "/" + hostAddress + "] - " + MOCK_EVENT;
        Assert.assertEquals(expectedEventString, EventsUtils.logToEvent(entryMock, hostName, hostAddress).getDescription());

    }

    @Test
    public void testExtractDesiredEvents() throws Exception {

        DeploymentEvents events = createEvents(0, 10);

        // test subset
        Assert.assertEquals(createEvents(0, 4).getEvents(), EventsUtils.extractDesiredEvents(events, 0, 4).getEvents());

        // test entire set
        Assert.assertEquals(createEvents(0, 10).getEvents(), EventsUtils.extractDesiredEvents(events, 0, 10).getEvents());

        // test empty set
        Assert.assertEquals(createEvents(0, -1).getEvents(), EventsUtils.extractDesiredEvents(events, 11, 15).getEvents());

        // test over set
        Assert.assertEquals(createEvents(0, 10).getEvents(), EventsUtils.extractDesiredEvents(events, 0, 100).getEvents());

        // test one event
        Assert.assertEquals(createEvents(0, 0).getEvents(), EventsUtils.extractDesiredEvents(events, 0, 0).getEvents());
    }

    @Test
    public void testRetrieveEventWithIndex() throws Exception {

        DeploymentEvents events = createEvents(0, 10);

        // test existing index
        Assert.assertEquals(createEvents(0, 0).getEvents().get(0), EventsUtils.retrieveEventWithIndex(0, events.getEvents()));

        // test non-existing index
        Assert.assertNull(EventsUtils.retrieveEventWithIndex(15, events.getEvents()));


    }

    @Test
    public void testEventsPresent() throws Exception {

        DeploymentEvents events = createEvents(0, 10);

        // test entire set
        Assert.assertTrue(EventsUtils.eventsPresent(events, 0, 10));

        // test subset set
        Assert.assertTrue(EventsUtils.eventsPresent(events, 5, 8));

        // test over set
        Assert.assertFalse(EventsUtils.eventsPresent(events, 0, 15));


    }

    private DeploymentEvents createEvents(final int from, final int to) {
        DeploymentEvents events = new DeploymentEvents();
        for (int i = from; i <= to ; i++) {
            DeploymentEvent event = createEvent(i);
            events.getEvents().add(event);
        }
        return events;
    }

    private DeploymentEvent createEvent(int i) {
        DeploymentEvent event = new DeploymentEvent();
        event.setIndex(i);
        event.setDescription("Event-" + i);
        return event;
    }

    private GridServiceContainer createMockContainer(final String containerUid) {
        GridServiceContainer mockContainer = Mockito.mock(GridServiceContainer.class);
        Mockito.when(mockContainer.getUid()).thenReturn(containerUid);
        return mockContainer;
    }
}
