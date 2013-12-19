package org.cloudifysource.esc.driver.provisioning.openstack;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Network;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Port;
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

	@Test
	public void testPortsResponse() throws Exception {
		File file = new File("./src/test/resources/openstack/mappings/ports-response.json");
		String jsonString = FileUtils.readFileToString(file);
		List<Port> ports = JsonUtils.unwrapRootToList(Port.class, jsonString);

		Assert.assertNotNull(ports);
		Assert.assertFalse(ports.isEmpty());
		for (Port port : ports) {
			Assert.assertNotNull(port.getFixedIps());
			Assert.assertFalse(port.getFixedIps().isEmpty());
			Assert.assertNotNull(port.getFixedIps().get(0).getIpAddress());
		}

	}
}
