// CHECKSTYLE:OFF
package org.cloudifysource.dsl.internal;

import java.io.File;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * The class <code>GroovyFileValidaterTest</code> contains tests for the class <code>{@link GroovyFileValidater}</code>.
 * 
 * @author barakme
 * @version $Revision: 1.0 $
 */
public class GroovyFileValidaterTest {

	private GroovyFileValidater validator;

	@Test
	public void testEmptyFile()
			throws Exception {

		GroovyFileCompilationResult result =
				validator.validateFile(new File("src/test/resources/groovyFileValidation/empty.groovy"));
		Assert.assertEquals(Boolean.TRUE, result.isSuccess());
	}

	@Test
	public void testSimpleFile()
			throws Exception {

		GroovyFileCompilationResult result =
				validator.validateFile(new File("src/test/resources/groovyFileValidation/simple.groovy"));
		Assert.assertEquals(Boolean.TRUE, result.isSuccess());

	}

	@Test
	public void testBadImportFile()
			throws Exception {

		GroovyFileCompilationResult result =
				validator.validateFile(new File("src/test/resources/groovyFileValidation/badImport.groovy"));
		Assert.assertEquals(Boolean.FALSE, result.isSuccess());

	}

	/**
	 * Perform pre-test initialization.
	 * 
	 * @throws Exception if the initialization fails for some reason
	 */
	@Before
	public void setUp()
			throws Exception {
		this.validator = new GroovyFileValidater();
	}

	/**
	 * Perform post-test clean-up.
	 * 
	 * @throws Exception if the clean-up fails for some reason
	 */
	@After
	public void tearDown()
			throws Exception {
		// Add additional tear down code here
	}
}