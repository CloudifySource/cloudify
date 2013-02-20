package org.cloudifysource.dsl.packaging;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.junit.Test;


public class BasicPackagingTest {


	@Test
	public void testPackage() throws IOException, PackagingException,
			DSLException {
		final File file = Packager
				.pack(new File(
						"src/test/resources/PackagerValidation/BasicTest"),
						new ArrayList<File>(0));
		assertNotNull(file);
		assertTrue(file.exists());

		final ZipFile zipFile = new ZipFile(file);

		final ZipEntry manifestEntry = zipFile.getEntry("META-INF/MANIFEST.MF");
		assertNotNull(manifestEntry);

		final InputStream inputStream = zipFile.getInputStream(manifestEntry);
		final Manifest mf = new Manifest();
		mf.read(inputStream);

		final String cp = mf.getMainAttributes().getValue("Class-Path");
		assertNotNull(cp);
		assertTrue(cp.contains("dsl.jar"));
		assertTrue(cp.contains("usm.jar"));
		inputStream.close();

		assertNotNull(zipFile.getEntry("ext/groovy-service.groovy"));
		assertNotNull(zipFile.getEntry("ext/run.groovy"));
		assertNotNull(zipFile.getEntry("META-INF/spring/pu.xml"));
		
		assertTrue(null == zipFile.getEntry("lib/usm.jar"));
		

		file.delete();
	}

	// @Test
	// public void testManifest() throws IOException, PackagingException,
	// DSLException {
	// Manifest mf = new Manifest();
	// mf.getMainAttributes().putValue("my", "value");
	// mf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
	// System.out.println(mf.getMainAttributes().entrySet());
	//
	// File file = File.createTempFile("test", "mf");
	// OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
	// mf.write(out);
	// out.close();
	//
	//
	// final String str = FileUtils.readFileToString(file);
	// System.out.println("File:");
	// System.out.println(str);
	//
	// }

}
