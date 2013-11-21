package org.cloudifysource.esc.driver.provisioning.openstack;

import java.util.List;

import org.cloudifysource.esc.driver.provisioning.openstack.rest.Network;
import org.junit.Assert;
import org.junit.Test;

public class JsonUtilsTest {

	@Test
	public void testUnwrapRootToList() throws Exception {
		List<Network> list = JsonUtils.unwrapRootToList(Network.class, "{\"networks\":[{\"name\":\"test\"}]}");
		Assert.assertNotNull(list);
		Assert.assertEquals(1, list.size());
		Assert.assertEquals("test", list.get(0).getName());

		list = JsonUtils.unwrapRootToList(Network.class, "{\"networks\": [0]}");
		Assert.assertNull(list);
	}

	@Test
	public void testUnwrapRootToObject() throws Exception {
		Network network = JsonUtils.unwrapRootToObject(Network.class, "{\"network\":{\"name\":\"test\"}}");
		Assert.assertNotNull(network);
		Assert.assertEquals("test", network.getName());
	}
}
