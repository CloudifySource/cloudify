package org.openspaces.shell;

import java.text.SimpleDateFormat;
import java.util.Date;

import junit.framework.Assert;

import org.cloudifysource.shell.ShellUtils;
import org.junit.Test;

public class getExpectedExecutionTimeMsg {

	@Test
	public void test() {
		Assert.assertTrue(ShellUtils.getExpectedExecutionTimeMessage().contains(new SimpleDateFormat("HH:mm").format(new Date())));
	}

}
