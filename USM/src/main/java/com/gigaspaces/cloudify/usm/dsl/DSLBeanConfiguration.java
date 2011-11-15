package com.gigaspaces.cloudify.usm.dsl;

import groovy.lang.Closure;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.math.NumberUtils;
import org.openspaces.pu.container.support.ResourceApplicationContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.gigaspaces.cloudify.dsl.Plugin;
import com.gigaspaces.cloudify.dsl.PluginDescriptor;
import com.gigaspaces.cloudify.dsl.Service;
import com.gigaspaces.cloudify.dsl.context.ServiceContext;
import com.gigaspaces.cloudify.usm.USMComponent;
import com.gigaspaces.cloudify.usm.USMException;
import com.gigaspaces.cloudify.usm.UniversalServiceManagerBean;
import com.gigaspaces.cloudify.usm.UniversalServiceManagerConfiguration;
import com.gigaspaces.cloudify.usm.details.Details;
import com.gigaspaces.cloudify.usm.details.DetailsException;
import com.gigaspaces.cloudify.usm.details.ProcessDetails;
import com.gigaspaces.cloudify.usm.events.EventResult;
import com.gigaspaces.cloudify.usm.events.USMEvent;
import com.gigaspaces.cloudify.usm.installer.USMInstaller;
import com.gigaspaces.cloudify.usm.launcher.DefaultProcessLauncher;
import com.gigaspaces.cloudify.usm.launcher.ProcessLauncher;
import com.gigaspaces.cloudify.usm.liveness.LivenessDetector;
import com.gigaspaces.cloudify.usm.monitors.Monitor;
import com.gigaspaces.cloudify.usm.monitors.MonitorException;
import com.gigaspaces.cloudify.usm.monitors.process.ProcessMonitor;
import com.gigaspaces.cloudify.usm.shutdown.DefaultProcessKiller;
import com.gigaspaces.cloudify.usm.shutdown.ProcessKiller;
import com.gigaspaces.cloudify.usm.stopDetection.ProcessStopDetector;
import com.gigaspaces.cloudify.usm.stopDetection.StopDetector;

import org.openspaces.ui.UserInterface;

@Configuration
public class DSLBeanConfiguration implements ApplicationContextAware {

	@Autowired(required = false)
	private UniversalServiceManagerConfiguration configuration;
	private boolean active;
	private ResourceApplicationContext context;
	private Service service;
	private File puExtDir;

	private ProcessLauncher launcher;
	private ServiceContext serviceContext;

	@PostConstruct
	public void init() {
		this.active = (configuration instanceof DSLConfiguration);
		if (this.active) {
			this.service = ((DSLConfiguration) configuration).getService();
			this.puExtDir = ((DSLConfiguration) configuration).getPuExtDir();
			this.serviceContext = ((DSLConfiguration) configuration)
					.getServiceContext();
		}
	}

	@Bean
	public USMComponent getDslDetails() {
		if (!active) {
			return null;
		}
		return new DSLDetails();
	}

	@Bean
	public USMComponent getDslLauncher() {
		if (!active) {
			return null;
		}

		final ProcessLauncher retval = (ProcessLauncher) createBeanIfNotExistsType(
				new DefaultProcessLauncher(), ProcessLauncher.class);

		if (retval != null) {
			this.launcher = retval;
		} else {
			final Collection<ProcessLauncher> launchers = this.context
					.getBeanFactory().getBeansOfType(ProcessLauncher.class)
					.values();
			if (launchers.size() == 0) {
				throw new IllegalStateException(
						"No ProcessLauncher was found in Context!");
			}
			this.launcher = launchers.iterator().next();
		}
		return retval;
	}

	@Bean
	public USMComponent getDslKiller() {
		return createBeanIfNotExistsType(new DefaultProcessKiller(),
				ProcessKiller.class);
	}

	@Bean
	public USMComponent getDslInstaller() {

		return createBeanIfNotExistsType(new USMInstaller() {

			public void install() {
				new DSLEntryExecutor(service.getLifecycle().getInstall(),
						launcher, puExtDir);

			}
		}, USMInstaller.class);
	}

	@Bean
	public USMComponent getDslProcessMonitor() {
		if (!active) {
			return null;
		}
		return new ProcessMonitor();
	}

	@Bean
	public USMComponent getDslProcessDetails() {
		if (!active) {
			return null;
		}
		return new ProcessDetails();
	}

	@Bean
	public USMComponent getDslEventCommand() {
		if (!active) {
			return null;
		}

		return new DSLCommandsLifecycleListener();

	}

	@Bean
	public USMComponent getDslUIDetails() {
		if (!active) {
			return null;
		}

		if (this.service.getUserInterface() == null) {
			return null;
		}

		final UserInterface ui = this.service.getUserInterface();
		return new UIDetails(ui);

	}

