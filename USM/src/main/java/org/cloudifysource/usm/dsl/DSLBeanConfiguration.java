/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.usm.dsl;

import groovy.lang.Closure;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.annotation.PostConstruct;

import org.cloudifysource.dsl.Plugin;
import org.cloudifysource.dsl.PluginDescriptor;
import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.context.ServiceContext;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.usm.TCPPortEventListener;
import org.cloudifysource.usm.USMComponent;
import org.cloudifysource.usm.USMException;
import org.cloudifysource.usm.USMUtils;
import org.cloudifysource.usm.UniversalServiceManagerBean;
import org.cloudifysource.usm.UniversalServiceManagerConfiguration;
import org.cloudifysource.usm.details.Details;
import org.cloudifysource.usm.details.DetailsException;
import org.cloudifysource.usm.details.ProcessDetails;
import org.cloudifysource.usm.events.EventResult;
import org.cloudifysource.usm.events.USMEvent;
import org.cloudifysource.usm.installer.USMInstaller;
import org.cloudifysource.usm.launcher.DefaultProcessLauncher;
import org.cloudifysource.usm.launcher.ProcessLauncher;
import org.cloudifysource.usm.liveness.LivenessDetector;
import org.cloudifysource.usm.monitors.Monitor;
import org.cloudifysource.usm.monitors.MonitorException;
import org.cloudifysource.usm.monitors.process.ProcessMonitor;
import org.cloudifysource.usm.shutdown.DefaultProcessKiller;
import org.cloudifysource.usm.shutdown.ProcessKiller;
import org.cloudifysource.usm.stopDetection.ProcessStopDetector;
import org.cloudifysource.usm.stopDetection.StopDetector;
import org.openspaces.pu.container.support.ResourceApplicationContext;
import org.openspaces.ui.UserInterface;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*****************
 * The Spring Bean configuration for the USM - Spring components are initialized in this one class.
 * 
 * @author barakme
 * @since 2.0.0
 * 
 */
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

	// There is lots of boiler plate code here - ignore checkstyle.
	// CHECKSTYLE:OFF
	
	@PostConstruct
	public void init() {
		this.active = configuration instanceof DSLConfiguration;
		if (this.active) {
			this.service = ((DSLConfiguration) configuration).getService();
			this.puExtDir = ((DSLConfiguration) configuration).getPuExtDir();
			this.serviceContext = ((DSLConfiguration) configuration).getServiceContext();
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

		final ProcessLauncher retval = (ProcessLauncher) createBeanIfNotExistsType(new DefaultProcessLauncher(),
				ProcessLauncher.class);

		if (retval != null) {
			this.launcher = retval;
		} else {
			final Collection<ProcessLauncher> launchers =
					this.context.getBeanFactory().getBeansOfType(ProcessLauncher.class).values();
			if (launchers.size() == 0) {
				throw new IllegalStateException("No ProcessLauncher was found in Context!");
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

			@Override
			public void install() {
				new DSLEntryExecutor(service.getLifecycle().getInstall(), launcher, puExtDir);

			}
		},
				USMInstaller.class);
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
		if (plugins == null || plugins.size() == 0) {
			return null;
		}

		for (final PluginDescriptor descriptor : plugins) {
			final Plugin plugin = createPlugin(descriptor);
			if (plugin != null) {
				final String name = descriptor.getName() == null ? descriptor.getClassName() : descriptor.getName();

				try {
					this.context.getBeanFactory().getBean(name);
					throw new IllegalArgumentException("The name: " + name
							+ " is already in use and can't be used to identify a plugin");

				} catch (final NoSuchBeanDefinitionException e) {
					// ignore - this is the expected result
				}

				this.context.getBeanFactory().registerSingleton(name,
						plugin);

				this.context.getBeanFactory().autowireBean(plugin);

			}
		}

		return null;
	}

	private static java.util.logging.Logger logger = java.util.logging.Logger.getLogger(DSLBeanConfiguration.class
			.getName());

	private Plugin createPlugin(final PluginDescriptor descriptor) {

		Class<?> clazz = null;
		try {
			final String className = descriptor.getClassName();
			if (className == null || className.length() == 0) {
				throw new IllegalArgumentException("Plugin must have a class name");
			}

			clazz = Class.forName(className);
		} catch (final ClassNotFoundException e) {
			logger.log(Level.SEVERE,
					"Class " + descriptor.getClassName() + " for Plugin was not found",
					e);
			throw new IllegalArgumentException("Could not find class: " + descriptor.getClassName() + " for plugin "
					+ descriptor.getName(), e);
		}

		Object obj;
		try {
			obj = clazz.newInstance();
		} catch (final InstantiationException e) {
			logger.log(Level.SEVERE,
					"Plugin of class " + descriptor.getClassName() + " could not be created",
					e);
			return null;
		} catch (final IllegalAccessException e) {
			logger.log(Level.SEVERE,
					"Plugin of class " + descriptor.getClassName() + " could not be created",
					e);
			return null;
		}

		Plugin plugin = null;
		if (obj instanceof Plugin) {
			plugin = (Plugin) obj;

		} else {
			logger.log(Level.SEVERE,
					"Plugin of class " + descriptor.getClassName() + " does not "
							+ "implement the required Plugin interface and will not be created");
			return null;
		}

		if (!(plugin instanceof USMComponent)) {
			logger.log(Level.SEVERE,
					"Plugin of class " + descriptor.getClassName() + " does not "
							+ "implement the required USMComponent interface and will not be created");
			throw new IllegalArgumentException("Plugin of class " + descriptor.getClassName() + " does not "
					+ "implement the required USMComponent interface and will not be created");
		}

		plugin.setServiceContext(this.serviceContext);
		plugin.setConfig(descriptor.getConfig());

		return plugin;

	}

	private USMComponent createBeanIfNotExistsType(final USMComponent bean, final Class<?> typeToCheck) {
		if (!active) {
			return null;
		}
		if (typeToCheck == null || this.context.getBeanFactory().getBeansOfType(typeToCheck).size() == 0) {
			return bean;
		}
		return null;

	}

	@Override
	public void setApplicationContext(final ApplicationContext applicationContext) {
		this.context = (ResourceApplicationContext) applicationContext;

	}

	public UniversalServiceManagerConfiguration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(final UniversalServiceManagerConfiguration configuration) {
		this.configuration = configuration;
	}

	@Bean
	public USMComponent getDetails() {
		final Object details = this.service.getLifecycle().getDetails();
		if (details == null) {
			return null;
		}
		return new Details() {

			@SuppressWarnings("unchecked")
			@Override
			public Map<String, Object> getDetails(final UniversalServiceManagerBean usm,
					final UniversalServiceManagerConfiguration config)
					throws DetailsException {
				try {
					final Map<String, Object> returnMap = new HashMap<String, Object>();
					// Map can appear in 2 forms. As a map of as a closure that
					// returns a map
					logger.fine("getting Details map from details closure");
					if (details instanceof Map<?, ?>) {
						for (final Map.Entry<String, Object> entry : ((Map<String, Object>) details).entrySet()) {
							if (entry.getValue() instanceof Closure) {
								final EventResult value =
										new DSLEntryExecutor(entry.getValue(), launcher, puExtDir).run();
								if (value.isSuccess()) {
									returnMap.put(entry.getKey(),
											value.getResult());
								} else {
									logger.warning("Failed to execute closure with key value of " + entry.getKey());
								}
							} else {
								returnMap.put(entry.getKey(),
										entry.getValue());
							}
						}
					} else if (details instanceof Closure) {
						final EventResult result = new DSLEntryExecutor(details, launcher, puExtDir).run();
						if (result.isSuccess()) {
							returnMap.putAll((Map<String, Object>) result.getResult());
						}
					}
					return returnMap;
				} catch (final Exception e) {
					return null;
				}
			}
		};
	}

	@Bean
	public USMComponent getMonitor() {
		final Object monitor = this.service.getLifecycle().getMonitors();
		if (monitor == null) {
			return null;
		}

		if (monitor instanceof Closure<?>) {
			return new Monitor() {

				@SuppressWarnings("unchecked")
				@Override
				public Map<String, Number> getMonitorValues(final UniversalServiceManagerBean usm,
						final UniversalServiceManagerConfiguration config)
						throws MonitorException {
					final Object obj = ((Closure<?>) monitor).call();
					if (obj instanceof Map<?, ?>) {
						return USMUtils.convertMapToNumericValues((Map<String, Object>) obj);
					} 
					throw new IllegalArgumentException(
							"The Monitor closure defined in the DSL file does not evaluate to a Map! "
									+ "Received object was of type: " + obj.getClass().getName());
				}

			};

		}
		// else if the monitor is of type Map we run all of the map's values
		// as closures and return the output map.
		return new Monitor() {

			@SuppressWarnings("unchecked")
			@Override
			public Map<String, Number> getMonitorValues(final UniversalServiceManagerBean usm,
					final UniversalServiceManagerConfiguration config)
					throws MonitorException {
				final Map<String, Object> returnMap = new HashMap<String, Object>();
				if (monitor instanceof Map<?, ?>) {

					for (final Map.Entry<String, Object> entryObject : ((Map<String, Object>) monitor).entrySet()) {
						final Object object = entryObject.getValue();
						final EventResult result = new DSLEntryExecutor(object, launcher, puExtDir).run();
						if (!result.isSuccess()) {
							logger.log(Level.WARNING,
									"DSL Entry failed to execute: " + result.getException());
						} else {
							returnMap.put(entryObject.getKey(),
									result.getResult());
						}

					}
				}
				return USMUtils.convertMapToNumericValues(returnMap);
			}
		};
	}

	@Bean
	public USMEvent getProcessStopDetection() {

		boolean enabled = true;
		final String enabledProperty =
				this.service.getCustomProperties().get(CloudifyConstants.CUSTOM_PROPERTY_ENABLE_PID_MONITOR);
		if (enabledProperty != null) {
			enabled = Boolean.parseBoolean(enabledProperty);
		}

		if (enabled) {
			return new ProcessStopDetector();
		} 
		logger.warning("PID Based stop detection has been disabled due to custom property setting: "
				+ CloudifyConstants.CUSTOM_PROPERTY_ENABLE_PID_MONITOR);
		return null;
	}

	@Bean
	public USMEvent getDSLStopDetection() {
		final Object detector = this.service.getLifecycle().getStopDetection();
		if (detector == null) {
			return null;
		}

		return new StopDetector() {

			@Override
			public void init(final UniversalServiceManagerBean usm) {
			}

			@Override
			public int getOrder() {
				return 5;
			}

			@Override
			public boolean isServiceStopped()
					throws USMException {
				final EventResult result = new DSLEntryExecutor(detector, launcher, puExtDir).run();
				if (result.isSuccess()) {
					final Object retcode = result.getResult();
					if (retcode instanceof Boolean) {
						return (Boolean) retcode;
					} else {
						throw new USMException("A stop detector returned a result that is not a boolean. Result was: "
								+ retcode);
					}
				} else {
					throw new USMException(
							"A Stop Detector failed to execute. Exception was: " + result.getException(),
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
			public boolean isProcessAlive()
					throws USMException {

				final EventResult result = new DSLEntryExecutor(detector, launcher, puExtDir).run();
				if (result.isSuccess()) {
					final Object retcode = result.getResult();
					if (retcode instanceof Boolean) {
						return (Boolean) retcode;
					} else {
						throw new IllegalArgumentException(
								"A liveness detector returned a result that is not a boolean. Result was of type: " + retcode.getClass().getName() + ".Result was: " + retcode);
					}
				} else {
					throw new USMException("A Liveness Detector failed to execute. Exception was: "
							+ result.getException(), result.getException());
				}

			}

			@Override
			public void init(final UniversalServiceManagerBean usm) {
			}

			@Override
			public int getOrder() {
				return 5;
			}
		};
	}

	@Bean
	public USMEvent getNetworkEventHandler() {

		if (this.service.getNetwork() == null) {
			return null;
		}

		// check for override for this setting
		final String override =
				this.service.getCustomProperties().get(CloudifyConstants.CUSTOM_PROPERTY_ENABLE_TCP_PORT_MONITOR);
		if (override != null && override.equalsIgnoreCase("false")) {
			return null;
		}

		final String protocol = this.service.getNetwork().getProtocolDescription();
		if (protocol.equalsIgnoreCase("tcp") || protocol.equalsIgnoreCase("http")) {

			final int port = this.service.getNetwork().getPort();
			if (port <= 0) {
				return null;
			}

			return new TCPPortEventListener(port);
		} else {
			return null;
		}
	}
	
	// CHECKSTYLE:ON

}
