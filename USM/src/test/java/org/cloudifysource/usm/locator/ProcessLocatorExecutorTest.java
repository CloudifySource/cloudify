package org.cloudifysource.usm.locator;

import static org.junit.Assert.assertNotNull;
import groovy.lang.Closure;

import java.io.File;

import org.cloudifysource.domain.entry.ExecutableDSLEntry;
import org.cloudifysource.dsl.entry.ExecutableDSLEntryFactory;
import org.cloudifysource.usm.launcher.DefaultProcessLauncher;
import org.cloudifysource.usm.launcher.ProcessLauncher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The class <code>ProcessLocatorExecutorTest</code> contains tests for the class
 * <code>{@link ProcessLocatorExecutor}</code>.
 * 
 * @generatedBy CodePro at 12/12/13 12:23 AM
 * @author barakme
 * @version $Revision: 1.0 $
 */
public class ProcessLocatorExecutorTest {
	/**
	 * Run the ProcessLocatorExecutor(ExecutableDSLEntry,ProcessLauncher,File) constructor test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 12/12/13 12:23 AM
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testProcessLocatorExecutor_1()
			throws Exception {
		final ExecutableDSLEntry locator = ExecutableDSLEntryFactory.createEntry(new Closure<Object>(null) {
			private static final long serialVersionUID = 1L;

			@Override
			public Object call(final Object... params) {
				return null;
			}
		}, "locator", new File("."));
		final ProcessLauncher launcher = new DefaultProcessLauncher();
		final File puExtDir = new File("");

		final ProcessLocatorExecutor result = new ProcessLocatorExecutor(locator, launcher, puExtDir);

		// add additional test code here
		assertNotNull(result);
		result.getProcessIDs();
	}

	/**
	 * Perform pre-test initialization.
	 * 
	 * @throws Exception
	 *             if the initialization fails for some reason
	 * 
	 * @generatedBy CodePro at 12/12/13 12:23 AM
	 */
	@Before
	public void setUp()
			throws Exception {
		// add additional set up code here
	}

	/**
	 * Perform post-test clean-up.
	 * 
	 * @throws Exception
	 *             if the clean-up fails for some reason
	 * 
	 * @generatedBy CodePro at 12/12/13 12:23 AM
	 */
	@After
	public void tearDown()
			throws Exception {
		// Add additional tear down code here
	}

	/**
	 * Launch the test.
	 * 
	 * @param args
	 *            the command line arguments
	 * 
	 * @generatedBy CodePro at 12/12/13 12:23 AM
	 */
	public static void main(final String[] args) {
		new org.junit.runner.JUnitCore().run(ProcessLocatorExecutorTest.class);
	}
}