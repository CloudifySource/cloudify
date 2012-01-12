package org.cloudifysource.usm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.math.NumberUtils;
import org.hyperic.sigar.FileInfo;
import org.hyperic.sigar.ProcState;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.jini.rio.boot.ServiceClassLoader;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.pu.container.support.ResourceApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import com.gigaspaces.internal.sigar.SigarHolder;

public final class USMUtils {

	

	//private static final int POST_SYNC_PROCESS_SLEEP_INTERVAL = 500;

	private USMUtils() {
		// private constructor to prevent instantiation.
	}
	private static java.util.logging.Logger logger =
			java.util.logging.Logger.getLogger(USMUtils.class.getName());
	private static Admin admin;

	public static boolean markFileAsExecutable(final Sigar sigar, final File file) throws USMException {

		if (!file.exists()) {
			return false;
		}
		if (USMUtils.isWindows()) {
			return true; // not used in windows
		}

		if (USMUtils.markFileAsExecutableUsingJdk(file)) {
			return true;
		}

		if (USMUtils.isFileExecutableSigar(file)) {
			return true;
		} 
		
		logger.info("File " + file + " is not executable according to sigar");
		return USMUtils.markFileAsExecutableUsingChmod(file);
	}

	private static boolean markFileAsExecutableUsingJdk(final File file) throws USMException {
		// TODO - refactor this - just catch exception on the whole block
		logger.fine("Attempting to set file as executable using REFLECTION");
		Method method = null;
		try {
			method = File.class.getMethod("setExecutable", new Class[] { Boolean.TYPE });
		} catch (final SecurityException e) {
			throw new USMException("Failed to check if file " + file.getAbsolutePath() + " is executable", e);
		} catch (final NoSuchMethodException e) {
			// ignore - this can happen
		}

		if (method == null) {
			logger.warning("setExecutable not supported by JDK. GigaSpaces Cloudify should be executed on Java Version >= 1.6");
			return false;
		} 

		try {
			final boolean retval = (Boolean) method.invoke(file, true);
			logger.fine("File " + file + " was marked as executable");
			if (!retval) {
				logger.warning("File: " + file + " is not executable. " +
						"Attempt to mark it as executable has failed. " +
				"Execution of this command may not be possible");
			}
			return retval;
		} catch (final IllegalArgumentException e) {
			throw new USMException("Failed to make file " + file.getAbsolutePath() + " as executable", e);
		} catch (final IllegalAccessException e) {
			throw new USMException("Failed to make file " + file.getAbsolutePath() + " as executable", e);
		} catch (final InvocationTargetException e) {
			throw new USMException("Failed to make file " + file.getAbsolutePath() + " as executable", e);
		}
	}

	private static boolean isFileExecutableSigar(final File file) throws USMException {
		long mode;
		try {
			logger.info("Checking if file " + file + " is executable using sigar");
			mode = SigarHolder.getSigar().getFileInfo(file.getAbsolutePath()).getMode();
		} catch (final SigarException e) {
			throw new USMException("Failed to check if file " + file.getAbsolutePath() + " is executable", e);
		}

		final long executable = mode & FileInfo.MODE_UEXECUTE;
		return (executable > 0);

	}

	protected static boolean markFileAsExecutableUsingChmod(final File file) throws USMException {
		try {
			logger.info("File " + file + " will be marked as executable using chmod command");
			final Process p = Runtime.getRuntime().exec(new String[] { "chmod", "+x", file.getAbsolutePath() });
			final int result = p.waitFor();
			logger.info("chmod command finished with result = " + result);
			if (result != 0) {
				logger.warning("chmod command did not terminate successfully. File " + file + " may not be executable!");
			}
			return (result == 0);
		} catch (final IOException e) {
			throw new USMException("Failed to execute chmod command:  chmod +x " + file.getAbsolutePath(), e);
		} catch (final InterruptedException e) {
			throw new USMException("Failed to execute chmod command on file " + file, e);
		}
	}

	public static boolean isWindows() {
		final String os = System.getProperty("os.name").toLowerCase();
		return (os.indexOf("win") >= 0);
	}

	public static boolean isLinuxOrUnix() {
		return !isWindows();
//		final String os = System.getProperty("os.name").toLowerCase();
//		// linux or unix
//		return ((os.indexOf("nix") >= 0) || (os.indexOf("nux") >= 0));
	}

	public static boolean isRunningInGSC(final ApplicationContext ctx) {
		return (ctx.getClassLoader() instanceof ServiceClassLoader) ;
	}
	
