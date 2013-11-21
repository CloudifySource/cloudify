package org.cloudifysource.dsl;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovySystem;
import groovy.lang.Script;
import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;

import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.DSLReader;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.codehaus.groovy.control.CompilationFailedException;
import org.junit.Ignore;
import org.junit.Test;

/*********
 * long running tests for permgen research around Groovy ConfigSlurper. They are disabled as they take a long time to run.
 * @author barakme
 *
 */
public class PermGenTests {

	@Ignore
	@Test
	public void testConfigSlurperPermGen() throws CompilationFailedException, InstantiationException, IllegalAccessException {
		for (int i = 0; i < 50000; i++) {
			GroovyClassLoader gcl = new GroovyClassLoader();
			Script script = (Script) gcl.parseClass("foo = bar").newInstance();
			ConfigObject config = new ConfigSlurper().parse(script);
			if(i % 10 == 0) {
				System.out.println(i);
			}
			//GroovySystem.getMetaClassRegistry().removeMetaClass(script.getClass());
			//gcl.clearCache();
		}
	}
	
	
	@Ignore
	@Test
	public void testConfigSlurperPermGenFromFile() throws CompilationFailedException, InstantiationException, IllegalAccessException, IOException {
		final File permgenFile = new File("src/test/resources/permgen/permgen.properties");
		for (int i = 0; i < 50000; i++) {
			GroovyClassLoader gcl = new GroovyClassLoader();
			Script script = (Script) gcl.parseClass(permgenFile).newInstance();					
			ConfigObject config = new ConfigSlurper().parse(script);
			if(i % 10 == 0) {
				System.out.println(i);
			}
			GroovySystem.getMetaClassRegistry().removeMetaClass(script.getClass());
		}
	}
	
	
	@Ignore
	@Test
	public void testConfigSlurperFromDSLReader() throws MalformedURLException {
		
		for (int i = 0; i < 50000; i++) {
			final File propertiesFile = new File("src/test/resources/permgen/permgen.properties");
			final ConfigObject config = new ConfigSlurper().parse(propertiesFile.toURI().toURL());
		}
		
	}
	
	@Ignore
	@Test
	public void testDNSApplicationWithExternalProperties() throws MalformedURLException, DSLException, InterruptedException {
		final File propertiesFile = new File("src/test/resources/permgen/dns-application/dns-application.properties");
		final File dnsApplicationDir = new File("src/test/resources/permgen/dns-application");
		final File dnsMasterDir = new File("src/test/resources/permgen/dns-application/dnsMasterService");
		
		ExecutorService pool = Executors.newFixedThreadPool(4);
		for(int i=0;i<4;++i) {
			final int workerIndex = i;
			pool.submit(new Runnable() {
				
				@Override
				public void run() {
					try {
						parseServiceFromApplicationInLoop(propertiesFile, dnsMasterDir, workerIndex);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
					
				}
			});
			
		}
		
		pool.awaitTermination(10, TimeUnit.MINUTES);
		
	}

	private void parseServiceFromApplicationInLoop(final File propertiesFile, final File dnsMasterDir, final int workerIndex)
			throws MalformedURLException, DSLException {
		for(int i=0;i<20000;++i) {
			parseServiceFromApplication(propertiesFile, dnsMasterDir);
			if(i%10 == 0) {
				System.out.println("Worker: " + workerIndex + ", Iteration: " + i);
			}
		}
	}

	private void parseServiceFromApplication(final File propertiesFile, final File dnsMasterDir)
			throws MalformedURLException, DSLException {
		ConfigObject config = new ConfigSlurper().parse(propertiesFile.toURI().toURL());
		Map<String, Object> applicationProperties = config;
		
		DSLReader dslReader = new DSLReader();
		dslReader.setRunningInGSC(false);
		dslReader.setPropertiesFileName(null); //find default
		
		dslReader.setWorkDir(dnsMasterDir);
		dslReader.setApplicationProperties(applicationProperties);
		dslReader.setDslFileNameSuffix(DSLUtils.SERVICE_DSL_FILE_NAME_SUFFIX);

		dslReader.readDslEntity(Service.class);
	}
}