	@Bean
	public USMComponent getDslPlugins() {
		if (!active) {
			return null;
		}
		final List<PluginDescriptor> plugins = this.service.getPlugins();
		if ((plugins == null) || (plugins.size() == 0)) {
			return null;
		}

		for (final PluginDescriptor descriptor : plugins) {
			final Plugin plugin = createPlugin(descriptor);
			if (plugin != null) {
				final String name = (descriptor.getName() == null ? descriptor
						.getClassName() : descriptor.getName());

				try {
					this.context.getBeanFactory().getBean(name);
					throw new IllegalArgumentException(
							"The name: "
									+ name
									+ " is already in use and can't be used to identify a plugin");

				} catch (NoSuchBeanDefinitionException e) {
					// ignore - this is the expected result
				}

				this.context.getBeanFactory().registerSingleton(name, plugin);

				this.context.getBeanFactory().autowireBean(plugin);

			}
		}

		return null;
	}

	private static java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(DSLBeanConfiguration.class.getName());

	private Plugin createPlugin(final PluginDescriptor descriptor) {

		Class<?> clazz = null;
		try {
			final String className = descriptor.getClassName();
			if ((className == null) || (className.length() == 0)) {
				throw new IllegalArgumentException(
						"Plugin must have a class name");
			}

			clazz = Class.forName(className);
		} catch (final ClassNotFoundException e) {
			logger.log(Level.SEVERE, "Class " + descriptor.getClassName()
					+ " for Plugin was not found", e);
			throw new IllegalArgumentException("Could not find class: "
					+ descriptor.getClassName() + " for plugin "
					+ descriptor.getName(), e);
		}

		Object obj;
		try {
			obj = clazz.newInstance();
		} catch (final InstantiationException e) {
			logger.log(Level.SEVERE,
					"Plugin of class " + descriptor.getClassName()
							+ " could not be created", e);
			return null;
		} catch (final IllegalAccessException e) {
			logger.log(Level.SEVERE,
					"Plugin of class " + descriptor.getClassName()
							+ " could not be created", e);
			return null;
		}

		Plugin plugin = null;
		if (obj instanceof Plugin) {
			plugin = (Plugin) obj;

		} else {
			logger.log(
					Level.SEVERE,
					"Plugin of class "
							+ descriptor.getClassName()
							+ " does not "
							+ "implement the required Plugin interface and will not be created");
			return null;
		}

		if (!(plugin instanceof USMComponent)) {
			logger.log(
					Level.SEVERE,
					"Plugin of class "
							+ descriptor.getClassName()
							+ " does not "
							+ "implement the required USMComponent interface and will not be created");
			throw new IllegalArgumentException(
					"Plugin of class "
							+ descriptor.getClassName()
							+ " does not "
							+ "implement the required USMComponent interface and will not be created");
		}

		plugin.setServiceContext(this.serviceContext);
		plugin.setConfig(descriptor.getConfig());

		return plugin;

	}

	private USMComponent createBeanIfNotExistsType(final USMComponent bean,
			final Class<?> typeToCheck) {
		if (!active) {
			return null;
		}
		if ((typeToCheck == null)
				|| (this.context.getBeanFactory().getBeansOfType(typeToCheck)
						.size() == 0)) {
			return bean;
		}
		return null;

	}

	public void setApplicationContext(
			final ApplicationContext applicationContext) throws BeansException {
		this.context = (ResourceApplicationContext) applicationContext;

	}

	public UniversalServiceManagerConfiguration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(
			final UniversalServiceManagerConfiguration configuration) {
		this.configuration = configuration;
	}

	@Bean
	public USMComponent getDetails() {
		final Object details = this.service.getLifecycle().getDetails();
		if (details == null) {
			return null;
		}
		return new Details() {

			@Override
			public Map<String, Object> getDetails(
					UniversalServiceManagerBean usm,
					UniversalServiceManagerConfiguration config)
					throws DetailsException {
				try {
					Map<String, Object> returnMap = new HashMap<String, Object>();
					// Map can appear in 2 forms. As a map of as a closure that
					// returns a map
					final Map<String, Object> detailsMap = getMapFromClosureObject(details);

					for (Map.Entry<String, Object> entryObject : detailsMap
							.entrySet()) {
						Object object = entryObject.getValue();
						EventResult result = new DSLEntryExecutor(object,
								launcher, puExtDir).run();
						if (!result.isSuccess()) {
							logger.log(Level.WARNING,
									"DSL Entry failed to execute: ", result
											.getException().getStackTrace());
						} else {
							returnMap.put(entryObject.getKey(),
									result.getResult());
						}
					}
					return returnMap;
				} catch (Exception e) {
					return null;
				}
			}
		};
	}

