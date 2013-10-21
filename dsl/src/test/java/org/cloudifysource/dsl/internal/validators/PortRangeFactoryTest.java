package org.cloudifysource.dsl.internal.validators;

import junit.framework.Assert;

import org.cloudifysource.domain.network.PortRange;
import org.cloudifysource.domain.network.PortRangeFactory;
import org.junit.Test;

public class PortRangeFactoryTest {

	@Test
	public void singlePortTest() {
		final PortRange range = PortRangeFactory.createPortRange("80");
		Assert.assertNotNull(range);
		Assert.assertEquals(1, range.getRanges().size());
		Assert.assertEquals((Integer) 80, range.getRanges().get(0).getFrom());
		Assert.assertEquals(null, range.getRanges().get(0).getTo());

	}

	@Test
	public void twoPortsTest() {
		final PortRange range = PortRangeFactory.createPortRange("80,81");
		Assert.assertNotNull(range);
		Assert.assertEquals(2, range.getRanges().size());
		Assert.assertEquals((Integer) 80, range.getRanges().get(0).getFrom());
		Assert.assertEquals(null, range.getRanges().get(0).getTo());
		Assert.assertEquals((Integer) 81, range.getRanges().get(1).getFrom());
	}

	@Test
	public void oneRangeTest() {
		final PortRange range = PortRangeFactory.createPortRange("80-81");
		Assert.assertNotNull(range);
		Assert.assertEquals(1, range.getRanges().size());
		Assert.assertEquals((Integer) 80, range.getRanges().get(0).getFrom());
		Assert.assertEquals((Integer) 81, range.getRanges().get(0).getTo());
	}

	@Test
	public void twoRangeTest() {
		final PortRange range = PortRangeFactory.createPortRange("80-81,82-83");
		Assert.assertNotNull(range);
		Assert.assertEquals(2, range.getRanges().size());
		Assert.assertEquals((Integer) 80, range.getRanges().get(0).getFrom());
		Assert.assertEquals((Integer) 81, range.getRanges().get(0).getTo());
		Assert.assertEquals((Integer) 82, range.getRanges().get(1).getFrom());
		Assert.assertEquals((Integer) 83, range.getRanges().get(1).getTo());

	}

	@Test
	public void complexTest() {
		final PortRange range = PortRangeFactory.createPortRange("80,81,80-81,82,83,82-83");
		Assert.assertNotNull(range);
		Assert.assertEquals(6, range.getRanges().size());
		Assert.assertEquals((Integer) 80, range.getRanges().get(0).getFrom());
		Assert.assertEquals((Integer) 81, range.getRanges().get(1).getFrom());
		Assert.assertEquals((Integer) 80, range.getRanges().get(2).getFrom());
		Assert.assertEquals((Integer) 81, range.getRanges().get(2).getTo());
		Assert.assertEquals((Integer) 82, range.getRanges().get(3).getFrom());
		Assert.assertEquals((Integer) 83, range.getRanges().get(4).getFrom());
		Assert.assertEquals((Integer) 82, range.getRanges().get(5).getFrom());
		Assert.assertEquals((Integer) 83, range.getRanges().get(5).getTo());

	}

	@Test(expected = IllegalArgumentException.class)
	public void badPortTest() {
		PortRangeFactory.createPortRange("80aaa");

	}

	@Test(expected = IllegalArgumentException.class)
	public void badPortRangeTest() {
		PortRangeFactory.createPortRange("80-aaa");

	}

	@Test(expected = IllegalArgumentException.class)
	public void badSyntaxTest() {
		PortRangeFactory.createPortRange("80-aaa-bbb");

	}

}