	public static File getPUWorkDir(final ApplicationContext ctx) {
		File puWorkDir = null;

		if (isRunningInGSC(ctx)) {// running in
																	// GSC
			final ServiceClassLoader scl = (ServiceClassLoader) ctx.getClassLoader();

			final URL url = scl.getSlashPath();
			URI uri;
			try {
				uri = url.toURI();
			} catch (final URISyntaxException e) {
				// TODO Auto-generated catch block
				throw new IllegalArgumentException("Failed to create URI from URL: " + url, e);
			}

			puWorkDir = new File(uri);

		} else {

			final ResourceApplicationContext rac = (ResourceApplicationContext) ctx;

			try {
				final Field resourcesField = rac.getClass().getDeclaredField("resources");
				final boolean accessibleBefore = resourcesField.isAccessible();

				resourcesField.setAccessible(true);
				final Resource[] resources = (Resource[]) resourcesField.get(rac);
				for (final Resource resource : resources) {
					// find META-INF/spring/pu.xml
					final File file = resource.getFile();
					if (file.getName().equals("pu.xml") &&
							file.getParentFile().getName().equals("spring") &&
							file.getParentFile().getParentFile().getName().equals("META-INF")) {
						puWorkDir = resource.getFile().getParentFile().getParentFile().getParentFile();
						break;
					}

				}

				resourcesField.setAccessible(accessibleBefore);
			} catch (final Exception e) {
				throw new IllegalArgumentException("Could not find pu.xml in the ResourceApplicationContext", e);
			}
			if (puWorkDir == null) {
				throw new IllegalArgumentException("Could not find pu.xml in the ResourceApplicationContext");
			}
		}

		if (!puWorkDir.exists()) {
			throw new IllegalStateException("Could not find PU work dir at: " + puWorkDir);
		}

		final File puExtDir = new File(puWorkDir, "ext");
		if (!puExtDir.exists()) {
			throw new IllegalStateException("Could not find PU ext dir at: " + puExtDir);
		}

		return puWorkDir;
	}
	
	public static String readFile(final File file) throws USMException {
		// TODO - rewrite using read(file.size())
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			final StringBuilder sb = new StringBuilder();
			while (true) {
				final String line = reader.readLine();
				if (line == null) {
					break;
				} else {
					sb.append(line).append("\n");
				}
			}
			reader.close();
			return sb.toString();
		} catch (final IOException ioe) {
			throw new USMException("Failed to read contents of file: " + file, ioe);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (final IOException ioe) {
					// ignore
				}

			}
		}
	}

	public static Admin getAdmin() {
		if (admin != null) {
			return admin;
		}

		// TODO: Should I handle LOOKUPGROUPS values with double quotes (")?
		final AdminFactory factory = new AdminFactory();
		factory.useDaemonThreads(true);
		admin = factory.createAdmin();

		logger.info("Created new Admin Object with groups: " + Arrays.toString(admin.getGroups())
				+ " and Locators: " + Arrays.toString(admin.getLocators()));

		return admin;
	}

	public static List<Long> getProcessParentChain(final long childPid, final long myPid) throws USMException {

		final LinkedList<Long> list = new LinkedList<Long>();
		if (childPid == 0) {
			logger.warning("The child PID is 0 - this usually means that this USM instance had a problem during startup. Please check the logs for more details.");
			return list;
		}

		long currentPid = childPid;

		while (currentPid != myPid) {
			list.add(currentPid);
			final long ppid = USMUtils.getParentPid(currentPid);

			if (ppid == 0) {
				logger.severe("Attempt to create a process chain from child process " + childPid + " this process("
						+ myPid + "). Process " + childPid + " is not a descendant of this process. Only process "
						+ childPid + " will be included in the chain");
				list.clear();
				list.add(childPid);
				return list;
			}
			currentPid = ppid;
		}

		return list;
	}

	public static long getParentPid(final long pid) throws USMException {
		ProcState procState = null;

		procState = USMUtils.getProcState(pid);
		if (procState == null) {
			return 0;
		}

		return procState.getPpid();

	}

	public static boolean isProcessAlive(final long pid) throws USMException {

		final ProcState procState = USMUtils.getProcState(pid);
		if ((procState == null) || (procState.getState() == ProcState.STOP)
				|| (procState.getState() == ProcState.ZOMBIE)) {
			return false;
		}

		return true;
	}

	private static ProcState getProcState(final long pid) throws USMException {
		final Sigar sigar = SigarHolder.getSigar();
		ProcState procState = null;
		try {
			procState = sigar.getProcState(pid);
		} catch (final SigarException e) {
			if ("No such process".equals(e.getMessage())) {
				return null;
			}

			throw new USMException("Failed to check if process with PID: "
					+ pid + " is alive. Error was: "
					+ e.getMessage(), e);
		}
		return procState;
	}
	

	//converts a map of type <String, Object> to a Map of <String, Number>
	public static Map<String, Number> convertMapToNumericValues(Map<String, Object> map) {
		Map<String, Number> returnMap = new HashMap<String, Number>();
		for (Map.Entry<String, Object> entryObject : ((Map<String, Object>)map).entrySet()){
			if (entryObject.getValue() instanceof Number) {
				returnMap.put(entryObject.getKey(),
						(Number) entryObject.getValue());
			} else if (entryObject.getValue() instanceof String) {
				if (NumberUtils.isNumber((String) entryObject.getValue())) {
					Number number = NumberUtils
					.createNumber((String) entryObject.getValue());
					returnMap.put(entryObject.getKey(), number);
				}
			}else{
				logger.info("monitor value is expected to be numeric: " + entryObject.getValue().toString());
			}
		}
		return returnMap;
	}

	public static void shutdownAdmin() {
		if (admin == null) {
			return;
		}
		
		admin.close();

	}


	
}