	// runs and returns the details/monitors closure if exists.
	private Map<String, Object> getMapFromClosureObject(Object mapOrClosure) {
		Map<String, Object> returnMap;
		if (!(mapOrClosure instanceof Map<?, ?>)) {

			EventResult run = new DSLEntryExecutor(mapOrClosure, launcher,
					puExtDir).run();
			returnMap = (Map<String, Object>) run.getResult();
		} else {
			returnMap = (Map<String, Object>) mapOrClosure;
		}
		return returnMap;
	}

	@Bean
	public USMComponent getMonitor() {
		final Object monitor = this.service.getLifecycle().getMonitors();
		if (monitor == null) {
			return null;
		}

		if (monitor instanceof Closure) {
			return new Monitor() {

				@Override
				public Map<String, Number> getMonitorValues(
						UniversalServiceManagerBean usm,
						UniversalServiceManagerConfiguration config)
						throws MonitorException {
					Object obj = ((Closure) monitor).call();
					if (obj instanceof Map<?, ?>) {
						// TODO - validate key and value types
						return (Map<String, Number>) obj;
					} else {
						throw new IllegalArgumentException(
								"The Monitor closure defined in the DSL file does not evaluate to a Map! Received object was of type: "
										+ obj.getClass().getName());
					}

				}
			};

		}

		return new Monitor() {
			@Override
			public Map<String, Number> getMonitorValues(
					UniversalServiceManagerBean usm,
					UniversalServiceManagerConfiguration config)
					throws MonitorException {
				try {
					Map<String, Number> returnMap = new HashMap<String, Number>();
					final Map<String, Object> monitorsMap = getMapFromClosureObject(monitor);
					for (Map.Entry<String, Object> entryObject : monitorsMap
							.entrySet()) {
						Object object = entryObject.getValue();
						EventResult result = new DSLEntryExecutor(object,
								launcher, puExtDir).run();
						if (!result.isSuccess()) {
							logger.log(
									Level.WARNING,
									"DSL Entry failed to execute: "
											+ result.getException());
						} else if (result.getResult() instanceof Number) {
							returnMap.put(entryObject.getKey(),
									(Number) result.getResult());
						} else if (result.getResult() instanceof String) {
							if (NumberUtils.isNumber((String) result
									.getResult())) {
								Number number = NumberUtils
										.createNumber((String) result
												.getResult());
								returnMap.put(entryObject.getKey(), number);
							}
						} else {
							logger.log(
									Level.WARNING,
									"Expected DSL result to be numeric but received a non-numeric value",
									result.getException().getStackTrace());
						}
					}
					return returnMap;
				} catch (Exception e) {
					return null;
				}
			}
		};
	}

	@Bean
	public USMEvent getProcessStopDetection() {
		return new ProcessStopDetector();
	}

	@Bean
	public USMEvent getDSLStopDetection() {
		final Object detector = this.service.getLifecycle().getStopDetection();
		if (detector == null) {
			return null;
		}

		return new StopDetector() {

			@Override
			public void init(UniversalServiceManagerBean usm) {
			}

			@Override
			public int getOrder() {
				return 5;
			}

			@Override
			public boolean isServiceStopped() throws USMException {
				EventResult result = new DSLEntryExecutor(detector, launcher,
						puExtDir).run();
				if (result.isSuccess()) {
					final Object retcode = result.getResult();
					if (retcode instanceof Boolean) {
						return (Boolean) retcode;
					} else {
						throw new USMException(
								"A stop detector returned a result that is not a boolean. Result was: "
										+ retcode);
					}
				} else {
					throw new USMException(
							"A Stop Detector failed to execute. Exception was: "
									+ result.getException(),
							result.getException());
				}
			}
		};
	}

	@Bean
	public USMEvent getDSLStartDetection() {
		final Object detector = this.service.getLifecycle().getStartDetection();
		if (detector == null) {
			return null;
		}
		return new LivenessDetector() {

			@Override
			public boolean isProcessAlive() throws USMException {

				EventResult result = new DSLEntryExecutor(detector, launcher,
						puExtDir).run();
				if (result.isSuccess()) {
					final Object retcode = result.getResult();
					if (retcode instanceof Boolean) {
						return (Boolean) retcode;
					} else {
						throw new IllegalArgumentException(
								"A liveness detector returned a result that is not a boolean. Result was: "
										+ retcode);
					}
				} else {
					throw new USMException(
							"A Liveness Detector failed to execute. Exception was: "
									+ result.getException(),
							result.getException());
				}

			}

			@Override
			public void init(UniversalServiceManagerBean usm) {
			}

			@Override
			public int getOrder() {
				return 5;
			}
		};
	}
}
