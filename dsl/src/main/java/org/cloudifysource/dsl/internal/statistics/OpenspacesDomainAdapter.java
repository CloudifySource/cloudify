/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.dsl.internal.statistics;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.activation.UnsupportedDataTypeException;

import org.apache.commons.beanutils.BeanUtils;
import org.cloudifysource.dsl.BalanceGauge;
import org.cloudifysource.dsl.BarLineChart;
import org.cloudifysource.dsl.MetricGroup;
import org.cloudifysource.dsl.Unit;
import org.cloudifysource.dsl.UserInterface;
import org.cloudifysource.dsl.Widget;
import org.cloudifysource.dsl.WidgetGroup;
import org.cloudifysource.dsl.statistics.AbstractConfig;

/**
 * adapter used to convert domain POJOs into Openspaces objects.  
 * 
 * @author adaml
 *
 */
public class OpenspacesDomainAdapter {

	private static final String OPENSPACES_STATISTICS_CONFIG_PACKAGE_NAME = "org.openspaces.admin.pu.statistics";

	/**
	 * Create an openspaces statistics config object using the DSL config POJO.
	 * @param config
	 * 			instance statistics config DSL POJO.
	 * @return
	 * 			the equivalent openspaces statistics config object.
	 * @throws InstantiationException .
	 * @throws IllegalAccessException .
	 * @throws ClassNotFoundException .
	 * @throws InvocationTargetException .
	 */
	public org.openspaces.admin.config.AbstractConfig createStatisticsConfig(
			final AbstractConfig config) 
					throws InstantiationException, IllegalAccessException, ClassNotFoundException, 
					InvocationTargetException {
		final String configClassName = config.getClass().getSimpleName();
		final String openspacesInstanceStatisticsClass = OPENSPACES_STATISTICS_CONFIG_PACKAGE_NAME 
				+ "." +  configClassName;
		Object obj = Class.forName(openspacesInstanceStatisticsClass).newInstance();
		//copy all properties from DSL POJO to its equivalent openspaces object. 
		BeanUtils.copyProperties(obj, config);
		return (org.openspaces.admin.config.AbstractConfig) obj;
	}
	
	/**
	 * Convert a DSL user interface POJO into an openspaces user interface POJO
	 * @param userInterface
	 * 			user interface DSL POJO.
	 * @return
	 * 			the equivalent openspaces user interface object.
	 * @throws UnsupportedDataTypeException 
	 * @throws IllegalAccessException .
	 * @throws InvocationTargetException .
	 */
	public org.openspaces.ui.UserInterface createOpenspacesUIObject (final UserInterface userInterface) 
			throws IllegalAccessException, InvocationTargetException, UnsupportedDataTypeException {
		org.openspaces.ui.UserInterface ui = new org.openspaces.ui.UserInterface();
		final List<MetricGroup> metricGroups = userInterface.getMetricGroups();
		final List<org.openspaces.ui.MetricGroup> destMetricGroups = new ArrayList<org.openspaces.ui.MetricGroup>();
		final List<Object> destMetrics = new ArrayList<Object>(); 
		for (MetricGroup metricGroup : metricGroups) {
			final org.openspaces.ui.MetricGroup group = new org.openspaces.ui.MetricGroup();
			List<Object> metrics = metricGroup.getMetrics();
			for (Object metric : metrics) {
				if (metric instanceof List<?>) {
					@SuppressWarnings("unchecked")
					List<Object> metricDef = (List<Object>) metric;
					String metricName = metricDef.get(0).toString();
					org.openspaces.ui.Unit axisYUnit = getOpenspacesAxisYUnit((Unit)metricDef.get(1));
					final List<Object> destMatrix = new ArrayList<Object>();
					destMatrix.add(metricName);
					destMatrix.add(axisYUnit);
					destMetrics.add(destMatrix);
				} else {
					destMetrics.add(metric);
				}
			}
			
			group.setMetrics(destMetrics);
			group.setName(metricGroup.getName());
			destMetricGroups.add(group);
		}
		
		final List<WidgetGroup> widgetGroups = userInterface.getWidgetGroups();
		final List<org.openspaces.ui.WidgetGroup> destWidgetGroups = new ArrayList<org.openspaces.ui.WidgetGroup>();
		for (WidgetGroup widgetGroup : widgetGroups) {
			final org.openspaces.ui.WidgetGroup destGroup = new org.openspaces.ui.WidgetGroup();
			destGroup.setName(widgetGroup.getName());
			destGroup.setTtile(widgetGroup.getTitle());
			final List<Widget> widgets = widgetGroup.getWidgets();
			final List<org.openspaces.ui.Widget> destWidgets = new ArrayList<org.openspaces.ui.Widget>();
			for (Widget widget : widgets) {
				if (widget instanceof BarLineChart) {
					final org.openspaces.ui.BarLineChart chart = new org.openspaces.ui.BarLineChart();
					final Unit axisYUnit = ((BarLineChart) widget).getAxisYUnit();
					org.openspaces.ui.Unit destUnit = getOpenspacesAxisYUnit(axisYUnit);
					chart.setAxisYUnit(destUnit);
					chart.setMetric(((BarLineChart) widget).getMetric());
					destWidgets.add(chart);
				} else if (widget instanceof BalanceGauge) {
					org.openspaces.ui.BalanceGauge gauge = new org.openspaces.ui.BalanceGauge();
					BeanUtils.copyProperties(gauge, widget);
					destWidgets.add(gauge);
				} else {
					throw new UnsupportedDataTypeException("widget type: " + widget.getClass().getSimpleName() 
							+ " is not supported.");
				}
			}
			destGroup.setWidgets(destWidgets);
			destWidgetGroups.add(destGroup);
		}
		
		ui.setMetricGroups(destMetricGroups);
		ui.setWidgetGroups(destWidgetGroups);
		return ui;
	}

	private org.openspaces.ui.Unit getOpenspacesAxisYUnit(final Unit axisYUnit) 
			throws UnsupportedDataTypeException {
		switch (axisYUnit) {
		case REGULAR:
			return org.openspaces.ui.Unit.REGULAR;
		case PERCENTAGE:
			return org.openspaces.ui.Unit.PERCENTAGE;
		case MEMORY: 
			return org.openspaces.ui.Unit.MEMORY;
		case DURATION:
			return org.openspaces.ui.Unit.DURATION;
		default:
			throw new UnsupportedDataTypeException("Unit type " + axisYUnit.toString()
					+ " is not supported");
		}
	}

}